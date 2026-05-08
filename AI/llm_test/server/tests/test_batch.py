#!/usr/bin/env python3
"""
2단계 프롬프트 배치 테스터 (v3)

1단계: 모든 프롬프트(27개) × 대표 케이스(10개) × 3모델 → 상위 3개 선별
2단계: 선별된 프롬프트(3개) × 전체 케이스 × 3모델

Usage:
    python tests/test_batch.py --step all
    python tests/test_batch.py --step 1 --type place
    python tests/test_batch.py --step 2 --type confirm
    python tests/test_batch.py --step all --type all
"""
import os
import sys
import time
import json
import argparse
import importlib
import glob as glob_module
from datetime import datetime
from typing import Optional, List, Dict, Tuple

# Windows 콘솔 UTF-8 강제
if sys.stdout.encoding and sys.stdout.encoding.lower() != "utf-8":
    sys.stdout.reconfigure(encoding="utf-8")
if sys.stderr.encoding and sys.stderr.encoding.lower() != "utf-8":
    sys.stderr.reconfigure(encoding="utf-8")

SERVER_ROOT = os.path.join(os.path.dirname(__file__), "..")
sys.path.insert(0, SERVER_ROOT)

from dotenv import load_dotenv
load_dotenv(os.path.join(SERVER_ROOT, ".env"))

from providers.gemini_provider import GeminiProvider
from providers.claude_provider import ClaudeProvider
from providers.gpt_mini_provider import GPTMiniProvider
from providers.utils import parse_json_response

MODELS = ["gemini", "claude", "gpt"]
PROVIDER_MAP: Dict = {
    "gemini": GeminiProvider(),
    "claude": ClaudeProvider(),
    "gpt":    GPTMiniProvider(),
}

MODEL_DELAY   = 1     # 모델 간 딜레이 (초)
CASE_DELAY    = 2     # 케이스 간 딜레이 (초)
BACKOFF_WAITS = [10, 20, 40]  # 429 exponential backoff (초)

RESULTS_DIR = os.path.join(os.path.dirname(__file__), "test_results")

# ─────────────────────── 테스트 케이스 ────────────────────────

PLACE_TEST_CASES = [
    {"id":  1, "input": "부산역 어디야",        "type": "표준어",            "expected_intent": "place_search", "expected_place_name": "부산역"},
    {"id":  2, "input": "롯데마트 찾아줘",       "type": "표준어",            "expected_intent": "place_search", "expected_place_name": "롯데마트"},
    {"id":  3, "input": "근처 스타벅스 알려줘",  "type": "표준어+위치부사",   "expected_intent": "place_search", "expected_place_name": "스타벅스"},
    {"id":  4, "input": "주변 CGV 있어?",        "type": "표준어+위치부사",   "expected_intent": "place_search", "expected_place_name": "CGV"},
    {"id":  5, "input": "해운대 어디야",         "type": "표준어",            "expected_intent": "place_search", "expected_place_name": "해운대"},
    {"id":  6, "input": "서면역 찾아줘",         "type": "표준어",            "expected_intent": "place_search", "expected_place_name": "서면역"},
    {"id":  7, "input": "자갈치시장 어디야",     "type": "표준어",            "expected_intent": "place_search", "expected_place_name": "자갈치시장"},
    {"id":  8, "input": "근처 맥도날드 알려줘",  "type": "표준어+위치부사",   "expected_intent": "place_search", "expected_place_name": "맥도날드"},
    {"id":  9, "input": "부산대학교 찾아줘",     "type": "표준어",            "expected_intent": "place_search", "expected_place_name": "부산대학교"},
    {"id": 10, "input": "주변 이마트 어디야",    "type": "표준어+위치부사",   "expected_intent": "place_search", "expected_place_name": "이마트"},
    {"id": 11, "input": "해운대 어디노",         "type": "사투리유지",         "expected_intent": "place_search", "expected_place_name": "해운대"},
    {"id": 12, "input": "부산역 어디 있노",      "type": "사투리유지",         "expected_intent": "place_search", "expected_place_name": "부산역"},
    {"id": 13, "input": "서면 어디 인노",        "type": "왜곡변환",           "expected_intent": "place_search", "expected_place_name": "서면"},
    {"id": 14, "input": "롯데마트 찾아도",       "type": "사투리유지",         "expected_intent": "place_search", "expected_place_name": "롯데마트"},
    {"id": 15, "input": "스타벅스 알려도",       "type": "사투리유지",         "expected_intent": "place_search", "expected_place_name": "스타벅스"},
    {"id": 16, "input": "근처 롯데마트 알려도",  "type": "사투리유지+위치부사","expected_intent": "place_search", "expected_place_name": "롯데마트"},
    {"id": 17, "input": "부산항 어디 인노",      "type": "왜곡변환",           "expected_intent": "place_search", "expected_place_name": "부산항"},
    {"id": 18, "input": "사상역서 어디 있나예",  "type": "사투리유지",         "expected_intent": "place_search", "expected_place_name": "사상역"},
    {"id": 19, "input": "맥도날드 차자줘",       "type": "왜곡변환",           "expected_intent": "place_search", "expected_place_name": "맥도날드"},
    {"id": 20, "input": "자갈치시장 알려더",     "type": "왜곡변환",           "expected_intent": "place_search", "expected_place_name": "자갈치시장"},
    {"id": 21, "input": "부산역",                "type": "장소명단독",         "expected_intent": "place_search", "expected_place_name": "부산역"},
    {"id": 22, "input": "해운대",                "type": "장소명단독",         "expected_intent": "place_search", "expected_place_name": "해운대"},
    {"id": 23, "input": "스타벅스",              "type": "장소명단독",         "expected_intent": "place_search", "expected_place_name": "스타벅스"},
    {"id": 24, "input": "롯데마트",              "type": "장소명단독",         "expected_intent": "place_search", "expected_place_name": "롯데마트"},
    {"id": 25, "input": "서면",                  "type": "장소명단독",         "expected_intent": "place_search", "expected_place_name": "서면"},
    {"id": 26, "input": "자갈치시장",            "type": "장소명단독",         "expected_intent": "place_search", "expected_place_name": "자갈치시장"},
    {"id": 27, "input": "맥도날드",              "type": "장소명단독",         "expected_intent": "place_search", "expected_place_name": "맥도날드"},
    {"id": 28, "input": "CGV",                   "type": "장소명단독",         "expected_intent": "place_search", "expected_place_name": "CGV"},
    {"id": 29, "input": "사상역",                "type": "장소명단독",         "expected_intent": "place_search", "expected_place_name": "사상역"},
    {"id": 30, "input": "부산대학교",            "type": "장소명단독",         "expected_intent": "place_search", "expected_place_name": "부산대학교"},
    {"id": 31, "input": "빨리",                  "type": "단독부사",           "expected_intent": "unknown",      "expected_place_name": None},
    {"id": 32, "input": "찾아줘",                "type": "단독동사",           "expected_intent": "unknown",      "expected_place_name": None},
    {"id": 33, "input": "알려줘",                "type": "단독동사",           "expected_intent": "unknown",      "expected_place_name": None},
    {"id": 34, "input": "어디야",                "type": "단독의문",           "expected_intent": "unknown",      "expected_place_name": None},
    {"id": 35, "input": "어디 있노",             "type": "사투리+장소없음",    "expected_intent": "unknown",      "expected_place_name": None},
    {"id": 36, "input": "어디 인노",             "type": "왜곡+장소없음",      "expected_intent": "unknown",      "expected_place_name": None},
    {"id": 37, "input": "어대 있노",             "type": "왜곡+장소없음",      "expected_intent": "unknown",      "expected_place_name": None},
    {"id": 38, "input": "찾아 주이소",           "type": "사투리+장소없음",    "expected_intent": "unknown",      "expected_place_name": None},
    {"id": 39, "input": "차자 주이소",           "type": "왜곡+장소없음",      "expected_intent": "unknown",      "expected_place_name": None},
    {"id": 40, "input": "근처",                  "type": "단독위치부사",       "expected_intent": "unknown",      "expected_place_name": None},
]

PLACE_SAMPLE_IDS   = [1, 3, 11, 13, 16, 21, 25, 31, 35, 39]
PLACE_SAMPLE_CASES = [c for c in PLACE_TEST_CASES if c["id"] in PLACE_SAMPLE_IDS]

CONFIRM_TEST_CASES = [
    {"id":  1, "input": "응",            "type": "표준어 긍정",   "expected_confirmed": True,  "expected_message": "안내를 시작할게요"},
    {"id":  2, "input": "어",            "type": "표준어 긍정",   "expected_confirmed": True,  "expected_message": "안내를 시작할게요"},
    {"id":  3, "input": "네",            "type": "표준어 긍정",   "expected_confirmed": True,  "expected_message": "안내를 시작할게요"},
    {"id":  4, "input": "예",            "type": "표준어 긍정",   "expected_confirmed": True,  "expected_message": "안내를 시작할게요"},
    {"id":  5, "input": "맞아",          "type": "표준어 긍정",   "expected_confirmed": True,  "expected_message": "안내를 시작할게요"},
    {"id":  6, "input": "맞어",          "type": "표준어 긍정",   "expected_confirmed": True,  "expected_message": "안내를 시작할게요"},
    {"id":  7, "input": "그래",          "type": "표준어 긍정",   "expected_confirmed": True,  "expected_message": "안내를 시작할게요"},
    {"id":  8, "input": "좋아",          "type": "표준어 긍정",   "expected_confirmed": True,  "expected_message": "안내를 시작할게요"},
    {"id":  9, "input": "맞습니다",      "type": "표준어 긍정",   "expected_confirmed": True,  "expected_message": "안내를 시작할게요"},
    {"id": 10, "input": "그렇습니다",    "type": "표준어 긍정",   "expected_confirmed": True,  "expected_message": "안내를 시작할게요"},
    {"id": 11, "input": "맞지",          "type": "사투리 긍정",   "expected_confirmed": True,  "expected_message": "안내를 시작할게요"},
    {"id": 12, "input": "맞제",          "type": "사투리 긍정",   "expected_confirmed": True,  "expected_message": "안내를 시작할게요"},
    {"id": 13, "input": "응 맞아",       "type": "복합 긍정",     "expected_confirmed": True,  "expected_message": "안내를 시작할게요"},
    {"id": 14, "input": "어 맞아",       "type": "복합 긍정",     "expected_confirmed": True,  "expected_message": "안내를 시작할게요"},
    {"id": 15, "input": "예 맞습니더",   "type": "사투리 긍정",   "expected_confirmed": True,  "expected_message": "안내를 시작할게요"},
    {"id": 16, "input": "아니",          "type": "표준어 부정",   "expected_confirmed": False, "expected_message": "다시 말씀해 주세요"},
    {"id": 17, "input": "아니야",        "type": "표준어 부정",   "expected_confirmed": False, "expected_message": "다시 말씀해 주세요"},
    {"id": 18, "input": "아니요",        "type": "표준어 부정",   "expected_confirmed": False, "expected_message": "다시 말씀해 주세요"},
    {"id": 19, "input": "틀려",          "type": "표준어 부정",   "expected_confirmed": False, "expected_message": "다시 말씀해 주세요"},
    {"id": 20, "input": "틀렸어",        "type": "표준어 부정",   "expected_confirmed": False, "expected_message": "다시 말씀해 주세요"},
    {"id": 21, "input": "달라",          "type": "표준어 부정",   "expected_confirmed": False, "expected_message": "다시 말씀해 주세요"},
    {"id": 22, "input": "다른데",        "type": "표준어 부정",   "expected_confirmed": False, "expected_message": "다시 말씀해 주세요"},
    {"id": 23, "input": "다시",          "type": "재입력 요청",   "expected_confirmed": False, "expected_message": "다시 말씀해 주세요"},
    {"id": 24, "input": "아이다",        "type": "사투리 부정",   "expected_confirmed": False, "expected_message": "다시 말씀해 주세요"},
    {"id": 25, "input": "아이가",        "type": "사투리 부정",   "expected_confirmed": False, "expected_message": "다시 말씀해 주세요"},
    {"id": 26, "input": "다르다 아이가", "type": "사투리 부정",   "expected_confirmed": False, "expected_message": "다시 말씀해 주세요"},
    {"id": 27, "input": "아닌데예",      "type": "사투리 부정",   "expected_confirmed": False, "expected_message": "다시 말씀해 주세요"},
    {"id": 28, "input": "부산역",        "type": "장소명 재발화", "expected_confirmed": False, "expected_message": "다시 말씀해 주세요"},
    {"id": 29, "input": "음",            "type": "판단불가",      "expected_confirmed": False, "expected_message": "다시 말씀해 주세요"},
    {"id": 30, "input": "잠깐만",        "type": "판단불가",      "expected_confirmed": False, "expected_message": "다시 말씀해 주세요"},
]

CONFIRM_SAMPLE_IDS   = [1, 3, 5, 7, 11, 16, 18, 24, 28, 30]
CONFIRM_SAMPLE_CASES = [c for c in CONFIRM_TEST_CASES if c["id"] in CONFIRM_SAMPLE_IDS]

# ─────────────────────── 프롬프트 목록 ────────────────────────

PLACE_PROMPT_FILES = [
    ("claude_A",  "prompt.claude_A",  "SYSTEM_PROMPT"),
    ("claude_B",  "prompt.claude_B",  "SYSTEM_PROMPT"),
    ("claude_C",  "prompt.claude_C",  "SYSTEM_PROMPT"),
    ("gpt_A",     "prompt.gpt_A",     "SYSTEM_PROMPT"),
    ("gpt_B",     "prompt.gpt_B",     "SYSTEM_PROMPT"),
    ("gpt_C",     "prompt.gpt_C",     "SYSTEM_PROMPT"),
    ("gemini_A",  "prompt.gemini_A",  "SYSTEM_PROMPT"),
    ("gemini_B",  "prompt.gemini_B",  "SYSTEM_PROMPT"),
    ("gemini_C",  "prompt.gemini_C",  "SYSTEM_PROMPT"),
]

VISUALLY_PROMPT_FILES = [
    ("claude_visually_A", "prompt.claude_visually_A", "SYSTEM_PROMPT_VISUALLY"),
    ("claude_visually_B", "prompt.claude_visually_B", "SYSTEM_PROMPT_VISUALLY"),
    ("claude_visually_C", "prompt.claude_visually_C", "SYSTEM_PROMPT_VISUALLY"),
    ("gpt_visually_A",    "prompt.gpt_visually_A",    "SYSTEM_PROMPT_VISUALLY"),
    ("gpt_visually_B",    "prompt.gpt_visually_B",    "SYSTEM_PROMPT_VISUALLY"),
    ("gpt_visually_C",    "prompt.gpt_visually_C",    "SYSTEM_PROMPT_VISUALLY"),
    ("gemini_visually_A", "prompt.gemini_visually_A", "SYSTEM_PROMPT_VISUALLY"),
    ("gemini_visually_B", "prompt.gemini_visually_B", "SYSTEM_PROMPT_VISUALLY"),
    ("gemini_visually_C", "prompt.gemini_visually_C", "SYSTEM_PROMPT_VISUALLY"),
]

CONFIRM_PROMPT_FILES = [
    ("claude_confirm_A", "prompt.claude_confirm_A", "SYSTEM_PROMPT_CONFIRM"),
    ("claude_confirm_B", "prompt.claude_confirm_B", "SYSTEM_PROMPT_CONFIRM"),
    ("claude_confirm_C", "prompt.claude_confirm_C", "SYSTEM_PROMPT_CONFIRM"),
    ("gpt_confirm_A",    "prompt.gpt_confirm_A",    "SYSTEM_PROMPT_CONFIRM"),
    ("gpt_confirm_B",    "prompt.gpt_confirm_B",    "SYSTEM_PROMPT_CONFIRM"),
    ("gpt_confirm_C",    "prompt.gpt_confirm_C",    "SYSTEM_PROMPT_CONFIRM"),
    ("gemini_confirm_A", "prompt.gemini_confirm_A", "SYSTEM_PROMPT_CONFIRM"),
    ("gemini_confirm_B", "prompt.gemini_confirm_B", "SYSTEM_PROMPT_CONFIRM"),
    ("gemini_confirm_C", "prompt.gemini_confirm_C", "SYSTEM_PROMPT_CONFIRM"),
]

# ─────────────────────── 유틸 함수 ────────────────────────────

def get_prompt_files(test_type: str) -> list:
    return {"place": PLACE_PROMPT_FILES,
            "visually": VISUALLY_PROMPT_FILES,
            "confirm": CONFIRM_PROMPT_FILES}[test_type]

def get_sample_cases(test_type: str) -> list:
    return CONFIRM_SAMPLE_CASES if test_type == "confirm" else PLACE_SAMPLE_CASES

def get_full_cases(test_type: str) -> list:
    return CONFIRM_TEST_CASES if test_type == "confirm" else PLACE_TEST_CASES

def load_prompt(module_path: str, attr_name: str) -> Optional[str]:
    try:
        mod = importlib.import_module(module_path)
        return getattr(mod, attr_name)
    except ModuleNotFoundError:
        print(f"  ⚠️  {module_path} 파일 없음 - 스킵")
        return None
    except AttributeError:
        print(f"  ⚠️  {module_path}.{attr_name} 없음 - 스킵")
        return None

def call_with_backoff(provider, text: str, system_prompt: str):
    """Exponential backoff: 10→20→40s on 429"""
    result = provider.call(text, system_prompt=system_prompt)
    if result.error and ("429" in str(result.error) or "rate" in str(result.error).lower()):
        for wait in BACKOFF_WAITS:
            print(f"    ⏳ Rate limit 감지, {wait}초 후 재시도...")
            time.sleep(wait)
            result = provider.call(text, system_prompt=system_prompt)
            if not (result.error and ("429" in str(result.error) or "rate" in str(result.error).lower())):
                break
    return result

def check_place_pass(case: dict, actual_intent: str, actual_place_name: Optional[str]) -> bool:
    intent_ok = (actual_intent == case["expected_intent"])
    exp = case["expected_place_name"]
    if exp is None:
        place_ok = not actual_place_name
    else:
        place_ok = exp.lower() in (actual_place_name or "").lower()
    return intent_ok and place_ok

def check_confirm_pass(case: dict, actual_confirmed) -> bool:
    return actual_confirmed == case["expected_confirmed"]

def find_latest_step1() -> Optional[dict]:
    if not os.path.exists(RESULTS_DIR):
        return None
    files = sorted(glob_module.glob(os.path.join(RESULTS_DIR, "*_step1.json")), reverse=True)
    if not files:
        return None
    with open(files[0], "r", encoding="utf-8") as f:
        return json.load(f)

def select_top3(prompt_results: list) -> list:
    sorted_r = sorted(prompt_results, key=lambda x: x["total_combined"], reverse=True)
    return [pr["prompt_name"] for pr in sorted_r[:3]]

# ─────────────────────── 콘솔 출력 ────────────────────────────

def print_banner(msg: str):
    print("\n" + "=" * 50)
    print(msg)
    print("=" * 50 + "\n")

def print_step1_table(test_type: str, prompt_results: list, selected: list):
    n_cases = len(CONFIRM_SAMPLE_CASES) if test_type == "confirm" else len(PLACE_SAMPLE_CASES)
    n_models = len(MODELS)
    max_total = n_cases * n_models

    print(f"\n=== [1단계] {test_type} 예비 결과 ===")
    header = f"{'프롬프트':<22}| {'gemini':^8}| {'claude':^8}| {'gpt':^8}| {'합산':^8}"
    sep    = "-" * len(header)
    print(sep)
    print(header)
    print(sep)
    for pr in prompt_results:
        gm = pr["models"]["gemini"]
        cl = pr["models"]["claude"]
        gp = pr["models"]["gpt"]
        row = (f"{pr['prompt_name']:<22}"
               f"| {gm['correct']}/{gm['total']:>2}   "
               f"| {cl['correct']}/{cl['total']:>2}   "
               f"| {gp['correct']}/{gp['total']:>2}   "
               f"| {pr['total_combined']}/{max_total:>2}")
        print(row)
    print(sep)
    print(f"\n  --> 2단계 진출 (합산 상위 3개): {', '.join(selected)}\n")

def print_step2_table(test_type: str, prompt_results: list):
    n_cases = len(get_full_cases(test_type))

    print(f"\n=== [2단계] {test_type} 최종 결과 ===")
    header = (f"{'프롬프트':<24}"
              f"| {'gemini':^14}"
              f"| {'claude':^14}"
              f"| {'gpt':^14}")
    sub    = (f"{'':<24}"
              f"| {'전체':^6} {'정확도':^7}"
              f"| {'전체':^6} {'정확도':^7}"
              f"| {'전체':^6} {'정확도':^7}")
    sep    = "-" * len(header)
    print(sep)
    print(header)
    print(sub)
    print(sep)

    best_prompt, best_model, best_acc = "", "", 0.0
    for pr in prompt_results:
        cols = []
        for m in MODELS:
            ms = pr["models"][m]
            cols.append(f"{ms['correct']}/{ms['total']:>2}  {ms['accuracy']*100:>5.1f}%")
            if ms["accuracy"] > best_acc:
                best_acc = ms["accuracy"]
                best_prompt = pr["prompt_name"]
                best_model  = m
        print(f"{pr['prompt_name']:<24}| {cols[0]:<14}| {cols[1]:<14}| {cols[2]:<14}")
    print(sep)
    print(f"\n  🏆 최고 조합: {best_prompt} x {best_model} ({best_acc*100:.1f}%)\n")

# ─────────────────────── 핵심 테스트 실행 ─────────────────────

def run_prompt_test(prompt_name: str, prompt_text: str,
                    cases: list, test_type: str, phase: str) -> dict:
    """프롬프트 1개 × 전체 케이스 × 3모델 실행. 결과 dict 반환."""

    model_stats: Dict[str, Dict] = {
        m: {"correct": 0, "by_type": {}} for m in MODELS
    }
    cases_result = []

    for case in cases:
        input_text = case["input"]
        print(f"\n[{test_type}/{phase}] {prompt_name} | #{case['id']} {input_text}")

        case_model_results: Dict[str, dict] = {}

        for i, model_key in enumerate(MODELS):
            if i > 0:
                time.sleep(MODEL_DELAY)

            provider = PROVIDER_MAP[model_key]
            start_ms = int(time.time() * 1000)
            result   = call_with_backoff(provider, input_text, prompt_text)
            latency  = int(time.time() * 1000) - start_ms

            if test_type in ("place", "visually"):
                if result.error:
                    actual_intent  = "error"
                    actual_place   = None
                    confirm_msg    = None
                    passed         = False
                else:
                    actual_intent  = result.intent or "unknown"
                    actual_place   = result.place_name
                    confirm_msg    = result.confirmation_message
                    passed         = check_place_pass(case, actual_intent, actual_place)

                status = "✅" if passed else "❌"
                if case["expected_intent"] == "place_search":
                    print(f"  {model_key:<6} --> {status} {actual_intent} / {actual_place or 'null'} ({latency}ms)")
                else:
                    print(f"  {model_key:<6} --> {status} {actual_intent} ({latency}ms)")

                case_model_results[model_key] = {
                    "actual_intent":         actual_intent,
                    "actual_place_name":     actual_place,
                    "confirmation_message":  confirm_msg,
                    "pass":       passed,
                    "latency_ms": latency,
                    "error":      result.error,
                }

            else:  # confirm
                if result.error:
                    actual_confirmed = False
                    actual_message   = "다시 말씀해 주세요"
                    passed           = False
                else:
                    parsed           = parse_json_response(result.raw_text)
                    actual_confirmed = parsed.get("confirmed", False)
                    actual_message   = parsed.get("message", "다시 말씀해 주세요")
                    passed           = check_confirm_pass(case, actual_confirmed)

                status = "✅" if passed else "❌"
                print(f"  {model_key:<6} --> {status} {str(actual_confirmed).lower()} ({latency}ms)")

                case_model_results[model_key] = {
                    "actual_confirmed": actual_confirmed,
                    "actual_message":   actual_message,
                    "pass":       passed,
                    "latency_ms": latency,
                    "error":      result.error,
                }

            # 통계 갱신
            if passed:
                model_stats[model_key]["correct"] += 1
            t = case["type"]
            bt = model_stats[model_key]["by_type"]
            if t not in bt:
                bt[t] = {"total": 0, "correct": 0}
            bt[t]["total"] += 1
            if passed:
                bt[t]["correct"] += 1

        # 케이스 결과 누적
        case_entry: dict = {
            "id":    case["id"],
            "input": case["input"],
            "type":  case["type"],
            "results": case_model_results,
        }
        if test_type in ("place", "visually"):
            case_entry["expected_intent"]     = case["expected_intent"]
            case_entry["expected_place_name"] = case["expected_place_name"]
        else:
            case_entry["expected_confirmed"] = case["expected_confirmed"]
        cases_result.append(case_entry)

        time.sleep(CASE_DELAY)

    # 프롬프트 요약
    total = len(cases)
    models_result = {}
    total_combined = 0
    for m in MODELS:
        correct = model_stats[m]["correct"]
        total_combined += correct
        models_result[m] = {
            "total":    total,
            "correct":  correct,
            "accuracy": round(correct / total, 4),
            "by_type":  model_stats[m]["by_type"],
        }

    # 콘솔 요약
    print(f"\n  [{prompt_name}] 완료 --- "
          f"gemini {model_stats['gemini']['correct']}/{total} | "
          f"claude {model_stats['claude']['correct']}/{total} | "
          f"gpt {model_stats['gpt']['correct']}/{total} | "
          f"합산 {total_combined}/{total * 3}")

    return {
        "prompt_name":    prompt_name,
        "models":         models_result,
        "total_combined": total_combined,
        "cases":          cases_result,
    }

# ─────────────────────── 저장 함수 ────────────────────────────

def save_step1(results_by_type: dict, timestamp: str):
    os.makedirs(RESULTS_DIR, exist_ok=True)
    ts = datetime.strptime(timestamp, "%Y%m%d_%H%M%S")

    selected_map = {tt: select_top3(prs) for tt, prs in results_by_type.items()}

    json_data = {
        "step":       1,
        "timestamp":  ts.isoformat(),
        "results_by_type": {
            tt: {
                "prompts":            prs,
                "selected_for_step2": selected_map[tt],
            }
            for tt, prs in results_by_type.items()
        },
    }
    path = os.path.join(RESULTS_DIR, f"{timestamp}_step1.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(json_data, f, ensure_ascii=False, indent=2)

    # Markdown
    md_lines = [f"# 1단계 예비 테스트 결과\n",
                f"- 실행 시각: {ts.strftime('%Y-%m-%d %H:%M:%S')}\n"]
    for tt, prs in results_by_type.items():
        n = len(get_sample_cases(tt))
        md_lines.append(f"## {tt} (샘플 {n}개)\n")
        md_lines.append("| 프롬프트 | gemini | claude | gpt | 합산 |")
        md_lines.append("|---|---|---|---|---|")
        for pr in prs:
            gm = pr["models"]["gemini"]
            cl = pr["models"]["claude"]
            gp = pr["models"]["gpt"]
            md_lines.append(
                f"| {pr['prompt_name']} "
                f"| {gm['correct']}/{gm['total']} "
                f"| {cl['correct']}/{cl['total']} "
                f"| {gp['correct']}/{gp['total']} "
                f"| {pr['total_combined']}/{n*3} |"
            )
        md_lines.append(f"\n**2단계 진출**: {', '.join(selected_map[tt])}\n")

    md_path = os.path.join(RESULTS_DIR, f"{timestamp}_step1.md")
    with open(md_path, "w", encoding="utf-8") as f:
        f.write("\n".join(md_lines))

    rel = os.path.relpath(RESULTS_DIR, os.path.dirname(__file__))
    print(f"💾 {rel}/{timestamp}_step1.json")
    print(f"💾 {rel}/{timestamp}_step1.md")

def save_step2_type(test_type: str, prompt_results: list, timestamp: str):
    os.makedirs(RESULTS_DIR, exist_ok=True)
    ts = datetime.strptime(timestamp, "%Y%m%d_%H%M%S")

    # 최고 조합 찾기
    best_prompt, best_model, best_acc = "", "", 0.0
    for pr in prompt_results:
        for m in MODELS:
            acc = pr["models"][m]["accuracy"]
            if acc > best_acc:
                best_acc    = acc
                best_prompt = pr["prompt_name"]
                best_model  = m

    json_data = {
        "step":      2,
        "test_type": test_type,
        "timestamp": ts.isoformat(),
        "prompts":   prompt_results,
        "best": {
            "prompt":   best_prompt,
            "model":    best_model,
            "accuracy": round(best_acc, 4),
        },
    }
    json_path = os.path.join(RESULTS_DIR, f"{timestamp}_step2_{test_type}.json")
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(json_data, f, ensure_ascii=False, indent=2)

    # Markdown
    n_cases = len(get_full_cases(test_type))
    md_lines = [f"# 2단계 풀 테스트 결과 — {test_type}\n",
                f"- 실행 시각: {ts.strftime('%Y-%m-%d %H:%M:%S')}",
                f"- 전체 케이스 수: {n_cases}\n"]

    # 정확도 표
    md_lines.append("## 프롬프트별 × 모델별 정확도\n")
    md_lines.append("| 프롬프트 | gemini 정답/전체 | gemini 정확도 | claude 정답/전체 | claude 정확도 | gpt 정답/전체 | gpt 정확도 |")
    md_lines.append("|---|---|---|---|---|---|---|")
    for pr in prompt_results:
        row_parts = [f"| {pr['prompt_name']}"]
        for m in MODELS:
            ms = pr["models"][m]
            row_parts.append(f"{ms['correct']}/{ms['total']}")
            row_parts.append(f"{ms['accuracy']*100:.1f}%")
        md_lines.append(" | ".join(row_parts) + " |")
    md_lines.append("")

    # 유형별 정확도
    md_lines.append("## 유형별 정확도\n")
    for pr in prompt_results:
        md_lines.append(f"### {pr['prompt_name']}\n")
        for m in MODELS:
            md_lines.append(f"#### {m}\n")
            md_lines.append("| 유형 | 정답 | 전체 | 정확도 |")
            md_lines.append("|---|---|---|---|")
            for t, cnt in pr["models"][m]["by_type"].items():
                acc = cnt["correct"] / cnt["total"] * 100 if cnt["total"] else 0
                md_lines.append(f"| {t} | {cnt['correct']} | {cnt['total']} | {acc:.1f}% |")
            md_lines.append("")

    # 실패 케이스
    md_lines.append("## 실패 케이스\n")
    any_fail = False
    for pr in prompt_results:
        failed_cases = [c for c in pr["cases"]
                        if any(not c["results"][m]["pass"] for m in MODELS)]
        if not failed_cases:
            continue
        any_fail = True
        md_lines.append(f"### {pr['prompt_name']}\n")
        if test_type in ("place", "visually"):
            md_lines.append("| # | 입력 | 유형 | 기대intent | gemini결과 | claude결과 | gpt결과 |")
            md_lines.append("|---|---|---|---|---|---|---|")
            for c in failed_cases:
                results_cols = []
                for m in MODELS:
                    mr = c["results"][m]
                    if mr["pass"]:
                        results_cols.append("✅")
                    else:
                        results_cols.append(f"❌ {mr['actual_intent']}/{mr['actual_place_name'] or 'null'}")
                md_lines.append(
                    f"| {c['id']} | {c['input']} | {c['type']} "
                    f"| {c['expected_intent']} "
                    f"| {results_cols[0]} | {results_cols[1]} | {results_cols[2]} |"
                )
        else:
            md_lines.append("| # | 입력 | 유형 | 기대 | gemini | claude | gpt |")
            md_lines.append("|---|---|---|---|---|---|---|")
            for c in failed_cases:
                results_cols = []
                for m in MODELS:
                    mr = c["results"][m]
                    if mr["pass"]:
                        results_cols.append("✅")
                    else:
                        results_cols.append(f"❌ {mr['actual_confirmed']}")
                md_lines.append(
                    f"| {c['id']} | {c['input']} | {c['type']} "
                    f"| {c['expected_confirmed']} "
                    f"| {results_cols[0]} | {results_cols[1]} | {results_cols[2]} |"
                )
        md_lines.append("")
    if not any_fail:
        md_lines.append("모든 케이스 통과 ✅\n")

    md_lines.append(f"\n## 최고 조합\n**{best_prompt}** × **{best_model}** — {best_acc*100:.1f}%\n")

    md_path = os.path.join(RESULTS_DIR, f"{timestamp}_step2_{test_type}.md")
    with open(md_path, "w", encoding="utf-8") as f:
        f.write("\n".join(md_lines))

    rel = os.path.relpath(RESULTS_DIR, os.path.dirname(__file__))
    print(f"💾 {rel}/{timestamp}_step2_{test_type}.json")
    print(f"💾 {rel}/{timestamp}_step2_{test_type}.md")

# ─────────────────────── 메인 실행 흐름 ───────────────────────

def run_step1(types_to_run: list, timestamp: str) -> dict:
    """1단계: 모든 프롬프트 × 샘플 케이스 × 3모델"""
    print_banner("[1단계] 예비 테스트 시작")
    results_by_type = {}

    for tt in types_to_run:
        pf_list      = get_prompt_files(tt)
        sample_cases = get_sample_cases(tt)
        print(f"\n--- {tt} ({len(pf_list)}개 프롬프트 × {len(sample_cases)}케이스 × {len(MODELS)}모델) ---")

        tt_results = []
        for prompt_name, module_path, attr_name in pf_list:
            prompt_text = load_prompt(module_path, attr_name)
            if prompt_text is None:
                continue
            pr = run_prompt_test(prompt_name, prompt_text, sample_cases, tt, "예비")
            tt_results.append(pr)

        results_by_type[tt] = tt_results
        selected = select_top3(tt_results)
        print_step1_table(tt, tt_results, selected)

    save_step1(results_by_type, timestamp)
    return results_by_type

def run_step2(types_to_run: list, step1_data: dict, timestamp: str):
    """2단계: 선별된 상위 3개 프롬프트 × 전체 케이스 × 3모델"""
    # 선별 정보 추출
    selected_map = {}
    for tt in types_to_run:
        if tt in step1_data.get("results_by_type", {}):
            selected_map[tt] = step1_data["results_by_type"][tt]["selected_for_step2"]
        elif tt in step1_data:  # run_step1 에서 바로 넘어온 경우
            selected_map[tt] = select_top3(step1_data[tt])
        else:
            print(f"  ⚠️  {tt}: 1단계 결과 없음, 스킵")

    print_banner(f"[2단계] 풀 테스트 시작\n선택된 프롬프트: "
                 + " | ".join(f"{tt}: {', '.join(selected_map.get(tt, []))}"
                               for tt in types_to_run if tt in selected_map))

    for tt in types_to_run:
        if tt not in selected_map:
            continue

        selected  = selected_map[tt]
        full_cases = get_full_cases(tt)
        pf_dict   = {pf[0]: (pf[1], pf[2]) for pf in get_prompt_files(tt)}

        print(f"\n--- {tt} 풀 테스트 ({len(selected)}개 프롬프트 × {len(full_cases)}케이스) ---")

        tt_results = []
        for prompt_name in selected:
            if prompt_name not in pf_dict:
                print(f"  ⚠️  {prompt_name}: 프롬프트 파일 정보 없음, 스킵")
                continue
            module_path, attr_name = pf_dict[prompt_name]
            prompt_text = load_prompt(module_path, attr_name)
            if prompt_text is None:
                continue
            pr = run_prompt_test(prompt_name, prompt_text, full_cases, tt, "풀")
            tt_results.append(pr)

        print_step2_table(tt, tt_results)
        save_step2_type(tt, tt_results, timestamp)

def main():
    parser = argparse.ArgumentParser(description="2단계 프롬프트 배치 테스터 v3")
    parser.add_argument("--step", choices=["1", "2", "all"], default="all",
                        help="실행 단계 (기본: all)")
    parser.add_argument("--type", choices=["place", "visually", "confirm", "all"],
                        default="all", help="테스트 유형 (기본: all)")
    args = parser.parse_args()

    types_to_run = (["place", "visually", "confirm"]
                    if args.type == "all" else [args.type])
    timestamp    = datetime.now().strftime("%Y%m%d_%H%M%S")

    print(f"실행 단계: {args.step}  |  테스트 유형: {', '.join(types_to_run)}")
    print(f"모델: {', '.join(MODELS)}  |  타임스탬프: {timestamp}")

    if args.step == "1":
        run_step1(types_to_run, timestamp)

    elif args.step == "2":
        step1_data = find_latest_step1()
        if step1_data is None:
            print("❌ 1단계 결과 파일이 없습니다. --step all 또는 --step 1을 먼저 실행하세요.")
            sys.exit(1)
        print(f"1단계 결과 로드: {step1_data['timestamp']}")
        run_step2(types_to_run, step1_data, timestamp)

    else:  # all
        step1_results = run_step1(types_to_run, timestamp)
        # step2 타임스탬프는 step1과 동일하게 유지 (같은 실행 세션)
        run_step2(types_to_run, step1_results, timestamp)

if __name__ == "__main__":
    main()
