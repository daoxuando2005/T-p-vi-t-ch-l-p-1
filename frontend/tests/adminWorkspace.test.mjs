import test from 'node:test';
import assert from 'node:assert/strict';
import { primaryRole, filterUsers, passwordError, curriculumPayload } from '../src/utils/adminWorkspace.js';

test('account search and filters respect primary role, status and empty fields', () => {
  const rows = [{ id: 1, roles: ['ROLE_TEACHER', 'ROLE_ADMIN'], fullName: 'Cô Mai', enabled: true }, { id: 2, username: 'student1', roles: ['ROLE_STUDENT'], enabled: false }];
  assert.equal(primaryRole(rows[0]), 'ADMIN');
  assert.deepEqual(filterUsers(rows, 'mai', 'ADMIN', 'ACTIVE').map(u => u.id), [1]);
  assert.deepEqual(filterUsers(rows, '', 'STUDENT', 'LOCKED').map(u => u.id), [2]);
  assert.equal(filterUsers(rows, 'mai', 'TEACHER', 'ALL').length, 0);
});
test('password validation follows the server UTF-8 byte limit', () => {
  assert.equal(passwordError('fixture-pass'), ''); assert.notEqual(passwordError('short'), '');
  assert.notEqual(passwordError('á'.repeat(37)), '');
});
test('curriculum preserves three exact lowercase blanks and phrase tile boundaries', () => {
  const row = { text: 'ba', picture: 'Hình ba', sentence: 'Bé | rất yêu | ba.' };
  assert.deepEqual(curriculumPayload([row, row, row], 'A').practices[0].sentence, ['Bé', 'rất yêu', 'ba.']);
  assert.throws(() => curriculumPayload([row, row, row], 'Ă'));
  assert.throws(() => curriculumPayload([row], 'A'));
  assert.throws(() => curriculumPayload([{ ...row, sentence: 'Bé || ba.' }, row, row], 'A'));
});
