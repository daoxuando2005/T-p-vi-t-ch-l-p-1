import { useState } from 'react';
import { useNavigate, Link, Navigate } from 'react-router-dom';
import { BookOpen, GraduationCap, ShieldCheck, UserRound, LockKeyhole, Eye, EyeOff, ArrowRight, AlertCircle } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { webHome } from '../../utils/webAccess';
import VuiBrand from '../../components/VuiBrand';
import TeacherDialog from '../../components/TeacherDialog';
import '../../teacher.css';
import '../../admin.css';

export default function Login() {
  const { login, user, notice } = useAuth(); const navigate = useNavigate();
  const [account, setAccount] = useState(''); const [password, setPassword] = useState(''); const [visible, setVisible] = useState(false);
  const [error, setError] = useState(''); const [busy, setBusy] = useState(false); const [help, setHelp] = useState(false);
  if (webHome(user?.roles)) return <Navigate to={webHome(user.roles)} replace/>;
  async function submit(event) {
    event.preventDefault(); if (busy) return; setError(''); setBusy(true);
    try { const result = await login(account.trim(), password); navigate(webHome(result.user.roles) || '/login', { replace: true }); }
    catch (failure) { setError(failure.message || 'Chưa đăng nhập được. Bạn thử lại nhé.'); }
    finally { setBusy(false); }
  }
  return <div className="teacher-shell auth-page"><div className="auth-frame">
    <section className="auth-story"><VuiBrand/><div className="auth-story-body"><span className="auth-eyebrow">MỖI NGÀY MỘT CHÚT TIẾN BỘ</span><h1>Cùng bé viết nên<br/><span>những điều hay.</span></h1><p>Một không gian để cô chuẩn bị bài học, nhà trường chăm chút học liệu và cùng gia đình dõi theo bé.</p>
      <div className="auth-illustration" aria-hidden="true"><div className="auth-letter tile-blue">a</div><div className="auth-letter tile-green">ă</div><div className="auth-letter tile-orange">â</div><svg viewBox="0 0 280 140"><path d="M28 95Q80 25 140 82T252 48" fill="none" stroke="#8ed7fa" strokeWidth="3" strokeDasharray="7 9"/><path d="m240 32 16 18-23 7" fill="none" stroke="#006590" strokeWidth="3" strokeLinecap="round"/></svg><span className="auth-book"><BookOpen size={42}/></span></div>
      <div className="auth-feature-list"><span><GraduationCap size={22}/> Bài học vừa sức, lời khen đúng lúc</span><span><ShieldCheck size={22}/> Tài khoản riêng, vai trò rõ ràng</span></div>
    </div><p className="auth-story-foot">Vui Học · Tiếng Việt lớp 1</p></section>
    <section className="auth-form-side"><div className="auth-login-card"><span className="auth-small-icon"><BookOpen size={24}/></span><h2>Chào mừng trở lại!</h2><p className="teacher-muted">Đăng nhập để mở không gian giáo viên hoặc quản trị viên.</p>
      {(error || notice) && <div className="teacher-alert auth-error" role="alert" id="login-error"><AlertCircle size={20}/><span>{error || notice}</span></div>}
      <form onSubmit={submit} className="teacher-stack" autoComplete="on">
        <label htmlFor="login-account">Tên đăng nhập hoặc email<div className="auth-input"><UserRound size={20}/><input id="login-account" name="username" type="text" autoComplete="username" value={account} onChange={e => setAccount(e.target.value)} placeholder="Nhập tài khoản của bạn" required maxLength={100} disabled={busy} aria-describedby={error ? 'login-error' : undefined}/></div></label>
        <label htmlFor="login-password">Mật khẩu<div className="auth-input"><LockKeyhole size={20}/><input id="login-password" name="password" type={visible ? 'text' : 'password'} autoComplete="current-password" value={password} onChange={e => setPassword(e.target.value)} placeholder="Nhập mật khẩu" required disabled={busy}/><button type="button" className="teacher-icon" aria-label={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} aria-pressed={visible} onClick={() => setVisible(!visible)}>{visible ? <EyeOff size={20}/> : <Eye size={20}/>}</button></div></label>
        <div className="auth-forgot"><button type="button" onClick={() => setHelp(true)}>Cần hỗ trợ đăng nhập?</button></div>
        <button className="teacher-button primary auth-submit" disabled={busy}>{busy ? 'Đang đăng nhập…' : 'Đăng nhập'}{!busy && <ArrowRight size={20}/>}</button>
      </form>
      <p className="auth-register">Chưa có tài khoản? <Link to="/register">Đăng ký</Link></p><div className="auth-role-note"><ShieldCheck size={22}/><p>Quyền truy cập được xác định theo tài khoản, không cần chọn vai trò khi đăng nhập.</p></div>
    </div><p className="auth-form-foot">Học từng chữ nhỏ, lớn lên mỗi ngày.</p></section>
  </div>{help && <TeacherDialog title="Hỗ trợ đăng nhập" onClose={() => setHelp(false)}><p>Bạn kiểm tra tên đăng nhập hoặc email, mật khẩu và phím Caps Lock.</p><p>Nếu quên mật khẩu hoặc tài khoản bị khóa, liên hệ quản trị viên để đặt mật khẩu mới hoặc mở khóa. Không gửi mật khẩu qua phần trò chuyện soạn bài.</p><button className="teacher-button primary" onClick={() => setHelp(false)}>Đã hiểu</button></TeacherDialog>}</div>;
}