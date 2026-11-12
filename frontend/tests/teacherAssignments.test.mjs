import test from 'node:test';
import assert from 'node:assert/strict';
import { newEditor, assignmentPayload, editorFromAssignment, mediaUrl, sentenceWords } from '../src/utils/teacherAssignments.js';

test('all three assignment editors serialize to their actual server contracts', () => {
  for (const type of ['WRITING', 'QUIZ', 'SENTENCE']) {
    const payload = assignmentPayload({ ...newEditor(type), title: 'Bài cho bé' }, 12);
    assert.equal(payload.classId, 12); assert.equal(payload.contentType, type);
    assert.deepEqual(assignmentPayload(editorFromAssignment(payload), 12), payload);
  }
});
test('sentence phrases and repeated word tiles keep their identities', () => {
  assert.deepEqual(sentenceWords('Con bò | ăn | cỏ.'), ['Con bò', 'ăn', 'cỏ.']);
  assert.deepEqual(sentenceWords('ba ba ơi'), ['ba', 'ba', 'ơi']);
});
test('teacher cannot publish blank titles, invalid letters, invalid answer keys or one-word sentences', () => {
  assert.throws(() => assignmentPayload(newEditor(), 1));
  assert.throws(() => assignmentPayload({ ...newEditor(), title: 'Bài viết', letters: ['F'] }, 1));
  const quiz = newEditor('QUIZ'); quiz.title = 'Đố vui'; quiz.quiz[0].answer = 'z';
  assert.throws(() => assignmentPayload(quiz, 1));
  assert.throws(() => assignmentPayload({ ...newEditor('SENTENCE'), title: 'Ghép câu', sentences: [{ prompt: 'Ghép', sentence: 'Bé' }] }, 1));
});
test('legacy letter assignments and media paths remain readable without unsafe URL schemes', () => {
  assert.deepEqual(editorFromAssignment({ title: 'Viết a', contentType: 'WRITING', contentData: 'A' }).letters, ['A']);
  assert.equal(mediaUrl('http://localhost:5173/audio/a.mp3'), '/api/public/media/audio/a.mp3');
  assert.equal(mediaUrl('a.mp3', 'audio'), '/api/public/media/audio/a.mp3');
  assert.equal(mediaUrl('javascript:alert(1)'), ''); assert.equal(mediaUrl('data:text/html,x'), '');
});
