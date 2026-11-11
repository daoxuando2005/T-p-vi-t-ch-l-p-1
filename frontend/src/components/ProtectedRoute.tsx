import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { webHome } from '../utils/webAccess';

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRole?: 'TEACHER' | 'ADMIN';
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

  const home = webHome(user.roles);
  if (!home) return <Navigate to="/login" replace />;
  if (allowedRole && !hasRole(allowedRole)) return <Navigate to={home} replace />;

  return <>{children}</>;
};

export default ProtectedRoute;
