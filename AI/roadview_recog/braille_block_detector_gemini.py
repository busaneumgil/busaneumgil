"""
점자블록 자동 판별 시스템 - Gemini API 파이프라인
PoC 계획서 2.1: Gemini API 파이프라인 안정성 및 일관성 검증

이미지 구조:
  capture_image/
    1/
      angle_000.jpg ~ angle_324.jpg
    2/ ...
    3812/ ...

  reference_images/       ← Few-shot 참고 이미지 (선택사항)
    positive/             ← 점자블록 있는 예시 이미지
      sample_1.jpg
      sample_2.jpg  ...
    negative/             ← 점자블록 없는 예시 이미지
      sample_1.jpg
      sample_2.jpg  ...

.env 파일 설정:
  GEMINI_API_KEY=your_api_key_here
  IMAGE_DIR=./capture_image
  OUTPUT_DIR=./results
  REFERENCE_DIR=./reference_images   ← 추가 (없으면 Few-shot 미사용)

실행 방법:
  python braille_block_detector.py            # 전체 실행
  python braille_block_detector.py --limit 10 # 테스트
  python braille_block_detector.py --start_from 500  # 재개
"""

import os
import json
import time
import base64
import logging
import argparse
from pathlib import Path
from datetime import datetime

import requests
from dotenv import load_dotenv

load_dotenv()

# ── 로깅 설정 ────────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler("detection_run.log", encoding="utf-8"),
    ],
)
logger = logging.getLogger(__name__)

# ── 상수 ─────────────────────────────────────────────────────────────────────
GEMINI_API_URL = (
    "https://generativelanguage.googleapis.com/v1beta/models/"
    "gemini-2.5-flash:generateContent"
)
ANGLES = [0, 36, 72, 108, 144, 180, 216, 252, 288, 324]
ANGLE_FILENAMES = [f"angle_{a:03d}.png" for a in ANGLES]

MAX_RETRIES = 5
INITIAL_WAIT = 2.0
REQUEST_INTERVAL = 1.0

# ── 프롬프트 ──────────────────────────────────────────────────────────────────
SYSTEM_PROMPT = """당신은 도로 안전 시설물 분석 전문가입니다.
카카오 로드뷰 이미지를 분석하여 횡단보도 진입 구간의 점자블록 설치 여부를 판별합니다.

판별 기준:
- 점자블록: 보도 바닥면에 깔린 황색 블록 (선형 또는 점형)
- 볼라드, 차수막, 도로 차선은 점자블록이 아닙니다
- 10장의 이미지는 동일 위치를 36도 간격으로 촬영한 로드뷰입니다
- 횡단보도 흰색 줄무늬가 보이는 구간의 보도 진입부를 집중적으로 확인하세요
- 점자블록이 불확실한 경우 confidence를 low로 설정하세요"""

# Few-shot 미사용 시 프롬프트
USER_PROMPT_NO_REF = """아래는 동일한 횡단보도 위치를 36도 간격으로 촬영한 로드뷰 이미지 10장입니다.
모든 이미지를 종합적으로 분석하여 횡단보도 진입 구간의 점자블록 설치 여부를 판별하세요.

다음 JSON 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요:
{
  "braille_block_present": true or false,
  "confidence": "high" or "medium" or "low",
  "curb_cut_present": true or false,
  "detected_angles": [점자블록이 확인된 각도 목록, 없으면 빈 배열]
}"""

# Few-shot 사용 시 — 참고 이미지 뒤에 붙는 분석 요청 프롬프트
USER_PROMPT_WITH_REF = """위 예시 이미지들을 참고하여, 아래 분석 대상 이미지 10장을 판별하세요.

[분석 대상 이미지]
{angle_labels}

모든 이미지를 종합적으로 분석하여 횡단보도 진입 구간의 점자블록 설치 여부를 판별하세요.

다음 JSON 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요:
{{
  "braille_block_present": true or false,
  "confidence": "high" or "medium" or "low",
  "curb_cut_present": true or false,
  "detected_angles": [점자블록이 확인된 각도 목록, 없으면 빈 배열]
}}"""


# ── 참고 이미지 로드 ──────────────────────────────────────────────────────────
def load_reference_images(reference_dir: Path) -> dict:
    """
    reference_images/positive/, reference_images/negative/ 폴더에서
    참고 이미지를 로드합니다.
    반환: {"positive": [...], "negative": [...]}
    각 항목: {"filename": str, "data": base64str, "mime_type": str}
    """
    ref = {"positive": [], "negative": []}

    if not reference_dir.exists():
        return ref

    for label in ["positive", "negative"]:
        folder = reference_dir / label
        if not folder.exists():
            logger.warning(f"  참고 이미지 폴더 없음: {folder}")
            continue

        image_extensions = {".jpg", ".jpeg", ".png", ".webp"}
        files = sorted([f for f in folder.iterdir() if f.suffix.lower() in image_extensions])

        for filepath in files:
            with open(filepath, "rb") as f:
                encoded = base64.b64encode(f.read()).decode("utf-8")
            mime = "image/jpeg" if filepath.suffix.lower() in {".jpg", ".jpeg"} else f"image/{filepath.suffix[1:].lower()}"
            ref[label].append({
                "filename": filepath.name,
                "data": encoded,
                "mime_type": mime,
            })

    logger.info(f"  참고 이미지 로드: 있음 {len(ref['positive'])}장 / 없음 {len(ref['negative'])}장")
    return ref


# ── 이미지 로드 ───────────────────────────────────────────────────────────────
def load_images_as_base64(location_dir: Path) -> list[dict]:
    """위치 폴더에서 10장의 이미지를 base64로 로드합니다."""
    images = []
    for filename in ANGLE_FILENAMES:
        filepath = location_dir / filename
        if not filepath.exists():
            logger.warning(f"  이미지 없음: {filepath}")
            continue
        with open(filepath, "rb") as f:
            encoded = base64.b64encode(f.read()).decode("utf-8")
        images.append({
            "angle": int(filename.split("_")[1].split(".")[0]),
            "filename": filename,
            "data": encoded,
        })
    return images


# ── 요청 parts 구성 ───────────────────────────────────────────────────────────
def build_parts(images: list[dict], ref_images: dict) -> list:
    """
    Few-shot 참고 이미지가 있으면 앞에 배치하고,
    분석 대상 이미지 10장을 뒤에 배치합니다.
    """
    parts = []
    use_fewshot = ref_images["positive"] or ref_images["negative"]

    if use_fewshot:
        # ── 참고 이미지: 있음 예시 ──
        if ref_images["positive"]:
            parts.append({"text": "=== 참고 예시: 점자블록 있음 ==="})
            for img in ref_images["positive"]:
                parts.append({
                    "inline_data": {
                        "mime_type": img["mime_type"],
                        "data": img["data"],
                    }
                })
                parts.append({"text": f"[점자블록 있음 예시: {img['filename']}]"})

        # ── 참고 이미지: 없음 예시 ──
        if ref_images["negative"]:
            parts.append({"text": "=== 참고 예시: 점자블록 없음 ==="})
            for img in ref_images["negative"]:
                parts.append({
                    "inline_data": {
                        "mime_type": img["mime_type"],
                        "data": img["data"],
                    }
                })
                parts.append({"text": f"[점자블록 없음 예시: {img['filename']}]"})

        # ── 분석 대상 이미지 ──
        parts.append({"text": "=== 분석 대상 이미지 ==="})
        for img in images:
            parts.append({
                "inline_data": {
                    "mime_type": "image/jpeg",
                    "data": img["data"],
                }
            })
            parts.append({"text": f"[각도 {img['angle']}도]"})

        angle_labels = ", ".join([f"{img['angle']}도" for img in images])
        parts.append({"text": USER_PROMPT_WITH_REF.format(angle_labels=angle_labels)})

    else:
        # ── Few-shot 미사용: 기존 방식 ──
        for img in images:
            parts.append({
                "inline_data": {
                    "mime_type": "image/jpeg",
                    "data": img["data"],
                }
            })
            parts.append({"text": f"[각도 {img['angle']}도 이미지]"})
        parts.append({"text": USER_PROMPT_NO_REF})

    return parts


# ── Gemini API 호출 (재시도 포함) ──────────────────────────────────────────────
def call_gemini_api(api_key: str, images: list[dict], ref_images: dict) -> dict:
    """
    Gemini API를 호출합니다.
    Rate Limit(429) 또는 서버 에러(5xx) 시 지수 백오프로 재시도합니다.
    """
    parts = build_parts(images, ref_images)

    payload = {
        "system_instruction": {
            "parts": [{"text": SYSTEM_PROMPT}]
        },
        "contents": [
            {"role": "user", "parts": parts}
        ],
        "generationConfig": {
            "temperature": 0.1,
            "maxOutputTokens": 2048,
        },
    }

    headers = {
        "Content-Type": "application/json",
        "x-goog-api-key": api_key,
    }

    wait = INITIAL_WAIT
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            response = requests.post(
                GEMINI_API_URL,
                headers=headers,
                json=payload,
                timeout=90,
            )

            if response.status_code == 429 or response.status_code >= 500:
                logger.warning(
                    f"  HTTP {response.status_code} — {attempt}/{MAX_RETRIES}회 시도, "
                    f"{wait:.1f}초 후 재시도..."
                )
                time.sleep(wait)
                wait *= 2
                continue

            response.raise_for_status()
            data = response.json()

            raw_text = data["candidates"][0]["content"]["parts"][0]["text"]
            # 마크다운 코드블록 제거
            clean_text = (
                raw_text.strip()
                .removeprefix("```json")
                .removeprefix("```")
                .removesuffix("```")
                .strip()
            )
            result = json.loads(clean_text)
            return {"success": True, "result": result, "attempts": attempt}

        except requests.exceptions.Timeout:
            logger.warning(f"  타임아웃 — {attempt}/{MAX_RETRIES}회, {wait:.1f}초 후 재시도...")
            time.sleep(wait)
            wait *= 2

        except json.JSONDecodeError as e:
            logger.error(f"  JSON 파싱 실패: {e} | 응답: {raw_text[:200]}")
            return {"success": False, "error": f"JSON 파싱 실패: {e}", "attempts": attempt}

        except Exception as e:
            logger.error(f"  예외 발생: {e}")
            if attempt < MAX_RETRIES:
                time.sleep(wait)
                wait *= 2
            else:
                return {"success": False, "error": str(e), "attempts": attempt}

    return {"success": False, "error": "최대 재시도 횟수 초과", "attempts": MAX_RETRIES}


# ── 단일 위치 처리 ─────────────────────────────────────────────────────────────
def process_location(api_key: str, location_id: int, location_dir: Path, ref_images: dict) -> dict:
    """단일 위치(폴더)의 이미지 10장을 분석합니다."""
    logger.info(f"[{location_id:04d}] 처리 시작: {location_dir.name}")

    images = load_images_as_base64(location_dir)
    if len(images) == 0:
        logger.error(f"[{location_id:04d}] 이미지 없음 — 건너뜀")
        return {
            "location_id": location_id,
            "success": False,
            "error": "이미지 파일 없음",
            "image_count": 0,
        }

    if len(images) < 10:
        logger.warning(f"[{location_id:04d}] 이미지 {len(images)}/10장만 존재")

    api_result = call_gemini_api(api_key, images, ref_images)

    record = {
        "location_id": location_id,
        "image_count": len(images),
        "success": api_result["success"],
        "attempts": api_result.get("attempts", 0),
        "processed_at": datetime.now().isoformat(),
    }

    if api_result["success"]:
        record.update(api_result["result"])
        status = "✅ 있음" if api_result["result"].get("braille_block_present") else "❌ 없음"
        confidence = api_result["result"].get("confidence", "?")
        logger.info(f"[{location_id:04d}] 점자블록: {status} | 신뢰도: {confidence}")
    else:
        record["error"] = api_result.get("error", "알 수 없는 에러")
        logger.error(f"[{location_id:04d}] 실패: {record['error']}")

    return record


# ── 결과 저장 ─────────────────────────────────────────────────────────────────
def save_results(results: list[dict], output_dir: Path, run_id: str):
    """판별 결과를 JSON 파일로 저장합니다."""
    output_dir.mkdir(parents=True, exist_ok=True)

    all_results_path = output_dir / f"results_{run_id}.json"
    with open(all_results_path, "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=2)

    total = len(results)
    success_list = [r for r in results if r.get("success")]
    failed_list = [r for r in results if not r.get("success")]
    detected = [r for r in success_list if r.get("braille_block_present")]

    summary = {
        "run_id": run_id,
        "processed_at": datetime.now().isoformat(),
        "total_locations": total,
        "success_count": len(success_list),
        "failed_count": len(failed_list),
        "braille_block_detected": len(detected),
        "braille_block_not_detected": len(success_list) - len(detected),
        "detection_rate_pct": round(len(detected) / len(success_list) * 100, 1) if success_list else 0,
        "confidence_breakdown": {
            "high": sum(1 for r in success_list if r.get("confidence") == "high"),
            "medium": sum(1 for r in success_list if r.get("confidence") == "medium"),
            "low": sum(1 for r in success_list if r.get("confidence") == "low"),
        },
        "failed_location_ids": [r["location_id"] for r in failed_list],
    }

    summary_path = output_dir / f"summary_{run_id}.json"
    with open(summary_path, "w", encoding="utf-8") as f:
        json.dump(summary, f, ensure_ascii=False, indent=2)

    logger.info(f"\n{'='*50}")
    logger.info(f"📊 처리 완료 요약")
    logger.info(f"  전체: {total}개소")
    logger.info(f"  성공: {len(success_list)}개소 / 실패: {len(failed_list)}개소")
    logger.info(f"  점자블록 감지: {len(detected)}개소 ({summary['detection_rate_pct']}%)")
    logger.info(f"  결과 저장: {all_results_path}")
    logger.info(f"  요약 저장: {summary_path}")
    logger.info(f"{'='*50}\n")

    return all_results_path, summary_path


# ── 메인 ──────────────────────────────────────────────────────────────────────
def main():
    parser = argparse.ArgumentParser(description="점자블록 자동 판별 시스템 - Gemini API")
    parser.add_argument("--image_dir", type=str, default=None,
                        help="이미지 루트 디렉토리 (기본값: .env의 IMAGE_DIR)")
    parser.add_argument("--output_dir", type=str, default=None,
                        help="결과 저장 디렉토리 (기본값: .env의 OUTPUT_DIR)")
    parser.add_argument("--reference_dir", type=str, default=None,
                        help="참고 이미지 디렉토리 (기본값: .env의 REFERENCE_DIR)")
    parser.add_argument("--limit", type=int, default=None,
                        help="처리할 최대 위치 수 (테스트용, 기본값: 전체)")
    parser.add_argument("--start_from", type=int, default=1,
                        help="시작 위치 ID (중단 후 재개 시 사용, 기본값: 1)")
    args = parser.parse_args()

    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        logger.error("GEMINI_API_KEY가 설정되지 않았습니다. .env 파일을 확인하세요.")
        return

    image_dir = args.image_dir or os.environ.get("IMAGE_DIR", "./capture_image")
    output_dir_str = args.output_dir or os.environ.get("OUTPUT_DIR", "./results")
    reference_dir_str = args.reference_dir or os.environ.get("REFERENCE_DIR", "")

    image_root = Path(image_dir)
    output_dir = Path(output_dir_str)
    run_id = datetime.now().strftime("%Y%m%d_%H%M%S")

    if not image_root.exists():
        logger.error(f"이미지 디렉토리가 없습니다: {image_root.resolve()}")
        return

    # 참고 이미지 로드
    ref_images = {"positive": [], "negative": []}
    if reference_dir_str:
        reference_dir = Path(reference_dir_str)
        ref_images = load_reference_images(reference_dir)
        use_fewshot = bool(ref_images["positive"] or ref_images["negative"])
    else:
        use_fewshot = False

    # 위치 폴더 목록 수집
    location_dirs = sorted(
        [d for d in image_root.iterdir() if d.is_dir() and d.name.isdigit()],
        key=lambda d: int(d.name),
    )
    location_dirs = [d for d in location_dirs if int(d.name) >= args.start_from]
    if args.limit:
        location_dirs = location_dirs[:args.limit]

    total = len(location_dirs)
    logger.info(f"\n{'='*50}")
    logger.info(f"🚀 점자블록 판별 시작")
    logger.info(f"  처리 대상: {total}개소")
    logger.info(f"  이미지 경로: {image_root.resolve()}")
    logger.info(f"  결과 저장: {output_dir.resolve()}")
    logger.info(f"  Few-shot: {'✅ 사용 (있음 {}장 / 없음 {}장)'.format(len(ref_images['positive']), len(ref_images['negative'])) if use_fewshot else '❌ 미사용'}")
    logger.info(f"  실행 ID: {run_id}")
    logger.info(f"{'='*50}\n")

    results = []
    start_time = time.time()

    for idx, location_dir in enumerate(location_dirs, 1):
        location_id = int(location_dir.name)

        record = process_location(api_key, location_id, location_dir, ref_images)
        results.append(record)

        if idx % 100 == 0:
            save_results(results, output_dir, run_id)
            elapsed = time.time() - start_time
            avg_sec = elapsed / idx
            remaining = avg_sec * (total - idx)
            logger.info(
                f"  진행률: {idx}/{total} ({idx/total*100:.1f}%) | "
                f"평균: {avg_sec:.1f}초/위치 | "
                f"남은 시간: {remaining/60:.0f}분"
            )

        time.sleep(REQUEST_INTERVAL)

    save_results(results, output_dir, run_id)


if __name__ == "__main__":
    main()