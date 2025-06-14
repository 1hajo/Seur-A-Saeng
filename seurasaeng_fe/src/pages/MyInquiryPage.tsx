import { useNavigate } from 'react-router-dom';
import { useState, useRef, useEffect } from 'react';
import TopBar from '../components/TopBar';
import apiClient from '../libs/axios';

// 문의 목록 아이템 타입
interface InquiryListItem {
  id: number;
  title: string;
  created_at: string;
  answer_status: boolean;
}

function getStatusBadge(status: string) {
  if (status === '답변완료') {
    return <span className="text-xs text-white bg-[#5382E0] rounded px-2 py-0.5 font-semibold">답변완료</span>;
  }
  if (status === '답변대기') {
    return <span className="text-xs text-white bg-[#DEE9FF] rounded px-2 py-0.5 font-semibold">답변대기</span>;
  }
  return null;
}

export default function MyInquiryPage({ isAdmin = false }) {
  const navigate = useNavigate();
  const [inquiries, setInquiries] = useState<InquiryListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [draggedId, setDraggedId] = useState<number|null>(null);
  const [dragXMap, setDragXMap] = useState<{[id:number]:number}>({});
  const startXRef = useRef(0);
  const [startY, setStartY] = useState(0);
  const [isScrolling, setIsScrolling] = useState(false);

  useEffect(() => {
    const url = isAdmin ? '/inquiries/admin' : '/inquiries';
    apiClient.get(url).then(res => {
      setInquiries(res.data);
    }).finally(() => {
      setLoading(false);
    });
  }, [isAdmin]);

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

    // 수직 스크롤이 더 큰 경우 스크롤 모드로 전환
    if (!isScrolling && Math.abs(deltaY) > Math.abs(deltaX)) {
      setIsScrolling(true);
      setDraggedId(null); // 스크롤 모드로 전환되면 draggedId를 null로 설정
      return;
    }

    // 스크롤 중이면 슬라이드 동작 무시
    if (isScrolling) return;

    setDragXMap(prev => ({ ...prev, [id]: Math.min(0, Math.max(deltaX, -80)) }));
  };

  // 슬라이드 끝
  const handleTouchEnd = (id: number) => {
    const dragX = dragXMap[id] || 0;
    if (dragX < -40) {
      setDragXMap(prev => ({ ...prev, [id]: -80 }));
    } else {
      setDragXMap(prev => ({ ...prev, [id]: 0 }));
    }
    setDraggedId(null);
    setIsScrolling(false);
  };

  // 문의 삭제 핸들러 (일반 사용자만)
  const handleDelete = async (id: number) => {
    try {
      await apiClient.delete(`/inquiries/${id}`);
      setInquiries(prev => prev.filter(inquiry => inquiry.id !== id));
    } catch {
      alert('문의 삭제에 실패했습니다.');
    }
  };

  return (
    <div className="min-h-screen pb-50 bg-white">
      {/* 상단바 */}
      <TopBar title={isAdmin ? "1:1 문의 관리" : "나의 문의 내역"} />
      <div className="border-b border-gray-100" />
      {/* 문의 목록 */}
      <div className="flex-1 pl-4 pt-14">
        {loading ? null : (
          inquiries.length === 0 ? (
            <div className="text-center text-gray-400 py-16 text-base">문의 내역이 없습니다.</div>
          ) : (
            inquiries.map(inquiry => {
              const dragX = dragXMap[inquiry.id] || 0;
              const isDragging = draggedId === inquiry.id;
              if (isAdmin) {
                // 관리자: 슬라이드 없이 카드만 클릭 가능
                return (
                  <div
                    key={inquiry.id}
                    className="py-3 border-b border-gray-100 bg-white relative"
                    onClick={() => navigate(`/admin/inquiry/${inquiry.id}`)}
                  >
                    <div className="font-bold text-sm mb-1">{inquiry.title}</div>
                    <div className="text-xs text-gray-400 mb-1">
                      {(() => {
                        const dateObj = new Date(inquiry.created_at);
                        if (isNaN(dateObj.getTime())) return inquiry.created_at;
                        const kst = new Date(dateObj.getTime() + 9 * 60 * 60 * 1000);
                        return `${kst.getFullYear()}.${(kst.getMonth()+1).toString().padStart(2,'0')}.${kst.getDate().toString().padStart(2,'0')} ${kst.getHours().toString().padStart(2,'0')}:${kst.getMinutes().toString().padStart(2,'0')}`;
                      })()}
                    </div>
                    {getStatusBadge(inquiry.answer_status ? '답변완료' : '답변대기') && (
                      <div className="mt-0.5">{getStatusBadge(inquiry.answer_status ? '답변완료' : '답변대기')}</div>
                    )}
                  </div>
                );
              } else {
                // 일반 사용자: 슬라이드/삭제 가능
                return (
                  <div key={inquiry.id} className="relative overflow-hidden">
                    {/* 문의 카드 */}
                    <div
                      className="py-3 border-b border-gray-100 bg-white pr-20"
                      style={{
                        transform: `translateX(${dragX}px)`,
                        transition: isDragging ? 'none' : 'transform 0.2s',
                      }}
                      onTouchStart={e => handleTouchStart(e, inquiry.id)}
                      onTouchMove={e => handleTouchMove(e, inquiry.id)}
                      onTouchEnd={() => handleTouchEnd(inquiry.id)}
                      onClick={() => navigate(`/inquiry/${inquiry.id}`)}
                    >
                      <div className="font-bold text-sm mb-1">{inquiry.title}</div>
                      <div className="text-xs text-gray-400 mb-1">
                        {(() => {
                          const dateObj = new Date(inquiry.created_at);
                          if (isNaN(dateObj.getTime())) return inquiry.created_at;
                          const kst = new Date(dateObj.getTime() + 9 * 60 * 60 * 1000);
                          return `${kst.getFullYear()}.${(kst.getMonth()+1).toString().padStart(2,'0')}.${kst.getDate().toString().padStart(2,'0')} ${kst.getHours().toString().padStart(2,'0')}:${kst.getMinutes().toString().padStart(2,'0')}`;
                        })()}
                      </div>
                      {getStatusBadge(inquiry.answer_status ? '답변완료' : '답변대기') && (
                        <div className="mt-0.5">{getStatusBadge(inquiry.answer_status ? '답변완료' : '답변대기')}</div>
                      )}
                    </div>
                    {/* 삭제 버튼: 일반 사용자만 */}
                    <button
                      className="absolute top-0 h-full w-20 bg-red-500 text-white font-bold text-base z-10 duration-300"
                      style={{
                        left: `calc(100% + ${dragX}px)`,
                        transition: isDragging ? 'none' : 'left 0.2s',
                        pointerEvents: Math.abs(dragX) > 40 ? 'auto' : 'none',
                      }}
                      onClick={() => handleDelete(inquiry.id)}
                    >
                      삭제
                    </button>
                  </div>
                );
              }
            })
          )
        )}
      </div>
      {/* 문의하기 FAB 버튼 - 하단 중앙 */}
      {!isAdmin && (
        <button
          className="fixed bottom-20 left-1/2 -translate-x-1/2 z-30 w-16 h-16 rounded-full bg-[#5382E0] text-white flex items-center justify-center shadow-lg text-xl font-bold"
          style={{ boxShadow: '0 4px 16px rgba(83,130,224,0.15)' }}
          onClick={() => navigate('/inquiry/write')}
          aria-label="문의하기"
        >
          <img src="/add.png" alt="문의하기" className="w-8 h-8 brightness-0 invert" />
        </button>
      )}
    </div>
  );
}
