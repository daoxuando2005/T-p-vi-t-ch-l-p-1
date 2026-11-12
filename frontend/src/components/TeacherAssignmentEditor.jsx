import { Plus, Trash2 } from 'lucide-react';
import TeacherDialog from './TeacherDialog';
import { ASSIGNMENT_GROUPS, LETTERS, sentenceWords, mediaUrl } from '../utils/teacherAssignments';

export default function TeacherAssignmentEditor({ editor, onChange, classes, classId, onClassChange, onClose, onSave, onSaveTemplate, onUpload, busy, error }) {
  const patch = changes => onChange({ ...editor, ...changes });
  const question = (index, changes) => patch({ quiz: editor.quiz.map((q, i) => i === index ? { ...q, ...changes } : q) });
  const sentence = (index, changes) => patch({ sentences: editor.sentences.map((q, i) => i === index ? { ...q, ...changes } : q) });
  return <TeacherDialog title={editor.id ? 'Chỉnh sửa bài đã giao' : 'Soạn bài cho lớp'} onClose={onClose} busy={busy} wide>
    <form onSubmit={onSave}>
      {error && <div className="teacher-alert" role="alert">{error}</div>}
      <div className="teacher-tabs" aria-label="Loại bài tập">{ASSIGNMENT_GROUPS.map(g => <button type="button" key={g.type} aria-pressed={editor.contentType === g.type} onClick={() => patch({ contentType: g.type })} disabled={busy || !!editor.id}>{g.label}</button>)}</div>
      <div className="teacher-field-grid">
        <label className="full">Tiêu đề bài tập<input autoFocus value={editor.title} onChange={e => patch({ title: e.target.value })} required maxLength={160} disabled={busy}/></label>
        <label>Lớp nhận bài<select value={classId} onChange={e => onClassChange(e.target.value)} disabled={busy || !!editor.id}><option value="">Chọn lớp</option>{classes.map(c => <option value={c.id} key={c.id}>{c.name}</option>)}</select></label>
        <label>Hạn nộp (không bắt buộc)<input type="datetime-local" value={editor.dueDate} onChange={e => patch({ dueDate: e.target.value })} disabled={busy}/></label>
        <label className="full">Lời hướng dẫn cho bé<textarea value={editor.description} onChange={e => patch({ description: e.target.value })} maxLength={2000} rows={2} disabled={busy}/></label>
      </div>
      {editor.id && <p className="teacher-muted" style={{ marginTop: 16 }}>Khi đã có học sinh nộp, cô có thể sửa tiêu đề, hướng dẫn và hạn nộp. Muốn thay đề, cô tạo bài mới để giữ kết quả cũ.</p>}
      {editor.contentType === 'WRITING' && <section className="teacher-form-section"><div className="teacher-question-head"><h3>Chọn chữ bé luyện viết</h3><span className="teacher-chip writing">{editor.letters.length}/29 chữ</span></div>
        <p className="teacher-muted">Bé luyện chữ thường trên ô li. Điểm tự động đánh giá độ bám mẫu; cô có thể xem bài và điều chỉnh.</p>
        <div className="teacher-letter-grid" aria-label="Bảng chữ cái">{LETTERS.map(letter => <button key={letter} type="button" aria-pressed={editor.letters.includes(letter)} disabled={busy} onClick={() => patch({ letters: editor.letters.includes(letter) ? editor.letters.filter(l => l !== letter) : LETTERS.filter(l => editor.letters.includes(l) || l === letter) })}>{letter}</button>)}</div>
        <div className="teacher-row" style={{ marginTop: 14 }}><button className="teacher-button" type="button" onClick={() => patch({ letters: [...LETTERS] })} disabled={busy}>Chọn đủ 29 chữ</button><button className="teacher-button" type="button" onClick={() => patch({ letters: [] })} disabled={busy}>Bỏ chọn</button></div>
      </section>}
      {editor.contentType === 'QUIZ' && <section><p className="teacher-muted" style={{ marginTop: 18 }}>Cô nhập câu đố điền/ghép chữ và đánh dấu đáp án đúng. Bé chọn đáp án trên app, hệ thống chấm khi nộp.</p>
        {editor.quiz.map((q, i) => <div className="teacher-form-section" key={i}>
          <div className="teacher-question-head"><h3>Câu {i + 1}</h3><button className="teacher-icon" type="button" disabled={busy || editor.quiz.length <= 1} aria-label={`Xóa câu ${i + 1}`} onClick={() => patch({ quiz: editor.quiz.filter((_, n) => n !== i) })}><Trash2 size={17}/></button></div>
          <label>Câu hỏi<input value={q.question} onChange={e => question(i, { question: e.target.value })} placeholder="Ví dụ: Điền chữ còn thiếu: b□" maxLength={500} required disabled={busy}/></label>
          <div className="teacher-options" role="group" aria-label={`Lựa chọn và đáp án câu ${i + 1}`}>{q.options.map((option, n) => <div className="teacher-option" key={n}>
            <input type="radio" name={`correct-${i}`} checked={q.answer === option && !!option} aria-label={`Đáp án đúng câu ${i + 1}, lựa chọn ${n + 1}`} disabled={busy} onChange={() => question(i, { answer: option })}/>
            <input type="text" value={option} aria-label={`Câu ${i + 1}, lựa chọn ${n + 1}`} maxLength={150} required disabled={busy} onChange={e => question(i, { options: q.options.map((v, index) => index === n ? e.target.value : v), answer: q.answer === option ? e.target.value : q.answer })}/>
            <button type="button" className="teacher-icon" disabled={busy || q.options.length <= 2} aria-label={`Bỏ lựa chọn ${n + 1} câu ${i + 1}`} onClick={() => question(i, { options: q.options.filter((_, index) => index !== n), answer: q.answer === option ? '' : q.answer })}><Trash2 size={15}/></button>
          </div>)}</div>
          <button className="teacher-button" type="button" disabled={busy || q.options.length >= 8} onClick={() => question(i, { options: [...q.options, ''] })}>Thêm lựa chọn</button>
          <div className="teacher-upload"><label>Ảnh minh họa (tối đa 8 MB)<input type="file" accept="image/png,image/jpeg,image/webp" disabled={busy} onChange={e => { const file = e.target.files?.[0]; if (file) onUpload(i, 'imageUrl', file); e.target.value = ''; }}/></label><label>Âm thanh câu hỏi (tối đa 8 MB)<input type="file" accept="audio/mpeg,audio/wav,audio/ogg,audio/mp4,audio/webm,.m4a" disabled={busy} onChange={e => { const file = e.target.files?.[0]; if (file) onUpload(i, 'audioUrl', file); e.target.value = ''; }}/></label></div>
          {q.imageUrl && <div className="teacher-row" style={{ marginTop: 12 }}><img className="teacher-question-image" src={mediaUrl(q.imageUrl)} alt={`Minh họa câu ${i + 1}`}/><button className="teacher-button" type="button" disabled={busy} onClick={() => question(i, { imageUrl: '' })}>Bỏ ảnh</button></div>}
          {q.audioUrl && <div className="teacher-row" style={{ marginTop: 12 }}><audio controls src={mediaUrl(q.audioUrl, 'audio')} aria-label={`Âm thanh câu ${i + 1}`}/><button className="teacher-button" type="button" disabled={busy} onClick={() => question(i, { audioUrl: '' })}>Bỏ âm thanh</button></div>}
        </div>)}
        <button className="teacher-button" type="button" style={{ marginTop: 16 }} disabled={busy || editor.quiz.length >= 30} onClick={() => patch({ quiz: [...editor.quiz, { question: '', options: ['', '', ''], answer: '' }] })}><Plus size={17}/> Thêm câu đố</button>
      </section>}
      {editor.contentType === 'SENTENCE' && <section><p className="teacher-muted" style={{ marginTop: 18 }}>Nhập câu đúng theo thứ tự. App sẽ đảo các thẻ để bé ghép lại. Có thể dùng dấu | để gộp nhiều từ thành một thẻ.</p>
        {editor.sentences.map((q, i) => <div className="teacher-form-section" key={i}><div className="teacher-question-head"><h3>Câu {i + 1}</h3><button className="teacher-icon" type="button" disabled={busy || editor.sentences.length <= 1} aria-label={`Xóa câu ghép ${i + 1}`} onClick={() => patch({ sentences: editor.sentences.filter((_, n) => n !== i) })}><Trash2 size={17}/></button></div>
          <label>Lời hướng dẫn<input value={q.prompt} onChange={e => sentence(i, { prompt: e.target.value })} maxLength={500} required disabled={busy}/></label>
          <label style={{ marginTop: 12 }}>Câu mẫu đúng<input value={q.sentence} onChange={e => sentence(i, { sentence: e.target.value })} placeholder="Bé yêu mẹ." maxLength={960} required disabled={busy}/></label>
          <div className="teacher-word-preview" aria-label={`Thẻ từ câu ${i + 1}`}>{sentenceWords(q.sentence).map((word, n) => <span key={n}>{word}</span>)}</div>
          <p className="teacher-muted" style={{ marginTop: 8, marginBottom: 0 }}>{sentenceWords(q.sentence).length}/12 thẻ từ</p>
        </div>)}
        <button className="teacher-button" type="button" style={{ marginTop: 16 }} disabled={busy || editor.sentences.length >= 20} onClick={() => patch({ sentences: [...editor.sentences, { prompt: 'Ghép các từ thành câu', sentence: '' }] })}><Plus size={17}/> Thêm câu ghép</button>
      </section>}
      <div className="teacher-form-actions"><button className="teacher-button" type="button" onClick={onClose} disabled={busy}>Để sau</button><button className="teacher-button green" type="button" onClick={onSaveTemplate} disabled={busy}>Lưu vào kho mẫu</button><button className="teacher-button primary" disabled={busy}>{busy ? 'Đang lưu…' : editor.id ? 'Lưu thay đổi' : 'Giao bài cho lớp'}</button></div>
    </form>
  </TeacherDialog>;
}
