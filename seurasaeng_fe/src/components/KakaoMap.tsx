import { useEffect, useRef, useState } from 'react';
import useWebSocket from '../hooks/useSocketReceive';
import { MOBILITY_API_KEY } from '../constants/env';
import { API } from '../constants/api';
import type { KakaoMapProps } from '../types/ComponentTypes';
import apiClient from '../libs/axios';

declare global {
  interface Window {
    kakao: any;
  }
}

// 아이티센 타워 위치
const ITCEN_TOWER_POSITION = {
  latitude: 37.4173,
  longitude: 126.9912,
};

// TODO: 상수 변수 어떻게 할지 
const START_MARKER_IMAGE = '/map-markers/start-marker.png';
const END_MARKER_IMAGE = '/map-markers/end-marker.png';
const BUS_MARKER_IMAGE_BLUE = '/map-markers/bus-marker-blue.png';
const BUS_MARKER_IMAGE_YELLOW = '/map-markers/bus-marker-orange.png';
const BUS_MARKER_IMAGE_RED = '/map-markers/bus-marker-red.png';

export default function KakaoMap({ route, activeTab }: KakaoMapProps) {

  // 카카오 맵을 띄울 HTML div 참조
  const mapRef = useRef<HTMLDivElement>(null);
  /* 카카오 맵 객체들 - map, polyline, markers, busMarker*/
  const [map, setMap] = useState<any>(null);
  const polylineRef = useRef<any>(null);
  const markerRefs = useRef<any[]>([]);
  const busMarkerRef = useRef<any>(null);
  /* 버스가 현재 운행 중인지를 나타내는 boolean */
  const [isBusOperating, setIsBusOperating] = useState(false); 
  /* 현재 탑승 인원을 저장 */
  const [currentCount, setCurrentCount] = useState<number>(0);
  /* 버스 정원 */
  const [maxCount] = useState<number>(45); 
  /* 버스 이미지 (탑승 인원에 따라 버스 색상이 다름) */
  const [busMarkerImage, setBusMarkerImage] = useState<string>(BUS_MARKER_IMAGE_BLUE);

   /* 실시간으로 수신하는 GPS 데이터 */
  const { gpsData } = useWebSocket(route ? route.id : null);

  // 지도 초기화 
  useEffect(() => {
    if (mapRef.current && window.kakao?.maps && !map) {
      const mapInstance = new window.kakao.maps.Map(mapRef.current, {
        center: new window.kakao.maps.LatLng(ITCEN_TOWER_POSITION.latitude, ITCEN_TOWER_POSITION.longitude),
        level: 5,
      });
      setMap(mapInstance);
    }
    console.log("[지도 초기화 완료]")
  }, [map]);

  // 출발지와 도착지 좌표 계산
  // 출근이면 출발지 : 노선 장소, 도착지 : 아이티센 타워
  // 퇴근이면 출발지: 아이티센 타워, 도착지: 노선 장소
  const getStartAndEndPoints = () => {
    if (!route) return null;

    const start = activeTab === '출근'
      ? { lat: route.latitude, lng: route.longitude }
      : { lat: ITCEN_TOWER_POSITION.latitude, lng: ITCEN_TOWER_POSITION.longitude };

    const end = activeTab === '출근'
      ? { lat: ITCEN_TOWER_POSITION.latitude, lng: ITCEN_TOWER_POSITION.longitude }
      : { lat: route.latitude, lng: route.longitude };
    
    console.log('[좌표 계산 완료] 출발지:', start, '도착지:', end);
    return { start, end };
  };

  // Mobility API로 경로 데이터 가져오는 함수
  const fetchRouteFromMobilityAPI = async (start: { lat: number; lng: number }, end: { lat: number; lng: number }) => {
    const url = new URL(API.mobility.baseUrl);
    url.searchParams.append('origin', `${start.lng},${start.lat}`);
    url.searchParams.append('destination', `${end.lng},${end.lat}`);
    url.searchParams.append('priority', 'RECOMMEND'); // 추천 경로
    url.searchParams.append('alternatives', 'false'); // 대안 경로 비활성화
    url.searchParams.append('road_details', 'false'); // 상세 도로 정보 비활성화

    const response = await fetch(url.toString(), {
      method: 'GET',
      headers: {
        'Authorization': `KakaoAK ${MOBILITY_API_KEY}`,
        'Content-Type': 'application/json', 
      },
    });

    if (!response.ok) {
      throw new Error('카카오 모빌리티 API 호출 실패');
    }

    return await response.json();
  };

  /** 지도에 마커와 노선 그리는 함수
   *  - start : 출발지 GPS 좌표 
   *  - end : 도착지 GPS 좌표 
   *  - vertexes : 노선(출발지 -> 도착지) GPS 좌표 배열
    */
  const drawRouteOnMap = (
  start: { lat: number; lng: number },
  end: { lat: number; lng: number },
  vertexes: number[]
) => {
  if (!map) return;

  // 기존 마커/폴리라인 삭제
  polylineRef.current?.setMap(null);
  polylineRef.current = null;
  console.log("[노선 변경으로 지도 초기화] 노선 제거 완료");

  markerRefs.current.forEach(marker => marker.setMap(null));
  markerRefs.current = [];
  console.log("[노선 변경으로 지도 초기화] 마커 제거 완료");

  busMarkerRef.current?.setMap(null);
  busMarkerRef.current = null;
  console.log("[노선 변경으로 지도 초기화] 버스 마커 제거 완료")

  // 경로 좌표 배열 만들기
  const path = [];
  for (let i = 0; i < vertexes.length; i += 2) {
    path.push(new window.kakao.maps.LatLng(vertexes[i + 1], vertexes[i]));
  }

  // 노선 생성
  const newPolyline = new window.kakao.maps.Polyline({
    path: path,
    strokeWeight: 5,
    strokeColor: '#1890ff',
    strokeOpacity: 0.8,
    strokeStyle: 'solid',
  });
  newPolyline.setMap(map);
  polylineRef.current = newPolyline;

// 출발지 마커 생성
const startMarker = new window.kakao.maps.Marker({
  position: new window.kakao.maps.LatLng(start.lat, start.lng),
  map: map,
  title: '출발지',
  image: new window.kakao.maps.MarkerImage(
    START_MARKER_IMAGE,
    new window.kakao.maps.Size(40, 40)
  )
});

// 도착지 마커 생성
const endMarker = new window.kakao.maps.Marker({
  position: new window.kakao.maps.LatLng(end.lat, end.lng),
  map: map,
  title: '도착지',
  image: new window.kakao.maps.MarkerImage(
    END_MARKER_IMAGE,
    new window.kakao.maps.Size(40, 40)
  )
});

  markerRefs.current = [startMarker, endMarker];

  // Bounds 설정 (경로 + 출발지/도착지 전부 포함)
  const bounds = new window.kakao.maps.LatLngBounds();
  
  // 경로 좌표 다 추가
  path.forEach(latlng => bounds.extend(latlng));

  // 출발지, 도착지 추가
  bounds.extend(new window.kakao.maps.LatLng(start.lat, start.lng));
  bounds.extend(new window.kakao.maps.LatLng(end.lat, end.lng));

  // 지도 범위 설정 + 패딩 줘서 깔끔하게
  map.setBounds(bounds, 50);
};

// ---------------------------- drawRouteOnMap 함수 끝끝끝끝끝끝끝끝끝끝끝끝

  /**
   *  route나 activeTab이 변경될 때 경로 다시 그림
   *  1) map이 새로 생성됐을 때 
   *  2) route가 바뀌었을 때 
   *  3) activeTab이 비뀌었을 때 
   *
   *  1. 출발/도착 좌표 계산 
   *  2. 지도에 선 그리기  
   * 
   */
  useEffect(() => {

    console.log('현재 선택된 route:', route);
    console.log('현재 활성화 탭:', activeTab);

    if (!map || !route) return;

    const updateMap = async () => {
      const points = getStartAndEndPoints(); // 출발/도착 좌표 계산
      if (!points) return;

      try {
        console.log('[API 요청] - 카카오 모빌리티 API | 출발지 : ', points.start, ' -> 도착지 : ', points.end);
        const data = await fetchRouteFromMobilityAPI(points.start, points.end);

        const vertexes = data.routes[0].sections[0].roads.flatMap((road: any) => road.vertexes);
        
        drawRouteOnMap(points.start, points.end, vertexes); // 지도에 선 그리기
      } catch (error) {
        console.error('[API 요청 실패] 경로 불러오기 실패:', error);
      }
    };

    updateMap();
  }, [map, route?.id, activeTab]);

  //   // 🚨 [추가] route(노선) 변경 시 기존 버스 마커 삭제
  // useEffect(() => {
  //   if (busMarker) {
  //     busMarker.setMap(null); // 지도에서 삭제
  //     setBusMarker(null);     // 상태 초기화
  //   }
  // }, [route]);
useEffect(() => {
    if (!map || !gpsData) return;

    const position = new window.kakao.maps.LatLng(gpsData.latitude, gpsData.longitude);

    if (!busMarkerRef.current) {
      const marker = new window.kakao.maps.Marker({
        position,
        map,
        image: new window.kakao.maps.MarkerImage(busMarkerImage, new window.kakao.maps.Size(40, 40)),
        title: '버스 위치',
      });
      marker.setMap(map);
      busMarkerRef.current = marker;
    } else {
      busMarkerRef.current.setPosition(position);
    }

    setIsBusOperating(true);
  }, [gpsData, map, busMarkerImage]);

  const fetchPassengerCount = async (shuttleId: string) => {
    try {
      const response = await apiClient.get(API.routes.count(shuttleId));
      setCurrentCount(response.data.count);
    } catch (error) {
      console.error('탑승 인원 API 오류:', error);
    }
  };

  useEffect(() => {
    if (!route?.id) return;

    let interval: any = null;

    if (isBusOperating) {
      fetchPassengerCount(String(route.id));
      interval = setInterval(() => {
        fetchPassengerCount(String(route.id));
      }, 2000);
    } else {
      setCurrentCount(0);
    }

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [isBusOperating, route]);

  const getBusImage = (count: number) => {
    if (count <= 15) return BUS_MARKER_IMAGE_BLUE;
    if (count <= 30) return BUS_MARKER_IMAGE_YELLOW;
    return BUS_MARKER_IMAGE_RED;
  };

  const getCountColor = (count: number) => {
    if (count <= 15) return 'text-blue-500';
    if (count <= 30) return 'text-yellow-500';
    return 'text-red-500';
  };

  useEffect(() => {
    if (!isBusOperating) return;

    const newImage = getBusImage(currentCount);
    setBusMarkerImage(newImage);

    if (busMarkerRef.current) {
      busMarkerRef.current.setImage(new window.kakao.maps.MarkerImage(
        newImage,
        new window.kakao.maps.Size(40, 40)
      ));
    }
  }, [currentCount]);

  return (
    <div>
      <div ref={mapRef} className="w-full h-64 rounded-lg shadow" />
      <div className="text-center mt-4 font-semibold">
        {isBusOperating
          ? <span className="text-green-600">셔틀버스 운행 중입니다.</span>
          : <span className="text-red-600">현재 운행 중이 아닙니다.</span>
        }
      </div>
      {isBusOperating && (
        <div className="w-full text-center text-base font-bold mb-2">
          현재 탑승인원 : <span className={getCountColor(currentCount)}>{currentCount}</span> / {maxCount}
        </div>
      )}
    </div>
  );
}