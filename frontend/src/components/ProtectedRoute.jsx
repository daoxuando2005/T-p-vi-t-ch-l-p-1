import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { webHome } from '../utils/webAccess';
const ProtectedRoute = ({ children, allowedRole }) => {
    const { user, loading, hasRole } = useAuth();
    if (loading) {
        return <div className="workspace-loading" role="status">Đang mở không gian làm việc…</div>;
    }
    if (!user) {
        return <Navigate to="/login" replace/>;
    }
    const home = webHome(user.roles);
    if (!home)
        return <Navigate to="/login" replace/>;
    if (allowedRole && !hasRole(allowedRole))
        return <Navigate to={home} replace/>;
    return <>{children}</>;
};
export default ProtectedRoute;
