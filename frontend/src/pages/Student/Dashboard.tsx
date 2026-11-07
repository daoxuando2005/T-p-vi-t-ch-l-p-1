import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { BookOpen, PenTool, ClipboardList } from 'lucide-react';

const StudentDashboard: React.FC = () => {
  const { user } = useAuth();

  // Phát âm thanh tiếng "ting" khi click
  const playInteractionSound = () => {
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(659.25, audioCtx.currentTime); // E5
      osc.frequency.exponentialRampToValueAtTime(987.77, audioCtx.currentTime + 0.12); // B5
      gain.gain.setValueAtTime(0.08, audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.15);
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start();
      osc.stop(audioCtx.currentTime + 0.15);
    } catch (e) {
      // Ignored
    }
  };

  return (
    <div className="flex flex-col h-full space-y-4 sm:space-y-6" style={{ fontFamily: "'Nunito', sans-serif" }}>
      
      {/* Banner / Mascot */}
      <div className="bg-gradient-to-r from-amber-100 via-orange-50 to-pink-100 border-2 sm:border-4 border-amber-300 rounded-3xl sm:rounded-[30px] p-4 sm:p-8 flex items-center gap-4 sm:gap-6 relative overflow-hidden shadow-sm shrink-0">
        <div className="absolute top-0 right-0 w-24 h-24 bg-amber-200/30 rounded-full -mr-8 -mt-8 pointer-events-none"></div>
        <div className="w-16 h-16 sm:w-24 sm:h-24 bg-white rounded-full border-2 sm:border-4 border-amber-300 flex items-center justify-center text-3xl sm:text-5xl shadow-md shrink-0 transform hover:scale-110 transition-transform relative group z-10">
          🐰
          <span className="absolute -top-1 -right-2 bg-pink-500 text-white font-extrabold text-[9px] sm:text-[10px] px-1.5 sm:px-2 py-0.5 rounded-full animate-bounce">Wow!</span>
        </div>
        <div className="z-10">
          <h1 className="text-xl sm:text-3xl font-black text-amber-800 leading-tight">
            Chào {user?.fullName}! 👋
          </h1>
          <p className="text-amber-900/80 text-xs sm:text-base font-bold mt-1">Bé muốn học gì hôm nay nào?</p>
        </div>
      </div>

      {/* 3 Nút Bấm Khổng Lồ (App Launcher) */}
      <div className="flex-1 grid grid-rows-3 gap-4 pb-2">
        
        {/* Nút Học Chữ Cái */}
        <Link 
          to="/student/alphabet" 
          onClick={playInteractionSound}
          className="group bg-gradient-to-r from-sky-400 to-blue-500 border-4 border-white shadow-md hover:shadow-xl rounded-3xl p-4 sm:p-6 flex items-center gap-5 relative overflow-hidden transform hover:-translate-y-1 transition-all active:scale-95"
        >
          <div className="absolute right-0 top-1/2 -translate-y-1/2 text-9xl opacity-20 transform rotate-12 pointer-events-none group-hover:rotate-0 transition-transform duration-500">
            📖
          </div>
          <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl bg-white/20 border-2 border-white/50 backdrop-blur-sm flex items-center justify-center text-4xl sm:text-5xl shadow-inner shrink-0 z-10">
            🅰️
          </div>
          <div className="z-10 flex-1">
            <h3 className="font-black text-xl sm:text-2xl text-white drop-shadow-md">ĐỌC CHỮ CÁI</h3>
            <p className="text-white/90 text-xs sm:text-sm font-bold mt-0.5">Làm quen với 29 chữ cái</p>
          </div>
          <div className="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center text-white backdrop-blur-sm z-10 sm:hidden">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M9 5l7 7-7 7" /></svg>
          </div>
        </Link>

        {/* Nút Luyện Viết */}
        <Link 
          to="/student/practice" 
          onClick={playInteractionSound}
          className="group bg-gradient-to-r from-pink-400 to-rose-500 border-4 border-white shadow-md hover:shadow-xl rounded-3xl p-4 sm:p-6 flex items-center gap-5 relative overflow-hidden transform hover:-translate-y-1 transition-all active:scale-95"
        >
          <div className="absolute right-0 top-1/2 -translate-y-1/2 text-9xl opacity-20 transform -rotate-12 pointer-events-none group-hover:rotate-0 transition-transform duration-500">
            ✏️
          </div>
          <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl bg-white/20 border-2 border-white/50 backdrop-blur-sm flex items-center justify-center text-4xl sm:text-5xl shadow-inner shrink-0 z-10">
            🖍️
          </div>
          <div className="z-10 flex-1">
            <h3 className="font-black text-xl sm:text-2xl text-white drop-shadow-md">TẬP VIẾT CHỮ</h3>
            <p className="text-white/90 text-xs sm:text-sm font-bold mt-0.5">Luyện tô nét chữ cái</p>
          </div>
          <div className="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center text-white backdrop-blur-sm z-10 sm:hidden">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M9 5l7 7-7 7" /></svg>
          </div>
        </Link>

        {/* Nút Bài Tập / Lớp của Bé */}
        <Link 
          to="/student/classes" 
          onClick={playInteractionSound}
          className="group bg-gradient-to-r from-purple-400 to-violet-500 border-4 border-white shadow-md hover:shadow-xl rounded-3xl p-4 sm:p-6 flex items-center gap-5 relative overflow-hidden transform hover:-translate-y-1 transition-all active:scale-95"
        >
          <div className="absolute right-0 top-1/2 -translate-y-1/2 text-9xl opacity-20 transform rotate-12 pointer-events-none group-hover:rotate-0 transition-transform duration-500">
            🏫
          </div>
          <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl bg-white/20 border-2 border-white/50 backdrop-blur-sm flex items-center justify-center text-4xl sm:text-5xl shadow-inner shrink-0 z-10">
            🎒
          </div>
          <div className="z-10 flex-1">
            <h3 className="font-black text-xl sm:text-2xl text-white drop-shadow-md">VÀO LỚP HỌC</h3>
            <p className="text-white/90 text-xs sm:text-sm font-bold mt-0.5">Làm bài tập cô giao</p>
          </div>
          <div className="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center text-white backdrop-blur-sm z-10 sm:hidden">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M9 5l7 7-7 7" /></svg>
          </div>
        </Link>

      </div>
    </div>
  );
};

export default StudentDashboard;
