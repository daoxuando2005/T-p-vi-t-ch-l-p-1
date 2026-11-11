import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import api from '../../services/api';
import { 
  Shield, Users, BookOpen, Trash2, Plus, Check, AlertCircle,
  Edit3, X, Upload, Key, CheckSquare, Square, UserPlus, Edit2,
  Eye, EyeOff, Mic, Volume2
} from 'lucide-react';
import { sortVietnameseAlphabet } from '../../utils/alphabetSort';
import { getExampleEmoji } from '../../utils/getExampleEmoji';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend
} from 'recharts';

interface UserProfile {
  id: number;
  username: string;
  email: string;
  fullName: string;
  phoneNumber: string;
  roles: string[];
  enabled: boolean;
}

interface AlphabetItem {
  id: number;
  letter: string;
  pronunciationGuide: string;
  exampleWord: string;
  audioUrl?: string;
  imageUrl?: string;
  isHidden?: boolean;
  exampleAudioUrl?: string;
}

const VIETNAMESE_ALPHABET = [
  'a', 'ă', 'â', 'b', 'c', 'd', 'đ', 'e', 'ê', 'g', 'h', 'i', 'k', 'l', 'm',
  'n', 'o', 'ô', 'ơ', 'p', 'q', 'r', 's', 't', 'u', 'ư', 'v', 'x', 'y'
];

const pronunciationMapAdmin: Record<string, string> = {
  'a': 'a', 'ă': 'á', 'â': 'ớ', 'b': 'bờ', 'c': 'cờ', 'd': 'dờ', 'đ': 'đờ',
  'e': 'e', 'ê': 'ê', 'g': 'gờ', 'h': 'hờ', 'i': 'i', 'k': 'cờ', 'l': 'lờ',
  'm': 'mờ', 'n': 'nờ', 'o': 'o', 'ô': 'ô', 'ơ': 'ờ', 'p': 'pờ', 'q': 'quờ',
  'r': 'rờ', 's': 'sờ', 't': 'tờ', 'u': 'u', 'ư': 'ừ', 'v': 'vờ', 'x': 'sờ', 'y': 'y'
};

const speakLetterTTSAdmin = (letter: string) => {
  if ('speechSynthesis' in window) {
    window.speechSynthesis.cancel();
    const textToSpeak = pronunciationMapAdmin[letter.toLowerCase()] || letter.toLowerCase();
    const utterance = new SpeechSynthesisUtterance(textToSpeak);
    utterance.lang = 'vi-VN';
    utterance.rate = 0.65;
    window.speechSynthesis.speak(utterance);
  }
};

const speakWordTTSAdmin = (word: string) => {
  if ('speechSynthesis' in window) {
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(word.toLowerCase());
    utterance.lang = 'vi-VN';
    utterance.rate = 0.70;
    window.speechSynthesis.speak(utterance);
  }
};

const PERMISSION_DEFINITIONS = [
  {
    token: 'PERM_ASSIGN_HOMEWORK',
    title: 'Quyền giao bài tập cho học sinh',
    description: 'Cấp quyền này cho Phụ Huynh (được phép giao bài tập viết cho các bé).',
    defaultRoles: ['ROLE_TEACHER']
  },
  {
    token: 'PERM_VIEW_ALPHABET',
    title: 'Quyền xem học liệu chữ cái',
    description: 'Cho phép Giáo viên truy cập trang học liệu, nghe âm mẫu phát âm và xem hướng dẫn chi tiết.',
    defaultRoles: ['ROLE_TEACHER']
  },
  {
    token: 'PERM_EDIT_ALPHABET',
    title: 'Quyền chỉnh sửa học liệu',
    description: 'Cho phép Giáo viên chỉnh sửa hướng dẫn đọc, ghi âm mẫu trực tiếp, tải ảnh minh họa.',
    defaultRoles: ['ROLE_ADMIN']
  },
  {
    token: 'PERM_ADD_ALPHABET',
    title: 'Quyền thêm & xóa chữ cái',
    description: 'Cho phép Giáo viên tạo thêm các ký tự/chữ cái mới hoặc xóa bỏ chữ cái khỏi danh sách.',
    defaultRoles: ['ROLE_ADMIN']
  },
  {
    token: 'PERM_MANAGE_CLASSES',
    title: 'Quyền quản lý lớp học',
    description: 'Cho phép tạo lớp học, thêm học sinh theo tên đăng nhập và kiểm soát danh sách học sinh.',
    defaultRoles: ['ROLE_TEACHER', 'ROLE_ADMIN']
  },
  {
    token: 'PERM_PARENT_TRACKING',
    title: 'Quyền theo dõi tiến độ & nhắc bài bé',
    description: 'Cho phép xem lịch giao bài tập của cô giáo và gửi thông báo nhắc bé làm bài tập.',
    defaultRoles: ['ROLE_PARENT']
  }
];

const AdminDashboard: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const currentTabParam = searchParams.get('tab') as 'stats' | 'users' | 'alphabet' | null;
  const [activeTab, setActiveTab] = useState<'stats' | 'users' | 'alphabet'>(currentTabParam || 'stats');

  useEffect(() => {
    if (currentTabParam && ['stats', 'users', 'alphabet'].includes(currentTabParam)) {
      setActiveTab(currentTabParam);
    }
  }, [currentTabParam]);

  const handleTabChange = (tab: 'stats' | 'users' | 'alphabet') => {
    setActiveTab(tab);
    setSearchParams({ tab });
  };

  

  const [stats, setStats] = useState<any>({
    totalUsers: 0, totalClasses: 0, totalPractices: 0, totalFeedbacks: 0,
    studentCount: 0, teacherCount: 0, parentCount: 0
  });
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [lessons, setLessons] = useState<AlphabetItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [roleFilter, setRoleFilter] = useState<'ALL' | 'ROLE_STUDENT' | 'ROLE_TEACHER' | 'ROLE_PARENT' | 'ROLE_ADMIN'>('ALL');
  const [showEditModal, setShowEditModal] = useState(false);
  const [isAddingLetter, setIsAddingLetter] = useState(false);
  const [selectedChar, setSelectedChar] = useState('');
  const [editGuide, setEditGuide] = useState('');
  const [editExample, setEditExample] = useState('');
  const [selectedImageFile, setSelectedImageFile] = useState<File | null>(null);
  const [previewImageSrc, setPreviewImageSrc] = useState<string | null>(null);
  const [imageVersion, setImageVersion] = useState(Date.now());
  const [uploading, setUploading] = useState(false);
  const [selectedAudioFile, setSelectedAudioFile] = useState<File | null>(null);
  const [isRecording, setIsRecording] = useState(false);
  const [mediaRecorder, setMediaRecorder] = useState<MediaRecorder | null>(null);
  const [recordedAudioUrl, setRecordedAudioUrl] = useState<string | null>(null);
  const [recordedBlob, setRecordedBlob] = useState<Blob | null>(null);
  const [selectedExampleAudioFile, setSelectedExampleAudioFile] = useState<File | null>(null);
  const [recordedExampleAudioUrl, setRecordedExampleAudioUrl] = useState<string | null>(null);
  const [recordedExampleBlob, setRecordedExampleBlob] = useState<Blob | null>(null);
  const [isRecordingExample, setIsRecordingExample] = useState(false);
  const [selectedExampleImgFile, setSelectedExampleImgFile] = useState<File | null>(null);
  const [previewExampleImgSrc, setPreviewExampleImgSrc] = useState<string | null>(null);
  const [editIsHidden, setEditIsHidden] = useState(false);
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);
  const showToast = (message: string, type: 'success' | 'error' = 'success') => setToast({ message, type });
  useEffect(() => {
    if (toast) { const t = setTimeout(() => setToast(null), 3000); return () => clearTimeout(t); }
  }, [toast]);
  const [showPermissionModal, setShowPermissionModal] = useState(false);
  const [selectedUser, setSelectedUser] = useState<UserProfile | null>(null);
  const [userPermissions, setUserPermissions] = useState<string[]>([]);
  const [savingPermissions, setSavingPermissions] = useState(false);
  const [showAddUserModal, setShowAddUserModal] = useState(false);
  const [submittingAddUser, setSubmittingAddUser] = useState(false);
  const [newUsername, setNewUsername] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newFullName, setNewFullName] = useState('');
  const [newEmail, setNewEmail] = useState('');
  const [newRole, setNewRole] = useState('TEACHER');
  const [newPermissions, setNewPermissions] = useState<string[]>([]);
  const [showEditUserModal, setShowEditUserModal] = useState(false);
  const [editingUser, setEditingUser] = useState<UserProfile | null>(null);
  const [editFullName, setEditFullName] = useState('');
  const [editEmail, setEditEmail] = useState('');
  const [editNewPassword, setEditNewPassword] = useState('');
  const [savingEditUser, setSavingEditUser] = useState(false);

  useEffect(() => {
    if (newRole === 'TEACHER') setNewPermissions(['PERM_ASSIGN_HOMEWORK', 'PERM_MANAGE_CLASSES', 'PERM_VIEW_ALPHABET']);
    else if (newRole === 'PARENT') setNewPermissions(['PERM_PARENT_TRACKING']);
    else setNewPermissions([]);
  }, [newRole]);

  const fetchStats = async () => { try { const r = await api.get('/admin/stats'); setStats(r.data); } catch (e) { console.error(e); } };
  const fetchUsers = async () => { try { const r = await api.get('/admin/users'); setUsers(r.data); } catch (e) { console.error(e); } };
  const fetchLessons = async () => { try { const r = await api.get('/student/alphabet'); setLessons(sortVietnameseAlphabet(r.data)); } catch (e) { console.error(e); } };

  useEffect(() => { const init = async () => { setLoading(true); await fetchStats(); setLoading(false); }; init(); }, []);
  useEffect(() => {
    if (activeTab === 'users') fetchUsers();
    if (activeTab === 'alphabet') fetchLessons();
    if (activeTab === 'stats') fetchStats();
  }, [activeTab]);

  const handleToggleUser = async (userId: number) => {
    setUsers(prev => prev.map(u => u.id === userId ? { ...u, enabled: !u.enabled } : u));
    try {
      const res = await api.put(`/admin/users/${userId}/toggle`);
      showToast(res.data.message || 'Cập nhật trạng thái thành công');
    } catch (err: any) {
      setUsers(prev => prev.map(u => u.id === userId ? { ...u, enabled: !u.enabled } : u));
      showToast(err.response?.data?.message || 'Có lỗi xảy ra', 'error');
    }
  };

  const handleChangeRole = async (userId: number, newRole: string) => {
    try {
      const res = await api.put(`/admin/users/${userId}/role?role=${newRole}`);
      showToast(res.data.message || 'Đổi vai trò thành công');
      fetchUsers();
    } catch (err: any) { showToast(err.response?.data?.message || 'Không thể đổi vai trò', 'error'); }
  };

  const handleDeleteUser = async (userId: number) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa vĩnh viễn tài khoản này không?')) return;
    try {
      const res = await api.delete(`/admin/users/${userId}`);
      showToast(res.data.message || 'Xóa tài khoản thành công');
      fetchUsers();
    } catch (err: any) { showToast(err.response?.data?.message || 'Có lỗi xảy ra', 'error'); }
  };

  const handleOpenPermissionModal = (u: UserProfile) => {
    setSelectedUser(u);
    setUserPermissions([...( u.roles || [])]);
    setShowPermissionModal(true);
  };

  const handleSingleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newUsername || !newPassword || !newFullName || !newEmail || !newRole) {
      showToast('Vui lòng điền đầy đủ các thông tin bắt buộc', 'error'); return;
    }
    setSubmittingAddUser(true);
    try {
      const payload = { username: newUsername, password: newPassword, fullName: newFullName, email: newEmail, role: newRole,
        permissions: newRole !== 'STUDENT' && newRole !== 'ADMIN' ? newPermissions : [] };
      const res = await api.post('/admin/users', payload);
      showToast(res.data.message || 'Tạo tài khoản thành công', 'success');
      setNewUsername(''); setNewPassword(''); setNewFullName(''); setNewEmail(''); setNewRole('TEACHER');
      setShowAddUserModal(false); fetchUsers();
    } catch (err: any) { showToast(err.response?.data?.message || 'Có lỗi xảy ra', 'error'); }
    finally { setSubmittingAddUser(false); }
  };

  const handleOpenEditUserModal = (user: UserProfile) => {
    setEditingUser(user); setEditFullName(user.fullName || ''); setEditEmail(user.email || '');
    setEditNewPassword(''); setShowEditUserModal(true);
  };

  const handleSaveEditUser = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingUser) return;
    if (!editFullName.trim() || !editEmail.trim()) { showToast('Họ tên và Email không được để trống', 'error'); return; }
    setSavingEditUser(true);
    try {
      const payload: any = { fullName: editFullName.trim(), email: editEmail.trim() };
      if (editNewPassword && editNewPassword.length >= 6) payload.newPassword = editNewPassword;
      else if (editNewPassword && editNewPassword.length > 0) {
        showToast('Mật khẩu mới phải có ít nhất 6 ký tự', 'error'); setSavingEditUser(false); return;
      }
      await api.put(`/admin/users/${editingUser.id}/info`, payload);
      showToast('Cập nhật thông tin tài khoản thành công', 'success');
      setShowEditUserModal(false); setEditingUser(null); fetchUsers();
    } catch (err: any) { showToast(err.response?.data?.message || 'Có lỗi xảy ra', 'error'); }
    finally { setSavingEditUser(false); }
  };

  const togglePermissionToken = (token: string) => {
    setUserPermissions(prev => prev.includes(token) ? prev.filter(p => p !== token) : [...prev, token]);
  };

  const handleSavePermissionsModal = async () => {
    if (!selectedUser) return;
    setSavingPermissions(true);
    try {
      const selectedPermsOnly = userPermissions.filter(p => p.startsWith('PERM_'));
      const res = await api.put(`/admin/users/${selectedUser.id}/permissions`, selectedPermsOnly);
      showToast(res.data.message || 'Cập nhật bảng phân quyền thành công!', 'success');
      setShowPermissionModal(false); fetchUsers();
    } catch (err: any) { showToast(err.response?.data?.message || 'Có lỗi xảy ra khi lưu phân quyền', 'error'); }
    finally { setSavingPermissions(false); }
  };

  const handleOpenEditModal = (char: string, isAdding: boolean = false) => {
    const foundLesson = lessons.find(l => l.letter.toLowerCase() === char.toLowerCase());
    setSelectedChar(char); setIsAddingLetter(isAdding);
    setEditGuide(foundLesson?.pronunciationGuide || (char ? `Phát âm âm ${char}` : ''));
    setEditExample(foundLesson?.exampleWord || (char ? `Ví dụ chữ ${char}` : ''));
    setSelectedImageFile(null); setPreviewImageSrc(null);
    setSelectedAudioFile(null); setRecordedAudioUrl(foundLesson?.audioUrl || null);
    setRecordedBlob(null); setIsRecording(false);
    setSelectedExampleAudioFile(null); setRecordedExampleAudioUrl(foundLesson?.exampleAudioUrl || null);
    setRecordedExampleBlob(null); setIsRecordingExample(false);
    setSelectedExampleImgFile(null); setPreviewExampleImgSrc(foundLesson?.imageUrl || null);
    setEditIsHidden(foundLesson?.isHidden || false);
    setShowEditModal(true);
  };

  const startRecordingAdmin = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const recorder = new MediaRecorder(stream);
      const chunks: Blob[] = [];
      recorder.ondataavailable = (e) => { if (e.data.size > 0) chunks.push(e.data); };
      recorder.onstop = () => {
        const blob = new Blob(chunks, { type: 'audio/webm' });
        setRecordedBlob(blob); setRecordedAudioUrl(URL.createObjectURL(blob));
        stream.getTracks().forEach(t => t.stop());
      };
      recorder.start(); setMediaRecorder(recorder); setIsRecording(true);
    } catch (err) { showToast('Không thể truy cập Microphone: ' + err, 'error'); }
  };
  const stopRecordingAdmin = () => { if (mediaRecorder && isRecording) { mediaRecorder.stop(); setIsRecording(false); } };
  const deleteRecordedAudio = () => { setRecordedBlob(null); setRecordedAudioUrl(null); };
  const handleAudioFileChangeAdmin = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const file = e.target.files[0]; setSelectedAudioFile(file); setRecordedBlob(null); setRecordedAudioUrl(URL.createObjectURL(file));
    }
  };
  const startRecordingExampleAdmin = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const recorder = new MediaRecorder(stream);
      const chunks: Blob[] = [];
      recorder.ondataavailable = (e) => { if (e.data.size > 0) chunks.push(e.data); };
      recorder.onstop = () => {
        const blob = new Blob(chunks, { type: 'audio/webm' });
        setRecordedExampleBlob(blob); setRecordedExampleAudioUrl(URL.createObjectURL(blob));
        stream.getTracks().forEach(t => t.stop());
      };
      recorder.start(); setMediaRecorder(recorder); setIsRecordingExample(true);
    } catch (err) { showToast('Không thể truy cập Microphone: ' + err, 'error'); }
  };
  const stopRecordingExampleAdmin = () => { if (mediaRecorder && isRecordingExample) { mediaRecorder.stop(); setIsRecordingExample(false); } };
  const deleteRecordedExampleAudio = () => { setRecordedExampleBlob(null); setRecordedExampleAudioUrl(null); };
  const handleExampleAudioFileChangeAdmin = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const file = e.target.files[0]; setSelectedExampleAudioFile(file); setRecordedExampleBlob(null); setRecordedExampleAudioUrl(URL.createObjectURL(file));
    }
  };
  const handleExampleImgChangeAdmin = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const file = e.target.files[0]; setSelectedExampleImgFile(file);
      const reader = new FileReader();
      reader.onloadend = () => setPreviewExampleImgSrc(reader.result as string);
      reader.readAsDataURL(file);
    }
  };
  const playLetterAudioAdmin = () => {
    if (recordedAudioUrl) { const a = new Audio(recordedAudioUrl); a.play().catch(() => speakLetterTTSAdmin(selectedChar)); }
    else speakLetterTTSAdmin(selectedChar);
  };
  const playExampleAudioAdmin = () => {
    if (recordedExampleAudioUrl) { const a = new Audio(recordedExampleAudioUrl); a.play().catch(() => speakWordTTSAdmin(editExample || 'Từ ví dụ')); }
    else speakWordTTSAdmin(editExample || 'Từ ví dụ');
  };
  const handleImageFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const file = e.target.files[0]; setSelectedImageFile(file);
      const reader = new FileReader();
      reader.onloadend = () => setPreviewImageSrc(reader.result as string);
      reader.readAsDataURL(file);
    }
  };

  const handleSaveLetterModal = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedChar.trim()) { showToast('Vui lòng nhập tên chữ cái', 'error'); return; }
    setUploading(true);
    const rawChar = selectedChar.toLowerCase().trim();
    const letterImageName = `${rawChar}.png`;
    try {
      const foundLesson = lessons.find(l => l.letter.toLowerCase() === rawChar);
      let finalAudioUrl = foundLesson?.audioUrl || '';
      let finalImageUrl = foundLesson?.imageUrl || '';
      let finalExampleAudioUrl = foundLesson?.exampleAudioUrl || '';

      const uploadAudio = async (file: File | Blob, name: string) => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('filename', name);
        const r = await api.post('/admin/alphabet/upload-audio', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
        return r.data.success ? r.data.message : '';
      };

      if (selectedAudioFile) finalAudioUrl = await uploadAudio(selectedAudioFile, `${rawChar}_audio_${Date.now()}.${selectedAudioFile.name.split('.').pop()}`);
      else if (recordedBlob) finalAudioUrl = await uploadAudio(new File([recordedBlob], `${rawChar}_audio_${Date.now()}.webm`, { type: 'audio/webm' }), `${rawChar}_audio_${Date.now()}.webm`);

      if (selectedExampleAudioFile) finalExampleAudioUrl = await uploadAudio(selectedExampleAudioFile, `${rawChar}_example_audio_${Date.now()}.${selectedExampleAudioFile.name.split('.').pop()}`);
      else if (recordedExampleBlob) finalExampleAudioUrl = await uploadAudio(new File([recordedExampleBlob], `${rawChar}_example_audio_${Date.now()}.webm`, { type: 'audio/webm' }), `${rawChar}_example_audio_${Date.now()}.webm`);

      if (selectedExampleImgFile) {
        const fd = new FormData(); fd.append('file', selectedExampleImgFile); fd.append('filename', `${rawChar}_example_${Date.now()}.${selectedExampleImgFile.name.split('.').pop()}`);
        const r = await api.post('/admin/alphabet/upload-example-image', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
        if (r.data.success) finalImageUrl = r.data.message;
      }
      if (selectedImageFile) {
        const fd = new FormData(); fd.append('file', selectedImageFile); fd.append('filename', letterImageName);
        await api.post('/admin/alphabet/upload-image', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
      }

      const payload = {
        ...(foundLesson ? { id: foundLesson.id } : {}),
        letter: rawChar, pronunciationGuide: editGuide, exampleWord: editExample,
        audioUrl: finalAudioUrl, imageUrl: finalImageUrl, exampleAudioUrl: finalExampleAudioUrl,
        isHidden: editIsHidden
      };
      await api.post('/admin/alphabet', payload);
      showToast('Lưu học liệu chữ cái thành công', 'success');
      setShowEditModal(false); setImageVersion(Date.now()); fetchLessons();
    } catch (err: any) { showToast(err.response?.data?.message || 'Có lỗi xảy ra khi lưu', 'error'); }
    finally { setUploading(false); }
  };

  const handleDeleteLetterAdmin = async () => {
    const rawChar = selectedChar.toLowerCase().trim();
    if (!window.confirm(`Bạn có chắc chắn muốn XÓA chữ cái "${rawChar}" không?`)) return;
    const foundLesson = lessons.find(l => l.letter.toLowerCase() === rawChar);
    if (foundLesson && foundLesson.id) {
      try {
        const res = await api.delete(`/admin/alphabet/${foundLesson.id}`);
        showToast(res.data.message || 'Xóa chữ cái thành công');
        fetchLessons();
      } catch (err: any) { showToast(err.response?.data?.message || 'Xóa chữ cái thất bại!', 'error'); }
    } else { showToast('Không tìm thấy ID của chữ cái này!', 'error'); }
    setShowEditModal(false);
  };

  if (loading) return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] gap-3">
      <div className="w-12 h-12 border-4 border-sky-200 border-t-sky-600 rounded-full animate-spin"></div>
      <p className="text-sm font-bold text-sky-800">Đang tải dữ liệu quản trị viên...</p>
    </div>
  );

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-12">
      {/* Tab Navigation */}
      <div className="bg-white border-2 border-sky-100 p-2 rounded-2xl shadow-sm flex flex-wrap gap-2">
        {[
          { key: 'stats', label: 'Thống kê tổng quan', icon: Shield, color: 'text-sky-600' },
          { key: 'users', label: 'Quản lý người dùng & Phân Quyền', icon: Users, color: 'text-indigo-600' },
          { key: 'alphabet', label: 'Quản Lý Học Liệu Chữ Cái', icon: BookOpen, color: 'text-emerald-600' },
        ].map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.key;
          return (
            <button key={tab.key} onClick={() => handleTabChange(tab.key as any)}
              className={`flex items-center gap-2.5 px-5 py-3 rounded-xl font-extrabold text-xs transition-all cursor-pointer ${isActive ? 'bg-sky-600 text-white shadow-md shadow-sky-600/30 scale-[1.02]' : 'bg-transparent text-slate-600 hover:bg-sky-50 hover:text-sky-700'}`}>
              <Icon className={`w-4 h-4 ${isActive ? 'text-white' : tab.color}`} />
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* STATS TAB */}
      {activeTab === 'stats' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
            {[
              { label: 'Tổng tài khoản', value: stats.totalUsers || 0, sub: 'Đã đăng ký hệ thống', color: 'border-sky-100', textColor: 'text-sky-700' },
              { label: 'Tổng lớp học', value: stats.totalClasses || 0, sub: 'Lớp học đang tạo', color: 'border-indigo-100', textColor: 'text-indigo-600' },
              { label: 'Lượt viết chữ', value: stats.totalPractices || 0, sub: 'Bài tập bé đã hoàn thành', color: 'border-emerald-100', textColor: 'text-emerald-600' },
              { label: 'Góp ý mở', value: stats.totalFeedbacks || 0, sub: 'Cần admin phản hồi', color: 'border-rose-100', textColor: 'text-rose-500' },
            ].map(s => (
              <div key={s.label} className={`bg-white border-2 ${s.color} p-6 rounded-3xl shadow-sm space-y-2`}>
                <span className="text-xs font-black uppercase text-slate-500 tracking-wider">{s.label}</span>
                <p className={`text-4xl font-black ${s.textColor}`}>{s.value}</p>
                <p className="text-[11px] font-bold text-slate-400">{s.sub}</p>
              </div>
            ))}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            <div className="bg-gradient-to-br from-amber-50 to-orange-50 border-2 border-amber-200 p-6 rounded-3xl space-y-2 text-center">
              <span className="text-xs font-black text-amber-800 uppercase tracking-widest">Học sinh (Student)</span>
              <p className="text-3xl font-black text-amber-600">{stats.studentCount || 0} bé</p>
            </div>
            <div className="bg-gradient-to-br from-sky-50 to-blue-50 border-2 border-sky-200 p-6 rounded-3xl space-y-2 text-center">
              <span className="text-xs font-black text-sky-800 uppercase tracking-widest">Giáo viên (Teacher)</span>
              <p className="text-3xl font-black text-sky-600">{stats.teacherCount || 0} cô</p>
            </div>
            <div className="bg-gradient-to-br from-pink-50 to-rose-50 border-2 border-pink-200 p-6 rounded-3xl space-y-2 text-center">
              <span className="text-xs font-black text-pink-800 uppercase tracking-widest">Phụ huynh (Parent)</span>
              <p className="text-3xl font-black text-pink-600">{stats.parentCount || 0} người</p>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="bg-white border-2 border-sky-100 rounded-3xl p-6 shadow-sm">
              <h3 className="text-sm font-black text-slate-700 mb-1">📊 Phân bổ người dùng</h3>
              <p className="text-[11px] font-bold text-slate-400 mb-2">Tỷ lệ các vai trò trong hệ thống</p>
              {(stats.studentCount || 0) + (stats.teacherCount || 0) + (stats.parentCount || 0) === 0 ? (
                <div className="flex items-center justify-center h-[220px] text-slate-300">
                  <p className="text-xs font-bold">Chưa có người dùng nào</p>
                </div>
              ) : (
                <ResponsiveContainer width="100%" height={240}>
                  <PieChart>
                    <Pie data={[
                      { name: 'Học sinh', value: stats.studentCount || 0 },
                      { name: 'Giáo viên', value: stats.teacherCount || 0 },
                      { name: 'Phụ huynh', value: stats.parentCount || 0 },
                    ]} cx="50%" cy="50%" innerRadius={60} outerRadius={90} paddingAngle={5} dataKey="value">
                      <Cell fill="#f59e0b" /><Cell fill="#0ea5e9" /><Cell fill="#ec4899" />
                    </Pie>
                    <Tooltip contentStyle={{ borderRadius: '12px', border: '2px solid #e0f2fe', fontSize: '12px', fontWeight: 700 }} formatter={(value: any, name: any) => [`${value} người`, name]} />
                    <Legend iconType="circle" iconSize={8} wrapperStyle={{ fontSize: '11px', fontWeight: 700 }} />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </div>

            <div className="bg-white border-2 border-indigo-100 rounded-3xl p-6 shadow-sm">
              <h3 className="text-sm font-black text-slate-700 mb-1">📈 Tổng quan hệ thống</h3>
              <p className="text-[11px] font-bold text-slate-400 mb-5">Các chỉ số toàn hệ thống hiện tại</p>
              <ResponsiveContainer width="100%" height={240}>
                <BarChart data={[
                  { name: 'Tổng TK', value: stats.totalUsers || 0 },
                  { name: 'Lớp học', value: stats.totalClasses || 0 },
                  { name: 'Lượt viết', value: stats.totalPractices || 0 },
                  { name: 'Góp ý', value: stats.totalFeedbacks || 0 },
                ]} margin={{ top: 5, right: 10, left: -20, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                  <XAxis dataKey="name" tick={{ fontSize: 11, fontWeight: 700, fill: '#64748b' }} />
                  <YAxis tick={{ fontSize: 11, fill: '#94a3b8' }} allowDecimals={false} />
                  <Tooltip contentStyle={{ borderRadius: '12px', border: '2px solid #e8e8ff', fontSize: '12px', fontWeight: 700 }} cursor={{ fill: '#f5f3ff' }} />
                  <Bar dataKey="value" radius={[8, 8, 0, 0]}>
                    {[{ fill: '#6366f1' }, { fill: '#0ea5e9' }, { fill: '#10b981' }, { fill: '#f43f5e' }].map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.fill} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      )}

      {/* USERS TAB */}
      {activeTab === 'users' && (
        <div className="bg-white border-2 border-sky-100 rounded-3xl p-6 shadow-sm space-y-5">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b-2 border-sky-50">
            <div>
              <h2 className="text-xl font-black text-slate-800">Danh Sách Tài Khoản & Bộ Lọc Phân Loại 4 Đối Tượng ({users.length})</h2>
              <p className="text-xs font-bold text-slate-500">Lọc danh sách theo từng đối tượng và quản lý các quyền riêng biệt.</p>
            </div>
            <button onClick={() => setShowAddUserModal(true)}
              className="px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-black text-xs flex items-center gap-2 shadow-md cursor-pointer transition-all active:scale-95 shrink-0">
              <UserPlus className="w-4 h-4" /> Thêm Tài Khoản Người Dùng Mới
            </button>
          </div>

          <div className="flex flex-wrap items-center gap-2 p-1.5 bg-slate-100/80 border-2 border-slate-200 rounded-2xl">
            {[
              { key: 'ALL', label: 'Tất cả', count: users.length },
              { key: 'ROLE_STUDENT', label: 'Học sinh (Student)', count: users.filter(u => u.roles?.includes('ROLE_STUDENT')).length },
              { key: 'ROLE_TEACHER', label: 'Giáo viên (Teacher)', count: users.filter(u => u.roles?.includes('ROLE_TEACHER')).length },
              { key: 'ROLE_PARENT', label: 'Phụ huynh (Parent)', count: users.filter(u => u.roles?.includes('ROLE_PARENT')).length },
              { key: 'ROLE_ADMIN', label: 'Quản trị viên', count: users.filter(u => u.roles?.includes('ROLE_ADMIN')).length },
            ].map((f) => (
              <button key={f.key} onClick={() => setRoleFilter(f.key as any)}
                className={`px-4 py-2 rounded-xl font-black text-xs transition-all cursor-pointer flex items-center gap-1.5 ${roleFilter === f.key ? 'bg-indigo-600 text-white shadow-md scale-[1.02]' : 'bg-white text-slate-700 hover:bg-indigo-50 hover:text-indigo-700 border border-slate-200'}`}>
                <span>{f.label}</span>
                <span className={`px-2 py-0.5 rounded-full text-[10px] font-black ${roleFilter === f.key ? 'bg-white/20 text-white' : 'bg-slate-200 text-slate-700'}`}>{f.count}</span>
              </button>
            ))}
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-slate-100/80 text-slate-700 font-black uppercase tracking-wider rounded-xl">
                  <th className="py-3.5 px-4 rounded-l-xl">Họ và Tên</th>
                  <th className="py-3.5 px-4">Tên Đăng Nhập</th>
                  <th className="py-3.5 px-4">Đối Tượng (Vai Trò)</th>
                  <th className="py-3.5 px-4">Quyền Riêng Được Cấp</th>
                  <th className="py-3.5 px-4">Trạng Thái</th>
                  <th className="py-3.5 px-4 text-center rounded-r-xl">Hành Động</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium">
                {users.filter(u => roleFilter === 'ALL' || u.roles?.includes(roleFilter)).map((u) => {
                  const primaryRole = (u.roles || []).find(r => ['ROLE_STUDENT', 'ROLE_TEACHER', 'ROLE_PARENT', 'ROLE_ADMIN'].includes(r)) || 'ROLE_STUDENT';
                  const activePerms = u.roles || [];
                  const permBadges = [
                    { token: 'PERM_ASSIGN_HOMEWORK', label: 'Giao bài tập', color: 'bg-amber-100 text-amber-900 border-amber-300' },
                    { token: 'PERM_MANAGE_CLASSES', label: 'Quản Lý Lớp', color: 'bg-blue-100 text-blue-900 border-blue-300' },
                    { token: 'PERM_PARENT_TRACKING', label: 'Theo dõi tiến độ', color: 'bg-pink-100 text-pink-900 border-pink-300' },
                  ].filter(p => activePerms.includes(p.token));
                  return (
                    <tr key={u.id} className="hover:bg-sky-50/50 transition-colors">
                      <td className="py-4 px-4 font-extrabold text-slate-900">{u.fullName}</td>
                      <td className="py-4 px-4 font-bold text-sky-700">{u.username}</td>
                      <td className="py-4 px-4">
                        <select value={primaryRole} onChange={(e) => handleChangeRole(u.id, e.target.value)}
                          className="bg-sky-50 border-2 border-sky-200 text-sky-900 font-extrabold text-xs rounded-xl px-3 py-1.5 focus:outline-none focus:border-sky-500 cursor-pointer shadow-sm">
                          <option value="ROLE_STUDENT">Học sinh (STUDENT)</option>
                          <option value="ROLE_TEACHER">Giáo viên (TEACHER)</option>
                          <option value="ROLE_PARENT">Phụ huynh (PARENT)</option>
                          <option value="ROLE_ADMIN">Quản trị viên (ADMIN)</option>
                        </select>
                      </td>
                      <td className="py-4 px-4">
                        <div className="flex flex-wrap gap-1.5 max-w-xs">
                          {permBadges.length > 0 ? permBadges.map(b => (
                            <span key={b.token} className={`px-2.5 py-1 rounded-lg font-black text-[10px] border shadow-xs ${b.color}`}>{b.label}</span>
                          )) : <span className="text-[11px] text-slate-400 font-bold italic">Quyền cơ bản</span>}
                        </div>
                      </td>
                      <td className="py-4 px-4">
                        <div className="flex items-center gap-2.5">
                          <button type="button" onClick={() => handleToggleUser(u.id)}
                            className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${u.enabled ? 'bg-emerald-500' : 'bg-slate-300'}`}>
                            <span className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-md ring-0 transition duration-200 ease-in-out ${u.enabled ? 'translate-x-5' : 'translate-x-0'}`} />
                          </button>
                          <span className={`text-[11px] font-black ${u.enabled ? 'text-emerald-700' : 'text-rose-600'}`}>
                            {u.enabled ? 'Hoạt động' : 'Bị khóa'}
                          </span>
                        </div>
                      </td>
                      <td className="py-4 px-4 text-center">
                        <div className="inline-flex gap-2">
                          {primaryRole !== 'ROLE_STUDENT' ? (
                            <button onClick={() => handleOpenPermissionModal(u)}
                              className="px-3 py-1.5 rounded-xl bg-indigo-50 text-indigo-700 border-2 border-indigo-200 hover:bg-indigo-100 font-black text-xs transition-all cursor-pointer flex items-center gap-1 shadow-sm">
                              <Key className="w-3.5 h-3.5" /> Phân Quyền
                            </button>
                          ) : (
                            <button disabled className="px-3 py-1.5 rounded-xl bg-slate-100 text-slate-400 border-2 border-slate-200 font-extrabold text-xs cursor-not-allowed">
                              Học sinh
                            </button>
                          )}
                          <button onClick={() => handleOpenEditUserModal(u)}
                            className="p-1.5 text-sky-600 bg-sky-50 border-2 border-sky-200 hover:bg-sky-100 rounded-xl transition-all cursor-pointer">
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button onClick={() => handleDeleteUser(u.id)}
                            className="p-1.5 text-rose-600 bg-rose-50 border-2 border-rose-200 hover:bg-rose-100 rounded-xl transition-all cursor-pointer">
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ALPHABET TAB */}
      {activeTab === 'alphabet' && (
        <div className="bg-white border-2 border-sky-100 rounded-3xl p-6 sm:p-8 shadow-sm space-y-6">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b-2 border-sky-50">
            <div>
              <h2 className="text-xl sm:text-2xl font-black text-slate-800 flex items-center gap-2">
                <BookOpen className="w-7 h-7 text-emerald-600" /> Quản Lý Học Liệu & Chỉnh Sửa Chữ Cái
              </h2>
              <p className="text-xs font-bold text-slate-500 mt-1">
                Bấm vào bất kỳ ô chữ nào bên dưới để <strong>chỉnh sửa thông tin & tải/thay tệp ảnh, âm thanh mẫu</strong>
              </p>
            </div>
            <button onClick={() => handleOpenEditModal('', true)}
              className="px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-black text-xs flex items-center gap-2 shadow-md transition-all active:scale-95 cursor-pointer shrink-0">
              <Plus className="w-4 h-4" /> Thêm Chữ Cái Mới
            </button>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-5 pt-2">
            {Array.from(new Set([...VIETNAMESE_ALPHABET, ...lessons.map(l => l.letter.toLowerCase())])).map((char) => {
              const displayChar = char.length === 1 ? `${char.toUpperCase()}${char.toLowerCase()}` : char.toUpperCase();
              const fileName = `${char.toLowerCase()}.png`;
              const imgUrl = `/alphabet/${fileName}?v=${imageVersion}`;
              const foundLesson = lessons.find(l => l.letter.toLowerCase() === char.toLowerCase());
              const emoji = getExampleEmoji(foundLesson?.exampleWord, char); //'🌟';
              const isHidden = foundLesson?.isHidden || false;
              return (
                <div key={char} onClick={() => handleOpenEditModal(char)}
                  className={`bg-white border-2 ${isHidden ? 'border-rose-300 opacity-70' : 'border-sky-200 hover:border-emerald-500'} rounded-2xl p-3 shadow-sm hover:shadow-xl transition-all flex flex-col items-center justify-between text-center space-y-3 relative group cursor-pointer`}>
                  {isHidden && (
                    <div className="absolute top-1 right-1 bg-rose-100 rounded-full p-0.5">
                      <EyeOff className="w-3 h-3 text-rose-500" />
                    </div>
                  )}
                  <div className="w-full flex items-center justify-between text-[10px] font-black text-slate-500 border-b border-sky-100 pb-1.5">
                    <span className="uppercase">Học Liệu</span>
                    <span className="font-mono text-emerald-700 font-bold">{fileName}</span>
                  </div>
                  <div className="relative w-24 h-24 rounded-xl border-2 border-sky-300 overflow-hidden bg-white flex items-center justify-center p-1 shadow-inner group-hover:border-emerald-500 transition-colors">
                    <div className="absolute inset-0 flex flex-col items-center justify-center">
                      <div className="relative w-16 h-16 flex items-center justify-center">
                        <div className="absolute inset-0 flex flex-col items-center justify-center">
                          <span className="text-2xl leading-none">{emoji}</span>
                          <span className="text-xs font-black text-slate-800 font-mali leading-none mt-0.5">{displayChar}</span>
                        </div>
                        {foundLesson?.imageUrl && (
                          <img src={foundLesson.imageUrl.startsWith('http') || foundLesson.imageUrl.startsWith('/') ? foundLesson.imageUrl : `/example_images/${foundLesson.imageUrl}`}
                            alt={foundLesson.exampleWord}
                            className="absolute inset-0 w-full h-full object-contain rounded-lg border border-slate-100 bg-white z-10"
                            onError={(e) => { (e.target as HTMLElement).style.display = 'none'; }} />
                        )}
                      </div>
                    </div>
                    <img src={imgUrl} alt={displayChar}
                      className="absolute inset-0 w-full h-full object-fill crisp-letter-img pointer-events-none rounded-lg z-20 bg-white"
                      onError={(e) => { (e.target as HTMLElement).style.display = 'none'; }} />
                  </div>
                  <div className="w-full pt-1 border-t border-sky-50 flex items-center justify-center gap-1.5 text-xs font-black text-sky-700 group-hover:text-emerald-600">
                    <Edit3 className="w-3.5 h-3.5" />
                    <span>Sửa chữ {displayChar}</span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* PERMISSION MODAL */}
      {showPermissionModal && selectedUser && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 animate-fade-in">
          <div className="bg-white border-4 border-indigo-200 rounded-[32px] p-6 sm:p-8 max-w-xl w-full shadow-2xl space-y-6 relative max-h-[90vh] overflow-y-auto">
            <button onClick={() => setShowPermissionModal(false)} className="absolute top-5 right-5 p-2 rounded-full bg-slate-100 text-slate-500 hover:bg-rose-100 hover:text-rose-600 transition-colors"><X className="w-5 h-5" /></button>
            <div className="flex items-center gap-3 border-b-2 border-indigo-50 pb-4">
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-500 to-purple-600 flex items-center justify-center text-white font-black shadow-md"><Key className="w-6 h-6 text-white" /></div>
              <div>
                <h3 className="text-lg font-black text-slate-800">Phân Quyền Chi Tiết - {selectedUser.fullName}</h3>
                <p className="text-xs font-bold text-indigo-600">Username: <span className="font-mono">{selectedUser.username}</span></p>
              </div>
            </div>
            <div className="space-y-4">
              <p className="text-xs font-extrabold text-slate-600 bg-indigo-50 p-3 rounded-xl border border-indigo-100">
                Chọn các quyền hành động cụ thể bên dưới để <strong>Cấp (Thêm)</strong> hoặc <strong>Tắt (Khóa)</strong> cho tài khoản này.
              </p>
              <div className="space-y-3">
                {selectedUser.roles.includes('ROLE_STUDENT') && <div className="text-center py-8 text-slate-400 font-extrabold text-xs">Tài khoản Học sinh không có quyền hành động chi tiết nào cần cấu hình.</div>}
                {selectedUser.roles.includes('ROLE_ADMIN') && <div className="text-center py-8 text-indigo-500 font-extrabold text-xs bg-indigo-50/50 rounded-2xl border border-indigo-100">Tài khoản Quản trị viên mặc định có toàn quyền hệ thống, không cần cấu hình quyền chi tiết.</div>}
                {selectedUser.roles.includes('ROLE_TEACHER') && (
                  <div className="space-y-3">
                    {[
                      { token: 'PERM_ASSIGN_HOMEWORK', label: 'Quyền giao bài tập cho học sinh' },
                      { token: 'PERM_MANAGE_CLASSES', label: 'Quyền quản lý lớp học' },
                    ].map(perm => (
                      <div key={perm.token} onClick={() => togglePermissionToken(perm.token)}
                        className={`p-4 rounded-2xl border-2 transition-all cursor-pointer flex items-center justify-between ${userPermissions.includes(perm.token) ? 'bg-amber-50 border-amber-300 shadow-sm' : 'bg-slate-50 border-slate-200 hover:border-slate-300'}`}>
                        <span className="text-xs font-black text-slate-800">{perm.label}</span>
                        {userPermissions.includes(perm.token) ? <CheckSquare className="w-5 h-5 text-amber-600 fill-amber-100" /> : <Square className="w-5 h-5 text-slate-400" />}
                      </div>
                    ))}
                    <div className="bg-sky-50/50 border-2 border-sky-100 rounded-3xl p-4 space-y-3">
                      <div onClick={() => {
                        const has = ['PERM_VIEW_ALPHABET', 'PERM_ADD_ALPHABET', 'PERM_EDIT_ALPHABET', 'PERM_DELETE_ALPHABET'].some(t => userPermissions.includes(t));
                        if (has) setUserPermissions(prev => prev.filter(p => !['PERM_VIEW_ALPHABET', 'PERM_ADD_ALPHABET', 'PERM_EDIT_ALPHABET', 'PERM_DELETE_ALPHABET'].includes(p)));
                        else setUserPermissions(prev => [...prev, 'PERM_VIEW_ALPHABET']);
                      }} className={`p-3 rounded-2xl border-2 transition-all cursor-pointer flex items-center justify-between bg-white ${['PERM_VIEW_ALPHABET', 'PERM_ADD_ALPHABET', 'PERM_EDIT_ALPHABET', 'PERM_DELETE_ALPHABET'].some(t => userPermissions.includes(t)) ? 'border-sky-300 shadow-sm' : 'border-slate-100 hover:border-slate-200'}`}>
                        <div>
                          <span className="text-xs font-black text-slate-800">Quyền quản lý học liệu chữ cái</span>
                          <span className="text-[9px] text-slate-400 font-bold ml-2">(Chủ yếu là quản lý phần đọc chữ của học sinh)</span>
                        </div>
                        {['PERM_VIEW_ALPHABET', 'PERM_ADD_ALPHABET', 'PERM_EDIT_ALPHABET', 'PERM_DELETE_ALPHABET'].some(t => userPermissions.includes(t)) ? <CheckSquare className="w-5 h-5 text-sky-600 fill-sky-50" /> : <Square className="w-5 h-5 text-slate-300" />}
                      </div>
                      {['PERM_VIEW_ALPHABET', 'PERM_ADD_ALPHABET', 'PERM_EDIT_ALPHABET', 'PERM_DELETE_ALPHABET'].some(t => userPermissions.includes(t)) && (
                        <div className="grid grid-cols-2 gap-3">
                          {[{ label: 'Thêm', token: 'PERM_ADD_ALPHABET' }, { label: 'Sửa', token: 'PERM_EDIT_ALPHABET' }, { label: 'Xóa', token: 'PERM_DELETE_ALPHABET' }, { label: 'Xem', token: 'PERM_VIEW_ALPHABET' }].map(sub => {
                            const has = userPermissions.includes(sub.token);
                            return (
                              <div key={sub.token} onClick={() => togglePermissionToken(sub.token)}
                                className={`p-3 rounded-xl border-2 transition-all cursor-pointer flex items-center justify-between ${has ? 'bg-amber-50 border-amber-300 shadow-sm' : 'bg-white border-slate-100 hover:border-slate-200'}`}>
                                <span className="text-xs font-black text-slate-700">{sub.label}</span>
                                {has ? <CheckSquare className="w-5 h-5 text-amber-600 fill-amber-50" /> : <Square className="w-5 h-5 text-slate-300" />}
                              </div>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  </div>
                )}
                {selectedUser.roles.includes('ROLE_PARENT') && (
                  <div className="space-y-3">
                    {[
                      { token: 'PERM_ASSIGN_HOMEWORK', label: 'Quyền giao bài tập cho con' },
                      { token: 'PERM_PARENT_TRACKING', label: 'Quyền theo dõi tiến độ & nhắc bài bé' },
                    ].map(perm => (
                      <div key={perm.token} onClick={() => togglePermissionToken(perm.token)}
                        className={`p-4 rounded-2xl border-2 transition-all cursor-pointer flex items-center justify-between ${userPermissions.includes(perm.token) ? 'bg-amber-50 border-amber-300 shadow-sm' : 'bg-slate-50 border-slate-200 hover:border-slate-300'}`}>
                        <span className="text-xs font-black text-slate-800">{perm.label}</span>
                        {userPermissions.includes(perm.token) ? <CheckSquare className="w-5 h-5 text-amber-600 fill-amber-100" /> : <Square className="w-5 h-5 text-slate-400" />}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
            <div className="flex items-center justify-end gap-3 pt-4 border-t border-indigo-100">
              <button onClick={() => setShowPermissionModal(false)} className="px-5 py-2.5 rounded-xl bg-slate-100 text-slate-600 font-extrabold text-xs hover:bg-slate-200 transition-colors">Hủy Bỏ</button>
              <button onClick={handleSavePermissionsModal} disabled={savingPermissions}
                className="px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-black text-xs shadow-lg transition-all active:scale-95 cursor-pointer disabled:opacity-50 flex items-center gap-2">
                {savingPermissions ? 'Đang lưu...' : 'Lưu Cấu Hình Phân Quyền'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ADD USER MODAL */}
      {showAddUserModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 animate-fade-in">
          <div className="bg-white border-4 border-indigo-200 rounded-[32px] p-6 sm:p-8 max-w-2xl w-full shadow-2xl space-y-6 relative max-h-[90vh] overflow-y-auto">
            <button onClick={() => setShowAddUserModal(false)} className="absolute top-5 right-5 p-2 rounded-full bg-slate-100 text-slate-500 hover:bg-rose-100 hover:text-rose-600 transition-colors"><X className="w-5 h-5" /></button>
            <div className="flex items-center gap-3 border-b-2 border-indigo-50 pb-4">
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-500 to-purple-600 flex items-center justify-center text-white font-black shadow-md"><UserPlus className="w-6 h-6 text-white" /></div>
              <div>
                <h3 className="text-lg font-black text-slate-800">Thêm Tài Khoản Người Dùng Mới</h3>
                <p className="text-xs font-bold text-indigo-600">Tài khoản sẽ kích hoạt ngay, không cần xác thực OTP</p>
              </div>
            </div>
            <form onSubmit={handleSingleCreate} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">Tên đăng nhập *</label>
                  <input type="text" value={newUsername} onChange={(e) => setNewUsername(e.target.value)} placeholder="VD: teacher_nguyen"
                    className="w-full bg-slate-50 border-2 border-slate-200 rounded-xl px-4 py-2.5 text-xs font-bold text-slate-800 focus:outline-none focus:border-indigo-500" required />
                </div>
                <div>
                  <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">Mật khẩu *</label>
                  <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} placeholder="Tối thiểu 6 ký tự"
                    className="w-full bg-slate-50 border-2 border-slate-200 rounded-xl px-4 py-2.5 text-xs font-bold text-slate-800 focus:outline-none focus:border-indigo-500" required />
                </div>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">Họ và tên *</label>
                  <input type="text" value={newFullName} onChange={(e) => setNewFullName(e.target.value)} placeholder="VD: Nguyễn Văn A"
                    className="w-full bg-slate-50 border-2 border-slate-200 rounded-xl px-4 py-2.5 text-xs font-bold text-slate-800 focus:outline-none focus:border-indigo-500" required />
                </div>
                <div>
                  <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">Email *</label>
                  <input type="email" value={newEmail} onChange={(e) => setNewEmail(e.target.value)} placeholder="VD: email@example.com"
                    className="w-full bg-slate-50 border-2 border-slate-200 rounded-xl px-4 py-2.5 text-xs font-bold text-slate-800 focus:outline-none focus:border-indigo-500" required />
                </div>
              </div>
              <div>
                <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">Vai trò chính *</label>
                <select value={newRole} onChange={(e) => setNewRole(e.target.value)}
                  className="w-full bg-slate-50 border-2 border-slate-200 rounded-xl px-4 py-2.5 text-xs font-black text-slate-800 focus:outline-none focus:border-indigo-500 cursor-pointer">
                  <option value="TEACHER">Giáo viên (TEACHER)</option>
                  <option value="PARENT">Phụ huynh (PARENT)</option>
                  <option value="STUDENT">Học sinh (STUDENT)</option>
                  <option value="ADMIN">Quản trị viên (ADMIN)</option>
                </select>
              </div>
              {newRole !== 'STUDENT' && newRole !== 'ADMIN' && (
                <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200 space-y-3">
                  <span className="text-xs font-black text-slate-700 block">Cấu Hình Quyền Chi Tiết Ngay Khi Tạo</span>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    {PERMISSION_DEFINITIONS.filter(perm => {
                      if (newRole === 'TEACHER') return ['PERM_ASSIGN_HOMEWORK', 'PERM_MANAGE_CLASSES', 'PERM_VIEW_ALPHABET'].includes(perm.token);
                      if (newRole === 'PARENT') return ['PERM_PARENT_TRACKING'].includes(perm.token);
                      return false;
                    }).map((perm) => {
                      const isChecked = newPermissions.includes(perm.token);
                      return (
                        <label key={perm.token} className="flex items-start gap-2.5 p-2 rounded-lg hover:bg-slate-100 cursor-pointer">
                          <input type="checkbox" checked={isChecked} onChange={() => setNewPermissions(prev => prev.includes(perm.token) ? prev.filter(p => p !== perm.token) : [...prev, perm.token])}
                            className="mt-0.5 rounded text-indigo-600 focus:ring-indigo-500 cursor-pointer w-4 h-4" />
                          <div className="text-[11px] font-bold text-slate-800">{perm.title}</div>
                        </label>
                      );
                    })}
                  </div>
                </div>
              )}
              <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100">
                <button type="button" onClick={() => setShowAddUserModal(false)} className="px-5 py-2.5 rounded-xl bg-slate-100 text-slate-600 hover:bg-slate-200 text-xs font-black transition-all">Hủy Bỏ</button>
                <button type="submit" disabled={submittingAddUser}
                  className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-black shadow-md flex items-center gap-1.5 transition-all disabled:opacity-60">
                  {submittingAddUser ? 'Đang tạo...' : 'Tạo Tài Khoản'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* EDIT USER MODAL */}
      {showEditUserModal && editingUser && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 animate-fade-in">
          <div className="bg-white border-4 border-sky-200 rounded-[32px] p-6 sm:p-8 max-w-lg w-full shadow-2xl space-y-6 relative max-h-[90vh] overflow-y-auto">
            <button onClick={() => { setShowEditUserModal(false); setEditingUser(null); }} className="absolute top-5 right-5 p-2 rounded-full bg-slate-100 text-slate-500 hover:bg-rose-100 hover:text-rose-600 transition-colors"><X className="w-5 h-5" /></button>
            <div className="flex items-center gap-3 border-b-2 border-sky-50 pb-4">
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-sky-500 to-blue-600 flex items-center justify-center text-white font-black shadow-md"><Edit2 className="w-6 h-6 text-white" /></div>
              <div>
                <h3 className="text-lg font-black text-slate-800">Chỉnh Sửa Thông Tin Tài Khoản</h3>
                <p className="text-xs font-bold text-sky-600">Username: <span className="font-mono bg-sky-50 px-1.5 py-0.5 rounded-lg">{editingUser.username}</span></p>
              </div>
            </div>
            <form onSubmit={handleSaveEditUser} className="space-y-4">
              <div className="bg-slate-50 border border-slate-200 rounded-xl px-4 py-3 space-y-0.5">
                <span className="text-[10px] font-black text-slate-400 uppercase tracking-wider block">Tên đăng nhập (không thể thay đổi)</span>
                <span className="text-sm font-black text-slate-700 font-mono">{editingUser.username}</span>
              </div>
              <div>
                <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">Họ và tên *</label>
                <input type="text" value={editFullName} onChange={(e) => setEditFullName(e.target.value)} placeholder="Nhập họ và tên đầy đủ"
                  className="w-full bg-slate-50 border-2 border-slate-200 rounded-xl px-4 py-2.5 text-xs font-bold text-slate-800 focus:outline-none focus:border-sky-500" required />
              </div>
              <div>
                <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">Email *</label>
                <input type="email" value={editEmail} onChange={(e) => setEditEmail(e.target.value)} placeholder="email@example.com"
                  className="w-full bg-slate-50 border-2 border-slate-200 rounded-xl px-4 py-2.5 text-xs font-bold text-slate-800 focus:outline-none focus:border-sky-500" required />
              </div>
              <div>
                <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">
                  Đặt lại mật khẩu mới <span className="text-slate-400 font-bold normal-case">(Để trống = giữ nguyên)</span>
                </label>
                <input type="password" value={editNewPassword} onChange={(e) => setEditNewPassword(e.target.value)} placeholder="Tối thiểu 6 ký tự nếu muốn đổi"
                  className="w-full bg-slate-50 border-2 border-slate-200 rounded-xl px-4 py-2.5 text-xs font-bold text-slate-800 focus:outline-none focus:border-sky-500" />
              </div>
              <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100">
                <button type="button" onClick={() => { setShowEditUserModal(false); setEditingUser(null); }} className="px-5 py-2.5 rounded-xl bg-slate-100 text-slate-600 hover:bg-slate-200 text-xs font-black transition-all">Hủy Bỏ</button>
                <button type="submit" disabled={savingEditUser}
                  className="px-5 py-2.5 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-black shadow-md flex items-center gap-1.5 transition-all disabled:opacity-60">
                  {savingEditUser ? 'Đang lưu...' : 'Lưu Thay Đổi'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* EDIT LETTER MODAL */}
      {showEditModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 animate-fade-in">
          <div className="bg-white border-4 border-emerald-200 rounded-[32px] p-6 sm:p-8 max-w-4xl w-full shadow-2xl space-y-6 relative max-h-[90vh] overflow-y-auto">
            <button onClick={() => setShowEditModal(false)} className="absolute top-5 right-5 p-2 rounded-full bg-slate-100 text-slate-500 hover:bg-rose-100 hover:text-rose-600 transition-colors"><X className="w-5 h-5" /></button>
            <div className="flex items-center gap-3 border-b-2 border-emerald-50 pb-4">
              <div className="w-16 h-12 rounded-2xl bg-gradient-to-tr from-emerald-400 to-teal-600 flex items-center justify-center text-white font-black text-2xl shadow-md uppercase tracking-wider">
                {selectedChar.toUpperCase()}{selectedChar.toLowerCase()}
              </div>
              <div>
                <h3 className="text-lg font-black text-slate-800">Chỉnh Sửa Học Liệu Chữ Cái {selectedChar.toUpperCase()}{selectedChar.toLowerCase()}</h3>
                <p className="text-xs font-bold text-emerald-600">Chỉnh sửa thông tin & tải/thay tệp ảnh, âm thanh mẫu</p>
              </div>
            </div>
            <form onSubmit={handleSaveLetterModal} className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* PHẦN 1 */}
                <div className="bg-sky-50/40 border-2 border-sky-100 p-5 rounded-[24px] space-y-4">
                  <div className="flex items-center gap-2 border-b border-sky-200 pb-2">
                    <span className="text-base">📚</span>
                    <h4 className="text-sm font-black text-sky-850">PHẦN 1: HỌC CHỮ CÁI</h4>
                  </div>
                  <div>
                    <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">Trạng Thái (Ẩn/Hiện)</label>
                    <button type="button" onClick={() => setEditIsHidden(!editIsHidden)}
                      className={"flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-bold transition-all " + (editIsHidden ? "bg-rose-100 text-rose-600 border border-rose-200" : "bg-emerald-100 text-emerald-700 border border-emerald-200")}>
                      {editIsHidden ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      {editIsHidden ? "Đang Ẩn (Học sinh không thấy)" : "Đang Hiện (Học sinh học bình thường)"}
                    </button>
                  </div>
                  <div>
                    <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">Tên Chữ</label>
                    <input type="text" value={selectedChar} onChange={(e) => setSelectedChar(e.target.value)}
                      className="w-full bg-white border-2 border-sky-100 rounded-xl px-3 py-2.5 text-xs font-black text-slate-900 focus:outline-none focus:border-sky-400 disabled:opacity-75"
                      disabled={!isAddingLetter} required />
                  </div>
                  <div className="bg-white border border-sky-200 rounded-2xl p-4 space-y-3 shadow-sm">
                    <span className="text-[11px] font-black text-sky-850 block">🎙️ Phát Âm Chữ Cái (Ghi âm hoặc Chọn file)</span>
                    <div className="flex flex-col gap-2.5">
                      <div className="flex items-center gap-2 flex-wrap">
                        {!isRecording ? (
                          <button type="button" onClick={startRecordingAdmin}
                            className="px-3 py-1.5 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-[11px] font-black flex items-center gap-1 shadow-sm active:scale-95 transition-all">
                            <Mic className="w-3 h-3" /> Ghi âm
                          </button>
                        ) : (
                          <button type="button" onClick={stopRecordingAdmin}
                            className="px-3 py-1.5 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-[11px] font-black flex items-center gap-1 shadow-sm animate-pulse active:scale-95 transition-all">
                            <Square className="w-3 h-3" /> Lưu
                          </button>
                        )}
                        <label className="px-3 py-1.5 rounded-xl bg-white border border-sky-300 text-sky-600 hover:bg-sky-50 text-[11px] font-black flex items-center gap-1 shadow-sm cursor-pointer transition-all active:scale-95">
                          <Upload className="w-3 h-3" /> Chọn file
                          <input type="file" accept="audio/*" onChange={handleAudioFileChangeAdmin} className="hidden" />
                        </label>
                        {recordedAudioUrl && (
                          <button type="button" onClick={deleteRecordedAudio}
                            className="p-1.5 rounded-xl bg-rose-50 hover:bg-rose-100 text-rose-600 border border-rose-200 transition-colors" title="Xóa âm thanh">
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>
                      <button type="button" onClick={playLetterAudioAdmin}
                        className="px-3 py-2 bg-sky-100 hover:bg-sky-200 text-sky-700 rounded-xl text-xs font-black flex items-center gap-1.5 active:scale-95 transition-all shadow-sm w-fit">
                        <Volume2 className="w-4 h-4" /> Nghe thử phát âm chữ cái
                      </button>
                      {selectedAudioFile && <p className="text-[10px] font-extrabold text-sky-700">✅ Đã chọn: {selectedAudioFile.name}</p>}
                      {recordedBlob && !selectedAudioFile && <p className="text-[10px] font-extrabold text-teal-700">🔴 Ghi âm sẵn sàng ({Math.round(recordedBlob.size / 1024)} KB)</p>}
                    </div>
                  </div>
                  <div className="bg-white border border-sky-200 rounded-2xl p-4 space-y-3 shadow-sm">
                    <span className="text-[11px] font-black text-slate-700 block">📷 Ảnh Nét Chữ Mẫu (.png)</span>
                    <div className="flex items-center gap-4">
                      <div className="w-16 h-16 rounded-xl border-2 border-emerald-400 bg-white overflow-hidden flex items-center justify-center p-1 shadow-inner shrink-0 relative">
                        <div className="absolute inset-0 flex items-center justify-center text-2xl font-black text-slate-800 font-mali">{selectedChar.toLowerCase()}</div>
                        <img src={previewImageSrc || `/alphabet/${selectedChar.toLowerCase()}.png?v=${imageVersion}`} alt="Preview"
                          className="absolute inset-0 w-full h-full object-fill crisp-letter-img bg-white z-10 p-1"
                          onError={(e) => { (e.target as HTMLElement).style.display = 'none'; }} />
                      </div>
                      <label className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-black text-xs flex items-center gap-2 shadow-md cursor-pointer transition-all active:scale-95">
                        <Upload className="w-3.5 h-3.5" /> Chọn Ảnh Mới
                        <input type="file" accept="image/*" onChange={handleImageFileChange} className="hidden" />
                      </label>
                    </div>
                    {selectedImageFile && <p className="text-[10px] font-extrabold text-emerald-700">✅ Đã chọn: {selectedImageFile.name}</p>}
                  </div>
                </div>

                {/* PHẦN 2 */}
                <div className="bg-amber-50/40 border-2 border-amber-100 p-5 rounded-[24px] space-y-4">
                  <div className="flex items-center gap-2 border-b border-amber-200 pb-2">
                    <span className="text-base">🎈</span>
                    <h4 className="text-sm font-black text-amber-900">PHẦN 2: Từ VÍ DỤ MINH HỌA</h4>
                  </div>
                  <div>
                    <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">Từ Ví Dụ Minh Họa</label>
                    <input type="text" value={editExample} onChange={(e) => setEditExample(e.target.value)} placeholder="Ví dụ: Con cá, Quả bóng..."
                      className="w-full bg-white border-2 border-amber-100 rounded-xl px-3 py-2.5 text-xs font-bold text-slate-800 focus:outline-none focus:border-amber-400" />
                  </div>
                  <div className="bg-white border border-amber-200 rounded-2xl p-4 space-y-3 shadow-sm">
                    <span className="text-[11px] font-black text-amber-900 block">🎙️ Phát Âm Từ Ví Dụ (Ghi âm hoặc Chọn file)</span>
                    <div className="flex flex-col gap-2.5">
                      <div className="flex items-center gap-2 flex-wrap">
                        {!isRecordingExample ? (
                          <button type="button" onClick={startRecordingExampleAdmin}
                            className="px-3 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-500 text-white text-[11px] font-black flex items-center gap-1 shadow-sm active:scale-95 transition-all">
                            <Mic className="w-3 h-3" /> Ghi âm
                          </button>
                        ) : (
                          <button type="button" onClick={stopRecordingExampleAdmin}
                            className="px-3 py-1.5 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-[11px] font-black flex items-center gap-1 shadow-sm animate-pulse active:scale-95 transition-all">
                            <Square className="w-3 h-3" /> Lưu
                          </button>
                        )}
                        <label className="px-3 py-1.5 rounded-xl bg-white border border-amber-300 text-amber-600 hover:bg-amber-50 text-[11px] font-black flex items-center gap-1 shadow-sm cursor-pointer transition-all active:scale-95">
                          <Upload className="w-3 h-3" /> Chọn file
                          <input type="file" accept="audio/*" onChange={handleExampleAudioFileChangeAdmin} className="hidden" />
                        </label>
                        {recordedExampleAudioUrl && (
                          <button type="button" onClick={deleteRecordedExampleAudio}
                            className="p-1.5 rounded-xl bg-rose-50 hover:bg-rose-100 text-rose-600 border border-rose-200 transition-colors" title="Xóa âm thanh">
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>
                      <button type="button" onClick={playExampleAudioAdmin}
                        className="px-3 py-2 bg-amber-100 hover:bg-amber-200 text-amber-800 rounded-xl text-xs font-black flex items-center gap-1.5 active:scale-95 transition-all shadow-sm w-fit">
                        <Volume2 className="w-4 h-4" /> Nghe thử phát âm từ ví dụ
                      </button>
                      {selectedExampleAudioFile && <p className="text-[10px] font-extrabold text-amber-700">✅ Đã chọn: {selectedExampleAudioFile.name}</p>}
                      {recordedExampleBlob && !selectedExampleAudioFile && <p className="text-[10px] font-extrabold text-amber-700">🔴 Ghi âm sẵn sàng ({Math.round(recordedExampleBlob.size / 1024)} KB)</p>}
                    </div>
                  </div>
                  <div className="bg-white border border-amber-200 rounded-2xl p-4 space-y-3 shadow-sm">
                    <span className="text-[11px] font-black text-slate-700 block">🖼️ Ảnh Minh Họa Ví Dụ</span>
                    <div className="flex items-center gap-4">
                      <div className="w-16 h-16 rounded-xl border-2 border-amber-200 bg-slate-50 overflow-hidden flex items-center justify-center p-1 shrink-0 relative">
                        {previewExampleImgSrc ? (
                          <img src={previewExampleImgSrc} alt="Preview Example" className="w-full h-full object-contain" />
                        ) : (
                          <span className="text-2xl">{getExampleEmoji(editExample, selectedChar)}</span>
                        )}
                      </div>
                      <label className="px-4 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-white font-black text-xs flex items-center gap-2 shadow-md cursor-pointer transition-all active:scale-95">
                        <Upload className="w-3.5 h-3.5" /> Chọn Ảnh Mới
                        <input type="file" accept="image/*" onChange={handleExampleImgChangeAdmin} className="hidden" />
                      </label>
                    </div>
                    {selectedExampleImgFile && <p className="text-[10px] font-extrabold text-amber-700">✅ Đã chọn: {selectedExampleImgFile.name}</p>}
                  </div>
                </div>
              </div>

              <div className="flex items-center justify-between pt-6 border-t-2 border-slate-100">
                <button type="button" onClick={handleDeleteLetterAdmin}
                  className="px-4 py-2.5 rounded-xl bg-rose-50 text-rose-600 border-2 border-rose-200 hover:bg-rose-100 font-black text-xs flex items-center gap-1.5 transition-all cursor-pointer">
                  <Trash2 className="w-4 h-4" /> Xóa Chữ Cái Này
                </button>
                <div className="flex items-center gap-3">
                  <button type="button" onClick={() => setShowEditModal(false)}
                    className="px-6 py-3 rounded-xl bg-slate-100 text-slate-600 font-extrabold text-sm hover:bg-slate-200 transition-colors">
                    Hủy Bỏ
                  </button>
                  <button type="submit" disabled={uploading}
                    className="px-8 py-3 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-black text-sm shadow-lg shadow-emerald-600/30 transition-all active:scale-95 cursor-pointer disabled:opacity-50 flex items-center gap-2">
                    {uploading ? 'Đang Lưu...' : 'Lưu Thay Đổi'}
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* TOAST */}
      {toast && (
        <div className="fixed bottom-6 right-6 z-[9999] flex items-center gap-3 px-5 py-4 rounded-2xl shadow-xl border animate-fade-in bg-white border-sky-100 max-w-sm">
          <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 ${toast.type === 'success' ? 'bg-emerald-100 text-emerald-600' : 'bg-rose-100 text-rose-600'}`}>
            {toast.type === 'success' ? <Check className="w-4 h-4" /> : <AlertCircle className="w-4 h-4" />}
          </div>
          <div className="space-y-0.5">
            <h4 className="text-xs font-black text-slate-800">{toast.type === 'success' ? 'Thành công' : 'Có lỗi xảy ra'}</h4>
            <p className="text-[11px] font-bold text-slate-500">{toast.message}</p>
          </div>
          <button onClick={() => setToast(null)} className="p-1 rounded-full text-slate-400 hover:bg-slate-50 transition-colors cursor-pointer self-start"><X className="w-3.5 h-3.5" /></button>
        </div>
      )}
    </div>
  );
};

export default AdminDashboard;
