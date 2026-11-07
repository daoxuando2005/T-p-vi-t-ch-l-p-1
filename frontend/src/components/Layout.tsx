import React, { useState } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import StudentLayout from './StudentLayout';
import { 
  BookOpen, 
  MessageSquare, 
  LogOut, 
  Home, 
  ClipboardList, 
  Shield, 
  Menu,
  X,
  GraduationCap
} from 'lucide-react';

const Layout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(true);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (!user) return null;

  if (user.roles.includes('ROLE_STUDENT')) {
    return <StudentLayout>{children}</StudentLayout>;
  }

  const getMenuItems = () => {
    const hasPerm = (perm: string) => user.roles.includes(perm) || user.roles.includes('ROLE_ADMIN');

    if (user.roles.includes('ROLE_TEACHER')) {
      const items = [
        { label: 'Thống kê tổng quan', path: '/teacher', icon: Home }
      ];
      if (hasPerm('PERM_MANAGE_CLASSES')) {
        items.push({ label: 'Quản lý lớp học', path: '/teacher/classes', icon: GraduationCap });
      }
      if (hasPerm('PERM_ASSIGN_HOMEWORK')) {
        items.push({ label: 'Giao bài & Chấm điểm', path: '/teacher/assignments', icon: ClipboardList });
      }
      if (hasPerm('PERM_CHAT_ACCESS')) {
        items.push({ label: 'Trao đổi tin nhắn', path: '/chat', icon: MessageSquare });
      }
      if (hasPerm('PERM_VIEW_ALPHABET')) {
        items.push({ label: 'Quản lý học liệu', path: '/teacher/alphabet', icon: BookOpen });
      }
      return items;
    }
    if (user.roles.includes('ROLE_PARENT')) {
      const items = [];
      if (hasPerm('PERM_PARENT_TRACKING')) {
        items.push({ label: 'Theo dõi tiến độ con', path: '/parent', icon: Home });
      }
      if (hasPerm('PERM_CHAT_ACCESS')) {
        items.push({ label: 'Trao đổi tin nhắn', path: '/chat', icon: MessageSquare });
      }
      if (hasPerm('PERM_ASSIGN_HOMEWORK')) {
        items.push({ label: 'Giao bài tập', path: '/parent/assignments', icon: ClipboardList });
      }
      return items;
    }
    if (user.roles.includes('ROLE_ADMIN')) {
      return [
        { label: 'Thống kê hệ thống', path: '/admin?tab=stats', icon: Home },
        { label: 'Quản lý người dùng', path: '/admin?tab=users', icon: Shield },
        { label: 'Quản lý học liệu (29 chữ)', path: '/admin?tab=alphabet', icon: BookOpen },
      ];
    }
    return [];
  };

  const menuItems = getMenuItems();

  const roleLabel = () => {
    if (user.roles.includes('ROLE_TEACHER')) return 'Giáo viên';
    if (user.roles.includes('ROLE_PARENT')) return 'Phụ huynh';
    if (user.roles.includes('ROLE_ADMIN')) return 'Quản trị viên';
    return 'Người dùng';
  };

  return (
    <div className="min-h-screen flex bg-sky-50/60" style={{ fontFamily: "'Nunito', sans-serif" }}>
      <aside
        className={`fixed top-0 bottom-0 left-0 z-30 w-[272px] bg-white border-r-2 border-sky-100 shadow-sm transition-transform duration-300 transform hidden md:flex flex-col translate-x-0`}
      >
        <div className="h-20 flex items-center justify-between px-5 border-b-2 border-sky-100 shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-sky-400 to-blue-500 flex items-center justify-center shadow-md">
              <BookOpen className="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 className="font-black text-base text-sky-800">Học Chữ Lớp 1</h1>
            </div>
          </div>
          <button className="md:hidden p-1.5 rounded-xl bg-sky-50 text-sky-400 hover:bg-sky-100" onClick={() => setSidebarOpen(false)}>
            <X className="w-5 h-5" />
          </button>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-1.5 overflow-y-auto">
          {menuItems.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.path || (location.pathname + location.search) === item.path;

            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-3.5 px-4 py-3 rounded-2xl text-xs font-black transition-all ${
                  isActive
                    ? 'bg-sky-50 text-sky-600 shadow-sm border-2 border-sky-100'
                    : 'text-slate-500 hover:bg-sky-50/50 hover:text-sky-500'
                }`}
              >
                <Icon className={`w-5 h-5 ${isActive ? 'text-sky-500' : 'text-slate-400'}`} />
                <span>{item.label}</span>
              </Link>
            );
          })}
        </nav>

        <div className="p-4 border-t-2 border-sky-100 shrink-0">
          <div className="flex items-center gap-3 bg-sky-50 rounded-2xl p-3">
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-sky-400 to-blue-500 flex items-center justify-center text-sm font-black text-white uppercase">
              {user.fullName.substring(0, 1)}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-black text-slate-700 truncate">{user.fullName}</p>
              <p className="text-xs text-sky-500 font-bold">{roleLabel()}</p>
            </div>
            <button onClick={handleLogout} className="p-2 rounded-xl text-slate-400 hover:text-rose-500 transition-colors" title="Đăng xuất">
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </aside>

      <div className="flex-1 flex flex-col min-h-screen pl-0 md:pl-[272px] pb-20 md:pb-0 w-full overflow-x-hidden">
        <header className="h-16 flex items-center justify-between px-6 bg-white border-b-2 border-sky-100 sticky top-0 z-20">
          <h2 className="text-sm font-black text-slate-500">Xin chào, <span className="text-sky-600">{user.fullName}</span> 👋</h2>
          <div className="flex items-center gap-3 relative">
            <div className="flex items-center gap-2">
              <span className="text-sm font-black text-slate-600 hidden lg:inline">{user.fullName}</span>
              <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-sky-400 to-blue-500 flex items-center justify-center font-black text-white text-sm shadow-md">
                {user.fullName.substring(0, 1)}
              </div>
            </div>
            <button onClick={handleLogout} className="p-2 rounded-xl text-slate-400 hover:text-rose-500 transition-colors">
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </header>

        <main className="flex-1 p-4 md:p-8 w-full max-w-full">
          {children}
        </main>
      </div>

      <nav className="fixed bottom-0 left-0 right-0 bg-white border-t-2 border-sky-100 z-50 md:hidden pb-safe">
        <div className="flex items-center justify-around p-2">
          {menuItems.slice(0, 5).map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.path || (location.pathname + location.search) === item.path;
            return (
              <Link key={item.path} to={item.path} className={`flex flex-col items-center justify-center w-full py-2 gap-1 rounded-2xl ${isActive ? 'bg-sky-50' : ''}`}>
                <Icon className={`w-6 h-6 ${isActive ? 'text-sky-500' : 'text-slate-400'}`} />
                <span className={`text-[9px] font-black ${isActive ? 'text-sky-600' : 'text-slate-400'}`}>{item.label}</span>
              </Link>
            );
          })}
        </div>
      </nav>
    </div>
  );
};

export default Layout;
