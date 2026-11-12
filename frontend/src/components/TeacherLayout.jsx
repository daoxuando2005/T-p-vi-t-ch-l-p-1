import { Link } from 'react-router-dom';
import { Home, GraduationCap, ClipboardList, BookOpen, Plus } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import WorkspaceLayout from './WorkspaceLayout';

export default function TeacherLayout({ children }) {
  const { user } = useAuth(); const permissions = user?.roles || [];
  const items = [{ to: '/teacher', label: 'Tổng quan', icon: Home, end: true },
    ...(permissions.includes('PERM_MANAGE_CLASSES') ? [{ to: '/teacher/classes', label: 'Lớp học của cô', icon: GraduationCap }] : []),
    ...(permissions.includes('PERM_ASSIGN_HOMEWORK') ? [{ to: '/teacher/assignments', label: 'Bài tập & chấm bài', icon: ClipboardList }] : []),
    ...(permissions.includes('PERM_VIEW_ALPHABET') ? [{ to: '/teacher/alphabet', label: 'Học liệu 29 chữ', icon: BookOpen }] : [])];
  return <WorkspaceLayout home="/teacher" label="Góc giáo viên" items={items} action={permissions.includes('PERM_ASSIGN_HOMEWORK') && <Link className="teacher-button primary" to="/teacher/assignments?new=1"><Plus size={18}/> Soạn bài</Link>}>{children}</WorkspaceLayout>;
}