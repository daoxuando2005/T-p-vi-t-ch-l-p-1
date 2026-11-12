import { useEffect, useRef, useState } from 'react';
import { NavLink, Link, useLocation } from 'react-router-dom';
import { Home, GraduationCap, ClipboardList, BookOpen, LogOut, Menu, X, Plus } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import '../teacher.css';

function Brand() {
  return <Link to="/teacher" className="teacher-brand" aria-label="Vui Học – trang giáo viên">
    <svg viewBox="0 0 64 64" width="48" height="48" aria-hidden="true"><ellipse cx="32" cy="33" rx="28" ry="29" fill="#1cb0f6"/><ellipse cx="32" cy="51" rx="18" ry="9" fill="#e5f3ff"/><circle cx="21" cy="25" r="12" fill="white"/><circle cx="43" cy="25" r="12" fill="white"/><circle cx="23" cy="25" r="5" fill="#263e50"/><circle cx="41" cy="25" r="5" fill="#263e50"/><path d="M26 38h12l-6 8z" fill="#ffc800"/></svg>
    <span><strong>Vui Học</strong><small>Góc giáo viên</small></span>
  </Link>;
}
export default function TeacherLayout({ children }) {
  const { user, logout } = useAuth(); const location = useLocation();
  const [open, setOpen] = useState(false); const dialog = useRef(null);
  const permissions = user?.roles || [];
  const items = [{ to: '/teacher', label: 'Tổng quan', icon: Home, end: true },
    ...(permissions.includes('PERM_MANAGE_CLASSES') ? [{ to: '/teacher/classes', label: 'Lớp học của cô', icon: GraduationCap }] : []),
    ...(permissions.includes('PERM_ASSIGN_HOMEWORK') ? [{ to: '/teacher/assignments', label: 'Bài tập & chấm bài', icon: ClipboardList }] : []),
    ...(permissions.includes('PERM_VIEW_ALPHABET') ? [{ to: '/teacher/alphabet', label: 'Học liệu 29 chữ', icon: BookOpen }] : [])];
  useEffect(() => { if (open && !dialog.current.open) dialog.current.showModal(); else if (!open && dialog.current.open) dialog.current.close(); }, [open]);
  useEffect(() => { setOpen(false); }, [location.pathname]);
  const navigation = <nav aria-label="Điều hướng giáo viên">{items.map(({ icon: Icon, ...item }) =>
    <NavLink key={item.to} to={item.to} end={item.end} className={({ isActive }) => isActive ? 'active' : ''}><Icon size={22}/><span>{item.label}</span></NavLink>)}</nav>;
  return <div className="teacher-shell">
    <aside className="teacher-sidebar"><Brand/>{navigation}<div className="teacher-sidebar-foot"><BookOpen size={25}/><strong>Cùng bé tiến bộ mỗi ngày</strong><p>Đọc, viết và khám phá tiếng Việt lớp 1.</p></div></aside>
    <dialog ref={dialog} className="teacher-menu-dialog" onCancel={() => setOpen(false)} onClose={() => setOpen(false)}>
      <div className="teacher-row"><Brand/><button className="teacher-icon" onClick={() => setOpen(false)} aria-label="Đóng menu"><X/></button></div>{navigation}
    </dialog>
    <div className="teacher-workspace">
      <header className="teacher-header"><button className="teacher-icon teacher-menu-toggle" onClick={() => setOpen(true)} aria-label="Mở menu" aria-expanded={open}><Menu/></button>
        <span className="teacher-header-label">Lớp 1 <span>·</span> Tiếng Việt</span>
        <div className="teacher-header-actions">{permissions.includes('PERM_ASSIGN_HOMEWORK') && <Link className="teacher-button primary" to="/teacher/assignments?new=1"><Plus size={18}/> Soạn bài</Link>}
          <span className="teacher-user">{user?.fullName || user?.username}</span>
          <button className="teacher-icon" onClick={logout} aria-label="Đăng xuất"><LogOut size={20}/></button>
        </div>
      </header>
      <main className="teacher-main" id="teacher-content">{children}</main>
    </div>
  </div>;
}
