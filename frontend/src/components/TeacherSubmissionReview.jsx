import { useEffect, useState } from 'react';
import TeacherDialog from './TeacherDialog';
import { groupLabel, mediaUrl, schoolDate } from '../utils/teacherAssignments';

function evidence(assignment, submission) {
  try {
    const raw = submission.submissionData || '';
    const data = raw.startsWith('data:image/') ? [{ image: raw }] : JSON.parse(raw);
    if (assignment.contentType === 'WRITING') return { images: Array.isArray(data) ? data : [], answers: [] };
    const questions = JSON.parse(assignment.contentData);
    if (assignment.contentType === 'QUIZ') return { images: [], answers: questions.map((q, i) => ({ question: q.question, selected: (data.answers || data)[i] || 'Chưa có', answer: q.answer })) };
    if (assignment.contentType === 'SENTENCE') return { images: [], answers: questions.sentences.map((q, i) => ({ question: q.prompt || `Ghép câu ${i + 1}`, selected: data.sentences[i].map(n => q.words[n] || '?').join(' '), answer: q.words.join(' ') })) };
  } catch { /* Historical formats remain readable through feedback. */ }
  return { images: [], answers: [], error: 'Bài được lưu theo định dạng cũ. Cô vẫn có thể xem điểm và nhận xét.' };
}
function rubric(submission) {
  try { const rows = JSON.parse(submission.assessmentData); return Array.isArray(rows) ? rows : []; } catch { return []; }
}
export default function TeacherSubmissionReview({ assignment, submissions, loading, error, busy, onClose, onGrade }) {
  const [selectedId, setSelectedId] = useState(null); const [score, setScore] = useState(''); const [feedback, setFeedback] = useState('');
  const selected = submissions.find(s => s.id === selectedId) || submissions[0];
  useEffect(() => { setScore(selected?.score == null ? '' : String(selected.score)); setFeedback(selected?.feedback || ''); }, [selected]);
  const work = selected ? evidence(assignment, selected) : null;
  return <TeacherDialog title={`Bài nộp · ${assignment.title}`} onClose={onClose} busy={busy} wide>
    <div className="teacher-row teacher-spread" style={{ marginBottom: 18 }}><span className="teacher-chip">{groupLabel(assignment.contentType)}</span><span className="teacher-muted">{submissions.length} bài đã nộp</span></div>
    {error && <p className="teacher-alert" role="alert">{error}</p>}
    {loading ? <p role="status" className="teacher-loading">Đang tải bài làm…</p> : !selected ? <div className="teacher-empty">Chưa có học sinh nộp bài. Bài sẽ xuất hiện tại đây khi bé gửi thành công.</div> : <div className="teacher-two-column">
      <section><label>Chọn học sinh<select value={selected.id} onChange={e => setSelectedId(Number(e.target.value))} disabled={busy}>{submissions.map(s => <option value={s.id} key={s.id}>{s.studentName || `Học sinh ${s.studentId}`} · {s.score == null ? 'Chưa có điểm' : `${s.score}/10`}</option>)}</select></label>
        <div className="teacher-form-section"><h3>{selected.studentName}</h3><p className="teacher-muted">Nộp lúc {schoolDate(selected.submittedAt)}</p>
          <p><strong>{selected.gradeSource === 'TEACHER' ? 'Điểm cô chấm' : selected.gradeSource === 'AUTO' ? 'Điểm tự động' : 'Điểm hiện tại'}: {selected.score == null ? 'Chưa có' : `${selected.score}/10`}</strong></p>
          {selected.autoScore != null && selected.gradeSource === 'TEACHER' && <p className="teacher-muted">Điểm tự động ban đầu: {selected.autoScore}/10</p>}
          {assignment.contentType === 'WRITING' && <p className="teacher-muted">Điểm tự động đo độ bám mẫu và độ phủ nét, chưa đánh giá thứ tự nét. Cô xem ảnh và điều chỉnh khi cần.</p>}
        </div>
        <form className="teacher-form-section teacher-stack" onSubmit={event => { event.preventDefault(); onGrade(selected.id, Number(score), feedback); }}>
          <h3>Nhận xét của cô</h3><label>Điểm (0–10)<input type="number" min="0" max="10" step="0.1" value={score} onChange={e => setScore(e.target.value)} required disabled={busy}/></label>
          <label>Lời động viên và hướng dẫn<textarea value={feedback} onChange={e => setFeedback(e.target.value)} maxLength={4000} rows={4} disabled={busy}/></label>
          <button className="teacher-button primary" disabled={busy || score === ''}>{busy ? 'Đang lưu…' : 'Lưu điểm và nhận xét'}</button>
        </form>
      </section>
      <section className="teacher-stack"><h3>Bài làm của bé</h3>{work.error && <p className="teacher-muted">{work.error}</p>}
        {work.images.map((image, i) => <div className="teacher-card" key={i}>{image.letter && <h3>Chữ {image.letter}</h3>}<img src={mediaUrl(image.image)} className="teacher-writing-image" alt={`Bài viết chữ ${image.letter || ''} của ${selected.studentName}`}/></div>)}
        <div className="teacher-review-answers">{work.answers.map((answer, i) => <div className="teacher-review-answer" key={i}><h3>Câu {i + 1}: {answer.question}</h3><p>Bé đã chọn: <strong>{answer.selected}</strong></p><p className="teacher-muted">Đáp án mẫu: {answer.answer}</p></div>)}</div>
        {rubric(selected).map((r, i) => <div className="teacher-review-answer" key={i}><h3>Chữ {r.letter} · {r.score}/100 độ bám mẫu</h3><p>{r.feedback}</p><p className="teacher-muted">Phủ mẫu: {r.coverage}/100 · Nét trong vùng mẫu: {r.precision}/100</p></div>)}
      </section>
    </div>}
  </TeacherDialog>;
}
