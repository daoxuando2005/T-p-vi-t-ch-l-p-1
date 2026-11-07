import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRole?: string; // STUDENT, TEACHER, PARENT, ADMIN
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, allowedRole }) => {
  const { user, loading, hasRole } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-950">
        <div className="relative w-16 h-16">
          <div className="absolute top-0 left-0 w-full h-full border-4 border-slate-800 rounded-full"></div>
          <div className="absolute top-0 left-0 w-full h-full border-4 border-t-brand-500 rounded-full animate-spin"></div>
        </div>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRole && !hasRole(allowedRole)) {
    // Nếu có tài khoản nhưng sai quyền hạn, tự động chuyển về trang của họ
    if (user.roles.includes('ROLE_STUDENT')) return <Navigate to="/student" replace />;
    if (user.roles.includes('ROLE_TEACHER')) return <Navigate to="/teacher" replace />;
    if (user.roles.includes('ROLE_PARENT')) return <Navigate to="/parent" replace />;
    if (user.roles.includes('ROLE_ADMIN')) return <Navigate to="/admin" replace />;
  }

  return <>{children}</>;
};

export default ProtectedRoute;
