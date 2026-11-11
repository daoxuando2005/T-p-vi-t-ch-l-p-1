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
            const response = await api.post('/auth/signup', data);
            return response.data;
        }
        catch (error) {
            const message = error.response?.data?.message || 'Đăng ký không thành công';
            throw new Error(message);
        }
    };
    const verifyOtp = async (emailOrUsername, otpCode) => {
        try {
            const response = await api.post('/auth/verify-otp', { emailOrUsername, otpCode });
            return response.data;
        }
        catch (error) {
            const message = error.response?.data?.message || 'Kích hoạt OTP thất bại';
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
    return (<AuthContext.Provider value={{ user, token, loading, notice, login, register, verifyOtp, logout, hasRole }}>
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
