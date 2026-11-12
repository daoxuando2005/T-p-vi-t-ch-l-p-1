import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Search, UserPlus, Users, RefreshCw, KeyRound, ShieldCheck, LockKeyhole, Unlock, Trash2, ArrowLeft, ArrowRight, Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import TeacherDialog from '../../components/TeacherDialog';
import { teacherError } from '../../utils/teacherAssignments';
import { ADMIN_ROLES, TEACHER_PERMISSIONS, primaryRole, roleLabel, filterUsers, passwordError } from '../../utils/adminWorkspace';

const emptyUser = () => ({ username: '', fullName: '', email: '', password: '', role: 'STUDENT', permissions: [] });
export default function AdminUsers() {
  const { user: currentUser, logout, refreshUser } = useAuth(); const [params, setParams] = useSearchParams();
  const [users, setUsers] = useState([]); const [query, setQuery] = useState(''); const [role, setRole] = useState('ALL'); const [status, setStatus] = useState('ALL'); const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true); const [revision, setRevision] = useState(0); const [error, setError] = useState(''); const [notice, setNotice] = useState('');
  const [form, setForm] = useState(null); const [action, setAction] = useState(null); const [busy, setBusy] = useState(false); const [formError, setFormError] = useState(''); const [showPassword, setShowPassword] = useState(false);
  useEffect(() => { const abort = new AbortController(); setLoading(true); setError('');
    api.get('/admin/users', { signal: abort.signal }).then(r => { if (!Array.isArray(r.data)) throw new Error('Danh sách tài khoản chưa hợp lệ.'); setUsers(r.data); })
      .catch(e => { if (!abort.signal.aborted) setError(teacherError(e)); }).finally(() => { if (!abort.signal.aborted) setLoading(false); }); return () => abort.abort();
  }, [revision]);
  useEffect(() => { if (params.get('new') === '1') { setForm(emptyUser()); setFormError(''); setShowPassword(false); const next = new URLSearchParams(params); next.delete('new'); setParams(next, { replace: true }); } }, [params, setParams]);
  const filtered = filterUsers(users, query, role, status); const pages = Math.max(1, Math.ceil(filtered.length / 10)); const currentPage = Math.min(page, pages - 1); const visible = filtered.slice(currentPage * 10, (currentPage + 1) * 10);
  function open(target) { setFormError(''); setShowPassword(false); setAction(null); setForm(target ? { ...target, role: primaryRole(target), permissions: target.roles.filter(r => r.startsWith('PERM_')), fullName: target.fullName || '', email: target.email || '', phoneNumber: target.phoneNumber || '' } : emptyUser()); }
  async function mutate(callback, message, selfReset = false) {
    setBusy(true); setFormError('');
    try { const reply = await callback(); if (!reply.data.success) throw new Error(reply.data.message || 'Chưa lưu được thay đổi.');
      if (selfReset) { logout(); return; }
      setNotice(message || reply.data.message); setForm(null); setAction(null); setShowPassword(false); setRevision(n => n + 1);
    } catch (e) { setFormError(teacherError(e)); } finally { setBusy(false); }
  }
  function save(event) {
    event.preventDefault();
    if (!form.id) { const validation = passwordError(form.password); if (validation) { setFormError(validation); return; }
      mutate(() => api.post('/admin/users', form), 'Đã tạo tài khoản. Bạn có thể bàn giao thông tin đăng nhập cho người dùng.');
    } else mutate(async () => { const reply = await api.put(`/admin/users/${form.id}/info`, { fullName: form.fullName, email: form.email, phoneNumber: form.phoneNumber }); if (reply.data.success && form.id === currentUser.id) await refreshUser(); return reply; }, 'Đã cập nhật thông tin tài khoản.');
  }
  function chooseAction(kind, target) {
    if (busy) return;
    setFormError(''); setShowPassword(false); setForm(null);
    setAction({ kind, target, role: primaryRole(target), password: '', permissions: target.roles.filter(r => TEACHER_PERMISSIONS.some(p => p.value === r)) });
  }
  function confirmAction(event) {
    event.preventDefault(); const id = action.target.id;
    if (action.kind === 'password') { const validation = passwordError(action.password); if (validation) { setFormError(validation); return; }
      mutate(() => api.post(`/admin/users/${id}/password`, { password: action.password }), 'Đã đặt lại mật khẩu và kết thúc hiệu lực các phiên cũ.', id === currentUser.id);
    } else if (action.kind === 'permissions') mutate(() => api.put(`/admin/users/${id}/permissions`, action.permissions));
    else if (action.kind === 'role') mutate(() => api.put(`/admin/users/${id}/role`, null, { params: { role: action.role } }));
    else if (action.kind === 'delete') mutate(() => api.delete(`/admin/users/${id}`));
    else mutate(() => api.put(`/admin/users/${id}/toggle`));
  }
  const titles = { password: 'Đặt mật khẩu mới', permissions: 'Quyền giáo viên', role: 'Đổi vai trò', delete: 'Xóa tài khoản chưa có dữ liệu', toggle: action?.target.enabled ? 'Khóa tài khoản?' : 'Mở khóa tài khoản?' };
  return <>
    <div className="teacher-page-head"><div><span className="admin-eyebrow">CON NGƯỜI & KẾT NỐI</span><h1>Tài khoản & phân quyền</h1><p className="teacher-muted">Mỗi người một tài khoản, đúng vai trò để cùng đồng hành với bé.</p></div><button className="teacher-button primary" onClick={() => open()}><UserPlus size={18}/> Tạo tài khoản</button></div>
    {error && <p className="teacher-alert" role="alert">{error}</p>}{notice && <p className="teacher-success" role="status">{notice}</p>}
    <section className="teacher-card"><div className="admin-filter-bar"><label className="admin-search">Tìm tài khoản<div className="admin-search-input"><Search size={19}/><input value={query} onChange={e => { setQuery(e.target.value); setPage(0); }} placeholder="Tên, tài khoản hoặc email…"/></div></label><label>Vai trò<select value={role} onChange={e => { setRole(e.target.value); setPage(0); }}><option value="ALL">Tất cả vai trò</option>{ADMIN_ROLES.map(r => <option key={r.value} value={r.value}>{r.label}</option>)}</select></label><label>Trạng thái<select value={status} onChange={e => { setStatus(e.target.value); setPage(0); }}><option value="ALL">Tất cả trạng thái</option><option value="ACTIVE">Đang hoạt động</option><option value="LOCKED">Đang khóa</option></select></label><button className="teacher-icon" onClick={() => setRevision(n => n + 1)} disabled={loading} aria-label="Tải lại tài khoản"><RefreshCw size={19}/></button></div>
      {loading ? <p className="teacher-loading" role="status">Đang tải tài khoản…</p> : !visible.length ? <div className="teacher-empty"><Users size={42}/><h3>Chưa có tài khoản phù hợp</h3><p>Thử tên khác hoặc thay bộ lọc để tìm lại.</p></div> : <div className="teacher-table-scroll"><table className="teacher-table admin-user-table"><thead><tr><th>Người dùng</th><th>Vai trò</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>{visible.map(target => <tr key={target.id}><td><div className="admin-person"><span className={`admin-avatar ${primaryRole(target).toLowerCase()}`}>{(target.fullName || target.username || '?').slice(0, 1)}</span><div><strong>{target.fullName || target.username}{target.id === currentUser.id && <small className="admin-you">Bạn</small>}</strong><span>@{target.username}</span><small>{target.email || 'Chưa có email'}</small></div></div></td><td><span className={`teacher-chip admin-role-${primaryRole(target).toLowerCase()}`}>{roleLabel(target)}</span></td><td><span className={`admin-status ${target.enabled ? 'active' : 'locked'}`}><i/>{target.enabled ? 'Hoạt động' : 'Đang khóa'}</span></td><td><button className="teacher-button" onClick={() => open(target)}>Chi tiết</button></td></tr>)}</tbody></table></div>}
      <div className="admin-pagination"><span>{filtered.length} tài khoản · Trang {currentPage + 1}/{pages}</span><div><button className="teacher-icon" disabled={currentPage === 0} onClick={() => setPage(currentPage - 1)} aria-label="Trang trước"><ArrowLeft size={18}/></button><button className="teacher-icon" disabled={currentPage + 1 >= pages} onClick={() => setPage(currentPage + 1)} aria-label="Trang sau"><ArrowRight size={18}/></button></div></div>
    </section>
    {form && <TeacherDialog title={form.id ? 'Thông tin tài khoản' : 'Tạo tài khoản mới'} busy={busy} onClose={() => setForm(null)} wide><form onSubmit={save}>
      {formError && <p className="teacher-alert" role="alert">{formError}</p>}<div className="teacher-field-grid"><label>Họ và tên<input autoFocus required maxLength={form.id ? 100 : 50} value={form.fullName} onChange={e => setForm({ ...form, fullName: e.target.value })} disabled={busy}/></label><label>Tên đăng nhập<input required pattern="[a-zA-Z0-9_.\-]{3,20}" maxLength={20} value={form.username} onChange={e => setForm({ ...form, username: e.target.value })} disabled={busy || !!form.id}/></label><label>Email<input required type="email" maxLength={100} value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} disabled={busy}/></label>{form.id && <label>Số điện thoại<input type="tel" maxLength={20} value={form.phoneNumber} onChange={e => setForm({ ...form, phoneNumber: e.target.value })} disabled={busy}/></label>}
      {!form.id && <><label>Vai trò<select value={form.role} disabled={busy} onChange={e => setForm({ ...form, role: e.target.value, permissions: e.target.value === 'TEACHER' ? TEACHER_PERMISSIONS.map(p => p.value) : [] })}>{ADMIN_ROLES.map(r => <option key={r.value} value={r.value}>{r.label}</option>)}</select></label><label>Mật khẩu khởi tạo<div className="auth-input"><input required minLength={6} autoComplete="new-password" type={showPassword ? 'text' : 'password'} value={form.password} disabled={busy} onChange={e => setForm({ ...form, password: e.target.value })}/><button type="button" className="teacher-icon" aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} onClick={() => setShowPassword(!showPassword)}>{showPassword ? <EyeOff size={18}/> : <Eye size={18}/>}</button></div></label></>}
      </div>{!form.id && form.role === 'TEACHER' && <PermissionFields selected={form.permissions} busy={busy} onChange={permissions => setForm({ ...form, permissions })}/>}
      <div className="teacher-form-actions"><button type="button" className="teacher-button" onClick={() => setForm(null)} disabled={busy}>Đóng</button><button className="teacher-button primary" disabled={busy}>{busy ? 'Đang lưu…' : form.id ? 'Lưu thông tin' : 'Tạo tài khoản'}</button></div>
    </form>{form.id && <section className="admin-account-actions"><h3>Quản lý tài khoản</h3><p className="teacher-muted">Khóa, đổi quyền và đặt mật khẩu mới sẽ kết thúc hiệu lực các phiên cũ. Tài khoản có lịch sử được giữ lại để bảo toàn dữ liệu.</p><div className="teacher-row"><button className="teacher-button" onClick={() => chooseAction('password', form)}><KeyRound size={17}/> Đặt mật khẩu mới</button>{form.role === 'TEACHER' && <button className="teacher-button" onClick={() => chooseAction('permissions', form)}><ShieldCheck size={17}/> Quyền giáo viên</button>}{form.id !== currentUser.id && <><button className="teacher-button" onClick={() => chooseAction('role', form)}>Đổi vai trò</button><button className="teacher-button" onClick={() => chooseAction('toggle', form)}>{form.enabled ? <LockKeyhole size={17}/> : <Unlock size={17}/>} {form.enabled ? 'Khóa tài khoản' : 'Mở khóa'}</button><button className="teacher-button danger" onClick={() => chooseAction('delete', form)}><Trash2 size={17}/> Xóa tài khoản</button></>}</div></section>}</TeacherDialog>}
    {action && <TeacherDialog title={titles[action.kind]} busy={busy} onClose={() => setAction(null)}><form className="teacher-stack" onSubmit={confirmAction}><p>Tài khoản <strong>{action.target.fullName || action.target.username}</strong> · @{action.target.username}</p>{formError && <p className="teacher-alert" role="alert">{formError}</p>}
      {action.kind === 'password' ? <><p className="teacher-muted">Người dùng cần đăng nhập bằng mật khẩu mới. {action.target.id === currentUser.id && 'Bạn sẽ trở về trang đăng nhập sau khi lưu.'}</p><label>Mật khẩu mới<div className="auth-input"><input autoFocus required minLength={6} type={showPassword ? 'text' : 'password'} autoComplete="new-password" value={action.password} disabled={busy} onChange={e => setAction({ ...action, password: e.target.value })}/><button type="button" className="teacher-icon" aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} onClick={() => setShowPassword(!showPassword)}>{showPassword ? <EyeOff size={18}/> : <Eye size={18}/>}</button></div></label></> : action.kind === 'permissions' ? <PermissionFields selected={action.permissions} busy={busy} onChange={permissions => setAction({ ...action, permissions })}/> : action.kind === 'role' ? <><p className="teacher-muted">Chỉ đổi vai trò khi tài khoản chưa có dữ liệu lớp học, gia đình hoặc tiến độ. Các quyền cũ được thay bằng quyền mặc định của vai trò mới.</p><label>Vai trò mới<select value={action.role} onChange={e => setAction({ ...action, role: e.target.value })} disabled={busy}>{ADMIN_ROLES.map(r => <option key={r.value} value={r.value}>{r.label}</option>)}</select></label></> : <p className="teacher-muted">{action.kind === 'delete' ? 'Chỉ xóa tài khoản chưa có dữ liệu liên quan. Nếu đã có lịch sử, hãy khóa tài khoản để giữ dữ liệu.' : action.target.enabled ? 'Người dùng sẽ không đăng nhập hoặc gọi API bằng phiên cũ được nữa. Tiến độ và bài nộp vẫn được giữ.' : 'Người dùng có thể đăng nhập lại. Các phiên trước khi khóa vẫn không được phục hồi.'}</p>}
      <div className="teacher-form-actions"><button type="button" className="teacher-button" disabled={busy} onClick={() => setAction(null)}>Hủy</button><button className={`teacher-button ${action.kind === 'delete' ? 'danger' : 'primary'}`} disabled={busy}>{busy ? 'Đang lưu…' : 'Xác nhận'}</button></div>
    </form></TeacherDialog>}
  </>;
}
function PermissionFields({ selected, onChange, busy }) {
  function toggle(item, checked) {
    let next = checked ? [...selected, item] : selected.filter(p => p !== item);
    if (item === 'PERM_EDIT_ALPHABET' && checked) next = [...new Set([...next, 'PERM_VIEW_ALPHABET'])];
    if (item === 'PERM_VIEW_ALPHABET' && !checked) next = next.filter(p => p !== 'PERM_EDIT_ALPHABET');
    onChange(next);
  }
  return <fieldset className="admin-permissions"><legend>Quyền giáo viên</legend>{TEACHER_PERMISSIONS.map(item => <label key={item.value}><input type="checkbox" checked={selected.includes(item.value)} disabled={busy} onChange={e => toggle(item.value, e.target.checked)}/><span>{item.label}</span></label>)}</fieldset>;
}
