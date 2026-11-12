import { useCallback, useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { GraduationCap, Users, ClipboardList, Plus, RefreshCw, Copy, Pencil, Trash2, UserPlus, ArrowRight, BookOpen } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import TeacherDialog from '../../components/TeacherDialog';
import { teacherError } from '../../utils/teacherAssignments';

export default function TeacherDashboard() {
  const { user } = useAuth(); const classesView = useLocation().pathname.endsWith('/classes');
  const canManage = user?.roles?.includes('PERM_MANAGE_CLASSES');
  const canAssign = user?.roles?.includes('PERM_ASSIGN_HOMEWORK');
  const [classes, setClasses] = useState([]); const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true); const [error, setError] = useState(''); const [notice, setNotice] = useState('');
  const [query, setQuery] = useState(''); const [selectedId, setSelectedId] = useState(null);
  const [students, setStudents] = useState([]); const [membersLoading, setMembersLoading] = useState(false);
  const [memberRevision, setMemberRevision] = useState(0); const [studentName, setStudentName] = useState('');
  const [form, setForm] = useState(null); const [confirm, setConfirm] = useState(null); const [busy, setBusy] = useState(false);
  const selected = classes.find(c => c.id === selectedId);
  const load = useCallback(async () => {
    setLoading(true); setError('');
    const [classReply, statReply] = await Promise.allSettled([api.get('/teacher/class'), api.get('/teacher/stats')]);
    if (classReply.status === 'fulfilled' && Array.isArray(classReply.value.data)) {
      setClasses(classReply.value.data); setSelectedId(old => classReply.value.data.some(c => c.id === old) ? old : classReply.value.data[0]?.id ?? null);
    } else setError('Chưa tải được lớp học. Cô thử tải lại nhé.');
    if (statReply.status === 'fulfilled') setStats(statReply.value.data); else setStats(null);
    setLoading(false);
  }, []);
  useEffect(() => { load(); }, [load]);
  useEffect(() => {
    setStudents([]); if (!selectedId) return;
    const abort = new AbortController(); setMembersLoading(true);
    api.get(`/teacher/class/${selectedId}/students`, { signal: abort.signal }).then(r => setStudents(r.data))
      .catch(e => { if (!abort.signal.aborted) setError(teacherError(e)); }).finally(() => { if (!abort.signal.aborted) setMembersLoading(false); });
    return () => abort.abort();
  }, [selectedId, memberRevision]);
  async function act(fn, success) {
    setBusy(true); setError('');
    try { await fn(); setNotice(success); setForm(null); setConfirm(null); setStudentName(''); setMemberRevision(n => n + 1); await load(); }
    catch (e) { setError(teacherError(e)); } finally { setBusy(false); }
  }
  function saveClass(event) {
    event.preventDefault(); const params = { className: form.name.trim(), description: form.description.trim() };
    if (!params.className) return;
    act(() => form.id ? api.put(`/teacher/class/${form.id}`, null, { params }) : api.post('/teacher/class', null, { params }), form.id ? 'Đã cập nhật lớp học.' : 'Lớp mới đã sẵn sàng. Cô gửi mã mời cho học sinh nhé.');
  }
  async function copyCode() {
    try { await navigator.clipboard.writeText(selected.inviteCode); setNotice('Đã sao chép mã tham gia lớp.'); }
    catch { setError('Chưa sao chép được. Cô có thể chọn và sao chép mã đang hiển thị.'); }
  }
  const totals = [
    { label: 'Lớp đang quản lý', value: stats?.totalClasses, Icon: GraduationCap },
    { label: 'Học sinh', value: stats?.totalStudents, Icon: Users },
    { label: 'Bài đã giao', value: stats?.totalAssignments, Icon: ClipboardList },
  ];
  return <>
    <div className="teacher-page-head"><div><h1>{classesView ? 'Lớp học của cô' : 'Mỗi ngày một bước tiến'}</h1><p className="teacher-muted">{classesView ? 'Quản lý lớp, mã mời và danh sách học sinh.' : 'Theo dõi lớp học và chuẩn bị những bài tập vừa sức cho bé.'}</p></div>
      <div className="teacher-row"><button className="teacher-button" onClick={load} disabled={loading}><RefreshCw size={18}/> Tải lại</button>
        {canManage && <button className="teacher-button primary" onClick={() => { setError(''); setForm({ name: '', description: '' }); }}><Plus size={18}/> Tạo lớp</button>}</div></div>
    {error && <div className="teacher-alert" role="alert">{error}</div>}
    {notice && <div className="teacher-success" role="status">{notice}</div>}
    {!classesView && <><div className="teacher-stats">{totals.map(({ label, value, Icon }) => <div className="teacher-stat" key={label}><Icon size={30}/><div><strong>{Number.isFinite(value) ? value : '—'}</strong><span>{label}</span></div></div>)}</div>
      <div className="teacher-card" style={{ marginBottom: 26 }}><div className="teacher-row teacher-spread"><div><h2>Chuẩn bị bài học tiếp theo</h2><p className="teacher-muted">Tập viết chữ, đố vui ghép chữ và ghép câu — cùng một nhịp học với bé.</p></div>{canAssign && <Link to="/teacher/assignments?new=1" className="teacher-button green">Soạn bài mới <ArrowRight size={18}/></Link>}</div>
        <div className="teacher-row"><span className="teacher-chip writing">Tập viết chữ</span><span className="teacher-chip quiz">Đố vui ghép chữ</span><span className="teacher-chip sentence">Ghép câu</span></div>
        {stats && <p className="teacher-muted" style={{ marginTop: 16, marginBottom: 0 }}>{stats.totalSubmissions ?? '—'} bài nộp · {stats.gradedSubmissions ?? '—'} bài đã có điểm · {stats.ungradedSubmissions ?? '—'} bài chưa có điểm</p>}
      </div></>}
    <div className="teacher-two-column"><section className="teacher-stack"><h2>Danh sách lớp</h2><label className="teacher-search">Tìm lớp<input value={query} onChange={e => setQuery(e.target.value)} placeholder="Nhập tên lớp…"/></label>
      {loading && <p className="teacher-loading" role="status">Đang tải lớp học…</p>}
      {!loading && !classes.length && <div className="teacher-empty"><GraduationCap size={42}/><h3>Cùng tạo lớp đầu tiên</h3><p>Học sinh tham gia bằng mã mời riêng của lớp.</p>{canManage && <button className="teacher-button primary" onClick={() => setForm({ name: '', description: '' })}>Tạo lớp</button>}</div>}
      <div className="teacher-class-list">{classes.filter(c => c.name.toLocaleLowerCase('vi').includes(query.toLocaleLowerCase('vi'))).map(c => <button key={c.id} className={`teacher-class-item ${selectedId === c.id ? 'active' : ''}`} onClick={() => { setSelectedId(c.id); setStudentName(''); }} aria-pressed={selectedId === c.id}><div className="teacher-class-top"><strong>{c.name}</strong><GraduationCap size={22}/></div><p className="teacher-muted">{c.description || 'Lớp học tiếng Việt'}</p></button>)}</div>
    </section><section className="teacher-card">
      {!selected ? <div className="teacher-empty"><BookOpen size={40}/><p>Chọn một lớp để xem học sinh và mã mời.</p></div> : <>
        <div className="teacher-row teacher-spread"><h2>{selected.name}</h2>{canManage && <div className="teacher-row"><button className="teacher-icon" aria-label={`Sửa ${selected.name}`} onClick={() => { setError(''); setForm({ id: selected.id, name: selected.name, description: selected.description || '' }); }}><Pencil size={18}/></button><button className="teacher-icon" aria-label={`Xóa ${selected.name}`} onClick={() => setConfirm({ kind: 'class', id: selected.id, name: selected.name })}><Trash2 size={18}/></button></div>}</div>
        {selected.description && <p className="teacher-muted">{selected.description}</p>}
        <div className="teacher-row" style={{ marginBottom: 20 }}>{selected.inviteCode && <><span className="teacher-invite">Mã lớp: {selected.inviteCode}</span><button className="teacher-icon" onClick={copyCode} aria-label="Sao chép mã lớp"><Copy size={18}/></button></>}{canAssign && <Link to={`/teacher/assignments?class=${selected.id}`} className="teacher-button">Bài tập của lớp <ArrowRight size={17}/></Link>}</div>
        {canManage && <form className="teacher-toolbar" onSubmit={e => { e.preventDefault(); act(() => api.post(`/teacher/class/${selected.id}/students`, null, { params: { studentUsername: studentName.trim() } }), 'Đã thêm học sinh vào lớp.'); }}><label>Tên đăng nhập học sinh<input value={studentName} onChange={e => setStudentName(e.target.value)} placeholder="Ví dụ: student1" required maxLength={100}/></label><button className="teacher-button green" disabled={busy || !studentName.trim()}><UserPlus size={18}/> Thêm vào lớp</button></form>}
        <div className="teacher-row teacher-spread"><h3>Học sinh trong lớp</h3><span className="teacher-chip">{membersLoading ? '…' : students.length} học sinh</span></div>
        {membersLoading ? <p role="status" className="teacher-loading">Đang tải danh sách…</p> : !students.length ? <p className="teacher-muted">Chưa có học sinh. Cô gửi mã lớp cho bé hoặc thêm bằng tên đăng nhập.</p> : <div className="teacher-table-scroll"><table className="teacher-table"><thead><tr><th>Học sinh</th><th>Tên đăng nhập</th>{canManage && <th>Thao tác</th>}</tr></thead><tbody>{students.map(s => <tr key={s.id}><td><strong>{s.fullName || s.username}</strong></td><td>{s.username}</td>{canManage && <td><button className="teacher-icon" aria-label={`Xóa ${s.fullName || s.username} khỏi lớp`} onClick={() => setConfirm({ kind: 'student', id: s.id, classId: selected.id, name: s.fullName || s.username })}><Trash2 size={16}/></button></td>}</tr>)}</tbody></table></div>}
      </>}
    </section></div>
    {form && <TeacherDialog title={form.id ? 'Chỉnh sửa lớp học' : 'Tạo lớp học mới'} busy={busy} onClose={() => setForm(null)}><form onSubmit={saveClass} className="teacher-stack">{error && <p className="teacher-alert" role="alert">{error}</p>}<label>Tên lớp<input autoFocus value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} required maxLength={150}/></label><label>Giới thiệu ngắn<textarea value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} maxLength={1000}/></label><div className="teacher-form-actions"><button type="button" className="teacher-button" onClick={() => setForm(null)} disabled={busy}>Hủy</button><button className="teacher-button primary" disabled={busy}>{busy ? 'Đang lưu…' : 'Lưu lớp học'}</button></div></form></TeacherDialog>}
    {confirm && <TeacherDialog title={confirm.kind === 'class' ? 'Xóa lớp học?' : 'Đưa học sinh khỏi lớp?'} busy={busy} onClose={() => setConfirm(null)}><p>{confirm.kind === 'class' ? `Cô muốn xóa lớp “${confirm.name}”? Hãy kiểm tra bài và dữ liệu của lớp trước khi xóa.` : `Học sinh ${confirm.name} sẽ không nhận thêm bài mới của lớp. Bài đã nộp vẫn được giữ.`}</p>{error && <p role="alert" className="teacher-alert">{error}</p>}<div className="teacher-form-actions"><button className="teacher-button" onClick={() => setConfirm(null)} disabled={busy}>Giữ lại</button><button className="teacher-button danger" disabled={busy} onClick={() => act(() => api.delete(confirm.kind === 'class' ? `/teacher/class/${confirm.id}` : `/teacher/class/${confirm.classId}/students/${confirm.id}`), 'Đã cập nhật lớp học.')}>{busy ? 'Đang xử lý…' : 'Xác nhận xóa'}</button></div></TeacherDialog>}
  </>;
}