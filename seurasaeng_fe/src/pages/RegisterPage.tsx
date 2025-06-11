import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../libs/axios';
import TopBar from '../components/TopBar';
import axios from 'axios';

export default function RegisterPage() {
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [error, setError] = useState('');
  const [emailChecked, setEmailChecked] = useState(false);
  const [codeSent, setCodeSent] = useState(false);
  const [emailLoading, setEmailLoading] = useState(false);
  const [codeVerified, setCodeVerified] = useState(false);
  const [codeError, setCodeError] = useState('');
  const [serverCode, setServerCode] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (!email || !name || !password || !passwordConfirm) {
      setError('모든 필드를 입력해주세요.');
      return;
    }
    if (password !== passwordConfirm) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }
    try {
      const res = await apiClient.post('/users/signup', {
        name,
        email,
        password,
        role: 'user',
      });
      // 응답 객체에서 token과 user 정보 분리
      const { token, ...userFields } = res.data;
      if (token) {
        localStorage.setItem('accessToken', token);
        localStorage.setItem('user', JSON.stringify(userFields));
        navigate('/main'); // 회원가입 후 메인 페이지로 이동
      } else {
        setError('토큰이 응답에 없습니다.');
      }
    } catch (err) {
      if (axios.isAxiosError(err)) {
        setError(err.response?.data?.message || '회원가입 실패');
      } else {
        setError('회원가입 실패');
      }
    }
  };

  const handleEmailButton = async () => {
    setError('');
    if (!email) {
      setError('이메일을 입력해주세요.');
      return;
    }
    setEmailLoading(true);
    try {
      if (!emailChecked) {
        try {
          const res = await apiClient.post('/users/verify-email', undefined, { params: { email } });
          setEmail(res.data);
          setEmailChecked(true);
        } catch (err) {
          if (axios.isAxiosError(err)) {
            const msg = err.response?.data?.error || '요청 실패';
            setError(msg);
            setEmailChecked(false);
          } else {
            setError('요청 실패');
            setEmailChecked(false);
          }
        }
      } else {
        const res = await apiClient.post('/users/email', undefined, { params: { email } });
        setServerCode(String(res.data));
        setCodeSent(true);
      }
    } catch (err) {
      if (axios.isAxiosError(err)) {
        setError(err.response?.data?.message || '요청 실패');
      } else {
        setError('요청 실패');
      }
    } finally {
      setEmailLoading(false);
    }
  };

  const handleVerifyCode = () => {
    setError('');
    setCodeError('');
    if (code === serverCode && code.length > 0) {
      setCodeVerified(true);
    } else {
      setCodeError('인증 코드가 틀렸습니다.');
      setCodeSent(false);
    }
  };

  // 비밀번호 유효성 검사 함수
  const validatePassword = (pw: string) => {
    return /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]).{8,}$/.test(pw);
  };
  const pwValid = validatePassword(password);
  const pwMatch = password === passwordConfirm && password.length > 0;

  return (
    <div className="flex flex-col justify-between items-center bg-[#fdfdfe] pt-6 pb-4 min-h-full">
      <TopBar title="회원가입" />
      <div className="w-full flex flex-col items-center">
        <h2 className="text-xl font-bold text-center mb-6">회원가입</h2>
        <form className="w-full flex flex-col gap-4 mt-8 pb-4 px-4" onSubmit={handleSubmit}>
          {/* 이메일 */}
          <label className="text-sm font-normal text-black">이메일</label>
          <div className="flex gap-2 w-full">
            <input
              type="email"
              className={`flex-1 min-w-0 border border-gray-300 rounded-lg px-3 py-3 focus:outline-none focus:ring-2 focus:ring-blue-200 placeholder:text-gray-400 text-base appearance-none ${codeVerified ? 'bg-gray-200' : ''}`}
              placeholder="example@itcen.com"
              value={email}
              onChange={e => {
                setEmail(e.target.value);
                setEmailChecked(false);
                setCodeSent(false);
                setCodeError('');
                setError('');
              }}
              disabled={(emailChecked && codeSent) || codeVerified}
            />
            <button
              type="button"
              className={`flex-shrink-0 min-w-[72px] px-2 py-3 rounded-lg text-white text-sm font-normal transition text-base appearance-none ${codeVerified ? 'bg-gray-200 cursor-not-allowed' : 'bg-[#5382E0] hover:bg-blue-600'}`}
              onClick={handleEmailButton}
              disabled={emailLoading || !email || (emailChecked && codeSent) || codeVerified}
            >
              {emailLoading ? (
                <span className="flex items-center justify-center">
                  <svg className="animate-spin h-4 w-4 mr-1 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"></path>
                  </svg>
                  로딩...
                </span>
              ) : (
                emailChecked ? (codeSent ? '재전송' : '전송') : '중복확인'
              )}
            </button>
          </div>
          {/* 이메일 관련 에러 메시지 */}
          {(error === '이미 사용 중인 이메일입니다.' || error === '이미 존재하는 회원입니다.') && (
            <div className="text-red-500 text-xs mt-1 mb-1">{error}</div>
          )}
          {/* 인증 코드 확인: 전송 버튼을 누른 후에만 표시 */}
          {(codeSent || codeVerified || codeError) && (
            <>
              <label className="text-sm font-normal text-black">인증 코드 확인</label>
              <div className="flex gap-2 w-full">
                <input
                  type="text"
                  className={`flex-1 min-w-0 border border-gray-300 rounded-lg px-3 py-3 focus:outline-none focus:ring-2 focus:ring-blue-200 placeholder:text-gray-400 text-base appearance-none ${codeVerified ? 'bg-gray-200' : ''}`}
                  placeholder="인증 코드 입력"
                  value={code}
                  onChange={e => setCode(e.target.value)}
                  disabled={codeVerified}
                />
                <button type="button" className={`flex-shrink-0 min-w-[60px] px-2 py-3 rounded-lg text-white text-sm font-normal transition text-base appearance-none ${codeVerified ? 'bg-gray-200 cursor-not-allowed' : 'bg-[#5382E0] hover:bg-blue-600'}`} onClick={handleVerifyCode} disabled={codeVerified || !code}>확인</button>
              </div>
              {codeVerified && <div className="text-green-600 text-xs mt-1">인증 완료</div>}
              {codeError && <div className="text-red-500 text-xs mt-1 mb-1">{codeError}</div>}
            </>
          )}
          {/* 이름 */}
          <label className="text-sm font-normal text-black">이름</label>
          <input
            type="text"
            className="w-full border border-gray-300 rounded-lg px-3 py-3 focus:outline-none focus:ring-2 focus:ring-blue-200 placeholder:text-gray-400 text-base appearance-none"
            placeholder="이름을 입력해주세요."
            value={name}
            onChange={e => setName(e.target.value)}
          />
          {/* 비밀번호 */}
          <label className="text-sm font-normal text-black">비밀번호</label>
          <input
            type="password"
            className="w-full border border-gray-300 rounded-lg px-3 py-3 focus:outline-none focus:ring-2 focus:ring-blue-200 placeholder:text-gray-400 text-base appearance-none"
            placeholder="8자리 이상, 영문+숫자+특수문자"
            value={password}
            onChange={e => setPassword(e.target.value)}
          />
          {/* 비밀번호 조건 피드백 */}
          {password.length > 0 && !pwValid && (
            <div className="text-xs text-red-500 mt-[-8px] mb-1">8자리 이상, 영문+숫자+특수문자를 모두 포함해야 합니다.</div>
          )}
          {/* 비밀번호 확인 */}
          <label className="text-sm font-normal text-black">비밀번호 확인</label>
          <input
            type="password"
            className="w-full border border-gray-300 rounded-lg px-3 py-3 focus:outline-none focus:ring-2 focus:ring-blue-200 placeholder:text-gray-400 text-base appearance-none"
            placeholder="비밀번호를 한번 더 입력해주세요"
            value={passwordConfirm}
            onChange={e => setPasswordConfirm(e.target.value)}
          />
          {/* 비밀번호 일치 피드백 */}
          {passwordConfirm.length > 0 && !pwMatch && (
            <div className="text-xs text-red-500 mt-[-8px] mb-1">비밀번호가 일치하지 않습니다.</div>
          )}
          <button
            type="submit"
            className={`w-full py-3 rounded-lg bg-[#5382E0] text-white text-base font-normal shadow hover:bg-blue-600 transition mt-8 ${(!pwValid || !pwMatch || !codeVerified) ? 'opacity-60 cursor-not-allowed' : ''}`}
            disabled={!pwValid || !pwMatch || !codeVerified}
          >
            회원가입
          </button>
        </form>
      </div>
    </div>
  );
}
