import axios from 'axios';
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';
const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});
// Interceptor để chèn token JWT vào header của mọi request
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
}, (error) => {
    return Promise.reject(error);
});
export default api;
export { API_BASE_URL };
api.interceptors.response.use(response => response, error => {
    const stored = localStorage.getItem('token');
    if (error.response?.status === 401 && stored && error.config?.headers?.Authorization === `Bearer ${stored}` && !error.config?.url?.startsWith('/auth/signin')) {
        window.dispatchEvent(new Event('vuihoc:session-expired'));
    }
    return Promise.reject(error);
});
