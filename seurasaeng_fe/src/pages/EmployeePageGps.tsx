import { useState, useEffect, useRef, useMemo } from 'react';
import type { RouteType, RoutesResponse } from '../types/RouteType';
import KakaoMap from '../components/KakaoMap';
import { loadKakaoMapSDK } from '../libs/loadKakaoMap';
import BottomBar from '../components/BottomBar';
import SlideTab from '../components/SlideTab';
import TopBar from '../components/TopBar';
import apiClient from '../libs/axios'; // 네 API 클라이언트 import
import {API} from '../constants/api'; // API 엔드포인트 상수
import CeniLoading from '../components/CeniLoading';

// 즐겨찾기 노선 타입
interface UserPreferences {
  favoritesWorkId?: number;
  favoritesHomeId?: number;
}

interface ApiRouteItem {
  id: number;
  commute: boolean;
  departureName: string;
  departureLatitude: number;
  departureLongitude: number;
  destinationName: string;
  destinationLatitude: number;
  destinationLongitude: number;
}

// 위치 정보 포함 노선 목록 조회 API 호출 함수
const fetchRouteData = async (): Promise<RoutesResponse> => {
  const response = await apiClient.get(API.routes.listWithLocation);
  const data: ApiRouteItem[] = response.data;

  // 출근/퇴근 분리
  const commuteRoutes = data.filter((item) => item.commute === true);
  const offworkRoutes = data.filter((item) => item.commute === false);

  // RoutesResponse 형태로 변환
  const formattedRoutes: RoutesResponse = {
    출근: commuteRoutes.map((item) => ({
      id: item.id,
      name: item.departureName,
      latitude: item.departureLatitude,
      longitude: item.departureLongitude,
    })),
    퇴근: offworkRoutes.map((item) => ({
      id: item.id,
      name: item.destinationName,
      latitude: item.destinationLatitude,
      longitude: item.destinationLongitude,
    })),
  };

  return formattedRoutes;
};

export default function EmployeeGPSApp() {
  const [activeTab, setActiveTab] = useState<RouteType>(() => {
    const hour = new Date().getHours();
    return hour < 12 ? '출근' : '퇴근';
  });
  const [isMapReady, setIsMapReady] = useState(false);
  const [routeData, setRouteData] = useState<RoutesResponse | null>(null);

  const locationTabRef = useRef<HTMLDivElement>(null);
  const selectedBtnRefs = useRef<(HTMLButtonElement | null)[]>([]);

  const [locationIdx, setLocationIdx] = useState(-1);
  const [userPrefs, setUserPrefs] = useState<UserPreferences | null>(null);

  useEffect(() => {
    const fetchAll = async () => {
      try {
        // 1. 즐겨찾기 노선 정보 요청
        const prefRes = await apiClient.get('/users/me/preferences');
        setUserPrefs(prefRes.data);
        // 2. 노선 데이터 요청
        const data = await fetchRouteData();
        setRouteData(data);
        // 즐겨찾기 거점 적용 (activeTab은 useState 초기값 사용)
        setLocationIdx(getFavoriteRouteIndex(activeTab, prefRes.data ?? undefined));
      } catch (err) {
        console.error('[API 호출 에러] Error fetching route data or preferences:', err);
      }
    };
    fetchAll();
  }, []);

  useEffect(() => {
    loadKakaoMapSDK(() => {
      setIsMapReady(true);
    }).catch((error) => {
      console.error('[Kakao Map 로드 실패] Kakao Maps SDK 로드 실패:', error);
    });
  }, []);

  const routes = routeData ? routeData[activeTab] : [];

  function getFavoriteRouteIndex(tab: RouteType, prefs?: UserPreferences) {
    if (!routeData) return 0;
    const favoriteId = tab === '출근' ? prefs?.favoritesWorkId : prefs?.favoritesHomeId;
    const routes = routeData[tab];
    if (favoriteId) {
      const index = routes.findIndex(route => route.id === favoriteId);
      return index !== -1 ? index : 0;
    }
    return 0;
  }

  useEffect(() => {
    if (routeData) {
      const idx = getFavoriteRouteIndex(activeTab, userPrefs ?? undefined);
      if (locationIdx !== idx) {
        setLocationIdx(idx);
      }
    }
    // locationIdx는 의존성에서 제외 (setState로 인한 무한루프 방지)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [routeData, activeTab, userPrefs]);

  const selectedRoute = routes[locationIdx] || null;

  const locations = useMemo(() => {
    if (!routeData) return [];
    const go = routeData["출근"].map((item) => item.name);
    const back = routeData["퇴근"].map((item) => item.name);
    return Array.from(new Set([...go, ...back]));
  }, [routeData]);

  useEffect(() => {
    const btn = selectedBtnRefs.current[locationIdx];
    if (btn && locationTabRef.current) {
      btn.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });
    }
  }, [locationIdx]);

  const handleTabClick = (tab: RouteType) => {
    setActiveTab(tab);
    if (routeData) {
      setLocationIdx(getFavoriteRouteIndex(tab, userPrefs ?? undefined));
    }
  };

  return (
    <div className="min-h-screen bg-[#fdfdfe] flex flex-col relative">
      <TopBar title="실시간 셔틀 확인" />
      {(!isMapReady) ? (
        <div className="absolute left-0 right-0 top-14 bottom-16 flex items-center justify-center z-40 bg-white bg-opacity-80">
          <CeniLoading />
        </div>
      ) : (
        <div className="flex-1 flex flex-col pb-24 min-h-0 relative">
          <div className="pt-16">
            <SlideTab
              locations={locations}
              locationIdx={locationIdx}
              onLocationChange={setLocationIdx}
              tab={activeTab}
              onTabChange={handleTabClick}
              className="w-full"
            />
          </div>
          <KakaoMap route={selectedRoute} activeTab={activeTab} />
        </div>
      )}
      <BottomBar />
    </div>
  );
}