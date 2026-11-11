import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import ts from 'typescript';

const source = await readFile(new URL('../src/utils/webAccess.ts', import.meta.url), 'utf8');
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 } });
const { webHome, AndroidAccountError, ANDROID_ACCOUNT_MESSAGE } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`);

test('teacher and admin use their own web home, with admin taking priority', () => {
  assert.equal(webHome(['ROLE_TEACHER', 'PERM_VIEW_ALPHABET']), '/teacher');
  assert.equal(webHome(['ROLE_ADMIN']), '/admin');
  assert.equal(webHome(['ROLE_TEACHER', 'ROLE_ADMIN']), '/admin');
});

test('Android accounts and permissions alone never grant web access', () => {
  for (const roles of [['ROLE_STUDENT'], ['ROLE_PARENT'], ['ROLE_PARENT', 'PERM_ASSIGN_HOMEWORK'], ['PERM_VIEW_ALPHABET'], []]) {
    assert.equal(webHome(roles), null);
  }
  assert.match(new AndroidAccountError().message, /Android/);
  assert.equal(new AndroidAccountError().message, ANDROID_ACCOUNT_MESSAGE);
});

test('missing and malformed roles fail closed', () => {
  for (const roles of [undefined, null, 'ROLE_ADMIN', { ROLE_ADMIN: true }, [1, 'ROLE_ADMIN']]) {
    assert.equal(webHome(roles), null);
  }
});
