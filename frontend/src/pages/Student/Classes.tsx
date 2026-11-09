import React, { useState, useEffect, useRef } from 'react';
import api from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { WritingEvaluationEngine } from './writing/engine';
import type { Stroke } from './writing/engine/types';
import { AlertCircle, BookOpen, PenTool, X, Target, Sparkles, RefreshCw, Volume2 } from 'lucide-react';

interface ClassItem {
  id: number;
  name: string;
  description: string;
  inviteCode: string;
  teacherName: string;
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
}

// Helper lưu trữ tiến trình làm bài luyện viết theo từng chữ vào localStorage
const getLocalProgressKey = (userId: number | undefined, asmId: number) => {
  return `student_asm_progress_${userId || 'guest'}_${asmId}`;
};

const getSavedCompletedLetters = (userId: number | undefined, asmId: number): string[] => {
  try {
    const data = localStorage.getItem(getLocalProgressKey(userId, asmId));
    return data ? JSON.parse(data) : [];
  } catch (e) {
    return [];
  }
};

const saveCompletedLetter = (userId: number | undefined, asmId: number, letter: string) => {
  try {
    const current = getSavedCompletedLetters(userId, asmId);
    if (!current.includes(letter)) {
      const updated = [...current, letter];
      localStorage.setItem(getLocalProgressKey(userId, asmId), JSON.stringify(updated));
    }
  } catch (e) {}
};

const clearSavedProgress = (userId: number | undefined, asmId: number) => {
  try {
    localStorage.removeItem(getLocalProgressKey(userId, asmId));
  } catch (e) {}
};

const saveLetterImage = (userId: number | undefined, asmId: number, letter: string, dataUrl: string) => {
  try {
    localStorage.setItem(`student_img_${userId || 'guest'}_${asmId}_${letter}`, dataUrl);
  } catch (e) {}
};

const clearLetterImages = (userId: number | undefined, asmId: number, letters: string[]) => {
  try {
    letters.forEach(l => localStorage.removeItem(`student_img_${userId || 'guest'}_${asmId}_${l}`));
  } catch (e) {}
};

// Phát âm tên con vật bằng giọng vi-VN
const speakAnimalName = (text: string) => {
  if ('speechSynthesis' in window) {
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'vi-VN';
    utterance.rate = 0.85;
    window.speechSynthesis.speak(utterance);
  }
};

const playQuestionAudio = (q: any) => {
  if (q.audioUrl) {
    const audio = new Audio(q.audioUrl);
    audio.play().catch(err => {
      console.warn("Lỗi phát âm thanh custom, chuyển sang TTS:", err);
      speakAnimalName(q.animalName || 'Con Vật');
    });
  } else {
    speakAnimalName(q.animalName || 'Con Vật');
  }
};

const StudentClasses: React.FC = () => {
  const { user } = useAuth();
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [assignments, setAssignments] = useState<AssignmentItem[]>([]);
  const [inviteCode, setInviteCode] = useState('');
  const [loading, setLoading] = useState(true);
  const [activeClassId, setActiveClassId] = useState<number | null>(null);
  const [assignmentFilter, setAssignmentFilter] = useState<'PENDING' | 'ALL'>('PENDING');

  // Modal làm bài tập
  const [selectedAsm, setSelectedAsm] = useState<AssignmentItem | null>(null);
  const [quizAnswers, setQuizAnswers] = useState<string[]>([]);
  
  // Trạng thái cho Bài tập luyện viết (nhiều chữ + lưu tiến trình + chỉ tiêu)
  const [lettersList, setLettersList] = useState<string[]>(['A']);
  const [completedLetters, setCompletedLetters] = useState<string[]>([]);
  const [currentLetterIndex, setCurrentLetterIndex] = useState<number>(0);
  const [targetScore, setTargetScore] = useState<number>(50);
  const [practiceFeedback, setPracticeFeedback] = useState<string | null>(null);
  const [practiceSuccess, setPracticeSuccess] = useState<boolean>(false);
  const [evaluating, setEvaluating] = useState<boolean>(false);
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [isUppercase, setIsUppercase] = useState<boolean>(false);

  // Canvas cho bài tập luyện viết
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const strokesRef = useRef<Stroke[]>([]);
  const currentStrokeRef = useRef<Stroke>([]);
  const [isDrawing, setIsDrawing] = useState(false);
  const [lineWidth] = useState(15); // Kích thước nét chuẩn 15px như phần luyện tập
  const [showCompletionModal, setShowCompletionModal] = useState(false);

  // Khóa cuộn trang khi làm bài tập để tránh di màn hình
  useEffect(() => {
    if (!selectedAsm) return;

    const originalOverflow = document.body.style.overflow;
    const originalTouchAction = document.body.style.touchAction;

    document.body.style.overflow = 'hidden';

    return () => {
      document.body.style.overflow = originalOverflow;
      document.body.style.touchAction = originalTouchAction;
    };
  }, [selectedAsm]);

  // Thiết lập canvas và chặn triệt để hành vi kéo cuộn màn hình khi vẽ
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (ctx) {
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';
      ctx.strokeStyle = '#0258a1';
    }

    const preventDefaultTouch = (e: TouchEvent) => {
      if (e.cancelable) {
        e.preventDefault();
      }
    };

    canvas.addEventListener('touchstart', preventDefaultTouch, { passive: false });
    canvas.addEventListener('touchmove', preventDefaultTouch, { passive: false });
    canvas.addEventListener('touchend', preventDefaultTouch, { passive: false });

    return () => {
      canvas.removeEventListener('touchstart', preventDefaultTouch);
      canvas.removeEventListener('touchmove', preventDefaultTouch);
      canvas.removeEventListener('touchend', preventDefaultTouch);
    };
  }, [selectedAsm, currentLetterIndex]);

  const playClickSound = () => {
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(523.25, audioCtx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(783.99, audioCtx.currentTime + 0.1);
      gain.gain.setValueAtTime(0.05, audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.12);
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start();
      osc.stop(audioCtx.currentTime + 0.12);
    } catch (e) {}
  };

  const fetchData = async () => {
    try {
      const [classesRes, asmRes] = await Promise.allSettled([
        api.get('/student/class/joined'),
        api.get('/student/assignments')
      ]);
      if (classesRes.status === 'fulfilled') {
        setClasses(classesRes.value.data || []);
      }
      if (asmRes.status === 'fulfilled') {
        setAssignments(asmRes.value.data || []);
      }
    } catch (err) {
      console.error('Không thể tải dữ liệu lớp học', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleJoinClass = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inviteCode) return;
    playClickSound();
    try {
      const res = await api.post(`/student/class/join?inviteCode=${inviteCode}`);
      alert(res.data.message);
      setInviteCode('');
      fetchData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Không thể tham gia lớp học');
    }
  };

  const getCoordinates = (e: any) => {
    const canvas = canvasRef.current;
    if (!canvas) return { x: 0, y: 0 };
    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;

    if (e.touches && e.touches.length > 0) {
      return {
        x: (e.touches[0].clientX - rect.left) * scaleX,
        y: (e.touches[0].clientY - rect.top) * scaleY
      };
    }
    return {
      x: (e.clientX - rect.left) * scaleX,
      y: (e.clientY - rect.top) * scaleY
    };
  };

  const startDrawing = (e: any) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const coords = getCoordinates(e);

    const ctx = canvas.getContext('2d');
    if (ctx) {
      ctx.beginPath();
      ctx.moveTo(coords.x, coords.y);
      ctx.lineWidth = lineWidth;
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';
      ctx.strokeStyle = '#0258a1';
      setIsDrawing(true);
      currentStrokeRef.current = [{ x: coords.x, y: coords.y, timestamp: Date.now() }];
    }
    e.preventDefault();
  };

  const draw = (e: any) => {
    if (!isDrawing) return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const coords = getCoordinates(e);

    const ctx = canvas.getContext('2d');
    if (ctx) {
      ctx.lineTo(coords.x, coords.y);
      ctx.stroke();
      currentStrokeRef.current.push({ x: coords.x, y: coords.y, timestamp: Date.now() });
    }
    e.preventDefault();
  };

  const stopDrawing = () => {
    if (isDrawing && currentStrokeRef.current.length > 0) {
      strokesRef.current.push([...currentStrokeRef.current]);
      currentStrokeRef.current = [];
    }
    setIsDrawing(false);
  };

  const clearCanvas = () => {
    playClickSound();
    const canvas = canvasRef.current;
    if (canvas) {
      const ctx = canvas.getContext('2d');
      if (ctx) ctx.clearRect(0, 0, canvas.width, canvas.height);
    }
    strokesRef.current = [];
    setPracticeFeedback(null);
    setPracticeSuccess(false);
  };

  const openAssignmentModal = (asm: AssignmentItem) => {
    playClickSound();
    setSelectedAsm(asm);
    setPracticeFeedback(null);
    setPracticeSuccess(false);

    if (asm.contentType === 'QUIZ') {
      try {
        const questions = JSON.parse(asm.contentData);
        setQuizAnswers(new Array(questions.length).fill(''));
        if (questions[0]) {
          setTimeout(() => playQuestionAudio(questions[0]), 500);
        }
      } catch (e) {
        setQuizAnswers([]);
      }
    } else {
      const parsed = parseWritingContentData(asm.contentData);
      setLettersList(parsed.letters);
      setTargetScore(parsed.targetScore);

      // Khôi phục tiến trình làm bài đã lưu từ localStorage
      const savedDone = getSavedCompletedLetters(user?.id, asm.id);
      setCompletedLetters(savedDone);

      // Tìm chữ cái đầu tiên chưa hoàn thành
      const firstUncompleted = parsed.letters.findIndex((l: string) => !savedDone.includes(l));
      if (firstUncompleted !== -1) {
        setCurrentLetterIndex(firstUncompleted);
        if (savedDone.length > 0) {
          setPracticeFeedback(`Hệ thống đã lưu các chữ bé đã viết xong (${savedDone.join(', ')}). Tiếp tục làm chữ "${parsed.letters[firstUncompleted]}" nhé! ✨`);
          setPracticeSuccess(true);
        }
      } else {
        setCurrentLetterIndex(0);
      }
    }
  };

  const closeAssignmentModal = () => {
    playClickSound();
    setSelectedAsm(null);
    setPracticeFeedback(null);
    setPracticeSuccess(false);
    strokesRef.current = [];
  };

  const handleQuizAnswer = (questionIdx: number, val: string) => {
    playClickSound();
    const nextAnswers = [...quizAnswers];
    nextAnswers[questionIdx] = val;
    setQuizAnswers(nextAnswers);
  };

  const calculateWritingAccuracy = async (canvas: HTMLCanvasElement, rawLetter: string) => {
    if (strokesRef.current.length === 0 && currentStrokeRef.current.length === 0) {
      return { status: 'NO_INPUT', finalScore: 0 };
    }
    
    // Ensure the last stroke is added if user didn't trigger mouseUp
    const allStrokes = [...strokesRef.current];
    if (currentStrokeRef.current.length > 0) {
      allStrokes.push([...currentStrokeRef.current]);
    }

    const targetChar = isUppercase ? rawLetter.toUpperCase() : rawLetter.toLowerCase();
    return WritingEvaluationEngine.evaluate(allStrokes, targetChar);
  };

  const handleSubmitAssignment = async () => {
    if (!selectedAsm || submitting || evaluating) return;
    playClickSound();
    
    if (selectedAsm.contentType === 'QUIZ') {
      setSubmitting(true);
      const submissionData = JSON.stringify({ answers: quizAnswers });
      try {
        await api.post(`/student/assignments/${selectedAsm.id}/submit`, { submissionData });
        setShowCompletionModal(true);
        fetchData();
      } catch (err: any) {
        console.error('Lỗi nộp bài tập', err);
        setShowCompletionModal(true);
      } finally {
        setSubmitting(false);
      }
      return;
    }

    const canvas = canvasRef.current;
    if (!canvas) return;

    setEvaluating(true);
    setPracticeFeedback(null);

    const currentLetter = lettersList[currentLetterIndex] || 'A';
    const evaluation = await calculateWritingAccuracy(canvas, currentLetter);
    setEvaluating(false);

    if (evaluation.status === 'TEMPLATE_NOT_FOUND') {
      alert(`Bài tập yêu cầu chữ '${currentLetter}' nhưng hệ thống chưa có dữ liệu mẫu để chấm điểm.`);
      return;
    }

    const accuracy = evaluation.finalScore || 0;

    if (accuracy < targetScore) {
      setPracticeSuccess(false);
      setPracticeFeedback(
        `Nét vẽ chữ "${currentLetter}" đạt ${accuracy}%, chưa đủ chỉ tiêu ≥${targetScore}% cô giao. Bé hãy bấm "Viết lại" và nắn nót tô lại nhé! 💪`
      );
      return;
    }

    // Đạt chỉ tiêu cho chữ cái hiện tại
    setPracticeSuccess(true);

    // LƯU LẠI TIẾN TRÌNH CHỮ CÁI ĐÃ ĐẠT VÀO LOCALSTORAGE
    saveCompletedLetter(user?.id, selectedAsm.id, currentLetter);
    const updatedDone = [...completedLetters, currentLetter];
    setCompletedLetters(updatedDone);

    // Kiểm tra xem đã hoàn thành tất cả chữ cái trong bài tập chưa
    const allFinished = lettersList.every((l) => updatedDone.includes(l));

    if (!allFinished) {
      // Lưu ảnh chữ cái vừa vẽ
      const dataUrl = canvas.toDataURL('image/png');
      saveLetterImage(user?.id, selectedAsm.id, currentLetter, dataUrl);

      // Tìm chữ tiếp theo chưa làm
      const nextIndex = lettersList.findIndex((l) => !updatedDone.includes(l));
      setPracticeFeedback(
        `Xuất sắc! Chữ "${currentLetter}" đạt ${accuracy}%. Chuẩn bị viết chữ tiếp theo! ⭐`
      );
      setTimeout(() => {
        if (nextIndex !== -1) {
          setCurrentLetterIndex(nextIndex);
        } else {
          setCurrentLetterIndex(prev => prev + 1);
        }
        clearCanvas();
      }, 1500);
    } else {
      // Đã hoàn thành tất cả các chữ cái! Xóa tiến trình tạm và nộp bài chính thức
      setSubmitting(true);
      const currentDataUrl = canvas.toDataURL('image/png');
      saveLetterImage(user?.id, selectedAsm.id, currentLetter, currentDataUrl);
      
      const allImages = lettersList.map(l => ({
        letter: l,
        image: localStorage.getItem(`student_img_${user?.id || 'guest'}_${selectedAsm.id}_${l}`) || currentDataUrl
      }));

      clearSavedProgress(user?.id, selectedAsm.id);
      clearLetterImages(user?.id, selectedAsm.id, lettersList);
      
      try {
        await api.post(`/student/assignments/${selectedAsm.id}/submit`, {
          submissionData: JSON.stringify(allImages),
          accuracyScore: accuracy
        });
        setShowCompletionModal(true);
        fetchData();
      } catch (err: any) {
        // Tắt thông báo lỗi theo yêu cầu ("đừng thông báo hết")
        console.error('Lỗi nộp bài tập', err);
        // Vẫn hiển thị modal hoàn thành dù nộp có lỗi do quá hạn (nếu backend vẫn còn lỗi)
        setShowCompletionModal(true);
      } finally {
        setSubmitting(false);
      }
    }
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[40vh] gap-3">
        <div className="w-16 h-16 border-8 border-sky-100 border-t-sky-500 rounded-full animate-spin"></div>
        <p className="text-sky-650 font-extrabold text-sm animate-bounce">Đang tải lớp học của bé...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6 sm:space-y-8 animate-fade-in pb-10" style={{ fontFamily: "'Nunito', sans-serif" }}>

      {/* Nhập mã lớp & Danh sách lớp đang học (Thu nhỏ lại) */}
      <div className="flex flex-col items-center justify-center gap-4">
         <form onSubmit={handleJoinClass} className="flex items-center gap-2 bg-slate-50/80 backdrop-blur-sm p-1.5 rounded-full border-4 border-slate-200 focus-within:border-sky-300 focus-within:bg-white transition-all shadow-sm">
           <input 
             type="text" 
             value={inviteCode} 
             onChange={(e) => setInviteCode(e.target.value)} 
             placeholder="Mã lớp mới..." 
             className="bg-transparent border-none outline-none font-black text-slate-700 px-4 w-32 sm:w-48 text-sm placeholder-slate-400" 
           />
           <button type="submit" className="bg-sky-500 text-white font-black px-4 sm:px-6 py-2.5 rounded-full hover:bg-sky-400 active:scale-95 text-xs sm:text-sm shadow-md transition-transform">
             Vào Lớp
           </button>
         </form>

         {/* Hiển thị các lớp đã tham gia dạng bong bóng nhỏ để bấm lọc */}
         {classes.length > 0 && (
           <div className="flex flex-wrap items-center justify-center gap-2 mt-2">
             <span className="text-xs font-bold text-slate-400">Lọc bài tập theo lớp:</span>
             <button 
               onClick={() => setActiveClassId(null)}
               className={`font-black px-3 py-1.5 rounded-full text-[11px] sm:text-xs border-2 shadow-sm flex items-center gap-1.5 transition-transform active:scale-95 ${
                 activeClassId === null 
                   ? 'bg-sky-500 text-white border-sky-600 shadow-md scale-105' 
                   : 'bg-white text-slate-500 border-slate-200 hover:bg-slate-50'
               }`}
             >
               🌟 Tất cả
             </button>
             {classes.map(c => (
               <button 
                 key={c.id}
                 onClick={() => setActiveClassId(c.id)}
                 className={`font-black px-3 py-1.5 rounded-full text-[11px] sm:text-xs border-2 shadow-sm flex items-center gap-1.5 transition-transform active:scale-95 ${
                   activeClassId === c.id
                     ? 'bg-sky-500 text-white border-sky-600 shadow-md scale-105'
                     : 'bg-sky-50 text-sky-600 border-sky-200 hover:bg-sky-100'
                 }`}
               >
                 🎒 {c.name}
               </button>
             ))}
           </div>
         )}
      </div>

      {/* Danh sách Bài Tập (Dạng thẻ Game khổng lồ) */}
      <div className="w-full max-w-5xl mx-auto px-4 space-y-4">
        {/* Bộ lọc trạng thái bài tập: Cần làm / Tất cả */}
        <div className="flex items-center justify-between border-b-2 border-slate-100 pb-2">
          <div className="flex items-center gap-2">
            <button
              onClick={() => setAssignmentFilter('PENDING')}
              className={`px-4 py-1.5 rounded-full text-xs font-black transition-all ${
                assignmentFilter === 'PENDING'
                  ? 'bg-rose-500 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-500 hover:bg-slate-200'
              }`}
            >
              🚀 Cần làm ({assignments.filter(a => (activeClassId === null || a.classId === activeClassId) && !a.isSubmitted).length})
            </button>
            <button
              onClick={() => setAssignmentFilter('ALL')}
              className={`px-4 py-1.5 rounded-full text-xs font-black transition-all ${
                assignmentFilter === 'ALL'
                  ? 'bg-sky-500 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-500 hover:bg-slate-200'
              }`}
            >
              📚 Tất cả bài tập ({assignments.filter(a => activeClassId === null || a.classId === activeClassId).length})
            </button>
          </div>
        </div>

        {(() => {
          const filtered = assignments
            .filter(asm => activeClassId === null || asm.classId === activeClassId)
            .filter(asm => assignmentFilter === 'ALL' || !asm.isSubmitted)
            .sort((a, b) => new Date(b.dueDate || 0).getTime() - new Date(a.dueDate || 0).getTime());

          const totalInCurrentFilterClass = assignments.filter(a => activeClassId === null || a.classId === activeClassId).length;

          if (filtered.length === 0) {
            return (
              <div className="py-16 text-center flex flex-col items-center justify-center space-y-3">
                <div className="text-6xl opacity-60">🎈</div>
                <h3 className="text-slate-700 text-base font-black">
                  {totalInCurrentFilterClass === 0 
                    ? (activeClassId !== null ? 'Lớp này cô giáo chưa giao bài tập nào!' : 'Bé chưa có bài tập nào!')
                    : 'Bé đã hoàn thành hết bài tập trong mục này rồi! 🎉'}
                </h3>
                <p className="text-slate-400 text-xs font-bold max-w-sm">
                  {totalInCurrentFilterClass === 0 
                    ? 'Khi cô giáo giao bài mới cho lớp, bài tập sẽ tự động xuất hiện ở đây nhé.'
                    : 'Bé hãy bấm vào nút "Tất cả bài tập" ở trên để xem lại các bài mình đã nộp.'}
                </p>
              </div>
            );
          }

          return (
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
              {filtered.map((asm) => {
                const isDone = asm.isSubmitted;
                const isQuiz = asm.contentType === 'QUIZ';
                const savedDone = getSavedCompletedLetters(user?.id, asm.id);
                
                return (
                  <div 
                    key={asm.id} 
                    onClick={() => openAssignmentModal(asm)}
                    className={`relative p-5 sm:p-6 rounded-[35px] border-4 flex flex-col items-center text-center transition-all duration-300 select-none cursor-pointer active:scale-95 ${
                      isDone 
                        ? 'bg-gradient-to-b from-emerald-50 to-teal-50 border-emerald-300 shadow-md hover:-translate-y-1 hover:shadow-lg' 
                        : isQuiz 
                          ? 'bg-gradient-to-b from-purple-100 to-fuchsia-100 border-fuchsia-300 shadow-xl shadow-fuchsia-200/50 hover:-translate-y-2 hover:shadow-2xl'
                          : 'bg-gradient-to-b from-pink-100 to-rose-100 border-pink-300 shadow-xl shadow-pink-200/50 hover:-translate-y-2 hover:shadow-2xl'
                    }`}
                  >
                    {/* Tên lớp nhỏ ở góc */}
                    <div className="absolute top-2 right-2 bg-white/60 backdrop-blur-sm px-2 py-0.5 rounded-lg text-[9px] font-black text-slate-500 border border-white/50 max-w-[80px] truncate">
                      {asm.className}
                    </div>

                    {/* Icon khổng lồ */}
                    <div className="text-5xl sm:text-7xl mb-3 mt-2 drop-shadow-md transform transition-transform group-hover:scale-110">
                      {isDone ? '🏆' : isQuiz ? '🐾' : '✏️'}
                    </div>
                    
                    {/* Tên bài ngắn gọn */}
                    <h3 className="font-black text-sm sm:text-lg text-slate-800 leading-tight mb-4 line-clamp-2">
                      {asm.title}
                    </h3>

                    {/* Nút trạng thái */}
                    <div className="mt-auto w-full">
                      <div className={`text-white px-2 py-2 rounded-full font-black text-[11px] sm:text-xs flex items-center justify-center gap-1 shadow-md border-2 border-white/50 ${
                        isDone 
                          ? 'bg-emerald-500' 
                          : isQuiz ? 'bg-fuchsia-500' : 'bg-rose-500'
                      }`}>
                        {isDone 
                          ? (asm.score !== null ? `Đã xong: ${asm.score}đ ⭐` : 'Đã nộp ✓') 
                          : savedDone.length > 0 ? 'Làm tiếp 🚀' : 'Chơi Ngay 🚀'}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          );
        })()}
      </div>

      {/* Modal Làm Bài Tập Hoạt Hình */}
      {selectedAsm && (
        <div className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-xl bg-gradient-to-b from-amber-50 to-orange-50 border-4 border-amber-200 rounded-[35px] overflow-hidden shadow-2xl relative flex flex-col max-h-[90vh]">
            
            {/* Header */}
            <div className="p-5 border-b-2 border-amber-200/50 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className={`w-10 h-10 rounded-xl flex items-center justify-center border-2 ${
                  selectedAsm.contentType === 'QUIZ' ? 'bg-purple-100 text-purple-600 border-purple-200' : 'bg-pink-100 text-pink-500 border-pink-200'
                }`}>
                  {selectedAsm.contentType === 'QUIZ' ? <BookOpen className="w-5 h-5" /> : <PenTool className="w-5 h-5" />}
                </div>
                <div>
                  <h3 className="font-black text-amber-900 text-lg sm:text-xl">{selectedAsm.title}</h3>
                </div>
              </div>
              <button 
                onClick={closeAssignmentModal} 
                className="p-1.5 rounded-full bg-white text-slate-400 hover:text-slate-600 border-2 border-amber-200 active:scale-90 transition-transform"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Body */}
            <div className="p-4 sm:p-6 overflow-y-auto flex-1 space-y-5 bg-white/50">
              {/* Ẩn bớt mô tả dài dòng */}
              {selectedAsm.contentType === 'QUIZ' ? (
                /* GIAO DIỆN TRẮC NGHIỆM CON VẬT VỚI HÌNH ẢNH VÀ NÚT NGHE PHÁT ÂM */
                <div className="space-y-6">
                  {JSON.parse(selectedAsm.contentData).map((q: any, qIdx: number) => (
                    <div key={qIdx} className="space-y-4 bg-white p-5 rounded-3xl border-3 border-amber-250/60 shadow-sm text-center">
                      <p className="text-xs sm:text-sm font-black text-slate-700 text-left">
                        Câu {qIdx + 1}: {q.question || 'Bé hãy nghe âm thanh và chọn chữ cái đúng:'}
                      </p>

                      <div 
                        onClick={() => playQuestionAudio(q)}
                        className="bg-gradient-to-br from-amber-100 to-orange-100 border-3 border-amber-300 hover:border-amber-400 rounded-3xl p-4 cursor-pointer transform hover:scale-102 active:scale-95 transition-all shadow-md max-w-xs mx-auto space-y-2 group"
                      >
                        {q.imageUrl ? (
                          <img src={q.imageUrl} alt={q.animalName || 'Hình ảnh'} className="w-32 h-32 object-cover rounded-2xl mx-auto shadow-sm group-hover:scale-105 transition-all" />
                        ) : (
                          <div className="text-6xl animate-bounce-slow">
                            {q.animalEmoji || '🐾'}
                          </div>
                        )}
                        <h4 className="font-black text-base text-amber-900 group-hover:text-sky-600 flex items-center justify-center gap-1.5">
                          {q.animalName || 'Bé nghe âm thanh'}
                          <Volume2 className="w-5 h-5 text-amber-600 animate-pulse" />
                        </h4>
                      </div>

                      <div>
                        <p className="text-xs font-black text-amber-900 mb-2">Chọn chữ cái đúng:</p>
                        <div className="grid grid-cols-3 gap-3">
                          {q.options?.map((opt: string) => (
                            <button
                              key={opt}
                              type="button"
                              onClick={() => handleQuizAnswer(qIdx, opt)}
                              className={`py-3.5 px-4 rounded-2xl border-3 text-base font-black transition-all duration-150 active:scale-95 ${
                                quizAnswers[qIdx] === opt
                                  ? 'bg-sky-500 border-sky-600 text-white shadow-md scale-105'
                                  : 'border-amber-100 bg-amber-50/50 text-slate-700 hover:bg-amber-100 hover:border-amber-300'
                              }`}
                            >
                              Chữ {opt}
                            </button>
                          ))}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                /* GIAO DIỆN LUYỆN VIẾT BÀI TẬP (Nhiều chữ + Tự động lưu tiến trình từng chữ) */
                <div className="space-y-4 text-center">
                  <div className="bg-white p-3 rounded-2xl border-2 border-amber-200 flex flex-wrap items-center justify-between gap-2">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-xs font-black text-amber-900">Bộ chữ ({currentLetterIndex + 1}/{lettersList.length}):</span>
                      <div className="flex flex-wrap items-center gap-1">
                        {lettersList.map((letStr, idx) => {
                          const isDone = completedLetters.includes(letStr);
                          const isCurrent = idx === currentLetterIndex;
                          const displayChar = isUppercase ? letStr.toUpperCase() : letStr.toLowerCase();
                          return (
                            <button
                              key={idx}
                              type="button"
                              onClick={() => {
                                playClickSound();
                                setCurrentLetterIndex(idx);
                                clearCanvas();
                              }}
                              className={`w-7 h-7 rounded-lg font-black text-xs flex items-center justify-center transition-all ${
                                isCurrent
                                  ? 'bg-sky-500 text-white shadow-sm scale-110 border-2 border-sky-600'
                                  : isDone
                                  ? 'bg-teal-500 text-white border border-teal-600 shadow-sm'
                                  : 'bg-slate-100 text-slate-400'
                              }`}
                              title={isDone ? `Chữ ${letStr} (Đã viết xong ✓)` : `Chữ ${letStr}`}
                            >
                              {isDone ? '✓' : displayChar}
                            </button>
                          );
                        })}
                      </div>

                      <button
                        type="button"
                        onClick={() => {
                          playClickSound();
                          setIsUppercase(!isUppercase);
                          clearCanvas();
                        }}
                        className={`px-2.5 py-1 text-xs font-black rounded-lg transition-colors flex items-center gap-1 border ${
                          isUppercase 
                            ? 'bg-sky-500 text-white border-sky-600 shadow-sm' 
                            : 'bg-slate-100 text-slate-500 hover:bg-slate-200 border-slate-200'
                        }`}
                        title="Đổi chữ hoa / chữ thường"
                      >
                        <span className={isUppercase ? 'text-white' : 'text-slate-400'}>A</span> 
                        <span className="text-[9px] opacity-70">⇄</span> 
                        <span className={!isUppercase ? 'text-slate-700' : 'text-sky-200'}>a</span>
                      </button>
                    </div>

                    <span className="text-xs font-black text-amber-800 bg-amber-100 border border-amber-200 px-3 py-1 rounded-full flex items-center gap-1">
                      <Target className="w-3.5 h-3.5 text-amber-600" /> Chỉ tiêu: ≥{targetScore}%
                    </span>
                  </div>

                  {practiceFeedback && (
                    <div className={`p-3.5 rounded-2xl border-2 text-xs font-black flex items-center justify-center gap-2 ${
                      practiceSuccess 
                        ? 'bg-teal-50 border-teal-200 text-teal-700' 
                        : 'bg-rose-50 border-rose-200 text-rose-600 animate-shake'
                    }`}>
                      {practiceSuccess ? <Sparkles className="w-4 h-4 shrink-0 text-teal-500" /> : <AlertCircle className="w-4 h-4 shrink-0 text-rose-500" />}
                      <span>{practiceFeedback}</span>
                    </div>
                  )}

                  <p className="text-xs text-indigo-700 bg-indigo-50 border-2 border-indigo-100 px-4 py-2 rounded-full inline-block font-black shadow-inner">
                    ✏️ Hãy tô đè thật nắn nót chữ cái "{isUppercase ? (lettersList[currentLetterIndex] || 'A').toUpperCase() : (lettersList[currentLetterIndex] || 'A').toLowerCase()}" nhé!
                  </p>
                  
                  {/* Canvas container tập viết */}
                  <div className="relative w-full max-w-[340px] sm:max-w-[380px] mx-auto aspect-square rounded-2xl overflow-hidden shadow-md border-[3px] sm:border-4 border-slate-300 bg-white shrink-0 touch-none select-none">
                    
                    {/* Chữ mẫu đứt nét để bé tô theo */}
                    <div className="absolute inset-0 z-0 flex items-center justify-center pointer-events-none select-none">
                      <svg viewBox="0 0 428 428" className="w-full h-full absolute inset-0">
                        <text 
                          x="50%" 
                          y="309" 
                          dominantBaseline="alphabetic" 
                          textAnchor="middle" 
                          fill="none" 
                          stroke="#cbd5e1" 
                          strokeWidth="4" 
                          strokeDasharray="12 10" 
                          style={{ fontFamily: "'HP001', cursive", fontSize: '280px' }}
                        >
                          {isUppercase 
                            ? (lettersList[currentLetterIndex] || 'A').toUpperCase() 
                            : (lettersList[currentLetterIndex] || 'A').toLowerCase()}
                        </text>
                      </svg>
                    </div>

                    {/* Canvas vẽ trên cùng */}
                    <canvas
                      ref={canvasRef}
                      width={428}
                      height={428}
                      onMouseDown={startDrawing}
                      onMouseMove={draw}
                      onMouseUp={stopDrawing}
                      onMouseLeave={stopDrawing}
                      onTouchStart={startDrawing}
                      onTouchMove={draw}
                      onTouchEnd={stopDrawing}
                      style={{ touchAction: 'none' }}
                      className="absolute inset-0 z-20 w-full h-full bg-transparent cursor-crosshair"
                    />
                  </div>
                  
                  <button 
                    onClick={clearCanvas}
                    className="text-xs text-rose-500 hover:text-rose-600 font-black bg-rose-50 border border-rose-100 px-4 py-1.5 rounded-full active:scale-90 transition-transform inline-flex items-center gap-1"
                  >
                    <RefreshCw className="w-3.5 h-3.5" />
                    Viết lại nét chữ
                  </button>
                </div>
              )}
            </div>

            {/* Footer */}
            <div className="p-5 border-t-2 border-amber-200/50 flex gap-4 bg-white/40">
              <button
                onClick={closeAssignmentModal}
                className="flex-1 py-3.5 rounded-full bg-slate-100 border-2 border-slate-300 text-slate-500 hover:text-slate-600 font-black text-xs shadow-md transition-all active:scale-95"
              >
                Tạm dừng & Thoát
              </button>
              <button
                onClick={handleSubmitAssignment}
                disabled={evaluating || submitting}
                className="flex-1 py-3.5 rounded-full bg-gradient-to-r from-emerald-400 to-teal-500 text-white hover:from-emerald-350 hover:to-teal-450 font-black text-xs border-2 border-white shadow-lg shadow-emerald-200/50 transition-all active:scale-[0.98] disabled:opacity-50"
              >
                {submitting ? 'Đang gửi bài...' : evaluating ? 'Đang chấm...' : selectedAsm.contentType === 'WRITING' ? `Kiểm tra chữ "${lettersList[currentLetterIndex]}" 🚀` : 'Nộp bài tập 🚀'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Màn hình thông báo hoàn thành bài tập */}
      {showCompletionModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-slate-900/60 backdrop-blur-sm" onClick={() => setShowCompletionModal(false)}></div>
          <div className="bg-white rounded-[40px] p-8 max-w-sm w-full relative z-10 text-center shadow-2xl border-8 border-yellow-300 animate-bounce-in">
            <div className="w-24 h-24 bg-yellow-100 rounded-full flex items-center justify-center text-5xl mx-auto mb-4 shadow-inner border-4 border-yellow-200">
              🏆
            </div>
            <h2 className="text-3xl font-black text-rose-500 mb-2 drop-shadow-sm">Tuyệt Vời!</h2>
            <p className="text-slate-600 font-bold mb-6 text-lg">Bé đã hoàn thành xuất sắc tất cả chữ cái cô giao!</p>
            <button
              onClick={() => {
                setShowCompletionModal(false);
                closeAssignmentModal();
              }}
              className="w-full py-4 rounded-full bg-gradient-to-r from-yellow-400 to-orange-500 text-white font-black text-xl border-4 border-yellow-200 shadow-xl active:scale-95 transition-transform"
            >
              Nhận Thưởng 🌟
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

function parseWritingContentData(contentData: string) {
  if (!contentData) return { letters: ['A'], targetScore: 50 };
  try {
    const data = JSON.parse(contentData);
    if (data.letters && Array.isArray(data.letters)) {
      return { letters: data.letters, targetScore: data.targetScore || 50 };
    }
    if (data.letter) {
      return { letters: [data.letter], targetScore: data.targetScore || 50 };
    }
    return { letters: ['A'], targetScore: 50 };
  } catch (e) {
    return { letters: [contentData || 'A'], targetScore: 50 };
  }
}

export default StudentClasses;
