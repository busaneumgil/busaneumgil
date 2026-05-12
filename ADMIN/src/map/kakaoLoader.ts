declare global {
  interface Window {
    kakao?: {
      maps: {
        load: (callback: () => void) => void;
        LatLng: new (lat: number, lng: number) => unknown;
        LatLngBounds: new () => { extend: (latLng: unknown) => void };
        Map: new (container: HTMLElement, options: Record<string, unknown>) => KakaoMap;
        Polyline: new (options: Record<string, unknown>) => KakaoOverlay;
        Polygon: new (options: Record<string, unknown>) => KakaoOverlay;
        Circle: new (options: Record<string, unknown>) => KakaoOverlay;
        Marker: new (options: Record<string, unknown>) => KakaoOverlay;
        CustomOverlay: new (options: Record<string, unknown>) => KakaoOverlay;
        Roadview?: new (container: HTMLElement) => KakaoRoadview;
        RoadviewClient?: new () => KakaoRoadviewClient;
        event: {
          addListener: (target: unknown, eventName: string, callback: (...args: unknown[]) => void) => void;
        };
      };
    };
  }
}

export interface KakaoMap {
  setCenter: (latLng: unknown) => void;
  setBounds?: (bounds: unknown) => void;
  getLevel?: () => number;
  setLevel: (level: number) => void;
}

export interface KakaoOverlay {
  setMap: (map: KakaoMap | null) => void;
}

export interface KakaoRoadview {
  setPanoId: (panoId: number | string, position: unknown) => void;
  relayout?: () => void;
  getViewpoint?: () => { pan?: number | string };
}

export interface KakaoRoadviewClient {
  getNearestPanoId: (position: unknown, radius: number, callback: (panoId: number | string | null) => void) => void;
}

let loadingPromise: Promise<void> | null = null;

export function loadKakaoMap(): Promise<void> {
  const appKey = import.meta.env.VITE_KAKAO_MAP_KEY as string | undefined;

  if (!appKey) {
    return Promise.reject(new Error("VITE_KAKAO_MAP_KEY가 없습니다."));
  }

  if (window.kakao?.maps) {
    return new Promise((resolve) => window.kakao?.maps.load(resolve));
  }

  if (loadingPromise) return loadingPromise;

  loadingPromise = new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.async = true;
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(appKey)}&autoload=false&libraries=services`;
    script.onload = () => {
      if (!window.kakao?.maps) {
        reject(new Error("Kakao Map SDK를 초기화할 수 없습니다."));
        return;
      }
      window.kakao.maps.load(resolve);
    };
    script.onerror = () => reject(new Error("Kakao Map SDK 로드 실패"));
    document.head.appendChild(script);
  });

  return loadingPromise;
}
