import { useNavigate } from 'react-router-dom';
import { useState, useRef, useEffect } from 'react';
import TopBar from '../components/TopBar';
import apiClient from '../libs/axios';
import { MdAccountCircle } from 'react-icons/md';

const LOCATIONS = [
  { id: 1, name: '정부과천청사역' },
  { id: 2, name: '양재역' },
  { id: 3, name: '사당역' },
  { id: 4, name: '이수역' },
  { id: 5, name: '금정역' },
  { id: 6, name: '정부과천청사역' },
  { id: 7, name: '양재역' },
  { id: 8, name: '사당역' },
  { id: 9, name: '이수역' },
  { id: 10, name: '금정역' },
];

export default function EditProfilePage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [profileImg, setProfileImg] = useState<string | null>(null);
  const [routeGo, setRouteGo] = useState('');
  const [routeReturn, setRouteReturn] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    apiClient.get('/users/me').then(res => {
      setEmail(res.data.email);
      setName(res.data.name);
      setProfileImg(res.data.image);
      setRouteGo(res.data.favorites_work_id ? String(res.data.favorites_work_id) : '');
      setRouteReturn(res.data.favorites_home_id ? String(res.data.favorites_home_id) : '');
    });
  }, []);

  const pwValid = password.length > 0 && /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]).{8,}$/.test(password);
  const pwMatch = password === confirmPassword && password.length > 0 && confirmPassword.length > 0;
  const pwRequired = password.length > 0 && confirmPassword.length > 0;

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await apiClient.patch('/users/me', {
        password: password,
        image: profileImg,
        favorites_work_id: Number(routeGo) || 0,
        favorites_home_id: Number(routeReturn) || 0,
      });
      // 로컬스토리지 user 정보 업데이트
      const user = JSON.parse(localStorage.getItem('user') || '{}');
      const updatedUser = {
        ...user,
        image: profileImg,
      };
      localStorage.setItem('user', JSON.stringify(updatedUser));
      setLoading(false);
      navigate(-1);
    } catch {
      setLoading(false);
      setError('저장에 실패했습니다.');
    }
  };

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (ev) => {
        if (ev.target?.result) setProfileImg(ev.target.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  return (
    <div className="flex flex-col min-h-screen bg-[#fdfdfe]">
      {/* 상단바 */}
      <TopBar title="개인정보 수정" />
      <div className="flex-1 flex flex-col items-center justify-center pt-14 pb-8">
        <form className="w-full max-w-xs flex flex-col items-center gap-3 bg-white rounded-xl shadow px-4 py-8 mt-4" onSubmit={handleSave}>
          {/* 프로필 이미지 */}
          <div className="flex flex-col items-center mb-2">
            <div className="w-24 h-24 rounded-full flex items-center justify-center mb-2 overflow-hidden">
              {profileImg ? (
                <img src={profileImg} alt="프로필" className="w-20 h-20 object-contain rounded-full bg-white" />
              ) : (
                <MdAccountCircle size={80} color="#5382E0" className="bg-white rounded-full w-20 h-20" />
              )}
            </div>
            <input
              type="file"
              accept="image/*"
              className="hidden"
              ref={fileInputRef}
              onChange={handleImageChange}
            />
            <button
              type="button"
              className="px-4 py-2 rounded-lg bg-[#5382E0] text-white text-sm font-semibold mt-1 mb-2 hover:bg-blue-600 transition"
              onClick={() => fileInputRef.current?.click()}
            >
              이미지 수정
            </button>
          </div>
          {/* 이름 */}
          <label className="w-full text-sm">이름</label>
          <input
            type="text"
            className="w-full border border-gray-300 rounded-lg px-3 py-3 focus:outline-none focus:ring-2 focus:ring-blue-200 text-sm placeholder:text-gray-400 bg-gray-100"
            value={name}
            disabled
          />
          {/* 이메일 */}
          <label className="w-full text-sm">이메일</label>
          <input
            type="email"
            className="w-full border border-gray-300 rounded-lg px-3 py-3 focus:outline-none focus:ring-2 focus:ring-blue-200 text-sm placeholder:text-gray-400 bg-gray-100"
            value={email}
            disabled
          />
          {/* 비밀번호 */}
          <label className="w-full text-sm">비밀번호</label>
          <input
            type="password"
            className="w-full border border-gray-300 rounded-lg px-3 py-3 focus:outline-none focus:ring-2 focus:ring-blue-200 text-sm placeholder:text-gray-400"
            placeholder="8자 이상, 영문+숫자+특수문자 조합"
            value={password}
            onChange={e => setPassword(e.target.value)}
            autoComplete="new-password"
          />
          {/* 비밀번호 유효성 피드백 */}
          {password && !pwValid && (
            <div className="w-full text-xs text-red-500">8자 이상, 영문+숫자+특수문자를 모두 포함해야 합니다.</div>
          )}
          <label className="w-full text-sm">비밀번호 확인</label>
          <input
            type="password"
            className="w-full border border-gray-300 rounded-lg px-3 py-3 focus:outline-none focus:ring-2 focus:ring-blue-200 text-sm placeholder:text-gray-400"
            placeholder="비밀번호를 한 번 더 입력해 주세요."
            value={confirmPassword}
            onChange={e => setConfirmPassword(e.target.value)}
            autoComplete="new-password"
          />
          {/* 비밀번호 확인 피드백 */}
          {confirmPassword && !pwMatch && (
            <div className="w-full text-xs text-red-500">비밀번호가 일치하지 않습니다.</div>
          )}
          {/* 즐겨찾기 노선 섹션 - 카드 스타일 제거, 구분선만 */}
          <div className="w-full border-t border-gray-200 pt-4 mt-2">
            <div className="font-bold text-base text-[#5382E0] mb-1 flex items-center">
              즐겨찾기 노선
            </div>
            <div className="text-xs text-gray-500 mb-3">출근/퇴근 시 자주 이용하는 노선을 각각 선택해 주세요.</div>
            <div className="flex flex-col gap-4">
              <div>
                <label className="text-sm flex items-center mb-1">출근</label>
                <select
                  className="w-full border border-gray-300 rounded-lg px-3 py-3 focus:outline-none focus:ring-2 focus:ring-blue-200 text-sm bg-white"
                  value={routeGo}
                  onChange={e => setRouteGo(e.target.value)}
                >
                  <option value="">선택 안 함</option>
                  {LOCATIONS.filter(loc => loc.id >= 1 && loc.id <= 5).map(loc => (
                    <option key={loc.id} value={String(loc.id)}>{loc.name}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="text-sm flex items-center mb-1">퇴근</label>
                <select
                  className="w-full border border-gray-300 rounded-lg px-3 py-3 focus:outline-none focus:ring-2 focus:ring-blue-200 text-sm bg-white"
                  value={routeReturn}
                  onChange={e => setRouteReturn(e.target.value)}
                >
                  <option value="">선택 안 함</option>
                  {LOCATIONS.filter(loc => loc.id >= 6 && loc.id <= 10).map(loc => (
                    <option key={loc.id} value={String(loc.id)}>{loc.name}</option>
                  ))}
                </select>
              </div>
            </div>
          </div>
          {/* 에러 메시지 */}
          {error && (
            <div className="w-full text-xs text-red-500 mt-[-8px] mb-1">{error}</div>
          )}
          <button
            type="submit"
            className={`w-full py-3 rounded-lg bg-[#5382E0] text-white text-base font-normal shadow hover:bg-blue-600 transition mt-10 ${(!pwRequired || loading || !pwValid || !pwMatch) ? 'opacity-60 cursor-not-allowed' : ''}`}
            disabled={!pwRequired || loading || !pwValid || !pwMatch}
          >
            {loading ? '저장 중...' : '저장'}
          </button>
        </form>
      </div>
    </div>
  );
} 