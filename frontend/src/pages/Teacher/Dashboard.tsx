import React, { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import { Users, Plus, Trash2, UserPlus, Key, ChevronRight, Folder, X, BookOpen, AlertCircle, GraduationCap, Copy, Check, Edit } from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend
} from 'recharts';

interface ClassItem {
  id: number;
  name: string;
  description: string;
  inviteCode: string;
  teacherName: string;
}

interface StudentItem {
  id: number;
  username: string;
  fullName: string;
  email: string;
  phoneNumber: string;
}

const TeacherDashboard: React.FC = () => {
  const { user } = useAuth();
  const location = useLocation();
  const isClassesView = location.pathname.includes('/classes');
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [selectedClass, setSelectedClass] = useState<ClassItem | null>(null);
  const [students, setStudents] = useState<StudentItem[]>([]);

  const [newClassName, setNewClassName] = useState('');
  const [newClassDesc, setNewClassDesc] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createError, setCreateError] = useState('');
  const [createLoading, setCreateLoading] = useState(false);

  const [showEditModal, setShowEditModal] = useState(false);
  const [editClassId, setEditClassId] = useState<number | null>(null);
  const [editClassName, setEditClassName] = useState('');
  const [editClassDesc, setEditClassDesc] = useState('');
  const [editLoading, setEditLoading] = useState(false);
  const [editError, setEditError] = useState('');

  const [newStudentUser, setNewStudentUser] = useState('');
  const [showAddStudentForm, setShowAddStudentForm] = useState(false);
  const [addStudentError, setAddStudentError] = useState('');

  const [confirmModal, setConfirmModal] = useState<{
    isOpen: boolean;
    title: string;
    message: string;
    onConfirm: () => void;
    confirmText?: string;
    type?: 'danger' | 'warning' | 'info';
  }>({
    isOpen: false,
    title: '',
    message: '',
    onConfirm: () => {},
    confirmText: 'Xác nhận',
    type: 'danger'
  });

  const [loading, setLoading] = useState(true);
  const [statsLoading, setStatsLoading] = useState(true);
  const [fetchError, setFetchError] = useState('');
  const [actionError, setActionError] = useState('');

  const [stats, setStats] = useState<any>({
    totalClasses: 0,
    totalStudents: 0,
    totalAssignments: 0,
    totalSubmissions: 0,
    gradedSubmissions: 0,
    ungradedSubmissions: 0
  });

  // Trạng thái sao chép mã mời
  const [copiedCode, setCopiedCode] = useState<string | null>(null);

  const fetchStats = async () => {
    try {
      const res = await api.get('/teacher/stats');
      setStats(res.data);
    } catch (err) {
      console.error('Lỗi lấy thống kê giáo viên:', err);
    } finally {
      setStatsLoading(false);
    }
  };

  const fetchClasses = async () => {
    setFetchError('');
    try {
      const res = await api.get('/teacher/class');
      setClasses(res.data);
      if (res.data.length > 0 && !selectedClass) {
        handleSelectClass(res.data[0]);
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Không thể lấy danh sách lớp học';
      setFetchError(msg);
      console.error('Lỗi lấy lớp học:', err.response?.status, msg);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchClasses();
    fetchStats();
  }, []);

  const handleSelectClass = async (clazz: ClassItem) => {
    setSelectedClass(clazz);
    setShowAddStudentForm(false);
    setNewStudentUser('');
    setAddStudentError('');
    try {
      const res = await api.get(`/teacher/class/${clazz.id}/students`);
      setStudents(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleCreateClass = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newClassName.trim()) return;
    setCreateLoading(true);
    setCreateError('');
    try {
      await api.post(`/teacher/class?className=${encodeURIComponent(newClassName)}&description=${encodeURIComponent(newClassDesc)}`);
      setNewClassName('');
      setNewClassDesc('');
      setShowCreateModal(false);
      await fetchClasses();
    } catch (err: any) {
      const status = err.response?.status;
      const msg = err.response?.data?.message || err.message || 'Có lỗi xảy ra khi tạo lớp';
      setCreateError(`[${status}] ${msg}`);
      console.error('Lỗi tạo lớp:', err.response);
    } finally {
      setCreateLoading(false);
    }
  };

  const handleOpenEditModal = (c: ClassItem) => {
    setEditClassId(c.id);
    setEditClassName(c.name);
    setEditClassDesc(c.description || '');
    setEditError('');
    setShowEditModal(true);
  };

  const handleUpdateClass = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editClassId) return;
    setEditLoading(true);
    setEditError('');
    try {
      const params = new URLSearchParams();
      params.append('className', editClassName);
      params.append('description', editClassDesc);

      const res = await api.put(`/teacher/class/${editClassId}`, null, { params });
      
      // Update local state
      setClasses(classes.map(c => c.id === editClassId ? res.data : c));
      setSelectedClass(res.data);
      setShowEditModal(false);
    } catch (err: any) {
      setEditError(err.response?.data?.message || 'Chỉnh sửa lớp học thất bại');
    } finally {
      setEditLoading(false);
    }
  };

  const handleDeleteClass = (classId: number) => {
    setConfirmModal({
      isOpen: true,
      title: 'Xóa lớp học',
      message: 'Bạn có chắc chắn muốn XÓA lớp học này không? Tất cả học sinh, bài tập và bài nộp liên quan cũng sẽ bị xóa vĩnh viễn và không thể khôi phục!',
      type: 'danger',
      confirmText: 'Xóa vĩnh viễn 💥',
      onConfirm: async () => {
        try {
          await api.delete(`/teacher/class/${classId}`);
          const updatedClasses = classes.filter(c => c.id !== classId);
          setClasses(updatedClasses);
          if (selectedClass?.id === classId) {
            setSelectedClass(updatedClasses.length > 0 ? updatedClasses[0] : null);
          }
        } catch (err: any) {
          setActionError(err.response?.data?.message || 'Xóa lớp học thất bại');
        } finally {
          setConfirmModal(prev => ({ ...prev, isOpen: false }));
        }
      }
    });
  };

  const handleAddStudent = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedClass || !newStudentUser) return;
    setAddStudentError('');
    try {
      await api.post(`/teacher/class/${selectedClass.id}/students?studentUsername=${encodeURIComponent(newStudentUser)}`);
      setNewStudentUser('');
      setShowAddStudentForm(false);
      handleSelectClass(selectedClass);
    } catch (err: any) {
      setAddStudentError(err.response?.data?.message || 'Không thể thêm học sinh vào lớp');
    }
  };

  const handleRemoveStudent = (studentId: number) => {
    if (!selectedClass) return;
    setConfirmModal({
      isOpen: true,
      title: 'Xóa học sinh khỏi lớp',
      message: 'Bạn có chắc chắn muốn xóa học sinh này khỏi lớp không?',
      type: 'warning',
      confirmText: 'Xác nhận xóa',
      onConfirm: async () => {
        try {
          await api.delete(`/teacher/class/${selectedClass.id}/students/${studentId}`);
          handleSelectClass(selectedClass);
        } catch (err) {
          setActionError('Xóa học sinh thất bại');
        } finally {
          setConfirmModal(prev => ({ ...prev, isOpen: false }));
        }
      }
    });
  };

  const handleCopyInviteCode = (code: string, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    navigator.clipboard.writeText(code);
    setCopiedCode(code);
    setTimeout(() => setCopiedCode(null), 2000);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <div className="w-12 h-12 border-4 border-sky-200 border-t-sky-500 rounded-full animate-spin"></div>
      </div>
    );
  }

  const hasManageClasses = user?.roles?.includes('PERM_MANAGE_CLASSES') || user?.roles?.includes('ROLE_ADMIN');

  if (isClassesView && !hasManageClasses) {
    return (
      <div className="bg-white border-2 border-rose-200 rounded-3xl p-8 max-w-2xl mx-auto my-12 text-center space-y-4 shadow-lg animate-fade-in" style={{ fontFamily: "'Nunito', sans-serif" }}>
        <div className="w-16 h-16 rounded-full bg-rose-100 text-rose-600 flex items-center justify-center mx-auto">
          <AlertCircle className="w-8 h-8" />
        </div>
        <h2 className="text-xl font-black text-slate-800">Chưa Được Cấp Quyền</h2>
        <p className="text-sm text-slate-500 font-bold leading-relaxed">
          Tài khoản của bạn chưa được Admin cấp <strong>Quyền Quản Lý Lớp Học & Học Sinh (`PERM_MANAGE_CLASSES`)</strong>. Vui lòng liên hệ Admin hệ thống để mở quyền này.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6 p-2" style={{ fontFamily: "'Nunito', sans-serif" }}>

      {/* TAB 1: THỐNG KÊ TỔNG QUAN */}
      {!isClassesView && (
        <div className="space-y-6 animate-fade-in">
          {statsLoading ? (
            <div className="flex flex-col items-center justify-center min-h-[30vh] gap-3">
              <div className="w-10 h-10 border-4 border-sky-200 border-t-sky-600 rounded-full animate-spin"></div>
              <p className="text-xs font-bold text-sky-850">Đang tải số liệu thống kê...</p>
            </div>
          ) : (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
                <div className="bg-white border-2 border-sky-100 p-6 rounded-3xl shadow-sm space-y-2">
                  <span className="text-xs font-black uppercase text-slate-500 tracking-wider">Số lớp học của cô</span>
                  <p className="text-4xl font-black text-sky-700">{stats.totalClasses || 0}</p>
                  <p className="text-[11px] font-bold text-slate-400">Lớp học đang quản lý</p>
                </div>
                <div className="bg-white border-2 border-indigo-100 p-6 rounded-3xl shadow-sm space-y-2">
                  <span className="text-xs font-black uppercase text-slate-500 tracking-wider">Số học sinh của cô</span>
                  <p className="text-4xl font-black text-indigo-600">{stats.totalStudents || 0}</p>
                  <p className="text-[11px] font-bold text-slate-400">Học sinh trong các lớp</p>
                </div>
                <div className="bg-white border-2 border-emerald-100 p-6 rounded-3xl shadow-sm space-y-2">
                  <span className="text-xs font-black uppercase text-slate-500 tracking-wider">Số bài tập đã giao</span>
                  <p className="text-4xl font-black text-emerald-600">{stats.totalAssignments || 0}</p>
                  <p className="text-[11px] font-bold text-slate-400">Bài tập đã tạo</p>
                </div>
                <div className="bg-white border-2 border-rose-100 p-6 rounded-3xl shadow-sm space-y-2">
                  <span className="text-xs font-black uppercase text-slate-500 tracking-wider">Bài nộp của bé</span>
                  <p className="text-4xl font-black text-rose-500">{stats.totalSubmissions || 0}</p>
                  <p className="text-[11px] font-bold text-slate-400">Tổng số bài nộp nhận được</p>
                </div>
              </div>

              {/* Chi tiết Bài nộp đã/chưa chấm */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div className="bg-gradient-to-br from-emerald-50 to-teal-50 border-2 border-emerald-200 p-6 rounded-3xl space-y-2 text-center shadow-sm">
                  <span className="text-xs font-black text-emerald-800 uppercase tracking-widest">Bài nộp đã chấm điểm</span>
                  <p className="text-3xl font-black text-emerald-650">{stats.gradedSubmissions || 0} bài</p>
                  <p className="text-xs font-bold text-slate-400 mt-1">Đã có nhận xét & điểm số</p>
                </div>
                <div className="bg-gradient-to-br from-amber-50 to-orange-50 border-2 border-amber-200 p-6 rounded-3xl space-y-2 text-center shadow-sm">
                  <span className="text-xs font-black text-amber-800 uppercase tracking-widest">Bài nộp chưa chấm</span>
                  <p className="text-3xl font-black text-amber-600">{stats.ungradedSubmissions || 0} bài</p>
                  <p className="text-xs font-bold text-slate-400 mt-1">Cần cô kiểm tra & chấm điểm</p>
                </div>
              </div>

              {/* ===== BIỂU ĐỒ THỐNG KÊ ===== */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

                {/* Biểu đồ Bar: Tổng quan hoạt động */}
                <div className="bg-white border-2 border-sky-100 rounded-3xl p-6 shadow-sm">
                  <h3 className="text-sm font-black text-slate-700 mb-1">📊 Tổng quan hoạt động</h3>
                  <p className="text-[11px] font-bold text-slate-400 mb-5">So sánh các chỉ số lớp học của cô</p>
                  <ResponsiveContainer width="100%" height={220}>
                    <BarChart
                      data={[
                        { name: 'Lớp học', value: stats.totalClasses || 0, fill: '#0ea5e9' },
                        { name: 'Học sinh', value: stats.totalStudents || 0, fill: '#6366f1' },
                        { name: 'Bài tập', value: stats.totalAssignments || 0, fill: '#10b981' },
                        { name: 'Bài nộp', value: stats.totalSubmissions || 0, fill: '#f43f5e' },
                      ]}
                      margin={{ top: 5, right: 10, left: -20, bottom: 5 }}
                    >
                      <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                      <XAxis dataKey="name" tick={{ fontSize: 11, fontWeight: 700, fill: '#64748b' }} />
                      <YAxis tick={{ fontSize: 11, fill: '#94a3b8' }} allowDecimals={false} />
                      <Tooltip
                        contentStyle={{ borderRadius: '12px', border: '2px solid #e0f2fe', fontSize: '12px', fontWeight: 700 }}
                        cursor={{ fill: '#f0f9ff' }}
                      />
                      <Bar dataKey="value" radius={[8, 8, 0, 0]}>
                        {[
                          { fill: '#0ea5e9' },
                          { fill: '#6366f1' },
                          { fill: '#10b981' },
                          { fill: '#f43f5e' },
                        ].map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={entry.fill} />
                        ))}
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                </div>

                {/* Biểu đồ Donut: Tỉ lệ bài nộp */}
                <div className="bg-white border-2 border-indigo-100 rounded-3xl p-6 shadow-sm">
                  <h3 className="text-sm font-black text-slate-700 mb-1">🍩 Tình trạng bài nộp</h3>
                  <p className="text-[11px] font-bold text-slate-400 mb-2">Tỉ lệ bài đã chấm và chưa chấm</p>
                  {(stats.gradedSubmissions || 0) + (stats.ungradedSubmissions || 0) === 0 ? (
                    <div className="flex items-center justify-center h-[200px] text-slate-300">
                      <div className="text-center">
                        <div className="text-5xl mb-2">📭</div>
                        <p className="text-xs font-bold">Chưa có bài nộp nào</p>
                      </div>
                    </div>
                  ) : (
                    <ResponsiveContainer width="100%" height={220}>
                      <PieChart>
                        <Pie
                          data={[
                            { name: 'Đã chấm', value: stats.gradedSubmissions || 0 },
                            { name: 'Chưa chấm', value: stats.ungradedSubmissions || 0 },
                          ]}
                          cx="50%"
                          cy="50%"
                          innerRadius={55}
                          outerRadius={85}
                          paddingAngle={4}
                          dataKey="value"
                        >
                          <Cell fill="#10b981" />
                          <Cell fill="#f59e0b" />
                        </Pie>
                        <Tooltip
                          contentStyle={{ borderRadius: '12px', border: '2px solid #e0f2fe', fontSize: '12px', fontWeight: 700 }}
                          formatter={(value: any, name: any) => [`${value} bài`, name]}
                        />
                        <Legend
                          iconType="circle"
                          iconSize={8}
                          wrapperStyle={{ fontSize: '11px', fontWeight: 700 }}
                        />
                      </PieChart>
                    </ResponsiveContainer>
                  )}
                </div>

              </div>
            </>
          )}
        </div>
      )}

      {/* TAB 2: QUẢN LÝ LỚP HỌC & HỌC SINH */}
      {isClassesView && hasManageClasses && (
        <div className="space-y-6 animate-fade-in">
          {/* Header */}
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-gradient-to-r from-sky-400 to-blue-500 rounded-3xl p-6 shadow-lg shadow-sky-200/50">
            <div className="flex items-center gap-4">
              <div className="w-14 h-14 rounded-2xl bg-white/20 backdrop-blur flex items-center justify-center border-2 border-white/40">
                <GraduationCap className="w-7 h-7 text-white" />
              </div>
              <div>
                <h1 className="text-xl sm:text-2xl font-black text-white">Quản lý lớp học</h1>
                <p className="text-sky-100 text-sm font-bold mt-0.5">Tạo lớp, cấp mã mời và quản lý học sinh của cô</p>
              </div>
            </div>
            <button
              onClick={() => { setShowCreateModal(true); setCreateError(''); }}
              className="px-5 py-3 rounded-2xl bg-white text-sky-600 font-black text-sm shadow-md hover:shadow-lg active:scale-95 transition-all flex items-center gap-2 self-start sm:self-center"
            >
              <Plus className="w-4 h-4" />
              Tạo lớp học mới
            </button>
          </div>

          {/* Fetch error */}
          {fetchError && (
            <div className="p-4 bg-rose-50 border-2 border-rose-200 rounded-2xl text-sm font-bold text-rose-600 flex items-center gap-3">
              <AlertCircle className="w-5 h-5 shrink-0" />
              <span>{fetchError}</span>
            </div>
          )}

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Danh sách lớp */}
            <div className="space-y-3">
              <h2 className="text-base font-black text-sky-800 flex items-center gap-2">
                <Folder className="w-5 h-5 text-sky-500" />
                Danh sách lớp ({classes.length})
              </h2>

              {classes.length === 0 ? (
                <div className="bg-white border-3 border-sky-100 rounded-2xl p-8 text-center">
                  <BookOpen className="w-10 h-10 text-sky-200 mx-auto mb-3" />
                  <p className="text-slate-500 text-sm font-bold">Cô chưa tạo lớp học nào.</p>
                  <p className="text-slate-400 text-xs mt-1">Nhấn nút phía trên để tạo lớp nhé!</p>
                </div>
              ) : (
                classes.map((c) => {
                  const isSelected = selectedClass?.id === c.id;
                  const isCopied = copiedCode === c.inviteCode;
                  return (
                    <div
                      key={c.id}
                      onClick={() => handleSelectClass(c)}
                      className={`w-full text-left p-4 rounded-2xl border-3 transition-all duration-200 cursor-pointer ${
                        isSelected
                          ? 'bg-sky-50 border-sky-400 shadow-md shadow-sky-100'
                          : 'bg-white border-sky-100 hover:border-sky-300 hover:bg-sky-50/50'
                      }`}
                    >
                      <h3 className={`font-black text-sm ${isSelected ? 'text-sky-700' : 'text-slate-700'}`}>{c.name}</h3>
                      <p className="text-slate-400 text-xs mt-1 truncate">{c.description || 'Không có mô tả'}</p>
                      
                      <div className="flex items-center justify-between mt-3">
                        {/* Thẻ mã mời + Nút Sao chép */}
                        <div className="flex items-center gap-1.5">
                          <span className="text-[10px] font-black text-sky-600 bg-sky-100 px-2.5 py-1 rounded-full flex items-center gap-1">
                            <Key className="w-3 h-3 text-sky-500" />
                            Mã: {c.inviteCode}
                          </span>
                          <button
                            onClick={(e) => handleCopyInviteCode(c.inviteCode, e)}
                            className={`px-2 py-0.5 rounded-full text-[10px] font-black flex items-center gap-1 transition-all ${
                              isCopied 
                                ? 'bg-teal-100 text-teal-700 border border-teal-200' 
                                : 'bg-sky-50 text-sky-600 hover:bg-sky-100 border border-sky-200'
                            }`}
                            title="Sao chép mã mời"
                          >
                            {isCopied ? <Check className="w-3 h-3 text-teal-600" /> : <Copy className="w-3 h-3" />}
                            {isCopied ? 'Đã chép!' : 'Sao chép'}
                          </button>
                        </div>
                        <ChevronRight className={`w-4 h-4 ${isSelected ? 'text-sky-500' : 'text-slate-300'}`} />
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            {/* Chi tiết lớp */}
            <div className="lg:col-span-2">
              {selectedClass ? (
                <div className="bg-white border-3 border-sky-100 rounded-3xl p-6 shadow-sm space-y-5">
                  {/* Action error */}
                  {actionError && (
                    <div className="p-4 bg-rose-50 border-2 border-rose-200 rounded-2xl text-xs font-bold text-rose-600 flex items-center gap-3 animate-fade-in">
                      <AlertCircle className="w-5 h-5 shrink-0 text-rose-500" />
                      <span className="flex-1">{actionError}</span>
                      <button type="button" onClick={() => setActionError('')} className="p-1 text-rose-500 hover:bg-rose-100 rounded-xl font-black text-xs transition-all">
                        Đóng
                      </button>
                    </div>
                  )}

                  {/* Header lớp */}
                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pb-4 border-b-2 border-sky-100">
                    <div>
                      <div className="flex items-center gap-2">
                        <h3 className="text-lg font-black text-sky-800">{selectedClass.name}</h3>
                        <button
                          onClick={() => handleOpenEditModal(selectedClass)}
                          className="p-1.5 text-slate-400 hover:text-sky-600 hover:bg-sky-50 rounded-xl transition-all"
                          title="Sửa lớp học"
                        >
                          <Edit className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleDeleteClass(selectedClass.id)}
                          className="p-1.5 text-slate-400 hover:text-rose-500 hover:bg-rose-50 rounded-xl transition-all"
                          title="Xóa lớp học"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                      <p className="text-slate-400 text-xs font-bold mt-0.5">{selectedClass.description || 'Không có mô tả'}</p>

                      {/* Mã mời lớp */}
                      <div className="flex items-center gap-2 mt-2">
                        <span className="text-xs font-black text-amber-800 bg-amber-50 border-2 border-amber-200 px-3 py-1 rounded-xl flex items-center gap-1.5">
                          <Key className="w-4 h-4 text-amber-500" />
                          Mã mời lớp: <strong className="text-sm font-black text-amber-900 uppercase tracking-widest">{selectedClass.inviteCode}</strong>
                        </span>
                        <button
                          onClick={() => handleCopyInviteCode(selectedClass.inviteCode)}
                          className={`px-3 py-1 rounded-xl text-xs font-black flex items-center gap-1.5 transition-all ${
                            copiedCode === selectedClass.inviteCode
                              ? 'bg-teal-500 text-white shadow-sm'
                              : 'bg-sky-50 text-sky-600 hover:bg-sky-100 border border-sky-200'
                          }`}
                        >
                          {copiedCode === selectedClass.inviteCode ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                          {copiedCode === selectedClass.inviteCode ? 'Đã sao chép mã!' : 'Sao chép mã'}
                        </button>
                      </div>
                    </div>

                    <button
                      onClick={() => { setShowAddStudentForm(!showAddStudentForm); setAddStudentError(''); }}
                      className="px-4 py-2.5 rounded-xl bg-sky-50 border-2 border-sky-200 text-sky-600 hover:bg-sky-100 text-xs font-black transition-all flex items-center gap-1.5 self-start sm:self-center"
                    >
                      <UserPlus className="w-4 h-4" />
                      Thêm học sinh
                    </button>
                  </div>

                  {/* Thêm học sinh form */}
                  {showAddStudentForm && (
                    <form onSubmit={handleAddStudent} className="bg-sky-50 p-4 rounded-2xl border-2 border-sky-100 space-y-3">
                      {addStudentError && (
                        <div className="text-xs font-bold text-rose-600 bg-rose-50 border border-rose-200 rounded-xl p-3 flex items-center gap-2">
                          <AlertCircle className="w-4 h-4 shrink-0" />
                          {addStudentError}
                        </div>
                      )}
                      <div className="flex flex-col sm:flex-row gap-3">
                        <div className="flex-1">
                          <label className="block text-xs font-black text-sky-700 mb-1.5">Tên đăng nhập của học sinh</label>
                          <input
                            type="text"
                            placeholder="Ví dụ: student1"
                            value={newStudentUser}
                            onChange={(e) => setNewStudentUser(e.target.value)}
                            className="w-full py-3 px-4 bg-white border-2 border-sky-200 focus:border-sky-400 focus:outline-none rounded-xl text-slate-700 font-bold text-sm transition-all"
                            required
                          />
                        </div>
                        <button
                          type="submit"
                          className="px-5 py-3 rounded-xl bg-gradient-to-r from-sky-400 to-blue-500 text-white font-black text-sm shadow-md active:scale-95 transition-all self-end"
                        >
                          Thêm vào lớp
                        </button>
                      </div>
                    </form>
                  )}

                  {/* Danh sách học sinh */}
                  <div>
                    <h4 className="font-black text-sky-800 text-sm mb-3 flex items-center gap-2">
                      <Users className="w-4 h-4 text-sky-500" />
                      Học sinh trong lớp ({students.length})
                    </h4>

                    {students.length === 0 ? (
                      <div className="py-8 text-center bg-sky-50/50 rounded-2xl border-2 border-dashed border-sky-200 text-slate-400 text-xs font-bold">
                        Chưa có học sinh nào trong lớp. Cô có thể bấm nút "Thêm học sinh" ở trên hoặc gửi mã mời cho bé/phụ huynh nhé!
                      </div>
                    ) : (
                      <div className="space-y-2.5">
                        {students.map((s) => (
                          <div
                            key={s.id}
                            className="p-3.5 rounded-2xl border-2 border-sky-100 bg-white hover:border-sky-200 flex items-center justify-between gap-3 transition-all"
                          >
                            <div className="flex items-center gap-3">
                              <div className="w-10 h-10 rounded-xl bg-sky-100 text-sky-600 flex items-center justify-center font-black text-sm border border-sky-200">
                                {s.fullName ? s.fullName.charAt(0).toUpperCase() : s.username.charAt(0).toUpperCase()}
                              </div>
                              <div>
                                <p className="font-black text-sm text-slate-700">{s.fullName || s.username}</p>
                                <p className="text-slate-400 text-xs font-bold">Username: {s.username}</p>
                              </div>
                            </div>

                            <button
                              onClick={() => handleRemoveStudent(s.id)}
                              className="p-2 text-slate-300 hover:text-rose-500 hover:bg-rose-50 rounded-xl transition-all"
                              title="Xóa khỏi lớp"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              ) : (
                <div className="bg-white border-3 border-sky-100 rounded-3xl p-10 text-center flex flex-col items-center justify-center min-h-[350px]">
                  <div className="w-16 h-16 rounded-2xl bg-sky-50 flex items-center justify-center mb-4">
                    <Folder className="w-8 h-8 text-sky-300" />
                  </div>
                  <p className="text-slate-400 font-bold text-sm">Chọn một lớp học ở danh sách bên trái để xem danh sách học sinh</p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Modal Tạo Lớp Học */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4">
          <form
            onSubmit={handleCreateClass}
            className="w-full max-w-md bg-white rounded-3xl shadow-2xl overflow-hidden border-4 border-sky-100"
          >
            <div className="bg-gradient-to-r from-sky-400 to-blue-500 px-6 py-4 flex items-center justify-between">
              <h3 className="font-black text-white text-base flex items-center gap-2">
                <Plus className="w-5 h-5" /> Tạo lớp học mới
              </h3>
              <button
                type="button"
                onClick={() => setShowCreateModal(false)}
                className="p-1.5 rounded-xl bg-white/20 text-white hover:bg-white/30"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-4">
              {createError && (
                <div className="p-3 bg-rose-50 border-2 border-rose-200 rounded-2xl text-xs font-bold text-rose-600 flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{createError}</span>
                </div>
              )}

              <div>
                <label className="block text-xs font-black text-sky-700 uppercase mb-1.5">Tên lớp học <span className="text-rose-500">*</span></label>
                <input
                  type="text"
                  placeholder="Ví dụ: Lớp 1A - Tiếng Việt"
                  value={newClassName}
                  onChange={(e) => setNewClassName(e.target.value)}
                  className="w-full py-3 px-4 bg-white border-2 border-sky-200 focus:border-sky-400 focus:outline-none rounded-2xl text-slate-700 font-bold text-sm"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-black text-sky-700 uppercase mb-1.5">Mô tả lớp học</label>
                <textarea
                  placeholder="Mô tả ngắn gọn về lớp học..."
                  value={newClassDesc}
                  onChange={(e) => setNewClassDesc(e.target.value)}
                  className="w-full py-3 px-4 bg-white border-2 border-sky-200 focus:border-sky-400 focus:outline-none rounded-2xl text-slate-700 font-bold text-sm h-20 resize-none"
                />
              </div>
            </div>

            <div className="p-5 border-t-2 border-sky-100 flex gap-3">
              <button
                type="button"
                onClick={() => setShowCreateModal(false)}
                className="flex-1 py-3 rounded-2xl bg-sky-50 border border-sky-100 text-slate-500 font-black text-xs"
              >
                Hủy bỏ
              </button>
              <button
                type="submit"
                disabled={createLoading}
                className="flex-1 py-3 rounded-2xl bg-gradient-to-r from-sky-400 to-blue-500 text-white font-black text-xs shadow-md active:scale-95 transition-all flex items-center justify-center gap-2 disabled:opacity-50"
              >
                {createLoading ? 'Đang tạo...' : 'Xác nhận tạo lớp ✨'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Modal Chỉnh Sửa Lớp Học */}
      {showEditModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4">
          <form
            onSubmit={handleUpdateClass}
            className="w-full max-w-md bg-white rounded-3xl shadow-2xl overflow-hidden border-4 border-sky-100"
          >
            <div className="bg-gradient-to-r from-sky-400 to-blue-500 px-6 py-4 flex items-center justify-between">
              <h3 className="font-black text-white text-base flex items-center gap-2">
                <Edit className="w-5 h-5" /> Chỉnh sửa thông tin lớp
              </h3>
              <button
                type="button"
                onClick={() => setShowEditModal(false)}
                className="p-1.5 rounded-xl bg-white/20 text-white hover:bg-white/30"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-4">
              {editError && (
                <div className="p-3 bg-rose-50 border-2 border-rose-200 rounded-2xl text-xs font-bold text-rose-600 flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{editError}</span>
                </div>
              )}

              <div>
                <label className="block text-xs font-black text-sky-700 uppercase mb-1.5">Tên lớp học <span className="text-rose-500">*</span></label>
                <input
                  type="text"
                  placeholder="Ví dụ: Lớp 1A - Tiếng Việt"
                  value={editClassName}
                  onChange={(e) => setEditClassName(e.target.value)}
                  className="w-full py-3 px-4 bg-white border-2 border-sky-200 focus:border-sky-400 focus:outline-none rounded-2xl text-slate-700 font-bold text-sm"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-black text-sky-700 uppercase mb-1.5">Mô tả lớp học</label>
                <textarea
                  placeholder="Mô tả ngắn gọn về lớp học..."
                  value={editClassDesc}
                  onChange={(e) => setEditClassDesc(e.target.value)}
                  className="w-full py-3 px-4 bg-white border-2 border-sky-200 focus:border-sky-400 focus:outline-none rounded-2xl text-slate-700 font-bold text-sm h-20 resize-none"
                />
              </div>
            </div>

            <div className="p-5 border-t-2 border-sky-100 flex gap-3">
              <button
                type="button"
                onClick={() => setShowEditModal(false)}
                className="flex-1 py-3 rounded-2xl bg-sky-50 border border-sky-100 text-slate-500 font-black text-xs"
              >
                Hủy bỏ
              </button>
              <button
                type="submit"
                disabled={editLoading}
                className="flex-1 py-3 rounded-2xl bg-gradient-to-r from-sky-400 to-blue-500 text-white font-black text-xs shadow-md active:scale-95 transition-all flex items-center justify-center gap-2 disabled:opacity-50"
              >
                {editLoading ? 'Đang lưu...' : 'Lưu thay đổi ✨'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Reusable Custom Confirmation Modal */}
      {confirmModal.isOpen && (
        <div className="fixed inset-0 z-[100] bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-sm bg-white rounded-3xl shadow-2xl p-6 border-4 border-rose-50 text-center space-y-5 animate-fade-in">
            <div className={`w-14 h-14 rounded-full mx-auto flex items-center justify-center ${
              confirmModal.type === 'danger' ? 'bg-rose-50 text-rose-500' : 'bg-amber-50 text-amber-500'
            }`}>
              <AlertCircle className="w-8 h-8" />
            </div>
            
            <div className="space-y-2">
              <h3 className="text-base font-black text-slate-800">{confirmModal.title}</h3>
              <p className="text-slate-500 text-xs font-bold leading-relaxed">{confirmModal.message}</p>
            </div>

            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => setConfirmModal(prev => ({ ...prev, isOpen: false }))}
                className="flex-1 py-3 rounded-2xl bg-slate-50 border border-slate-200 text-slate-500 font-black text-xs cursor-pointer hover:bg-slate-100 transition-all"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={confirmModal.onConfirm}
                className={`flex-1 py-3 rounded-2xl text-white font-black text-xs shadow-md cursor-pointer hover:brightness-95 active:scale-95 transition-all ${
                  confirmModal.type === 'danger' ? 'bg-rose-500 shadow-rose-200' : 'bg-amber-500 shadow-amber-200'
                }`}
              >
                {confirmModal.confirmText || 'Xác nhận'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TeacherDashboard;
