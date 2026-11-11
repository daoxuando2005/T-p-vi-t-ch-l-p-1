import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { existsSync } from 'node:fs';
import { mkdtemp, rm, writeFile } from 'node:fs/promises';
import { createServer } from 'node:net';
import path from 'node:path';

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
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.text);
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
        body = JSON.stringify(url.pathname === '/api/auth/signin' ? { ...account, accessToken: 'browser-test-token', tokenType: 'Bearer' }
          : url.pathname === '/api/auth/me' ? account : url.pathname.endsWith('/stats') ? {} : []);
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
  await reset([]);
  for (const route of ['/student', '/student/practice', '/parent', '/parent/assignments']) { await navigate(route); await pathname('/login'); }
  assert.deepEqual(errors, [], 'Browser JavaScript/interception errors');
  console.log('Browser checks passed: Android sign-in/restore blocked, teacher/admin routes preserved, retired URLs redirected.');
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
