import { useNavigate } from 'react-router-dom';
import React from 'react';
import OptimizedImage from './OptimizedImage';

interface TopBarProps {
  title: string;
  showBackButton?: boolean;
  rightContent?: React.ReactNode;
  bgColorClass?: string; // 기본값: bg-[#5382E0]
}

export default function TopBar({
  title,
  showBackButton = true,
  rightContent,
  bgColorClass = 'bg-[#5382E0]',
}: TopBarProps) {
  const navigate = useNavigate();
  return (
    <div className={`fixed left-0 top-0 right-0 z-20 flex items-center h-14 px-4 ${bgColorClass}`}>
      {showBackButton && (
        <button className="absolute left-4" onClick={() => navigate(-1)}>
          <OptimizedImage
            src="/back.png"
            alt="뒤로가기"
            width={24}
            height={24}
            className="invert brightness-0"
            priority={true}
          />
        </button>
      )}
      <div className="flex-1 text-center font-semibold text-lg text-white">{title}</div>
      {rightContent && <div className="absolute right-4">{rightContent}</div>}
    </div>
  );
} 