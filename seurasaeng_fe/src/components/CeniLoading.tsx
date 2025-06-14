import React from 'react';

const CeniLoading: React.FC = () => (
  <div className="flex flex-col items-center justify-center min-h-[500px]">
    <img
      src="/ceni.webp"
      alt="로딩 중"
      className="w-20 h-20 animate-pulse"
      style={{ animationDuration: '0.5s' }}
    />
  </div>
);

export default CeniLoading;
