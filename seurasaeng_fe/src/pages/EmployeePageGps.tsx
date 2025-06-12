import { useState, useEffect, useRef, useMemo } from 'react';
import type { RouteType, RoutesResponse } from '../types/RouteType';
import KakaoMap from '../components/KakaoMap';
import { loadKakaoMapSDK } from '../libs/loadKakaoMap';
import BottomBar from '../components/BottomBar';
import SlideTab from '../components/SlideTab';
import TopBar from '../components/TopBar';
import apiClient from '../libs/axios'; // 네 API 클라이언트 import
import {API} from '../constants/api'; // API 엔드포인트 상수

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
  const [activeTab, setActiveTab] = useState<RouteType>('출근');
  const [isMapReady, setIsMapReady] = useState(false);
  const [routeData, setRouteData] = useState<RoutesResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const locationTabRef = useRef<HTMLDivElement>(null);
  const selectedBtnRefs = useRef<(HTMLButtonElement | null)[]>([]);

  const [locationIdx, setLocationIdx] = useState(0);
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
        // 3. 오전/오후에 따라 탭/노선 자동 선택
        const now = new Date();
        const hour = now.getHours();
        const initialTab: RouteType = hour < 12 ? '출근' : '퇴근';
        setActiveTab(initialTab);
        setLocationIdx(getFavoriteRouteIndex(initialTab, prefRes.data));
      } catch (err) {
        console.error('[API 호출 에러] Error fetching route data or preferences:', err);
      } finally {
        setLoading(false);
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
      setLocationIdx(getFavoriteRouteIndex(activeTab, userPrefs));
    }
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
      setLocationIdx(getFavoriteRouteIndex(tab, userPrefs));
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        로딩 중...
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#fdfdfe] flex flex-col relative">
      <TopBar title="실시간 셔틀 확인" />
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
      <div className="flex-1 flex flex-col items-center justify-start pb-24 w-full max-w-xl mx-auto px-4">
        <div className="w-full aspect-square overflow-hidden shadow mb-6 bg-gray-100 flex items-center justify-center">
          {isMapReady && selectedRoute ? (
            <KakaoMap route={selectedRoute} activeTab={activeTab} />
          ) : (
            <div>지도를 불러오는 중입니다...</div>
          )}
        </div>
      </div>
      <BottomBar />
    </div>
  );
}