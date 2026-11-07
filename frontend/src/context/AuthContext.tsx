import React, { createContext, useState, useEffect, useContext } from 'react';
import api from '../services/api';

interface User {
  id: number;
  username: string;
  fullName: string;
  roles: string[];
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  loading: boolean;
  login: (usernameOrEmail: string, password: string) => Promise<any>;
  register: (data: any) => Promise<any>;
  verifyOtp: (emailOrUsername: string, otpCode: string) => Promise<any>;
  logout: () => void;
  hasRole: (role: string) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    // Khôi phục trạng thái đăng nhập khi tải trang
    const storedToken = localStorage.getItem('token');
    if (storedToken) {
      setToken(storedToken);
      api.get('/auth/me')
        .then((response) => {
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
    } else {
      setLoading(false);
    }
  }, []);

  const login = async (usernameOrEmail: string, password: string) => {
    setLoading(true);
    try {
      const response = await api.post('/auth/signin', { usernameOrEmail, password });
      const { accessToken, id, username, fullName, roles } = response.data;
      
      localStorage.setItem('token', accessToken);
      setToken(accessToken);
      
      const loggedUser = { id, username, fullName, roles };
      setUser(loggedUser);
      
      return { success: true, user: loggedUser };
    } catch (error: any) {
      localStorage.removeItem('token');
      setToken(null);
      setUser(null);
      const message = error.response?.data?.message || 'Đăng nhập không thành công';
      throw new Error(message);
    } finally {
      setLoading(false);
    }
  };

  const register = async (data: any) => {
    try {
      const response = await api.post('/auth/signup', data);
      return response.data;
    } catch (error: any) {
      const message = error.response?.data?.message || 'Đăng ký không thành công';
      throw new Error(message);
    }
  };

  const verifyOtp = async (emailOrUsername: string, otpCode: string) => {
    try {
      const response = await api.post('/auth/verify-otp', { emailOrUsername, otpCode });
      return response.data;
    } catch (error: any) {
      const message = error.response?.data?.message || 'Kích hoạt OTP thất bại';
      throw new Error(message);
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
  };

  const hasRole = (role: string) => {
    if (!user) return false;
    // Bỏ tiền tố ROLE_ nếu có
    const checkRole = role.startsWith('ROLE_') ? role : `ROLE_${role}`;
    return user.roles.includes(checkRole);
  };

  return (
    <AuthContext.Provider value={{ user, token, loading, login, register, verifyOtp, logout, hasRole }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
