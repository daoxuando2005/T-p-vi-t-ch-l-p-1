import { useEffect, useState } from 'react';
import { Sparkles, Send, FilePenLine } from 'lucide-react';
import api from '../services/api';
import { ASSIGNMENT_GROUPS, groupLabel, teacherError } from '../utils/teacherAssignments';

export default function TeacherAssistant({ onUseDraft }) {
  const [status, setStatus] = useState(null); const [statusError, setStatusError] = useState('');
  const [type, setType] = useState('WRITING'); const [count, setCount] = useState(3);
  const [message, setMessage] = useState(''); const [messages, setMessages] = useState([]);
  const [draft, setDraft] = useState(null); const [busy, setBusy] = useState(false); const [error, setError] = useState('');
  useEffect(() => { const abort = new AbortController(); api.get('/teacher/assistant/status', { signal: abort.signal }).then(r => setStatus(r.data))
    .catch(e => { if (!abort.signal.aborted) setStatusError(teacherError(e)); }); return () => abort.abort(); }, []);
  async function ask(event) {
    event.preventDefault(); if (!message.trim() || busy || !status?.configured) return;
    setBusy(true); setError(''); const prompt = message.trim();
    try {
      const reply = await api.post('/teacher/assistant/draft', { message: prompt, type, count: Number(count), history: messages.slice(-6), currentDraft: draft }, { timeout: 45000 });
      setMessages(old => [...old, { role: 'user', content: prompt }, { role: 'assistant', content: reply.data.reply }]);
      setDraft(reply.data.draft); setMessage('');
    } catch (e) { setError(teacherError(e, 'Chưa soạn được bài. Cô vui lòng thử lại.')); }
    finally { setBusy(false); }
  }
  return <aside className="teacher-card teacher-assistant" aria-label="Trợ lý soạn bài">
    <div className="teacher-assistant-head"><Sparkles size={25} color="#006590"/><div><h2>Trợ lý soạn bài</h2><span className="teacher-muted">Từ ý tưởng đến bản nháp</span></div></div>
    {status === null && !statusError && <p className="teacher-muted" role="status">Đang kiểm tra kết nối trợ lý…</p>}
    {status && !status.configured && <div className="teacher-alert" role="status">Trợ lý AI chưa được quản trị viên kết nối. Cô vẫn có thể soạn bài thủ công và dùng kho mẫu.</div>}
    {statusError && <p className="teacher-error-text" role="alert">{statusError}</p>}
    <p className="teacher-assistant-hint">Mô tả chữ cần luyện, chủ đề và mức độ. Chỉ nhập nội dung bài học, không nhập thông tin riêng của học sinh.</p>
    {!!messages.length && <div className="teacher-chat-log" aria-label="Trao đổi soạn bài">{messages.map((m, i) => <div className={`teacher-chat-message ${m.role}`} key={i}><span className="teacher-chat-label">{m.role === 'user' ? 'Yêu cầu của cô' : 'Trợ lý'}</span>{m.content}</div>)}</div>}
    {draft && <div className="teacher-ai-draft"><span className="teacher-chip">{groupLabel(draft.contentType)}</span><strong style={{ marginTop: 10 }}>{draft.title}</strong><p className="teacher-muted">{draft.description}</p><button className="teacher-button green" onClick={() => onUseDraft(draft)}><FilePenLine size={17}/> Xem và chỉnh bản nháp</button><p className="teacher-assistant-hint" style={{ margin: '10px 0 0' }}>Bản nháp chỉ được giao sau khi cô kiểm tra và bấm Giao bài.</p></div>}
    <form className="teacher-stack" onSubmit={ask}>
      <label>Loại bài<select value={type} onChange={e => setType(e.target.value)} disabled={busy}>{ASSIGNMENT_GROUPS.map(g => <option value={g.type} key={g.type}>{g.label}</option>)}</select></label>
      <label>{type === 'WRITING' ? 'Số chữ cần luyện' : 'Số câu'}<input type="number" min="1" max="20" value={count} onChange={e => setCount(e.target.value)} required disabled={busy}/></label>
      <label>Cô muốn soạn bài gì?<textarea value={message} onChange={e => setMessage(e.target.value)} placeholder="Ví dụ: Soạn 3 câu ghép ngắn chủ đề gia đình, dùng từ ba, mẹ và bé." rows={4} maxLength={2000} required disabled={busy}/></label>
      {error && <p className="teacher-error-text" role="alert">{error}</p>}
      <button className="teacher-button primary" disabled={busy || !status?.configured || !message.trim()}><Send size={17}/>{busy ? 'Đang soạn bản nháp…' : 'Nhờ trợ lý soạn bài'}</button>
    </form>
  </aside>;
}
