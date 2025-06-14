import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import TopBar from '../components/TopBar';
import BottomBar from '../components/BottomBar';
import apiClient from '../libs/axios';
import CeniLoading from '../components/CeniLoading';

export default function NoticePage({ isAdmin = false }) {
  const navigate = useNavigate();
  type NoticeType = {
    id: number;
    title: string;
    content: string | null;
    created_at: string;
    popup: boolean;
  };
  const [notices, setNotices] = useState<NoticeType[]>([]);
  const [draggedId, setDraggedId] = useState<number|null>(null);
  const [dragXMap, setDragXMap] = useState<{[id:number]:number}>({});
  const startXRef = useRef(0);
  const [startY, setStartY] = useState(0);
  const [isScrolling, setIsScrolling] = useState(false);
  const [loading, setLoading] = useState(true);
  const [popupSettingId, setPopupSettingId] = useState<number|null>(null);
  const [popupNoticeId, setPopupNoticeId] = useState<number|null>(null);

  // 버튼 관련 상수 (중복 제거)
  const BUTTON_WIDTH = 80; // px, w-20
  const BUTTON_COUNT = 2;
  const BUTTONS_TOTAL_WIDTH = BUTTON_WIDTH * BUTTON_COUNT;

  useEffect(() => {
    setLoading(true);
    apiClient.get('/notices')
      .then(res => {
        setNotices(res.data);
      })
      .catch(() => {
        setNotices([]);
      })
      .finally(() => setLoading(false));

    apiClient.get('/notices/popup')
      .then(res => {
        setPopupNoticeId(res.data?.id ?? null);
      })
      .catch(() => {
        setPopupNoticeId(null);
      });
  }, []);

  // 슬라이드 시작
  const handleTouchStart = (e: React.TouchEvent, id: number) => {
    startXRef.current = e.touches[0].clientX - (dragXMap[id] || 0);
    setStartY(e.touches[0].clientY);
    setDraggedId(id);
    setIsScrolling(false);
  };

  // 슬라이드 중
  const handleTouchMove = (e: React.TouchEvent, id: number) => {
    if (draggedId !== id) return;
    const deltaX = e.touches[0].clientX - startXRef.current;
    const deltaY = e.touches[0].clientY - startY;
    if (!isScrolling && Math.abs(deltaY) > Math.abs(deltaX)) {
      setIsScrolling(true);
      setDraggedId(null);
      return;
    }
    if (isScrolling) return;
    setDragXMap(prev => ({ ...prev, [id]: Math.min(0, Math.max(deltaX, -BUTTONS_TOTAL_WIDTH)) }));
  };

  // 슬라이드 끝
  const handleTouchEnd = (id: number) => {
    const dragX = dragXMap[id] || 0;
    const THRESHOLD = -BUTTONS_TOTAL_WIDTH / 2;
    if (dragX < THRESHOLD) {
      setDragXMap(prev => ({ ...prev, [id]: -BUTTONS_TOTAL_WIDTH }));
    } else {
      setDragXMap(prev => ({ ...prev, [id]: 0 }));
    }
    setDraggedId(null);
    setIsScrolling(false);
  };

  // 삭제 핸들러 (실제 삭제 로직은 추후 구현)
  const handleDelete = async (id: number) => {
    try {
      await apiClient.delete(`/notices/${id}`);
      setNotices(prev => prev.filter(notice => notice.id !== id));
    } catch {
      alert('공지 삭제에 실패했습니다.');
    }
  };

  const handleSetPopup = async (id: number) => {
    setPopupSettingId(id); // 1. 버튼 즉시 비활성화
    setDragXMap(prev => ({ ...prev, [id]: 0 })); // 2. 슬라이드 원위치
    try {
      await apiClient.post(`/notices/${id}/popup`); // 3. API 호출
      setTimeout(() => setPopupSettingId(null), 1500);
      setPopupNoticeId(id); // 새로 팝업 설정된 id로 갱신
    } catch {
      setPopupSettingId(null); // 실패 시 버튼 다시 활성화
    }
  };

  return (
    <div className="min-h-screen bg-white pb-40 relative">
      {/* 상단바 */}
      <TopBar title={isAdmin ? "공지사항 관리" : "공지사항"} />
      {/* 로딩 오버레이 (상단바/하단바 제외) */}
      {loading && (
        <div className="absolute left-0 right-0 top-14 bottom-16 flex items-center justify-center z-40 bg-white bg-opacity-80">
          <CeniLoading />
        </div>
      )}
      {/* 공지 리스트 */}
      <div className="pt-14 flex-1 px-4">
        {!loading && notices.length === 0 ? (
          <div className="text-center text-gray-400 py-12">공지사항이 없습니다.</div>
        ) : null}
        {[...notices].sort((a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime()).map(notice => {
          if (isAdmin) {
            const dragX = dragXMap[notice.id] || 0;
            const isDragging = draggedId === notice.id;
            return (
              <div key={notice.id} className="relative overflow-hidden">
                <div
                  className="py-3 border-b border-gray-100 bg-white relative"
                  style={{
                    transform: `translateX(${dragX}px)`,
                    transition: isDragging ? 'none' : 'transform 0.2s',
                  }}
                  onTouchStart={e => handleTouchStart(e, notice.id)}
                  onTouchMove={e => handleTouchMove(e, notice.id)}
                  onTouchEnd={() => handleTouchEnd(notice.id)}
                  onClick={() => navigate(`/notice/${notice.id}`)}
                >
                  <div className="font-bold text-sm mb-1">{notice.title}</div>
                  <div className="text-xs text-gray-400 mb-2">
                    {(() => {
                      const dateObj = new Date(notice.created_at);
                      if (isNaN(dateObj.getTime())) return notice.created_at;
                      const kst = new Date(dateObj.getTime() + 9 * 60 * 60 * 1000);
                      return `${kst.getFullYear()}.${(kst.getMonth()+1).toString().padStart(2,'0')}.${kst.getDate().toString().padStart(2,'0')} ${kst.getHours().toString().padStart(2,'0')}:${kst.getMinutes().toString().padStart(2,'0')}`;
                    })()}
                  </div>
                  <div className="text-xs text-gray-600 truncate select-none">{notice.content}</div>
                </div>
                {/* 버튼 컨테이너 */}
                <div
                  className="absolute top-0 h-full flex z-5"
                  style={{
                    left: `calc(100% + ${dragX}px)`,
                    transition: isDragging ? 'none' : 'left 0.2s',
                    pointerEvents: Math.abs(dragX) > 40 ? 'auto' : 'none',
                  }}
                >
                  <button
                    className={`w-20 h-full font-bold text-base duration-100 
                      ${popupSettingId === notice.id || popupNoticeId === notice.id
                        ? 'bg-gray-300 text-white cursor-not-allowed opacity-60' 
                        : 'bg-blue-500 text-white'}
                    `}
                    onClick={() => handleSetPopup(notice.id)}
                    disabled={popupSettingId === notice.id || popupNoticeId === notice.id}
                  >
                    팝업 설정
                  </button>
                  <button
                    className="w-20 h-full bg-red-500 text-white font-bold text-base duration-300"
                    onClick={() => handleDelete(notice.id)}
                  >
                    삭제
                  </button>
                </div>
              </div>
            );
          } else {
            return (
              <div
                key={notice.id}
                className="py-3 border-b border-gray-100 relative"
                onClick={() => navigate(`/notice/${notice.id}`)}
              >
                <div className="font-bold text-sm mb-1">{notice.title}</div>
                <div className="text-xs text-gray-400 mb-2">
                  {(() => {
                    const dateObj = new Date(notice.created_at);
                    if (isNaN(dateObj.getTime())) return notice.created_at;
                    const kst = new Date(dateObj.getTime() + 9 * 60 * 60 * 1000);
                    return `${kst.getFullYear()}.${(kst.getMonth()+1).toString().padStart(2,'0')}.${kst.getDate().toString().padStart(2,'0')} ${kst.getHours().toString().padStart(2,'0')}:${kst.getMinutes().toString().padStart(2,'0')}`;
                  })()}
                </div>
                <div className="text-xs text-gray-600 truncate select-none">{notice.content}</div>
              </div>
            );
          }
        })}
      </div>
      {/* 하단바 */}
      {!isAdmin && <BottomBar />}
      {isAdmin && (
        <button
          className="fixed bottom-20 left-1/2 -translate-x-1/2 z-30 w-16 h-16 rounded-full bg-[#5382E0] text-white flex items-center justify-center shadow-lg text-xl font-bold"
          style={{ boxShadow: '0 4px 16px rgba(83,130,224,0.15)' }}
          onClick={() => navigate('/admin/notice/write')}
          aria-label="공지 추가"
        >
          <img src="/add.png" alt="공지 추가" className="w-8 h-8 brightness-0 invert" />
        </button>
      )}
    </div>
  );
}
