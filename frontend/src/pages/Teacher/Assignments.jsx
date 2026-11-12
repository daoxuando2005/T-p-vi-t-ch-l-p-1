import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Pencil, Puzzle, Sparkles, Plus, RefreshCw, ClipboardCheck, Trash2, BookOpen, FolderHeart } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import TeacherDialog from '../../components/TeacherDialog';
import TeacherAssignmentEditor from '../../components/TeacherAssignmentEditor';
import TeacherSubmissionReview from '../../components/TeacherSubmissionReview';
import TeacherAssistant from '../../components/TeacherAssistant';
import { ASSIGNMENT_GROUPS, assignmentPayload, editorFromAssignment, groupLabel, newEditor, schoolDate, teacherError } from '../../utils/teacherAssignments';

const icons = { WRITING: Pencil, QUIZ: Puzzle, SENTENCE: Sparkles };
function initialTemplates() {
  return ASSIGNMENT_GROUPS.map(group => ({ id: `sample-${group.type}`, ...assignmentPayload({ ...newEditor(group.type), title: `${group.label} cùng bé`, description: 'Bé nghe mẫu và luyện tập từng câu nhé.' }) }));
}
export default function TeacherAssignments() {
  const { user } = useAuth(); const [params, setParams] = useSearchParams();
  const initialClass = useRef(params.get('class')).current;
  const canAssign = user?.roles?.includes('PERM_ASSIGN_HOMEWORK');
  const storageKey = `teacher.templates.${user?.id}`;
  const [classes, setClasses] = useState([]); const [classId, setClassId] = useState('');
  const [loadingClasses, setLoadingClasses] = useState(true); const [loading, setLoading] = useState(false);
  const [assignments, setAssignments] = useState([]); const [reload, setReload] = useState(0);
  const [type, setType] = useState('ALL'); const [view, setView] = useState('assigned');
  const [error, setError] = useState(''); const [notice, setNotice] = useState('');
  const [editor, setEditor] = useState(null); const [editorClassId, setEditorClassId] = useState(''); const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false); const [confirm, setConfirm] = useState(null);
  const [templates, setTemplates] = useState(() => { try { const data = JSON.parse(localStorage.getItem(storageKey)); return Array.isArray(data) ? data : initialTemplates(); } catch { return initialTemplates(); } });
  const [review, setReview] = useState(null); const [submissions, setSubmissions] = useState([]); const [reviewLoading, setReviewLoading] = useState(false); const [reviewError, setReviewError] = useState('');
  const reviewSequence = useRef(0); const editorRef = useRef(null); editorRef.current = editor;
  useEffect(() => {
    const abort = new AbortController(); setLoadingClasses(true);
    api.get('/teacher/class', { signal: abort.signal }).then(reply => {
      if (!Array.isArray(reply.data)) throw new Error('Danh sách lớp chưa hợp lệ.');
      setClasses(reply.data); setClassId(String(reply.data.find(c => String(c.id) === initialClass)?.id || reply.data[0]?.id || ''));
    }).catch(e => { if (!abort.signal.aborted) setError(teacherError(e)); }).finally(() => { if (!abort.signal.aborted) setLoadingClasses(false); });
    return () => abort.abort();
  }, [initialClass]);
  useEffect(() => {
    const abort = new AbortController(); setAssignments([]); if (!classId) return;
    setLoading(true); setError('');
    api.get(`/teacher/class/${classId}/assignments`, { signal: abort.signal }).then(reply => setAssignments(Array.isArray(reply.data) ? reply.data : []))
      .catch(e => { if (!abort.signal.aborted) setError(teacherError(e)); }).finally(() => { if (!abort.signal.aborted) setLoading(false); });
    return () => abort.abort();
  }, [classId, reload]);
  const openEditor = useCallback(item => {
    try { setFormError(''); setEditor({ ...(item ? editorFromAssignment(item) : newEditor()), draftKey: crypto.randomUUID() }); setEditorClassId(item?.classId ? String(item.classId) : classId); }
    catch (e) { setError(teacherError(e)); }
  }, [classId]);
  useEffect(() => {
    if (params.get('new') === '1' && !loadingClasses) { openEditor(); const next = new URLSearchParams(params); next.delete('new'); setParams(next, { replace: true }); }
  }, [params, loadingClasses, openEditor, setParams]);
  function writeTemplates(next) {
    localStorage.setItem(storageKey, JSON.stringify(next)); setTemplates(next);
  }
  function saveTemplate() {
    try {
      const { dueDate: _dueDate, ...payload } = assignmentPayload(editor);
      writeTemplates([{ ...payload, id: crypto.randomUUID() }, ...templates]); setNotice('Đã lưu mẫu trên trình duyệt này. Bài chưa được giao cho học sinh.'); setEditor(null); setView('templates');
    } catch (e) { setFormError(teacherError(e)); }
  }
  async function saveAssignment(event) {
    event.preventDefault(); setFormError(''); let payload;
    try { payload = assignmentPayload(editor, editorClassId); } catch (e) { setFormError(teacherError(e)); return; }
    setBusy(true);
    try {
      if (editor.id) await api.put(`/teacher/assignments/${editor.id}`, payload); else await api.post('/teacher/assignments', payload);
      setEditor(null); setNotice(editor.id ? 'Đã cập nhật bài tập.' : 'Đã giao bài. Bé sẽ thấy bài trong lớp trên app.');
      setView('assigned'); setType(payload.contentType); setClassId(String(payload.classId)); setReload(n => n + 1);
    } catch (e) { setFormError(teacherError(e)); } finally { setBusy(false); }
  }
  async function upload(index, field, file) {
    if (file.size > 8 * 1024 * 1024) { setFormError('Tệp tối đa 8 MB.'); return; }
    const extension = file.name.split('.').pop().toLowerCase();
    const allowed = field === 'imageUrl' ? ['png', 'jpg', 'jpeg', 'webp'] : ['mp3', 'wav', 'ogg', 'm4a', 'mp4', 'webm', 'mpeg'];
    if (!allowed.includes(extension)) { setFormError('Định dạng ảnh hoặc âm thanh chưa được hỗ trợ.'); return; }
    const key = editor.draftKey; setBusy(true); setFormError('');
    try {
      const body = new FormData(); body.append('file', file); body.append('filename', `lesson_${crypto.randomUUID()}.${extension}`);
      const reply = await api.post(field === 'imageUrl' ? '/teacher/upload-image' : '/teacher/upload-audio', body, { headers: { 'Content-Type': undefined } });
      if (!reply.data.success) throw new Error(reply.data.message || 'Chưa tải được tệp.');
      setEditor(old => old?.draftKey === key ? { ...old, quiz: old.quiz.map((q, n) => n === index ? { ...q, [field]: reply.data.message } : q) } : old);
    } catch (e) { if (editorRef.current?.draftKey === key) setFormError(teacherError(e)); } finally { setBusy(false); }
  }
  async function openReview(assignment) {
    const sequence = ++reviewSequence.current; setReview(assignment); setSubmissions([]); setReviewLoading(true); setReviewError('');
    try { const reply = await api.get(`/teacher/assignments/${assignment.id}/submissions`); if (sequence === reviewSequence.current) setSubmissions(reply.data); }
    catch (e) { if (sequence === reviewSequence.current) setReviewError(teacherError(e)); }
    finally { if (sequence === reviewSequence.current) setReviewLoading(false); }
  }
  async function grade(id, score, feedback) {
    if (!Number.isFinite(score) || score < 0 || score > 10) { setReviewError('Điểm nằm trong khoảng 0–10.'); return; }
    setBusy(true); setReviewError('');
    try {
      const reply = await api.post(`/teacher/submissions/${id}/grade`, { score, feedback });
      if (!reply.data.success) throw new Error(reply.data.message || 'Chưa lưu được điểm.');
      setSubmissions(old => old.map(s => s.id === id ? { ...s, score, feedback, gradeSource: 'TEACHER' } : s));
      setNotice('Đã lưu điểm và nhận xét. Học sinh và phụ huynh có thể tải lại để xem.');
    } catch (e) { setReviewError(teacherError(e)); } finally { setBusy(false); }
  }
  async function remove() {
    setBusy(true); setError('');
    try {
      if (confirm.template) writeTemplates(templates.filter(t => t.id !== confirm.id));
      else { await api.delete(`/teacher/assignments/${confirm.id}`); setAssignments(old => old.filter(a => a.id !== confirm.id)); }
      setConfirm(null); setNotice('Đã xóa mục được chọn.');
    } catch (e) { setError(teacherError(e)); } finally { setBusy(false); }
  }
  function importLegacy() {
    try {
      const data = JSON.parse(localStorage.getItem('teacher_assignment_templates') || '[]'); if (!Array.isArray(data) || !data.length) throw new Error('Trình duyệt này chưa có kho mẫu cũ.');
      const imported = data.filter(t => ASSIGNMENT_GROUPS.some(g => g.type === t.contentType)).map(t => ({ ...t, id: crypto.randomUUID(), dueDate: null }));
      writeTemplates([...templates, ...imported]); setNotice(`Đã nhập ${imported.length} mẫu vào kho tài khoản này. Kho cũ được giữ nguyên.`);
    } catch (e) { setError(teacherError(e)); }
  }
  if (!canAssign) return <div className="teacher-empty"><BookOpen size={40}/><h1>Bài tập của lớp</h1><p>Tài khoản chưa được cấp quyền giao bài tập. Cô liên hệ quản trị viên để mở quyền này.</p></div>;
  const source = view === 'assigned' ? assignments : templates;
  const visible = source.filter(a => type === 'ALL' || a.contentType === type);
  const school = classes.find(c => String(c.id) === classId);
  return <>
    <div className="teacher-page-head"><div><h1>Bài tập & những bước tiến nhỏ</h1><p className="teacher-muted">Soạn bài vừa sức, xem nét viết và gửi lời động viên cho bé.</p></div><button className="teacher-button primary" data-testid="teacher-new-assignment" onClick={() => openEditor()}><Plus size={18}/> Soạn bài mới</button></div>
    {error && <div className="teacher-alert" role="alert">{error}</div>}{notice && <div className="teacher-success" role="status">{notice}</div>}
    <div className="teacher-assign-grid"><section>
      <div className="teacher-toolbar"><label>Lớp học<select value={classId} onChange={e => { setClassId(e.target.value); setType('ALL'); }} disabled={loadingClasses}><option value="">{loadingClasses ? 'Đang tải lớp…' : 'Chọn lớp học'}</option>{classes.map(c => <option value={c.id} key={c.id}>{c.name}</option>)}</select></label><button className="teacher-button" onClick={() => setReload(n => n + 1)} disabled={loading || !classId}><RefreshCw size={18}/> Tải lại</button></div>
      {!loadingClasses && !classes.length && <div className="teacher-empty" style={{ marginBottom: 20 }}><p>Cô tạo lớp trước để giao bài cho học sinh. Vẫn có thể chuẩn bị nội dung và lưu vào kho mẫu.</p><Link className="teacher-button" to="/teacher/classes">Mở lớp học</Link></div>}
      <div className="teacher-group-grid" aria-label="Ba loại bài tập">{ASSIGNMENT_GROUPS.map(g => { const Icon = icons[g.type]; return <button key={g.type} className={`teacher-group-card ${g.color} ${type === g.type ? 'selected' : ''}`} aria-pressed={type === g.type} onClick={() => setType(type === g.type ? 'ALL' : g.type)}><Icon size={28}/><strong>{g.label}</strong><span>{source.filter(a => a.contentType === g.type).length} {view === 'assigned' ? 'bài đã giao' : 'mẫu trong kho'}</span></button>; })}</div>
      <div className="teacher-tabs" aria-label="Danh sách bài tập"><button aria-pressed={view === 'assigned'} onClick={() => setView('assigned')}>Bài đã giao</button><button aria-pressed={view === 'templates'} onClick={() => setView('templates')}>Kho mẫu của cô</button><button aria-pressed={type === 'ALL'} onClick={() => setType('ALL')}>Tất cả loại bài</button></div>
      <h2>{view === 'assigned' ? school?.name || 'Bài tập của lớp' : 'Kho mẫu trên trình duyệt này'}</h2>
      {view === 'templates' && <div className="teacher-row" style={{ marginBottom: 18 }}><p className="teacher-muted" style={{ margin: 0 }}>Mẫu giúp cô soạn nhanh, chưa tự giao cho lớp.</p><button className="teacher-button" onClick={importLegacy}><FolderHeart size={17}/> Nhập kho mẫu cũ</button></div>}
      {loading && view === 'assigned' ? <p role="status" className="teacher-loading">Đang tải bài tập…</p> : !visible.length ? <div className="teacher-empty"><ClipboardCheck size={40}/><h3>Chưa có bài ở mục này</h3><p>Cô có thể soạn mới hoặc nhờ trợ lý chuẩn bị bản nháp.</p><button className="teacher-button" onClick={() => openEditor()}>Soạn bài</button></div> : visible.map(item => <article className="teacher-card teacher-assignment" key={item.id}>
        <div className="teacher-row teacher-spread"><span className={`teacher-chip ${ASSIGNMENT_GROUPS.find(g => g.type === item.contentType)?.color || ''}`}>{groupLabel(item.contentType)}</span>{view === 'assigned' && <span className="teacher-muted">{schoolDate(item.dueDate)}</span>}</div><h3>{item.title}</h3>{item.description && <p className="teacher-muted">{item.description}</p>}
        <div className="teacher-assignment-actions">{view === 'assigned' ? <><button className="teacher-button green" onClick={() => openReview(item)}><ClipboardCheck size={17}/> Xem & chấm bài</button><button className="teacher-button" onClick={() => openEditor(item)}><Pencil size={16}/> Chỉnh sửa</button></> : <button className="teacher-button green" onClick={() => openEditor({ ...item, id: undefined, dueDate: null })}>Dùng mẫu này</button>}<button className="teacher-icon" aria-label={`Xóa ${item.title}`} onClick={() => { setError(''); setConfirm({ id: item.id, title: item.title, template: view === 'templates' }); }}><Trash2 size={17}/></button></div>
      </article>)}
    </section><TeacherAssistant onUseDraft={draft => openEditor({ ...draft, id: undefined })}/></div>
    {editor && <TeacherAssignmentEditor editor={editor} onChange={setEditor} classes={classes} classId={editorClassId} onClassChange={setEditorClassId} onClose={() => setEditor(null)} onSave={saveAssignment} onSaveTemplate={saveTemplate} onUpload={upload} busy={busy} error={formError}/>}
    {review && <TeacherSubmissionReview assignment={review} submissions={submissions} loading={reviewLoading} error={reviewError} busy={busy} onClose={() => { reviewSequence.current++; setReview(null); }} onGrade={grade}/>}
    {confirm && <TeacherDialog title={confirm.template ? 'Bỏ mẫu khỏi kho?' : 'Xóa bài tập đã giao?'} busy={busy} onClose={() => setConfirm(null)}><p>Cô muốn xóa “{confirm.title}”?</p><p className="teacher-muted">{confirm.template ? 'Mẫu sẽ được bỏ khỏi kho trên trình duyệt này. Bài đã giao cho lớp được giữ nguyên.' : 'Hãy kiểm tra các bản nộp liên quan trước khi xóa bài khỏi lớp.'}</p>{error && <p className="teacher-alert" role="alert">{error}</p>}<div className="teacher-form-actions"><button className="teacher-button" onClick={() => setConfirm(null)} disabled={busy}>Giữ lại</button><button className="teacher-button danger" onClick={remove} disabled={busy}>{busy ? 'Đang xóa…' : 'Xác nhận xóa'}</button></div></TeacherDialog>}
  </>;
}
