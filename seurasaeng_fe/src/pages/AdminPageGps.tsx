import { useState, useEffect, useRef, type ChangeEvent } from "react";
import { useGeoLocation } from "../hooks/useGeoLocation";
import { useSocket } from "../hooks/useSocketSend";
import { getKstTimestamp } from "../utils/getKstTimestamp";
import { API } from "../constants/api";
import type { GpsPayload } from "../types/GpsPayload";
import type { DropdownOption } from "../types/DropdownOption";
import apiClient from "../libs/axios";
import TopBar from '../components/TopBar';
import { useNavigate } from 'react-router-dom';


const ResponseToDropdownOptions = (data: any[]): DropdownOption[] => {
  const initialOption: DropdownOption = {
    value: "0",
    label: "선택해주세요.",
    departure: "",
    destination: "",
    is_commute: "",
  };

  const mappedOptions: DropdownOption[] = data.map((item) => ({
    value: item.id.toString(),
    label: `[${item.commute ? '출근' : '퇴근'}] ${item.departureName} → ${item.destinationName}`,
    departure: item.departureName,
    destination: item.destinationName,
    is_commute: item.commute ? "출근" : "퇴근",
  }));

  return [initialOption, ...mappedOptions];
};

function AdminPage() {
  const navigate = useNavigate();
  // 인터벌 ID 저장
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  // 마지막 수집된 GPS 데이터 저장 
  const latestGpsRef = useRef<GpsPayload | null>(null);
  // 선택된 노선 ID
  const [selectedValue, setSelectedValue] = useState<string>("0");
  // 운행 중 여부 
  const [isOperating, setIsOperating] = useState<boolean>(false);
  // 현재 상태 메시지
  const [operationMessage, setOperationMessage] = useState<string>("현재 운행 중이 아닙니다.");
  // 현재 GPS 정보
  const [currentGpsInfo, setCurrentGpsInfo] = useState<string | null>(null);

  // GPS 위치 수집 hook
  const { location, error, isLoading, fetchLocation } = useGeoLocation();

  const [options, setOptions] = useState<DropdownOption[]>([]);

  const handleMaxRetryExceeded = () => {
    console.log("재연결 실패로 운행 종료 처리");

    // GPS 수집 인터벌 정리
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }

    setIsOperating(false);
    setOperationMessage("네트워크 오류로 운행이 종료되었습니다. 다시 시작해주세요.");
  };

    // WebSocket 연결 hook
  const { stompClientRef, connectSocket, disconnectSocket } = useSocket(handleMaxRetryExceeded);

  useEffect(() => {
  const fetchRouteOptions = async () => {
    try {
      const response = await apiClient.get(API.routes.list);
      const data = response.data;
      const mappedOptions = ResponseToDropdownOptions(data);
      setOptions(mappedOptions);
    } catch (err) {
      console.error("노선 목록 가져오기 실패:", err);
    }
  };

    fetchRouteOptions();
  }, []);

  // 노선 선택 변경 핸들러
  const handleSelectChange = (event: ChangeEvent<HTMLSelectElement>) => {
    setSelectedValue(event.target.value);

    // 운행 중이 아니면 상태 초기화
    if (!isOperating) {
      setCurrentGpsInfo(null);
      setOperationMessage("현재 운행 중이 아닙니다.");
    }
  };

  // 운행 시작 버튼 핸들러
  const handleStartOperation = () => {
    if (window.confirm("GPS 정보를 사용하여 운행을 시작하시겠습니까?")) {
      connectSocket().then(() => {
        setIsOperating(true);
        setOperationMessage("GPS 정보 가져오는 중...");
        setCurrentGpsInfo(null);

        sessionStorage.setItem("isOperating", "true");
        sessionStorage.setItem("selectedValue", selectedValue);

        fetchLocation();
  
        if (intervalRef.current !== null) {
          clearInterval(intervalRef.current);
      }

        intervalRef.current = setInterval(() => {
          fetchLocation();
          console.log("[자동 GPS 요청] fetchLocation() 실행됨");
        }, 1000);
      })
      .catch((error) => {
        console.error("WebSocket 연결 실패:", error);
        setOperationMessage("네트워크 연결 실패: 페이지를 새로고침 해주세요.");
      });
    } else {
      setOperationMessage("운행이 취소되었습니다.");
      setIsOperating(false);
    }
  };

  useEffect(() => {
    const storedIsOperating = sessionStorage.getItem("isOperating");
    const storedSelectedValue = sessionStorage.getItem("selectedValue");

    if (storedIsOperating === "true" && storedSelectedValue) {
      setSelectedValue(storedSelectedValue);
      setIsOperating(true);
      setOperationMessage("GPS 정보 가져오는 중...");

      connectSocket().then(() => {
        fetchLocation();

        if (intervalRef.current !== null) {
        clearInterval(intervalRef.current);
      }

      intervalRef.current = setInterval(() => {
        fetchLocation();
        console.log("[자동 GPS 요청 - 복원됨] fetchLocation() 실행됨");
      }, 1000);
    }).catch((err) => {
      console.error("복원 중 WebSocket 연결 실패", err);
      setIsOperating(false);
      sessionStorage.removeItem("isOperating");
      sessionStorage.removeItem("selectedValue");
    });
  }
}, []);

  // 운행 종료 버튼 핸들러
const handleEndOperation = async () => {
    if (intervalRef.current !== null) {
    clearInterval(intervalRef.current);
    intervalRef.current = null;
    console.log("GPS 인터벌 제거 완료");
  }

  try {
    await apiClient.post(API.routes.endOperation(selectedValue));
    await apiClient.delete(API.routes.countDelete(selectedValue));
    console.log("운행 종료 및 인원 초기화 완료");
  } catch (err) {
    console.error("운행 종료 API 실패", err);
  }

  disconnectSocket();

  // 세션 제거
  sessionStorage.removeItem("isOperating");
  sessionStorage.removeItem("selectedValue");

  setIsOperating(false);
  setOperationMessage("운행이 종료되었습니다. 다시 시작하려면 노선 선택 후 운행 시작 버튼을 누르세요.");
  setCurrentGpsInfo(null);
};

  // QR 스캔 버튼 핸들러 (선택된 노선 id 전달)
  const handleQrScan = () => {
    if (selectedValue && selectedValue !== '0') {
      navigate('/qr-scan', { state: { shuttleId: selectedValue } });
    } else {
      alert('노선을 먼저 선택하세요.');
    }
  };

  useEffect(() => {
  // 1. 운행 중이 아니면 기본 메시지
  if (!isOperating && !isLoading && !location && !error) {
    if (operationMessage !== "현재 운행 중이 아닙니다.") {
      setOperationMessage("현재 운행 중이 아닙니다.");
    }
    setCurrentGpsInfo(null);
    return;
  }

  // 2. GPS 수집 중이면
  if (isLoading) {
    if (operationMessage !== "GPS 정보 가져오는 중...") {
      setOperationMessage("GPS 정보 가져오는 중...");
    }
    setCurrentGpsInfo("로딩 중...");
    return;
  }

  // 3. 에러가 발생했으면
  if (error) {
    const errMsg = `GPS 오류: ${error}`;
    if (operationMessage !== errMsg) {
      setOperationMessage(errMsg);
    }
    setCurrentGpsInfo(`오류: ${error}`);
    setIsOperating(false);
    return;
  }

  // 4. GPS 위치 정상 수집 완료
  if (location && isOperating) {
    const gpsPayload: GpsPayload = {
      routeId: selectedValue,
      type: "RUNNING",
      latitude: location.latitude,
      longitude: location.longitude,
      timestamp: getKstTimestamp(),
    };

    latestGpsRef.current = gpsPayload;

    if (stompClientRef.current?.connected) {
      stompClientRef.current.publish({
        destination: API.websocket.destination(selectedValue),
        body: JSON.stringify(gpsPayload),
      });
      console.log("소켓 전송됨:", gpsPayload);
    } else {
      console.warn("소켓 연결되지 않음");
    }

    // 🔄 메시지가 이미 '운행중'이 아니면 업데이트
    if (operationMessage !== "운행중") {
      setOperationMessage("운행중");
    }
    setCurrentGpsInfo(null);
  }
}, [location, error, isLoading, isOperating, selectedValue]);


  // 현재 선택된 노선 객체 가져옴
  const selectedOption = options.find(option => option.value === selectedValue);

  // 버튼 활성화 조건
  const isStartDisabled = isOperating || isLoading || !selectedValue || selectedValue === "0";
  const isEndDisabled = !isOperating;

  return (
    <div className="flex flex-col min-h-screen bg-[#fdfdfe] pb-16">
      <TopBar title="셔틀 운행 관리" showBackButton={true} />
      <div className="pt-20 px-4 w-full max-w-md mx-auto">
        {/* 운행 노선 선택 */}
        <div className="mb-4">
          <div className="text-sm font-medium mb-1 text-gray-700">운행 노선 선택</div>
          <select
            className="w-full border border-gray-300 rounded-lg px-3 py-3 focus:outline-none focus:ring-2 focus:ring-blue-200 text-sm bg-white"
            value={selectedValue}
            onChange={handleSelectChange}
            disabled={isOperating || isLoading}
          >
            {options.map(option => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
        </div>
        {/* 노선 정보 카드 */}
        {selectedOption && selectedValue !== "0" && (
          <div className="bg-gray-100 rounded-xl p-4 mb-4 flex flex-col gap-2">
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">출발지</span>
              <span className="font-semibold text-gray-800">{selectedOption.departure}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">도착지</span>
              <span className="font-semibold text-gray-800">{selectedOption.destination}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">출/퇴근</span>
              <span className="font-semibold text-gray-800">{selectedOption.is_commute}</span>
            </div>
          </div>
        )}
        {/* 운행 제어 버튼 및 안내 메시지 */}
        {selectedOption && selectedValue !== "0" ? (
          <div className="flex gap-2 justify-center mb-6">
            <button
              className="flex-1 py-2 rounded-lg font-semibold text-white bg-green-500 disabled:bg-green-200"
              onClick={handleStartOperation}
              disabled={isStartDisabled}
            >운행 시작</button>
            <button
              className="flex-1 py-2 rounded-lg font-semibold text-white bg-red-500 disabled:bg-red-200"
              onClick={handleEndOperation}
              disabled={isEndDisabled}
            >운행 종료</button>
            <button
              className="flex-1 py-2 rounded-lg font-semibold text-white bg-blue-500 disabled:bg-blue-200"
              onClick={handleQrScan}
              disabled={!isOperating}
            >QR 스캔</button>
          </div>
        ) : (
          <div className="text-center text-gray-400 mb-6">먼저 운행 노선을 선택해 주세요.</div>
        )}
        {/* 상태 박스 */}
        <div className="rounded-xl border border-gray-200 bg-white p-6 flex flex-col items-center justify-center min-h-[120px]">
          <div className={`text-2xl font-bold mb-2 ${isOperating ? 'text-green-500' : 'text-gray-400'}`}>{isOperating ? '운행중' : '운행대기'}</div>
          <div className="text-gray-600 text-center text-sm min-h-[24px]">{operationMessage}</div>
          {currentGpsInfo && (
            <div className="mt-3 w-full text-center text-xs text-gray-500 break-all">{currentGpsInfo}</div>
          )}
        </div>
      </div>
    </div>
  );
}

export default AdminPage;