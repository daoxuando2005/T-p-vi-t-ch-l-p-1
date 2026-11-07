import React, { useState } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { 
  Home, 
  BookOpen, 
  PenTool, 
  ClipboardList, 
  LogOut, 
  Star
} from 'lucide-react';

const StudentLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  
  // Game stars mock/store
  const [stars] = useState(80);

  // Phát âm thanh khi bấm nút bằng Web Audio API
  const playClickSound = () => {
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      
      osc.type = 'sine';
      osc.frequency.setValueAtTime(587.33, audioCtx.currentTime); // D5 note
      osc.frequency.exponentialRampToValueAtTime(880.00, audioCtx.currentTime + 0.15); // A5 note
      
      gain.gain.setValueAtTime(0.1, audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.18);
      
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      
      osc.start();
      osc.stop(audioCtx.currentTime + 0.18);
    } catch (e) {
      console.log("Web Audio API not supported or blocked");
    }
  };

  const handleLogout = () => {
    playClickSound();
    logout();
    navigate('/login');
  };

  if (!user) return null;

  const menuItems = [
    { label: 'Trang Chủ', path: '/student', icon: Home, color: 'from-amber-400 to-orange-400', shadow: 'shadow-orange-300/40', emoji: '🏠' },
    { label: 'Đọc Chữ', path: '/student/alphabet', icon: BookOpen, color: 'from-sky-400 to-blue-500', shadow: 'shadow-blue-300/40', emoji: '📖' },
    { label: 'Tập Viết', path: '/student/practice', icon: PenTool, color: 'from-pink-400 to-rose-500', shadow: 'shadow-pink-300/40', emoji: '✏️' },
    { label: 'Bài Tập', path: '/student/classes', icon: ClipboardList, color: 'from-purple-400 to-violet-500', shadow: 'shadow-purple-300/40', emoji: '🏫' },
  ];

  return (
    <div 
      className="h-[100dvh] flex flex-col bg-white text-slate-700 selection:bg-pink-200 overflow-hidden relative"
      style={{ fontFamily: "'Nunito', sans-serif" }}
    >
      {/* Khung nền trang trí nhẹ (chỉ hiện trên Desktop) */}
      <div className="hidden sm:block absolute top-0 left-0 right-0 h-40 bg-gradient-to-b from-sky-50 to-transparent pointer-events-none z-0"></div>
      
      {/* Kid Header */}
      <header className="relative z-40 w-full max-w-7xl mx-auto sm:px-6 sm:pt-5 sticky top-0 bg-white/95 sm:bg-transparent backdrop-blur-md">
        <div className="sm:bg-white/80 sm:backdrop-blur-md sm:border-4 sm:border-sky-200 border-b-2 border-sky-100 sm:rounded-3xl p-3 sm:p-5 flex flex-row items-center justify-between gap-2 shadow-sm sm:shadow-xl sm:shadow-sky-100/50">
          {/* Logo & Mascot chào đón */}
          <div className="flex items-center gap-3 sm:gap-4 flex-1 min-w-0">
            <div className="w-12 h-12 sm:w-16 sm:h-16 rounded-xl sm:rounded-2xl bg-amber-400 border-2 sm:border-4 border-white flex items-center justify-center text-2xl sm:text-3xl shadow-md transform -rotate-3 hover:rotate-3 transition-transform shrink-0">
              🐻
            </div>
            <div className="min-w-0">
              <div className="hidden sm:block relative bg-amber-100 border-2 border-amber-300 text-amber-900 rounded-2xl px-4 py-1.5 text-xs font-bold shadow-sm mb-1 w-max">
                Chào bé {user.fullName}! Cùng học tiếng Việt nhé! ✨
                <div className="absolute top-1/2 -left-2 -translate-y-1/2 w-0 h-0 border-t-8 border-t-transparent border-r-8 border-r-amber-100 border-b-8 border-b-transparent"></div>
                <div className="absolute top-1/2 -left-2.5 -translate-y-1/2 w-0 h-0 border-t-[9px] border-t-transparent border-r-[9px] border-r-amber-300 border-b-[9px] border-b-transparent -z-10"></div>
              </div>
              <h1 className="text-sm sm:text-2xl font-black text-sky-600 sm:mt-1 uppercase tracking-tight sm:tracking-wide truncate">
                Học Chữ Tiếng Việt
              </h1>
              <p className="sm:hidden text-[10px] font-bold text-slate-400 truncate">Xin chào, {user.fullName}</p>
            </div>
          </div>

          {/* Gamification, Đăng xuất */}
          <div className="flex items-center gap-2 sm:gap-3 shrink-0">
            {/* Stars Scorecard */}
            <div className="flex items-center gap-1 sm:gap-2 bg-gradient-to-r from-amber-400 to-orange-400 text-white font-extrabold text-[10px] sm:text-sm px-2.5 py-1.5 sm:px-4 sm:py-2.5 rounded-full shadow-md sm:shadow-lg shadow-orange-200/50 border-2 border-white transform hover:scale-105 transition-transform">
              <Star className="w-3.5 h-3.5 sm:w-5 sm:h-5 fill-white animate-spin-slow" />
              <span>{stars} Sao</span>
            </div>

            {/* Logout button */}
            <button 
              onClick={handleLogout}
              className="p-1.5 sm:p-2.5 rounded-full bg-rose-50 hover:bg-rose-100 text-rose-500 sm:border-2 sm:border-rose-200 transition-all active:scale-95"
              title="Đăng xuất"
            >
              <LogOut className="w-4 h-4 sm:w-5 sm:h-5" />
            </button>
          </div>
        </div>
      </header>

      {/* Bubble Tabs Menu (Desktop) */}
      <nav className="relative z-10 w-full max-w-7xl mx-auto px-4 sm:px-6 mt-6 hidden sm:block">
        <div className="flex flex-wrap items-center justify-center gap-3.5">
          {menuItems.map((item) => {
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.path}
                to={item.path}
                onClick={playClickSound}
                className={`flex items-center gap-2.5 px-6 py-4 rounded-full text-sm font-black transition-all duration-200 border-4 ${
                  isActive 
                    ? `bg-gradient-to-r ${item.color} text-white border-white shadow-xl ${item.shadow} scale-105 transform`
                    : 'bg-white text-slate-500 border-sky-100 hover:border-sky-300 hover:text-sky-500 hover:scale-102 transform'
                }`}
              >
                <span className="text-base">{item.emoji}</span>
                <span>{item.label}</span>
              </Link>
            );
          })}
        </div>
      </nav>

      {/* Page Content area with scrollable inner container */}
      <main className="relative z-10 w-full max-w-7xl mx-auto sm:px-6 mt-2 sm:mt-6 flex-1 overflow-y-auto sm:overflow-visible pb-[80px] sm:pb-0 flex flex-col">
        <div className="bg-white sm:border-4 sm:border-sky-200 sm:rounded-[35px] px-4 py-4 sm:p-8 sm:shadow-xl sm:shadow-sky-100/30 flex-1 flex flex-col">
          {children}
        </div>
      </main>

      {/* Bottom Navigation (Mobile Only) */}
      <nav className="fixed bottom-0 left-0 right-0 bg-white/95 backdrop-blur-md border-t-2 border-sky-100 z-50 sm:hidden pb-safe">
        <div className="flex items-center justify-around p-1.5">
          {menuItems.map((item) => {
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.path}
                to={item.path}
                onClick={playClickSound}
                className={`flex flex-col items-center justify-center flex-1 py-1.5 gap-1 rounded-2xl transition-all ${
                  isActive ? 'bg-sky-50 shadow-sm' : 'bg-transparent'
                }`}
              >
                <div className={`text-2xl ${isActive ? 'scale-110 transform transition-transform' : 'grayscale opacity-70'}`}>
                  {item.emoji}
                </div>
                <span className={`text-[10px] font-black ${isActive ? 'text-sky-600' : 'text-slate-400'}`}>
                  {item.label}
                </span>
              </Link>
            );
          })}
        </div>
      </nav>
    </div>
  );
};

export default StudentLayout;
