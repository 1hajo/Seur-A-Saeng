import { useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import apiClient from '../libs/axios';
import TopBar from '../components/TopBar';
import axios from 'axios';
import OptimizedImage from '../components/OptimizedImage';

export default function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  useEffect(() => {
    const msg = localStorage.getItem('toast');
    if (msg) {
      setToast(msg);
      localStorage.removeItem('toast');
      setTimeout(() => setToast(null), 2000);
    }
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await apiClient.post('/users/login', { email, password });
      const { token, ...userFields } = res.data;
      if (token) {
        localStorage.setItem('accessToken', token);
        localStorage.setItem('user', JSON.stringify(userFields));
        if (userFields.role === 'admin') {
          navigate('/admin');
        } else {
          navigate('/main');
        }
      } else {
        setError('토큰이 응답에 없습니다.');
      }
    } catch (err) {
      if (axios.isAxiosError(err)) {
        setError(err.response?.data?.message || '로그인 실패');
      } else {
        setError('로그인 실패');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col justify-between items-center bg-[#fdfdfe] pt-6 pb-4">
      {toast && (
        <div className="fixed bottom-20 left-1/2 -translate-x-1/2 bg-[#5382E0] text-white px-6 py-3 rounded-2xl shadow-lg z-50 text-base font-semibold animate-fade-in whitespace-nowrap max-w-[90vw]">
          {toast}
        </div>
      )}
      <TopBar title="로그인" />
      <div className="w-full flex flex-col items-center">
        <h2 className="text-2xl font-bold text-center mb-4">로그인</h2>
        <OptimizedImage
          src="/ceni.webp"
          alt="캐릭터"
          width={128}
          height={128}
          className="mb-6 mt-24"
          priority={true}
        />
        <form className="w-full max-w-xs flex flex-col gap-3 mt-12" onSubmit={handleSubmit}>
          <label className="text-sm font-normal text-black">이메일</label>
          <input
            type="email"
            className="border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-200"
            placeholder="ceni@itcen.com"
            value={email}
            onChange={e => setEmail(e.target.value)}
            disabled={loading}
          />
          <label className="text-sm font-normal text-black">비밀번호</label>
          <input
            type="password"
            className="border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-200"
            placeholder="비밀번호를 입력하세요."
            value={password}
            onChange={e => setPassword(e.target.value)}
            disabled={loading}
          />
          <button
            type="button"
            className="mt-2 text-sm text-gray-700 underline underline-offset-2 text-left"
            onClick={() => navigate('/reset-password')}
            disabled={loading}
          >
            비밀번호를 잊으셨나요?
          </button>
          {error && <div className="text-red-500 text-xs mt-2">{error}</div>}
          <button
            type="submit"
            className={`w-full py-3 rounded-lg bg-[#5382E0] text-white text-base font-normal shadow hover:bg-blue-600 transition mt-8 ${loading ? 'opacity-60 cursor-not-allowed' : ''}`}
            disabled={loading || !email || !password}
          >
            {loading ? (
              <span className="flex items-center justify-center">
                <svg className="animate-spin h-4 w-4 mr-1 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"></path>
                </svg>
              </span>
            ) : (
              '로그인'
            )}
          </button>
        </form>
      </div>
    </div>
  );
} 