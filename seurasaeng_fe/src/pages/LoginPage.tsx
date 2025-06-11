import { useNavigate } from "react-router-dom";
import { useState } from "react";
import apiClient from '../libs/axios';
import TopBar from '../components/TopBar';
import axios from 'axios';

export default function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

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
        navigate('/main');
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
      <TopBar title="로그인" />
      <div className="w-full flex flex-col items-center">
        <h2 className="text-2xl font-bold text-center mb-4">로그인</h2>
        <img
          src="/ceni.webp"
          alt="캐릭터"
          className="w-32 h-32 object-contain mb-6 mt-24"
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
            {loading ? '로그인 중...' : '로그인'}
          </button>
        </form>
      </div>
    </div>
  );
} 