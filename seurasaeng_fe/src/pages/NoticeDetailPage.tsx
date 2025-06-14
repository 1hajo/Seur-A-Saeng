import { useParams } from 'react-router-dom';
import { useEffect, useState } from 'react';
import TopBar from '../components/TopBar';
import apiClient from '../libs/axios';
import CeniLoading from '../components/CeniLoading';

export default function NoticeDetailPage() {
  const { id } = useParams();
  const [loading, setLoading] = useState(true);

  type NoticeType = {
    id: number;
    title: string;
    content: string | null;
    created_at: string;
  };
  const [notice, setNotice] = useState<NoticeType | null>(null);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    apiClient.get(`/notices/${id}`)
      .then(res => setNotice(res.data))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <div className="max-w-md mx-auto min-h-screen bg-[#fdfdfe] pb-16 relative">
        <TopBar title="공지사항" />
        <div className="absolute left-0 right-0 top-14 bottom-0 flex items-center justify-center z-40 bg-white bg-opacity-80">
          <CeniLoading />
        </div>
      </div>
    );
  }
  if (!notice) {
    return <div className="text-center text-gray-400 py-20">존재하지 않는 공지입니다.</div>;
  }

  return (
    <div className="max-w-md mx-auto min-h-screen bg-[#fdfdfe] pb-16">
      <TopBar title="공지사항" />
      <div className="px-5 pt-20 pb-2">
        <div className="text-2xl font-bold mb-2">{notice.title}</div>
        <div className="text-xs text-gray-400 mb-4">
          {(() => {
            const dateObj = new Date(notice.created_at);
            if (isNaN(dateObj.getTime())) return notice.created_at;
            return `${dateObj.getFullYear()}.${(dateObj.getMonth()+1).toString().padStart(2,'0')}.${dateObj.getDate().toString().padStart(2,'0')} ${dateObj.getHours().toString().padStart(2,'0')}:${dateObj.getMinutes().toString().padStart(2,'0')}`;
          })()}
        </div>
      </div>
      <div className="mx-5 mb-4 border border-[#DEE9FF] rounded-xl p-4 text-gray-800 text-[15px] whitespace-pre-line">
        {notice.content}
      </div>
    </div>
  );
}
