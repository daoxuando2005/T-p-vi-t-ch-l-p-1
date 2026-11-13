import { useRef, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { BookOpen, UserRoundPlus, ShieldCheck, CheckCircle2, Eye, EyeOff, ArrowRight } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { webHome } from '../../utils/webAccess';
import VuiBrand from '../../components/VuiBrand';
import '../../teacher.css';
import '../../admin.css';

export default function Register() {
  const { register, user } = useAuth();
  const [values, setValues] = useState({ fullName: '', username: '', email: '', password: '', confirmation: '', role: 'TEACHER' });
  const [visible, setVisible] = useState(false); const [busy, setBusy] = useState(false);
  const [errors, setErrors] = useState({}); const [error, setError] = useState(''); const [done, setDone] = useState(false);
  const summary = useRef(null); const success = useRef(null);
  if (webHome(user?.roles)) return <Navigate to={webHome(user.roles)} replace/>;
  const change = (name, value) => { setValues(v => ({ ...v, [name]: value })); setErrors(e => ({ ...e, [name]: undefined })); setError(''); };
  async function submit(event) {
    event.preventDefault(); if (busy) return;
    const invalid = {};
    if (!values.fullName.trim() || values.fullName.trim().length > 100) invalid.fullName = 'Nhập họ tên, tối đa 100 ký tự.';
    if (!/^[A-Za-z0-9_.-]{3,50}$/.test(values.username.trim())) invalid.username = 'Dùng 3–50 chữ không dấu, số hoặc dấu . _ -';
    if (values.email.trim().length > 100 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim())) invalid.email = 'Nhập địa chỉ email hợp lệ.';
    if (values.password.length < 6 || new TextEncoder().encode(values.password).length > 72) invalid.password = 'Mật khẩu cần từ 6 ký tự, tối đa 72 byte.';
    if (values.password !== values.confirmation) invalid.confirmation = 'Hai mật khẩu chưa khớp.';
    setErrors(invalid); setError('');
    if (Object.keys(invalid).length) { requestAnimationFrame(() => summary.current?.focus()); return; }
    setBusy(true);
    try {
      await register({ fullName: values.fullName.trim(), username: values.username.trim(), email: values.email.trim(), password: values.password, role: values.role });
      setValues(v => ({ ...v, password: '', confirmation: '' })); setDone(true);
      requestAnimationFrame(() => success.current?.focus());
    } catch (failure) { setError(failure.message || 'Chưa tạo được tài khoản. Bạn thử lại nhé.'); requestAnimationFrame(() => summary.current?.focus()); }
    finally { setBusy(false); }
  }
  function field(name, label, type, complete, maxLength) {
    const password = name === 'password';
    return <label htmlFor={`register-${name}`}>{label}<div className="auth-input">
      <input id={`register-${name}`} name={name} type={password && visible ? 'text' : type} autoComplete={complete} value={values[name]} onChange={e => change(name, e.target.value)} disabled={busy} maxLength={maxLength} aria-required="true" aria-invalid={!!errors[name]} aria-describedby={errors[name] ? `register-${name}-error` : password ? 'password-help' : undefined}/>
      {password && <button type="button" className="teacher-icon" disabled={busy} aria-label={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} aria-pressed={visible} onClick={() => setVisible(v => !v)}>{visible ? <EyeOff size={20}/> : <Eye size={20}/>}</button>}
    </div>{errors[name] && <small className="register-field-error" id={`register-${name}-error`}>{errors[name]}</small>}{password && <small id="password-help" className="teacher-muted">Từ 6 ký tự. Giữ riêng mật khẩu của bạn.</small>}</label>;
  }
  return <div className="teacher-shell auth-page"><div className="auth-frame auth-register-frame">
    <section className="auth-story"><VuiBrand/><div className="auth-story-body"><span className="auth-eyebrow">BẮT ĐẦU HÀNH TRÌNH MỚI</span><h1>Mỗi chữ nhỏ,<br/><span>một niềm vui.</span></h1><p>Tạo tài khoản riêng để cùng bé khám phá tiếng Việt, luyện tập mỗi ngày và ghi nhận từng bước tiến bộ.</p><div className="auth-feature-list"><span><BookOpen size={22}/> Học và luyện tập theo từng chữ</span><span><ShieldCheck size={22}/> Dữ liệu riêng theo tài khoản</span></div></div><p className="auth-story-foot">Vui Học · Tiếng Việt lớp 1</p></section>
    <section className="auth-form-side"><div className="auth-login-card">
      {done ? <div className="teacher-stack" ref={success} tabIndex={-1} role="status"><span className="auth-small-icon"><CheckCircle2 size={28}/></span><h2>Tài khoản đã sẵn sàng!</h2><p>Bạn có thể đăng nhập ngay bằng tên <strong>{values.username.trim()}</strong>.</p><Link className="teacher-button primary auth-submit" to="/login">Đăng nhập <ArrowRight size={20}/></Link></div> : <>
        <span className="auth-small-icon"><UserRoundPlus size={24}/></span><h2>Tạo tài khoản</h2><p className="teacher-muted">Chọn vai trò và điền thông tin của bạn.</p>
        {(error || Object.values(errors).some(Boolean)) && <div className="teacher-alert auth-error" role="alert" ref={summary} tabIndex={-1}><div><strong>{error || 'Kiểm tra lại các thông tin sau:'}</strong>{Object.entries(errors).filter(([, message]) => message).map(([name, message]) => <p key={name}><a href={`#register-${name}`}>{message}</a></p>)}</div></div>}
        <form className="teacher-stack" onSubmit={submit} noValidate autoComplete="on" aria-busy={busy}>
          <label htmlFor="register-role">Bạn là<select id="register-role" name="role" value={values.role} onChange={e => change('role', e.target.value)} disabled={busy}><option value="TEACHER">Giáo viên</option><option value="STUDENT">Học sinh</option><option value="PARENT">Phụ huynh</option></select></label>
          {field('fullName', 'Họ và tên', 'text', 'name', 100)}{field('username', 'Tên đăng nhập', 'text', 'username', 50)}{field('email', 'Email', 'email', 'email', 100)}{field('password', 'Mật khẩu', 'password', 'new-password', 72)}{field('confirmation', 'Nhập lại mật khẩu', 'password', 'new-password', 72)}
          <button className="teacher-button primary auth-submit" disabled={busy}>{busy ? 'Đang tạo tài khoản…' : 'Tạo tài khoản'}{!busy && <ArrowRight size={20}/>}</button>
        </form><p className="auth-register">Đã có tài khoản? <Link to="/login">Đăng nhập</Link></p>
      </>}
    </div></section>
  </div></div>;
}
