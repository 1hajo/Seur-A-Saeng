import TopBar from '../components/TopBar';
import { useNavigate } from 'react-router-dom';

export default function AdminMainPage() {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('user');
    navigate('/');
  };

  return (
    <div className="flex flex-col min-h-screen bg-[#fdfdfe] pb-16">
      <TopBar title="관리자 페이지" showBackButton={false} />
      <div className="pt-16 px-4">
        {/* 게시판 관리 */}
        <div className="text-[15px] text-blue-500 font-medium mb-2 mt-4">게시판 관리</div>
        <div className="grid grid-cols-2 gap-3 mb-6">
          <button className="bg-[#DEE9FF] rounded-xl p-4 flex flex-col items-center shadow-sm min-h-[90px]" onClick={() => navigate('/admin/notice')}>
            <img src="/announcement.png" alt="공지사항 관리" className="w-8 h-8 mb-2" />
            <span className="font-bold text-base text-[#5382E0]">공지사항 관리</span>
          </button>
          <button className="bg-[#DEE9FF] rounded-xl p-4 flex flex-col items-center shadow-sm min-h-[90px]" onClick={() => navigate('/admin/inquiry')}>
            <img src="/lost-items.png" alt="1:1 문의 관리" className="w-8 h-8 mb-2" />
            <span className="font-bold text-base text-[#5382E0]">1:1 문의 관리</span>
          </button>
        </div>
        {/* 셔틀 관리 */}
        <div className="text-[15px] text-blue-500 font-medium mb-2 mt-15">셔틀 관리</div>
        <div className="grid grid-cols-2 gap-3">
          <button className="bg-[#DEE9FF] rounded-xl p-4 flex flex-col items-center shadow-sm min-h-[90px]" onClick={() => navigate('/admin/race-gps')}>
            <img src="/shuttle.png" alt="셔틀 운행 관리" className="w-8 h-8 mb-2" />
            <span className="font-bold text-base text-[#5382E0]">셔틀 운행 관리</span>
          </button>
          <button className="bg-[#DEE9FF] rounded-xl p-4 flex flex-col items-center shadow-sm min-h-[90px]" onClick={() => navigate('/admin/timetable')}>
            <img src="/schedule.png" alt="시간표 관리" className="w-8 h-8 mb-2" />
            <span className="font-bold text-base text-[#5382E0]">시간표 관리</span>
          </button>
        </div>
        <div className="flex justify-end px-4 pt-20">
          <button
            onClick={handleLogout}
            className="bg-red-500 text-white px-4 py-2 rounded-lg font-bold shadow hover:bg-red-600 transition"
          >
            로그아웃
          </button>
        </div>
      </div>
    </div>
  );
}
