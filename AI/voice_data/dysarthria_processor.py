"""
dysarthria_processor.py
구음장애 데이터(AI Hub) 전처리
입력: 디렉토리 내 .wav(48kHz) + .json 쌍  (또는 zip 아카이브)
출력: 리샘플된 세그먼트 WAV + records 리스트

기대 데이터 구조:
  data_dir/
  ├── neuro/    (Type 01 - 뇌신경)
  │   ├── audio/   *.wav  (48000 Hz)
  │   └── label/   *.json
  ├── speech/   (Type 02 - 언어청각)
  │   ├── audio/
  │   └── label/
  └── larynx/   (Type 03 - 후두)
      ├── audio/
      └── label/

JSON 구조:
{
  "playTime": 2913.2,
  "Transcript": "나는 바지를 입고 단추를 채웁니다. 책상 위에 가방이 있습니다.",
  "File_id": "ID-02-25-N-KSM-02-04-M-45-JL.wav",
  "Disease_info": {"Type": "02", "Subcategory2": "25"},
  "Meta_info": {"SamplingRate": 48000},
  "Patient_info": {"Sex": "M", "Age": "45", "Area": "JL"}
}
"""

import io
import json
import logging
import re
import struct
import zipfile
from pathlib import Path
from typing import Dict, List, Optional, Tuple

import numpy as np
import soundfile as sf
from tqdm import tqdm

try:
    import librosa

    HAS_LIBROSA = True
except ImportError:
    HAS_LIBROSA = False
    logging.warning("librosa 미설치: 리샘플링에 soundfile 백엔드 사용")

try:
    import webrtcvad

    HAS_VAD = True
except ImportError:
    HAS_VAD = False
    logging.warning("webrtcvad 미설치: 에너지 기반 VAD 사용")

logger = logging.getLogger(__name__)

TARGET_SR = 16000
SENTENCE_SPLIT_RE = re.compile(r"(?<=[.?!])\s+")

TYPE_TO_SOURCE = {"01": "dysarthria_neuro", "02": "dysarthria_speech", "03": "dysarthria_larynx"}
SUBDIR_TO_TYPE = {"neuro": "01", "speech": "02", "larynx": "03"}


def resample_audio(audio: np.ndarray, orig_sr: int, target_sr: int = TARGET_SR) -> np.ndarray:
    """48kHz → 16kHz 리샘플링."""
    if orig_sr == target_sr:
        return audio
    if HAS_LIBROSA:
        return librosa.resample(audio.astype(np.float32), orig_sr=orig_sr, target_sr=target_sr)
    # librosa 없을 때: 단순 정수배 다운샘플(48k→16k = 1/3)
    ratio = orig_sr // target_sr
    if orig_sr % target_sr == 0:
        return audio[::ratio]
    raise RuntimeError(f"librosa 없이 비정수배 리샘플 불가: {orig_sr}→{target_sr}")


def split_sentences(transcript: str) -> List[str]:
    """Transcript 문자열을 문장 단위로 분리하고 끝 구두점 제거."""
    parts = SENTENCE_SPLIT_RE.split(transcript.strip())
    cleaned = []
    for p in parts:
        p = p.strip().rstrip(".?!").strip()
        if len(p) >= 2:
            cleaned.append(p)
    return cleaned


# ------------------------------------------------------------------ #
#  VAD 유틸리티                                                         #
# ------------------------------------------------------------------ #


def _float_to_pcm16(audio: np.ndarray) -> bytes:
    """float32 → int16 PCM 바이트열 변환."""
    pcm = np.clip(audio, -1.0, 1.0)
    pcm = (pcm * 32767).astype(np.int16)
    return pcm.tobytes()


def vad_speech_segments(
    audio: np.ndarray,
    sr: int,
    aggressiveness: int = 2,
    frame_ms: int = 30,
    min_gap_ms: int = 300,
    min_dur_ms: int = 300,
) -> List[Tuple[float, float]]:
    """VAD로 음성 구간(초 단위) 목록 반환.

    HAS_VAD=False 이면 에너지 기반 폴백 사용.
    """
    if audio.ndim > 1:
        audio = audio[:, 0]

    if HAS_VAD:
        return _vad_webrtc(audio, sr, aggressiveness, frame_ms, min_gap_ms, min_dur_ms)
    return _vad_energy(audio, sr, frame_ms, min_gap_ms, min_dur_ms)


def _vad_webrtc(
    audio: np.ndarray,
    sr: int,
    aggressiveness: int,
    frame_ms: int,
    min_gap_ms: int,
    min_dur_ms: int,
) -> List[Tuple[float, float]]:
    vad = webrtcvad.Vad(aggressiveness)
    frame_len = int(sr * frame_ms / 1000)
    pcm = _float_to_pcm16(audio.astype(np.float32))

    speech_flags = []
    for i in range(0, len(audio) - frame_len, frame_len):
        frame = pcm[i * 2 : (i + frame_len) * 2]
        if len(frame) < frame_len * 2:
            break
        try:
            is_speech = vad.is_speech(frame, sr)
        except Exception:
            is_speech = False
        speech_flags.append((i / sr, (i + frame_len) / sr, is_speech))

    return _merge_flags(speech_flags, min_gap_ms / 1000, min_dur_ms / 1000)


def _vad_energy(
    audio: np.ndarray,
    sr: int,
    frame_ms: int,
    min_gap_ms: int,
    min_dur_ms: int,
) -> List[Tuple[float, float]]:
    frame_len = int(sr * frame_ms / 1000)
    threshold = np.percentile(np.abs(audio), 30) * 3

    speech_flags = []
    for i in range(0, len(audio) - frame_len, frame_len):
        frame = audio[i : i + frame_len]
        is_speech = np.mean(np.abs(frame)) > threshold
        speech_flags.append((i / sr, (i + frame_len) / sr, is_speech))

    return _merge_flags(speech_flags, min_gap_ms / 1000, min_dur_ms / 1000)


def _merge_flags(
    flags: List[Tuple[float, float, bool]],
    min_gap: float,
    min_dur: float,
) -> List[Tuple[float, float]]:
    """연속 음성 프레임을 병합해 (start, end) 구간 목록 반환."""
    segments = []
    cur_start = None
    cur_end = None

    for t0, t1, is_speech in flags:
        if is_speech:
            if cur_start is None:
                cur_start, cur_end = t0, t1
            else:
                if t0 - cur_end <= min_gap:
                    cur_end = t1
                else:
                    if cur_end - cur_start >= min_dur:
                        segments.append((cur_start, cur_end))
                    cur_start, cur_end = t0, t1
        else:
            if cur_start is not None and t0 - cur_end > min_gap:
                if cur_end - cur_start >= min_dur:
                    segments.append((cur_start, cur_end))
                cur_start, cur_end = None, None

    if cur_start is not None and cur_end - cur_start >= min_dur:
        segments.append((cur_start, cur_end))

    return segments


# ------------------------------------------------------------------ #
#  DysarthriaProcessor                                                #
# ------------------------------------------------------------------ #


class DysarthriaProcessor:
    """구음장애 데이터 전처리기.

    데이터가 extracted 디렉토리 구조일 때도, zip 아카이브일 때도 동작.

    체크포인트 (중단/재시작 지원):
      output_dir/checkpoints/
      ├── dysarthria_done.txt       ← 처리 완료 key 목록
      └── dysarthria_records.jsonl  ← 처리 완료 record 전체 (재시작 복원)
    """

    def __init__(
        self,
        data_dir: str,
        output_dir: str,
        subset_hours: Optional[float] = None,
        seed: int = 42,
    ):
        self.data_dir = Path(data_dir)
        self.output_dir = Path(output_dir)
        self.subset_hours = subset_hours
        self.seed = seed

        self.audio_out = self.output_dir / "processed_audio" / "dysarthria"
        self.audio_out.mkdir(parents=True, exist_ok=True)

        ckpt_dir = self.output_dir / "checkpoints"
        ckpt_dir.mkdir(parents=True, exist_ok=True)

        self.ckpt_keys_file    = ckpt_dir / "dysarthria_done.txt"
        self.ckpt_records_file = ckpt_dir / "dysarthria_records.jsonl"
        self.ckpt_stems_file   = ckpt_dir / "dysarthria_done_stems.txt"

        self.done_keys: set  = self._load_done_keys()
        self.done_stems: set = self._load_done_stems()
        self.records: List[Dict] = self._load_records()
        self.skipped: List[Dict] = []
        self._interrupted = False

        if self.records:
            logger.info(
                f"체크포인트 복원: {len(self.records)}건 이전 결과 로드 "
                f"(완료 파일 {len(self.done_stems)}개 / 발화 key {len(self.done_keys)}개)"
            )

    # ------------------------------------------------------------------ #
    #  체크포인트                                                           #
    # ------------------------------------------------------------------ #

    def _load_done_keys(self) -> set:
        if self.ckpt_keys_file.exists():
            return set(self.ckpt_keys_file.read_text(encoding="utf-8").splitlines())
        return set()

    def _load_done_stems(self) -> set:
        if self.ckpt_stems_file.exists():
            return set(self.ckpt_stems_file.read_text(encoding="utf-8").splitlines())
        return set()

    def _load_records(self) -> List[Dict]:
        records = []
        if not self.ckpt_records_file.exists():
            return records
        with open(self.ckpt_records_file, encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line:
                    try:
                        records.append(json.loads(line))
                    except json.JSONDecodeError:
                        pass
        return records

    def _save_record(self, rec: Dict):
        with open(self.ckpt_keys_file, "a", encoding="utf-8") as f:
            f.write(rec["key"] + "\n")
        with open(self.ckpt_records_file, "a", encoding="utf-8") as f:
            f.write(json.dumps(rec, ensure_ascii=False) + "\n")
        self.done_keys.add(rec["key"])

    def _mark_stem_done(self, stem: str):
        """원본 파일 하나를 완전히 처리했을 때 파일명 기록 → 재시작 시 오디오 읽기 전 스킵."""
        with open(self.ckpt_stems_file, "a", encoding="utf-8") as f:
            f.write(stem + "\n")
        self.done_stems.add(stem)



    # ------------------------------------------------------------------ #

    def process(self) -> List[Dict]:
        if not self.data_dir.exists():
            logger.warning(f"구음장애 데이터 디렉토리 없음: {self.data_dir}")
            return []

        try:
            for subdir_name, type_code in SUBDIR_TO_TYPE.items():
                if self._interrupted:
                    break
                subdir = self.data_dir / subdir_name
                if not subdir.exists():
                    logger.warning(f"서브디렉토리 없음: {subdir}")
                    continue
                logger.info(f"구음장애 처리: {subdir_name} (Type {type_code})")
                try:
                    self._process_subdir(subdir, subdir_name, type_code)
                except KeyboardInterrupt:
                    self._interrupted = True
                    logger.warning("KeyboardInterrupt — 현재 subdir 처리 중단")
                    break
        except KeyboardInterrupt:
            self._interrupted = True

        if self._interrupted:
            logger.warning(
                f"\n{'='*50}\n"
                f"처리 중단됨. 체크포인트 저장 완료.\n"
                f"처리 완료: {len(self.records)}건\n"
                f"다음 실행 시 자동으로 이어서 처리합니다.\n"
                f"{'='*50}"
            )
        else:
            logger.info(
                f"구음장애 처리 완료: records={len(self.records)}, skipped={len(self.skipped)}"
            )
        return self.records

    # ------------------------------------------------------------------ #

    def _process_subdir(
        self,
        subdir: Path,
        subdir_name: str,
        type_code: str,
    ):
        audio_dir = subdir / "audio"
        label_dir = subdir / "label"

        if not audio_dir.exists() or not label_dir.exists():
            logger.warning(f"audio/label 디렉토리 없음: {subdir}")
            return

        json_files = sorted(label_dir.glob("*.json"))
        logger.info(f"  JSON 파일 {len(json_files)}개")

        pbar = tqdm(json_files, desc=f"dysarthria_{subdir_name}", unit="files", leave=True)
        try:
            for json_path in pbar:
                if self._interrupted:
                    break

                # ★ 파일 단위 스킵
                if json_path.stem in self.done_stems:
                    continue

                try:
                    with open(json_path, encoding="utf-8-sig") as f:
                        meta = json.load(f)

                    file_id = meta.get("File_id", json_path.stem + ".wav")
                    audio_path = audio_dir / file_id
                    if not audio_path.exists():
                        audio_path = audio_dir / (json_path.stem + ".wav")
                    if not audio_path.exists():
                        self.skipped.append({"key": json_path.stem, "reason": "audio_not_found"})
                        continue

                    audio, sr = sf.read(str(audio_path))
                    if audio.ndim > 1:
                        audio = audio[:, 0]

                    orig_sr = int(meta.get("Meta_info", {}).get("SamplingRate", sr))
                    if orig_sr != TARGET_SR:
                        audio = resample_audio(audio, orig_sr, TARGET_SR)
                        sr = TARGET_SR

                    recs = self._process_file(meta, audio, sr, type_code, subdir_name, json_path.stem)
                    for rec in recs:
                        if rec["key"] not in self.done_keys:
                            self.records.append(rec)
                            self._save_record(rec)

                    # ★ 파일 완료 표시
                    self._mark_stem_done(json_path.stem)

                    pbar.set_postfix({"done": len(self.records)})

                except KeyboardInterrupt:
                    self._interrupted = True
                    pbar.close()
                    raise
                except Exception as e:
                    logger.warning(f"파일 스킵 {json_path.stem}: {e}")
                    self.skipped.append({"key": json_path.stem, "reason": str(e)[:120]})
        except KeyboardInterrupt:
            pbar.close()
            raise

    # ------------------------------------------------------------------ #

    def _process_file(
        self,
        meta: Dict,
        audio: np.ndarray,
        sr: int,
        type_code: str,
        subdir_name: str,
        file_stem: str,
    ) -> List[Dict]:
        """문장 분리 + VAD 매핑 후 세그먼트 저장."""
        transcript = (meta.get("Transcript") or "").strip()
        if not transcript:
            return []

        sentences = split_sentences(transcript)
        if not sentences:
            return []

        patient = meta.get("Patient_info", {})
        disease = meta.get("Disease_info", {})

        # VAD로 음성 구간 추출
        vad_segs = vad_speech_segments(audio, sr)
        if not vad_segs:
            logger.warning(f"VAD 구간 없음: {file_stem}")
            return []

        recs = []
        vad_idx = 0

        for sent_idx, sentence in enumerate(sentences):
            if vad_idx >= len(vad_segs):
                break

            t0, t1 = vad_segs[vad_idx]
            dur = t1 - t0
            vad_idx += 1

            key = f"dys_{type_code}_{file_stem}_{sent_idx:04d}"
            if key in self.done_keys:
                continue

            if dur < 0.5 or dur > 30.0:
                self.skipped.append({"key": key, "reason": f"duration_{dur:.2f}s"})
                continue
            if len(sentence) < 2:
                self.skipped.append({"key": key, "reason": "text_too_short"})
                continue

            s0 = max(0, int(t0 * sr))
            s1 = min(len(audio), int(t1 * sr))
            segment = audio[s0:s1]

            out_path = self.audio_out / f"{key}.wav"
            sf.write(str(out_path), segment, sr)

            try:
                age = int(patient.get("Age", 0))
            except (ValueError, TypeError):
                age = 0

            recs.append(
                {
                    "key": key,
                    "wav": str(out_path.resolve()),
                    "text": sentence,
                    "standard": sentence,
                    "source": TYPE_TO_SOURCE.get(type_code, "dysarthria_unknown"),
                    "duration": round(dur, 4),
                    "speaker": file_stem,
                    "gender": patient.get("Sex", ""),
                    "birth_year": None,
                    "age": age,
                    "disease_type": type_code,
                    "subcategory": disease.get("Subcategory2", ""),
                    "area": patient.get("Area", ""),
                }
            )

        return recs

    # ------------------------------------------------------------------ #
    #  zip 모드 지원 (AI Hub 원본이 zip인 경우)                             #
    # ------------------------------------------------------------------ #

    def process_from_zip(
        self,
        audio_zip_path: str,
        label_zip_path: str,
        subdir_name: str,
        type_code: str,
    ) -> List[Dict]:
        """zip 아카이브에서 직접 처리 (다운로드 직후 구조에 대응)."""
        with zipfile.ZipFile(audio_zip_path, "r") as az, zipfile.ZipFile(
            label_zip_path, "r"
        ) as lz:
            audio_idx = {
                Path(n.lstrip("/")).stem: n
                for n in az.namelist()
                if n.lower().endswith(".wav")
            }
            json_names = sorted(n for n in lz.namelist() if n.lower().endswith(".json"))

            pbar = tqdm(json_names, desc=f"dysarthria_{subdir_name}", unit="files", leave=True)
            try:
                for jname in pbar:
                    if self._interrupted:
                        break

                    stem = Path(jname.lstrip("/")).stem

                    # ★ 파일 단위 스킵
                    if stem in self.done_stems:
                        continue

                    if stem not in audio_idx:
                        self.skipped.append({"key": stem, "reason": "no_audio_in_zip"})
                        continue

                    try:
                        with lz.open(jname) as f:
                            meta = json.loads(f.read().decode("utf-8-sig"))
                        with az.open(audio_idx[stem]) as f:
                            audio, sr = sf.read(io.BytesIO(f.read()))
                        if audio.ndim > 1:
                            audio = audio[:, 0]

                        orig_sr = int(meta.get("Meta_info", {}).get("SamplingRate", sr))
                        if orig_sr != TARGET_SR:
                            audio = resample_audio(audio, orig_sr, TARGET_SR)
                            sr = TARGET_SR

                        recs = self._process_file(meta, audio, sr, type_code, subdir_name, stem)
                        for rec in recs:
                            if rec["key"] not in self.done_keys:
                                self.records.append(rec)
                                self._save_record(rec)

                        # ★ 파일 완료 표시
                        self._mark_stem_done(stem)

                        pbar.set_postfix({"done": len(self.records)})

                    except KeyboardInterrupt:
                        self._interrupted = True
                        pbar.close()
                        raise
                    except Exception as e:
                        logger.warning(f"파일 스킵 {stem}: {e}")
                        self.skipped.append({"key": stem, "reason": str(e)[:120]})
            except KeyboardInterrupt:
                pbar.close()
                raise

        return self.records
