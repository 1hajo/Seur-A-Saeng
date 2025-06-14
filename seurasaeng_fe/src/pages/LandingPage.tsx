import { useNavigate } from 'react-router-dom';
import OptimizedImage from '../components/OptimizedImage';
import { useEffect, useState } from 'react';

export default function LandingPage() {
  const navigate = useNavigate();
  const [deferredPrompt, setDeferredPrompt] = useState<any>(null);
  const [showInstallBanner, setShowInstallBanner] = useState(false);

  useEffect(() => {
    const handler = (e: any) => {
      e.preventDefault();
      setDeferredPrompt(e);
      setShowInstallBanner(true);
    };
    window.addEventListener('beforeinstallprompt', handler);
    return () => window.removeEventListener('beforeinstallprompt', handler);
  }, []);

  const handleInstallClick = async () => {
    if (deferredPrompt) {
      deferredPrompt.prompt();
      const { outcome } = await deferredPrompt.userChoice;
      setShowInstallBanner(false);
      setDeferredPrompt(null);
    }
  };

  return (
    <div className="flex flex-col justify-between items-center bg-[#fdfdfe] pt-12 pb-8">
      <div className="flex flex-col items-center w-full">
        <p className="text-xs text-blue-300 mb-8">특별한 우리들의 평범한 매일</p>
        <h1 className="text-3xl font-bold text-center leading-tight mb-2">
          슬기로운<br />
          <span className="tracking-wider">아이티센 생활<sup className="text-xs align-super">+</sup></span>
        </h1>
        <OptimizedImage
          src="/ceni-bus-blue.webp"
          alt="셔틀버스"
          width={160}
          height={160}
          className="mx-auto my-4 mt-32 drop-shadow-md"
          priority={true}
        />
        <div className="text-center text-base text-gray-500 mb-2 mt-12">-셔틀편-</div>
      </div>
      <div className="w-full flex flex-col gap-3 px-6 mt-8">
        <button
          className="w-full py-3 rounded-lg bg-[#5382E0] text-white text-base font-normal shadow hover:bg-blue-600 transition"
          onClick={() => navigate('/register')}
        >
          회원가입
        </button>
        <button
          className="w-full py-3 rounded-lg border border-[#5382E0] text-[#5382E0] text-base font-normal bg-white hover:bg-blue-50 transition"
          onClick={() => navigate('/login')}
        >
          로그인
        </button>
      </div>
      {showInstallBanner && (
        <div
          className="fixed top-4 left-1/2 -translate-x-1/2 bg-white border rounded-xl shadow-lg px-6 py-4 flex items-center z-50"
          style={{ minWidth: 320, maxWidth: '90vw' }}
        >
          <div className="flex-1">
            <div className="font-bold text-base mb-1">앱 설치하고<br />더 편리하게 이용하세요!</div>
            <div className="flex gap-2 mt-2">
              <button
                className="w-full bg-[#5382E0] text-white px-4 py-2 rounded-lg font-bold"
                onClick={handleInstallClick}
              >
                앱 설치
              </button>
              <button
                className="w-full rounded-lg border border-[#5382E0] text-[#5382E0]"
                onClick={() => setShowInstallBanner(false)}
              >
                닫기
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
