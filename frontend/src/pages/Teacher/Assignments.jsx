import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { ClipboardList, Plus, Clock, AlertCircle, Eye, X, Trash2, Send, FileText, Layers, ChevronRight, Target, Edit3, PenTool, HelpCircle, Save, Volume2, Calendar, Upload } from 'lucide-react';
const ALL_VIETNAMESE_LETTERS = [
    'A', 'Ă', 'Â', 'B', 'C', 'D', 'Đ', 'E', 'Ê', 'G', 'H', 'I',
    'K', 'L', 'M', 'N', 'O', 'Ô', 'Ơ', 'P', 'Q', 'R', 'S', 'T', 'U', 'Ư', 'V', 'X', 'Y'
];
const speakAnimalName = (text) => {
    if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel();
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.lang = 'vi-VN';
        utterance.rate = 0.85;
        window.speechSynthesis.speak(utterance);
    }
};
const INITIAL_WRITING_TEMPLATES = [
    {
        id: 'tpl-writing-1',
        title: 'Bài 1: Luyện tô viết chữ A, Ă, Â, B, C, D',
        description: 'Bài 1 gồm 6 chữ cái: A, Ă, Â, B, C, D. Yêu cầu bé tô nắn nót nét chữ chuẩn chỉ tiêu ≥70%.',
        contentType: 'WRITING',
        contentData: JSON.stringify({ letters: ['A', 'Ă', 'Â', 'B', 'C', 'D'], targetScore: 70 }),
        category: 'Luyện tô viết chữ'
    },
    {
        id: 'tpl-writing-2',
        title: 'Bài 2: Luyện tô viết chữ Đ, E, Ê, G, H, I',
        description: 'Bài 2 gồm 6 chữ cái: Đ, E, Ê, G, H, I. Yêu cầu bé tô nét chuẩn tròn đều đạt chỉ tiêu ≥70%.',
        contentType: 'WRITING',
        contentData: JSON.stringify({ letters: ['Đ', 'E', 'Ê', 'G', 'H', 'I'], targetScore: 70 }),
        category: 'Luyện tô viết chữ'
    },
    {
        id: 'tpl-writing-3',
        title: 'Bài 3: Luyện tô viết chữ K, L, M, N, O, Ô',
        description: 'Bài 3 gồm 6 chữ cái: K, L, M, N, O, Ô. Yêu cầu bé rèn nét chữ đều tay đạt chỉ tiêu ≥70%.',
        contentType: 'WRITING',
        contentData: JSON.stringify({ letters: ['K', 'L', 'M', 'N', 'O', 'Ô'], targetScore: 70 }),
        category: 'Luyện tô viết chữ'
    },
    {
        id: 'tpl-writing-4',
        title: 'Bài 4: Luyện tô viết chữ Ơ, P, Q, R, S, T',
        description: 'Bài 4 gồm 6 chữ cái: Ơ, P, Q, R, S, T. Yêu cầu bé tô chuẩn chỉ tiêu ≥70%.',
        contentType: 'WRITING',
        contentData: JSON.stringify({ letters: ['Ơ', 'P', 'Q', 'R', 'S', 'T'], targetScore: 70 }),
        category: 'Luyện tô viết chữ'
    },
    {
        id: 'tpl-writing-5',
        title: 'Bài 5: Luyện tô viết chữ U, Ư, V, X, Y',
        description: 'Bài 5 gồm 5 chữ cái: U, Ư, V, X, Y. Yêu cầu bé hoàn thành nắn nót đạt chỉ tiêu ≥70%.',
        contentType: 'WRITING',
        contentData: JSON.stringify({ letters: ['U', 'Ư', 'V', 'X', 'Y'], targetScore: 70 }),
        category: 'Luyện tô viết chữ'
    }
];
const INITIAL_QUIZ_TEMPLATES = [
    {
        id: 'tpl-quiz-1',
        title: 'Bài 1 Trắc nghiệm: Nghe đọc tên con vật (Gà, Mèo, Bò) & Chọn chữ cái',
        description: 'Bé bấm vào hình ảnh để nghe cô đọc tên con vật (Con Gà, Con Mèo, Con Bò) rồi chọn chữ cái bắt đầu đúng nhé!',
        contentType: 'QUIZ',
        contentData: JSON.stringify([
            {
                question: '',
                animalName: 'Con Gà',
                animalEmoji: '🐓',
                options: ['G', 'B', 'C'],
                answer: 'G'
            },
            {
                question: '',
                animalName: 'Con Mèo',
                animalEmoji: '🐱',
                options: ['M', 'D', 'N'],
                answer: 'M'
            },
            {
                question: '',
                animalName: 'Con Bò',
                animalEmoji: '🐄',
                options: ['B', 'A', 'H'],
                answer: 'B'
            }
        ]),
        category: 'Bài tập trắc nghiệm'
    },
    {
        id: 'tpl-quiz-2',
        title: 'Bài 2 Trắc nghiệm: Nghe đọc tên con vật (Cá, Vịt, Chó) & Chọn chữ cái',
        description: 'Bé bấm vào hình con vật Cá, Vịt, Chó để nghe phát âm và chọn đáp án chính xác.',
        contentType: 'QUIZ',
        contentData: JSON.stringify([
            {
                question: '',
                animalName: 'Con Cá',
                animalEmoji: '🐟',
                options: ['C', 'T', 'K'],
                answer: 'C'
            },
            {
                question: '',
                animalName: 'Con Vịt',
                animalEmoji: '🦆',
                options: ['V', 'R', 'L'],
                answer: 'V'
            },
            {
                question: '',
                animalName: 'Con Chó',
                animalEmoji: '🐶',
                options: ['C', 'N', 'S'],
                answer: 'C'
            }
        ]),
        category: 'Bài tập trắc nghiệm'
    },
    {
        id: 'tpl-quiz-3',
        title: 'Bài 3 Trắc nghiệm: Nghe đọc tên con vật (Hổ, Voi, Ngựa) & Chọn chữ cái',
        description: 'Bé chạm nghe đọc tên con vật Hổ, Voi, Ngựa và chọn đáp án chữ cái.',
        contentType: 'QUIZ',
        contentData: JSON.stringify([
            {
                question: '',
                animalName: 'Con Hổ',
                animalEmoji: '🐅',
                options: ['H', 'G', 'T'],
                answer: 'H'
            },
            {
                question: '',
                animalName: 'Con Voi',
                animalEmoji: '🐘',
                options: ['V', 'B', 'M'],
                answer: 'V'
            },
            {
                question: '',
                animalName: 'Con Ngựa',
                animalEmoji: '🐎',
                options: ['N', 'D', 'Đ'],
                answer: 'N'
            }
        ]),
        category: 'Bài tập trắc nghiệm'
    },
    {
        id: 'tpl-quiz-4',
        title: 'Bài 4 Trắc nghiệm: Nghe đọc tên con vật (Chim, Thỏ, Dê) & Chọn chữ cái',
        description: 'Bé bấm nghe giọng phát âm tên con vật Chim, Thỏ, Dê để chọn đáp án.',
        contentType: 'QUIZ',
        contentData: JSON.stringify([
            {
                question: '',
                animalName: 'Con Chim',
                animalEmoji: '🐦',
                options: ['C', 'H', 'L'],
                answer: 'C'
            },
            {
                question: '',
                animalName: 'Con Thỏ',
                animalEmoji: '🐰',
                options: ['T', 'S', 'P'],
                answer: 'T'
            },
            {
                question: '',
                animalName: 'Con Dê',
                animalEmoji: '🐐',
                options: ['D', 'Đ', 'R'],
                answer: 'D'
            }
        ]),
        category: 'Bài tập trắc nghiệm'
    },
    {
        id: 'tpl-quiz-5',
        title: 'Bài 5 Trắc nghiệm: Nghe đọc tên con vật (Rắn, Gấu, Lợn) & Chọn chữ cái',
        description: 'Bé nghe phát âm chuẩn tên các con vật Rắn, Gấu, Lợn và hoàn thành bài tập.',
        contentType: 'QUIZ',
        contentData: JSON.stringify([
            {
                question: '',
                animalName: 'Con Rắn',
                animalEmoji: '🐍',
                options: ['R', 'G', 'S'],
                answer: 'R'
            },
            {
                question: '',
                animalName: 'Con Gấu',
                animalEmoji: '🐻',
                options: ['G', 'B', 'K'],
                answer: 'G'
            },
            {
                question: '',
                animalName: 'Con Lợn',
                animalEmoji: '🐖',
                options: ['L', 'N', 'M'],
                answer: 'L'
            }
        ]),
        category: 'Bài tập trắc nghiệm'
    }
];
const DEFAULT_TEMPLATES = [...INITIAL_WRITING_TEMPLATES, ...INITIAL_QUIZ_TEMPLATES];
const TeacherAssignments = () => {
    const [classes, setClasses] = useState([]);
    const [selectedClassId, setSelectedClassId] = useState('');
    const [assignments, setAssignments] = useState([]);
    const [submissions, setSubmissions] = useState([]);
    const [selectedAsm, setSelectedAsm] = useState(null);
    const [confirmModal, setConfirmModal] = useState({
        isOpen: false,
        title: '',
        message: '',
        onConfirm: () => { },
        confirmText: 'Xác nhận',
        type: 'danger'
    });
    // Kho bài tập mẫu
    const [templates, setTemplates] = useState(() => {
        const saved = localStorage.getItem('teacher_assignment_templates');
        if (saved) {
            const parsed = JSON.parse(saved);
            const hasQuizData = parsed.some((t) => t.id === 'tpl-quiz-1');
            if (hasQuizData)
                return parsed;
        }
        return DEFAULT_TEMPLATES;
    });
    // Modal Tạo / Chỉnh Sửa Bài Tập Mẫu vào Kho
    const [showTemplateModal, setShowTemplateModal] = useState(false);
    const [editingTemplateId, setEditingTemplateId] = useState(null);
    const [tplTitle, setTplTitle] = useState('');
    const [tplDesc, setTplDesc] = useState('');
    const [tplType, setTplType] = useState('WRITING');
    const [tplSelectedLetters, setTplSelectedLetters] = useState(['A']);
    const [tplTargetScore, setTplTargetScore] = useState(70);
    const [tplQuizQuestions, setTplQuizQuestions] = useState([
        {
            question: 'Bé hãy bấm vào hình nghe đọc tên con vật và chọn chữ cái bắt đầu:',
            animalName: 'Con Gà',
            animalEmoji: '🐓',
            options: ['G', 'B', 'C'],
            answer: 'G'
        }
    ]);
    // Modal Giao Bài Tập Từ Kho
    const [showAssignModal, setShowAssignModal] = useState(false);
    const [selectedTemplateId, setSelectedTemplateId] = useState('');
    const [assignDueDate, setAssignDueDate] = useState('');
    const [assigningLoading, setAssigningLoading] = useState(false);
    const [assignError, setAssignError] = useState('');
    // Modal Chỉnh Sửa Hạn Nộp / Chi Tiết Bài Tập ĐÃ GIAO
    const [showEditAssignedModal, setShowEditAssignedModal] = useState(false);
    const [editingAssignedItem, setEditingAssignedItem] = useState(null);
    const [editAssignedTitle, setEditAssignedTitle] = useState('');
    const [editAssignedDesc, setEditAssignedDesc] = useState('');
    const [editAssignedDueDate, setEditAssignedDueDate] = useState('');
    const [editAssignedLoading, setEditAssignedLoading] = useState(false);
    const [editAssignedError, setEditAssignedError] = useState('');
    // Chấm điểm modal
    const [gradingSub, setGradingSub] = useState(null);
    const [gradeScore, setGradeScore] = useState('');
    const [gradeFeedback, setGradeFeedback] = useState('');
    const [gradingLoading, setGradingLoading] = useState(false);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState('ASSIGNED');
    const [khoSubTab, setKhoSubTab] = useState('WRITING');
    const fetchInitialData = async () => {
        try {
            const res = await api.get('/teacher/class');
            setClasses(res.data);
            if (res.data.length > 0) {
                setSelectedClassId(res.data[0].id);
                fetchAssignments(res.data[0].id);
            }
        }
        catch (err) {
            console.error(err);
        }
        finally {
            setLoading(false);
        }
    };
    useEffect(() => {
        fetchInitialData();
    }, []);
    const fetchAssignments = async (classId) => {
        try {
            const res = await api.get(`/teacher/class/${classId}/assignments`);
            const list = res.data || [];
            setAssignments(list);
            if (list.length > 0) {
                handleSelectAssignment(list[0]);
            }
            else {
                setSelectedAsm(null);
                setSubmissions([]);
            }
        }
        catch (err) {
            console.error(err);
        }
    };
    const handleClassChange = (classId) => {
        setSelectedClassId(classId);
        fetchAssignments(classId);
    };
    const handleSelectAssignment = async (asm) => {
        setSelectedAsm(asm);
        try {
            const res = await api.get(`/teacher/assignments/${asm.id}/submissions`);
            setSubmissions(res.data);
        }
        catch (err) {
            console.error(err);
        }
    };
    const saveTemplates = (newTemplates) => {
        setTemplates(newTemplates);
        localStorage.setItem('teacher_assignment_templates', JSON.stringify(newTemplates));
    };
    const toggleLetterSelection = (letter) => {
        if (tplSelectedLetters.includes(letter)) {
            if (tplSelectedLetters.length === 1)
                return;
            setTplSelectedLetters(tplSelectedLetters.filter(l => l !== letter));
        }
        else {
            setTplSelectedLetters([...tplSelectedLetters, letter]);
        }
    };
    const handleOpenCreateModal = (type = 'WRITING') => {
        setEditingTemplateId(null);
        setTplTitle('');
        setTplDesc('');
        setTplType(type);
        setTplSelectedLetters(['A', 'B', 'C']);
        setTplTargetScore(70);
        setTplQuizQuestions([
            {
                question: 'Bé hãy bấm vào hình nghe đọc tên con vật và chọn chữ cái bắt đầu:',
                animalName: 'Con Gà',
                animalEmoji: '🐓',
                options: ['G', 'B', 'C'],
                answer: 'G'
            }
        ]);
        setShowTemplateModal(true);
    };
    const handleOpenEditModal = (tpl) => {
        setEditingTemplateId(tpl.id);
        setTplTitle(tpl.title);
        setTplDesc(tpl.description);
        setTplType(tpl.contentType);
        if (tpl.contentType === 'WRITING') {
            const info = parseContentInfo(tpl.contentType, tpl.contentData);
            setTplSelectedLetters(info.letters.length > 0 ? info.letters : ['A']);
            setTplTargetScore(info.targetScore || 70);
        }
        else {
            try {
                setTplQuizQuestions(JSON.parse(tpl.contentData));
            }
            catch (e) {
                setTplQuizQuestions([{ question: '', animalName: 'Con Vật', animalEmoji: '🐾', options: ['', '', ''], answer: '' }]);
            }
        }
        setShowTemplateModal(true);
    };
    const handleSaveTemplate = (e) => {
        e.preventDefault();
        if (!tplTitle.trim())
            return;
        let contentData = '';
        if (tplType === 'WRITING') {
            contentData = JSON.stringify({
                letters: tplSelectedLetters,
                targetScore: Number(tplTargetScore) || 70
            });
        }
        else {
            contentData = JSON.stringify(tplQuizQuestions);
        }
        if (editingTemplateId) {
            const updated = templates.map(t => {
                if (t.id === editingTemplateId) {
                    return {
                        ...t,
                        title: tplTitle,
                        description: tplDesc,
                        contentType: tplType,
                        contentData,
                        category: tplType === 'WRITING' ? `Luyện tô (${tplSelectedLetters.length} chữ)` : 'Bài tập trắc nghiệm'
                    };
                }
                return t;
            });
            saveTemplates(updated);
        }
        else {
            const newTpl = {
                id: 'tpl-' + Date.now(),
                title: tplTitle,
                description: tplDesc,
                contentType: tplType,
                contentData,
                category: tplType === 'WRITING' ? `Luyện tô (${tplSelectedLetters.length} chữ)` : 'Bài tập trắc nghiệm'
            };
            saveTemplates([newTpl, ...templates]);
        }
        setShowTemplateModal(false);
    };
    const handleDeleteTemplate = (id) => {
        setConfirmModal({
            isOpen: true,
            title: 'Xóa bài tập mẫu',
            message: 'Cô có chắc muốn xóa bài tập mẫu này khỏi Kho không?',
            type: 'warning',
            confirmText: 'Xóa mẫu',
            onConfirm: () => {
                const updated = templates.filter(t => t.id !== id);
                saveTemplates(updated);
                setConfirmModal(prev => ({ ...prev, isOpen: false }));
            }
        });
    };
    const handleAssignTemplateToClass = async (e) => {
        e.preventDefault();
        if (!selectedClassId) {
            setAssignError('Vui lòng chọn lớp học để giao bài!');
            return;
        }
        const tpl = templates.find(t => t.id === selectedTemplateId);
        if (!tpl) {
            setAssignError('Vui lòng chọn một bài tập từ Kho mẫu!');
            return;
        }
        setAssigningLoading(true);
        setAssignError('');
        try {
            await api.post('/teacher/assignments', {
                title: tpl.title,
                description: tpl.description,
                contentType: tpl.contentType,
                contentData: tpl.contentData,
                dueDate: assignDueDate ? new Date(assignDueDate).toISOString() : null,
                classId: selectedClassId
            });
            setShowAssignModal(false);
            setSelectedTemplateId('');
            setAssignDueDate('');
            fetchAssignments(Number(selectedClassId));
        }
        catch (err) {
            setAssignError(err.response?.data?.message || 'Có lỗi xảy ra khi giao bài tập');
        }
        finally {
            setAssigningLoading(false);
        }
    };
    const handleOpenAssignModalWithTemplate = (templateId) => {
        setSelectedTemplateId(templateId);
        setAssignError('');
        setShowAssignModal(true);
    };
    // --- XỬ LÝ CHỈNH SỬA HẠN NỘP & XÓA BÀI TẬP ĐÃ GIAO ---
    const handleOpenEditAssignedModal = (asm, e) => {
        if (e)
            e.stopPropagation();
        setEditingAssignedItem(asm);
        setEditAssignedTitle(asm.title);
        setEditAssignedDesc(asm.description || '');
        setEditAssignedDueDate(asm.dueDate ? new Date(asm.dueDate).toISOString().slice(0, 16) : '');
        setEditAssignedError('');
        setShowEditAssignedModal(true);
    };
    const handleSaveEditAssignedAssignment = async (e) => {
        e.preventDefault();
        if (!editingAssignedItem)
            return;
        setEditAssignedLoading(true);
        setEditAssignedError('');
        try {
            await api.put(`/teacher/assignments/${editingAssignedItem.id}`, {
                title: editAssignedTitle,
                description: editAssignedDesc,
                dueDate: editAssignedDueDate ? new Date(editAssignedDueDate).toISOString() : null,
                contentType: editingAssignedItem.contentType,
                contentData: editingAssignedItem.contentData,
                classId: selectedClassId
            });
            setShowEditAssignedModal(false);
            setEditingAssignedItem(null);
            if (selectedClassId) {
                fetchAssignments(Number(selectedClassId));
            }
        }
        catch (err) {
            setEditAssignedError(err.response?.data?.message || 'Lỗi cập nhật bài tập');
        }
        finally {
            setEditAssignedLoading(false);
        }
    };
    const handleDeleteAssignedAssignment = (asmId, e) => {
        if (e)
            e.stopPropagation();
        setConfirmModal({
            isOpen: true,
            title: 'Xóa bài tập đã giao',
            message: 'Cô có chắc chắn muốn xóa bài tập đã giao này không? Tất cả bài nộp của học sinh thuộc bài tập này cũng sẽ bị xóa.',
            type: 'danger',
            confirmText: 'Xóa vĩnh viễn 💥',
            onConfirm: async () => {
                try {
                    await api.delete(`/teacher/assignments/${asmId}`);
                    if (selectedAsm?.id === asmId) {
                        setSelectedAsm(null);
                        setSubmissions([]);
                    }
                    if (selectedClassId) {
                        fetchAssignments(Number(selectedClassId));
                    }
                }
                catch (err) {
                    alert(err.response?.data?.message || 'Xóa bài tập thất bại');
                }
                finally {
                    setConfirmModal(prev => ({ ...prev, isOpen: false }));
                }
            }
        });
    };
    const addQuizQuestion = () => {
        setTplQuizQuestions([
            ...tplQuizQuestions,
            { question: '', animalName: 'Từ mới', animalEmoji: '⭐', options: ['', '', ''], answer: '', imageUrl: '', audioUrl: '' }
        ]);
    };
    const handleUploadImageForQuestion = async (qIdx, file) => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('filename', `quiz_img_${Date.now()}_${file.name}`);
        try {
            const res = await api.post('/teacher/upload-image', formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            });
            if (res.data.success) {
                const nextQ = [...tplQuizQuestions];
                nextQ[qIdx].imageUrl = res.data.message;
                setTplQuizQuestions(nextQ);
            }
        }
        catch (err) {
            alert('Không thể tải lên tệp ảnh!');
        }
    };
    const handleUploadAudioForQuestion = async (qIdx, file) => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('filename', `quiz_aud_${Date.now()}_${file.name}`);
        try {
            const res = await api.post('/teacher/upload-audio', formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            });
            if (res.data.success) {
                const nextQ = [...tplQuizQuestions];
                nextQ[qIdx].audioUrl = res.data.message;
                setTplQuizQuestions(nextQ);
            }
        }
        catch (err) {
            alert('Không thể tải lên tệp âm thanh!');
        }
    };
    const updateQuestionAnimalName = (qIdx, text) => {
        const nextQ = [...tplQuizQuestions];
        nextQ[qIdx].animalName = text;
        setTplQuizQuestions(nextQ);
    };
    const updateQuestionOption = (qIdx, optIdx, text) => {
        const nextQ = [...tplQuizQuestions];
        nextQ[qIdx].options[optIdx] = text;
        setTplQuizQuestions(nextQ);
    };
    const updateQuestionAnswer = (qIdx, ans) => {
        const nextQ = [...tplQuizQuestions];
        nextQ[qIdx].answer = ans;
        setTplQuizQuestions(nextQ);
    };
    const handleOpenGradingModal = (sub) => {
        setGradingSub(sub);
        setGradeScore(sub.score !== null ? sub.score.toString() : '');
        setGradeFeedback(sub.feedback || '');
    };
    const handleGradeSubmission = async (e) => {
        e.preventDefault();
        if (!gradingSub)
            return;
        setGradingLoading(true);
        try {
            await api.post(`/teacher/submissions/${gradingSub.id}/grade`, {
                score: parseFloat(gradeScore),
                feedback: gradeFeedback
            });
            setGradingSub(null);
            if (selectedAsm) {
                handleSelectAssignment(selectedAsm);
            }
        }
        catch (err) {
            alert('Lỗi chấm điểm bài tập');
        }
        finally {
            setGradingLoading(false);
        }
    };
    const writingTemplates = templates.filter(t => t.contentType === 'WRITING');
    const quizTemplates = templates.filter(t => t.contentType === 'QUIZ');
    if (loading) {
        return (<div className="flex items-center justify-center min-h-[50vh]">
        <div className="w-12 h-12 border-4 border-sky-200 border-t-sky-500 rounded-full animate-spin"></div>
      </div>);
    }
    return (<div className="space-y-6 p-2" style={{ fontFamily: "'Nunito', sans-serif" }}>

      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-gradient-to-r from-sky-400 to-blue-500 rounded-3xl p-6 shadow-lg shadow-sky-200/50">
        <div className="flex items-center gap-4">
          <div className="w-14 h-14 rounded-2xl bg-white/20 backdrop-blur flex items-center justify-center border-2 border-white/40">
            <ClipboardList className="w-7 h-7 text-white"/>
          </div>
          <div>
            <h1 className="text-xl sm:text-2xl font-black text-white">Quản lý & Giao bài tập</h1>
            <p className="text-sky-100 text-sm font-bold mt-0.5">Cô có thể chỉnh sửa hạn nộp bài tập hoặc xóa bài tập đã giao dễ dàng</p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <button onClick={() => { setShowAssignModal(true); setAssignError(''); }} disabled={classes.length === 0} className="px-5 py-3 rounded-2xl bg-white text-sky-600 font-black text-sm shadow-md hover:shadow-lg active:scale-95 transition-all flex items-center gap-2 disabled:opacity-50">
            <Send className="w-4 h-4"/>
            Giao bài từ Kho mẫu
          </button>
        </div>
      </div>

      {/* Navigation Main Tabs */}
      <div className="flex items-center justify-between border-b-2 border-sky-100 pb-2">
        <div className="flex items-center gap-3">
          <button onClick={() => setActiveTab('ASSIGNED')} className={`px-5 py-3 rounded-2xl font-black text-sm flex items-center gap-2 transition-all ${activeTab === 'ASSIGNED'
            ? 'bg-sky-500 text-white shadow-md shadow-sky-200'
            : 'bg-white text-slate-500 hover:bg-sky-50 border-2 border-sky-100'}`}>
            <ClipboardList className="w-4 h-4"/>
            Bài tập đã giao ({assignments.length})
          </button>

          <button onClick={() => setActiveTab('TEMPLATES')} className={`px-5 py-3 rounded-2xl font-black text-sm flex items-center gap-2 transition-all ${activeTab === 'TEMPLATES'
            ? 'bg-sky-500 text-white shadow-md shadow-sky-200'
            : 'bg-white text-slate-500 hover:bg-sky-50 border-2 border-sky-100'}`}>
            <Layers className="w-4 h-4"/>
            Kho bài tập mẫu ({templates.length})
          </button>
        </div>

        {activeTab === 'ASSIGNED' && (<div className="flex items-center gap-2">
            <span className="text-xs font-black text-sky-700 hidden sm:inline">Chọn lớp:</span>
            <select value={selectedClassId} onChange={(e) => handleClassChange(Number(e.target.value))} className="py-2.5 px-4 bg-white border-2 border-sky-200 rounded-xl text-slate-700 font-extrabold text-xs focus:outline-none focus:border-sky-400">
              {classes.map((c) => (<option key={c.id} value={c.id}>{c.name}</option>))}
            </select>
          </div>)}
      </div>

      {/* TAB 1: BÀI TẬP ĐÃ GIAO & BÀI NỘP */}
      {activeTab === 'ASSIGNED' && (<div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="space-y-3">
            <h2 className="text-base font-black text-sky-800 flex items-center gap-2">
              <FileText className="w-5 h-5 text-sky-500"/>
              Bài tập của lớp
            </h2>

            {assignments.length === 0 ? (<div className="bg-white border-3 border-sky-100 rounded-3xl p-8 text-center">
                <ClipboardList className="w-10 h-10 text-sky-200 mx-auto mb-3"/>
                <p className="text-slate-500 text-sm font-bold">Chưa có bài tập nào được giao cho lớp này.</p>
                <button onClick={() => setShowAssignModal(true)} className="mt-3 px-4 py-2 rounded-xl bg-sky-50 text-sky-600 font-black text-xs border border-sky-200 hover:bg-sky-100">
                  Giao bài từ Kho mẫu ngay
                </button>
              </div>) : (assignments.map((asm) => {
                const isSelected = selectedAsm?.id === asm.id;
                const info = parseContentInfo(asm.contentType, asm.contentData);
                return (<div key={asm.id} onClick={() => handleSelectAssignment(asm)} className={`w-full text-left p-4 rounded-2xl border-3 transition-all duration-200 cursor-pointer ${isSelected
                        ? 'bg-sky-50 border-sky-400 shadow-md shadow-sky-100'
                        : 'bg-white border-sky-100 hover:border-sky-300 hover:bg-sky-50/50'}`}>
                    <div className="flex justify-between items-start gap-2">
                      <h3 className={`font-black text-sm ${isSelected ? 'text-sky-700' : 'text-slate-700'}`}>
                        {asm.title}
                      </h3>
                      <div className="flex items-center gap-1 shrink-0">
                        <span className={`text-[10px] uppercase font-black px-2 py-0.5 rounded-full ${asm.contentType === 'QUIZ' ? 'bg-purple-100 text-purple-600' : 'bg-pink-100 text-pink-600'}`}>
                          {asm.contentType === 'QUIZ' ? 'Trắc nghiệm' : 'Luyện tô'}
                        </span>

                        {/* Nút chỉnh sửa hạn nộp & xóa bài tập đã giao */}
                        <button onClick={(e) => handleOpenEditAssignedModal(asm, e)} className="p-1 text-sky-500 hover:bg-sky-100 rounded-lg transition-colors" title="Sửa hạn nộp bài tập">
                          <Calendar className="w-3.5 h-3.5"/>
                        </button>
                        <button onClick={(e) => handleDeleteAssignedAssignment(asm.id, e)} className="p-1 text-slate-300 hover:text-rose-500 rounded-lg transition-colors" title="Xóa bài tập này khỏi lớp">
                          <Trash2 className="w-3.5 h-3.5"/>
                        </button>
                      </div>
                    </div>

                    {asm.contentType === 'WRITING' && (<div className="mt-2 flex flex-wrap items-center gap-1.5 text-[11px] font-extrabold text-sky-600">
                        <span className="bg-sky-100 px-2 py-0.5 rounded-md">Bộ chữ: {info.letters.join(', ')}</span>
                        <span className="bg-amber-100 text-amber-800 px-2 py-0.5 rounded-md flex items-center gap-1">
                          <Target className="w-3 h-3"/> Chỉ tiêu: ≥{info.targetScore}%
                        </span>
                      </div>)}

                    <p className="text-slate-400 text-xs mt-1 line-clamp-2">{asm.description || 'Không có mô tả'}</p>

                    <div className="flex items-center justify-between mt-3 text-[11px] font-bold text-slate-400">
                      <span className="flex items-center gap-1 text-sky-600 font-black">
                        <Clock className="w-3.5 h-3.5 text-sky-500"/>
                        Hạn: {asm.dueDate ? new Date(asm.dueDate).toLocaleString('vi-VN') : 'Không giới hạn'}
                      </span>
                      <ChevronRight className={`w-4 h-4 ${isSelected ? 'text-sky-500' : 'text-slate-300'}`}/>
                    </div>
                  </div>);
            }))}
          </div>

          {/* Cột chi tiết bài nộp học sinh */}
          <div className="lg:col-span-2">
            {selectedAsm ? (<div className="bg-white border-3 border-sky-100 rounded-3xl p-6 shadow-sm space-y-5">
                <div className="pb-4 border-b-2 border-sky-100 flex items-center justify-between">
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="text-lg font-black text-sky-800">{selectedAsm.title}</h3>
                      <button onClick={() => handleOpenEditAssignedModal(selectedAsm)} className="px-2 py-1 bg-sky-50 text-sky-600 rounded-lg text-xs font-black border border-sky-200 hover:bg-sky-100 flex items-center gap-1" title="Đổi hạn nộp">
                        <Calendar className="w-3.5 h-3.5"/> Đổi hạn nộp
                      </button>
                      <button onClick={() => handleDeleteAssignedAssignment(selectedAsm.id)} className="px-2 py-1 bg-rose-50 text-rose-500 rounded-lg text-xs font-black border border-rose-200 hover:bg-rose-100 flex items-center gap-1" title="Xóa bài tập">
                        <Trash2 className="w-3.5 h-3.5"/> Xóa bài
                      </button>
                    </div>
                    <p className="text-slate-400 text-xs font-bold mt-1">
                      {selectedAsm.description || 'Không có mô tả'} • <span className="text-sky-600">Hạn nộp: {selectedAsm.dueDate ? new Date(selectedAsm.dueDate).toLocaleString('vi-VN') : 'Không hạn'}</span>
                    </p>
                  </div>
                  <span className="text-xs font-black text-sky-600 bg-sky-50 px-3 py-1.5 rounded-xl border border-sky-100">
                    Đã nộp: {submissions.length} bài
                  </span>
                </div>

                {submissions.length === 0 ? (<div className="py-12 text-center bg-sky-50/50 rounded-2xl border-2 border-dashed border-sky-200">
                    <Clock className="w-8 h-8 text-sky-200 mx-auto mb-2"/>
                    <p className="text-slate-400 text-sm font-bold">Chưa có học sinh nào nộp bài tập này.</p>
                  </div>) : (<div className="space-y-3">
                    {submissions.map((sub) => (<div key={sub.id} className="p-4 rounded-2xl border-2 border-sky-100 bg-white hover:border-sky-200 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 transition-all">
                        <div>
                          <p className="font-black text-sm text-slate-700">{sub.studentName}</p>
                          <p className="text-slate-400 text-xs font-bold mt-0.5">
                            Nộp lúc: {new Date(sub.submittedAt).toLocaleString('vi-VN')}
                          </p>
                        </div>

                        <div className="flex items-center gap-3">
                          {sub.score !== null ? (<span className="text-xs text-teal-700 bg-teal-50 border border-teal-200 px-3 py-1 rounded-full font-black">
                              Đã chấm: {sub.score} điểm
                            </span>) : (<span className="text-xs text-amber-700 bg-amber-50 border border-amber-200 px-3 py-1 rounded-full font-black">
                              Chưa chấm
                            </span>)}

                          <button onClick={() => handleOpenGradingModal(sub)} className="px-4 py-2 rounded-xl bg-gradient-to-r from-sky-400 to-blue-500 text-white text-xs font-black shadow-md hover:shadow-lg active:scale-95 transition-all flex items-center gap-1.5">
                            <Eye className="w-4 h-4"/>
                            Xem & Chấm bài
                          </button>
                        </div>
                      </div>))}
                  </div>)}
              </div>) : (<div className="bg-white border-3 border-sky-100 rounded-3xl p-10 text-center flex flex-col items-center justify-center min-h-[350px]">
                <div className="w-16 h-16 rounded-2xl bg-sky-50 flex items-center justify-center mb-4">
                  <ClipboardList className="w-8 h-8 text-sky-300"/>
                </div>
                <p className="text-slate-400 font-bold text-sm">Chọn một bài tập ở cột bên trái để quản lý danh sách bài nộp</p>
              </div>)}
          </div>
        </div>)}

      {/* TAB 2: KHO BÀI TẬP MẪU */}
      {activeTab === 'TEMPLATES' && (<div className="space-y-5">
          <div className="flex items-center justify-between bg-sky-50/60 p-2 rounded-2xl border-2 border-sky-100">
            <div className="flex items-center gap-2">
              <button onClick={() => setKhoSubTab('WRITING')} className={`px-5 py-2.5 rounded-xl font-black text-xs sm:text-sm flex items-center gap-2 transition-all ${khoSubTab === 'WRITING'
                ? 'bg-pink-500 text-white shadow-md shadow-pink-200'
                : 'bg-white text-slate-600 hover:bg-sky-100 border border-sky-100'}`}>
                <PenTool className="w-4 h-4"/>
                Phần Luyện tô viết chữ ({writingTemplates.length})
              </button>

              <button onClick={() => setKhoSubTab('QUIZ')} className={`px-5 py-2.5 rounded-xl font-black text-xs sm:text-sm flex items-center gap-2 transition-all ${khoSubTab === 'QUIZ'
                ? 'bg-purple-500 text-white shadow-md shadow-purple-200'
                : 'bg-white text-slate-600 hover:bg-sky-100 border border-sky-100'}`}>
                <HelpCircle className="w-4 h-4"/>
                Phần Bài tập trắc nghiệm con vật ({quizTemplates.length})
              </button>
            </div>

            <button onClick={() => handleOpenCreateModal(khoSubTab)} className="px-4 py-2.5 rounded-xl bg-sky-500 hover:bg-sky-600 text-white font-black text-xs shadow-md transition-all flex items-center gap-1.5">
              <Plus className="w-4 h-4"/>
              Tạo bài {khoSubTab === 'WRITING' ? 'luyện tô' : 'trắc nghiệm'} mới
            </button>
          </div>

          {khoSubTab === 'WRITING' && (<div>
              {writingTemplates.length === 0 ? (<div className="bg-white border-3 border-sky-100 rounded-3xl p-10 text-center">
                  <PenTool className="w-12 h-12 text-pink-200 mx-auto mb-3"/>
                  <p className="text-slate-500 text-sm font-bold">Chưa có bài tập luyện tô viết chữ nào trong Kho.</p>
                  <button onClick={() => handleOpenCreateModal('WRITING')} className="mt-3 px-4 py-2 rounded-xl bg-sky-500 text-white font-black text-xs shadow-md">
                    Tạo bài luyện tô ngay
                  </button>
                </div>) : (<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                  {writingTemplates.map((tpl) => {
                        const info = parseContentInfo(tpl.contentType, tpl.contentData);
                        return (<div key={tpl.id} className="bg-white border-3 border-pink-100 hover:border-pink-300 rounded-3xl p-5 shadow-sm hover:shadow-md transition-all flex flex-col justify-between">
                        <div>
                          <div className="flex items-start justify-between gap-2 mb-2">
                            <span className="text-[10px] uppercase font-black px-2.5 py-0.5 rounded-full bg-pink-100 text-pink-600">
                              Luyện tô ({info.letters.length} chữ)
                            </span>
                            <div className="flex items-center gap-1">
                              <button onClick={() => handleOpenEditModal(tpl)} className="p-1.5 text-sky-500 hover:bg-sky-50 rounded-lg transition-colors" title="Chỉnh sửa bài tập này">
                                <Edit3 className="w-4 h-4"/>
                              </button>
                              <button onClick={() => handleDeleteTemplate(tpl.id)} className="p-1.5 text-slate-300 hover:text-rose-500 rounded-lg transition-colors" title="Xóa bài tập mẫu">
                                <Trash2 className="w-4 h-4"/>
                              </button>
                            </div>
                          </div>

                          <h3 onClick={() => handleOpenEditModal(tpl)} className="font-black text-base text-slate-700 hover:text-sky-600 cursor-pointer">
                            {tpl.title}
                          </h3>

                          <div className="mt-2.5 flex flex-wrap items-center gap-1.5 text-[11px] font-extrabold">
                            <span className="bg-sky-50 text-sky-600 px-2 py-0.5 rounded-md border border-sky-100">
                              Bộ chữ: {info.letters.join(', ')}
                            </span>
                            <span className="bg-amber-50 text-amber-700 px-2 py-0.5 rounded-md border border-amber-100 flex items-center gap-1">
                              <Target className="w-3 h-3 text-amber-500"/> Chỉ tiêu: ≥{info.targetScore}%
                            </span>
                          </div>

                          <p className="text-slate-400 text-xs font-bold mt-2 line-clamp-3">{tpl.description}</p>
                        </div>

                        <div className="mt-5 pt-3 border-t-2 border-sky-50 flex items-center justify-between">
                          <button onClick={() => handleOpenEditModal(tpl)} className="text-[11px] font-bold text-sky-500 hover:underline flex items-center gap-1">
                            <Edit3 className="w-3 h-3"/> Sửa nội dung
                          </button>
                          <button onClick={() => handleOpenAssignModalWithTemplate(tpl.id)} className="px-3.5 py-2 rounded-xl bg-gradient-to-r from-sky-400 to-blue-500 text-white text-xs font-black shadow-sm active:scale-95 transition-all flex items-center gap-1">
                            <Send className="w-3.5 h-3.5"/>
                            Giao bài này
                          </button>
                        </div>
                      </div>);
                    })}
                </div>)}
            </div>)}

          {khoSubTab === 'QUIZ' && (<div>
              {quizTemplates.length === 0 ? (<div className="bg-white border-3 border-sky-100 rounded-3xl p-10 text-center">
                  <HelpCircle className="w-12 h-12 text-purple-200 mx-auto mb-3"/>
                  <p className="text-slate-500 text-sm font-bold">Chưa có bài tập trắc nghiệm nào trong Kho.</p>
                  <button onClick={() => handleOpenCreateModal('QUIZ')} className="mt-4 px-4 py-2 rounded-xl bg-purple-500 text-white font-black text-xs shadow-md">
                    Tạo bài trắc nghiệm mới
                  </button>
                </div>) : (<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                  {quizTemplates.map((tpl) => (<div key={tpl.id} className="bg-white border-3 border-purple-100 hover:border-purple-300 rounded-3xl p-5 shadow-sm hover:shadow-md transition-all flex flex-col justify-between">
                      <div>
                        <div className="flex items-start justify-between gap-2 mb-2">
                          <span className="text-[10px] uppercase font-black px-2.5 py-0.5 rounded-full bg-purple-100 text-purple-600">
                            Trắc nghiệm con vật 🔊
                          </span>
                          <div className="flex items-center gap-1">
                            <button onClick={() => handleOpenEditModal(tpl)} className="p-1.5 text-sky-500 hover:bg-sky-50 rounded-lg transition-colors" title="Chỉnh sửa bài tập này">
                              <Edit3 className="w-4 h-4"/>
                            </button>
                            <button onClick={() => handleDeleteTemplate(tpl.id)} className="p-1.5 text-slate-300 hover:text-rose-500 rounded-lg transition-colors" title="Xóa bài tập mẫu">
                              <Trash2 className="w-4 h-4"/>
                            </button>
                          </div>
                        </div>

                        <h3 onClick={() => handleOpenEditModal(tpl)} className="font-black text-base text-slate-700 hover:text-purple-600 cursor-pointer">
                          {tpl.title}
                        </h3>

                        <p className="text-slate-400 text-xs font-bold mt-2 line-clamp-3">{tpl.description}</p>
                      </div>

                      <div className="mt-5 pt-3 border-t-2 border-purple-50 flex items-center justify-between">
                        <button onClick={() => handleOpenEditModal(tpl)} className="text-[11px] font-bold text-purple-600 hover:underline flex items-center gap-1">
                          <Edit3 className="w-3 h-3"/> Sửa nội dung
                        </button>
                        <button onClick={() => handleOpenAssignModalWithTemplate(tpl.id)} className="px-3.5 py-2 rounded-xl bg-gradient-to-r from-purple-500 to-indigo-600 text-white text-xs font-black shadow-sm active:scale-95 transition-all flex items-center gap-1">
                          <Send className="w-3.5 h-3.5"/>
                          Giao bài này
                        </button>
                      </div>
                    </div>))}
                </div>)}
            </div>)}
        </div>)}

      {/* MODAL: CHỈNH SỬA HẠN NỘP & BÀI TẬP ĐÃ GIAO */}
      {showEditAssignedModal && editingAssignedItem && (<div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4">
          <form onSubmit={handleSaveEditAssignedAssignment} className="w-full max-w-md bg-white rounded-3xl shadow-2xl overflow-hidden border-4 border-sky-100">
            <div className="bg-gradient-to-r from-sky-400 to-blue-500 px-6 py-4 flex items-center justify-between">
              <h3 className="font-black text-white text-base flex items-center gap-2">
                <Calendar className="w-5 h-5"/> Đổi hạn nộp & Sửa bài tập đã giao
              </h3>
              <button type="button" onClick={() => setShowEditAssignedModal(false)} className="p-1.5 rounded-xl bg-white/20 text-white hover:bg-white/30">
                <X className="w-5 h-5"/>
              </button>
            </div>

            <div className="p-6 space-y-4">
              {editAssignedError && (<div className="p-3 bg-rose-50 border-2 border-rose-200 rounded-2xl text-xs font-bold text-rose-600 flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0"/>
                  <span>{editAssignedError}</span>
                </div>)}

              <div>
                <label className="block text-xs font-black text-sky-700 uppercase mb-1.5">Tên bài tập</label>
                <input type="text" value={editAssignedTitle} onChange={(e) => setEditAssignedTitle(e.target.value)} className="w-full py-3 px-4 bg-white border-2 border-sky-100 focus:border-sky-400 focus:outline-none rounded-2xl text-slate-700 font-bold text-sm" required/>
              </div>

              <div>
                <label className="block text-xs font-black text-sky-700 uppercase mb-1.5">Mô tả bài tập</label>
                <textarea value={editAssignedDesc} onChange={(e) => setEditAssignedDesc(e.target.value)} className="w-full py-3 px-4 bg-white border-2 border-sky-100 focus:border-sky-400 focus:outline-none rounded-2xl text-slate-700 font-bold text-sm h-20 resize-none"/>
              </div>

              <div>
                <label className="block text-xs font-black text-sky-700 uppercase mb-1.5 flex items-center gap-1">
                  <Clock className="w-4 h-4 text-sky-500"/> Hạn nộp bài tập mới
                </label>
                <input type="datetime-local" value={editAssignedDueDate} onChange={(e) => setEditAssignedDueDate(e.target.value)} className="w-full py-3 px-4 bg-white border-2 border-sky-200 focus:border-sky-400 focus:outline-none rounded-2xl text-slate-700 font-bold text-sm"/>
                <p className="text-[10px] text-slate-400 font-bold mt-1">Để trống nếu không muốn đặt giới hạn thời gian nộp.</p>
              </div>
            </div>

            <div className="p-5 border-t-2 border-sky-100 flex gap-3">
              <button type="button" onClick={() => setShowEditAssignedModal(false)} className="flex-1 py-3 rounded-2xl bg-sky-50 border border-sky-100 text-slate-500 font-black text-xs">
                Hủy bỏ
              </button>
              <button type="submit" disabled={editAssignedLoading} className="flex-1 py-3 rounded-2xl bg-gradient-to-r from-sky-400 to-blue-500 text-white font-black text-xs shadow-md active:scale-95 transition-all flex items-center justify-center gap-2 disabled:opacity-50">
                {editAssignedLoading ? 'Đang lưu...' : 'Lưu thay đổi ✨'}
              </button>
            </div>
          </form>
        </div>)}

      {/* MODAL: TẠO HOẶC CHỈNH SỬA BÀI TẬP MẪU VÀO KHO */}
      {showTemplateModal && (<div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4">
          <form onSubmit={handleSaveTemplate} className="w-full max-w-xl bg-white rounded-3xl shadow-2xl overflow-hidden border-4 border-sky-100 flex flex-col max-h-[90vh]">
            <div className="bg-gradient-to-r from-sky-400 to-blue-500 px-6 py-4 flex items-center justify-between">
              <h3 className="font-black text-white text-base flex items-center gap-2">
                {editingTemplateId ? <Edit3 className="w-5 h-5"/> : <Layers className="w-5 h-5"/>}
                {editingTemplateId ? 'Chỉnh sửa bài tập mẫu' : 'Soạn bài tập mẫu mới vào Kho'}
              </h3>
              <button type="button" onClick={() => setShowTemplateModal(false)} className="p-1.5 rounded-xl bg-white/20 text-white hover:bg-white/30">
                <X className="w-5 h-5"/>
              </button>
            </div>

            <div className="p-6 space-y-4 overflow-y-auto flex-1">
              <div>
                <label className="block text-xs font-black text-sky-700 uppercase mb-1.5">Tên bài tập mẫu <span className="text-rose-500">*</span></label>
                <input type="text" placeholder="Ví dụ: Bài 1 Trắc nghiệm: Nghe đọc tên con vật (Gà, Mèo, Bò)" value={tplTitle} onChange={(e) => setTplTitle(e.target.value)} className="w-full py-3 px-4 bg-white border-2 border-sky-100 focus:border-sky-400 focus:outline-none rounded-2xl text-slate-700 font-bold text-sm" required/>
              </div>

              <div>
                <label className="block text-xs font-black text-sky-700 uppercase mb-1.5">Mô tả bài tập</label>
                <textarea placeholder="Nhập hướng dẫn cho học sinh..." value={tplDesc} onChange={(e) => setTplDesc(e.target.value)} className="w-full py-3 px-4 bg-white border-2 border-sky-100 focus:border-sky-400 focus:outline-none rounded-2xl text-slate-700 font-bold text-sm h-20 resize-none"/>
              </div>



              {tplType === 'WRITING' ? (<div className="space-y-4 bg-sky-50/50 p-4 rounded-2xl border-2 border-sky-100">
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <label className="block text-xs font-black text-sky-800 uppercase">
                        1. Chọn các chữ cái cho bé luyện (Chọn nhiều chữ) <span className="text-rose-500">*</span>
                      </label>
                      <span className="text-[11px] font-black text-sky-600">Đã chọn: {tplSelectedLetters.length} chữ</span>
                    </div>

                    <div className="flex flex-wrap gap-1.5 max-h-40 overflow-y-auto p-2 bg-white rounded-xl border border-sky-200">
                      {ALL_VIETNAMESE_LETTERS.map((l) => {
                    const isSelected = tplSelectedLetters.includes(l);
                    return (<button key={l} type="button" onClick={() => toggleLetterSelection(l)} className={`w-9 h-9 rounded-xl font-black text-xs transition-all flex items-center justify-center border-2 ${isSelected
                            ? 'bg-sky-500 text-white border-sky-500 shadow-sm scale-105'
                            : 'bg-white text-slate-600 border-sky-100 hover:border-sky-300'}`}>
                            {l}
                          </button>);
                })}
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-black text-sky-800 uppercase mb-1.5 flex items-center gap-1.5">
                      <Target className="w-4 h-4 text-amber-500"/>
                      2. Chỉ tiêu độ chính xác cần đạt (% nét chuẩn) <span className="text-rose-500">*</span>
                    </label>
                    <div className="flex items-center gap-3">
                      <input type="range" min="50" max="95" step="5" value={tplTargetScore} onChange={(e) => setTplTargetScore(Number(e.target.value))} className="flex-1 accent-sky-500"/>
                      <span className="px-3 py-1.5 rounded-xl bg-amber-100 border border-amber-200 text-amber-900 font-black text-sm shrink-0">
                        ≥ {tplTargetScore}%
                      </span>
                    </div>
                  </div>
                </div>) : (<div className="space-y-4">
                  <div className="flex items-center justify-between border-t-2 border-sky-100 pt-3">
                    <label className="text-xs font-black text-sky-700 uppercase">Soạn các câu hỏi trắc nghiệm con vật</label>
                    <button type="button" onClick={addQuizQuestion} className="px-3 py-1.5 rounded-xl bg-purple-50 text-purple-600 text-xs font-black border border-purple-200">
                      + Thêm câu hỏi
                    </button>
                  </div>

                  {tplQuizQuestions.map((q, qIdx) => (<div key={qIdx} className="p-4 rounded-2xl border-2 border-purple-100 bg-purple-50/40 space-y-3">
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-xs font-black text-purple-800">Câu hỏi {qIdx + 1}:</span>

                        <div className="flex items-center gap-1.5">
                          <button type="button" onClick={() => {
                        if (q.audioUrl) {
                            new Audio(q.audioUrl).play().catch(() => speakAnimalName(q.animalName || 'Từ mới'));
                        }
                        else {
                            speakAnimalName(q.animalName || 'Từ mới');
                        }
                    }} className="px-2.5 py-1 bg-purple-100 text-purple-700 rounded-lg text-xs font-bold flex items-center gap-1 hover:bg-purple-200">
                            <Volume2 className="w-3.5 h-3.5"/> Nghe thử
                          </button>

                          <button type="button" onClick={() => {
                        const nextQ = tplQuizQuestions.filter((_, idx) => idx !== qIdx);
                        setTplQuizQuestions(nextQ.length > 0 ? nextQ : [{ question: '', animalName: 'Từ mới', animalEmoji: '⭐', options: ['', '', ''], answer: '', imageUrl: '', audioUrl: '' }]);
                    }} className="p-1.5 hover:bg-rose-100 text-rose-500 rounded-lg transition-all" title="Xóa câu hỏi này">
                            <Trash2 className="w-4 h-4"/>
                          </button>
                        </div>
                      </div>

                      <div className="grid grid-cols-2 gap-3">
                        {/* Từ hiển thị */}
                        <div className="flex flex-col justify-center">
                          <span className="text-[10px] font-bold text-slate-500">Từ hiển thị / phát âm:</span>
                          <input type="text" placeholder="Ví dụ: Con Gà" value={q.animalName || ''} onChange={(e) => updateQuestionAnimalName(qIdx, e.target.value)} className="w-full py-2.5 px-3 bg-white border border-purple-200 rounded-xl text-xs font-bold mt-1.5" required/>
                        </div>

                        {/* Hình ảnh */}
                        <div>
                          <span className="text-[10px] font-bold text-slate-500 block">Hình ảnh minh họa tùy chọn:</span>
                          {q.imageUrl ? (<div className="relative w-20 h-20 rounded-xl overflow-hidden border-2 border-purple-200 mt-1 flex items-center justify-center bg-white group mx-auto">
                              <img src={q.imageUrl} className="w-full h-full object-cover"/>
                              <button type="button" onClick={() => {
                            const nextQ = [...tplQuizQuestions];
                            nextQ[qIdx].imageUrl = '';
                            setTplQuizQuestions(nextQ);
                        }} className="absolute inset-0 bg-rose-500/80 text-white flex items-center justify-center text-[10px] font-black opacity-0 group-hover:opacity-100 transition-all">
                                Xóa ảnh
                              </button>
                            </div>) : (<label className="flex flex-col items-center justify-center border-2 border-dashed border-purple-200 rounded-2xl p-2.5 mt-1 cursor-pointer hover:bg-purple-100/50 transition-all text-purple-600">
                              <Plus className="w-4 h-4 mb-0.5"/>
                              <span className="text-[9px] font-black">Chọn ảnh</span>
                              <input type="file" accept="image/*" className="hidden" onChange={(e) => {
                            if (e.target.files && e.target.files[0]) {
                                handleUploadImageForQuestion(qIdx, e.target.files[0]);
                            }
                        }}/>
                            </label>)}
                        </div>
                      </div>

                      {/* Âm thanh */}
                      <div className="p-2.5 bg-sky-50/50 border border-sky-100 rounded-2xl">
                        <span className="text-[10px] font-bold text-slate-500 block">Âm thanh tự chọn (ghi âm/file đọc):</span>
                        {q.audioUrl ? (<div className="flex items-center justify-between gap-2 mt-1">
                            <audio src={q.audioUrl} controls className="h-6 max-w-[170px] scale-90 origin-left"/>
                            <button type="button" onClick={() => {
                            const nextQ = [...tplQuizQuestions];
                            nextQ[qIdx].audioUrl = '';
                            setTplQuizQuestions(nextQ);
                        }} className="px-2 py-1 bg-rose-100 text-rose-600 rounded-lg text-[9px] font-bold hover:bg-rose-200 transition-all">
                              Xóa âm thanh
                            </button>
                          </div>) : (<label className="flex items-center gap-1.5 px-3 py-1.5 bg-white border border-sky-200 hover:bg-sky-50 rounded-xl cursor-pointer mt-1 text-[10px] font-black text-sky-700 w-fit">
                            <Upload className="w-3.5 h-3.5"/> Chọn file ghi âm (.mp3/.wav)
                            <input type="file" accept="audio/*" className="hidden" onChange={(e) => {
                            if (e.target.files && e.target.files[0]) {
                                handleUploadAudioForQuestion(qIdx, e.target.files[0]);
                            }
                        }}/>
                          </label>)}
                      </div>

                      <div className="grid grid-cols-3 gap-2">
                        {q.options?.map((opt, optIdx) => (<div key={optIdx}>
                            <span className="text-[10px] font-bold text-slate-400">Đáp án {optIdx + 1}:</span>
                            <input type="text" placeholder="Lựa chọn..." value={opt} onChange={(e) => updateQuestionOption(qIdx, optIdx, e.target.value)} className="w-full py-1.5 px-2 bg-white border border-purple-200 rounded-lg text-xs font-bold mt-0.5 text-center" required/>
                          </div>))}
                      </div>

                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-slate-500">Đáp án chữ cái đúng:</span>
                        <select value={q.answer} onChange={(e) => updateQuestionAnswer(qIdx, e.target.value)} className="py-1 px-3 bg-white border border-purple-200 rounded-lg text-xs font-black text-purple-700" required>
                          <option value="">Chọn đáp án đúng...</option>
                          {q.options?.map((opt) => opt && (<option key={opt} value={opt}>{opt}</option>))}
                        </select>
                      </div>
                    </div>))}
                </div>)}
            </div>

            <div className="p-5 border-t-2 border-sky-100 flex gap-3">
              <button type="button" onClick={() => setShowTemplateModal(false)} className="flex-1 py-3 rounded-2xl bg-sky-50 border border-sky-100 text-slate-500 font-black text-xs">
                Hủy bỏ
              </button>
              <button type="submit" className="flex-1 py-3 rounded-2xl bg-gradient-to-r from-sky-400 to-blue-500 text-white font-black text-xs shadow-md flex items-center justify-center gap-1.5">
                <Save className="w-4 h-4"/>
                {editingTemplateId ? 'Lưu thay đổi' : 'Lưu vào Kho mẫu'}
              </button>
            </div>
          </form>
        </div>)}

      {/* MODAL: GIAO BÀI TẬP TỪ KHO CHO LỚP HỌC */}
      {showAssignModal && (<div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4">
          <form onSubmit={handleAssignTemplateToClass} className="w-full max-w-lg bg-white rounded-3xl shadow-2xl overflow-hidden border-4 border-sky-100">
            <div className="bg-gradient-to-r from-sky-400 to-blue-500 px-6 py-4 flex items-center justify-between">
              <h3 className="font-black text-white text-base flex items-center gap-2">
                <Send className="w-5 h-5"/> Giao bài tập cho học sinh
              </h3>
              <button type="button" onClick={() => setShowAssignModal(false)} className="p-1.5 rounded-xl bg-white/20 text-white hover:bg-white/30">
                <X className="w-5 h-5"/>
              </button>
            </div>

            <div className="p-6 space-y-5">
              {assignError && (<div className="p-3 bg-rose-50 border-2 border-rose-200 rounded-2xl text-xs font-bold text-rose-600 flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0"/>
                  <span>{assignError}</span>
                </div>)}

              <div>
                <label className="block text-xs font-black text-sky-700 uppercase mb-1.5">
                  1. Chọn Lớp nhận bài tập <span className="text-rose-500">*</span>
                </label>
                <select value={selectedClassId} onChange={(e) => setSelectedClassId(Number(e.target.value))} className="w-full py-3 px-4 bg-white border-2 border-sky-200 rounded-2xl text-slate-700 font-bold text-sm focus:outline-none focus:border-sky-400" required>
                  <option value="">-- Chọn lớp học --</option>
                  {classes.map((c) => (<option key={c.id} value={c.id}>{c.name}</option>))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-black text-sky-700 uppercase mb-1.5">
                  2. Chọn bài tập từ Kho mẫu <span className="text-rose-500">*</span>
                </label>
                <select value={selectedTemplateId} onChange={(e) => setSelectedTemplateId(e.target.value)} className="w-full py-3 px-4 bg-white border-2 border-sky-200 rounded-2xl text-slate-700 font-bold text-sm focus:outline-none focus:border-sky-400" required>
                  <option value="">-- Chọn bài tập mẫu --</option>
                  {templates.map((tpl) => (<option key={tpl.id} value={tpl.id}>
                      [{tpl.contentType === 'QUIZ' ? 'Trắc nghiệm con vật 🔊' : 'Luyện tô'}] {tpl.title}
                    </option>))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-black text-sky-700 uppercase mb-1.5">
                  3. Hạn nộp bài tập
                </label>
                <input type="datetime-local" value={assignDueDate} onChange={(e) => setAssignDueDate(e.target.value)} className="w-full py-3 px-4 bg-white border-2 border-sky-200 rounded-2xl text-slate-700 font-bold text-sm focus:outline-none focus:border-sky-400"/>
              </div>
            </div>

            <div className="p-5 border-t-2 border-sky-100 flex gap-3">
              <button type="button" onClick={() => setShowAssignModal(false)} className="flex-1 py-3 rounded-2xl bg-sky-50 border border-sky-100 text-slate-500 font-black text-xs">
                Hủy bỏ
              </button>
              <button type="submit" disabled={assigningLoading} className="flex-1 py-3 rounded-2xl bg-gradient-to-r from-sky-400 to-blue-500 text-white font-black text-xs shadow-md active:scale-95 transition-all flex items-center justify-center gap-2 disabled:opacity-50">
                {assigningLoading ? 'Đang giao...' : 'Xác nhận giao bài 🚀'}
              </button>
            </div>
          </form>
        </div>)}

      {/* MODAL: CHẤM ĐIỂM BÀI NỘP */}
      {gradingSub && (<div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4">
          <form onSubmit={handleGradeSubmission} className="w-full max-w-lg bg-white rounded-3xl shadow-2xl overflow-hidden border-4 border-sky-100 flex flex-col max-h-[90vh]">
            <div className="bg-gradient-to-r from-sky-400 to-blue-500 px-6 py-4 flex items-center justify-between">
              <div>
                <h3 className="font-black text-white text-base">Chấm điểm bài nộp học sinh</h3>
                <p className="text-sky-100 text-xs font-bold">Học sinh: {gradingSub.studentName}</p>
              </div>
              <button type="button" onClick={() => setGradingSub(null)} className="p-1.5 rounded-xl bg-white/20 text-white hover:bg-white/30">
                <X className="w-5 h-5"/>
              </button>
            </div>

            <div className="p-6 space-y-5 overflow-y-auto flex-1">
              <div>
                <span className="text-xs font-black text-sky-700 uppercase">Bài làm của bé:</span>

                {selectedAsm?.contentType === 'QUIZ' ? (<div className="bg-purple-50/60 p-4 rounded-2xl border-2 border-purple-100 text-xs space-y-2 mt-2">
                    <p className="font-black text-purple-800">Các đáp án bé đã chọn:</p>
                    {(() => {
                    try {
                        const parsed = JSON.parse(gradingSub.submissionData);
                        const ansList = Array.isArray(parsed) ? parsed : (parsed.answers || []);
                        return ansList.map((ans, idx) => (<div key={idx} className="bg-white p-2.5 rounded-xl border border-purple-200 flex items-center justify-between">
                            <span className="font-bold text-slate-700">Câu {idx + 1}:</span>
                            <span className="font-black text-purple-700 bg-purple-100 px-3 py-1 rounded-lg">Đáp án: {ans}</span>
                          </div>));
                    }
                    catch {
                        return <p className="text-slate-600 font-bold">{gradingSub.submissionData}</p>;
                    }
                })()}
                  </div>) : (<div className="bg-sky-50/50 p-3.5 rounded-2xl border-2 border-sky-100 mt-2 space-y-2.5">
                    {(() => {
                    const subData = gradingSub.submissionData || '';
                    if (!subData) {
                        return <p className="text-xs text-slate-400 font-bold text-center py-4">Chưa có bài vẽ nào.</p>;
                    }
                    // Nếu là chuỗi URL hoặc base64 ảnh đơn
                    if (subData.startsWith('data:image') || subData.startsWith('http')) {
                        return (<div className="bg-white p-3 rounded-2xl border-2 border-sky-200 flex flex-col items-center justify-center max-w-[280px] mx-auto shadow-sm">
                            <img src={subData} alt="Bài vẽ học sinh" className="w-56 h-56 object-contain rounded-xl"/>
                          </div>);
                    }
                    // Nếu là chuỗi JSON (Mảng các chữ cái đã tô [ {letter, image}, ... ])
                    try {
                        const parsed = JSON.parse(subData);
                        if (Array.isArray(parsed) && parsed.length > 0 && parsed[0].image) {
                            return (<div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5 max-h-72 overflow-y-auto p-1">
                              {parsed.map((item, idx) => (<div key={idx} className="bg-white p-2.5 rounded-2xl border-2 border-sky-200 flex flex-col items-center shadow-sm hover:border-sky-400 transition-all">
                                  <span className="text-[11px] font-black text-sky-700 bg-sky-100 px-2.5 py-0.5 rounded-lg mb-1.5">
                                    Chữ "{item.letter}"
                                  </span>
                                  <div className="w-full h-32 bg-slate-50 rounded-xl flex items-center justify-center border border-slate-100 overflow-hidden">
                                    <img src={item.image} alt={`Chữ ${item.letter}`} className="w-full h-full object-contain"/>
                                  </div>
                                </div>))}
                            </div>);
                        }
                        if (parsed.image) {
                            return (<div className="bg-white p-3 rounded-2xl border-2 border-sky-200 flex flex-col items-center justify-center max-w-[280px] mx-auto shadow-sm">
                              <img src={parsed.image} alt="Bài vẽ học sinh" className="w-56 h-56 object-contain rounded-xl"/>
                            </div>);
                        }
                        return <p className="text-xs text-slate-600 font-bold">{subData}</p>;
                    }
                    catch {
                        return <p className="text-xs text-slate-600 font-bold">{subData}</p>;
                    }
                })()}
                  </div>)}
              </div>

              <div>
                <label className="block text-xs font-black text-sky-700 uppercase mb-1.5">
                  Điểm số (thang điểm 10) <span className="text-rose-500">*</span>
                </label>
                <input type="number" step="0.1" min="0" max="10" placeholder="Nhập điểm số..." value={gradeScore} onChange={(e) => setGradeScore(e.target.value)} className="w-full py-3 px-4 bg-white border-2 border-sky-200 rounded-2xl text-slate-700 font-black text-base text-center focus:outline-none focus:border-sky-400" required/>
              </div>

              <div>
                <label className="block text-xs font-black text-sky-700 uppercase mb-1.5 flex items-center justify-between">
                  <span>Nhận xét của cô dành cho bé</span>
                  <span className="text-[10px] font-bold text-sky-500 normal-case">Chọn nhanh:</span>
                </label>

                {/* 3 gợi ý nhận xét chọn nhanh */}
                <div className="flex flex-wrap gap-1.5 mb-2">
                  <button type="button" onClick={() => setGradeFeedback("Bé hoàn thành bài viết rất tốt, nét chữ đều và đẹp! Cô khen con. 🌟")} className="px-2 py-1 bg-teal-50 hover:bg-teal-100 border border-teal-200 text-teal-700 text-[10px] font-black rounded-lg transition-all">
                    Khen nét chữ đẹp 🌟
                  </button>
                  <button type="button" onClick={() => setGradeFeedback("Bài làm khá tốt, con nhớ chú ý nắn nót thêm một chút cho nét chữ tròn đều hơn nhé! Cố lên. ❤️")} className="px-2 py-1 bg-amber-50 hover:bg-amber-100 border border-amber-200 text-amber-700 text-[10px] font-black rounded-lg transition-all">
                    Nhắc nắn nét ❤️
                  </button>
                  <button type="button" onClick={() => setGradeFeedback("Con làm bài trắc nghiệm rất xuất sắc, tất cả đáp án đều chính xác! ✨")} className="px-2 py-1 bg-sky-50 hover:bg-sky-100 border border-sky-200 text-sky-700 text-[10px] font-black rounded-lg transition-all">
                    Khen trắc nghiệm ✨
                  </button>
                </div>

                <textarea placeholder="Khen ngợi hoặc nhắc nhở bé..." value={gradeFeedback} onChange={(e) => setGradeFeedback(e.target.value)} className="w-full py-3 px-4 bg-white border-2 border-sky-200 rounded-2xl text-slate-700 font-bold text-xs h-24 resize-none focus:outline-none focus:border-sky-400" required/>
              </div>
            </div>

            <div className="p-5 border-t-2 border-sky-100 flex gap-3">
              <button type="button" onClick={() => setGradingSub(null)} className="flex-1 py-3 rounded-2xl bg-sky-50 border border-sky-100 text-slate-500 font-black text-xs">
                Hủy bỏ
              </button>
              <button type="submit" disabled={gradingLoading} className="flex-1 py-3 rounded-2xl bg-gradient-to-r from-sky-400 to-blue-500 text-white font-black text-xs shadow-md active:scale-95 transition-all flex items-center justify-center gap-2 disabled:opacity-50">
                {gradingLoading ? 'Đang lưu...' : 'Hoàn thành chấm điểm ✨'}
              </button>
            </div>
          </form>
        </div>)}

      {/* Reusable Custom Confirmation Modal */}
      {confirmModal.isOpen && (<div className="fixed inset-0 z-[100] bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-sm bg-white rounded-3xl shadow-2xl p-6 border-4 border-rose-50 text-center space-y-5 animate-fade-in">
            <div className={`w-14 h-14 rounded-full mx-auto flex items-center justify-center ${confirmModal.type === 'danger' ? 'bg-rose-50 text-rose-500' : 'bg-amber-50 text-amber-500'}`}>
              <AlertCircle className="w-8 h-8"/>
            </div>

            <div className="space-y-2">
              <h3 className="text-base font-black text-slate-800">{confirmModal.title}</h3>
              <p className="text-slate-500 text-xs font-bold leading-relaxed">{confirmModal.message}</p>
            </div>

            <div className="flex gap-3">
              <button type="button" onClick={() => setConfirmModal(prev => ({ ...prev, isOpen: false }))} className="flex-1 py-3 rounded-2xl bg-slate-50 border border-slate-200 text-slate-500 font-black text-xs cursor-pointer hover:bg-slate-100 transition-all">
                Hủy bỏ
              </button>
              <button type="button" onClick={confirmModal.onConfirm} className={`flex-1 py-3 rounded-2xl text-white font-black text-xs shadow-md cursor-pointer hover:brightness-95 active:scale-95 transition-all ${confirmModal.type === 'danger' ? 'bg-rose-500 shadow-rose-200' : 'bg-amber-500 shadow-amber-200'}`}>
                {confirmModal.confirmText || 'Xác nhận'}
              </button>
            </div>
          </div>
        </div>)}
    </div>);
};
function parseContentInfo(contentType, contentData) {
    if (contentType !== 'WRITING')
        return { letters: [], targetScore: 70 };
    try {
        const data = JSON.parse(contentData);
        if (data.letters && Array.isArray(data.letters)) {
            return { letters: data.letters, targetScore: data.targetScore || 70 };
        }
        if (data.letter) {
            return { letters: [data.letter], targetScore: data.targetScore || 70 };
        }
        return { letters: ['A'], targetScore: 70 };
    }
    catch (e) {
        return { letters: [contentData || 'A'], targetScore: 70 };
    }
}
export default TeacherAssignments;
