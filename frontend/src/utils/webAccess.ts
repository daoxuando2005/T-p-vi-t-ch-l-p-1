export const ANDROID_ACCOUNT_MESSAGE = 'Tài khoản học sinh và phụ huynh sử dụng ứng dụng Vui Học trên Android. Website dành cho giáo viên và quản trị viên.';

export function webHome(roles: unknown): '/admin' | '/teacher' | null {
  if (!Array.isArray(roles) || !roles.every(role => typeof role === 'string')) return null;
  if (roles.includes('ROLE_ADMIN')) return '/admin';
  if (roles.includes('ROLE_TEACHER')) return '/teacher';
  return null;
}

export class AndroidAccountError extends Error {
  constructor() { super(ANDROID_ACCOUNT_MESSAGE); }
}
