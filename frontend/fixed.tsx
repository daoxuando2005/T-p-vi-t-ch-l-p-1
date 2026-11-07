import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import api from '../../services/api';
import { 
  Shield, 
  Users, 
  BookOpen, 
  Trash2, 
  Plus, 
  Check, 
  AlertCircle,
  Search,
  Edit3,
  X,
  Upload,
  Key,
  CheckSquare,
  Square,
  UserPlus,
  Edit2,
  Eye,
  EyeOff,
  Mic,
  Volume2
} from 'lucide-react';
import { sortVietnameseAlphabet } from '../../utils/alphabetSort';
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
  exampleAudioUrl?: string;
}

const VIETNAMESE_ALPHABET = [
  'a', '�', '�¢', 'b', 'c', 'd', '��', 'e', '�ª', 'g', 'h', 'i', 'k', 'l', 'm', 
  'n', 'o', '�´', ' ¡', 'p', 'q', 'r', 's', 't', 'u', ' °', 'v', 'x', 'y'
];

const pronunciationMapAdmin: Record<string, string> = {
  'a': 'a', '�': '�¡', '�¢': '�»⬺', 'b': 'b�»', 'c': 'c�»', 'd': 'd�»', '��': '���»',
  'e': 'e', '�ª': '�ª', 'g': 'g�»', 'h': 'h�»', 'i': 'i', 'k': 'c�»', 'l': 'l�»',
  'm': 'm�»', 'n': 'n�»', 'o': 'o', '�´': '�´', ' ¡': '�»', 'p': 'p�»', 'q': 'qu�»',
  'r': 'r�»', 's': 's�»', 't': 't�»', 'u': 'u', ' °': '�»«', 'v': 'v�»', 'x': 's�»', 'y': 'y'
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
    title: '�Ÿ�S Quy�»n Giao B� i T�º­p Cho H�»c Sinh',
    description: 'C�º¥p quy�»n n� y cho Ph�»¥ Huynh (ho�º·c Gi�¡o Vi�ªn) �� °�»£c ph�©p t�º¡o v�  giao b� i t�º­p t�º­p vi�º¿t cho c�¡c b�©.',
    defaultRoles: ['ROLE_TEACHER']
  },
  {
    token: 'PERM_VIEW_ALPHABET',
    title: '�Ÿ�S� Quy�»n Xem H�»c Li�»⬡u Ch�»¯ C�¡i',
    description: 'Cho ph�©p Gi�¡o vi�ªn truy c�º­p trang h�»c li�»⬡u, nghe �¢m m�º«u ph�¡t �¢m v�  xem h °�»⬺ng d�º«n chi ti�º¿t.',
    defaultRoles: ['ROLE_TEACHER']
  },
  {
    token: 'PERM_EDIT_ALPHABET',
    title: '���¸ Quy�»n S�»­a H�»c Li�»⬡u Ch�»¯ C�¡i',
    description: 'Cho ph�©p Gi�¡o vi�ªn ch�»⬰nh s�»­a h °�»⬺ng d�º«n ���»c, ghi �¢m m�º«u tr�»±c ti�º¿p, t�º£i �º£nh minh h�»a n�©t ch�»¯ & v�­ d�»¥.',
    defaultRoles: ['ROLE_ADMIN']
  },
  {
    token: 'PERM_ADD_ALPHABET',
    title: '�⬢ Quy�»n Th�ªm & X�³a Ch�»¯ C�¡i',
    description: 'Cho ph�©p Gi�¡o vi�ªn t�º¡o th�ªm c�¡c k�½ t�»±/ch�»¯ c�¡i m�»⬺i ho�º·c x�³a b�» ch�»¯ c�¡i kh�»i danh s�¡ch.',
    defaultRoles: ['ROLE_ADMIN']
  },
  {
    token: 'PERM_MANAGE_CLASSES',
    title: '�Ÿ« Quy�»n Qu�º£n L�½ L�»⬺p H�»c & H�»c Sinh',
    description: 'Cho ph�©p t�º¡o l�»⬺p h�»c, th�ªm h�»c sinh theo t�ªn ���ng nh�º­p v�  ki�»�m so�¡t danh s�¡ch h�»c sinh.',
    defaultRoles: ['ROLE_TEACHER', 'ROLE_ADMIN']
  },
  {
    token: 'PERM_CHAT_ACCESS',
    title: '�Ÿ�"¬ Quy�»n Nh�º¯n Tin & Trao �»⬢i Realtime',
    description: 'Cho ph�©p g�»­i tin nh�º¯n trao ���»⬢i gi�»¯a Gi�¡o vi�ªn v�  Ph�»¥ huynh.',
    defaultRoles: ['ROLE_TEACHER', 'ROLE_PARENT']
  },
  {
    token: 'PERM_PARENT_TRACKING',
    title: '�Ÿ�����Ÿ⬝⬝ Quy�»n Theo D�µi Ti�º¿n �»�� & Nh�º¯c B� i B�©',
    description: 'Cho ph�©p xem l�»⬹ch giao b� i t�º­p c�»§a c�´ gi�¡o v�  b�º¥m n�ºt g�»­i th�´ng b�¡o nh�º¯c b�© l� m b� i t�º­p.',
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

  const getExampleEmoji = (word: string): string => {
    if (!word) return '�Ÿ�Ÿ';
    const wordLower = word.toLowerCase();
    if (wordLower.includes('ca')) return '�Ÿ¥⬺';
    if (wordLower.includes('m�º¯t')) return '�Ÿ���¸';
    if (wordLower.includes('n�º¥m')) return '�Ÿ⬞';
    if (wordLower.includes('b�³ng')) return '�Ÿ� ';
    if (wordLower.includes('c�¡')) return '�ŸŸ';
    if (wordLower.includes('d °a')) return '�Ÿ⬰';
    if (wordLower.includes('��u ���»§')) return '�Ÿ¥­';
    if (wordLower.includes('xe')) return '�Ÿ�';
    if (wordLower.includes('b�ª')) return '�Ÿ⬚';
    if (wordLower.includes('g� ')) return '�Ÿ⬝';
    if (wordLower.includes('hoa')) return '�Ÿ�¸';
    if (wordLower.includes('bi')) return '�Ÿ⬝®';
    if (wordLower.includes('k�©o')) return '��⬚�¸';
    if (wordLower.includes('l�¡')) return '�Ÿ�';
    if (wordLower.includes('m�­t')) return '�Ÿ';
    if (wordLower.includes('na')) return '�Ÿ� ';
    if (wordLower.includes('tr�²n')) return '�ª';
    if (wordLower.includes('�´')) return '�Ÿ�⬚';
    if (wordLower.includes('c�»')) return '�Ÿ©';
    if (wordLower.includes('c�»')) return '�Ÿ�¿';
    if (wordLower.includes('pin')) return '�Ÿ⬝⬹';
    if (wordLower.includes('qu�½t')) return '�Ÿ';
    if (wordLower.includes('r�¹a')) return '�Ÿ¢';
    if (wordLower.includes('sao')) return '�­';
    if (wordLower.includes('t� u')) return '�Ÿ¢';
    if (wordLower.includes('m&©')) return '�Ÿ���"';
    if (wordLower.includes('s ° t�»­')) return '�Ÿ¦';
    if (wordLower.includes('v�»⬹t')) return '�Ÿ¦⬠';
    if (wordLower.includes('y�º¿m')) return '�Ÿ��¶';
    return '�Ÿ�Ÿ';
  };

  // States
  const [stats, setStats] = useState<any>({
    totalUsers: 0,
    totalClasses: 0,
    totalPractices: 0,
    totalFeedbacks: 0,
    studentCount: 0,
    teacherCount: 0,
    parentCount: 0
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

  const showToast = (message: string, type: 'success' | 'error' = 'success') => {
    setToast({ message, type });
  };

  useEffect(() => {
    if (toast) {
      const timer = setTimeout(() => setToast(null), 3000);
      return () => clearTimeout(timer);
    }
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
    if (newRole === 'TEACHER') {
      setNewPermissions(['PERM_ASSIGN_HOMEWORK', 'PERM_MANAGE_CLASSES', 'PERM_VIEW_ALPHABET', 'PERM_CHAT_ACCESS']);
    } else if (newRole === 'PARENT') {
      setNewPermissions(['PERM_PARENT_TRACKING', 'PERM_CHAT_ACCESS']);
    } else {
      setNewPermissions([]);
    }
  }, [newRole]);

  const fetchStats = async () => {
    try {
      const res = await api.get('/admin/stats');
      setStats(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchUsers = async () => {
    try {
      const res = await api.get('/admin/users');
      setUsers(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchLessons = async () => {
    try {
      const res = await api.get('/student/alphabet');
      setLessons(sortVietnameseAlphabet(res.data));
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => {
    const initData = async () => {
      setLoading(true);
      await fetchStats();
      setLoading(false);
    };
    initData();
  }, []);

  useEffect(() => {
    if (activeTab === 'users') fetchUsers();
    if (activeTab === 'alphabet') fetchLessons();
    if (activeTab === 'stats') fetchStats();
  }, [activeTab]);

  const handleToggleUser = async (userId: number) => {
    setUsers(prev => prev.map(u => u.id === userId ? { ...u, enabled: !u.enabled } : u));
    try {
      const res = await api.put(`/admin/users/${userId}/toggle`);
      showToast(res.data.message || 'Thay ���»⬢i tr�º¡ng th�¡i t� i kho�º£n th� nh c�´ng!');
    } catch (err: any) {
      setUsers(prev => prev.map(u => u.id === userId ? { ...u, enabled: !u.enabled } : u));
      showToast(err.response?.data?.message || 'C�³ l�»�i x�º£y ra khi ���»⬢i tr�º¡ng th�¡i', 'error');
    }
  };

  const handleChangeRole = async (userId: number, newRole: string) => {
    try {
      const res = await api.put(`/admin/users/${userId}/role?role=${newRole}`);
      showToast(res.data.message || 'Thay ���»⬢i vai tr�² t� i kho�º£n th� nh c�´ng!');
      fetchUsers();
    } catch (err: any) {
      showToast(err.response?.data?.message || 'Kh�´ng th�»� ���»⬢i vai tr�²', 'error');
    }
  };

  const handleDeleteUser = async (userId: number) => {
    if (!window.confirm('B�º¡n c�³ ch�º¯c ch�º¯n mu�»��n x�³a v©nh vi�»⬦n t� i kho�º£n ng °�»i d�¹ng n� y kh�´ng?')) return;
    try {
      const res = await api.delete(`/admin/users/${userId}`);
      showToast(res.data.message || 'X�³a t� i kho�º£n th� nh c�´ng!');
      fetchUsers();
    } catch (err: any) {
      showToast(err.response?.data?.message || 'C�³ l�»�i x�º£y ra', 'error');
    }
  };

  const handleOpenPermissionModal = (u: UserProfile) => {
    setSelectedUser(u);
    const existingRoles = u.roles || [];
    setUserPermissions([...existingRoles]);
    setShowPermissionModal(true);
  };

  const handleSingleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newUsername || !newPassword || !newFullName || !newEmail || !newRole) {
      showToast('Vui l�²ng ��i�»n ���º§y ���»§ c�¡c th�´ng tin b�º¯t bu�»��c', 'error');
      return;
    }
    setSubmittingAddUser(true);
    try {
      const payload = {
        username: newUsername,
        password: newPassword,
        fullName: newFullName,
        email: newEmail,
        role: newRole,
        permissions: newRole !== 'STUDENT' && newRole !== 'ADMIN' ? newPermissions : []
      };
      const res = await api.post('/admin/users', payload);
      showToast(res.data.message || 'T�º¡o t� i kho�º£n th� nh c�´ng!', 'success');
      setNewUsername('');
      setNewPassword('');
      setNewFullName('');
      setNewEmail('');
      setNewRole('TEACHER');
      setShowAddUserModal(false);
      fetchUsers();
    } catch (err: any) {
      showToast(err.response?.data?.message || 'Kh�´ng th�»� t�º¡o t� i kho�º£n', 'error');
    } finally {
      setSubmittingAddUser(false);
    }
  };

  const handleOpenEditUserModal = (user: UserProfile) => {
    setEditingUser(user);
    setEditFullName(user.fullName || '');
    setEditEmail(user.email || '');
    setEditNewPassword('');
    setShowEditUserModal(true);
  };

  const handleSaveEditUser = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingUser) return;
    if (!editFullName.trim() || !editEmail.trim()) {
      showToast('H�» t�ªn v�  Email kh�´ng �� °�»£c ���»� tr�»��ng', 'error');
      return;
    }
    setSavingEditUser(true);
    try {
      const payload: any = {
        fullName: editFullName.trim(),
        email: editEmail.trim(),
      };
      if (editNewPassword && editNewPassword.length >= 6) {
        payload.newPassword = editNewPassword;
      } else if (editNewPassword && editNewPassword.length > 0) {
        showToast('M�º­t kh�º©u m�»⬺i ph�º£i c�³ �­t nh�º¥t 6 k�½ t�»±', 'error');
        setSavingEditUser(false);
        return;
      }
      await api.put(`/admin/users/${editingUser.id}/info`, payload);
      showToast('C�º­p nh�º­t th�´ng tin t� i kho�º£n th� nh c�´ng!', 'success');
      setShowEditUserModal(false);
      setEditingUser(null);
      fetchUsers();
    } catch (err: any) {
      showToast(err.response?.data?.message || 'Kh�´ng th�»� c�º­p nh�º­t th�´ng tin', 'error');
    } finally {
      setSavingEditUser(false);
    }
  };

  const togglePermissionToken = (token: string) => {
    setUserPermissions(prev => 
      prev.includes(token)
        ? prev.filter(p => p !== token)
        : [...prev, token]
    );
  };

  const handleSavePermissionsModal = async () => {
    if (!selectedUser) return;
    setSavingPermissions(true);
    try {
      const selectedPermsOnly = userPermissions.filter(p => p.startsWith('PERM_'));
      const res = await api.put(`/admin/users/${selectedUser.id}/permissions`, selectedPermsOnly);
      showToast(res.data.message || 'C�º­p nh�º­t b�º£ng ph�¢n quy�»n th� nh c�´ng!', 'success');
      setShowPermissionModal(false);
      fetchUsers();
    } catch (err: any) {
      showToast(err.response?.data?.message || 'C�³ l�»�i x�º£y ra khi l °u ph�¢n quy�»n', 'error');
    } finally {
      setSavingPermissions(false);
    }
  };

  const handleOpenEditModal = (char: string, isAdding: boolean = false) => {
    const foundLesson = lessons.find(l => l.letter.toLowerCase() === char.toLowerCase());
    setSelectedChar(char);
    setIsAddingLetter(isAdding);
    setEditGuide(foundLesson?.pronunciationGuide || (char ? `Ph�¡t �¢m �¢m ${char}` : ''));
    setEditExample(foundLesson?.exampleWord || (char ? `V�­ d�»¥ ch�»¯ ${char}` : ''));
    setSelectedImageFile(null);
    setPreviewImageSrc(null);
    setSelectedAudioFile(null);
    setRecordedAudioUrl(foundLesson?.audioUrl || null);
    setRecordedBlob(null);
    setIsRecording(false);
    setSelectedExampleAudioFile(null);
    setRecordedExampleAudioUrl(foundLesson?.exampleAudioUrl || null);
    setRecordedExampleBlob(null);
    setIsRecordingExample(false);
    setSelectedExampleImgFile(null);
    setPreviewExampleImgSrc(foundLesson?.imageUrl || null);
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
        setRecordedBlob(blob);
        setRecordedAudioUrl(URL.createObjectURL(blob));
        stream.getTracks().forEach(t => t.stop());
      };
      recorder.start();
      setMediaRecorder(recorder);
      setIsRecording(true);
    } catch (err) { showToast('Kh�´ng th�»� truy c�º­p Microphone: ' + err, 'error'); }
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
        setRecordedExampleBlob(blob);
        setRecordedExampleAudioUrl(URL.createObjectURL(blob));
        stream.getTracks().forEach(t => t.stop());
      };
      recorder.start();
      setMediaRecorder(recorder);
      setIsRecordingExample(true);
    } catch (err) { showToast('Kh�´ng th�»� truy c�º­p Microphone: ' + err, 'error'); }
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
    if (recordedAudioUrl) { const audio = new Audio(recordedAudioUrl); audio.play().catch(() => speakLetterTTSAdmin(selectedChar)); }
    else { speakLetterTTSAdmin(selectedChar); }
  };
  const playExampleAudioAdmin = () => {
    if (recordedExampleAudioUrl) { const audio = new Audio(recordedExampleAudioUrl); audio.play().catch(() => speakWordTTSAdmin(editExample || 'T�»« v�­ d�»¥')); }
    else { speakWordTTSAdmin(editExample || 'T�»« v�­ d�»¥'); }
  };

  const handleImageFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const file = e.target.files[0];
      setSelectedImageFile(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreviewImageSrc(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleSaveLetterModal = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedChar.trim()) {
      showToast('Vui l�²ng nh�º­p t�ªn ch�»¯ c�¡i!', 'error');
      return;
    }
    setUploading(true);
    const rawChar = selectedChar.toLowerCase().trim();
    const letterImageName = `${rawChar}.png`;

    try {
      const foundLesson = lessons.find(l => l.letter.toLowerCase() === rawChar);

      let finalAudioUrl = foundLesson?.audioUrl || '';
      let finalImageUrl = foundLesson?.imageUrl || '';
      let finalExampleAudioUrl = foundLesson?.exampleAudioUrl || '';

      if (selectedAudioFile) {
        const formData = new FormData();
        formData.append('file', selectedAudioFile);
        formData.append('filename', `${rawChar}_audio_${Date.now()}.${selectedAudioFile.name.split('.').pop()}`);
        const uploadRes = await api.post('/admin/alphabet/upload-audio', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });
        if (uploadRes.data.success) finalAudioUrl = uploadRes.data.message;
      } else if (recordedBlob) {
        const audioFile = new File([recordedBlob], `${rawChar}_audio_${Date.now()}.webm`, { type: 'audio/webm' });
        const formData = new FormData();
        formData.append('file', audioFile);
        formData.append('filename', `${rawChar}_audio_${Date.now()}.webm`);
        const uploadRes = await api.post('/admin/alphabet/upload-audio', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });
        if (uploadRes.data.success) finalAudioUrl = uploadRes.data.message;
      }

      if (selectedExampleAudioFile) {
        const formData = new FormData();
        formData.append('file', selectedExampleAudioFile);
        formData.append('filename', `${rawChar}_example_audio_${Date.now()}.${selectedExampleAudioFile.name.split('.').pop()}`);
        const uploadRes = await api.post('/admin/alphabet/upload-audio', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });
        if (uploadRes.data.success) finalExampleAudioUrl = uploadRes.data.message;
      } else if (recordedExampleBlob) {
        const audioFile = new File([recordedExampleBlob], `${rawChar}_example_audio_${Date.now()}.webm`, { type: 'audio/webm' });
        const formData = new FormData();
        formData.append('file', audioFile);
        formData.append('filename', `${rawChar}_example_audio_${Date.now()}.webm`);
        const uploadRes = await api.post('/admin/alphabet/upload-audio', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });
        if (uploadRes.data.success) finalExampleAudioUrl = uploadRes.data.message;
      }

      if (selectedExampleImgFile) {
        const formData = new FormData();
        formData.append('file', selectedExampleImgFile);
        formData.append('filename', `${rawChar}_example_${Date.now()}.${selectedExampleImgFile.name.split('.').pop()}`);
        const uploadRes = await api.post('/admin/alphabet/upload-example-image', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });
        if (uploadRes.data.success) finalImageUrl = uploadRes.data.message;
      }

      if (selectedImageFile) {
        const formData = new FormData();
        formData.append('file', selectedImageFile);
        formData.append('filename', letterImageName);
        await api.post('/admin/alphabet/upload-image', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });
      }

      const payload = {
        ...(foundLesson ? { id: foundLesson.id } : {}),
        letter: rawChar,
        pronunciationGuide: editGuide,
        exampleWord: editExample,
        audioUrl: finalAudioUrl,
        imageUrl: finalImageUrl,
        exampleAudioUrl: finalExampleAudioUrl,
          isHidden: editIsHidden
      };

      await api.post('/admin/alphabet', payload);
      showToast('C�º­p nh�º­t h�»c li�»⬡u ch�»¯ c�¡i th� nh c�´ng!', 'success');

      setShowEditModal(false);
      setImageVersion(Date.now());
      fetchLessons();
    } catch (err: any) {
      showToast(err.response?.data?.message || 'C�³ l�»�i x�º£y ra khi l °u ch�»¯ c�¡i', 'error');
    } finally {
      setUploading(false);
    }
  };

  const handleDeleteLetterAdmin = async () => {
    const rawChar = selectedChar.toLowerCase().trim();
    if (!window.confirm(`B�º¡n c�³ ch�º¯c ch�º¯n mu�»��n X��SA ch�»¯ c�¡i "${rawChar}" kh�´ng? Thao t�¡c n� y s�º½ x�³a m�»i h�»c li�»⬡u li�ªn quan.`)) return;
    
    const foundLesson = lessons.find(l => l.letter.toLowerCase() === rawChar);
    if (foundLesson && foundLesson.id) {
      try {
        const res = await api.delete(`/admin/alphabet/${foundLesson.id}`);
        showToast(res.data.message || 'X�³a ch�»¯ c�¡i th� nh c�´ng!');
        fetchLessons();
      } catch (err: any) {
        showToast(err.response?.data?.message || 'X�³a ch�»¯ c�¡i th�º¥t b�º¡i!', 'error');
      }
    } else {
      showToast('Kh�´ng t�¬m th�º¥y ID c�»§a ch�»¯ c�¡i n� y trong h�»⬡ th�»��ng!', 'error');
    }
    setShowEditModal(false);
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-3">
        <div className="w-12 h-12 border-4 border-sky-200 border-t-sky-600 rounded-full animate-spin"></div>
        <p className="text-sm font-bold text-sky-800">ang t�º£i d�»¯ li�»⬡u qu�º£n tr�»⬹ vi�ªn...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-12">
      <div className="bg-white border-2 border-sky-100 p-2 rounded-2xl shadow-sm flex flex-wrap gap-2">
        {[
          { key: 'stats', label: 'Th�»��ng k�ª t�»⬢ng quan', icon: Shield, color: 'text-sky-600' },
          { key: 'users', label: 'Qu�º£n l�½ ng °�»i d�¹ng & Ph�¢n Quy�»n Chi Ti�º¿t', icon: Users, color: 'text-indigo-600' },
          { key: 'alphabet', label: 'Qu�º£n l�½ h�»c li�»⬡u (Th�ªm / S�»­a ch�»¯ & T�º£i �º£nh)', icon: BookOpen, color: 'text-emerald-600' },
        ].map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.key;
          return (
            <button
              key={tab.key}
              onClick={() => handleTabChange(tab.key as any)}
              className={`flex items-center gap-2.5 px-5 py-3 rounded-xl font-extrabold text-xs transition-all cursor-pointer ${
                isActive 
                  ? 'bg-sky-600 text-white shadow-md shadow-sky-600/30 scale-[1.02]' 
                  : 'bg-transparent text-slate-600 hover:bg-sky-50 hover:text-sky-700'
              }`}
            >
              <Icon className={`w-4 h-4 ${isActive ? 'text-white' : tab.color}`} />
              {tab.label}
            </button>
          );
        })}
      </div>

      {activeTab === 'stats' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
            <div className="bg-white border-2 border-sky-100 p-6 rounded-3xl shadow-sm space-y-2">
              <span className="text-xs font-black uppercase text-slate-500 tracking-wider">T�»⬢ng t� i kho�º£n</span>
              <p className="text-4xl font-black text-sky-700">{stats.totalUsers || 0}</p>
              <p className="text-[11px] font-bold text-slate-400">�£ ���ng k�½ h�»⬡ th�»��ng</p>
            </div>
            <div className="bg-white border-2 border-indigo-100 p-6 rounded-3xl shadow-sm space-y-2">
              <span className="text-xs font-black uppercase text-slate-500 tracking-wider">T�»⬢ng l�»⬺p h�»c</span>
              <p className="text-4xl font-black text-indigo-600">{stats.totalClasses || 0}</p>
              <p className="text-[11px] font-bold text-slate-400">L�»⬺p h�»c ��ang t�º¡o</p>
            </div>
            <div className="bg-white border-2 border-emerald-100 p-6 rounded-3xl shadow-sm space-y-2">
              <span className="text-xs font-black uppercase text-slate-500 tracking-wider">L °�»£t vi�º¿t ch�»¯</span>
              <p className="text-4xl font-black text-emerald-600">{stats.totalPractices || 0}</p>
              <p className="text-[11px] font-bold text-slate-400">B� i t�º­p b�© ���£ ho� n th� nh</p>
            </div>
            <div className="bg-white border-2 border-rose-100 p-6 rounded-3xl shadow-sm space-y-2">
              <span className="text-xs font-black uppercase text-slate-500 tracking-wider">G�³p �½ m�»Ÿ</span>
              <p className="text-4xl font-black text-rose-500">{stats.totalFeedbacks || 0}</p>
              <p className="text-[11px] font-bold text-slate-400">C�º§n admin ph�º£n h�»�Si</p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            <div className="bg-gradient-to-br from-amber-50 to-orange-50 border-2 border-amber-200 p-6 rounded-3xl space-y-2 text-center">
              <span className="text-xs font-black text-amber-800 uppercase tracking-widest">H�»c sinh (Student)</span>
              <p className="text-3xl font-black text-amber-600">{stats.studentCount || 0} b�©</p>
            </div>
            <div className="bg-gradient-to-br from-sky-50 to-blue-50 border-2 border-sky-200 p-6 rounded-3xl space-y-2 text-center">
              <span className="text-xs font-black text-sky-800 uppercase tracking-widest">Gi�¡o vi�ªn (Teacher)</span>
              <p className="text-3xl font-black text-sky-600">{stats.teacherCount || 0} c�´</p>
            </div>
            <div className="bg-gradient-to-br from-pink-50 to-rose-50 border-2 border-pink-200 p-6 rounded-3xl space-y-2 text-center">
              <span className="text-xs font-black text-pink-800 uppercase tracking-widest">Ph�»¥ huynh (Parent)</span>
              <p className="text-3xl font-black text-pink-600">{stats.parentCount || 0} ng °�»i</p>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="bg-white border-2 border-sky-100 rounded-3xl p-6 shadow-sm">
              <h3 className="text-sm font-black text-slate-700 mb-1">�Ÿ�S Ph�¢n b�»⬢ ng °�»i d�¹ng</h3>
              <p className="text-[11px] font-bold text-slate-400 mb-2">T�»· l�»⬡ c�¡c vai tr�² trong h�»⬡ th�»��ng</p>
              {(stats.studentCount || 0) + (stats.teacherCount || 0) + (stats.parentCount || 0) === 0 ? (
                <div className="flex items-center justify-center h-[220px] text-slate-300">
                  <div className="text-center">
                    <div className="text-5xl mb-2">�Ÿ�S�</div>
                    <p className="text-xs font-bold">Ch °a c�³ ng °�»i d�¹ng n� o</p>
                  </div>
                </div>
              ) : (
                <ResponsiveContainer width="100%" height={240}>
                  <PieChart>
                    <Pie
                      data={[
                        { name: 'H�»c sinh', value: stats.studentCount || 0 },
                        { name: 'Gi�¡o vi�ªn', value: stats.teacherCount || 0 },
                        { name: 'Ph�»¥ huynh', value: stats.parentCount || 0 },
                      ]}
                      cx="50%"
                      cy="50%"
                      innerRadius={60}
                      outerRadius={90}
                      paddingAngle={5}
                      dataKey="value"
                    >
                      <Cell fill="#f59e0b" />
                      <Cell fill="#0ea5e9" />
                      <Cell fill="#ec4899" />
                    </Pie>
                    <Tooltip
                      contentStyle={{ borderRadius: '12px', border: '2px solid #e0f2fe', fontSize: '12px', fontWeight: 700 }}
                      formatter={(value: number, name: string) => [`${value} ng °�»i`, name]}
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

            <div className="bg-white border-2 border-indigo-100 rounded-3xl p-6 shadow-sm">
              <h3 className="text-sm font-black text-slate-700 mb-1">�Ÿ�S�  T�»⬢ng quan h�»⬡ th�»��ng</h3>
              <p className="text-[11px] font-bold text-slate-400 mb-5">C�¡c ch�»⬰ s�»�� to� n h�»⬡ th�»��ng hi�»⬡n t�º¡i</p>
              <ResponsiveContainer width="100%" height={240}>
                <BarChart
                  data={[
                    { name: 'T� i kho�º£n', value: stats.totalUsers || 0 },
                    { name: 'L�»⬺p h�»c', value: stats.totalClasses || 0 },
                    { name: 'L °�»£t vi�º¿t', value: stats.totalPractices || 0 },
                    { name: 'G�³p �½', value: stats.totalFeedbacks || 0 },
                  ]}
                  margin={{ top: 5, right: 10, left: -20, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                  <XAxis dataKey="name" tick={{ fontSize: 11, fontWeight: 700, fill: '#64748b' }} />
                  <YAxis tick={{ fontSize: 11, fill: '#94a3b8' }} allowDecimals={false} />
                  <Tooltip
                    contentStyle={{ borderRadius: '12px', border: '2px solid #e8e8ff', fontSize: '12px', fontWeight: 700 }}
                    cursor={{ fill: '#f5f3ff' }}
                  />
                  <Bar dataKey="value" radius={[8, 8, 0, 0]}>
                    {[
                      { fill: '#6366f1' },
                      { fill: '#0ea5e9' },
                      { fill: '#10b981' },
                      { fill: '#f43f5e' },
                    ].map((entry, index) => (
                      <Cell key={`cell-admin-${index}`} fill={entry.fill} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'users' && (
        <div className="bg-white border-2 border-sky-100 rounded-3xl p-6 shadow-sm space-y-5">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b-2 border-sky-50">
            <div>
              <h2 className="text-xl font-black text-slate-800">Danh S�¡ch T� i Kho�º£n & B�»�� L�»c Ph�¢n Lo�º¡i 4 �»��i T °�»£ng ({users.length})</h2>
              <p className="text-xs font-bold text-slate-500">
                L�»c danh s�¡ch theo t�»«ng ���»��i t °�»£ng (H�»c sinh, Gi�¡o vi�ªn, Ph�»¥ huynh, Admin) v�  qu�º£n l�½ c�¡c quy�»n ri�ªng bi�»⬡t.
              </p>
            </div>
            <button
              onClick={() => setShowAddUserModal(true)}
              className="px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-black text-xs flex items-center gap-2 shadow-md hover:shadow-indigo-500/20 cursor-pointer transition-all active:scale-95 shrink-0"
            >
              <UserPlus className="w-4 h-4" /> Th�ªm Ng °�»i D�¹ng M�»⬺i
            </button>
          </div>

          <div className="flex flex-wrap items-center gap-2 p-1.5 bg-slate-100/80 border-2 border-slate-200 rounded-2xl">
            {[
              { key: 'ALL', label: 'T�º¥t C�º£ T� i Kho�º£n', count: users.length },
              { key: 'ROLE_STUDENT', label: 'H�»c Sinh (Student)', count: users.filter(u => u.roles && u.roles.includes('ROLE_STUDENT')).length },
              { key: 'ROLE_TEACHER', label: 'Gi�¡o Vi�ªn (Teacher)', count: users.filter(u => u.roles && u.roles.includes('ROLE_TEACHER')).length },
              { key: 'ROLE_PARENT', label: 'Ph�»¥ Huynh (Parent)', count: users.filter(u => u.roles && u.roles.includes('ROLE_PARENT')).length },
              { key: 'ROLE_ADMIN', label: 'Qu�º£n Tr�»⬹ Vi�ªn (Admin)', count: users.filter(u => u.roles && u.roles.includes('ROLE_ADMIN')).length },
            ].map((f) => (
              <button
                key={f.key}
                onClick={() => setRoleFilter(f.key as any)}
                className={`px-4 py-2 rounded-xl font-black text-xs transition-all cursor-pointer flex items-center gap-1.5 ${
                  roleFilter === f.key
                    ? 'bg-indigo-600 text-white shadow-md scale-[1.02]'
                    : 'bg-white text-slate-700 hover:bg-indigo-50 hover:text-indigo-700 border border-slate-200'
                }`}
              >
                <span>{f.label}</span>
                <span className={`px-2 py-0.5 rounded-full text-[10px] font-black ${
                  roleFilter === f.key ? 'bg-white/20 text-white' : 'bg-slate-200 text-slate-700'
                }`}>
                  {f.count}
                </span>
              </button>
            ))}
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-slate-100/80 text-slate-700 font-black uppercase tracking-wider rounded-xl">
                  <th className="py-3.5 px-4 rounded-l-xl">H�» v�  T�ªn</th>
                  <th className="py-3.5 px-4">T�ªn �ng Nh�º­p</th>
                  <th className="py-3.5 px-4">�»��i T °�»£ng (Vai Tr�²)</th>
                  <th className="py-3.5 px-4">Quy�»n Ri�ªng  °�»£c C�º¥p</th>
                  <th className="py-3.5 px-4">Tr�º¡ng Th�¡i</th>
                  <th className="py-3.5 px-4 text-center rounded-r-xl">H� nh �»��ng</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium">
                {users
                  .filter(u => roleFilter === 'ALL' || (u.roles && u.roles.includes(roleFilter)))
                  .map((u) => {
                    const primaryRole = (u.roles || []).find(r => ['ROLE_STUDENT', 'ROLE_TEACHER', 'ROLE_PARENT', 'ROLE_ADMIN'].includes(r)) || 'ROLE_STUDENT';
                    const activePerms = u.roles || [];
                    const permBadges = [
                      { token: 'PERM_ASSIGN_HOMEWORK', label: 'Giao B� i T�º­p', color: 'bg-amber-100 text-amber-900 border-amber-300' },
                      { token: 'PERM_MANAGE_ALPHABET', label: 'Qu�º£n L�½ H�»c Li�»⬡u', color: 'bg-emerald-100 text-emerald-900 border-emerald-300' },
                      { token: 'PERM_MANAGE_CLASSES', label: 'Qu�º£n L�½ L�»⬺p', color: 'bg-blue-100 text-blue-900 border-blue-300' },
                      { token: 'PERM_CHAT_ACCESS', label: 'Chat Realtime', color: 'bg-purple-100 text-purple-900 border-purple-300' },
                      { token: 'PERM_PARENT_TRACKING', label: 'Theo D�µi Con', color: 'bg-pink-100 text-pink-900 border-pink-300' },
                    ].filter(p => activePerms.includes(p.token));

                    return (
                      <tr key={u.id} className="hover:bg-sky-50/50 transition-colors">
                        <td className="py-4 px-4 font-extrabold text-slate-900">{u.fullName}</td>
                        <td className="py-4 px-4 font-bold text-sky-700">{u.username}</td>
                        <td className="py-4 px-4">
                          <select
                            value={primaryRole}
                            onChange={(e) => handleChangeRole(u.id, e.target.value)}
                            className="bg-sky-50 border-2 border-sky-200 text-sky-900 font-extrabold text-xs rounded-xl px-3 py-1.5 focus:outline-none focus:border-sky-500 cursor-pointer shadow-sm"
                          >
                            <option value="ROLE_STUDENT">H�»c sinh (STUDENT)</option>
                            <option value="ROLE_TEACHER">Gi�¡o vi�ªn (TEACHER)</option>
                            <option value="ROLE_PARENT">Ph�»¥ huynh (PARENT)</option>
                            <option value="ROLE_ADMIN">Qu�º£n tr�»⬹ vi�ªn (ADMIN)</option>
                          </select>
                        </td>
                        <td className="py-4 px-4">
                          <div className="flex flex-wrap gap-1.5 max-w-xs">
                            {primaryRole === 'ROLE_STUDENT' ? (
                              <span className="text-[11px] font-black text-amber-800 bg-amber-50 px-2.5 py-1 rounded-lg border border-amber-200">
                                Quy�»n h�»c t�º­p m�º·c ���»⬹nh
                              </span>
                            ) : permBadges.length > 0 ? (
                              permBadges.map(b => (
                                <span key={b.token} className={`px-2.5 py-1 rounded-lg font-black text-[10px] border shadow-xs ${b.color}`}>
                                  {b.label}
                                </span>
                              ))
                            ) : (
                              <span className="text-[11px] text-slate-400 font-bold italic">Quy�»n c ¡ b�º£n</span>
                            )}
                          </div>
                        </td>
                        <td className="py-4 px-4">
                          <div className="flex items-center gap-2.5">
                            <button
                              type="button"
                              onClick={() => handleToggleUser(u.id)}
                              className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                                u.enabled ? 'bg-emerald-500 shadow-sm shadow-emerald-500/30' : 'bg-slate-300'
                              }`}
                              title={u.enabled ? 'ang ho�º¡t ���»��ng - B�º¥m g�º¡t ���»� Kh�³a t� i kho�º£n' : 'ang b�»⬹ kh�³a - B�º¥m g�º¡t ���»� M�»Ÿ t� i kho�º£n'}
                            >
                              <span
                                className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-md ring-0 transition duration-200 ease-in-out ${
                                  u.enabled ? 'translate-x-5' : 'translate-x-0'
                                }`}
                              />
                            </button>
                            <span className={`text-[11px] font-black ${u.enabled ? 'text-emerald-700' : 'text-rose-600'}`}>
                              {u.enabled ? 'Ho�º¡t �»��ng' : 'ang kh�³a'}
                            </span>
                          </div>
                        </td>
                        <td className="py-4 px-4 text-center">
                          <div className="inline-flex gap-2">
                            {primaryRole !== 'ROLE_STUDENT' ? (
                              <button
                                onClick={() => handleOpenPermissionModal(u)}
                                className="px-3 py-1.5 rounded-xl bg-indigo-50 text-indigo-700 border-2 border-indigo-200 hover:bg-indigo-100 font-black text-xs transition-all cursor-pointer flex items-center gap-1 shadow-sm"
                                title="Qu�º£n l�½ chi ti�º¿t c�¡c quy�»n h� nh ���»��ng"
                              >
                                <Key className="w-3.5 h-3.5" /> Quy�»n Chi Ti�º¿t
                              </button>
                            ) : (
                              <button
                                disabled
                                className="px-3 py-1.5 rounded-xl bg-slate-100 text-slate-400 border-2 border-slate-200 font-extrabold text-xs cursor-not-allowed flex items-center gap-1"
                                title="T� i kho�º£n H�»c sinh kh�´ng �� °�»£c ph�¢n th�ªm quy�»n"
                              >
                                Kh�´ng ph�¢n quy�»n
                              </button>
                            )}
                            <button
                              onClick={() => handleOpenEditUserModal(u)}
                              className="p-1.5 text-sky-600 bg-sky-50 border-2 border-sky-200 hover:bg-sky-100 rounded-xl transition-all cursor-pointer"
                              title="Ch�»⬰nh s�»­a th�´ng tin t� i kho�º£n"
                            >
                              <Edit2 className="w-4 h-4" />
                            </button>
                            <button
                              onClick={() => handleDeleteUser(u.id)}
                              className="p-1.5 text-rose-600 bg-rose-50 border-2 border-rose-200 hover:bg-rose-100 rounded-xl transition-all cursor-pointer"
                              title="X�³a v©nh vi�»⬦n"
                            >
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

      {activeTab === 'alphabet' && (
        <div className="space-y-6">
          <div className="bg-white border-2 border-sky-100 rounded-3xl p-6 sm:p-8 shadow-sm space-y-6">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b-2 border-sky-50">
              <div>
                <h2 className="text-xl sm:text-2xl font-black text-slate-800 flex items-center gap-2">
                  <BookOpen className="w-7 h-7 text-emerald-600" /> Qu�º£n L�½ H�»c Li�»⬡u & Ch�»⬰nh S�»­a Ch�»¯ C�¡i
                </h2>
                <p className="text-xs font-bold text-slate-500 mt-1">
                  B�º¥m v� o b�º¥t k�»³ �´ ch�»¯ n� o b�ªn d °�»⬺i ���»� <strong>Ch�»⬰nh s�»­a th�´ng tin & T�º£i/Thay t�»⬡p �º£nh, �¢m thanh m�º«u</strong> l�ªn h�»⬡ th�»��ng.
                </p>
              </div>
              <button
                onClick={() => handleOpenEditModal('', true)}
                className="px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-black text-xs flex items-center gap-2 shadow-md transition-all active:scale-95 cursor-pointer shrink-0"
              >
                <Plus className="w-4 h-4" /> Th�ªm Ch�»¯ C�¡i M�»⬺i
              </button>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-5 pt-2">
              {Array.from(new Set([...VIETNAMESE_ALPHABET, ...lessons.map(l => l.letter.toLowerCase())])).map((char) => {
                const displayChar = char.length === 1 ? `${char.toUpperCase()}${char.toLowerCase()}` : char.toUpperCase();
                const fileName = `${char.toLowerCase()}.png`;
                const imgUrl = `/alphabet/${fileName}?v=${imageVersion}`;
                const foundLesson = lessons.find(l => l.letter.toLowerCase() === char.toLowerCase());
                const emoji = foundLesson ? getExampleEmoji(foundLesson.exampleWord) : '�Ÿ�Ÿ';
                return (
                  <div
                    key={char}
                    onClick={() => handleOpenEditModal(char)}
                    className="bg-white border-2 border-sky-200 hover:border-emerald-500 rounded-2xl p-3 shadow-sm hover:shadow-xl transition-all flex flex-col items-center justify-between text-center space-y-3 relative group cursor-pointer"
                  >
                    <div className="w-full flex items-center justify-between text-[10px] font-black text-slate-500 border-b border-sky-100 pb-1.5">
                      <span className="uppercase">H�»c Li�»⬡u</span>
                      <span className="font-mono text-emerald-700 font-bold">{fileName}</span>
                    </div>
                    <div className="relative w-24 h-24 rounded-xl border-2 border-sky-300 overflow-hidden bg-white flex items-center justify-center p-1 shadow-inner group-hover:border-emerald-500 transition-colors">
                      <div className="absolute inset-0 flex flex-col items-center justify-center bg-sky-50/20 group-hover:bg-emerald-50/20 transition-colors">
                        <div className="relative w-16 h-16 flex items-center justify-center">
                          <div className="absolute inset-0 flex flex-col items-center justify-center">
                            <span className="text-2xl leading-none">{emoji}</span>
                            <span className="text-xs font-black text-slate-800 font-mali leading-none mt-0.5">{displayChar}</span>
                          </div>
                          {foundLesson?.imageUrl && (
                            <img
                              src={foundLesson.imageUrl.startsWith('http') || foundLesson.imageUrl.startsWith('/') ? foundLesson.imageUrl : `/example_images/${foundLesson.imageUrl}`}
                              alt={foundLesson.exampleWord}
                              className="absolute inset-0 w-full h-full object-contain rounded-lg border border-slate-100 bg-white z-10"
                              onError={(e) => { (e.target as HTMLElement).style.display = 'none'; }}
                            />
                          )}
                        </div>
                      </div>
                      <img src={imgUrl} alt={displayChar}
                        className="absolute inset-0 w-full h-full object-fill crisp-letter-img pointer-events-none rounded-lg z-20 bg-white"
                        onError={(e) => { (e.target as HTMLElement).style.display = 'none'; }}
                      />
                    </div>
                    <div className="w-full pt-1 border-t border-sky-50 flex items-center justify-center gap-1.5 text-xs font-black text-sky-700 group-hover:text-emerald-600">
                      <Edit3 className="w-3.5 h-3.5" />
                      <span>S�»­a ch�»¯ {displayChar}</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {showPermissionModal && selectedUser && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 animate-fade-in">
          <div className="bg-white border-4 border-indigo-200 rounded-[32px] p-6 sm:p-8 max-w-xl w-full shadow-2xl space-y-6 relative max-h-[90vh] overflow-y-auto">
            <button
              onClick={() => setShowPermissionModal(false)}
              className="absolute top-5 right-5 p-2 rounded-full bg-slate-100 text-slate-500 hover:bg-rose-100 hover:text-rose-600 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="flex items-center gap-3 border-b-2 border-indigo-50 pb-4">
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-500 to-purple-600 flex items-center justify-center text-white font-black text-xl shadow-md">
                <Key className="w-6 h-6 text-white" />
              </div>
              <div>
                <h3 className="text-lg font-black text-slate-800">
                  B�º£ng Ph�¢n Quy�»n Chi Ti�º¿t: {selectedUser.fullName}
                </h3>
                <p className="text-xs font-bold text-indigo-600">Username: <span className="font-mono">{selectedUser.username}</span> | Email: {selectedUser.email || '--'}</p>
              </div>
            </div>

            <div className="space-y-4">
              <p className="text-xs font-extrabold text-slate-600 bg-indigo-50 p-3 rounded-xl border border-indigo-100">
                Ch�»n c�¡c quy�»n h� nh ���»��ng c�»¥ th�»� b�ªn d °�»⬺i ���»� <strong>C�º¥p (Th�ªm)</strong> ho�º·c <strong>T�º¯t (Kh�³a)</strong> cho t� i kho�º£n n� y. V�­ d�»¥: b�º¡n c�³ th�»� c�º¥p quy�»n Giao b� i t�º­p cho Ph�»¥ Huynh ho�º·c c�º¥p quy�»n Qu�º£n l�½ h�»c li�»⬡u cho Gi�¡o Vi�ªn!
              </p>

              <div className="space-y-3">
                {selectedUser.roles.includes('ROLE_STUDENT') && (
                  <div className="text-center py-8 text-slate-400 font-extrabold text-xs">
                    T� i kho�º£n H�»c sinh kh�´ng c�³ quy�»n h� nh ���»��ng chi ti�º¿t n� o c�º§n c�º¥u h�¬nh.
                  </div>
                )}

                {selectedUser.roles.includes('ROLE_ADMIN') && (
                  <div className="text-center py-8 text-indigo-500 font-extrabold text-xs bg-indigo-50/50 rounded-2xl border border-indigo-100">
                    T� i kho�º£n Qu�º£n tr�»⬹ vi�ªn m�º·c ���»⬹nh c�³ to� n quy�»n h�»⬡ th�»��ng, kh�´ng c�º§n c�º¥u h�¬nh quy�»n chi ti�º¿t.
                  </div>
                )}

                {selectedUser.roles.includes('ROLE_TEACHER') && (
                  <div className="space-y-4">
                    <div 
                      onClick={() => togglePermissionToken('PERM_ASSIGN_HOMEWORK')}
                      className={`p-4 rounded-2xl border-2 transition-all cursor-pointer flex items-center justify-between ${
                        userPermissions.includes('PERM_ASSIGN_HOMEWORK') 
                          ? 'bg-amber-50 border-amber-300 shadow-sm' 
                          : 'bg-slate-50 border-slate-200 hover:border-slate-300'
                      }`}
                    >
                      <span className="text-xs font-black text-slate-800">Quy�»n giao b� i t�º­p cho h�»c sinh</span>
                      {userPermissions.includes('PERM_ASSIGN_HOMEWORK') ? (
                        <CheckSquare className="w-5 h-5 text-amber-600 fill-amber-100" />
                      ) : (
                        <Square className="w-5 h-5 text-slate-400" />
                      )}
                    </div>

                    <div 
                      onClick={() => togglePermissionToken('PERM_MANAGE_CLASSES')}
                      className={`p-4 rounded-2xl border-2 transition-all cursor-pointer flex items-center justify-between ${
                        userPermissions.includes('PERM_MANAGE_CLASSES') 
                          ? 'bg-amber-50 border-amber-300 shadow-sm' 
                          : 'bg-slate-50 border-slate-200 hover:border-slate-300'
                      }`}
                    >
                      <span className="text-xs font-black text-slate-800">Quy�»n qu�º£n l�½ l�»⬺p h�»c</span>
                      {userPermissions.includes('PERM_MANAGE_CLASSES') ? (
                        <CheckSquare className="w-5 h-5 text-amber-600 fill-amber-100" />
                      ) : (
                        <Square className="w-5 h-5 text-slate-400" />
                      )}
                    </div>

                    <div 
                      onClick={() => togglePermissionToken('PERM_CHAT_ACCESS')}
                      className={`p-4 rounded-2xl border-2 transition-all cursor-pointer flex items-center justify-between ${
                        userPermissions.includes('PERM_CHAT_ACCESS') 
                          ? 'bg-amber-50 border-amber-300 shadow-sm' 
                          : 'bg-slate-50 border-slate-200 hover:border-slate-300'
                      }`}
                    >
                      <span className="text-xs font-black text-slate-800">Quy�»n trao ���»⬢i tin nh�º¯n</span>
                      {userPermissions.includes('PERM_CHAT_ACCESS') ? (
                        <CheckSquare className="w-5 h-5 text-amber-600 fill-amber-100" />
                      ) : (
                        <Square className="w-5 h-5 text-slate-400" />
                      )}
                    </div>

                    <div className="bg-sky-50/50 border-2 border-sky-100 rounded-3xl p-5 space-y-4">
                      <div 
                        onClick={() => {
                          const hasAny = ['PERM_VIEW_ALPHABET', 'PERM_ADD_ALPHABET', 'PERM_EDIT_ALPHABET', 'PERM_DELETE_ALPHABET'].some(t => userPermissions.includes(t));
                          if (hasAny) {
                            setUserPermissions(prev => prev.filter(p => !['PERM_VIEW_ALPHABET', 'PERM_ADD_ALPHABET', 'PERM_EDIT_ALPHABET', 'PERM_DELETE_ALPHABET'].includes(p)));
                          } else {
                            setUserPermissions(prev => [...prev, 'PERM_VIEW_ALPHABET']);
                          }
                        }}
                        className={`p-3 rounded-2xl border-2 transition-all cursor-pointer flex items-center justify-between bg-white ${
                          ['PERM_VIEW_ALPHABET', 'PERM_ADD_ALPHABET', 'PERM_EDIT_ALPHABET', 'PERM_DELETE_ALPHABET'].some(t => userPermissions.includes(t))
                            ? 'border-sky-300 shadow-sm'
                            : 'border-slate-100 hover:border-slate-200'
                        }`}
                      >
                        <div className="flex flex-col pr-2">
                          <span className="text-xs font-black text-slate-800">Quy�»n qu�º£n l�½ h�»c li�»⬡u ch�»¯ c�¡i</span>
                          <span className="text-[9px] text-slate-400 font-bold mt-0.5">
                            (Ch�»§ y�º¿u l�  qu�º£n l�½ ph�º§n ���»c ch�»¯ c�»§a h�»c sinh g�»�Sm h�¬nh �º£nh v�  �¢m thanh)
                          </span>
                        </div>
                        {['PERM_VIEW_ALPHABET', 'PERM_ADD_ALPHABET', 'PERM_EDIT_ALPHABET', 'PERM_DELETE_ALPHABET'].some(t => userPermissions.includes(t)) ? (
                          <CheckSquare className="w-5 h-5 text-sky-600 fill-sky-50" />
                        ) : (
                          <Square className="w-5 h-5 text-slate-300" />
                        )}
                      </div>

                      {['PERM_VIEW_ALPHABET', 'PERM_ADD_ALPHABET', 'PERM_EDIT_ALPHABET', 'PERM_DELETE_ALPHABET'].some(t => userPermissions.includes(t)) && (
                        <div className="grid grid-cols-2 gap-3 animate-fade-in">
                          {[
                            { label: 'Th�ªm', token: 'PERM_ADD_ALPHABET' },
                            { label: 'S�»­a', token: 'PERM_EDIT_ALPHABET' },
                            { label: 'X�³a', token: 'PERM_DELETE_ALPHABET' },
                            { label: 'Xem', token: 'PERM_VIEW_ALPHABET' }
                          ].map((sub) => {
                            const hasSub = userPermissions.includes(sub.token);
                            return (
                              <div
                                key={sub.token}
                                onClick={() => togglePermissionToken(sub.token)}
                                className={`p-3 rounded-xl border-2 transition-all cursor-pointer flex items-center justify-between ${
                                  hasSub 
                                    ? 'bg-amber-50 border-amber-300 shadow-sm' 
                                    : 'bg-white border-slate-100 hover:border-slate-200'
                                }`}
                              >
                                <span className="text-xs font-black text-slate-700">{sub.label}</span>
                                {hasSub ? (
                                  <CheckSquare className="w-5 h-5 text-amber-600 fill-amber-50" />
                                ) : (
                                  <Square className="w-5 h-5 text-slate-300" />
                                )}
                              </div>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {selectedUser.roles.includes('ROLE_PARENT') && (
                  <div className="space-y-4">
                    <div 
                      onClick={() => togglePermissionToken('PERM_ASSIGN_HOMEWORK')}
                      className={`p-4 rounded-2xl border-2 transition-all cursor-pointer flex items-center justify-between ${
                        userPermissions.includes('PERM_ASSIGN_HOMEWORK') 
                          ? 'bg-amber-50 border-amber-300 shadow-sm' 
                          : 'bg-slate-50 border-slate-200 hover:border-slate-300'
                      }`}
                    >
                      <span className="text-xs font-black text-slate-800">Quy�»n giao b� i t�º­p cho con</span>
                      {userPermissions.includes('PERM_ASSIGN_HOMEWORK') ? (
                        <CheckSquare className="w-5 h-5 text-amber-600 fill-amber-100" />
                      ) : (
                        <Square className="w-5 h-5 text-slate-400" />
                      )}
                    </div>

                    <div 
                      onClick={() => togglePermissionToken('PERM_PARENT_TRACKING')}
                      className={`p-4 rounded-2xl border-2 transition-all cursor-pointer flex items-center justify-between ${
                        userPermissions.includes('PERM_PARENT_TRACKING') 
                          ? 'bg-amber-50 border-amber-300 shadow-sm' 
                          : 'bg-slate-50 border-slate-200 hover:border-slate-300'
                      }`}
                    >
                      <span className="text-xs font-black text-slate-800">Quy�»n theo d�µi ti�º¿n ���»�� & nh�º¯c b� i b�©</span>
                      {userPermissions.includes('PERM_PARENT_TRACKING') ? (
                        <CheckSquare className="w-5 h-5 text-amber-600 fill-amber-100" />
                      ) : (
                        <Square className="w-5 h-5 text-slate-400" />
                      )}
                    </div>

                    <div 
                      onClick={() => togglePermissionToken('PERM_CHAT_ACCESS')}
                      className={`p-4 rounded-2xl border-2 transition-all cursor-pointer flex items-center justify-between ${
                        userPermissions.includes('PERM_CHAT_ACCESS') 
                          ? 'bg-amber-50 border-amber-300 shadow-sm' 
                          : 'bg-slate-50 border-slate-200 hover:border-slate-300'
                      }`}
                    >
                      <span className="text-xs font-black text-slate-800">Quy�»n trao ���»⬢i tin nh�º¯n</span>
                      {userPermissions.includes('PERM_CHAT_ACCESS') ? (
                        <CheckSquare className="w-5 h-5 text-amber-600 fill-amber-100" />
                      ) : (
                        <Square className="w-5 h-5 text-slate-400" />
                      )}
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-4 border-t border-indigo-100">
              <button
                type="button"
                onClick={() => setShowPermissionModal(false)}
                className="px-5 py-2.5 rounded-xl bg-slate-100 text-slate-600 font-extrabold text-xs hover:bg-slate-200 transition-colors"
              >
                H�»§y B�»
              </button>
              <button
                onClick={handleSavePermissionsModal}
                disabled={savingPermissions}
                className="px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-black text-xs shadow-lg transition-all active:scale-95 cursor-pointer disabled:opacity-50 flex items-center gap-2"
              >
                {savingPermissions ? 'ang l °u...' : 'L °u C�º¥u H�¬nh Ph�¢n Quy�»n'}
              </button>
            </div>
          </div>
        </div>
      )}

      {showAddUserModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 animate-fade-in">
          <div className="bg-white border-4 border-indigo-200 rounded-[32px] p-6 sm:p-8 max-w-2xl w-full shadow-2xl space-y-6 relative max-h-[90vh] overflow-y-auto">
            <button
              onClick={() => setShowAddUserModal(false)}
              className="absolute top-5 right-5 p-2 rounded-full bg-slate-100 text-slate-500 hover:bg-rose-100 hover:text-rose-600 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="flex items-center gap-3 border-b-2 border-indigo-50 pb-4">
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-500 to-purple-600 flex items-center justify-center text-white font-black text-xl shadow-md">
                <UserPlus className="w-6 h-6 text-white" />
              </div>
              <div>
                <h3 className="text-lg font-black text-slate-800">Th�ªm T� i Kho�º£n Ng °�»i D�¹ng M�»⬺i</h3>
                <p className="text-xs font-bold text-indigo-600">T� i kho�º£n s�º½ k�­ch ho�º¡t ngay, kh�´ng c�º§n x�¡c th�»±c OTP</p>
              </div>
            </div>

            <form onSubmit={handleSingleCreate} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">T�ªn ���ng nh�º­p *</label>
                  <input
                    type="text"
                    value={newUsername}
                    onChange={(e) => setNewUsername(e.target.value)}
                    placeholder="VD: teacher_nguyen"
                    className="w-full bg-slate-50 border-2 border-slate-200 rounded-xl px-4 py-2.5 text-xs font-bold text-slate-800 focus:outline-none focus:border-indigo-500"
                    required
                  />
                </div>
                <div>
                  <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">M�º­t kh�º©u *</label>
                  <input
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="T�»��i thi�»�u 6 k�½ t�»±"
                    className="w-full bg-slate-50 border-2 border-slate-200 rounded-xl px-4 py-2.5 text-xs font-bold text-slate-800 focus:outline-none focus:border-indigo-500"
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">H�» v�  t�ªn *</label>
                  <input
                    type="text"
                    value={newFullName}
                    onChange={(e) => setNewFullName(e.target.value)}
                    placeholder="VD: Nguy�»⬦n V�n A"
                    className="w-full bg-slate-50 border-2 border-slate-200 rounded-xl px-4 py-2.5 text-xs font-bold text-slate-800 focus:outline-none focus:border-indigo-500"
                    required
                  />
                </div>
                <div>
                  <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">Email *</label>
                  <input
                    type="email"
                    value={newEmail}
                    onChange={(e) => setNewEmail(e.target.value)}
                    placeholder="VD: email@example.com"
                    className="w-full bg-slate-50 border-2 border-slate-200 rounded-xl px-4 py-2.5 text-xs font-bold text-slate-800 focus:outline-none focus:border-indigo-500"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">Vai tr�² ch�­nh *</label>
                <select
                  value={newRole}
                  onChange={(e) => setNewRole(e.target.value)}
                  className="w-full bg-slate-50 border-2 border-slate-200 rounded-xl px-4 py-2.5 text-xs font-black text-slate-800 focus:outline-none focus:border-indigo-500 cursor-pointer"
                >
                  <option value="TEACHER">Gi�¡o vi�ªn (TEACHER)</option>
                  <option value="PARENT">Ph�»¥ huynh (PARENT)</option>
                  <option value="STUDENT">H�»c sinh (STUDENT)</option>
                  <option value="ADMIN">Qu�º£n tr�»⬹ vi�ªn (ADMIN)</option>
                </select>
              </div>

              {newRole !== 'STUDENT' && newRole !== 'ADMIN' && (
                <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200 space-y-3">
                  <span className="text-xs font-black text-slate-700 block">C�º¥u H�¬nh Quy�»n Chi Ti�º¿t Ngay Khi T�º¡o</span>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    {PERMISSION_DEFINITIONS
                      .filter((perm) => {
                        if (newRole === 'TEACHER') {
                          return ['PERM_ASSIGN_HOMEWORK', 'PERM_MANAGE_CLASSES', 'PERM_VIEW_ALPHABET', 'PERM_CHAT_ACCESS'].includes(perm.token);
                        }
                        if (newRole === 'PARENT') {
                          return ['PERM_PARENT_TRACKING', 'PERM_CHAT_ACCESS'].includes(perm.token);
                        }
                        return false;
                      })
                      .map((perm) => {
                        const isChecked = newPermissions.includes(perm.token);
                        return (
                          <label
                            key={perm.token}
                            className="flex items-start gap-2.5 p-2 rounded-lg hover:bg-slate-100 cursor-pointer"
                          >
                            <input
                              type="checkbox"
                              checked={isChecked}
                              onChange={() => {
                                setNewPermissions(prev =>
                                  prev.includes(perm.token)
                                    ? prev.filter(p => p !== perm.token)
                                    : [...prev, perm.token]
                                );
                              }}
                              className="mt-0.5 rounded text-indigo-600 focus:ring-indigo-500 cursor-pointer w-4 h-4"
                            />
                            <div className="text-[11px] font-bold text-slate-800">{perm.title}</div>
                          </label>
                        );
                      })}
                  </div>
                </div>
              )}

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowAddUserModal(false)}
                  className="px-5 py-2.5 rounded-xl bg-slate-100 text-slate-600 hover:bg-slate-200 text-xs font-black transition-all"
                >
                  H�»§y
                </button>
                <button
                  type="submit"
                  disabled={submittingAddUser}
                  className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-black shadow-md flex items-center gap-1.5 transition-all disabled:opacity-60"
                >
                  {submittingAddUser ? 'ang t�º¡o...' : 'T�º¡o t� i kho�º£n'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showEditUserModal && editingUser && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 animate-fade-in">
          <div className="bg-white border-4 border-sky-200 rounded-[32px] p-6 sm:p-8 max-w-lg w-full shadow-2xl space-y-6 relative max-h-[90vh] overflow-y-auto">
            <button
              onClick={() => { setShowEditUserModal(false); setEditingUser(null); }}
              className="absolute top-5 right-5 p-2 rounded-full bg-slate-100 text-slate-500 hover:bg-rose-100 hover:text-rose-600 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="flex items-center gap-3 border-b-2 border-sky-50 pb-4">
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-sky-500 to-blue-600 flex items-center justify-center text-white font-black shadow-md">
                <Edit2 className="w-6 h-6 text-white" />
              </div>
              <div>
                <h3 className="text-lg font-black text-slate-800">Ch�»⬰nh S�»­a Th�´ng Tin T� i Kho�º£n</h3>
                <p className="text-xs font-bold text-sky-600">
                  Username: <span className="font-mono bg-sky-50 px-1.5 py-0.5 rounded-lg">{editingUser.username}</span>
                </p>
              </div>
            </div>

            <form onSubmit={handleSaveEditUser} className="space-y-4">
              <div className="bg-slate-50 border border-slate-200 rounded-xl px-4 py-3 space-y-0.5">
                <span className="text-[10px] font-black text-slate-400 uppercase tracking-wider block">T�ªn ���ng nh�º­p (kh�´ng th�»� thay ���»⬢i)</span>
                <span className="text-sm font-black text-slate-700 font-mono">{editingUser.username}</span>
              </div>

              <div>
                <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">H�» v�  t�ªn *</label>
                <input
                  type="text"
                  value={editFullName}
                  onChange={(e) => setEditFullName(e.target.value)}
                  placeholder="Nh�º­p h�» v�  t�ªn ���º§y ���»§"
                  className="w-full bg-slate-50 border-2 border-slate-200 rounded-xl px-4 py-2.5 text-xs font-bold text-slate-800 focus:outline-none focus:border-sky-500"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">Email *</label>
                <input
                  type="email"
                  value={editEmail}
                  onChange={(e) => setEditEmail(e.target.value)}
                  placeholder="Nh�º­p ���»⬹a ch�»⬰ email"
                  className="w-full bg-slate-50 border-2 border-slate-200 rounded-xl px-4 py-2.5 text-xs font-bold text-slate-800 focus:outline-none focus:border-sky-500"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">
                  �º·t l�º¡i m�º­t kh�º©u m�»⬺i <span className="text-slate-400 font-bold normal-case">(�»� tr�»��ng = gi�»¯ nguy�ªn)</span>
                </label>
                <input
                  type="password"
                  value={editNewPassword}
                  onChange={(e) => setEditNewPassword(e.target.value)}
                  placeholder="T�»��i thi�»�u 6 k�½ t�»± n�º¿u mu�»��n ���»⬢i"
                  className="w-full bg-slate-50 border-2 border-slate-200 rounded-xl px-4 py-2.5 text-xs font-bold text-slate-800 focus:outline-none focus:border-sky-500"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => { setShowEditUserModal(false); setEditingUser(null); }}
                  className="px-5 py-2.5 rounded-xl bg-slate-100 text-slate-600 hover:bg-slate-200 text-xs font-black transition-all"
                >
                  H�»§y
                </button>
                <button
                  type="submit"
                  disabled={savingEditUser}
                  className="px-5 py-2.5 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-black shadow-md flex items-center gap-1.5 transition-all disabled:opacity-60"
                >
                  {savingEditUser ? 'ang l °u...' : 'L °u thay ���»⬢i'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showEditModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 animate-fade-in">
          <div className="bg-white border-4 border-emerald-200 rounded-[32px] p-6 sm:p-8 max-w-4xl w-full shadow-2xl space-y-6 relative max-h-[90vh] overflow-y-auto">
            <button
              onClick={() => setShowEditModal(false)}
              className="absolute top-5 right-5 p-2 rounded-full bg-slate-100 text-slate-500 hover:bg-rose-100 hover:text-rose-600 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="flex items-center gap-3 border-b-2 border-emerald-50 pb-4">
              <div className="w-16 h-12 rounded-2xl bg-gradient-to-tr from-emerald-400 to-teal-600 flex items-center justify-center text-white font-black text-2xl shadow-md uppercase tracking-wider">
                {selectedChar.toUpperCase()}{selectedChar.toLowerCase()}
              </div>
              <div>
                <h3 className="text-lg font-black text-slate-800">
                  Ch�»⬰nh S�»­a H�»c Li�»⬡u Ch�»¯ C�¡i {selectedChar.toUpperCase()}{selectedChar.toLowerCase()}
                </h3>
                <p className="text-xs font-bold text-emerald-600">
                  Thay ���»⬢i th�´ng tin h�¬nh �º£nh, �¢m thanh m�º«u c�»§a ch�»¯ c�¡i l�ªn server
                </p>
              </div>
            </div>

            <form onSubmit={handleSaveLetterModal} className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                
                {/* PH�º¦N 1 - H�»�C CH�»® C�I */}
                <div className="bg-sky-50/40 border-2 border-sky-100 p-5 rounded-[24px] space-y-4">
                  <div className="flex items-center gap-2 border-b border-sky-200 pb-2">
                    <span className="text-base">�Ÿ⬦°�¸</span>
                    <h4 className="text-sm font-black text-sky-850">PH�º¦N 1: H�»�C CH�»® C�I</h4>
                  </div>

                                    <div className="flex items-center justify-between">
                    <div>
                      <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">Tr�ng Th�i (�n/Hi�n)</label>
                      <button
                        type="button"
                        onClick={() => setEditIsHidden(!editIsHidden)}
                        className={"flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-bold transition-all " + (editIsHidden ? "bg-rose-100 text-rose-600 border border-rose-200" : "bg-emerald-100 text-emerald-700 border border-emerald-200")}
                      >
                        {editIsHidden ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                        {editIsHidden ? "ang �n (H�c sinh kh�ng th�y)" : "ang Hi�n (H�c sinh h�c b�nh th��ng)"}
                      </button>
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">T�ªn Ch�»¯</label>
                    <input
                      type="text"
                      value={selectedChar}
                      onChange={(e) => setSelectedChar(e.target.value)}
                      className="w-full bg-white border-2 border-sky-100 rounded-xl px-3 py-2.5 text-xs font-black text-slate-900 focus:outline-none focus:border-sky-400 disabled:opacity-75"
                      disabled={!isAddingLetter}
                      required
                    />
                  </div>

                  <div className="bg-white border border-sky-200 rounded-2xl p-4 space-y-3 shadow-sm">
                    <span className="text-[11px] font-black text-sky-850 block">�Ÿ���¸ Ph�¡t �⬚m Ch�»¯ C�¡i (Ghi �¢m ho�º·c Ch�»n file)</span>
                    
                    <div className="flex flex-col gap-2.5">
                      <div className="flex items-center gap-2 flex-wrap">
                        {!isRecording ? (
                          <button
                            type="button"
                            onClick={startRecordingAdmin}
                            className="px-3 py-1.5 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-[11px] font-black flex items-center gap-1 shadow-sm active:scale-95 transition-all"
                          >
                            <Mic className="w-3 h-3" /> Ghi �¢m
                          </button>
                        ) : (
                          <button
                            type="button"
                            onClick={stopRecordingAdmin}
                            className="px-3 py-1.5 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-[11px] font-black flex items-center gap-1 shadow-sm animate-pulse active:scale-95 transition-all"
                          >
                            <Square className="w-3 h-3" /> L °u
                          </button>
                        )}

                        <label className="px-3 py-1.5 rounded-xl bg-white border border-sky-300 text-sky-600 hover:bg-sky-50 text-[11px] font-black flex items-center gap-1 shadow-sm cursor-pointer transition-all active:scale-95">
                          <Upload className="w-3 h-3" /> Ch�»n file
                          <input
                            type="file"
                            accept="audio/*"
                            onChange={handleAudioFileChangeAdmin}
                            className="hidden"
                          />
                        </label>

                        {recordedAudioUrl && (
                          <button
                            type="button"
                            onClick={deleteRecordedAudio}
                            className="p-1.5 rounded-xl bg-rose-50 hover:bg-rose-100 text-rose-600 border border-rose-200 transition-colors"
                            title="X�³a �¢m thanh"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>

                      <button
                        type="button"
                        onClick={playLetterAudioAdmin}
                        className="px-3 py-2 bg-sky-100 hover:bg-sky-200 text-sky-700 rounded-xl text-xs font-black flex items-center gap-1.5 active:scale-95 transition-all shadow-sm w-fit"
                      >
                        <Volume2 className="w-4 h-4" /> Nghe th�»­ ph�¡t �¢m ch�»¯ c�¡i
                      </button>

                      {selectedAudioFile && (
                        <p className="text-[10px] font-extrabold text-sky-700">�ŸŸ¢ �£ ch�»n: {selectedAudioFile.name}</p>
                      )}
                      {recordedBlob && !selectedAudioFile && (
                        <p className="text-[10px] font-extrabold text-teal-700">�Ÿ⬝´ Ghi �¢m s�ºµn s� ng ({Math.round(recordedBlob.size / 1024)} KB)</p>
                      )}
                    </div>
                  </div>

                  <div className="bg-white border border-sky-200 rounded-2xl p-4 space-y-3 shadow-sm">
                    <span className="text-[11px] font-black text-slate-700 block">�Ÿ�S· �º¢nh N�©t Ch�»¯ M�º«u (.png)</span>
                    
                    <div className="flex items-center gap-4">
                      <div className="w-16 h-16 rounded-xl border-2 border-emerald-400 bg-white overflow-hidden flex items-center justify-center p-1 shadow-inner shrink-0 relative">
                        <div className="absolute inset-0 flex items-center justify-center text-2xl font-black text-slate-800 font-mali">
                          {selectedChar.toLowerCase()}
                        </div>

                        <img
                          src={previewImageSrc || `/alphabet/${selectedChar.toLowerCase()}.png?v=${imageVersion}`}
                          alt="Preview"
                          className="absolute inset-0 w-full h-full object-fill crisp-letter-img bg-white z-10 p-1"
                          onError={(e) => {
                            (e.target as HTMLElement).style.display = 'none';
                          }}
                        />
                      </div>

                      <label className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-black text-xs flex items-center gap-2 shadow-md cursor-pointer transition-all active:scale-95">
                        <Upload className="w-3.5 h-3.5" /> Ch�»n �º¢nh M�»⬺i
                        <input
                          type="file"
                          accept="image/*"
                          onChange={handleImageFileChange}
                          className="hidden"
                        />
                      </label>
                    </div>
                    
                    {selectedImageFile && (
                      <p className="text-[10px] font-extrabold text-emerald-700">�ŸŸ¢ �£ ch�»n: {selectedImageFile.name}</p>
                    )}
                  </div>
                </div>

                {/* PH�º¦N 2 - T�»ª V� D�»¤ MINH H�»�A */}
                <div className="bg-amber-50/40 border-2 border-amber-100 p-5 rounded-[24px] space-y-4">
                  <div className="flex items-center gap-2 border-b border-amber-200 pb-2">
                    <span className="text-base">�Ÿ� </span>
                    <h4 className="text-sm font-black text-amber-900">PH�º¦N 2: T�»ª V� D�»¤ MINH H�»�A</h4>
                  </div>

                  <div>
                    <label className="block text-xs font-black text-slate-700 uppercase tracking-wider mb-1.5">T�»« V�­ D�»¥ Minh H�»a</label>
                    <input
                      type="text"
                      value={editExample}
                      onChange={(e) => setEditExample(e.target.value)}
                      placeholder="V�­ d�»¥: Con c�¡, Qu�º£ b�³ng..."
                      className="w-full bg-white border-2 border-amber-100 rounded-xl px-3 py-2.5 text-xs font-bold text-slate-800 focus:outline-none focus:border-amber-400"
                    />
                  </div>

                  <div className="bg-white border border-amber-200 rounded-2xl p-4 space-y-3 shadow-sm">
                    <span className="text-[11px] font-black text-amber-900 block">�Ÿ���¸ Ph�¡t �⬚m T�»« V�­ D�»¥ (Ghi �¢m ho�º·c Ch�»n file)</span>
                    
                    <div className="flex flex-col gap-2.5">
                      <div className="flex items-center gap-2 flex-wrap">
                        {!isRecordingExample ? (
                          <button
                            type="button"
                            onClick={startRecordingExampleAdmin}
                            className="px-3 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-500 text-white text-[11px] font-black flex items-center gap-1 shadow-sm active:scale-95 transition-all"
                          >
                            <Mic className="w-3 h-3" /> Ghi �¢m
                          </button>
                        ) : (
                          <button
                            type="button"
                            onClick={stopRecordingExampleAdmin}
                            className="px-3 py-1.5 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-[11px] font-black flex items-center gap-1 shadow-sm animate-pulse active:scale-95 transition-all"
                          >
                            <Square className="w-3 h-3" /> L °u
                          </button>
                        )}

                        <label className="px-3 py-1.5 rounded-xl bg-white border border-amber-300 text-amber-600 hover:bg-amber-50 text-[11px] font-black flex items-center gap-1 shadow-sm cursor-pointer transition-all active:scale-95">
                          <Upload className="w-3 h-3" /> Ch�»n file
                          <input
                            type="file"
                            accept="audio/*"
                            onChange={handleExampleAudioFileChangeAdmin}
                            className="hidden"
                          />
                        </label>

                        {recordedExampleAudioUrl && (
                          <button
                            type="button"
                            onClick={deleteRecordedExampleAudio}
                            className="p-1.5 rounded-xl bg-rose-50 hover:bg-rose-100 text-rose-600 border border-rose-200 transition-colors"
                            title="X�³a �¢m thanh"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>

                      <button
                        type="button"
                        onClick={playExampleAudioAdmin}
                        className="px-3 py-2 bg-amber-100 hover:bg-amber-200 text-amber-800 rounded-xl text-xs font-black flex items-center gap-1.5 active:scale-95 transition-all shadow-sm w-fit"
                      >
                        <Volume2 className="w-4 h-4" /> Nghe th�»­ ph�¡t �¢m t�»« v�­ d�»¥
                      </button>

                      {selectedExampleAudioFile && (
                        <p className="text-[10px] font-extrabold text-amber-700">�ŸŸ¢ �£ ch�»n: {selectedExampleAudioFile.name}</p>
                      )}
                      {recordedExampleBlob && !selectedExampleAudioFile && (
                        <p className="text-[10px] font-extrabold text-amber-700">�Ÿ⬝´ Ghi �¢m s�ºµn s� ng ({Math.round(recordedExampleBlob.size / 1024)} KB)</p>
                      )}
                    </div>
                  </div>

                  <div className="bg-white border border-amber-200 rounded-2xl p-4 space-y-3 shadow-sm">
                    <span className="text-[11px] font-black text-slate-700 block">�Ÿ�¼�¸ �º¢nh Minh H�»a V�­ D�»¥</span>
                    
                    <div className="flex items-center gap-4">
                      <div className="w-16 h-16 rounded-xl border-2 border-amber-200 bg-slate-50 overflow-hidden flex items-center justify-center p-1 shrink-0 relative">
                        {previewExampleImgSrc ? (
                          <img
                            src={previewExampleImgSrc}
                            alt="Preview Example"
                            className="w-full h-full object-contain"
                          />
                        ) : (
                          <span className="text-2xl">{getExampleEmoji(editExample)}</span>
                        )}
                      </div>

                      <label className="px-4 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-white font-black text-xs flex items-center gap-2 shadow-md cursor-pointer transition-all active:scale-95">
                        <Upload className="w-3.5 h-3.5" /> Ch�»n �º¢nh M�»⬺i
                        <input
                          type="file"
                          accept="image/*"
                          onChange={handleExampleImgChangeAdmin}
                          className="hidden"
                        />
                      </label>
                    </div>
                    
                    {selectedExampleImgFile && (
                      <p className="text-[10px] font-extrabold text-amber-700">�ŸŸ¢ �£ ch�»n: {selectedExampleImgFile.name}</p>
                    )}
                  </div>
                </div>
              </div>

              <div className="flex items-center justify-end gap-3 pt-6 border-t-2 border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowEditModal(false)}
                  className="px-6 py-3 rounded-xl bg-slate-100 text-slate-600 font-extrabold text-sm hover:bg-slate-200 transition-colors"
                >
                  H�»§y B�»
                </button>
                <button
                  type="submit"
                  disabled={uploading}
                  className="px-8 py-3 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-black text-sm shadow-lg shadow-emerald-600/30 transition-all active:scale-95 cursor-pointer disabled:opacity-50 flex items-center gap-2"
                >
                  {uploading ? 'ang L °u...' : 'L °u Thay �»⬢i'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {toast && (
        <div className="fixed bottom-6 right-6 z-[9999] flex items-center gap-3 px-5 py-4 rounded-2xl shadow-xl border animate-fade-in bg-white border-sky-100 max-w-sm">
          <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 ${
            toast.type === 'success' ? 'bg-emerald-100 text-emerald-600' : 'bg-rose-100 text-rose-600'
          }`}>
            {toast.type === 'success' ? (
              <Check className="w-4.5 h-4.5" />
            ) : (
              <AlertCircle className="w-4.5 h-4.5" />
            )}
          </div>
          <div className="space-y-0.5">
            <h4 className="text-xs font-black text-slate-800">
              {toast.type === 'success' ? 'Th� nh c�´ng' : 'C�³ l�»�i x�º£y ra'}
            </h4>
            <p className="text-[11px] font-bold text-slate-500">{toast.message}</p>
          </div>
          <button 
            onClick={() => setToast(null)}
            className="p-1 rounded-full text-slate-400 hover:bg-slate-50 transition-colors cursor-pointer self-start"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}
    </div>
  );
};

export default AdminDashboard;



