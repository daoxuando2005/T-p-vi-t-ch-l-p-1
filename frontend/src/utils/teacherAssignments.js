export const LETTERS = 'A Ă Â B C D Đ E Ê G H I K L M N O Ô Ơ P Q R S T U Ư V X Y'.split(' ');
export const ASSIGNMENT_GROUPS = [
  { type: 'WRITING', label: 'Tập viết chữ', color: 'writing' },
  { type: 'QUIZ', label: 'Đố vui ghép chữ', color: 'quiz' },
  { type: 'SENTENCE', label: 'Ghép câu', color: 'sentence' },
];
export function newEditor(type = 'WRITING') {
  return { title: '', description: '', contentType: type, dueDate: '', letters: ['A', 'Ă', 'Â'],
    quiz: [
      { question: 'Điền chữ còn thiếu: b□', options: ['a', 'ă', 'â'], answer: 'a' },
      { question: 'Điền chữ còn thiếu: kh□n', options: ['a', 'ă', 'â'], answer: 'ă' },
      { question: 'Điền chữ còn thiếu: c□n', options: ['a', 'ă', 'â'], answer: 'â' },
    ], sentences: ['Bé yêu mẹ.', 'Bé đi học.', 'Mẹ đọc sách.'].map(sentence => ({ prompt: 'Ghép các từ thành câu', sentence })) };
}
export function sentenceWords(value) {
  return value.includes('|') ? value.split('|').map(s => s.trim()).filter(Boolean) : value.trim().split(/\s+/).filter(Boolean);
}
export function editorFromAssignment(item) {
  try {
  const result = { ...newEditor(item.contentType), ...item, title: String(item.title || ''), description: String(item.description || ''), dueDate: item.dueDate?.slice(0, 16) || '' };
  if (item.contentType === 'WRITING') {
    const raw = (item.contentData || '').trim(); const data = raw.startsWith('{') ? JSON.parse(raw) : { letters: [raw] };
    result.letters = (data.letters || [data.letter]).map(s => String(s).toUpperCase());
  } else if (item.contentType === 'QUIZ') {
    const questions = JSON.parse(item.contentData);
    if (!Array.isArray(questions) || questions.some(q => !Array.isArray(q.options))) throw new Error('Định dạng câu hỏi cũ chưa mở được.');
    result.quiz = questions.map(q => ({ ...q, question: q.question || '', options: q.options.map(String), answer: q.answer || '' }));
  }
  else if (item.contentType === 'SENTENCE') result.sentences = JSON.parse(item.contentData).sentences.map(q => ({ prompt: q.prompt || 'Ghép các từ thành câu', sentence: q.words.join(' | ') }));
  else throw new Error('Loại bài cũ chưa được hỗ trợ trong biểu mẫu mới.');
  return result;
  } catch { throw new Error('Định dạng bài cũ chưa mở được trong trình soạn mới. Cô vẫn có thể xem bài nộp và tạo bài mới.'); }
}
export function assignmentPayload(editor, classId) {
  const title = editor.title.trim(); const description = editor.description.trim();
  if (!title || title.length > 160) throw new Error('Nhập tiêu đề bài tập từ 1 đến 160 ký tự.');
  if (description.length > 2000) throw new Error('Hướng dẫn tối đa 2000 ký tự.');
  if (classId !== undefined && (!Number.isSafeInteger(Number(classId)) || Number(classId) <= 0)) throw new Error('Chọn lớp nhận bài trước khi giao.');
  let content;
  if (editor.contentType === 'WRITING') {
    if (!editor.letters.length || editor.letters.some(l => !LETTERS.includes(l)) || new Set(editor.letters).size !== editor.letters.length) throw new Error('Chọn các chữ khác nhau trong bảng 29 chữ.');
    content = { letters: editor.letters };
  } else if (editor.contentType === 'QUIZ') {
    if (!editor.quiz.length || editor.quiz.length > 30) throw new Error('Bài đố vui cần từ 1 đến 30 câu.');
    content = editor.quiz.map((q, i) => {
      const options = q.options.map(s => s.trim()); const answer = q.answer?.trim();
      if (!q.question.trim() || q.question.length > 500 || options.length < 2 || options.length > 8 || options.some(s => !s || s.length > 150) || new Set(options).size !== options.length || !options.includes(answer)) throw new Error(`Kiểm tra câu ${i + 1}: cần câu hỏi, các lựa chọn khác nhau và đáp án đúng.`);
      return { question: q.question.trim(), options, answer, animalName: q.animalName || '', imageUrl: q.imageUrl || '', audioUrl: q.audioUrl || '' };
    });
  } else if (editor.contentType === 'SENTENCE') {
    if (!editor.sentences.length || editor.sentences.length > 20) throw new Error('Bài ghép câu cần từ 1 đến 20 câu.');
    content = { sentences: editor.sentences.map((q, i) => {
      const words = sentenceWords(q.sentence);
      if (words.length < 2 || words.length > 12 || words.some(w => w.length > 80) || !q.prompt.trim() || q.prompt.length > 500) throw new Error(`Câu ${i + 1} cần hướng dẫn và từ 2 đến 12 thẻ từ/cụm từ.`);
      return { prompt: q.prompt.trim(), words };
    }) };
  } else throw new Error('Chọn một trong ba loại bài tập.');
  return { title, description, contentType: editor.contentType, contentData: JSON.stringify(content), dueDate: editor.dueDate || null,
    ...(classId === undefined ? {} : { classId: Number(classId) }) };
}
export function teacherError(error, fallback = 'Chưa thực hiện được. Cô vui lòng thử lại.') {
  const data = error.response?.data;
  if (error.response?.status === 401) return 'Phiên đăng nhập đã hết hạn. Cô đăng nhập lại nhé.';
  if (error.response?.status === 403) return 'Tài khoản chưa có quyền thực hiện thao tác này.';
  return (typeof data?.message === 'string' && data.message.length < 350 ? data.message : undefined) ||
    (data && Object.values(data).find(v => typeof v === 'string' && v.length < 250)) || error.message || fallback;
}
export function schoolDate(value) { return value ? new Date(value).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' }) : 'Chưa đặt hạn nộp'; }
export function groupLabel(type) { return ASSIGNMENT_GROUPS.find(g => g.type === type)?.label || 'Bài tập khác'; }
export function mediaUrl(raw, folder = 'example_images') {
  let source = (raw || '').trim();
  if (/^data:image\/(png|jpeg|webp);base64,/.test(source)) return source;
  if (!source || source.startsWith('data:') || /^[a-z]+:/i.test(source) && !/^https?:\/\//i.test(source)) return '';
  source = source.replace(/^http:\/\/(localhost|127\.0\.0\.1):5173(?=\/)/, '');
  if (/^\/(alphabet|audio|example_images)\//.test(source)) return '/api/public/media' + source;
  if (!source.includes('/') && !source.includes(':')) return `/api/public/media/${folder}/${source}`;
  return source;
}
