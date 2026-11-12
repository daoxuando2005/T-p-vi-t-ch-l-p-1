import { useEffect, useRef, useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { BookOpen, LogOut, Menu, X } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import VuiBrand from './VuiBrand';
import '../teacher.css';

export default function WorkspaceLayout({ children, items, home, label, action, admin = false }) {
  const { user, logout } = useAuth(); const location = useLocation();
  const [open, setOpen] = useState(false); const dialog = useRef(null);
  useEffect(() => { if (open && !dialog.current.open) dialog.current.showModal(); else if (!open && dialog.current.open) dialog.current.close(); }, [open]);
  useEffect(() => { setOpen(false); }, [location.pathname, location.search]);
  const navigation = <nav aria-label={admin ? 'Điều hướng quản trị' : 'Điều hướng giáo viên'}>{items.map(({ icon: Icon, ...item }) =>
    <NavLink key={item.to} to={item.to} end={item.end} className={({ isActive }) => isActive ? 'active' : ''}><Icon size={22}/><span>{item.label}</span></NavLink>)}</nav>;
  return <div className={`teacher-shell ${admin ? 'admin-shell' : ''}`}>
    <a className="workspace-skip" href="#teacher-content">Đến nội dung chính</a>
    <aside className="teacher-sidebar"><VuiBrand home={home} label={label}/>{navigation}<div className="teacher-sidebar-foot"><BookOpen size={25}/><strong>{admin ? 'Một nơi học tập được chăm chút' : 'Cùng bé tiến bộ mỗi ngày'}</strong><p>{admin ? 'Kết nối lớp học, gia đình và những bước tiến của bé.' : 'Đọc, viết và khám phá tiếng Việt lớp 1.'}</p></div></aside>
    <dialog ref={dialog} className="teacher-menu-dialog" onCancel={() => setOpen(false)} onClose={() => setOpen(false)}>
      <div className="teacher-row"><VuiBrand home={home} label={label}/><button className="teacher-icon" onClick={() => setOpen(false)} aria-label="Đóng menu"><X/></button></div>{navigation}
    </dialog>
    <div className="teacher-workspace">
      <header className="teacher-header"><button className="teacher-icon teacher-menu-toggle" onClick={() => setOpen(true)} aria-label="Mở menu" aria-expanded={open}><Menu/></button>
        <span className="teacher-header-label">{admin ? 'Không gian quản trị' : 'Lớp 1 · Tiếng Việt'}</span>
        <div className="teacher-header-actions">{action}<span className="teacher-user">{user?.fullName || user?.username}</span><button className="teacher-icon" onClick={logout} aria-label="Đăng xuất"><LogOut size={20}/></button></div>
      </header>
      <main className="teacher-main" id="teacher-content" tabIndex={-1}>{children}</main>
    </div>
  </div>;
}