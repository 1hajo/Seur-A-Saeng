import { useParams, useLocation } from "react-router-dom";
import { useState, useEffect } from "react";
import apiClient from '../libs/axios';
import TopBar from '../components/TopBar';
import BottomBar from '../components/BottomBar';

const InquiryDetailPage = ({ isAdmin = false }) => {
  const { id } = useParams();
  const location = useLocation();
  const initialInquiry = location.state as InquiryType | null;
  const [inquiry, setInquiry] = useState<InquiryType>(initialInquiry);
  const [loading, setLoading] = useState(true);
  const [answer, setAnswer] = useState('');

  type InquiryType = {
    id: number;
    title: string;
    content: string;
    user_name?: string;
    created_at?: string;
    date?: string;
    answer?: string;
    answered_at?: string;
    answer_status?: boolean;
    inquiry_id?: number;
  } | null;

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    apiClient.get(`/inquiries/${id}`)
      .then(res => {
        setInquiry(res.data);
      })
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <div className="max-w-md mx-auto min-h-screen bg-[#fdfdfe] pb-16 flex flex-col">
        <TopBar title="1:1 문의" />
        <div className="flex-1 flex items-center justify-center text-gray-400">로딩 중...</div>
        {!isAdmin && <BottomBar />}
      </div>
    );
  }
  if (!inquiry) {
    return (
      <div className="max-w-md mx-auto min-h-screen bg-[#fdfdfe] pb-16 flex flex-col">
        <TopBar title="1:1 문의" />
        <div className="flex-1 flex items-center justify-center text-gray-400">존재하지 않는 문의입니다.</div>
        {!isAdmin && <BottomBar />}
      </div>
    );
  }

  const dateStr = inquiry.created_at || inquiry.date || '';
  const dateObj = new Date(dateStr);
  const formattedDate = `${dateObj.getFullYear()}.${(dateObj.getMonth()+1).toString().padStart(2,'0')}.${dateObj.getDate().toString().padStart(2,'0')} ${dateObj.getHours().toString().padStart(2,'0')}:${dateObj.getMinutes().toString().padStart(2,'0')}`;

  const handleAnswerSubmit = async () => {
    if (!id) return;
    try {
      await apiClient.post(`/inquiries/${id}/answer`, { content: answer });
      setAnswer('');
      // 답변 등록 후 상세 페이지 새로고침
      setLoading(true);
      apiClient.get(`/inquiries/${id}`)
        .then(res => {
          setInquiry(res.data);
        })
        .finally(() => setLoading(false));
    } catch {
      alert('답변 등록에 실패했습니다.');
    }
  };

  const isAnswerValid = answer.trim() !== '';

  return (
    <div className="max-w-md mx-auto min-h-screen bg-[#fdfdfe] pb-16">
      {/* 상단 바 */}
      <TopBar title="1:1 문의" />

      {/* 문의 제목 및 시간 */}
      <div className="px-5 pt-20 pb-2">
        <div className="text-[#5382E0] font-bold text-base mb-1">{inquiry.title}</div>
        <div className="text-xs text-gray-400 mb-4">{formattedDate}</div>
        {inquiry.answer_status ? (
          <span className="text-xs text-white bg-[#5382E0] rounded px-2 py-0.5 font-semibold mb-2">답변완료</span>
        ) : (
          <span className="text-xs text-white bg-[#DEE9FF] rounded px-2 py-0.5 font-semibold mb-2">답변대기</span>
        )}
      </div>

      {/* 문의 내용 */}
      <div className="mx-5 mb-8 border border-[#5382E0] rounded-xl p-4 text-gray-800 text-[15px] whitespace-pre-line">
        {inquiry.content}
      </div>

      {/* 답변 */}
      {inquiry.answer && (
        <>
          <div className="mx-5 text-xs text-gray-400 mb-1 font-semibold">관리자 답변</div>
          <div className="mx-5 mb-2 bg-white border border-gray-400 rounded-xl p-4 text-gray-800 text-[15px] whitespace-pre-line">
            {typeof inquiry.answer === 'string'
              ? inquiry.answer
              : (inquiry.answer && typeof inquiry.answer === 'object' && 'answer_content' in inquiry.answer
                  ? (inquiry.answer as { answer_content: string }).answer_content
                  : '')}
            {/* 답변일 */}
            {(() => {
              const answerObj = inquiry.answer;
              const answerCreated = (answerObj && typeof answerObj === 'object' && 'created_at' in answerObj)
                ? (answerObj as { created_at: string }).created_at
                : inquiry.answered_at;
              if (answerCreated) {
                const match = (answerCreated || '').match(/(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})/);
                if (match) {
                  return (
                    <div className="mt-3 text-xs text-gray-400">
                      답변일: {`${match[1]}.${match[2]}.${match[3]} ${match[4]}:${match[5]}`}
                    </div>
                  );
                }
                return <div className="mt-3 text-xs text-gray-400">답변일: {answerCreated}</div>;
              }
              return null;
            })()}
          </div>
        </>
      )}
      {isAdmin && !inquiry.answer && (
        <div className="mb-2 bg-white rounded-xl p-4 text-gray-800 text-[15px]">
          <div className="mb-2 text-xs text-gray-400 font-semibold">답변 작성</div>
          <textarea
            value={answer}
            onChange={e => setAnswer(e.target.value)}
            placeholder="답변을 입력하세요."
            className="w-full h-28 p-3 border border-gray-300 rounded-lg focus:outline-none focus:border-[#5382E0] resize-none mb-3"
          />
          <button
            onClick={handleAnswerSubmit}
            className={`w-full h-11 rounded-lg font-bold ${isAnswerValid ? 'bg-[#5382E0] text-white' : 'bg-gray-300 text-white cursor-not-allowed'}`}
            disabled={!isAnswerValid}
          >
            등록
          </button>
        </div>
      )}
      {!isAdmin && <BottomBar />}
    </div>
  );
};

export default InquiryDetailPage;
