export const ADMIN_ROLES = [{ value: 'STUDENT', label: 'Học sinh' }, { value: 'PARENT', label: 'Phụ huynh' }, { value: 'TEACHER', label: 'Giáo viên' }, { value: 'ADMIN', label: 'Quản trị viên' }];
export const TEACHER_PERMISSIONS = [
  { value: 'PERM_MANAGE_CLASSES', label: 'Quản lý lớp và thành viên' }, { value: 'PERM_ASSIGN_HOMEWORK', label: 'Giao bài và chấm bài của lớp' },
  { value: 'PERM_VIEW_ALPHABET', label: 'Xem học liệu 29 chữ' }, { value: 'PERM_EDIT_ALPHABET', label: 'Sửa học liệu dùng chung' },
];
export function primaryRole(user) { return ['ADMIN', 'TEACHER', 'PARENT', 'STUDENT'].find(role => user?.roles?.includes(`ROLE_${role}`)) || ''; }
export function roleLabel(user) { return ADMIN_ROLES.find(r => r.value === primaryRole(user))?.label || 'Chưa có vai trò'; }
export function filterUsers(users, query, role, status) {
  const text = query.trim().toLocaleLowerCase('vi');
  return users.filter(u => (!text || [u.fullName, u.username, u.email].some(v => (v || '').toLocaleLowerCase('vi').includes(text))) &&
    (role === 'ALL' || primaryRole(u) === role) && (status === 'ALL' || Boolean(u.enabled) === (status === 'ACTIVE')));
}
export function passwordError(value) { return value.length < 6 || new TextEncoder().encode(value).length > 72 ? 'Mật khẩu cần ít nhất 6 ký tự và tối đa 72 byte UTF-8.' : ''; }
export function curriculumPayload(rows, letter) {
  if (rows.length !== 3) throw new Error('Cần đúng ba cặp từ và câu.');
  return { practices: rows.map(row => { const sentence = row.sentence.split('|').map(s => s.trim()); const text = row.text.trim();
    if (!text.includes(letter.toLocaleLowerCase('vi')) || text.length > 100 || !row.picture.trim() || row.picture.length > 40 || sentence.length < 2 || sentence.length > 12 || sentence.some(s => !s || s.length > 80)) throw new Error('Mỗi từ phải có chữ đang học viết thường, kèm minh họa và câu có 2–12 thẻ.');
    return { text, picture: row.picture.trim(), sentence };
  }) };
}