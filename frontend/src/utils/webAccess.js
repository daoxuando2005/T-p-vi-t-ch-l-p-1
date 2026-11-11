export const WEB_ACCESS_MESSAGE = 'Website chỉ dành cho giáo viên và quản trị viên.';
export function webHome(roles) {
    if (!Array.isArray(roles) || !roles.every(role => typeof role === 'string'))
        return null;
    if (roles.includes('ROLE_ADMIN'))
        return '/admin';
    if (roles.includes('ROLE_TEACHER'))
        return '/teacher';
    return null;
}
export class WebAccessError extends Error {
    constructor() { super(WEB_ACCESS_MESSAGE); }
}
