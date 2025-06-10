import React, { useEffect, useState } from 'react';
import BottomBar from '../components/BottomBar';
import TopBar from '../components/TopBar';
import apiClient from '../libs/axios';

const formatDate = (dateStr: string) => {
  const dateObj = new Date(dateStr);
  if (isNaN(dateObj.getTime())) return dateStr;
  return `${dateObj.getFullYear()}.${(dateObj.getMonth()+1).toString().padStart(2,'0')}.${dateObj.getDate().toString().padStart(2,'0')} ${dateObj.getHours().toString().padStart(2,'0')}:${dateObj.getMinutes().toString().padStart(2,'0')}`;
};

interface RideHistoryItem {
  boarding_id: number;
  departure: string;
  destination: string;
  boarding_time: string;
}

const MyRideHistoryPage: React.FC = () => {
  const [rides, setRides] = useState<RideHistoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchRides = async () => {
      try {
        const res = await apiClient.get('/shuttle/rides');
        setRides(res.data);
      } catch {
        setError('탑승 내역을 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };
    fetchRides();
  }, []);

  return (
    <div className="min-h-screen bg-[#fdfdfe] pb-16">
      {/* 상단바 */}
      <TopBar title="나의 탑승 내역" />
      {/* 탑승 내역 리스트 */}
      <div className="pt-20 flex-1 px-4">
        {loading ? (
          <div className="text-center text-gray-400 py-8">불러오는 중...</div>
        ) : error ? (
          <div className="text-center text-red-400 py-8">{error}</div>
        ) : rides.length === 0 ? (
          <div className="text-center text-gray-400 py-8">탑승 내역이 없습니다.</div>
        ) : (
          rides.map((ride) => (
            <div
              key={ride.boarding_id}
              className="mb-3 bg-white rounded-xl p-4 shadow-sm"
            >
              <div className="flex items-center justify-between mb-3">
                <div className="text-sm text-gray-500">{formatDate(ride.boarding_time)}</div>
              </div>
              <div className="flex items-center">
                <div className="flex-1">
                  <div className="flex items-center">
                    <div className="w-2 h-2 rounded-full bg-[#5382E0] mr-2"></div>
                    <div className="text-sm font-medium">{ride.departure}</div>
                  </div>
                  <div className="flex items-center mt-3">
                    <div className="w-2 h-2 rounded-full bg-[#FF6B6B] mr-2"></div>
                    <div className="text-sm font-medium">{ride.destination}</div>
                  </div>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
      {/* 하단바 */}
      <BottomBar />
    </div>
  );
};

export default MyRideHistoryPage; 