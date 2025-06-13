import React, { useRef, useState, useEffect } from "react";
import { fetchPerplexityChat } from "../api/perplexity";
import ReactMarkdown from 'react-markdown';
import apiClient from '../libs/axios';
import type { ShuttleScheduleJson } from '../types/ShuttleTypes';

const DEFAULT_POSITION = {
  x: window.innerWidth - 340 - 16,
  y: window.innerHeight - 550 - 40,
};

interface Message {
  role: 'user' | 'assistant';
  content: string;
  citations?: string[];
}

interface ApiTimetableItem {
  shuttleId: number;
  spotName: string;
  totalSeats: string;
  duration: string;
  boardingPoint: string;
  timetables: { turn: string; departureTime: string }[];
}

function mapApiToTimetable(apiData: { commute: ApiTimetableItem[]; offwork: ApiTimetableItem[] }) {
  function mapItem(item: ApiTimetableItem, isOffwork = false) {
    return {
      거점: item.spotName,
      차량규격: item.totalSeats,
      소요시간: item.duration,
      승차장소: isOffwork ? undefined : item.boardingPoint,
      하차장소: isOffwork ? item.boardingPoint : undefined,
      출발시간: (item.timetables || []).map((t) => ({ [t.turn]: t.departureTime })),
    };
  }
  return {
    출근: (apiData.commute || []).map(item => mapItem(item, false)),
    퇴근: (apiData.offwork || []).map(item => mapItem(item, true)),
  };
}

export default function Chatbot({ onClose }: { onClose: () => void }) {
  // 위치 & 리셋
  const [position, setPosition] = useState(DEFAULT_POSITION);
  const [isResetting, setIsResetting] = useState(false);

  // 메시지, 입력, 로딩
  const [messages, setMessages] = useState<Message[]>([
    { role: 'assistant', content: '아이티센의 마스코트, 세니입니다! 무엇을 도와드릴까요?' }
  ]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [loadingDots, setLoadingDots] = useState('.');

  // 시간표 데이터
  const [timetableData, setTimetableData] = useState<ShuttleScheduleJson | null>(null);

  // 레퍼런스
  const chatWindowRef = useRef<HTMLDivElement>(null);
  const chatContainerRef = useRef<HTMLDivElement>(null);
  const messageRefs = useRef<(HTMLDivElement | null)[]>([]);
  const dragging = useRef(false);
  const offset = useRef({ x: 0, y: 0 });
  const lastTouchPos = useRef(DEFAULT_POSITION);

  // 외부 클릭 시 닫기
  useEffect(() => {
    const handler = (e: MouseEvent | TouchEvent) => {
      if (chatWindowRef.current && !chatWindowRef.current.contains(e.target as Node)) {
        e.preventDefault(); e.stopPropagation();
        onClose();
      }
    };
    document.addEventListener('mousedown', handler);
    document.addEventListener('touchstart', handler, { passive: false });
    return () => {
      document.removeEventListener('mousedown', handler);
      document.removeEventListener('touchstart', handler);
    };
  }, [onClose]);

  // 로딩 애니메이션
  useEffect(() => {
    let iv: number;
    if (isLoading) {
      iv = window.setInterval(() => setLoadingDots(d => d === '...' ? '.' : d + '.'), 300);
    }
    return () => window.clearInterval(iv);
  }, [isLoading]);

  // 새 메시지 시작점으로 스크롤
  useEffect(() => {
    if (!isLoading && messages.length > 0) {
      const idx = messages.length - 1;
      const el = messageRefs.current[idx];
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [messages, isLoading]);

  // 시간표 fetch
  useEffect(() => {
    apiClient.get('/timetables')
      .then(res => setTimetableData(mapApiToTimetable(res.data)))
      .catch(console.error);
  }, []);

  // 메시지 전송
  const handleSendMessage = async (msg: string) => {
    if (!msg.trim() || isLoading) return;
    setMessages(m => [...m, { role: 'user', content: msg }]);
    setInput(''); setIsLoading(true);
    try {
      const r = await fetchPerplexityChat(msg);
      setMessages(m => [...m, { role: 'assistant', content: r.content, citations: r.citations }]);
    } catch {
      setMessages(m => [...m, { role: 'assistant', content: '죄송합니다. 응답을 받아오는데 실패했습니다.' }]);
    } finally {
      setIsLoading(false);
    }
  };

  // 버튼 로직
  const handleButtonClick = (message: string) => {
    if (!timetableData) {
      setMessages(m => [...m, { role: 'user', content: message }, { role: 'assistant', content: '시간표 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.' }]);
      return;
    }
    // 가장 빠른 셔틀
    if (message === '가장 빠른 셔틀 출발 시간') {
      const now = new Date();
      const isMorning = now.getHours() < 12;
      const types: ('출근'|'퇴근')[] = isMorning ? ['출근','퇴근'] : ['퇴근','출근'];
      let found: { time: string; spot: string; type: string } | null = null;
      for (const t of types) {
        for (const item of timetableData[t]) {
          for (const obj of item.출발시간) {
            const timeStr = Object.values(obj)[0];
            const [h,m] = timeStr.split(':').map(Number);
            const dt = new Date(now.getFullYear(),now.getMonth(),now.getDate(),h,m);
            if (dt<now) continue;
            if (!found || dt < new Date(now.getFullYear(),now.getMonth(),now.getDate(),...found.time.split(':').map(Number))) {
              found = { time: timeStr, spot: item.거점, type: t };
            }
          }
        }
        if (found) break;
      }
      setMessages(m => [...m, { role:'user', content:message }]);
      setMessages(m => [...m, {
        role:'assistant',
        content: found
          ? `지금 기준으로 가장 빠른 **${found.type} 셔틀**은 ${found.time}에 ${found.spot}에서 출발합니다.`
          : '오늘 남은 셔틀이 없습니다.'
      }]);
      return;
    }
    // 정류장 위치
    if (message === '셔틀 정류장 위치') {
      let reply = '**출근 셔틀 정류장**\n';
      timetableData['출근'].forEach(i=> reply += `- ${i.거점}: ${i.승차장소}\n`);
      reply += '\n**퇴근 셔틀 정류장**\n';
      timetableData['퇴근'].forEach(i=> reply += `- ${i.거점}: ${i.하차장소}\n`);
      setMessages(m=>[...m, { role:'user', content:message }, { role:'assistant', content:reply }]);
      return;
    }
    // 배차 간격
    if (message === '셔틀 배차 간격') {
      const calc = (times: string[]) => {
        if (times.length<2) return 'N/A';
        const mins = times.map(t=>{const [h,m]=t.split(':').map(Number);return h*60+m;}).sort((a,b)=>a-b);
        const diffs = mins.slice(1).map((v,i)=>v-mins[i]);
        return Math.round(diffs.reduce((a,b)=>a+b,0)/diffs.length)+'분';
      };
      let reply = '**출근 셔틀 배차 간격**\n';
      timetableData['출근'].forEach(i=>{
        const ts = i.출발시간.map(o=>Object.values(o)[0]);
        reply += `- ${i.거점}: ${calc(ts)}\n`;
      });
      reply += '\n**퇴근 셔틀 배차 간격**\n';
      timetableData['퇴근'].forEach(i=>{
        const ts = i.출발시간.map(o=>Object.values(o)[0]);
        reply += `- ${i.거점}: ${calc(ts)}\n`;
      });
      setMessages(m=>[...m, { role:'user', content:message }, { role:'assistant', content:reply }]);
      return;
    }

    // 기본
    handleSendMessage(message);
  };

  // 드래그/터치 이동
  const onTouchStart = (e: React.TouchEvent) => {
    e.preventDefault(); dragging.current = true; setIsResetting(false);
    offset.current = { x: e.touches[0].clientX - position.x, y: e.touches[0].clientY - position.y };
    lastTouchPos.current = position;
  };
  const onTouchMove = (e: React.TouchEvent) => {
    if (!dragging.current) return;
    e.preventDefault();
    const newPos = { x: e.touches[0].clientX - offset.current.x, y: e.touches[0].clientY - offset.current.y };
    setPosition(newPos); lastTouchPos.current = newPos;
  };
  const onTouchEnd = () => {
    dragging.current = false;
    const { innerWidth:w, innerHeight:h } = window;
    const bw=340, bh=400, { x, y } = lastTouchPos.current;
    if (x<0||y<0||x+bw>w||y+bh>h) {
      setIsResetting(true); setPosition(DEFAULT_POSITION);
    }
  };
  const handleTransitionEnd = () => {
    if (isResetting) setIsResetting(false);
  };

  const handleInputSubmit = () => {
    if (input.trim()) handleSendMessage(input.trim());
  };

  return (
    <div
      ref={chatWindowRef}
      className={`w-[340px] h-[430px] max-w-[95vw] bg-white rounded-2xl shadow-2xl z-40 flex flex-col overflow-hidden border border-gray-100${isResetting ? ' transition-all duration-300' : ''}`}
      style={{ position:"fixed", left:position.x, top:position.y }}
      onTransitionEnd={handleTransitionEnd}
    >
      {/* 상단바 */}
      <div
        className="flex items-center justify-center relative bg-[#5382E0] px-4 py-3 cursor-move select-none"
        onTouchStart={onTouchStart} onTouchMove={onTouchMove} onTouchEnd={onTouchEnd}
      >
        <span className="text-white text-xl font-bold">CENI</span>
        <button onClick={onClose} className="absolute right-4 top-1/2 -translate-y-1/2 text-white text-4xl font-bold">×</button>
      </div>

      {/* 메시지창 */}
      <div ref={chatContainerRef} className="flex-1 px-4 py-3 bg-[#f7faff] overflow-y-auto">
        {messages.map((message, idx) => (
          <div
            key={idx}
            ref={el => messageRefs.current[idx] = el}
            className={`flex ${message.role==='user'?'justify-end':''} ${idx===0?'':'mt-5'}`}
          >
            {message.role==='assistant' && <img src="/ceni-face.webp" alt="CENI" className="h-7 mr-2 object-cover" />}
            <div>
              <div className={`${message.role==='user'?'bg-[#5382E0] text-white rounded-xl rounded-tr-none':'bg-[#e6edfa] text-gray-900 rounded-xl rounded-tl-none'} px-4 py-2 text-sm max-w-[220px]`}>
                <ReactMarkdown>{message.content}</ReactMarkdown>
              </div>
              {idx===0 && (
                <div className="flex flex-col gap-2 mt-2">
                  <button onClick={()=>handleButtonClick('가장 빠른 셔틀 출발 시간')} className="bg-white border border-blue-200 text-blue-700 rounded-lg px-3 py-3 text-sm hover:bg-blue-50">가장 빠른 셔틀 출발 시간</button>
                  <button onClick={()=>handleButtonClick('셔틀 정류장 위치')}      className="bg-white border border-blue-200 text-blue-700 rounded-lg px-3 py-3 text-sm hover:bg-blue-50">셔틀 정류장 위치</button>
                  <button onClick={()=>handleButtonClick('셔틀 배차 간격')}      className="bg-white border border-blue-200 text-blue-700 rounded-lg px-3 py-3 text-sm hover:bg-blue-50">셔틀 배차 간격</button>
                </div>
              )}
            </div>
          </div>
        ))}
        {isLoading && (
          <div className="flex mt-5">
            <img src="/ceni-face.webp" alt="CENI" className="h-7 mr-2 object-cover" />
            <div className="bg-[#e6edfa] text-gray-900 rounded-xl rounded-tl-none px-4 py-2 text-sm">{loadingDots}</div>
          </div>
        )}
      </div>

      {/* 하단 입력창 */}
      <div className="flex items-center px-3 py-2 bg-white border-t border-gray-200">
        <input
          value={input}
          onChange={e=>setInput(e.target.value)}
          onKeyPress={e=>e.key==='Enter' && handleInputSubmit()}
          className="flex-1 px-3 py-2 mr-4 rounded-lg border border-gray-200 text-sm focus:outline-none"
          placeholder="세니에게 질문해 보세요!"
        />
        <button onClick={handleInputSubmit} disabled={isLoading||!input.trim()} className="disabled:opacity-50">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" className={!isLoading&&input.trim()?"text-[#5382E0]":"text-gray-400"}>
            <path d="M2.01 21L23 12L2.01 3L2 10L17 12L2 14L2.01 21Z" fill="currentColor"/>
          </svg>
        </button>
      </div>
    </div>
  );
}
