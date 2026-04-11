import asyncio
import os
from playwright.async_api import async_playwright

# ---- 설정 ----
HTML_URL = "http://localhost:8080/kakao_roadview.html"
OUTPUT_BASE = "capture_image"
ANGLES = list(range(0, 360, 36))  # 36도씩 회전 총 10장 캡처
TILT = 20      # 상하 각도 (양수 = 아래 방향, 도로가 보이게) → 확인 후 조정
ZOOM = 15      # 줌 레벨 (0이 기본, 양수=확대, 음수=축소) → 확인 후 조정
WAIT_AFTER_LOAD = 3000   # 로드뷰 초기 로딩 대기 (ms)
WAIT_AFTER_ROTATE = 1000 # 회전 후 렌더링 대기 (ms)
# --------------

async def capture_location(page, index: int):
    """한 위치에서 10방향 캡처"""
    folder = os.path.join(OUTPUT_BASE, str(index))
    os.makedirs(folder, exist_ok=True)

    for angle in ANGLES:
        # 로드뷰 viewpoint 설정
        await page.evaluate(f"""
            roadview.setViewpoint({{
                pan: {angle},
                tilt: {TILT},
                zoom: {ZOOM}
            }});
        """)
        await page.wait_for_timeout(WAIT_AFTER_ROTATE)

        # #roadview div 영역만 캡처
        roadview_el = page.locator("#roadview")
        screenshot_path = os.path.join(folder, f"angle_{angle:03d}.png")
        await roadview_el.screenshot(path=screenshot_path)
        print(f"  [{index}] pan={angle}° → {screenshot_path}")

async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page(viewport={"width": 1280, "height": 800})

        await page.goto(HTML_URL)
        await page.wait_for_load_state("networkidle")

        # 전체 좌표 개수 가져오기
        total = await page.evaluate("coords.length")
        print(f"총 {total}개 위치 처리 시작")

        for i in range(total):
            print(f"\n[{i+1}/{total}] 위치 로딩 중...")

            # 해당 인덱스로 이동 (currentIndex 변경 + loadRoadview 호출)
            await page.evaluate(f"""
                currentIndex = {i};
                loadRoadview({i});
            """)

            # 로드뷰 렌더링 대기
            await page.wait_for_timeout(WAIT_AFTER_LOAD)

            await capture_location(page, i + 1)  # 폴더명은 1부터

        await browser.close()
        print("\n✅ 모든 캡처 완료!")

if __name__ == "__main__":
    asyncio.run(main())