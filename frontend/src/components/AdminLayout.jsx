import { Link } from 'react-router-dom';
import { Home, Users, BookOpen, GraduationCap, Settings, UserPlus } from 'lucide-react';
import WorkspaceLayout from './WorkspaceLayout';
import '../admin.css';

export default function AdminLayout({ children }) {
  const items = [{ to: '/admin', label: 'Tổng quan', icon: Home, end: true }, { to: '/admin/users', label: 'Tài khoản & phân quyền', icon: Users },
    { to: '/admin/lessons', label: 'Học liệu 29 chữ', icon: BookOpen }, { to: '/admin/classes', label: 'Lớp học & gia đình', icon: GraduationCap }, { to: '/admin/settings', label: 'Cấu hình hệ thống', icon: Settings }];
  return <WorkspaceLayout admin home="/admin" label="Góc quản trị" items={items} action={<Link className="teacher-button primary" to="/admin/users?new=1"><UserPlus size={18}/> Tạo tài khoản</Link>}>{children}</WorkspaceLayout>;
}