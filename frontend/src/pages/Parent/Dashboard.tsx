import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { 
  Users, 
  Award, 
  BookOpen, 
  TrendingUp, 
  AlertCircle, 
  MessageSquare,
  Plus,
  BarChart2,
  Bell,
  Clock,
  CheckCircle2,
  Calendar,
  Star,
  Trophy,
  Medal,
  ThumbsUp,
  X,
  Sparkles,
  Eye,
  HelpCircle,
  PenTool
} from 'lucide-react';

interface ChildLink {
  id: number;
  studentId: number;
  studentName: string;
  studentUsername: string;
  avatarUrl?: string;
  className?: string;
  status: string;
}

interface ProgressData {
  studentId: number;
  studentName: string;
  totalPractices: number;
  averagePracticeScore: number;
  totalSubmissions: number;
  averageSubmissionScore: number;
  practiceHistory: { letter: string; score: number; date: string }[];
  submissionHistory: { assignmentTitle: string; score: number; date: string }[];
}

interface AssignmentItem {
  id: number;
  title: string;
  description: string;
  contentType: string;
  contentData: string;
  dueDate: string;
  classId: number;
  className: string;
  isSubmitted: boolean;
  score: number | null;
  feedback?: string | null;
  assignedBy?: string;
}

interface ClassmateItem {
  studentId: number;
  fullName: string;
  username: string;
  totalScore: number;
  totalAssignmentsCompleted: number;
}

const ParentDashboard: React.FC = () => {
  const { user } = useAuth();
  const [children, setChildren] = useState<ChildLink[]>([]);
  const [selectedChild, setSelectedChild] = useState<ChildLink | null>(null);
  const [progress, setProgress] = useState<ProgressData | null>(null);
  const [childAssignments, setChildAssignments] = useState<AssignmentItem[]>([]);
  const [classmates, setClassmates] = useState<ClassmateItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'OVERVIEW' | 'LEADERBOARD'>('OVERVIEW');

  // Cửa sổ nhỏ thông báo (Modal Alert)
  const [modalAlert, setModalAlert] = useState<{
    isOpen: boolean;
    title: string;
    message: string;
    type: 'success' | 'info' | 'error';
    icon?: string;
  }>({
    isOpen: false,
    title: '',
    message: '',
    type: 'info'
  });

  // Modal xem nhận xét chi tiết
  const [selectedAssignmentFeedback, setSelectedAssignmentFeedback] = useState<AssignmentItem | null>(null);

  // Link con mới trực tiếp trên Dashboard
  const [linkCode, setLinkCode] = useState('');
  const [showLinkForm, setShowLinkForm] = useState(false);

  const fetchChildren = async () => {
    try {
      const res = await api.get('/parent/students');
      setChildren(res.data);
      if (res.data.length > 0) {
        handleSelectChild(res.data[0]);
      } else {
        setLoading(false);
      }
    } catch (err) {
      console.error(err);
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchChildren();
  }, []);

  const handleSelectChild = async (child: ChildLink) => {
    setSelectedChild(child);
    setLoading(true);
    try {
      const [progRes, asmRes, classmatesRes] = await Promise.all([
        api.get(`/parent/students/${child.studentId}/progress`),
        api.get(`/parent/students/${child.studentId}/assignments`),
        api.get(`/parent/students/${child.studentId}/classmates`).catch(() => ({ data: [] }))
      ]);
      setProgress(progRes.data);
      setChildAssignments(asmRes.data);
      setClassmates(classmatesRes.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleLinkChild = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!linkCode) return;

    try {
      const res = await api.post(`/parent/link?linkCode=${encodeURIComponent(linkCode.trim())}`);
      setModalAlert({
        isOpen: true,
        title: 'Liên Kết Thành Công! 🎉',
        message: res.data.message || 'Đã liên kết thành công tài khoản học sinh của con!',
        type: 'success',
        icon: '👨‍👩‍👧'
      });
      setLinkCode('');
      setShowLinkForm(false);
      fetchChildren();
    } catch (err: any) {
      setModalAlert({
        isOpen: true,
        title: 'Chưa Thể Liên Kết',
        message: err.response?.data?.message || 'Liên kết thất bại. Vui lòng kiểm tra lại mã liên kết hoặc tên đăng nhập của bé.',
        type: 'error',
        icon: '⚠️'
      });
    }
  };

  const handleRemindChild = async (asmId: number) => {
    if (!selectedChild) return;
    try {
      const res = await api.post(`/parent/students/${selectedChild.studentId}/assignments/${asmId}/remind`);
      setModalAlert({
        isOpen: true,
        title: 'Gửi Nhắc Nhở Thành Công! ⏰',
        message: res.data.message || `Đã gửi lời nhắc làm bài tập tới tài khoản của bé ${selectedChild.studentName}!`,
        type: 'success',
        icon: '🔔'
      });
    } catch (err: any) {
      setModalAlert({
        isOpen: true,
        title: 'Chưa Gửi Được Nhắc Nhở',
        message: err.response?.data?.message || 'Có lỗi xảy ra khi gửi thông báo nhắc nhở.',
        type: 'error',
        icon: '⚠️'
      });
    }
  };

  const getEvaluationTag = (score: number | null) => {
    if (score === null) return { label: 'Chưa chấm', bg: 'bg-slate-100', text: 'text-slate-600', icon: '⏳' };
    if (score >= 8.5) return { label: 'Xuất sắc', bg: 'bg-emerald-100', text: 'text-emerald-800', icon: '🌟' };
    if (score >= 7.0) return { label: 'Tốt', bg: 'bg-sky-100', text: 'text-sky-800', icon: '👍' };
    if (score >= 5.0) return { label: 'Đạt', bg: 'bg-amber-100', text: 'text-amber-800', icon: '👌' };
    return { label: 'Cần cố gắng', bg: 'bg-rose-100', text: 'text-rose-800', icon: '💪' };
  };

  return (
    <div className="space-y-6 w-full overflow-hidden" style={{ fontFamily: "'Nunito', sans-serif" }}>
      {/* Upper header controls */}
      <div className="flex flex-col gap-4 bg-gradient-to-r from-sky-400 to-blue-500 rounded-3xl p-5 shadow-lg shadow-sky-200/50 w-full overflow-hidden">
        <div className="flex items-start sm:items-center gap-3">
          <div className="w-10 h-10 sm:w-12 sm:h-12 rounded-2xl bg-white/20 backdrop-blur flex items-center justify-center border border-white/40 text-white font-black shrink-0">
            👨‍👩‍👧
          </div>
          <div className="min-w-0 flex-1">
            <h1 className="text-lg sm:text-xl font-black text-white truncate">Góc Theo Dõi Phụ Huynh</h1>
            <div className="flex flex-wrap items-center gap-2 mt-1">
              <span className="text-xs font-bold text-sky-100 shrink-0">Chọn con:</span>
              {children.length > 0 ? (
                <select
                  value={selectedChild?.studentId || ''}
                  onChange={(e) => {
                    const child = children.find(c => c.studentId === Number(e.target.value));
                    if (child) handleSelectChild(child);
                  }}
                  className="bg-white border-2 border-sky-200 rounded-xl px-2 py-1 text-xs text-sky-800 font-black focus:outline-none focus:border-sky-400 shadow-sm flex-1 min-w-0 max-w-[150px] truncate cursor-pointer"
                >
                  {children.map((c) => (
                    <option key={c.studentId} value={c.studentId}>Bé {c.studentName}</option>
                  ))}
                </select>
              ) : (
                <span className="text-xs text-sky-100 font-bold">Chưa liên kết</span>
              )}
            </div>
          </div>
        </div>

        <button
          onClick={() => setShowLinkForm(!showLinkForm)}
          className="w-full sm:w-auto px-4 py-2.5 rounded-xl bg-white text-sky-600 font-black text-[11px] sm:text-xs shadow-md hover:shadow-lg active:scale-95 transition-all flex items-center justify-center gap-1.5"
        >
          <Plus className="w-4 h-4 shrink-0" />
          <span className="truncate">Liên kết con mới</span>
        </button>
      </div>

      {/* Link Child Input Form */}
      {showLinkForm && (
        <form onSubmit={handleLinkChild} className="max-w-md bg-white border-2 border-sky-100 p-6 rounded-3xl shadow-lg space-y-4">
          <h3 className="font-black text-sm text-sky-800">Liên kết tài khoản của con</h3>
          <p className="text-xs text-slate-500 font-bold">Mở hồ sơ trong app của con, chọn <strong>Liên kết phụ huynh</strong> và nhập mã được tạo tại đó. Mã có hiệu lực 30 phút.</p>
          <div className="flex flex-col sm:flex-row gap-3">
            <input
              type="text"
              aria-label="Mã liên kết từ hồ sơ của con"
              placeholder="Nhập mã liên kết của con"
              minLength={22}
              maxLength={22}
              autoCapitalize="none"
              autoCorrect="off"
              value={linkCode}
              onChange={(e) => setLinkCode(e.target.value)}
              className="flex-1 py-3 px-4 bg-white border-2 border-sky-200 focus:border-sky-400 focus:outline-none rounded-2xl text-slate-700 font-bold text-xs"
              required
            />
            <button
              type="submit"
              className="px-6 py-3 rounded-2xl bg-gradient-to-r from-sky-400 to-blue-500 hover:from-sky-350 hover:to-blue-450 text-white font-black text-xs transition-all shadow-md active:scale-95 shrink-0"
            >
              Xác nhận liên kết
            </button>
          </div>
        </form>
      )}

      {/* Navigation Tabs */}
      {selectedChild && (
        <div className="flex border-b-2 border-slate-100 gap-2 pb-1">
          <button
            onClick={() => setActiveTab('OVERVIEW')}
            className={`px-5 py-2.5 rounded-2xl font-black text-xs transition-all flex items-center gap-2 ${
              activeTab === 'OVERVIEW'
                ? 'bg-sky-500 text-white shadow-md shadow-sky-200'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            <BarChart2 className="w-4 h-4" />
            📊 Tổng Quan & Bài Tập
          </button>

          <button
            onClick={() => setActiveTab('LEADERBOARD')}
            className={`px-5 py-2.5 rounded-2xl font-black text-xs transition-all flex items-center gap-2 ${
              activeTab === 'LEADERBOARD'
                ? 'bg-amber-500 text-white shadow-md shadow-amber-200'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            <Trophy className="w-4 h-4" />
            🏆 Bảng Xếp Hạng Lớp ({classmates.length})
          </button>
        </div>
      )}

      {/* Loading indicator */}
      {loading ? (
        <div className="flex items-center justify-center min-h-[40vh]">
          <div className="w-12 h-12 border-4 border-sky-200 border-t-sky-500 rounded-full animate-spin"></div>
        </div>
      ) : selectedChild && progress ? (
        <div className="space-y-6">
          {/* TAB 1: OVERVIEW & ASSIGNMENTS */}
          {activeTab === 'OVERVIEW' && (
            <>
              {/* Stats Dashboard Grid */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4 w-full">
                <div className="bg-gradient-to-br from-sky-50 to-blue-50 border-2 border-sky-200 p-3 sm:p-4 rounded-[24px] shadow-sm flex flex-col items-center text-center">
                  <div className="w-8 h-8 sm:w-10 sm:h-10 bg-sky-200 text-sky-600 rounded-full flex items-center justify-center mb-1.5 text-lg sm:text-xl shrink-0">✍️</div>
                  <span className="text-[9px] sm:text-[10px] font-black text-sky-700 tracking-wide line-clamp-1 w-full">Luyện tô</span>
                  <p className="text-lg sm:text-2xl font-black mt-0.5 text-sky-900">{progress.totalPractices}</p>
                </div>

                <div className="bg-gradient-to-br from-teal-50 to-emerald-50 border-2 border-teal-200 p-3 sm:p-4 rounded-[24px] shadow-sm flex flex-col items-center text-center">
                  <div className="w-8 h-8 sm:w-10 sm:h-10 bg-teal-200 text-teal-600 rounded-full flex items-center justify-center mb-1.5 text-lg sm:text-xl shrink-0">🎯</div>
                  <span className="text-[9px] sm:text-[10px] font-black text-teal-700 tracking-wide line-clamp-1 w-full">Độ chính xác</span>
                  <p className="text-lg sm:text-2xl font-black mt-0.5 text-teal-900">{progress.averagePracticeScore}%</p>
                </div>

                <div className="bg-gradient-to-br from-indigo-50 to-violet-50 border-2 border-indigo-200 p-3 sm:p-4 rounded-[24px] shadow-sm flex flex-col items-center text-center">
                  <div className="w-8 h-8 sm:w-10 sm:h-10 bg-indigo-200 text-indigo-600 rounded-full flex items-center justify-center mb-1.5 text-lg sm:text-xl shrink-0">📚</div>
                  <span className="text-[9px] sm:text-[10px] font-black text-indigo-700 tracking-wide line-clamp-1 w-full">Bài tập</span>
                  <p className="text-lg sm:text-2xl font-black mt-0.5 text-indigo-900">{progress.totalSubmissions}</p>
                </div>

                <div className="bg-gradient-to-br from-amber-50 to-orange-50 border-2 border-amber-200 p-3 sm:p-4 rounded-[24px] shadow-sm flex flex-col items-center text-center">
                  <div className="w-8 h-8 sm:w-10 sm:h-10 bg-amber-200 text-amber-600 rounded-full flex items-center justify-center mb-1.5 text-lg sm:text-xl shrink-0">⭐</div>
                  <span className="text-[9px] sm:text-[10px] font-black text-amber-700 tracking-wide line-clamp-1 w-full">Điểm TB</span>
                  <p className="text-lg sm:text-2xl font-black mt-0.5 text-amber-900">{progress.averageSubmissionScore}/10</p>
                </div>
              </div>

              {/* NHIỆM VỤ CHƯA LÀM (BÀI TẬP MỚI) */}
              <div className="bg-amber-50/50 border-2 border-amber-200 p-4 sm:p-6 rounded-[32px] space-y-4">
                <h3 className="text-base font-black text-amber-800 flex items-center gap-2">
                  <AlertCircle className="w-5 h-5 text-amber-500" />
                  Nhiệm vụ bé chưa làm ({childAssignments.filter(a => !a.isSubmitted).length})
                </h3>
                
                {childAssignments.filter(a => !a.isSubmitted).length === 0 ? (
                  <div className="py-6 text-center bg-white rounded-2xl border-2 border-dashed border-amber-200 text-slate-400 text-xs font-bold shadow-sm">
                    Tuyệt vời! Bé không còn nợ bài tập nào. 🎉
                  </div>
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {childAssignments.filter(a => !a.isSubmitted).map((asm) => (
                      <div key={asm.id} className="p-4 bg-white border-2 border-amber-200 rounded-2xl flex flex-col shadow-sm">
                        <div className="flex items-start justify-between gap-2 mb-2">
                          <h4 className="font-black text-sm text-slate-800 line-clamp-2">{asm.title}</h4>
                          <span className="text-[10px] uppercase font-black px-2 py-0.5 rounded-md bg-slate-100 text-slate-500 shrink-0">
                            {asm.className}
                          </span>
                        </div>
                        <p className="text-slate-500 text-xs font-medium line-clamp-2 mb-4 flex-1">{asm.description}</p>
                        
                        <div className="flex flex-col sm:flex-row items-center gap-3 mt-auto">
                          <div className="text-[11px] font-black text-rose-500 flex items-center gap-1 bg-rose-50 px-2 py-1 rounded-lg w-full sm:w-auto justify-center">
                            <Clock className="w-3.5 h-3.5" />
                            Hạn: {asm.dueDate ? new Date(asm.dueDate).toLocaleDateString('vi-VN') : 'Không có'}
                          </div>
                          <button
                            onClick={() => handleRemindChild(asm.id)}
                            className="w-full sm:w-auto px-4 py-2.5 rounded-xl bg-amber-500 hover:bg-amber-600 text-white text-xs font-black shadow-md active:scale-95 transition-all flex items-center justify-center gap-1.5"
                          >
                            <Bell className="w-4 h-4 animate-bounce-slow" />
                            Bấm nhắc bé!
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* LỊCH SỬ BÀI TẬP ĐÃ NỘP (CÓ NHẬN XÉT CỦA GIÁO VIÊN) */}
              <div className="bg-emerald-50/50 border-2 border-emerald-200 p-4 sm:p-6 rounded-[32px] space-y-4">
                <h3 className="text-base font-black text-emerald-800 flex items-center justify-between">
                  <span className="flex items-center gap-2">
                    <CheckCircle2 className="w-5 h-5 text-emerald-500" />
                    Thành tích đã đạt & Nhận xét ({childAssignments.filter(a => a.isSubmitted).length})
                  </span>
                </h3>
                
                {childAssignments.filter(a => a.isSubmitted).length === 0 ? (
                  <div className="py-6 text-center bg-white rounded-2xl border-2 border-dashed border-emerald-200 text-slate-400 text-xs font-bold shadow-sm">
                    Bé chưa nộp bài tập nào.
                  </div>
                ) : (
                  <div className="space-y-3">
                    {childAssignments.filter(a => a.isSubmitted).map((asm) => {
                      const evalTag = getEvaluationTag(asm.score);
                      return (
                        <div key={asm.id} className="p-4 bg-white border-2 border-emerald-100 rounded-2xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-sm hover:shadow-md transition-shadow">
                          <div className="flex items-start gap-3 min-w-0 flex-1">
                            <div className="w-10 h-10 rounded-2xl bg-emerald-100 text-emerald-600 flex items-center justify-center shrink-0 text-xl font-bold">
                              🏆
                            </div>
                            <div className="min-w-0 space-y-1 flex-1 w-full overflow-hidden">
                              <div className="flex items-center gap-2 flex-wrap">
                                <h4 className="font-black text-sm text-slate-800 truncate" title={asm.title}>{asm.title}</h4>
                                <span className={`text-[10px] font-black px-2 py-0.5 rounded-md ${evalTag.bg} ${evalTag.text} flex items-center gap-1 shrink-0`}>
                                  <span>{evalTag.icon}</span>
                                  {evalTag.label}
                                </span>
                              </div>

                              <div className="text-[10px] sm:text-xs font-bold text-slate-400 flex items-center gap-2">
                                <span>{asm.className}</span>
                                <span className="text-slate-300">•</span>
                                <span className="text-emerald-600 font-bold">Đã hoàn thành</span>
                              </div>

                              {/* Hiển thị chỉ báo có nhận xét ngắn gọn */}
                              {(asm.feedback || asm.score) && (
                                <div className="text-[10px] text-emerald-700 font-bold bg-emerald-50 px-2 py-1 rounded-md border border-emerald-100 mt-1 inline-flex items-center gap-1 w-max">
                                  💬 Đã có điểm/nhận xét
                                </div>
                              )}
                            </div>
                          </div>

                          <div className="flex items-center sm:flex-col justify-between sm:justify-center w-full sm:w-auto shrink-0 gap-3 border-t sm:border-t-0 border-slate-100 pt-2 sm:pt-0">
                            {asm.score !== null && (
                              <div className="text-lg sm:text-xl font-black text-emerald-600 bg-emerald-50 px-3 py-1 rounded-xl border border-emerald-100 flex items-center gap-1">
                                <span>{asm.score}</span>
                                <span className="text-xs text-amber-500">⭐</span>
                              </div>
                            )}
                            <button
                              onClick={() => setSelectedAssignmentFeedback(asm)}
                              className="px-3 py-1.5 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white font-black text-xs shadow-sm flex items-center gap-1.5 transition-all"
                            >
                              <Eye className="w-3.5 h-3.5" />
                              Xem chi tiết
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            </>
          )}

          {/* TAB 2: CLASSMATES LEADERBOARD */}
          {activeTab === 'LEADERBOARD' && (
            <div className="bg-white border-2 border-slate-100 rounded-[32px] p-4 sm:p-6 shadow-sm space-y-6">
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 pb-4 border-b-2 border-slate-100">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-2xl bg-amber-100 text-amber-600 flex items-center justify-center text-2xl font-black shrink-0">
                    🏆
                  </div>
                  <div>
                    <h3 className="text-base sm:text-lg font-black text-slate-800">Bảng Xếp Hạng Lớp Học</h3>
                    <p className="text-xs font-bold text-slate-400">Thi đua điểm số bài tập cùng các bạn trong lớp</p>
                  </div>
                </div>

                {selectedChild && (
                  <div className="px-3 py-1.5 bg-sky-50 border border-sky-200 rounded-xl text-sky-800 text-xs font-black flex items-center gap-1.5">
                    <Sparkles className="w-4 h-4 text-sky-500" />
                    <span>Đang xem của bé: <strong>{selectedChild.studentName}</strong></span>
                  </div>
                )}
              </div>

              {classmates.length === 0 ? (
                <div className="py-12 text-center text-slate-400 font-bold text-xs bg-slate-50 rounded-2xl border-2 border-dashed border-slate-200 space-y-2">
                  <Users className="w-8 h-8 text-slate-300 mx-auto" />
                  <p>Chưa có thông tin danh sách các bạn cùng lớp.</p>
                </div>
              ) : (
                <div className="space-y-3">
                  {classmates.map((cm, idx) => {
                    const rank = idx + 1;
                    const isCurrentChild = selectedChild && cm.studentId === selectedChild.studentId;

                    let rankBadge = (
                      <span className="w-8 h-8 rounded-full bg-slate-100 text-slate-600 font-black text-xs flex items-center justify-center shrink-0">
                        #{rank}
                      </span>
                    );

                    if (rank === 1) {
                      rankBadge = (
                        <span className="w-8 h-8 rounded-full bg-gradient-to-tr from-amber-400 to-yellow-300 text-amber-950 font-black text-sm flex items-center justify-center shadow-md shrink-0">
                          🥇
                        </span>
                      );
                    } else if (rank === 2) {
                      rankBadge = (
                        <span className="w-8 h-8 rounded-full bg-gradient-to-tr from-slate-300 to-slate-200 text-slate-800 font-black text-sm flex items-center justify-center shadow-md shrink-0">
                          🥈
                        </span>
                      );
                    } else if (rank === 3) {
                      rankBadge = (
                        <span className="w-8 h-8 rounded-full bg-gradient-to-tr from-amber-700 to-amber-600 text-white font-black text-sm flex items-center justify-center shadow-md shrink-0">
                          🥉
                        </span>
                      );
                    }

                    return (
                      <div
                        key={cm.studentId}
                        className={`p-3.5 sm:p-4 rounded-2xl border-2 transition-all flex items-center justify-between gap-3 ${
                          isCurrentChild
                            ? 'bg-sky-50/90 border-sky-400 shadow-md ring-2 ring-sky-200'
                            : rank <= 3
                            ? 'bg-amber-50/30 border-amber-200 hover:border-amber-300'
                            : 'bg-white border-slate-100 hover:border-slate-200'
                        }`}
                      >
                        <div className="flex items-center gap-3 min-w-0">
                          {rankBadge}
                          <div className="w-9 h-9 rounded-full bg-gradient-to-tr from-sky-400 to-blue-500 text-white font-black flex items-center justify-center text-xs shrink-0 shadow-sm">
                            {(cm.fullName || cm.username || '?').charAt(0).toUpperCase()}
                          </div>
                          <div className="min-w-0">
                            <div className="flex items-center gap-2">
                              <h4 className="font-black text-sm text-slate-800 truncate">
                                {cm.fullName || cm.username}
                              </h4>
                              {isCurrentChild && (
                                <span className="px-2 py-0.5 rounded-full bg-sky-500 text-white text-[9px] font-black shrink-0 shadow-sm animate-pulse">
                                  Bé nhà bạn
                                </span>
                              )}
                            </div>
                            <p className="text-[10px] font-bold text-slate-400 truncate">
                              @{cm.username} • {cm.totalAssignmentsCompleted} bài tập hoàn thành
                            </p>
                          </div>
                        </div>

                        <div className="shrink-0 text-right">
                          <div className="text-base sm:text-lg font-black text-amber-600 bg-amber-50 px-3 py-1 rounded-xl border border-amber-200 inline-flex items-center gap-1">
                            <span>{cm.totalScore || 0}</span>
                            <span className="text-xs text-amber-500">⭐</span>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}
        </div>
      ) : (
        /* Empty screen if no linked child */
        <div className="bg-white border-3 border-sky-100 rounded-3xl p-12 text-center text-slate-400 flex flex-col items-center justify-center min-h-[350px]">
          <Users className="w-12 h-12 text-sky-200 mb-3" />
          <h3 className="font-black text-lg text-slate-700">Chưa liên kết tài khoản con</h3>
          <p className="text-slate-400 text-xs font-bold mt-2 max-w-sm">
            Phụ huynh hãy nhấn nút <strong>"Liên kết con mới"</strong> phía trên và nhập Tên đăng nhập của bé (Ví dụ: student1) để kết nối và xem kết quả học tập của con nhé!
          </p>
        </div>
      )}

      {/* Modal xem nhận xét & chi tiết bài làm của con */}
      {selectedAssignmentFeedback && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-3 sm:p-4 animate-in fade-in duration-200">
          <div className="bg-white border-2 border-slate-100 rounded-3xl max-w-lg w-full shadow-2xl animate-in zoom-in-95 duration-200 relative max-h-[85vh] sm:max-h-[90vh] flex flex-col overflow-hidden">
            
            {/* Header cố định */}
            <div className="p-4 sm:p-5 border-b border-slate-100 flex items-center justify-between shrink-0 bg-white">
              <div className="flex items-center gap-3 min-w-0 pr-2">
                <div className="w-10 h-10 rounded-2xl bg-emerald-100 text-emerald-600 flex items-center justify-center text-xl font-bold shrink-0">
                  {selectedAssignmentFeedback.contentType === 'QUIZ' ? '🐾' : '✏️'}
                </div>
                <div className="min-w-0 flex-1">
                  <h3 className="text-sm sm:text-base font-black text-slate-800 break-words line-clamp-1" title={selectedAssignmentFeedback.title}>
                    {selectedAssignmentFeedback.title}
                  </h3>
                  <span className="text-[11px] font-bold text-slate-400">{selectedAssignmentFeedback.className}</span>
                </div>
              </div>
              <button
                onClick={() => setSelectedAssignmentFeedback(null)}
                className="w-8 h-8 bg-slate-100 hover:bg-slate-200 text-slate-600 rounded-full flex items-center justify-center transition-colors shrink-0 cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Thân Modal cuộn mượt mà */}
            <div className="p-4 sm:p-5 overflow-y-auto overscroll-contain flex-1 space-y-4 touch-pan-y">
              {/* Kết quả chấm bài */}
              <div className="bg-gradient-to-r from-emerald-50 to-teal-50 border-2 border-emerald-200 p-3.5 sm:p-4 rounded-2xl flex items-center justify-between">
                <div>
                  <span className="text-[10px] font-black text-emerald-700 uppercase tracking-wider block">Kết quả chấm bài</span>
                  <span className="text-xl sm:text-2xl font-black text-emerald-900 flex items-center gap-1 mt-0.5">
                    {selectedAssignmentFeedback.score !== null ? selectedAssignmentFeedback.score : '--'}
                    <span className="text-amber-500 text-xs sm:text-sm">⭐ / 10</span>
                  </span>
                </div>
                
                {(() => {
                  const evalTag = getEvaluationTag(selectedAssignmentFeedback.score);
                  return (
                    <div className={`px-3 sm:px-4 py-1.5 sm:py-2 rounded-xl ${evalTag.bg} ${evalTag.text} font-black text-xs flex items-center gap-1.5 shadow-sm`}>
                      <span>{evalTag.icon}</span>
                      <span>{evalTag.label}</span>
                    </div>
                  );
                })()}
              </div>

              {/* Nhận xét chi tiết từ giáo viên */}
              <div className="space-y-1.5">
                <label className="text-xs font-black text-slate-700 flex items-center gap-1.5">
                  <MessageSquare className="w-4 h-4 text-emerald-600" />
                  Nhận xét chi tiết từ giáo viên:
                </label>
                <div className="p-3.5 bg-slate-50 border-2 border-slate-100 rounded-2xl text-xs text-slate-700 font-medium leading-relaxed italic">
                  "{selectedAssignmentFeedback.feedback 
                    ? selectedAssignmentFeedback.feedback 
                    : selectedAssignmentFeedback.score && selectedAssignmentFeedback.score >= 8 
                    ? 'Bài làm xuất sắc! Con làm bài rất chuẩn xác. Tiếp tục phát huy nhé! 🎉' 
                    : selectedAssignmentFeedback.score && selectedAssignmentFeedback.score >= 5
                    ? 'Bài làm khá tốt! Con chú ý rèn luyện thêm để đạt điểm tuyệt đối nhé. 👍'
                    : 'Con cần cố gắng nhiều hơn. Bố mẹ hãy đồng hành và động viên con nhé! 💪'}"
                </div>
              </div>

              {selectedAssignmentFeedback.description && (
                <div className="space-y-1">
                  <label className="text-[11px] font-black text-slate-400 uppercase">Ghi chú bài tập</label>
                  <p className="text-xs text-slate-600 font-medium bg-slate-50/60 p-2.5 rounded-xl border border-slate-100 break-words whitespace-pre-wrap">
                    {selectedAssignmentFeedback.description}
                  </p>
                </div>
              )}

              {/* Chi tiết bài làm của con */}
              {selectedAssignmentFeedback.contentType === 'QUIZ' ? (
                <div className="space-y-2">
                  <label className="text-xs font-black text-purple-700 uppercase flex items-center gap-1.5">
                    <HelpCircle className="w-4 h-4 text-purple-500" />
                    Chi tiết câu hỏi & Đáp án con đã chọn:
                  </label>
                  <div className="bg-purple-50/60 p-3 sm:p-3.5 rounded-2xl border-2 border-purple-200 space-y-2.5">
                    {(() => {
                      let questions: any[] = [];
                      let studentAnswers: string[] = [];
                      try {
                        const cData = selectedAssignmentFeedback.contentData;
                        questions = typeof cData === 'string' ? JSON.parse(cData) : cData;
                      } catch (e) {}

                      try {
                        const sData = (selectedAssignmentFeedback as any).submissionData;
                        if (typeof sData === 'string') {
                          const parsed = JSON.parse(sData);
                          studentAnswers = Array.isArray(parsed) ? parsed : (parsed.answers || []);
                        } else if (Array.isArray(sData)) {
                          studentAnswers = sData;
                        }
                      } catch (e) {}

                      if (!Array.isArray(questions) || questions.length === 0) {
                        return <p className="text-xs text-slate-500 font-bold text-center py-2">Không có dữ liệu chi tiết câu hỏi.</p>;
                      }

                      return questions.map((q, idx) => {
                        const childAns = studentAnswers[idx] !== undefined ? String(studentAnswers[idx]) : 'Chưa làm';
                        const rightAns = String(q.answer || '');
                        const isCorrect = childAns.trim().toUpperCase() === rightAns.trim().toUpperCase();

                        return (
                          <div key={idx} className={`p-3 rounded-xl border-2 bg-white ${isCorrect ? 'border-emerald-200' : 'border-rose-200'} space-y-2 shadow-sm`}>
                            <div className="flex items-center justify-between gap-2">
                              <span className="font-black text-xs text-slate-800 flex items-center gap-1.5">
                                <span className="text-base">{q.animalEmoji || '🐾'}</span>
                                <span>Câu {idx + 1}: {q.animalName ? `Chữ cái cho "${q.animalName}"` : q.question || 'Câu hỏi'}</span>
                              </span>
                              <span className={`text-[10px] font-black px-2 py-0.5 rounded-full shrink-0 ${isCorrect ? 'bg-emerald-100 text-emerald-700 border border-emerald-300' : 'bg-rose-100 text-rose-700 border border-rose-300'}`}>
                                {isCorrect ? '✓ Bé chọn đúng' : '✗ Bé chọn sai'}
                              </span>
                            </div>

                            <div className="flex flex-wrap items-center gap-1.5 pt-0.5">
                              {q.options?.map((opt: string, oIdx: number) => {
                                const isChildPicked = childAns.trim().toUpperCase() === opt.trim().toUpperCase();
                                const isSystemAnswer = rightAns.trim().toUpperCase() === opt.trim().toUpperCase();

                                let style = "bg-slate-50 text-slate-600 border-slate-200";
                                let badge = "";

                                if (isChildPicked && isSystemAnswer) {
                                  style = "bg-emerald-500 text-white border-emerald-600 shadow-sm";
                                  badge = " (Bé chọn ✓)";
                                } else if (isChildPicked && !isSystemAnswer) {
                                  style = "bg-rose-500 text-white border-rose-600 shadow-sm";
                                  badge = " (Bé chọn ✗)";
                                } else if (isSystemAnswer) {
                                  style = "bg-emerald-100 text-emerald-800 border-emerald-300 font-bold";
                                  badge = " (Đáp án đúng)";
                                }

                                return (
                                  <span key={oIdx} className={`px-2.5 py-1 rounded-lg text-xs font-black border ${style}`}>
                                    {opt}{badge}
                                  </span>
                                );
                              })}
                            </div>
                          </div>
                        );
                      });
                    })()}
                  </div>
                </div>
              ) : (
                /* Phần luyện viết chữ */
                <div className="space-y-2">
                  <label className="text-xs font-black text-pink-700 uppercase flex items-center gap-1.5">
                    <PenTool className="w-4 h-4 text-pink-500" />
                    Nét chữ con đã tập viết:
                  </label>
                  <div className="bg-pink-50/50 p-3 rounded-2xl border-2 border-pink-200 text-xs text-slate-800 shadow-inner">
                     {(() => {
                       const subData = (selectedAssignmentFeedback as any).submissionData;
                       if (!subData) return <p className="text-slate-400 text-xs font-bold text-center py-2">Chưa có bài nộp hình ảnh.</p>;
                       
                       if (subData.startsWith('data:image') || subData.startsWith('http')) {
                         return (
                           <div className="flex justify-center bg-white p-2 rounded-xl border border-pink-200 shadow-sm">
                             <img src={subData} alt="Bài làm của con" className="max-w-full max-h-48 object-contain rounded-lg" />
                           </div>
                         );
                       }
                       try {
                         const parsed = JSON.parse(subData);
                         if (Array.isArray(parsed) && parsed.length > 0 && parsed[0].image) {
                           return (
                             <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                               {parsed.map((item, idx) => (
                                 <div key={idx} className="flex flex-col items-center gap-1 bg-white p-2 rounded-xl border border-pink-200 shadow-sm">
                                   <span className="text-[10px] font-black text-pink-600 bg-pink-100 px-2 py-0.5 rounded-md">Chữ "{item.letter}"</span>
                                   <img src={item.image} alt={`Chữ ${item.letter}`} className="w-full h-auto max-h-32 object-contain" />
                                 </div>
                               ))}
                             </div>
                           );
                         }
                         return <div className="whitespace-pre-wrap w-full text-center text-xs font-bold text-slate-600">{subData}</div>;
                       } catch {
                         return <div className="whitespace-pre-wrap w-full text-center text-xs font-bold text-slate-600">{subData}</div>;
                       }
                     })()}
                  </div>
                </div>
              )}
            </div>

            {/* Footer cố định */}
            <div className="p-4 border-t border-slate-100 bg-slate-50/50 shrink-0">
              <button
                onClick={() => setSelectedAssignmentFeedback(null)}
                className="w-full py-3 rounded-2xl bg-slate-800 hover:bg-slate-900 text-white font-black text-xs transition-all shadow-md active:scale-95 cursor-pointer"
              >
                Đóng cửa sổ
              </button>
            </div>

          </div>
        </div>
      )}

      {/* Cửa sổ nhỏ thông báo (Modal Alert) */}
      {modalAlert.isOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4 animate-fade-in">
          <div className="w-full max-w-sm bg-white rounded-3xl shadow-2xl p-6 border-4 border-amber-100 text-center space-y-4 animate-bounce-in">
            <div className="w-16 h-16 rounded-full bg-gradient-to-tr from-amber-100 to-orange-100 text-amber-600 flex items-center justify-center text-3xl mx-auto shadow-inner border-2 border-amber-200">
              {modalAlert.icon || '⏰'}
            </div>
            
            <div className="space-y-1.5">
              <h3 className="text-base font-black text-slate-800">{modalAlert.title}</h3>
              <p className="text-xs text-slate-500 font-bold leading-relaxed">{modalAlert.message}</p>
            </div>

            <button
              onClick={() => setModalAlert(prev => ({ ...prev, isOpen: false }))}
              className="w-full py-3 rounded-2xl bg-gradient-to-r from-amber-400 to-orange-500 hover:from-amber-350 hover:to-orange-450 text-white font-black text-xs transition-all shadow-md active:scale-95 cursor-pointer"
            >
              Đã hiểu ✨
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default ParentDashboard;

