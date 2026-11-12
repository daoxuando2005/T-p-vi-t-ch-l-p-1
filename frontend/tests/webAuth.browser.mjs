import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { existsSync } from 'node:fs';
import { mkdtemp, rm, writeFile } from 'node:fs/promises';
import { createServer } from 'node:net';
import path from 'node:path';
import { adminFixture, resetAdminFixture, adminReply } from './adminBrowserFixtures.mjs';

// Opt-in browser check with an isolated profile and mocked APIs; never touches real accounts.
const origin = new URL(process.env.WEB_TEST_URL || 'http://127.0.0.1:5173').origin;
const browserPath = process.env.BROWSER_PATH || [
  path.join(process.env['ProgramFiles(x86)'] || '', 'Microsoft/Edge/Application/msedge.exe'),
  path.join(process.env.ProgramFiles || '', 'Google/Chrome/Application/chrome.exe'),
].find(existsSync);
assert.ok(browserPath, 'Set BROWSER_PATH to an installed Chrome/Edge executable.');
const profile = await mkdtemp(path.resolve('node_modules/.tmp/web-auth-'));
const server = createServer();
await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
const port = server.address().port;
await new Promise(resolve => server.close(resolve));
const browser = spawn(browserPath, ['--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
  '--disable-background-mode',
  `--user-data-dir=${profile}`, `--remote-debugging-port=${port}`, 'about:blank'], { windowsHide: true, stdio: 'ignore' });
const pending = new Map();
const errors = [];
let socket;
let nextId = 0;
let roles = [];
let requests = [];
let teacherFixtures = false;
let aiConfigured = false;
let assignmentRows = [];
let createdPayloads = [];
let gradePayload = null;
let adminFixtures = false;
const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
async function until(check) {
  const end = Date.now() + 15000;
  do { if (await check()) return; await pause(75); } while (Date.now() < end);
  throw new Error('Browser check timed out.');
}
function send(method, params = {}) {
  const id = ++nextId;
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => { pending.delete(id); reject(new Error(`CDP timeout: ${method}`)); }, 12000);
    pending.set(id, { resolve, reject, timer }); socket.send(JSON.stringify({ id, method, params }));
  });
}
async function evaluate(expression) {
  const result = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.exception?.description || result.exceptionDetails.text);
  return result.result?.value;
}
async function navigate(route) { await send('Page.navigate', { url: origin + route }); }
async function pathname(expected) { await until(() => evaluate(`location.pathname === ${JSON.stringify(expected)} && document.readyState === 'complete'`)); }
async function reset(nextRoles) {
  roles = nextRoles; requests = [];
  await evaluate('localStorage.clear()'); await navigate('/login');
  await until(() => evaluate('!!document.querySelector("form input[type=password]")'));
}
async function signIn() {
  await evaluate(`(() => {
    const set = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
    for (const [type,value] of [['text','browser-test'],['password','test-password']]) {
      const input = document.querySelector('form input[type='+type+']');
      set.call(input,value); input.dispatchEvent(new Event('input',{bubbles:true}));
    }
  })()`);
  await evaluate('document.querySelector("form").requestSubmit()');
}
try {
  let target;
  await until(async () => {
    try { target = (await (await fetch(`http://127.0.0.1:${port}/json/list`)).json()).find(t => t.type === 'page'); return !!target; }
    catch { return false; }
  });
  socket = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
  socket.onmessage = async event => {
    const message = JSON.parse(event.data);
    if (message.id) {
      const waiter = pending.get(message.id);
      if (waiter) {
        clearTimeout(waiter.timer); pending.delete(message.id);
        if (message.error) waiter.reject(new Error(message.error.message));
        else waiter.resolve(message.result);
      }
      return;
    }
    if (message.method === 'Runtime.exceptionThrown') errors.push(message.params.exceptionDetails.text);
    if (message.method !== 'Fetch.requestPaused') return;
    const { requestId, request } = message.params;
    try {
      const url = new URL(request.url);
      if (url.origin === origin && !url.pathname.startsWith('/api/')) { await send('Fetch.continueRequest', { requestId }); return; }
      const account = { id: 901, username: 'browser-test', fullName: 'Tài khoản kiểm thử', roles };
      let body = '';
      if (url.pathname.startsWith('/api/')) {
        requests.push(url.pathname);
        let result = url.pathname === '/api/auth/signin' ? { ...account, accessToken: 'browser-test-token', tokenType: 'Bearer' }
          : url.pathname === '/api/auth/me' ? account : url.pathname.endsWith('/stats') ? {} : [];
        if (teacherFixtures) {
          if (url.pathname === '/api/teacher/class') result = [{ id: 77, name: 'Lớp Hoa Hướng Dương', inviteCode: 'FLOWER', studentCount: 12 }];
          if (url.pathname === '/api/teacher/class/77/assignments') result = assignmentRows;
          if (url.pathname === '/api/teacher/assistant/status') result = { configured: aiConfigured };
          if (url.pathname === '/api/teacher/assistant/draft') result = { reply: 'Cô kiểm tra ba câu trước khi giao nhé.', draft: { title: 'Gia đình của bé', description: 'Ghép câu cùng bé', contentType: 'SENTENCE', contentData: JSON.stringify({ sentences: [ { words: ['Bé','yêu','mẹ.'] }, { words: ['Ba','đọc','sách.'] }, { words: ['Mẹ','và','bé.'] } ] }) } };
          if (url.pathname === '/api/teacher/assignments' && request.method === 'POST') {
            const payload = JSON.parse(request.postData); createdPayloads.push(payload);
            result = { ...payload, id: 100 + createdPayloads.length }; assignmentRows = [...assignmentRows, result];
          }
          if (/\/assignments\/\d+\/submissions$/.test(url.pathname)) result = [{ id: 811, studentId: 922, studentName: 'Bé Mây', score: 7, autoScore: 7, gradeSource: 'AUTO', submissionData: '{"sentences":[[0,1,2],[0,1,2],[0,1,2]]}', feedback: 'Đã đối chiếu', submittedAt: '2026-01-10T09:00:00' }];
          if (url.pathname === '/api/teacher/submissions/811/grade') { gradePayload = JSON.parse(request.postData); result = { success: true }; }
        }
        if (adminFixtures) result = adminReply(request, url) ?? result;
        body = JSON.stringify(result);
      }
      await send('Fetch.fulfillRequest', { requestId, responseCode: request.method === 'OPTIONS' ? 204 : 200,
        responseHeaders: [{ name: 'Content-Type', value: url.pathname.startsWith('/api/') ? 'application/json' : 'text/css' },
          { name: 'Access-Control-Allow-Origin', value: '*' }, { name: 'Access-Control-Allow-Headers', value: 'authorization,content-type' },
          { name: 'Access-Control-Allow-Methods', value: 'GET,POST,OPTIONS' }], body: Buffer.from(body).toString('base64') });
    } catch (error) { errors.push(error.message); }
  };
  await send('Page.enable'); await send('Runtime.enable');
  await send('Fetch.enable', { patterns: [{ urlPattern: 'http*' }] });
  await navigate('/login'); await pathname('/login');
  await until(() => evaluate('!!document.querySelector("form input[type=password]")'));
  await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false });
  await writeFile('node_modules/.tmp/login-desktop.png', Buffer.from((await send('Page.captureScreenshot', { format: 'png' })).data, 'base64'));
  await evaluate(`document.querySelector('button[aria-label="Hiện mật khẩu"]').click()`);
  assert.equal(await evaluate('document.querySelector("#login-password").type'), 'text');
  await evaluate(`document.querySelector('button[aria-label="Ẩn mật khẩu"]').click()`);
  assert.equal(await evaluate('document.querySelector("#login-password").getAttribute("autocomplete")'), 'current-password');
  await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true });
  assert.equal(await evaluate('document.documentElement.scrollWidth <= innerWidth'), true);
  await writeFile('node_modules/.tmp/login-mobile.png', Buffer.from((await send('Page.captureScreenshot', { format: 'png' })).data, 'base64'));
  await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false });
  assert.equal(await evaluate('document.body.textContent.includes("Android")'), false);
  await navigate('/register'); await pathname('/register');
  await until(() => evaluate('!!document.querySelector("form input[type=password]")'));
  assert.equal(await evaluate('document.body.textContent.includes("Android")'), false);
  await navigate('/login'); await pathname('/login');
  for (const role of ['ROLE_STUDENT', 'ROLE_PARENT']) {
    await reset([role]); await signIn();
    await until(() => evaluate('!!document.querySelector("[role=alert]")?.textContent.includes("giáo viên")'));
    assert.equal(await evaluate('document.body.textContent.includes("Android")'), false);
    assert.equal(await evaluate('localStorage.getItem("token")'), null);
    assert.ok(requests.every(p => p.startsWith('/api/auth/')));
    await evaluate('localStorage.setItem("token","old-android-web-token")');
    await navigate('/teacher'); await pathname('/login');
    await until(() => evaluate('!!document.querySelector("[role=alert]")?.textContent.includes("giáo viên")'));
    assert.equal(await evaluate('localStorage.getItem("token")'), null);
  }
  const screenshot = await send('Page.captureScreenshot', { format: 'png' });
  await writeFile('node_modules/.tmp/web-auth-role-notice.png', Buffer.from(screenshot.data, 'base64'));
  for (const role of ['ROLE_TEACHER', 'ROLE_ADMIN']) {
    await reset([role, 'PERM_VIEW_ALPHABET', 'PERM_ASSIGN_HOMEWORK', 'PERM_MANAGE_CLASSES']);
    await signIn(); const home = role === 'ROLE_ADMIN' ? '/admin' : '/teacher'; await pathname(home);
    const routes = role === 'ROLE_ADMIN' ? ['/admin/users', '/admin/lessons'] : ['/teacher/classes', '/teacher/assignments', '/teacher/alphabet'];
    for (const route of routes) { await navigate(route); await pathname(route); await until(() => evaluate('document.body.textContent.includes("Tài khoản kiểm thử")')); }
    await navigate(role === 'ROLE_ADMIN' ? '/teacher' : '/admin'); await pathname(home);
  }
  teacherFixtures = true;
  await reset(['ROLE_TEACHER', 'PERM_VIEW_ALPHABET', 'PERM_ASSIGN_HOMEWORK', 'PERM_MANAGE_CLASSES']);
  await signIn(); await pathname('/teacher');
  await navigate('/teacher/assignments'); await pathname('/teacher/assignments');
  await until(() => evaluate('document.body.textContent.includes("Lớp Hoa Hướng Dương") && document.body.textContent.includes("chưa được quản trị viên kết nối")'));
  const clickText = (selector, text) => evaluate(`(() => { const target=[...document.querySelectorAll(${JSON.stringify(selector)})].find(e=>e.textContent.trim()===${JSON.stringify(text)}); if(!target) throw new Error('Missing control: '+${JSON.stringify(text)}); target.click(); })()`);
  const fill = (selector, value) => evaluate(`(() => {const input=document.querySelector(${JSON.stringify(selector)}); Object.getOwnPropertyDescriptor(input instanceof HTMLTextAreaElement ? HTMLTextAreaElement.prototype : input instanceof HTMLSelectElement ? HTMLSelectElement.prototype : HTMLInputElement.prototype,'value').set.call(input,${JSON.stringify(value)}); input.dispatchEvent(new Event(input instanceof HTMLSelectElement?'change':'input',{bubbles:true}));})()`);
  await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false });
  await writeFile('node_modules/.tmp/teacher-desktop.png', Buffer.from((await send('Page.captureScreenshot', { format: 'png' })).data, 'base64'));
  for (const [type, label] of [['WRITING','Tập viết chữ'], ['QUIZ','Đố vui ghép chữ'], ['SENTENCE','Ghép câu']]) {
    await evaluate('document.querySelector("[data-testid=teacher-new-assignment]").click()');
    await until(() => evaluate('!!document.querySelector("dialog[open] form")'));
    await clickText('dialog .teacher-tabs button', label);
    await fill('dialog input[maxlength="160"]', `Bài ${type}`);
    if (type === 'SENTENCE') assert.equal(await evaluate('document.querySelectorAll("dialog .teacher-word-preview").length'), 3);
    await evaluate('document.querySelector("dialog form").requestSubmit()');
    await until(() => evaluate('!document.querySelector("dialog[open]")'));
    assert.equal(createdPayloads.at(-1).contentType, type); assert.equal(createdPayloads.at(-1).classId, 77);
  }
  await clickText('.teacher-assignment-actions button', 'Xem & chấm bài');
  await until(() => evaluate('!!document.querySelector("dialog input[type=number]")'));
  await fill('dialog input[type=number]', '8.5');
  await fill('dialog textarea', 'Cô đã xem, bé cố gắng nhé.');
  await evaluate('document.querySelector("dialog form").requestSubmit()');
  await until(() => Promise.resolve(gradePayload !== null)); assert.equal(gradePayload.score, 8.5);
  await until(() => evaluate('!document.querySelector("dialog .teacher-dialog-head button")?.disabled'));
  await evaluate('document.querySelector("dialog .teacher-dialog-head button").click()');
  aiConfigured = true; await navigate('/teacher/assignments');
  await until(() => evaluate('!!document.querySelector(".teacher-assistant form textarea")'));
  await fill('.teacher-assistant select', 'SENTENCE'); await fill('.teacher-assistant textarea', 'Soạn ba câu về gia đình');
  await evaluate('document.querySelector(".teacher-assistant form").requestSubmit()');
  await until(() => evaluate('!!document.querySelector(".teacher-ai-draft")'));
  assert.equal(createdPayloads.length, 3, 'Generating AI drafts must not publish');
  await clickText('.teacher-ai-draft button', 'Xem và chỉnh bản nháp');
  await until(() => evaluate('document.querySelector("dialog input[maxlength] ")?.value === "Gia đình của bé"'));
  assert.equal(createdPayloads.length, 3, 'Reviewing AI drafts must not publish');
  await clickText('dialog button', 'Để sau');
  await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true });
  await evaluate('window.scrollTo(0,0)');
  assert.equal(await evaluate('document.documentElement.scrollWidth <= innerWidth'), true, 'Teacher page must fit a phone');
  await writeFile('node_modules/.tmp/teacher-mobile.png', Buffer.from((await send('Page.captureScreenshot', { format: 'png' })).data, 'base64'));
  teacherFixtures = false; adminFixtures = true; resetAdminFixture();
  await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false });
  await reset(['ROLE_ADMIN']); await signIn(); await pathname('/admin');
  await until(() => evaluate('document.body.textContent.includes("128")'));
  await writeFile('node_modules/.tmp/admin-desktop.png', Buffer.from((await send('Page.captureScreenshot', { format: 'png' })).data, 'base64'));
  await navigate('/admin/users?new=1'); await until(() => evaluate('!!document.querySelector("dialog[open] form")'));
  await fill('dialog input[maxlength="50"]', 'Cô Hương'); await fill('dialog input[maxlength="20"]', 'teacher-new');
  await fill('dialog input[type=email]', 'teacher-new@example.invalid'); await fill('dialog input[autocomplete="new-password"]', 'fixture-password');
  await fill('dialog select', 'TEACHER');
  assert.equal(await evaluate('document.querySelectorAll("dialog input[type=checkbox]:checked").length'), 4);
  await evaluate('document.querySelector("dialog form").requestSubmit()');
  await until(() => evaluate('!document.querySelector("dialog[open]")')); assert.equal(adminFixture.created, 1);
  await fill('.admin-search input', 'Cô Mai');
  await until(() => evaluate('document.querySelectorAll(".admin-user-table tbody tr").length === 1'));
  await clickText('.admin-user-table button', 'Chi tiết'); await until(() => evaluate('!!document.querySelector("dialog form")'));
  await fill('dialog .teacher-field-grid label:first-child input', 'Cô Mai đã cập nhật');
  await evaluate('document.querySelector("dialog form").requestSubmit()'); await until(() => evaluate('!document.querySelector("dialog[open]")'));
  assert.equal(adminFixture.users.find(u => u.id === 902).fullName, 'Cô Mai đã cập nhật');
  await clickText('.admin-user-table button', 'Chi tiết'); await clickText('dialog button', 'Quyền giáo viên');
  await until(() => evaluate('!!document.querySelector("dialog .admin-permissions")'));
  await evaluate('document.querySelector("dialog input[type=checkbox]").click()');
  await evaluate('document.querySelector("dialog form").requestSubmit()'); await until(() => evaluate('!document.querySelector("dialog[open]")'));
  assert.equal(adminFixture.users.find(u => u.id === 902).roles.includes('PERM_MANAGE_CLASSES'), false);
  await clickText('.admin-user-table button', 'Chi tiết'); await clickText('dialog button', 'Đặt mật khẩu mới');
  await fill('dialog input[autocomplete="new-password"]', 'changed-fixture-password');
  await evaluate('document.querySelector("dialog form").requestSubmit()'); await until(() => evaluate('!document.querySelector("dialog[open]")'));
  assert.ok(requests.includes('/api/admin/users/902/password'));
  await clickText('.admin-user-table button', 'Chi tiết'); await clickText('dialog button', 'Khóa tài khoản');
  await evaluate('document.querySelector("dialog form").requestSubmit()'); await until(() => evaluate('!document.querySelector("dialog[open]")'));
  assert.equal(adminFixture.users.find(u => u.id === 902).enabled, false);
  await fill('.admin-search input', '');
  await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true });
  assert.equal(await evaluate('document.documentElement.scrollWidth <= innerWidth'), true, 'Admin table must scroll inside its card');
  await writeFile('node_modules/.tmp/admin-users-mobile.png', Buffer.from((await send('Page.captureScreenshot', { format: 'png' })).data, 'base64'));
  await evaluate(`document.querySelector('button[aria-label="Mở menu"]').click()`);
  assert.equal(await evaluate('!!document.querySelector(".teacher-menu-dialog[open]")'), true);
  await evaluate(`document.querySelector('button[aria-label="Đóng menu"]').click()`);
  await navigate('/admin/classes'); await until(() => evaluate('document.body.textContent.includes("Lớp Hoa Hướng Dương")'));
  await clickText('.admin-class-card button', 'Xem lớp học'); await until(() => evaluate('document.querySelector("dialog[open]")?.textContent.includes("Bé Mây")'));
  await evaluate('document.querySelector("dialog .teacher-dialog-head button").click()');
  await clickText('.admin-area-tabs button', 'Liên kết gia đình (1)'); await clickText('table button', 'Gỡ liên kết');
  assert.equal(adminFixture.unlinked, 0); await clickText('dialog button', 'Giữ liên kết');
  await clickText('table button', 'Gỡ liên kết'); await clickText('dialog button', 'Xác nhận gỡ');
  await until(() => Promise.resolve(adminFixture.unlinked === 1));
  await navigate('/admin/lessons'); await until(() => evaluate('!!document.querySelector(".admin-practice-grid")'));
  await clickText('.admin-curriculum button', 'Chỉnh nội dung hành trình');
  await fill('dialog fieldset:first-of-type input', 'cha');
  await evaluate('document.querySelector("dialog form").requestSubmit()'); await until(() => Promise.resolve(adminFixture.curriculumSaved === 1));
  await until(() => evaluate('!document.querySelector("dialog[open]")'));
  await clickText('.teacher-row button', 'Chỉnh học liệu'); await fill('dialog input[maxlength="100"]', 'cha');
  await evaluate('document.querySelector("dialog form").requestSubmit()'); await until(() => evaluate('!document.querySelector("dialog[open]")'));
  assert.equal(adminFixture.lessons[0].exampleWord, 'cha');
  await navigate('/admin/settings'); await until(() => evaluate('document.body.textContent.includes("Chưa cấu hình dịch vụ AI")'));
  assert.equal(await evaluate('!!document.querySelector("input[type=password]")'), false, 'API keys stay on the backend');
  await navigate('/admin'); await until(() => evaluate('document.body.textContent.includes("128")'));
  assert.equal(await evaluate('document.documentElement.scrollWidth <= innerWidth'), true);
  await writeFile('node_modules/.tmp/admin-mobile.png', Buffer.from((await send('Page.captureScreenshot', { format: 'png' })).data, 'base64'));
  adminFixtures = false;
  await reset([]);
  for (const route of ['/student', '/student/practice', '/parent', '/parent/assignments']) { await navigate(route); await pathname('/login'); }
  assert.deepEqual(errors, [], 'Browser JavaScript/interception errors');
  console.log('Browser checks passed: login, teacher flows, admin accounts/permissions, classes/family support, learning content, settings and responsive layouts.');
} finally {
  // Close the isolated browser's children before removing its temporary profile.
  if (socket?.readyState === WebSocket.OPEN) await send('Browser.close').catch(() => {});
  socket?.close(); for (const waiter of pending.values()) { clearTimeout(waiter.timer); waiter.reject(new Error('Browser closed')); }
  if (browser.exitCode === null) await Promise.race([new Promise(resolve => browser.once('exit', resolve)), pause(2000)]);
  if (browser.exitCode === null) browser.kill();
  // The generated profile is always inside this project's ignored node_modules/.tmp directory.
  assert.ok(profile.startsWith(path.resolve('node_modules/.tmp') + path.sep));
  await rm(profile, { recursive: true, force: true, maxRetries: 8, retryDelay: 250 });
}
