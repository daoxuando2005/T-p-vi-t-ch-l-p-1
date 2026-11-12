import { useAuth } from '../context/AuthContext';
import TeacherLayout from './TeacherLayout';
import AdminLayout from './AdminLayout';

export default function Layout({ children }) {
  const { user } = useAuth();
  if (!user) return null;
  if (user.roles.includes('ROLE_ADMIN')) return <AdminLayout>{children}</AdminLayout>;
  if (user.roles.includes('ROLE_TEACHER')) return <TeacherLayout>{children}</TeacherLayout>;
  return null;
}