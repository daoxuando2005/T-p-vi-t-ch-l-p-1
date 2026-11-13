import React, { createContext, useState, useEffect, useContext } from 'react';
import api from '../services/api';
import { WEB_ACCESS_MESSAGE, WebAccessError, webHome } from '../utils/webAccess';
const AuthContext = createContext(undefined);
export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [token, setToken] = useState(null);
    const [loading, setLoading] = useState(true);
    const [notice, setNotice] = useState(null);
    useEffect(() => {
        const expired = () => { localStorage.removeItem('token'); setToken(null); setUser(null); setNotice('Phiên đăng nhập đã hết hiệu lực. Bạn đăng nhập lại để tiếp tục.'); };
        window.addEventListener('vuihoc:session-expired', expired);
        return () => window.removeEventListener('vuihoc:session-expired', expired);
    }, []);
    useEffect(() => {
        // Khôi phục trạng thái đăng nhập khi tải trang
        const storedToken = localStorage.getItem('token');
        if (storedToken) {
            setToken(storedToken);
            api.get('/auth/me')
                .then((response) => {
                if (!webHome(response.data?.roles)) {
                    localStorage.removeItem('token');
                    setToken(null);
                    setUser(null);
                    setNotice(WEB_ACCESS_MESSAGE);
                    return;
                }
                setUser(response.data);
            })
                .catch(() => {
                // Token hỏng hoặc hết hạn
                localStorage.removeItem('token');
                setToken(null);
                setUser(null);
            })
                .finally(() => {
                setLoading(false);
            });
        }
        else {
            setLoading(false);
        }
    }, []);
    const login = async (usernameOrEmail, password) => {
        setLoading(true);
        setNotice(null);
        try {
            const response = await api.post('/auth/signin', { usernameOrEmail, password });
            const { accessToken, id, username, fullName, roles } = response.data;
            if (!webHome(roles))
                throw new WebAccessError();
            localStorage.setItem('token', accessToken);
            setToken(accessToken);
            const loggedUser = { id, username, fullName, roles };
            setUser(loggedUser);
            return { success: true, user: loggedUser };
        }
        catch (error) {
            localStorage.removeItem('token');
            setToken(null);
            setUser(null);
            const message = error instanceof WebAccessError ? error.message : error.response?.data?.message || 'Đăng nhập không thành công';
            throw new Error(message);
        }
        finally {
            setLoading(false);
        }
    };
    const register = async (data) => {
        try {
            const response = await api.post('/auth/signup', data, { timeout: 15000 });
            if (response.data?.success !== true) throw new Error('Chưa tạo được tài khoản. Vui lòng thử lại.');
            return response.data;
        }
        catch (error) {
            const message = error.response?.data?.message || 'Chưa tạo được tài khoản. Kiểm tra kết nối và thông tin đăng ký.';
            throw new Error(message);
        }
    };
    const logout = () => {
        localStorage.removeItem('token');
        setToken(null);
        setUser(null);
    };
    const hasRole = (role) => {
        if (!user)
            return false;
        // Bỏ tiền tố ROLE_ nếu có
        const checkRole = role.startsWith('ROLE_') ? role : `ROLE_${role}`;
        return user.roles.includes(checkRole);
    };
    const refreshUser = async () => { const reply = await api.get('/auth/me'); setUser(reply.data); };
    return (<AuthContext.Provider value={{ user, token, loading, notice, login, register, logout, hasRole, refreshUser }}>
      {children}
    </AuthContext.Provider>);
};
export const useAuth = () => {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
