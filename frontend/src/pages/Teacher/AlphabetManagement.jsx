import { useCallback, useEffect, useRef, useState } from 'react';
import { BookOpen, Volume2, Pencil, RefreshCw, EyeOff, Check } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import TeacherDialog from '../../components/TeacherDialog';
import AdminCurriculumEditor from '../../components/AdminCurriculumEditor';
import { LETTERS, mediaUrl, teacherError } from '../../utils/teacherAssignments';

export default function TeacherAlphabetManagement({ admin = false }) {
  const { user } = useAuth(); const canEdit = user?.roles?.includes('ROLE_ADMIN') || user?.roles?.includes('PERM_EDIT_ALPHABET');
  const canView = user?.roles?.includes('ROLE_ADMIN') || user?.roles?.includes('PERM_VIEW_ALPHABET');
  const [lessons, setLessons] = useState([]); const [selectedLetter, setSelectedLetter] = useState('A');
  const [loading, setLoading] = useState(true); const [error, setError] = useState(''); const [notice, setNotice] = useState('');
  const [edit, setEdit] = useState(null); const [busy, setBusy] = useState(false); const [imageFailed, setImageFailed] = useState(false);
  const audio = useRef(null); const voiceSequence = useRef(0); const editRef = useRef(null); editRef.current = edit;
  const selected = lessons.find(l => l.letter.toLocaleUpperCase('vi') === selectedLetter);
  async function load() {
    setLoading(true); setError('');
    try { const reply = await api.get('/student/alphabet'); setLessons(Array.isArray(reply.data) ? reply.data : []); }
    catch (e) { setError(teacherError(e)); } finally { setLoading(false); }
  }
  const stopAudio = useCallback(() => { voiceSequence.current++; audio.current?.pause(); window.speechSynthesis?.cancel(); }, []);
  useEffect(() => { if (canView) load(); return stopAudio; }, [stopAudio, canView]);
  useEffect(() => { setImageFailed(false); stopAudio(); }, [selectedLetter, stopAudio]);
  function fallback(text, sequence) {
    if (sequence !== voiceSequence.current) return;
    if (!window.speechSynthesis) { setError('Trình duyệt chưa hỗ trợ đọc mẫu. Cô có thể tải file âm thanh cho học liệu.'); return; }
    window.speechSynthesis.cancel(); const utterance = new SpeechSynthesisUtterance(text); utterance.lang = 'vi-VN'; utterance.rate = .8;
    utterance.onerror = event => { if (sequence === voiceSequence.current && !['canceled', 'interrupted'].includes(event.error)) setError('Chưa phát được giọng đọc. Cô kiểm tra âm lượng hoặc bổ sung file âm thanh.'); }; window.speechSynthesis.speak(utterance);
  }
  function listen(source, text) {
    const sequence = ++voiceSequence.current;
    audio.current?.pause(); window.speechSynthesis?.cancel();
    if (!source) { fallback(text, sequence); return; }
    const player = new Audio(mediaUrl(source, 'audio')); audio.current = player;
    let failed = false;
    const recover = () => { if (!failed && sequence === voiceSequence.current) { failed = true; player.pause(); fallback(text, sequence); } };
    player.onerror = recover; player.play().catch(recover);
  }
  async function upload(field, file) {
    if (file.size > 8 * 1024 * 1024) { setError('Tệp tối đa 8 MB.'); return; }
    const image = field === 'imageUrl'; const extension = file.name.split('.').pop().toLowerCase();
    const allowed = image ? ['png', 'jpg', 'jpeg', 'webp'] : ['mp3', 'wav', 'ogg', 'm4a', 'mp4', 'webm', 'mpeg'];
    if (!allowed.includes(extension)) { setError('Định dạng tệp chưa phù hợp.'); return; }
    const id = edit.id; setBusy(true); setError('');
    try {
      const body = new FormData(); body.append('file', file); body.append('filename', `alphabet_${crypto.randomUUID()}.${extension}`);
      const reply = await api.post(image ? '/admin/alphabet/upload-example-image' : '/admin/alphabet/upload-audio', body, { headers: { 'Content-Type': undefined } });
      if (!reply.data.success) throw new Error(reply.data.message || 'Chưa lưu được tệp.');
      if (editRef.current?.id === id) setEdit(old => ({ ...old, [field]: reply.data.message }));
    } catch (e) { setError(teacherError(e)); } finally { setBusy(false); }
  }
  async function save(event) {
    event.preventDefault(); setBusy(true); setError('');
    try { const reply = await api.post('/admin/alphabet', edit); setLessons(old => old.map(l => l.id === reply.data.id ? reply.data : l)); setEdit(null); setNotice('Đã cập nhật học liệu dùng chung. App sẽ nhận nội dung khi cập nhật học liệu.'); }
    catch (e) { setError(teacherError(e)); } finally { setBusy(false); }
  }
  if (!canView) return <div className="teacher-empty"><BookOpen size={40}/><h1>Học liệu 29 chữ</h1><p>Tài khoản chưa có quyền xem học liệu. Bạn liên hệ quản trị viên để cập nhật quyền.</p></div>;
  return <>
    <div className="teacher-page-head"><div><h1>Học liệu 29 chữ cái</h1><p className="teacher-muted">Cùng một bảng chữ với app học sinh: nghe mẫu, làm quen từ và luyện viết.</p></div><button className="teacher-button" onClick={load} disabled={loading}><RefreshCw size={18}/> Tải lại</button></div>
    {error && <p className="teacher-alert" role="alert">{error}</p>}{notice && <p className="teacher-success" role="status">{notice}</p>}
    {loading && <p className="teacher-loading" role="status">Đang tải học liệu…</p>}
    <div className="teacher-two-column"><section className="teacher-card"><h2>Chọn chữ khám phá</h2><div className="teacher-letter-grid">{LETTERS.map(letter => <button key={letter} aria-pressed={selectedLetter === letter} onClick={() => setSelectedLetter(letter)}>{letter}</button>)}</div><p className="teacher-muted" style={{ marginTop: 20 }}>Học liệu này dùng ở màn Đọc chữ. Bài tập cô giao được soạn riêng trong Bài tập & chấm bài.</p></section>
      <section className="teacher-card"><div className="teacher-row teacher-spread"><h2>Chữ {selectedLetter}</h2>{selected?.isHidden ? <span className="teacher-chip"><EyeOff size={15}/> Đang ẩn</span> : selected && <span className="teacher-chip quiz"><Check size={15}/> Đang sử dụng</span>}</div>
        {!selected ? <div className="teacher-empty"><BookOpen size={40}/><p>Chưa nhận được học liệu cho chữ này. Cô thử tải lại nhé.</p></div> : <>
          <div style={{ textAlign: 'center', padding: '20px 0' }}><div style={{ color: '#006590', fontSize: 90, fontWeight: 800 }}>{selectedLetter} {selectedLetter.toLocaleLowerCase('vi')}</div>
            {selected.imageUrl && !imageFailed && <img style={{ maxHeight: 200, maxWidth: '100%', margin: '0 auto 16px', objectFit: 'contain' }} src={mediaUrl(selected.imageUrl)} alt={selected.exampleWord || `Ví dụ chữ ${selectedLetter}`} onError={() => setImageFailed(true)}/>}
            {imageFailed && <p className="teacher-muted">Ảnh minh họa chưa tải được.</p>}<h3 style={{ fontSize: 25 }}>{selected.exampleWord || 'Chưa có từ ví dụ'}</h3>
          </div>
          {selected.pronunciationGuide && <p className="teacher-muted">{selected.pronunciationGuide}</p>}
          <div className="teacher-row"><button className="teacher-button primary" onClick={() => listen(selected.audioUrl, selected.pronunciationGuide || selectedLetter)}><Volume2 size={18}/> Nghe chữ</button><button className="teacher-button green" onClick={() => listen(selected.exampleAudioUrl, selected.exampleWord || selectedLetter)}><Volume2 size={18}/> Nghe từ ví dụ</button>{canEdit && <button className="teacher-button" onClick={() => { setError(''); setEdit({ ...selected }); }}><Pencil size={17}/> Chỉnh học liệu</button>}</div>
        </>}
      </section></div>
    {admin && <AdminCurriculumEditor key={selectedLetter} letter={selectedLetter}/>}
    {edit && <TeacherDialog title={`Chỉnh học liệu chữ ${edit.letter}`} onClose={() => setEdit(null)} busy={busy}><form onSubmit={save} className="teacher-stack">
      <p className="teacher-muted">Đây là học liệu dùng chung. Cô kiểm tra kỹ từ ví dụ và phát âm trước khi lưu.</p>{error && <p className="teacher-alert" role="alert">{error}</p>}
      <label>Từ ví dụ<input value={edit.exampleWord || ''} maxLength={100} required onChange={e => setEdit({ ...edit, exampleWord: e.target.value })} disabled={busy}/></label>
      <label>Hướng dẫn đọc<textarea value={edit.pronunciationGuide || ''} maxLength={250} onChange={e => setEdit({ ...edit, pronunciationGuide: e.target.value })} disabled={busy}/></label>
      <label className="teacher-inline-check"><input type="checkbox" checked={!!edit.isHidden} onChange={e => setEdit({ ...edit, isHidden: e.target.checked })} disabled={busy}/> Tạm ẩn chữ khi đang cập nhật</label>
      {[['imageUrl', 'Ảnh từ ví dụ'], ['audioUrl', 'Âm thanh đọc chữ'], ['exampleAudioUrl', 'Âm thanh từ ví dụ']].map(([field, label]) => <div className="teacher-form-section" key={field}><label>{label}<input type="file" accept={field === 'imageUrl' ? 'image/png,image/jpeg,image/webp' : 'audio/*'} disabled={busy} onChange={e => { const file = e.target.files?.[0]; if (file) upload(field, file); e.target.value = ''; }}/></label>{edit[field] && <div style={{ marginTop: 12 }}>{field === 'imageUrl' ? <img src={mediaUrl(edit[field])} className="teacher-question-image" alt="Ảnh đang chọn"/> : <audio controls src={mediaUrl(edit[field], 'audio')}/>}<button type="button" className="teacher-button" style={{ marginTop: 8 }} onClick={() => setEdit({ ...edit, [field]: '' })} disabled={busy}>Bỏ tệp này</button></div>}</div>)}
      <div className="teacher-form-actions"><button type="button" className="teacher-button" onClick={() => setEdit(null)} disabled={busy}>Hủy</button><button className="teacher-button primary" disabled={busy}>{busy ? 'Đang lưu…' : 'Lưu học liệu'}</button></div>
    </form></TeacherDialog>}
  </>;
}
