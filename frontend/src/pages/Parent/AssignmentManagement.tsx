import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { 
  ClipboardList, 
  Users, 
  Plus, 
  Calendar, 
  CheckCircle, 
  AlertCircle, 
  ShieldAlert, 
  Send,
  BookOpen
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

interface StudentLink {
  id: number;
  studentId: number;
  studentUsername: string;
  studentFullName: string;
  studentEmail: string;
  status: string;
}

interface AssignmentItem {
  id: number;
  title: string;
  description: string;
  contentType: string;
  contentData?: string;
  dueDate: string;
  className: string;
  createdAt: string;
  isSubmitted: boolean;
  score: number | null;
  assignedBy?: string;
}

const ParentAssignmentManagement: React.FC = () => {
  const { user } = useAuth();
  const [students, setStudents] = useState<StudentLink[]>([]);
  const [selectedStudentId, setSelectedStudentId] = useState<number | null>(null);
  const [assignments, setAssignments] = useState<AssignmentItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [filterType, setFilterType] = useState<'ALL' | 'TEACHER' | 'PARENT'>('ALL');

  // Form giao bài tập
  const [title, setTitle] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [msg, setMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const VIETNAMESE_ALPHABET = [
    'a', 'ă', 'â', 'b', 'c', 'd', 'đ', 'e', 'ê', 'g', 'h', 'i', 'k', 'l', 'm',
    'n', 'o', 'ô', 'ơ', 'p', 'q', 'r', 's', 't', 'u', 'ư', 'v', 'x', 'y',
    'A', 'Ă', 'Â', 'B', 'C', 'D', 'Đ', 'E', 'Ê', 'G', 'H', 'I', 'K', 'L', 'M',
    'N', 'O', 'Ô', 'Ơ', 'P', 'Q', 'R', 'S', 'T', 'U', 'Ư', 'V', 'X', 'Y'
  ];
  const [selectedLetters, setSelectedLetters] = useState<string[]>([]);

  // Sửa bài tập
  const [editAsmId, setEditAsmId] = useState<number | null>(null);
  const [editTitle, setEditTitle] = useState('');
  const [editDueDate, setEditDueDate] = useState('');
  const [editLetters, setEditLetters] = useState<string[]>([]);

  const handleToggleEditLetter = (letter: string) => {
    setEditLetters(prev => 
      prev.includes(letter) ? prev.filter(l => l !== letter) : [...prev, letter]
    );
  };

  const handleToggleLetter = (letter: string) => {
    setSelectedLetters(prev => 
      prev.includes(letter) ? prev.filter(l => l !== letter) : [...prev, letter]
    );
  };

  const fetchStudents = async () => {
    setLoading(true);
    try {
      const res = await api.get('/parent/students');
      setStudents(res.data);
      if (res.data.length > 0) {
        const firstStudent = res.data[0];
        setSelectedStudentId(firstStudent.studentId);
        fetchStudentAssignments(firstStudent.studentId);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const fetchStudentAssignments = async (studentId: number) => {
    try {
      const res = await api.get(`/parent/students/${studentId}/assignments`);
      setAssignments(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => {
    fetchStudents();
  }, []);

  const handleSelectStudent = (studentId: number) => {
    setSelectedStudentId(studentId);
    fetchStudentAssignments(studentId);
  };

  const handleAssignHomework = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedStudentId || !title.trim()) return;

    setSubmitting(true);
    setMsg(null);

    try {
      const emptyDesc = '';
      await api.post(
        `/parent/assignments?studentId=${selectedStudentId}&title=${encodeURIComponent(title)}&description=${encodeURIComponent(emptyDesc)}&dueDateStr=${encodeURIComponent(dueDate)}&letters=${encodeURIComponent(selectedLetters.join(','))}`
      );
      
      const studentName = students.find(s => s.studentId === selectedStudentId)?.studentFullName || 'bé';
      setMsg({ type: 'success', text: `Giao bài tập '${title}' cho ${studentName} thành công!` });
      
      setTitle('');
      setDueDate('');
      setSelectedLetters([]);
      fetchStudentAssignments(selectedStudentId);
    } catch (err: any) {
      setMsg({ type: 'error', text: err.response?.data?.message || 'Không thể giao bài tập cho bé' });
    } finally {
      setSubmitting(false);
    }
  };

  const hasAssignPerm = user?.roles?.includes('PERM_ASSIGN_HOMEWORK') || user?.roles?.includes('ROLE_ADMIN') || user?.roles?.includes('ROLE_TEACHER');

  const handleDeleteAssignment = async (e: React.MouseEvent, asmId: number) => {
    e.stopPropagation();
    if (!window.confirm('Bạn có chắc chắn muốn xóa bài tập này?')) return;
    try {
      await api.delete(`/parent/assignments/${asmId}`);
      if (selectedStudentId) fetchStudentAssignments(selectedStudentId);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Lỗi khi xóa bài tập');
    }
  };

  const handleUpdateAssignment = async (asmId: number) => {
    try {
      await api.put(`/parent/assignments/${asmId}?title=${encodeURIComponent(editTitle)}&dueDateStr=${encodeURIComponent(editDueDate)}&letters=${encodeURIComponent(editLetters.join(','))}`);
      setEditAsmId(null);
      if (selectedStudentId) fetchStudentAssignments(selectedStudentId);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Lỗi khi cập nhật bài tập');
    }
  };

  const handleOpenEdit = (asm: AssignmentItem) => {
    setEditTitle(asm.title);
    setEditDueDate(asm.dueDate ? new Date(asm.dueDate).toISOString().substring(0, 10) : '');
    
    let initialLetters: string[] = [];
    try {
      if (asm.contentData) {
        const data = JSON.parse(asm.contentData);
        if (data.letters && Array.isArray(data.letters)) {
          initialLetters = data.letters;
        }
      }
    } catch (err) {}
    setEditLetters(initialLetters);
    setEditAsmId(asm.id);
  };

  if (!hasAssignPerm) {
    return (
      <div className="bg-white border-2 border-rose-200 rounded-3xl p-8 max-w-2xl mx-auto my-12 text-center space-y-4 shadow-lg">
        <div className="w-16 h-16 rounded-full bg-rose-100 text-rose-600 flex items-center justify-center mx-auto">
          <ShieldAlert className="w-8 h-8" />
        </div>
        <h2 className="text-xl font-black text-slate-800">Chưa Được Cấp Quyền</h2>
        <p className="text-xs text-slate-600 font-medium leading-relaxed">
          Tài khoản Phụ huynh của bạn chưa được Admin cấp <strong>Quyền Giao Bài Tập Cho Con (`PERM_ASSIGN_HOMEWORK`)</strong>. Vui lòng liên hệ Admin hệ thống để mở quyền này.
        </p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-3">
        <div className="w-12 h-12 border-4 border-amber-200 border-t-amber-500 rounded-full animate-spin"></div>
        <p className="text-sm font-bold text-amber-800">Đang tải thông tin giao bài tập...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-12 w-full min-w-0">
      {/* Minimalist Header */}
      <div className="flex flex-col gap-1 px-2">
        <h1 className="text-xl sm:text-2xl font-black text-slate-800">Giao Bài Tập Cho Con</h1>
        <p className="text-slate-500 text-xs sm:text-sm font-bold">
          Tạo bài tập viết chữ và đặt hạn nộp bài.
        </p>
      </div>

      {students.length === 0 ? (
        <div className="bg-white border-2 border-amber-200 rounded-3xl p-8 text-center space-y-3 shadow-sm">
          <AlertCircle className="w-10 h-10 text-amber-500 mx-auto" />
          <h3 className="text-base font-black text-slate-800">Chưa Liên Kết Với Bé Nào</h3>
          <p className="text-xs text-slate-500 font-medium">Vui lòng vào mục "Liên kết với con" để nhập mã liên kết tài khoản bé trước khi giao bài tập nhé!</p>
        </div>
      ) : (
        <div className="space-y-6">
          {/* Nút bật tắt Form Giao bài tập */}
          <button
            onClick={() => setIsFormOpen(!isFormOpen)}
            className="w-full sm:w-auto px-6 py-3.5 bg-amber-500 hover:bg-amber-600 text-white font-black rounded-2xl flex items-center justify-center gap-2 shadow-md active:scale-95 transition-all"
          >
            <Plus className={`w-5 h-5 transition-transform ${isFormOpen ? 'rotate-45' : ''}`} />
            {isFormOpen ? 'Đóng form giao bài tập' : 'Giao bài tập mới'}
          </button>

          {/* Form Giao bài tập (Chỉ hiện khi isFormOpen = true) */}
          {isFormOpen && (
            <div className="bg-white border-2 border-amber-100 rounded-3xl p-5 shadow-sm space-y-4 animate-in fade-in slide-in-from-top-4 duration-300">
              <h3 className="text-base font-black text-slate-800 flex items-center gap-2 pb-3 border-b-2 border-amber-50">
                Tạo Bài Tập Cho Bé
              </h3>

            {msg && (
              <div className={`p-4 rounded-2xl text-xs font-bold flex items-center gap-2.5 ${
                msg.type === 'success' ? 'bg-emerald-50 text-emerald-800 border border-emerald-200' : 'bg-rose-50 text-rose-800 border border-rose-200'
              }`}>
                {msg.type === 'success' ? <CheckCircle className="w-4 h-4 text-emerald-600 shrink-0" /> : <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />}
                <span>{msg.text}</span>
              </div>
            )}

            <form onSubmit={handleAssignHomework} className="space-y-4">
              <div>
                <label className="block text-[11px] font-black text-slate-500 uppercase mb-1.5">Chọn Bé</label>
                <div className="space-y-2">
                  {students.map((st) => (
                    <button
                      key={st.studentId}
                      type="button"
                      onClick={() => handleSelectStudent(st.studentId)}
                      className={`w-full p-3 rounded-2xl text-left border-2 transition-all flex items-center justify-between ${
                        selectedStudentId === st.studentId
                          ? 'bg-amber-50 border-amber-400 text-amber-900 shadow-sm'
                          : 'bg-white border-slate-200 text-slate-700 hover:border-amber-200'
                      }`}
                    >
                      <span className="text-xs font-black">{st.studentFullName}</span>
                      <span className="text-[10px] font-bold text-slate-400">{st.studentUsername}</span>
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-[11px] font-black text-slate-500 uppercase mb-1.5">Tiêu đề</label>
                <input
                  type="text"
                  placeholder="Ví dụ: Luyện chữ a"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  className="w-full bg-white border-2 border-slate-200 rounded-xl px-4 py-2.5 text-xs font-bold text-slate-800 focus:outline-none focus:border-amber-400"
                  required
                />
              </div>

              <div>
                <label className="block text-[11px] font-black text-slate-500 uppercase mb-2">Chọn Chữ Cái (Nội dung luyện tô)</label>
                <div className="flex flex-wrap gap-2">
                  {VIETNAMESE_ALPHABET.map(letter => {
                    const isSelected = selectedLetters.includes(letter);
                    return (
                      <button
                        key={letter}
                        type="button"
                        onClick={() => handleToggleLetter(letter)}
                        className={`w-8 h-8 sm:w-10 sm:h-10 rounded-xl font-black text-sm flex items-center justify-center transition-all ${
                          isSelected 
                            ? 'bg-amber-500 text-white shadow-md ring-2 ring-amber-200 ring-offset-1' 
                            : 'bg-slate-100 text-slate-500 hover:bg-slate-200'
                        }`}
                      >
                        {letter}
                      </button>
                    );
                  })}
                </div>
                {selectedLetters.length === 0 && (
                  <p className="text-[10px] text-rose-500 font-bold mt-1.5">* Vui lòng chọn ít nhất 1 chữ cái để bé luyện tô.</p>
                )}
              </div>



              <div>
                <label className="block text-[11px] font-black text-slate-500 uppercase mb-1.5">Hạn nộp</label>
                <input
                  type="date"
                  value={dueDate}
                  onChange={(e) => setDueDate(e.target.value)}
                  className="w-full bg-white border-2 border-slate-200 rounded-xl px-4 py-2.5 text-xs font-bold text-slate-800 focus:outline-none focus:border-amber-400"
                />
              </div>

              <button
                type="submit"
                disabled={submitting || selectedLetters.length === 0}
                className="w-full py-3.5 rounded-2xl bg-amber-500 hover:bg-amber-600 text-white font-black text-xs shadow-md active:scale-95 transition-all flex items-center justify-center gap-2 disabled:opacity-50 mt-2 disabled:cursor-not-allowed"
              >
                <Send className="w-4 h-4" />
                {submitting ? 'Đang gửi...' : 'Giao Bài'}
              </button>
            </form>
          </div>
          )}

          {/* Danh sách bài tập đã giao cho bé */}
          <div className="bg-white border-2 border-slate-100 rounded-3xl p-5 shadow-sm space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b-2 border-slate-50">
              <h3 className="text-base font-black text-slate-800 flex items-center gap-2">
                <BookOpen className="w-5 h-5 text-amber-600" /> Các Bài Tập Đã Giao
              </h3>
              
              {/* Nút lọc */}
              <div className="flex gap-1.5">
                <button
                  onClick={() => setFilterType('ALL')}
                  className={`px-3 py-1.5 rounded-xl text-[10px] font-black transition-all ${
                    filterType === 'ALL' ? 'bg-amber-500 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  Tất cả ({assignments.length})
                </button>
                <button
                  onClick={() => setFilterType('TEACHER')}
                  className={`px-3 py-1.5 rounded-xl text-[10px] font-black transition-all ${
                    filterType === 'TEACHER' ? 'bg-sky-500 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  👩‍🏫 Lớp học ({assignments.filter(a => a.assignedBy !== 'PARENT').length})
                </button>
                <button
                  onClick={() => setFilterType('PARENT')}
                  className={`px-3 py-1.5 rounded-xl text-[10px] font-black transition-all ${
                    filterType === 'PARENT' ? 'bg-amber-500 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  👨‍👩‍👦 Gia đình ({assignments.filter(a => a.assignedBy === 'PARENT').length})
                </button>
              </div>
            </div>

            <div className="space-y-3">
              {assignments.length === 0 ? (
                <div className="py-12 text-center text-slate-400 font-bold text-xs">
                  Bé chưa có bài tập nào. Hãy dùng nút "Giao bài tập mới" để giao bài tập đầu tiên cho con nhé!
                </div>
              ) : assignments.filter(asm => {
                if (filterType === 'ALL') return true;
                if (filterType === 'TEACHER') return asm.assignedBy !== 'PARENT';
                return asm.assignedBy === 'PARENT';
              }).length === 0 ? (
                <div className="py-12 text-center text-slate-400 font-bold text-xs">
                  Không có bài tập nào trong mục này.
                </div>
              ) : (
                assignments
                  .filter(asm => {
                    if (filterType === 'ALL') return true;
                    if (filterType === 'TEACHER') return asm.assignedBy !== 'PARENT';
                    return asm.assignedBy === 'PARENT';
                  })
                  .map((asm) => (
                  <div 
                    key={asm.id} 
                    onClick={() => setExpandedId(expandedId === asm.id ? null : asm.id)}
                    className="p-3 sm:p-4 rounded-2xl border-2 border-slate-100 hover:border-amber-200 transition-all bg-slate-50/50 cursor-pointer"
                  >
                    <div className="flex items-center justify-between gap-3">
                      <div className="min-w-0 flex-1">
                        <h4 className="font-black text-sm text-slate-800 truncate">{asm.title}</h4>
                        <div className="text-[10px] text-slate-400 font-bold mt-1 flex items-center gap-2">
                          <span className={`px-1.5 py-0.5 rounded-md text-[9px] font-black ${
                            asm.assignedBy === 'PARENT' ? 'bg-amber-100 text-amber-800' : 'bg-sky-100 text-sky-800'
                          }`}>
                            {asm.assignedBy === 'PARENT' ? '👨‍👩‍👦 Gia đình' : '👩‍🏫 Lớp học'}
                          </span>
                          {asm.dueDate && (
                            <span className="flex items-center gap-1">
                              <Calendar className="w-3 h-3 text-slate-400" /> 
                              {new Date(asm.dueDate).toLocaleDateString('vi-VN')}
                            </span>
                          )}
                        </div>
                      </div>

                      <span className={`px-2.5 py-1 rounded-full text-[10px] font-black shrink-0 ${
                        asm.isSubmitted ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-200 text-slate-600'
                      }`}>
                        {asm.isSubmitted ? `Đã nộp (${asm.score}đ)` : 'Chưa nộp'}
                      </span>
                    </div>

                    {/* Chi tiết (Chỉ hiện khi bấm vào) */}
                    {expandedId === asm.id && (
                      <div className="mt-3 pt-3 border-t border-slate-200 text-xs text-slate-600 font-medium space-y-2 animate-in fade-in slide-in-from-top-2" onClick={e => e.stopPropagation()}>
                        {editAsmId === asm.id ? (
                          <div className="space-y-3 bg-white p-3 rounded-xl border-2 border-amber-100">
                            <div>
                              <label className="block text-[10px] font-black text-slate-500 uppercase mb-1">Tên bài tập mới</label>
                              <input 
                                type="text"
                                value={editTitle}
                                onChange={e => setEditTitle(e.target.value)}
                                className="w-full border-2 border-slate-200 rounded-lg px-3 py-1.5 text-xs font-bold focus:border-amber-400 focus:outline-none"
                              />
                            </div>
                            <div>
                              <label className="block text-[10px] font-black text-slate-500 uppercase mb-1">Hạn nộp mới</label>
                              <input 
                                type="date"
                                value={editDueDate}
                                onChange={e => setEditDueDate(e.target.value)}
                                className="w-full border-2 border-slate-200 rounded-lg px-3 py-1.5 text-xs font-bold focus:border-amber-400 focus:outline-none"
                              />
                            </div>
                            <div>
                              <label className="block text-[10px] font-black text-slate-500 uppercase mb-1">Chữ cái cần luyện</label>
                              <div className="flex flex-wrap gap-1.5">
                                {VIETNAMESE_ALPHABET.map(letter => {
                                  const isSelected = editLetters.includes(letter);
                                  return (
                                    <button
                                      key={letter}
                                      type="button"
                                      onClick={() => handleToggleEditLetter(letter)}
                                      className={`w-6 h-6 sm:w-8 sm:h-8 rounded-lg font-black text-[10px] sm:text-xs flex items-center justify-center transition-all ${
                                        isSelected 
                                          ? 'bg-amber-500 text-white shadow ring-1 ring-amber-200 ring-offset-1' 
                                          : 'bg-slate-100 text-slate-500 hover:bg-slate-200'
                                      }`}
                                    >
                                      {letter}
                                    </button>
                                  );
                                })}
                              </div>
                              {editLetters.length === 0 && (
                                <p className="text-[9px] text-rose-500 font-bold mt-1">* Vui lòng chọn ít nhất 1 chữ cái.</p>
                              )}
                            </div>
                            <div className="flex gap-2 justify-end mt-2">
                              <button onClick={() => setEditAsmId(null)} className="px-3 py-1.5 rounded-lg bg-slate-100 text-slate-600 font-bold hover:bg-slate-200">Hủy</button>
                              <button onClick={() => handleUpdateAssignment(asm.id)} disabled={editLetters.length === 0} className="px-3 py-1.5 rounded-lg bg-amber-500 text-white font-bold hover:bg-amber-600 shadow disabled:opacity-50 disabled:cursor-not-allowed">Lưu</button>
                            </div>
                          </div>
                        ) : (
                          <>
                            <p>{asm.description || 'Không có ghi chú'}</p>
                            <div className="flex items-center justify-between">
                              <p className="text-[10px] text-slate-400 font-bold">Ngày giao: {new Date(asm.createdAt).toLocaleDateString('vi-VN')}</p>
                              {asm.assignedBy === 'PARENT' && (
                                <div className="flex gap-2">
                                  <button onClick={(e) => { e.stopPropagation(); handleOpenEdit(asm); }} className="px-2.5 py-1 text-[10px] bg-sky-100 text-sky-600 font-black rounded-lg hover:bg-sky-200 transition-colors">Sửa</button>
                                  <button onClick={(e) => handleDeleteAssignment(e, asm.id)} className="px-2.5 py-1 text-[10px] bg-rose-100 text-rose-600 font-black rounded-lg hover:bg-rose-200 transition-colors">Xóa</button>
                                </div>
                              )}
                            </div>
                          </>
                        )}
                      </div>
                    )}
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ParentAssignmentManagement;
