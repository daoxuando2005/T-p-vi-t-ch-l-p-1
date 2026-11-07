import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  BookOpen, User, KeyRound, Mail, Phone, Shield,
  AlertCircle, CheckCircle, Sparkles, GraduationCap, Baby, Users
} from 'lucide-react';

const Register: React.FC = () => {
  const { register, verifyOtp } = useAuth();
  const navigate = useNavigate();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [role, setRole] = useState('STUDENT');

  const [step, setStep] = useState(1);
  const [otpCode, setOtpCode] = useState('');
  const [registeredEmail, setRegisteredEmail] = useState('');

  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username || !password || !fullName || !email) {
      setError('Vui lòng điền các trường bắt buộc');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const res = await register({ username, password, fullName, email, phoneNumber, role });
      setSuccess(res.message || 'Đăng ký thành công! Mã OTP đã được tạo.');
      setRegisteredEmail(email);
      setStep(2);
    } catch (err: any) {
      setError(err.message || 'Có lỗi xảy ra trong quá trình đăng ký');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!otpCode) { setError('Vui lòng nhập mã OTP'); return; }
    setLoading(true);
    setError('');
    try {
      const res = await verifyOtp(registeredEmail, otpCode);
      alert(res.message || 'Kích hoạt tài khoản thành công! Bạn có thể đăng nhập ngay.');
      navigate('/login');
    } catch (err: any) {
      setError(err.message || 'Kích hoạt OTP thất bại. Vui lòng kiểm tra lại.');
    } finally {
      setLoading(false);
    }
  };

  const roles = [
    { key: 'STUDENT', label: 'Học sinh', icon: Baby },
    { key: 'PARENT',  label: 'Phụ huynh', icon: Users },
    { key: 'TEACHER', label: 'Giáo viên', icon: GraduationCap },
  ];

  return (
    <div
      className="min-h-screen flex items-center justify-center p-4 bg-gradient-to-br from-sky-100 via-pink-100 to-amber-100"
      style={{ fontFamily: "'Nunito', sans-serif" }}
    >
      <div className="w-full max-w-lg">

        {/* Brand Header */}
        <div className="text-center mb-8">
          <div className="w-18 h-18 rounded-3xl bg-gradient-to-tr from-sky-450 to-blue-500 flex items-center justify-center shadow-xl shadow-blue-200/50 mx-auto mb-4 border-3 border-white" style={{ width: 72, height: 72 }}>
            <BookOpen className="w-9 h-9 text-white" />
          </div>
          <h1 className="text-3xl sm:text-4xl font-black text-sky-900 tracking-tight flex items-center justify-center gap-2">
            Học Chữ Tiếng Việt
            <Sparkles className="w-6 h-6 text-amber-500" />
          </h1>
          <p className="text-slate-500 text-sm font-bold mt-2">Hệ thống hỗ trợ học sinh lớp 1 luyện viết chữ</p>
        </div>

        {/* Card Panel */}
        <div className="bg-white/80 backdrop-blur-md rounded-[35px] border-4 border-sky-100 p-8 shadow-2xl relative overflow-hidden">
          <div className="absolute top-0 left-0 right-0 h-1.5 bg-gradient-to-r from-sky-400 via-pink-400 to-amber-400"></div>

          {step === 1 ? (
            <>
              <h2 className="text-xl sm:text-2xl font-black text-sky-800 mb-6">Đăng ký tài khoản mới</h2>

              {error && (
                <div className="mb-5 p-4 bg-rose-50 border-2 border-rose-200 rounded-2xl text-sm font-bold text-rose-600 flex items-center gap-3">
                  <AlertCircle className="w-5 h-5 shrink-0 text-rose-500" />
                  <span>{error}</span>
                </div>
              )}

              <form onSubmit={handleRegister} className="space-y-4" autoComplete="off">

                {/* Họ và tên */}
                <div>
                  <label className="block text-xs font-black text-sky-700/80 uppercase tracking-wider mb-1.5">
                    Họ và tên <span className="text-rose-500">*</span>
                  </label>
                  <div className="relative">
                    <span className="absolute inset-y-0 left-0 pl-4 flex items-center text-sky-400 pointer-events-none">
                      <User className="w-5 h-5" />
                    </span>
                    <input
                      type="text"
                      placeholder="Ví dụ: Nguyễn Văn An"
                      autoComplete="off"
                      value={fullName}
                      onChange={(e) => setFullName(e.target.value)}
                      className="w-full pl-12 pr-4 py-4 bg-white border-3 border-sky-100 focus:border-sky-400 focus:outline-none rounded-2xl text-slate-700 font-extrabold text-sm placeholder:text-slate-400/80 placeholder:font-bold transition-all shadow-inner"
                      required
                    />
                  </div>
                </div>

                {/* Tên đăng nhập + Mật khẩu (2 cột) */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-black text-sky-700/80 uppercase tracking-wider mb-1.5">
                      Tên đăng nhập <span className="text-rose-500">*</span>
                    </label>
                    <div className="relative">
                      <span className="absolute inset-y-0 left-0 pl-4 flex items-center text-sky-400 pointer-events-none">
                        <User className="w-5 h-5" />
                      </span>
                      <input
                        type="text"
                        placeholder="Tên đăng nhập của bạn"
                        autoComplete="off"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        className="w-full pl-12 pr-4 py-4 bg-white border-3 border-sky-100 focus:border-sky-400 focus:outline-none rounded-2xl text-slate-700 font-extrabold text-sm placeholder:text-slate-400/80 placeholder:font-bold transition-all shadow-inner"
                        required
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-black text-sky-700/80 uppercase tracking-wider mb-1.5">
                      Mật khẩu <span className="text-rose-500">*</span>
                    </label>
                    <div className="relative">
                      <span className="absolute inset-y-0 left-0 pl-4 flex items-center text-sky-400 pointer-events-none">
                        <KeyRound className="w-5 h-5" />
                      </span>
                      <input
                        type="password"
                        placeholder="Tối thiểu 6 ký tự"
                        autoComplete="new-password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        className="w-full pl-12 pr-4 py-4 bg-white border-3 border-sky-100 focus:border-sky-400 focus:outline-none rounded-2xl text-slate-700 font-extrabold text-sm placeholder:text-slate-400/80 placeholder:font-bold transition-all shadow-inner"
                        required
                      />
                    </div>
                  </div>
                </div>

                {/* Email + SĐT (2 cột) */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-black text-sky-700/80 uppercase tracking-wider mb-1.5">
                      Email kích hoạt <span className="text-rose-500">*</span>
                    </label>
                    <div className="relative">
                      <span className="absolute inset-y-0 left-0 pl-4 flex items-center text-sky-400 pointer-events-none">
                        <Mail className="w-5 h-5" />
                      </span>
                      <input
                        type="email"
                        placeholder="example@email.com"
                        autoComplete="off"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        className="w-full pl-12 pr-4 py-4 bg-white border-3 border-sky-100 focus:border-sky-400 focus:outline-none rounded-2xl text-slate-700 font-extrabold text-sm placeholder:text-slate-400/80 placeholder:font-bold transition-all shadow-inner"
                        required
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-black text-sky-700/80 uppercase tracking-wider mb-1.5">Số điện thoại</label>
                    <div className="relative">
                      <span className="absolute inset-y-0 left-0 pl-4 flex items-center text-sky-400 pointer-events-none">
                        <Phone className="w-5 h-5" />
                      </span>
                      <input
                        type="text"
                        placeholder="0123 456 789"
                        autoComplete="off"
                        value={phoneNumber}
                        onChange={(e) => setPhoneNumber(e.target.value)}
                        className="w-full pl-12 pr-4 py-4 bg-white border-3 border-sky-100 focus:border-sky-400 focus:outline-none rounded-2xl text-slate-700 font-extrabold text-sm placeholder:text-slate-400/80 placeholder:font-bold transition-all shadow-inner"
                      />
                    </div>
                  </div>
                </div>

                {/* Vai trò */}
                <div>
                  <label className="block text-xs font-black text-sky-700/80 uppercase tracking-wider mb-2">Bạn là ai?</label>
                  <div className="grid grid-cols-3 gap-3">
                    {roles.map(({ key, label, icon: Icon }) => (
                      <button
                        key={key}
                        type="button"
                        onClick={() => setRole(key)}
                        className={`py-3.5 px-2 rounded-2xl border-3 text-sm font-black transition-all flex flex-col items-center gap-1.5 ${
                          role === key
                            ? 'bg-sky-50 border-sky-400 text-sky-600 shadow-md shadow-sky-200/50'
                            : 'border-sky-100 bg-white text-slate-500 hover:border-sky-200 hover:bg-sky-50/50'
                        }`}
                      >
                        <Icon className={`w-5 h-5 ${role === key ? 'text-sky-500' : 'text-slate-400'}`} />
                        {label}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Submit */}
                <button
                  type="submit"
                  disabled={loading}
                  className="w-full py-4 px-6 rounded-2xl bg-gradient-to-r from-sky-400 to-blue-500 hover:from-sky-500 hover:to-blue-600 text-white font-black text-base border-2 border-white shadow-lg active:scale-[0.97] transition-all disabled:opacity-50 disabled:pointer-events-none mt-2 cursor-pointer"
                >
                  {loading ? 'Đang tạo tài khoản...' : 'Tiếp tục đăng ký →'}
                </button>
              </form>

              <div className="mt-6 text-center text-sm font-bold text-slate-500">
                Đã có tài khoản?{' '}
                <Link to="/login" className="text-sky-500 font-black hover:underline ml-1">
                  Đăng nhập ngay
                </Link>
              </div>
            </>
          ) : (
            <>
              <h2 className="text-xl sm:text-2xl font-black text-sky-800 mb-2">Xác thực mã kích hoạt OTP</h2>
              <p className="text-slate-500 text-sm font-bold mb-5">Mã OTP 6 số đã được tạo để kích hoạt tài khoản của bạn.</p>

              {success && (
                <div className="mb-5 p-4 bg-teal-50 border-2 border-teal-200 rounded-2xl text-sm font-bold text-teal-600 flex items-center gap-3">
                  <CheckCircle className="w-5 h-5 shrink-0 text-teal-500" />
                  <span>{success}</span>
                </div>
              )}

              {error && (
                <div className="mb-5 p-4 bg-rose-50 border-2 border-rose-200 rounded-2xl text-sm font-bold text-rose-600 flex items-center gap-3">
                  <AlertCircle className="w-5 h-5 shrink-0 text-rose-500" />
                  <span>{error}</span>
                </div>
              )}

              {/* OTP hint */}
              <div className="bg-amber-50 border-2 border-amber-200 rounded-2xl p-4 text-sm font-bold text-amber-700 mb-6 flex items-start gap-3">
                <Shield className="w-5 h-5 shrink-0 mt-0.5 text-amber-500" />
                <div>
                  <p className="font-black mb-1">Môi trường phát triển Local</p>
                  <p className="font-semibold text-amber-600/80">Mã OTP được in ra <strong>Console Log</strong> của Backend Spring Boot. Mở terminal backend để lấy mã 6 chữ số.</p>
                </div>
              </div>

              <form onSubmit={handleVerifyOtp} className="space-y-5">
                <div>
                  <label className="block text-xs font-black text-sky-700/80 uppercase tracking-wider mb-2">Mã OTP (6 chữ số)</label>
                  <input
                    type="text"
                    maxLength={6}
                    placeholder=""
                    value={otpCode}
                    onChange={(e) => setOtpCode(e.target.value)}
                    className="w-full text-center text-3xl tracking-[0.5em] font-black py-5 bg-white border-3 border-sky-100 focus:border-sky-400 focus:outline-none rounded-2xl text-sky-700 placeholder:text-slate-300 placeholder:tracking-[0.5em] transition-all shadow-inner"
                    required
                  />
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full py-4 px-6 rounded-2xl bg-gradient-to-r from-sky-400 to-blue-500 hover:from-sky-500 hover:to-blue-600 text-white font-black text-base border-2 border-white shadow-lg active:scale-[0.97] transition-all disabled:opacity-50 disabled:pointer-events-none cursor-pointer"
                >
                  {loading ? 'Đang kích hoạt...' : 'Kích hoạt tài khoản ✓'}
                </button>
              </form>

              <div className="mt-6 text-center">
                <button
                  type="button"
                  onClick={() => setStep(1)}
                  className="text-sm font-black text-sky-500 hover:underline"
                >
                  ← Quay lại chỉnh sửa thông tin
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default Register;
