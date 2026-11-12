import { useEffect, useState } from 'react';
import { Pencil, BookOpenCheck } from 'lucide-react';
import api from '../services/api';
import TeacherDialog from './TeacherDialog';
import { teacherError } from '../utils/teacherAssignments';
import { curriculumPayload } from '../utils/adminWorkspace';

export default function AdminCurriculumEditor({ letter }) {
  const [content, setContent] = useState(null); const [rows, setRows] = useState(null); const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [loading, setLoading] = useState(true); const [busy, setBusy] = useState(false); const [revision, setRevision] = useState(0);
  useEffect(() => { const abort = new AbortController(); setContent(null); setRows(null); setError(''); setLoading(true);
    api.get('/admin/learning-content', { signal: abort.signal }).then(r => { const item = r.data.find(c => c.letter === letter); if (!item) throw new Error('Chưa có nội dung cho chữ này.'); setContent(item); })
      .catch(e => { if (!abort.signal.aborted) setError(teacherError(e)); }).finally(() => { if (!abort.signal.aborted) setLoading(false); }); return () => abort.abort();
  }, [letter, revision]);
  function edit() { setError(''); setRows(content.practices.map(p => ({ ...p, sentence: p.sentence.join(' | ') }))); }
  async function save(event) {
    event.preventDefault(); let payload; try { payload = curriculumPayload(rows, letter); } catch (e) { setError(e.message); return; }
    setBusy(true); setError('');
    try { const reply = await api.put(`/admin/learning-content/${encodeURIComponent(letter)}`, payload); if (!reply.data.success) throw new Error(reply.data.message); setRows(null); setRevision(n => n + 1); setNotice(reply.data.message); }
    catch (e) { setError(teacherError(e)); } finally { setBusy(false); }
  }
  return <section className="teacher-card admin-curriculum"><div className="teacher-row teacher-spread"><div><h2><BookOpenCheck size={24}/> Hành trình chữ {letter}</h2><p className="teacher-muted">Ba từ điền chữ và ba câu ghép, dùng chung cho hành trình trên app.</p></div><button className="teacher-button green" disabled={loading || !content} onClick={edit}><Pencil size={17}/> Chỉnh nội dung hành trình</button></div>
    {notice && <p className="teacher-success" role="status">{notice}</p>}{!rows && error && <p className="teacher-alert" role="alert">{error}</p>}{loading ? <p role="status" className="teacher-muted">Đang tải từ và câu…</p> : content && <div className="admin-practice-grid">{content.practices.map((p, i) => <article key={i}><small>Cặp {i + 1}</small><strong><span>{p.picture}</span>{p.text}</strong><p>{p.sentence.join(' ')}</p></article>)}</div>}
    {rows && <TeacherDialog title={`Chỉnh hành trình chữ ${letter}`} busy={busy} wide onClose={() => setRows(null)}><form className="teacher-stack" onSubmit={save}><p className="teacher-muted">Giữ đúng ba cặp. Từ phải chứa chữ {letter.toLowerCase()} viết thường để app tạo ô trống. Dùng dấu | để phân chia thẻ từ/cụm từ của câu.</p>{error && <p className="teacher-alert" role="alert">{error}</p>}{rows.map((row, i) => <fieldset className="teacher-form-section" key={i}><legend>Cặp từ & câu {i + 1}</legend><div className="teacher-field-grid"><label>Từ/cụm từ<input value={row.text} maxLength={100} required disabled={busy} onChange={e => setRows(rows.map((r, n) => n === i ? { ...r, text: e.target.value } : r))}/></label><label>Ký hiệu minh họa<input value={row.picture} maxLength={40} required disabled={busy} onChange={e => setRows(rows.map((r, n) => n === i ? { ...r, picture: e.target.value } : r))}/></label><label className="full">Câu mẫu, chia thẻ bằng dấu |<input value={row.sentence} maxLength={960} required disabled={busy} onChange={e => setRows(rows.map((r, n) => n === i ? { ...r, sentence: e.target.value } : r))}/></label></div><div className="teacher-word-preview">{row.sentence.split('|').map((word, n) => <span key={n}>{word.trim() || '…'}</span>)}</div></fieldset>)}<p className="teacher-muted">Nội dung mới được app nhận khi cập nhật học liệu. Tiến độ đã đạt được giữ nguyên; cần giáo viên kiểm tra nội dung trước khi lưu.</p><div className="teacher-form-actions"><button type="button" className="teacher-button" disabled={busy} onClick={() => setRows(null)}>Hủy</button><button className="teacher-button primary" disabled={busy}>{busy ? 'Đang lưu…' : 'Lưu nội dung hành trình'}</button></div></form></TeacherDialog>}
  </section>;
}
