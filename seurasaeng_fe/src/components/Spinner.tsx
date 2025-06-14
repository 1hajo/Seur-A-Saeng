import React from 'react';

const Spinner: React.FC<{className?: string}> = ({ className = '' }) => (
  <svg className={`animate-spin h-10 w-10 text-blue-500 ${className}`} viewBox="0 0 40 40" fill="none">
    <circle
      cx="20"
      cy="20"
      r="16"
      stroke="currentColor"
      strokeWidth="4"
      strokeLinecap="round"
      strokeDasharray="80"
      strokeDashoffset="60"
    />
  </svg>
);

export default Spinner; 