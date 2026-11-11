import test from 'node:test';
import assert from 'node:assert/strict';
import { webHome, WebAccessError, WEB_ACCESS_MESSAGE } from '../src/utils/webAccess.js';

test('teacher and admin use their own web home, with admin taking priority', () => {
  assert.equal(webHome(['ROLE_TEACHER', 'PERM_VIEW_ALPHABET']), '/teacher');
  assert.equal(webHome(['ROLE_ADMIN']), '/admin');
  assert.equal(webHome(['ROLE_TEACHER', 'ROLE_ADMIN']), '/admin');
});

test('Android accounts and permissions alone never grant web access', () => {
  for (const roles of [['ROLE_STUDENT'], ['ROLE_PARENT'], ['ROLE_PARENT', 'PERM_ASSIGN_HOMEWORK'], ['PERM_VIEW_ALPHABET'], []]) {
    assert.equal(webHome(roles), null);
  }
  assert.match(new WebAccessError().message, /giáo viên/);
  assert.equal(new WebAccessError().message, WEB_ACCESS_MESSAGE);
  assert.doesNotMatch(WEB_ACCESS_MESSAGE, /Android/);
});

test('missing and malformed roles fail closed', () => {
  for (const roles of [undefined, null, 'ROLE_ADMIN', { ROLE_ADMIN: true }, [1, 'ROLE_ADMIN']]) {
    assert.equal(webHome(roles), null);
  }
});
