import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { BookOpen, KeyRound, User, AlertCircle, Sparkles } from 'lucide-react';

const Login: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  
  const [usernameOrEmail, setUsernameOrEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!usernameOrEmail || !password) {
      setError('Vui lòng điền đầy đủ thông tin');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const res = await login(usernameOrEmail, password);
      const roles = res.user.roles;
      if (roles.includes('ROLE_STUDENT')) navigate('/student');
      else if (roles.includes('ROLE_TEACHER')) navigate('/teacher');
      else if (roles.includes('ROLE_PARENT')) navigate('/parent');
      else if (roles.includes('ROLE_ADMIN')) navigate('/admin');
      else navigate('/');
    } catch (err: any) {
      setError(err.message || 'Tên đăng nhập hoặc mật khẩu không chính xác.');
    } finally {
      setLoading(false);
    }
  };

  const playClickSound = () => {
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(523.25, audioCtx.currentTime); // C5
      osc.frequency.exponentialRampToValueAtTime(783.99, audioCtx.currentTime + 0.1); // G5
      gain.gain.setValueAtTime(0.05, audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.12);
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start();
      osc.stop(audioCtx.currentTime + 0.12);
    } catch (e) {}
  };

  return (
    <div 
      className="min-h-screen flex items-center justify-center p-4 bg-gradient-to-br from-sky-100 via-pink-100 to-amber-100"
      style={{ fontFamily: "'Nunito', sans-serif" }}
    >
      <div className="w-full max-w-md">
        
        {/* Brand Header */}
        <div className="text-center mb-8">
          <div className="w-18 h-18 rounded-3xl bg-gradient-to-tr from-sky-450 to-blue-500 flex items-center justify-center shadow-xl shadow-blue-200/50 mx-auto mb-4 animate-float border-3 border-white">
            <BookOpen className="w-9 h-9 text-white" />
          </div>
          <h1 className="text-3xl sm:text-4xl font-black text-sky-850 tracking-tight flex items-center justify-center gap-1">
            Học Chữ Tiếng Việt
            <Sparkles className="w-6 h-6 text-amber-500 animate-spin-slow" />
          </h1>
          <p className="text-slate-500 text-sm font-bold mt-2">Hệ thống hỗ trợ học sinh lớp 1 luyện viết chữ</p>
        </div>

        {/* Card Panel (Bảng trắng bo tròn lớn) */}
        <div className="bg-white/80 backdrop-blur-md rounded-[35px] border-4 border-sky-100 p-8 shadow-2xl relative overflow-hidden">
          <div className="absolute top-0 left-0 right-0 h-1.5 bg-gradient-to-r from-sky-400 via-pink-400 to-amber-400"></div>

          <h2 className="text-xl sm:text-2xl font-black text-sky-800 mb-6">Đăng nhập tài khoản</h2>

          {/* Sáng sủa tươi vui lỗi báo */}
          {error && (
            <div className="mb-6 p-4 bg-rose-50 border-2 border-rose-200 rounded-2xl text-xs sm:text-sm font-black text-rose-600 flex items-center gap-3 animate-bounce-short">
              <AlertCircle className="w-5 h-5 shrink-0 text-rose-500" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6" autoComplete="off">
            
            {/* Username/Email Input */}
            <div className="space-y-2">
              <label className="block text-xs sm:text-sm font-black text-sky-700/80 uppercase tracking-wider">Tài khoản hoặc Email</label>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 pl-4.5 flex items-center text-sky-400 pointer-events-none">
                  <User className="w-5 h-5" />
                </span>
                <input
                  type="text"
                  placeholder="Nhập tên đăng nhập của bé"
                  value={usernameOrEmail}
                  onChange={(e) => setUsernameOrEmail(e.target.value)}
                  className="w-full pl-13 pr-4 py-4 bg-white border-3 border-sky-100 focus:border-sky-400 focus:outline-none rounded-2xl text-slate-700 font-extrabold text-sm placeholder:text-slate-400/80 placeholder:font-bold transition-all shadow-inner"
                  autoComplete="off"
                  required
                />
              </div>
            </div>

            {/* Password Input */}
            <div className="space-y-2">
              <label className="block text-xs sm:text-sm font-black text-sky-700/80 uppercase tracking-wider">Mật khẩu của bé</label>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 pl-4.5 flex items-center text-sky-400 pointer-events-none">
                  <KeyRound className="w-5 h-5" />
                </span>
                <input
                  type="password"
                  placeholder="Nhập mật khẩu"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full pl-13 pr-4 py-4 bg-white border-3 border-sky-100 focus:border-sky-400 focus:outline-none rounded-2xl text-slate-700 font-extrabold text-sm placeholder:text-slate-400/80 placeholder:font-bold transition-all shadow-inner"
                  autoComplete="new-password"
                  required
                />
              </div>
            </div>

            {/* Submit Button (To tròn nổi bật) */}
            <button
              type="submit"
              onClick={playClickSound}
              disabled={loading}
              className="w-full py-4.5 px-6 rounded-2xl bg-gradient-to-r from-sky-400 to-blue-500 hover:from-sky-350 hover:to-blue-450 text-white font-black text-base border-2 border-white shadow-lg active:scale-[0.97] transition-all disabled:opacity-50 disabled:pointer-events-none mt-2 cursor-pointer"
            >
              {loading ? 'Bé chờ cô tí nhé...' : 'Bắt đầu học tập 🚀'}
            </button>
          </form>

          {/* Footer controls */}
          <div className="mt-8 text-center text-sm font-bold text-slate-500">
            Chưa có tài khoản?{' '}
            <Link to="/register" className="text-sky-500 font-black hover:underline ml-1">
              Đăng ký ngay
            </Link>
          </div>

        </div>
      </div>
    </div>
  );
};

export default Login;
