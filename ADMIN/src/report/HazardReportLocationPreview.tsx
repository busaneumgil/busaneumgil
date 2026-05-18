import { useEffect, useRef, useState } from "react";
import { loadKakaoMap, type KakaoMap } from "../map/kakaoLoader";
import type { GeoPoint } from "../types";

interface HazardReportLocationPreviewProps {
  point: GeoPoint;
  label: string;
}

export function HazardReportLocationPreview({ point, label }: HazardReportLocationPreviewProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<KakaoMap | null>(null);
  const [status, setStatus] = useState<"loading" | "ready" | "error">("loading");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let disposed = false;

    setStatus("loading");
    setErrorMessage(null);

    void loadKakaoMap()
      .then(() => {
        if (disposed || !containerRef.current || !window.kakao?.maps) return;

        if (!mapRef.current) {
          mapRef.current = new window.kakao.maps.Map(containerRef.current, {
            center: new window.kakao.maps.LatLng(point.lat, point.lng),
            level: 3,
            draggable: false,
            scrollwheel: false,
            disableDoubleClick: true,
            disableDoubleClickZoom: true,
            keyboardShortcuts: false,
          });
        }

        setStatus("ready");
      })
      .catch((error: unknown) => {
        if (disposed) return;
        setStatus("error");
        setErrorMessage(error instanceof Error ? error.message : "지도 미리보기를 준비하지 못했습니다.");
      });

    return () => {
      disposed = true;
    };
  }, []);

  useEffect(() => {
    if (!mapRef.current || !window.kakao?.maps) return;
    const center = new window.kakao.maps.LatLng(point.lat, point.lng);
    mapRef.current.setCenter(center);
    mapRef.current.setLevel(3);
    mapRef.current.relayout?.();
  }, [point.lat, point.lng]);

  return (
    <div className="hazard-location-map" role="img" aria-label={`${label} 지도 미리보기`}>
      <div ref={containerRef} className="hazard-location-map__canvas" />
      <div className="hazard-location-map__pin" aria-hidden="true" />
      {status !== "ready" && (
        <div className="hazard-location-map__overlay">
          <strong>{status === "loading" ? "위치를 불러오는 중입니다." : "지도를 표시하지 못했습니다."}</strong>
          <span>{status === "loading" ? "좌표를 기준으로 Kakao 지도를 준비하고 있습니다." : errorMessage ?? "잠시 후 다시 시도해주세요."}</span>
        </div>
      )}
    </div>
  );
}
