"""
gyeongsang_processor.py
경상도 방언 데이터(AI Hub) 전처리

입력 데이터 위치 (data_dir 아래 구조):
  data_dir/               ← --gyeongsang_dir 로 지정
  ├── Training/
  │   ├── 01.원천데이터/   ← TS_02. 경상도_*.zip
  │   └── 02.라벨링데이터/ ← TL_02. 경상도_*.zip
  └── Validation/
      ├── 01.원천데이터/   ← VS_02. 경상도_*.zip
      └── 02.라벨링데이터/ ← VL_02. 경상도_*.zip

체크포인트 (중단/재시작 지원):
  output_dir/checkpoints/
  ├── gyeongsang_done.txt       ← 처리 완료 key 목록 (빠른 중복 검사용)
  └── gyeongsang_records.jsonl  ← 처리 완료 record 전체 (재시작 시 복원)
"""

import io
import json
import logging
import signal
import zipfile
from pathlib import Path
from typing import Dict, List, Optional, Tuple

import numpy as np
import soundfile as sf
from tqdm import tqdm

logger = logging.getLogger(__name__)


def parse_time(time_str: str) -> float:
    """'HH:MM:SS.mmm' 형식을 초(float)로 변환."""
    h, m, s = time_str.split(":")
    return int(h) * 3600 + int(m) * 60 + float(s)


class GyeongsangProcessor:
    # subset_hours 모드에서 유형별 목표 시간
    SUBSET_HOURS_BY_TYPE = {"st": 5.0, "say": 5.0, "talk": 4.0}

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

        self.audio_out = self.output_dir / "processed_audio" / "gyeongsang"
        self.audio_out.mkdir(parents=True, exist_ok=True)

        ckpt_dir = self.output_dir / "checkpoints"
        ckpt_dir.mkdir(parents=True, exist_ok=True)

        # 체크포인트 파일 2종
        self.ckpt_keys_file = ckpt_dir / "gyeongsang_done.txt"        # key 집합 (빠른 중복 검사)
        self.ckpt_records_file = ckpt_dir / "gyeongsang_records.jsonl"  # record 전체 (재시작 복원)

        # 이전 결과 로드
        self.done_keys: set = self._load_done_keys()
        self.records: List[Dict] = self._load_records()
        self.skipped: List[Dict] = []

        if self.records:
            logger.info(
                f"체크포인트 복원: {len(self.records)}건 이전 결과 로드 "
                f"(done_keys={len(self.done_keys)})"
            )

        self._interrupted = False

    # ------------------------------------------------------------------ #
    #  체크포인트 로드                                                       #
    # ------------------------------------------------------------------ #

    def _load_done_keys(self) -> set:
        if self.ckpt_keys_file.exists():
            return set(self.ckpt_keys_file.read_text(encoding="utf-8").splitlines())
        return set()

    def _load_records(self) -> List[Dict]:
        """이전 세션에서 처리된 records를 JSONL 체크포인트에서 복원."""
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

    # ------------------------------------------------------------------ #
    #  체크포인트 저장 (record 단위 즉시 기록)                               #
    # ------------------------------------------------------------------ #

    def _save_record(self, rec: Dict):
        """처리된 record를 두 체크포인트 파일에 즉시 기록."""
        with open(self.ckpt_keys_file, "a", encoding="utf-8") as f:
            f.write(rec["key"] + "\n")
        with open(self.ckpt_records_file, "a", encoding="utf-8") as f:
            f.write(json.dumps(rec, ensure_ascii=False) + "\n")
        self.done_keys.add(rec["key"])

    # ------------------------------------------------------------------ #
    #  hours_done 계산 (재시작 시 이전 진행량 복원)                          #
    # ------------------------------------------------------------------ #

    def _calc_hours_done(self) -> Dict[str, float]:
        """현재 records에서 유형별 처리 시간 계산."""
        hours: Dict[str, float] = {"st": 0.0, "say": 0.0, "talk": 0.0}
        for r in self.records:
            src = r.get("source", "")
            # "gyeongsang_st" → "st", "gyeongsang_talk" → "talk"
            dtype = src.replace("gyeongsang_", "")
            if dtype in hours:
                hours[dtype] += r.get("duration", 0.0) / 3600
        return hours

    # ------------------------------------------------------------------ #
    #  zip 파일 탐색                                                        #
    # ------------------------------------------------------------------ #

    def _zip_pairs(self) -> List[Tuple[Path, Path, str]]:
        """경상도 방언 (audio_zip, label_zip, type) 쌍 목록 반환.

        data_dir 바로 아래 Training/ 과 Validation/ 을 탐색.
        """
        type_tags = {
            "01. 1인발화 따라말하기": "st",
            "02. 1인발화 질문에답하기": "say",
            "03. 2인발화": "talk",
        }
        pairs = []
        for split in ("Training", "Validation"):
            audio_dir = self.data_dir / split / "01.원천데이터"
            label_dir = self.data_dir / split / "02.라벨링데이터"
            if not audio_dir.exists():
                logger.warning(f"디렉토리 없음 (건너뜀): {audio_dir}")
                continue

            for audio_zip in sorted(audio_dir.glob("*경상도*.zip")):
                for tag, dtype in type_tags.items():
                    if tag not in audio_zip.name:
                        continue
                    # 오디오 prefix → 라벨 prefix 변환: TS_→TL_, VS_→VL_
                    label_name = audio_zip.name
                    for src, dst in (("TS_", "TL_"), ("VS_", "VL_")):
                        if label_name.startswith(src):
                            label_name = dst + label_name[3:]
                            break
                    label_zip = label_dir / label_name
                    if label_zip.exists():
                        pairs.append((audio_zip, label_zip, dtype))
                        logger.info(f"zip 쌍: {dtype} | {audio_zip.name}")
                    else:
                        logger.warning(f"라벨 zip 없음: {label_zip}")
                    break
        return pairs

    # ------------------------------------------------------------------ #
    #  메인 처리 (KeyboardInterrupt 안전 종료)                              #
    # ------------------------------------------------------------------ #

    def process(self) -> List[Dict]:
        """전체 경상도 데이터를 처리한다.

        Ctrl+C 등으로 중단하면 현재까지의 결과를 보존하고 종료.
        다음 실행 시 체크포인트에서 자동으로 재개.
        """
        pairs = self._zip_pairs()
        logger.info(f"경상도 zip 쌍 {len(pairs)}개 발견")

        # 이전 진행량 복원 (재시작 시 subset 한도 계산에 반영)
        hours_done = self._calc_hours_done()
        limits: Dict[str, float] = {}
        if self.subset_hours is not None:
            limits = self.SUBSET_HOURS_BY_TYPE
            logger.info(
                f"서브셋 모드 재개: 이전 진행량 "
                f"st={hours_done['st']:.2f}h / say={hours_done['say']:.2f}h / talk={hours_done['talk']:.2f}h"
            )

        try:
            for audio_zip_path, label_zip_path, dtype in pairs:
                if self._interrupted:
                    break
                if limits and hours_done.get(dtype, 0.0) >= limits.get(dtype, float("inf")):
                    logger.info(f"서브셋 한도 도달: {dtype} ({hours_done[dtype]:.2f}h)")
                    continue
                logger.info(f"처리 시작: {dtype} | {label_zip_path.name}")
                try:
                    self._process_pair(audio_zip_path, label_zip_path, dtype, hours_done, limits)
                except KeyboardInterrupt:
                    self._interrupted = True
                    logger.warning("KeyboardInterrupt — 현재 zip 처리 중단, 이후 zip 건너뜀")
                    break
                except Exception as e:
                    logger.error(f"zip 처리 실패 {label_zip_path.name}: {e}", exc_info=True)

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
                f"경상도 처리 완료: records={len(self.records)}, skipped={len(self.skipped)}"
            )
        return self.records

    # ------------------------------------------------------------------ #
    #  zip 쌍 처리                                                          #
    # ------------------------------------------------------------------ #

    def _audio_index(self, audio_zip: zipfile.ZipFile) -> Dict[str, str]:
        """zip 내 stem → member 경로 매핑 생성."""
        return {
            Path(n.lstrip("/")).stem: n
            for n in audio_zip.namelist()
            if n.lower().endswith(".wav")
        }

    def _process_pair(
        self,
        audio_path: Path,
        label_path: Path,
        dtype: str,
        hours_done: Dict[str, float],
        limits: Dict[str, float],
    ):
        with zipfile.ZipFile(audio_path, "r") as az, zipfile.ZipFile(label_path, "r") as lz:
            audio_idx = self._audio_index(az)
            json_names = sorted(n for n in lz.namelist() if n.lower().endswith(".json"))

            pbar = tqdm(json_names, desc=f"gyeongsang_{dtype}", unit="files", leave=True)
            try:
                for jname in pbar:
                    if self._interrupted:
                        break
                    if limits and hours_done.get(dtype, 0.0) >= limits.get(dtype, float("inf")):
                        pbar.close()
                        break

                    stem = Path(jname.lstrip("/")).stem

                    if stem not in audio_idx:
                        self.skipped.append({"key": stem, "reason": "no_audio_in_zip"})
                        continue

                    try:
                        with lz.open(jname) as f:
                            meta = json.loads(f.read().decode("utf-8-sig"))
                        with az.open(audio_idx[stem]) as f:
                            audio, sr = sf.read(io.BytesIO(f.read()))

                        if dtype in ("st", "say"):
                            recs = self._process_sentences(meta, audio, sr, dtype, stem)
                        else:
                            recs = self._process_segments(meta, audio, sr, stem)

                        for rec in recs:
                            if rec["key"] not in self.done_keys:
                                self.records.append(rec)
                                self._save_record(rec)
                                hours_done[dtype] = hours_done.get(dtype, 0.0) + rec["duration"] / 3600

                        pbar.set_postfix(
                            {
                                "done": len(self.records),
                                f"{dtype}h": f"{hours_done.get(dtype,0):.2f}",
                            }
                        )

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

    # ------------------------------------------------------------------ #
    #  st_ / say_ 처리 (sentences 단위)                                    #
    # ------------------------------------------------------------------ #

    def _process_sentences(
        self, meta: Dict, audio: np.ndarray, sr: int, dtype: str, stem: str
    ) -> List[Dict]:
        recs = []
        spk_list = meta.get("speaker") or [{}]
        spk = spk_list[0] if spk_list else {}
        speaker_id = spk.get("speakerId", "unknown")
        sentences = meta.get("transcription", {}).get("sentences", [])

        for idx, sent in enumerate(sentences):
            start = parse_time(sent.get("startTime", "00:00:00.000"))
            end = parse_time(sent.get("endTime", "00:00:00.000"))
            dur = end - start

            dialect = (sent.get("dialect") or "").strip()
            standard = (sent.get("standard") or "").strip()
            # stem이 이미 'st_...' / 'say_...' 로 시작하므로 dtype prefix 불필요
            sent_id = int(sent["sentenceId"]) if "sentenceId" in sent else idx + 1
            key = f"{stem}_{sent_id:04d}"

            # 이미 처리된 key 스킵
            if key in self.done_keys:
                continue

            if dur < 0.5 or dur > 30.0:
                self.skipped.append({"key": key, "reason": f"duration_{dur:.2f}s"})
                continue
            if len(dialect) < 2:
                self.skipped.append({"key": key, "reason": "text_too_short"})
                continue

            s0 = max(0, int(start * sr))
            s1 = min(len(audio), int(end * sr))
            out_path = self.audio_out / f"{key}.wav"
            sf.write(str(out_path), audio[s0:s1], sr)

            recs.append(
                {
                    "key": key,
                    "wav": str(out_path.resolve()),
                    "text": dialect,
                    "standard": standard,
                    "source": f"gyeongsang_{dtype}",
                    "duration": round(dur, 4),
                    "speaker": speaker_id,
                    "gender": spk.get("gender", ""),
                    "birth_year": spk.get("birthYear"),
                }
            )
        return recs

    # ------------------------------------------------------------------ #
    #  talk_ 처리 (segments 단위 + 인접 병합)                               #
    # ------------------------------------------------------------------ #

    def _process_segments(
        self, meta: Dict, audio: np.ndarray, sr: int, stem: str
    ) -> List[Dict]:
        recs = []
        raw_segs = [
            s
            for s in meta.get("transcription", {}).get("segments", [])
            if s.get("voiceType") == "voice_speech"
        ]
        if not raw_segs:
            return recs

        raw_segs = sorted(raw_segs, key=lambda s: parse_time(s.get("startTime", "00:00:00")))

        # gap < 0.3초이면 병합
        merged: List[Dict] = []
        cur = None
        for s in raw_segs:
            t0 = parse_time(s["startTime"])
            t1 = parse_time(s["endTime"])
            txt = (s.get("dialect") or "").strip()
            if cur is None:
                cur = {"start": t0, "end": t1, "text": txt}
            elif t0 - cur["end"] < 0.3:
                cur["end"] = t1
                if txt:
                    cur["text"] = (cur["text"] + " " + txt).strip()
            else:
                merged.append(cur)
                cur = {"start": t0, "end": t1, "text": txt}
        if cur:
            merged.append(cur)

        # 30초 초과 병합 구간은 원래 segment 단위로 분해
        final: List[Dict] = []
        for m in merged:
            if m["end"] - m["start"] > 30.0:
                for s in raw_segs:
                    t0 = parse_time(s["startTime"])
                    t1 = parse_time(s["endTime"])
                    if m["start"] <= t0 and t1 <= m["end"]:
                        final.append({"start": t0, "end": t1, "text": (s.get("dialect") or "").strip()})
            else:
                final.append(m)

        for idx, seg in enumerate(final):
            dur = seg["end"] - seg["start"]
            txt = seg["text"]
            key = f"talk_{stem}_{idx:04d}"

            if key in self.done_keys:
                continue
            if dur < 0.5 or dur > 30.0:
                self.skipped.append({"key": key, "reason": f"duration_{dur:.2f}s"})
                continue
            if len(txt) < 2:
                self.skipped.append({"key": key, "reason": "text_too_short"})
                continue

            s0 = max(0, int(seg["start"] * sr))
            s1 = min(len(audio), int(seg["end"] * sr))
            out_path = self.audio_out / f"{key}.wav"
            sf.write(str(out_path), audio[s0:s1], sr)

            recs.append(
                {
                    "key": key,
                    "wav": str(out_path.resolve()),
                    "text": txt,
                    "standard": "",
                    "source": "gyeongsang_talk",
                    "duration": round(dur, 4),
                    "speaker": stem,
                    "gender": "",
                    "birth_year": None,
                }
            )
        return recs
