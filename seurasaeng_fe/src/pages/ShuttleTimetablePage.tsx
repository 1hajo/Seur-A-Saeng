import { useState, useRef, useEffect } from 'react';
import apiClient from '../libs/axios';
import SlideTab from '../components/SlideTab';
import TopBar from '../components/TopBar';
import type { TimetableItem, ShuttleScheduleJson } from '../types/ShuttleTypes';

// 도착 시간 계산 함수
function getArrivalTime(departure: string, duration: string): string {
  // departure: '07:20', duration: '15분' 등
  const [h, m] = departure.split(":").map(Number);
  const min = parseInt(duration);
  if (isNaN(h) || isNaN(m) || isNaN(min)) return '';
  const date = new Date(2000, 0, 1, h, m + min);
  const hh = date.getHours().toString().padStart(2, '0');
  const mm = date.getMinutes().toString().padStart(2, '0');
  return `${hh}:${mm}`;
}

// 출발 시간이 현재 시간 이전인지 확인하는 함수
function isPastTime(departure: string) {
  const now = new Date();
  const [h, m] = departure.split(":").map(Number);
  if (isNaN(h) || isNaN(m)) return false;
  const dep = new Date(now.getFullYear(), now.getMonth(), now.getDate(), h, m);
  return dep < now;
}

// 시간 입력 핸들러 (숫자만, 자동 HH:MM 포맷)
function handleTimeInput(val: string, setValue: (v: string) => void) {
  // 숫자만 남기기
  let digits = val.replace(/\D/g, '');
  if (digits.length > 4) digits = digits.slice(0, 4);
  let formatted = digits;
  if (digits.length > 2) {
    formatted = digits.slice(0, 2) + ':' + digits.slice(2);
  }
  setValue(formatted);
}

interface ApiTimetableItem {
  shuttleId: number;
  spotName: string;
  totalSeats: string;
  duration: string;
  boardingPoint: string;
  timetables: { turn: string; departureTime: string }[];
}

type TimetableItemWithId = TimetableItem & { id: number };

function mapApiToTimetable(apiData: { commute: ApiTimetableItem[]; offwork: ApiTimetableItem[] }): { 출근: TimetableItemWithId[]; 퇴근: TimetableItemWithId[] } {
  function mapItem(item: ApiTimetableItem, isOffwork = false): TimetableItemWithId {
    return {
      id: item.shuttleId,
      거점: item.spotName,
      차량규격: item.totalSeats,
      소요시간: item.duration,
      승차장소: isOffwork ? undefined : item.boardingPoint,
      하차장소: isOffwork ? item.boardingPoint : undefined,
      출발시간: (item.timetables || []).map((t: { turn: string; departureTime: string }) => ({ [t.turn]: t.departureTime })),
    };
  }
  return {
    출근: (apiData.commute || []).map((item: ApiTimetableItem) => mapItem(item, false)),
    퇴근: (apiData.offwork || []).map((item: ApiTimetableItem) => mapItem(item, true)),
  };
}

// 즐겨찾기 노선 타입
interface UserPreferences {
  favoritesWorkId?: number;
  favoritesHomeId?: number;
}

export default function ShuttleTimetablePage({ isAdmin = false }) {
  // timetableData는 수정/삭제를 위해 state로 관리
  const [timetableData, setTimetableData] = useState<ShuttleScheduleJson | null>(null);
  const [locations, setLocations] = useState<string[]>([]);

  const [locationIdx, setLocationIdx] = useState(0);

  const locationTabRef = useRef<HTMLDivElement>(null);
  const selectedBtnRefs = useRef<(HTMLButtonElement | null)[]>([]);

  // --- 추가: 수정 모드 및 액션시트 상태 ---
  const [isEditMode, setIsEditMode] = useState(false);
  const [editIdx, setEditIdx] = useState<{itemIdx: number, timeIdx: number} | null>(null); // 현재 수정 중인 시간
  const [editValue, setEditValue] = useState('');
  // 액션 팝업 방식 복원
  const [openActionIdx, setOpenActionIdx] = useState<number | null>(null); // 어떤 시간의 액션시트가 열려있는지
  const actionSheetRef = useRef<HTMLDivElement | null>(null);

  // 수정 모드 취소/완료를 위한 원본 백업
  const [originalTimetableData, setOriginalTimetableData] = useState<ShuttleScheduleJson | null>(null);

  const [tab, setTab] = useState<'출근' | '퇴근'>("출근");

  // Filter timetable for selected location and type
  const timetable: TimetableItem[] = timetableData && timetableData[tab]
    ? timetableData[tab].filter((item) => item.거점 === locations[locationIdx])
    : [];

  // 화살표로 이동 시, 선택된 거점이 항상 보이도록 스크롤
  useEffect(() => {
    const btn = selectedBtnRefs.current[locationIdx];
    if (btn && locationTabRef.current) {
      btn.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });
    }
  }, [locationIdx]);

  // 액션시트 바깥 클릭 시 닫기
  useEffect(() => {
    if (openActionIdx === null) return;
    function handleClick(e: MouseEvent | TouchEvent) {
      if (actionSheetRef.current && !actionSheetRef.current.contains(e.target as Node)) {
        setOpenActionIdx(null);
      }
    }
    document.addEventListener('mousedown', handleClick);
    document.addEventListener('touchstart', handleClick);
    return () => {
      document.removeEventListener('mousedown', handleClick);
      document.removeEventListener('touchstart', handleClick);
    };
  }, [openActionIdx]);

  // 전체적으로 추가/수정 중인지 체크 (어떤 카드든 editIdx가 있으면 true)
  const isAnyCardEditing = Boolean(editIdx);

  // 현재 locationIdx(선택된 거점)의 카드에서만 추가/수정 인풋이 열려 있을 때
  const isCurrentLocationEditing = Boolean(editIdx && editIdx.itemIdx === locationIdx);

  useEffect(() => {
    if (isAdmin) {
      setIsEditMode(true);
      setOriginalTimetableData(JSON.parse(JSON.stringify(timetableData)));
    }
  // eslint-disable-next-line
  }, [isAdmin]);

  // 시간표 데이터 API로 받아오기
  useEffect(() => {
    const fetchPreferencesAndTimetables = async () => {
      try {
        // 1. 즐겨찾기 노선 정보 요청
        const prefRes = await apiClient.get('/users/me/preferences');
        const preferences: UserPreferences = prefRes.data;

        // 2. 시간표 데이터 요청
        const res = await apiClient.get('/timetables');
        const mapped = mapApiToTimetable(res.data);
        setTimetableData({ 출근: mapped.출근, 퇴근: mapped.퇴근 });
        if (isAdmin) {
          setOriginalTimetableData({ 출근: mapped.출근, 퇴근: mapped.퇴근 });
        }
        // 거점명 추출
        const go = mapped.출근.map(item => item.거점);
        const back = mapped.퇴근.map(item => item.거점);
        const allLocations = Array.from(new Set([...go, ...back]));
        setLocations(allLocations);

        // 3. 즐겨찾기 노선이 있으면 오전/오후에 따라 탭과 노선 선택
        const now = new Date();
        const hour = now.getHours();
        const initialTab: '출근' | '퇴근' = hour < 12 ? '출근' : '퇴근';
        let initialLocationIdx = 0;
        if (initialTab === '출근' && preferences.favoritesWorkId) {
          // 출근 노선에서 id가 일치하는 index 찾기
          const commuteIdx = (mapped.출근 as TimetableItemWithId[]).findIndex(item => item.id === preferences.favoritesWorkId);
          if (commuteIdx !== -1) {
            const commuteName = mapped.출근[commuteIdx].거점;
            const idx = allLocations.indexOf(commuteName);
            if (idx !== -1) initialLocationIdx = idx;
          }
        } else if (initialTab === '퇴근' && preferences.favoritesHomeId) {
          // 퇴근 노선에서 id가 일치하는 index 찾기
          const offworkIdx = (mapped.퇴근 as TimetableItemWithId[]).findIndex(item => item.id === preferences.favoritesHomeId);
          if (offworkIdx !== -1) {
            const offworkName = mapped.퇴근[offworkIdx].거점;
            const idx = allLocations.indexOf(offworkName);
            if (idx !== -1) initialLocationIdx = idx;
          }
        }
        setTab(initialTab);
        setLocationIdx(initialLocationIdx);
      } catch {
        // 실패 시 기존대로 동작
        try {
          const res = await apiClient.get('/timetables');
          const mapped = mapApiToTimetable(res.data);
          setTimetableData({ 출근: mapped.출근, 퇴근: mapped.퇴근 });
          if (isAdmin) {
            setOriginalTimetableData({ 출근: mapped.출근, 퇴근: mapped.퇴근 });
          }
          const go = mapped.출근.map(item => item.거점);
          const back = mapped.퇴근.map(item => item.거점);
          setLocations(Array.from(new Set([...go, ...back])));
        } catch (err2) {
          console.error('셔틀 시간표 조회 실패', err2);
        }
      }
    };
    fetchPreferencesAndTimetables();
  }, []);

  // timetableData와 originalTimetableData가 다를 때만 초기화 버튼 활성화
  const isResetEnabled = isEditMode && originalTimetableData && timetableData && JSON.stringify(timetableData) !== JSON.stringify(originalTimetableData);

  return (
    <div className="bg-white pb-16" style={{ minHeight: 'calc(var(--vh, 1vh) * 100)' }}>
      {/* 상단바 */}
      <TopBar 
        title={isAdmin ? "시간표 관리" : "셔틀 시간표"}
      />
      {/* SlideTab 컴포넌트로 대체 */}
      <div className="pt-16">
        <SlideTab
          locations={locations}
          locationIdx={locationIdx}
          onLocationChange={setLocationIdx}
          tab={tab}
          onTabChange={setTab}
          className="w-full"
        />
      </div>
      {/* 시간표 */}
      <div className="px-4">
        {timetable.length === 0 ? (
          <div className="text-center text-gray-400 py-8">해당 노선의 시간표가 없습니다.</div>
        ) : (
          timetable.map((item, idx) => (
            <div key={idx} className="w-full max-w-md bg-[#5382E0] rounded-xl text-white flex flex-col items-center mb-6 py-3 px-4 relative">
              {/* 노선 헤더 */}
              <div className="text-lg font-bold mb-1 text-center w-full">
                {locations[locationIdx]} → 아이티센 타워
              </div>
              {/* 승/하차 장소 안내 (카드 안쪽, 헤더 아래) */}
              <div className="text-xs text-blue-100 mb-8 w-full text-center">
                {tab === '퇴근' ? (
                  <>하차 장소: {item.하차장소}</>
                ) : (
                  <>승차 장소: {item.승차장소}</>
                )}
              </div>
              {/* 소요시간 */}
              <div className="text-base font-semibold mb-2 w-full text-center">소요시간: {item.소요시간}</div>
              {/* 출발/도착/소요 카드형 2열 그리드 */}
              <div className="grid grid-cols-2 gap-3 w-full max-w-md mt-2">
                {item.출발시간
                  .map((t, i) => {
                    const depTime = Object.values(t)[0];
                    const isPast = isPastTime(depTime);
                    // 수정 중인지 확인
                    const isEditing = editIdx && editIdx.itemIdx === idx && editIdx.timeIdx === i;
                    return (
                      <div
                        key={i}
                        className={
                          `rounded-xl flex flex-col items-center py-3 shadow w-full relative ` +
                          ((isPast && !isEditMode)
                            ? 'bg-gray-200 text-gray-400'
                            : 'bg-white text-[#5382E0]')
                        }
                      >
                        {isEditing ? (
                          <>
                            <input
                              className="text-xl font-bold font-mono border rounded px-2 py-1 w-20 text-center text-[#5382E0]"
                              value={editValue}
                              onChange={e => handleTimeInput(e.target.value, setEditValue)}
                              maxLength={5}
                              placeholder="00:00"
                              inputMode="numeric"
                              pattern="[0-9]*"
                              autoFocus
                            />
                            <div className="flex gap-2 mt-2">
                              <button
                                className={`px-2 py-1 text-xs rounded bg-white border border-[#5382E0] font-semibold ${/^\d{2}:\d{2}$/.test(editValue) ? 'text-[#5382E0]' : 'text-gray-300 border-gray-200 cursor-not-allowed'}`}
                                disabled={!/^\d{2}:\d{2}$/.test(editValue)}
                                onClick={() => {
                                  if (!/^\d{2}:\d{2}$/.test(editValue) || !editValue.trim()) {
                                    // 빈 값이거나 잘못된 값이면 해당 카드 삭제
                                    setTimetableData(prev => {
                                      const newData = JSON.parse(JSON.stringify(prev));
                                      const list = newData[tab].find((it: TimetableItem) => it.거점 === locations[locationIdx]);
                                      if (list) {
                                        const removeIdx = item.출발시간.findIndex(obj => Object.values(obj)[0] === '' || Object.values(obj)[0] === editValue);
                                        if (removeIdx !== -1) {
                                          list.출발시간.splice(removeIdx, 1);
                                        }
                                      }
                                      return newData;
                                    });
                                    setEditIdx(null);
                                    return;
                                  }
                                  setTimetableData(prev => {
                                    const newData = JSON.parse(JSON.stringify(prev));
                                    const list = newData[tab].find((it: TimetableItem) => it.거점 === locations[locationIdx]);
                                    if (list) {
                                      // 정렬 전 원본 인덱스 찾기
                                      const origIdx = item.출발시간.findIndex(obj => Object.values(obj)[0] === depTime || Object.values(obj)[0] === '');
                                      if (origIdx !== -1) {
                                        const key = Object.keys(item.출발시간[origIdx])[0];
                                        // 기존 key(회차)는 그대로, 값만 변경
                                        list.출발시간[origIdx][key] = editValue;
                                        // 정렬
                                        list.출발시간.sort((a: { [key: string]: string }, b: { [key: string]: string }) => {
                                          const tA = Object.values(a)[0] as string;
                                          const tB = Object.values(b)[0] as string;
                                          if (tA === "") return 1;
                                          if (tB === "") return -1;
                                          return tA.localeCompare(tB);
                                        });
                                      }
                                    }
                                    return newData;
                                  });
                                  setEditIdx(null);
                                }}
                              >확인</button>
                              <button
                                className="px-2 py-1 text-xs rounded bg-white border border-gray-300 text-gray-500"
                                onClick={() => {
                                  // 만약 depTime이 ''(빈 값)이면, 방금 추가한 임시 시간 삭제
                                  if (depTime === '') {
                                    setTimetableData(prev => {
                                      const newData = JSON.parse(JSON.stringify(prev));
                                      const list = newData[tab].find((it: TimetableItem) => it.거점 === locations[locationIdx]);
                                      if (list) {
                                        const removeIdx = item.출발시간.findIndex(obj => Object.values(obj)[0] === '');
                                        if (removeIdx !== -1) {
                                          list.출발시간.splice(removeIdx, 1);
                                        }
                                      }
                                      return newData;
                                    });
                                  }
                                  setEditIdx(null);
                                }}
                              >취소</button>
                            </div>
                          </>
                        ) : (
                          <>
                            <div className="text-xl font-bold font-mono">{depTime}</div>
                            <div className="text-xs mt-1">도착: {getArrivalTime(depTime, item.소요시간)}</div>
                            {/* --- 더보기(⋮) 버튼: 수정 모드일 때만 --- */}
                            {isEditMode && !isEditing && (
                              <button
                                className={
                                  "absolute top-0 right-2 p-1 rounded hover:bg-gray-100" +
                                  (isAnyCardEditing ? " opacity-50 cursor-not-allowed" : "")
                                }
                                disabled={isAnyCardEditing}
                                onClick={() => {
                                  if (isAnyCardEditing) return;
                                  setOpenActionIdx(i);
                                }}
                              >
                                <span className="text-2xl font-bold">⋮</span>
                              </button>
                            )}
                            {/* --- 액션시트(수정/삭제) --- */}
                            {isEditMode && openActionIdx === i && !isEditing && (
                              <div
                                ref={actionSheetRef}
                                className="absolute top-8 right-2 bg-white border rounded shadow z-30 flex flex-col min-w-[80px]"
                              >
                                <button
                                  className="px-3 py-2 text-sm text-[#5382E0] hover:bg-blue-50 text-left"
                                  onClick={() => {
                                    setEditIdx({itemIdx: idx, timeIdx: i});
                                    setEditValue(depTime);
                                    setOpenActionIdx(null);
                                  }}
                                >수정</button>
                                <button
                                  className="px-3 py-2 text-sm text-red-500 hover:bg-red-50 text-left"
                                  onClick={() => {
                                    setTimetableData(prev => {
                                      const newData = JSON.parse(JSON.stringify(prev));
                                      const list = newData[tab].find((it: TimetableItem) => it.거점 === locations[locationIdx]);
                                      if (list) {
                                        // 정렬 전 원본 인덱스 찾기
                                        const origIdx = item.출발시간.findIndex(obj => Object.values(obj)[0] === depTime);
                                        if (origIdx !== -1) {
                                          list.출발시간.splice(origIdx, 1);
                                        }
                                      }
                                      return newData;
                                    });
                                    setOpenActionIdx(null);
                                  }}
                                >삭제</button>
                              </div>
                            )}
                          </>
                        )}
                      </div>
                    );
                  })}
              </div>
              {/* 카드 바깥에 출발시간 추가 버튼 복원 (수정 모드에서만) */}
              {isEditMode && (() => {
                // 출발시간 추가 인풋 또는 기존 시간 카드 수정 중인지 체크
                const isCurrentLocationEditing = Boolean(editIdx && editIdx.itemIdx === idx);
                return (
                  <div className="mt-6 w-full flex flex-col items-center">
                    <button
                      className={
                        "px-3 py-2 rounded-full border border-[#5382E0] text-[#5382E0] bg-white text-sm font-semibold hover:bg-blue-50" +
                        (isCurrentLocationEditing ? " opacity-50 cursor-not-allowed" : "")
                      }
                      disabled={isCurrentLocationEditing}
                      onClick={() => {
                        if (isCurrentLocationEditing) return;
                        setTimetableData(prev => {
                          const newData = JSON.parse(JSON.stringify(prev));
                          const list = newData[tab].find((it: TimetableItem) => it.거점 === locations[locationIdx]);
                          if (list) {
                            const nextNum = list.출발시간.length + 1;
                            list.출발시간.push({ [`${nextNum}회`]: '' });
                          }
                          return newData;
                        });
                        setEditIdx({ itemIdx: idx, timeIdx: timetable[idx].출발시간.length });
                        setEditValue('');
                      }}
                    >+ 출발시간 추가</button>
                  </div>
                );
              })()}
            </div>
          ))
        )}
      </div>
      {/* 하단 고정 완료/취소 바 (수정 모드에서만) */}
      {isEditMode && (
        <div className="bottom-0 left-0 right-0 z-30 bg-white flex h-16 px-4 gap-3 items-center justify-center">
          <button
            className={
              "flex-1 h-11 rounded-lg border font-semibold text-base " +
              (isCurrentLocationEditing
                ? "bg-gray-200 border-gray-200 text-gray-400 cursor-not-allowed"
                : (!isResetEnabled
                    ? "border-gray-200 bg-gray-100 text-gray-400 cursor-not-allowed"
                    : "border-gray-300 bg-white text-gray-600"))
            }
            disabled={isCurrentLocationEditing || !isResetEnabled}
            onClick={() => {
              if (isCurrentLocationEditing) return;
              if (isResetEnabled && originalTimetableData) setTimetableData(originalTimetableData);
              // 수정모드는 유지
            }}
          >초기화</button>
          <button
            className={
              "flex-1 h-11 rounded-lg font-semibold text-base " +
              (isCurrentLocationEditing
                ? "bg-gray-200 text-gray-400 cursor-not-allowed"
                : (!isResetEnabled
                    ? "bg-gray-300 text-gray-400 cursor-not-allowed"
                    : "bg-[#5382E0] text-white"))
            }
            disabled={isCurrentLocationEditing || !isResetEnabled}
            onClick={async () => {
              if (isCurrentLocationEditing) return;
              if (!timetableData) return;
              // 현재 선택된 tab(출근/퇴근)과 locationIdx(거점)에 해당하는 시간표만 추출
              const selectedList = timetableData[tab];
              const selectedItem = selectedList.find(item => item.거점 === locations[locationIdx]);
              // 타입 가드 함수로 id가 있는지 확인
              function hasId(item: unknown): item is { id: number } {
                return typeof (item as { id?: unknown }).id === 'number';
              }
              if (!selectedItem || !hasId(selectedItem)) {
                alert('저장할 시간표를 찾을 수 없습니다.');
                return;
              }
              const payload = {
                shuttleId: selectedItem.id,
                timetables: selectedItem.출발시간.map(obj => {
                  const turn = Object.keys(obj)[0];
                  return { turn, departureTime: obj[turn] };
                })
              };
              try {
                await apiClient.put('/timetable', payload);
                // 수정 모드는 유지, 오리지널 시간표를 현재 값으로 갱신
                setOriginalTimetableData(JSON.parse(JSON.stringify(timetableData)));
              } catch {
                alert('시간표 저장에 실패했습니다.');
              }
            }}
          >수정 완료</button>
        </div>
      )}
    </div>
  );
}
