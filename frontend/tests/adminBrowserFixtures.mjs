import { LETTERS } from '../src/utils/teacherAssignments.js';
export let adminFixture;
export function resetAdminFixture() {
  adminFixture = {
    created: 0, unlinked: 0, curriculumSaved: 0,
    users: [
      { id: 901, username: 'browser-test', fullName: 'Tài khoản kiểm thử', email: 'admin@example.invalid', roles: ['ROLE_ADMIN'], enabled: true },
      { id: 902, username: 'teacher-fixture', fullName: 'Cô Mai', email: 'teacher@example.invalid', roles: ['ROLE_TEACHER', 'PERM_MANAGE_CLASSES', 'PERM_ASSIGN_HOMEWORK', 'PERM_VIEW_ALPHABET'], enabled: true },
      { id: 903, username: 'student-fixture', fullName: 'Bé Mây', email: 'student@example.invalid', roles: ['ROLE_STUDENT'], enabled: true },
      { id: 904, username: 'parent-fixture', fullName: 'Mẹ của Mây', email: 'parent@example.invalid', roles: ['ROLE_PARENT'], enabled: false },
    ],
    links: [{ id: 81, parentId: 904, parentName: 'Mẹ của Mây', parentUsername: 'parent-fixture', studentId: 903, studentName: 'Bé Mây', studentUsername: 'student-fixture', status: 'APPROVED' }],
    lessons: LETTERS.map((letter, index) => ({ id: index + 1, letter, pronunciationGuide: letter.toLowerCase(), exampleWord: 'ba', isHidden: false })),
    curriculum: LETTERS.map(letter => ({ letter, practices: ['ba', 'hoa', 'na'].map(text => ({ text, picture: '🌼', sentence: ['Bé', 'yêu', text + '.'] })) })),
  };
}
export function adminReply(request, url) {
  const path = url.pathname; const data = request.postData ? JSON.parse(request.postData) : null;
  if (path === '/api/admin/stats') return { totalUsers: 128, lockedUsers: 3, totalClasses: 8, totalAssignments: 35, totalSubmissions: 246, approvedLinks: 47, hiddenLetters: 0, studentCount: 89, teacherCount: 8, parentCount: 30, schoolDate: '2026-11-12', days: [5, 12, 8, 19, 15, 24, 18].map((n, i) => ({ date: `2026-11-${String(i + 6).padStart(2, '0')}`, practices: n, submissions: Math.round(n / 3), activeStudents: Math.round(n / 2) })) };
  if (path === '/api/admin/users' && request.method === 'GET') return adminFixture.users;
  if (path === '/api/admin/users' && request.method === 'POST') { adminFixture.created++; adminFixture.users.push({ ...data, id: 990, enabled: true, roles: [`ROLE_${data.role}`, ...data.permissions] }); return { success: true }; }
  const match = path.match(/^\/api\/admin\/users\/(\d+)(?:\/(info|toggle|role|permissions|password))?$/);
  if (match) {
    const user = adminFixture.users.find(u => u.id === Number(match[1]));
    if (match[2] === 'info') Object.assign(user, data);
    if (match[2] === 'toggle') user.enabled = !user.enabled;
    if (match[2] === 'role') user.roles = ['ROLE_' + url.searchParams.get('role')];
    if (match[2] === 'permissions') user.roles = ['ROLE_TEACHER', ...data];
    if (!match[2] && request.method === 'DELETE') adminFixture.users = adminFixture.users.filter(u => u !== user);
    return { success: true, message: 'Đã lưu thay đổi.' };
  }
  if (path === '/api/admin/classes') return [{ id: 77, name: 'Lớp Hoa Hướng Dương', teacherName: 'Cô Mai', teacherUsername: 'teacher-fixture', inviteCode: 'FLOWER', studentCount: 12, assignmentCount: 3, submissionCount: 24 }];
  if (path === '/api/admin/classes/77') return { id: 77, name: 'Lớp Hoa Hướng Dương', teacherName: 'Cô Mai', students: [{ id: 903, username: 'student-fixture', fullName: 'Bé Mây', submittedCount: 2 }], assignments: [{ id: 1, title: 'Ghép câu cùng bé', contentType: 'SENTENCE', submissionCount: 2 }] };
  if (path === '/api/admin/family-links') return adminFixture.links;
  if (path === '/api/admin/family-links/81' && request.method === 'DELETE') { adminFixture.links = []; adminFixture.unlinked++; return { success: true, message: 'Đã gỡ liên kết.' }; }
  if (path === '/api/admin/settings') return { backend: 'UP', aiConfigured: false, chatEnabled: false, serverTime: '2026-11-12T16:00:00Z' };
  if (path === '/api/student/alphabet') return adminFixture.lessons;
  if (path === '/api/admin/alphabet') { adminFixture.lessons = adminFixture.lessons.map(l => l.id === data.id ? data : l); return data; }
  if (path === '/api/admin/learning-content') return adminFixture.curriculum;
  if (path === '/api/admin/learning-content/A' && request.method === 'PUT') { adminFixture.curriculumSaved++; adminFixture.curriculum[0].practices = data.practices; return { success: true, message: 'Đã lưu học liệu.' }; }
  return undefined;
}