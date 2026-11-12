import { Link } from 'react-router-dom';

export default function VuiBrand({ home = '/login', label = 'Cùng bé học chữ mỗi ngày' }) {
  return <Link to={home} className="teacher-brand" aria-label="Vui Học – trang chính">
    <svg viewBox="0 0 64 64" width="48" height="48" aria-hidden="true"><ellipse cx="32" cy="33" rx="28" ry="29" fill="#1cb0f6"/><ellipse cx="32" cy="51" rx="18" ry="9" fill="#e5f3ff"/><circle cx="21" cy="25" r="12" fill="white"/><circle cx="43" cy="25" r="12" fill="white"/><circle cx="23" cy="25" r="5" fill="#263e50"/><circle cx="41" cy="25" r="5" fill="#263e50"/><path d="M26 38h12l-6 8z" fill="#ffc800"/></svg>
    <span><strong>Vui Học</strong><small>{label}</small></span>
  </Link>;
}