// import { useNavigate } from "react-router-dom";
import { useState } from "react";
import TopBar from '../components/TopBar';
import apiClient from '../libs/axios';

export default function ResetPasswordPage() {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setSuccess(false);
    if (!email || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
      setError("올바른 이메일 주소를 입력해 주세요.");
      return;
    }
    setLoading(true);
    try {
      // 실제 API 연동
      await apiClient.post(`/users/forgot-password?email=${encodeURIComponent(email)}`);
      setLoading(false);
      setSuccess(true);
    } catch {
      setLoading(false);
      setError("임시 비밀번호 전송에 실패했습니다. 다시 시도해 주세요.");
    }
  };

  return (
    <div className="flex flex-col min-h-screen bg-[#fdfdfe]">
      {/* 상단바 */}
      <TopBar title="비밀번호 재설정" />
      {/* 중앙 컨텐츠 */}
      <div className="flex-1 flex flex-col items-center justify-center">
        <div className="w-full max-w-xs mx-auto flex flex-col items-center bg-white rounded-xl shadow px-4 py-8 gap-4">
          {/* 안내문구 */}
          <div className="text-[#5382E0] text-sm font-semibold text-center mb-10">
            가입하신 이메일을 입력하시면<br />임시 비밀번호를 전송해 드립니다.
          </div>
          {/* 입력폼 */}
          <form className="w-full flex flex-col gap-3" onSubmit={handleSubmit}>
            <label className="text-sm font-normal text-[#5382E0]">이메일</label>
            <input
              type="email"
              className="border border-gray-300 rounded-lg px-3 py-3 focus:outline-none focus:ring-2 focus:ring-blue-200 text-sm placeholder:text-gray-400 text-base appearance-none"
              placeholder="이메일을 입력해 주세요."
              value={email}
              onChange={e => setEmail(e.target.value)}
              autoComplete="email"
            />
            {/* 에러 메시지 */}
            {error && (
              <div className="text-sm text-red-500 text-center mb-2 w-full">{error}</div>
            )}
            {/* 성공 메시지 */}
            {success && (
              <div className="text-sm text-green-600 text-center mb-2 w-full">임시 비밀번호가 이메일로 전송되었습니다.</div>
            )}
            <button
              type="submit"
              className={`mt-20 w-full py-3 rounded-lg bg-[#5382E0] text-white text-base font-normal shadow hover:bg-blue-600 transition mt-6 mb-2 ${loading ? 'opacity-60 cursor-not-allowed' : ''}`}
              disabled={loading}
            >
              {loading ? '처리 중...' : '임시 비밀번호 받기'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
} 