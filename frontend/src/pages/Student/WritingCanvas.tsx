import React, { useRef, useState, useEffect } from 'react';
import api from '../../services/api';
import { Check } from 'lucide-react';
import { sortVietnameseAlphabet } from '../../utils/alphabetSort';
import type { Stroke } from './writing/engine/types';
import { WritingEvaluationEngine } from './writing/engine';

interface AlphabetItem {
  id: number;
  letter: string;
  imageUrl?: string;
}

const WritingCanvas: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const strokesRef = useRef<Stroke[]>([]);
  const currentStrokeRef = useRef<Stroke>([]);
  const [lessons, setLessons] = useState<AlphabetItem[]>([]);
  const [selectedLetter, setSelectedLetter] = useState('a');
  const [isUppercase, setIsUppercase] = useState(false);
  const [isDrawing, setIsDrawing] = useState(false);
  const [lineWidth] = useState(15);
  const [practiceResult, setPracticeResult] = useState<{ score: number; msg: string } | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);

  useEffect(() => {
    const fetchAlphabet = async () => {
      try {
        const res = await api.get('/student/alphabet');
        const sorted = sortVietnameseAlphabet(res.data);
        setLessons(sorted);
        if (sorted.length > 0) {
          setSelectedLetter(sorted[0].letter);
        }
      } catch (err) {
        console.error('Không thể lấy bảng chữ cái', err);
      } finally {
        setLoading(false);
      }
    };
    fetchAlphabet();
  }, []);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (canvas) {
      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.lineCap = 'round';
        ctx.lineJoin = 'round';
        ctx.strokeStyle = '#0258a1'; // Mực xanh học sinh viết
      }
    }
    
    const preventScroll = (e: TouchEvent) => {
      // Chỉ chặn cuộn, không chặn các thao tác chạm button
      if (e.touches.length > 1) return; // Cho phép zoom nếu cần thiết (tùy chọn)
      e.preventDefault();
    };

    // Khóa cuộn trang hoàn toàn khi ở màn hình tập viết bằng non-passive event (rất mạnh trên mobile)
    document.body.style.overflow = 'hidden';
    document.body.style.touchAction = 'none';
    document.addEventListener('touchmove', preventScroll, { passive: false });
    
    return () => {
      document.body.style.overflow = '';
      document.body.style.touchAction = '';
      document.removeEventListener('touchmove', preventScroll);
    };
  }, []);

  const playClickSound = () => {
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(523.25, audioCtx.currentTime); // C5
      osc.frequency.exponentialRampToValueAtTime(783.99, audioCtx.currentTime + 0.1); // G5
      gain.gain.setValueAtTime(0.05, audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.12);
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start();
      osc.stop(audioCtx.currentTime + 0.12);
    } catch (e) {}
  };

  const playSuccessSound = () => {
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = 'triangle';
      
      const now = audioCtx.currentTime;
      osc.frequency.setValueAtTime(523.25, now); // C5
      osc.frequency.setValueAtTime(659.25, now + 0.1); // E5
      osc.frequency.setValueAtTime(783.99, now + 0.2); // G5
      osc.frequency.setValueAtTime(1046.50, now + 0.3); // C6
      
      gain.gain.setValueAtTime(0, now);
      gain.gain.linearRampToValueAtTime(0.1, now + 0.05);
      gain.gain.setValueAtTime(0.1, now + 0.3);
      gain.gain.linearRampToValueAtTime(0, now + 0.5);
      
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start(now);
      osc.stop(now + 0.5);
    } catch (e) {}
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
    const coords = getCoordinates(e);
    const canvas = canvasRef.current;
    if (canvas) {
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
    }
    e.preventDefault();
  };

  const draw = (e: any) => {
    if (!isDrawing) return;
    const coords = getCoordinates(e);
    const canvas = canvasRef.current;
    if (canvas) {
      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.lineTo(coords.x, coords.y);
        ctx.stroke();
        currentStrokeRef.current.push({ x: coords.x, y: coords.y, timestamp: Date.now() });
      }
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
      if (ctx) {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        setPracticeResult(null);
      }
    }
    strokesRef.current = [];
  };

  const handleLetterChange = (letter: string) => {
    setSelectedLetter(letter);
    clearCanvas();
  };

  const calculateWritingAccuracy = async (canvas: HTMLCanvasElement) => {
    if (strokesRef.current.length === 0 && currentStrokeRef.current.length === 0) {
      return { status: 'NO_INPUT', finalScore: 0 };
    }
    
    // Ensure the last stroke is added if user didn't trigger mouseUp
    const allStrokes = [...strokesRef.current];
    if (currentStrokeRef.current.length > 0) {
      allStrokes.push([...currentStrokeRef.current]);
    }

    const targetChar = isUppercase ? selectedLetter.toUpperCase() : selectedLetter.toLowerCase();
    return WritingEvaluationEngine.evaluate(allStrokes, targetChar);
  };

  const handleSubmit = async () => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    setSubmitting(true);
    setPracticeResult(null);

    const evaluation = await calculateWritingAccuracy(canvas);

    if (evaluation.status === 'TEMPLATE_NOT_FOUND') {
      alert(`Tính năng chấm điểm chưa hỗ trợ chữ '${selectedLetter}' vì thiếu dữ liệu mẫu.`);
      setSubmitting(false);
      return;
    }

    const calculatedScore = evaluation.finalScore || 0;

    try {
      const dataUrl = canvas.toDataURL('image/png');
      
      const res = await api.post('/student/practice', {
        letter: selectedLetter,
        canvasImageUrl: dataUrl,
        accuracyScore: calculatedScore
      });
      
      const score = res.data.accuracyScore;
      let msg = '';
      if (score >= 90) msg = 'Bé viết chữ rất đẹp! 👍';
      else if (score >= 80) msg = 'Đẹp lắm! Cố gắng hơn xíu nhé! ✨';
      else if (score >= 70) msg = 'Khá tốt! Chăm chỉ tập thêm nhé! 💪';
      else msg = 'Tô thật chậm để đẹp hơn nhé!';

      setPracticeResult({ score, msg });
      playSuccessSound();

      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
      }
    } catch (err) {
      console.error(err);
      alert('Không thể gửi chấm điểm. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  const getStarRating = (score: number) => {
    if (score >= 90) return '⭐⭐⭐ Tuyệt vời!';
    if (score >= 80) return '⭐⭐ Khá lắm!';
    return '⭐ Cố lên!';
  };

  return (
    <div className="flex flex-col h-full space-y-3 sm:space-y-6" style={{ fontFamily: "'Nunito', sans-serif" }}>
      <div className="shrink-0">
        <h1 className="text-lg sm:text-3xl font-black text-sky-650 flex items-center gap-2">
          ✏️ Bảng luyện viết chữ
        </h1>
        <p className="hidden sm:block text-slate-500 font-bold text-xs sm:text-sm mt-1">
          Bé hãy chọn chữ cái mình thích, cầm bút (hoặc ngón tay) tô thật nắn nót đè lên chữ mờ nhé!
        </p>
      </div>

      <div className={`flex-1 flex flex-col lg:flex-row gap-4 sm:gap-8 items-start min-h-0 ${practiceResult ? 'justify-start' : 'justify-center'}`}>
        <div className={`w-full ${practiceResult ? 'lg:w-2/3 max-w-2xl' : 'max-w-2xl mx-auto'} flex flex-col h-full space-y-3 sm:space-y-6 transition-all duration-500`}>
          <div className="bg-gradient-to-r from-amber-100 to-yellow-200 border-2 sm:border-4 border-amber-300 p-2 sm:p-4 rounded-[20px] sm:rounded-[26px] flex flex-wrap items-center justify-between gap-2 sm:gap-4 shadow-sm shrink-0">
            <div className="flex items-center gap-2 sm:gap-4">
              <span className="text-[10px] sm:text-xs font-black text-amber-800 uppercase tracking-wider hidden md:inline">Tập viết chữ:</span>
              <div className="flex items-center gap-2">
                <div className="relative">
                  <button
                    onClick={() => setShowDropdown(!showDropdown)}
                    className="bg-white border-2 border-slate-200 rounded-xl px-4 py-2 sm:px-5 sm:py-2.5 text-sm sm:text-base font-black text-slate-700 hover:bg-slate-50 flex items-center gap-2 transition-all shadow-sm active:scale-95"
                  >
                    Chữ {selectedLetter.toUpperCase()}
                    <span className={`text-[10px] transition-transform ${showDropdown ? 'rotate-180' : ''}`}>▼</span>
                  </button>

                  {showDropdown && (
                    <div className="absolute top-full left-0 mt-2 bg-white border-2 border-slate-100 rounded-2xl shadow-xl p-3 w-[260px] sm:w-[320px] z-50 animate-fade-in origin-top-left">
                      <div className="flex flex-wrap gap-2">
                        {lessons.map((l) => (
                          <button
                            key={l.letter}
                            onClick={() => {
                              handleLetterChange(l.letter);
                              setShowDropdown(false);
                            }}
                            className={`w-9 h-9 sm:w-10 sm:h-10 shrink-0 rounded-xl font-black text-sm flex items-center justify-center transition-all ${
                              selectedLetter === l.letter
                                ? 'bg-amber-500 text-white shadow-md border-2 border-amber-600 ring-2 ring-amber-200 ring-offset-1'
                                : 'bg-slate-50 text-slate-600 hover:bg-slate-200 border-2 border-slate-100'
                            }`}
                            title={`Chữ ${l.letter.toUpperCase()}`}
                          >
                            {l.letter.toUpperCase()}
                          </button>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
                <div className="h-8 w-0.5 bg-amber-200 mx-1 shrink-0"></div>
                <button
                  onClick={() => { playClickSound(); setIsUppercase(!isUppercase); clearCanvas(); }}
                  className={`px-4 py-2 sm:px-5 sm:py-2.5 text-sm sm:text-base font-black rounded-lg transition-colors flex items-center gap-2 ${
                    isUppercase ? 'bg-sky-500 text-white shadow-md border-2 border-sky-600' : 'bg-slate-100 text-slate-500 hover:bg-slate-200 border-2 border-slate-200'
                  }`}
                  title="Đổi chữ hoa / chữ thường"
                >
                  <span className={isUppercase ? 'text-white' : 'text-slate-400'}>A</span> 
                  <span className="text-[10px] opacity-70">⇄</span> 
                  <span className={!isUppercase ? 'text-slate-700' : 'text-sky-200'}>a</span>
                </button>
              </div>
            </div>

            <div className="flex items-center gap-2 sm:gap-6">
              <button
                onClick={clearCanvas}
                className="flex items-center gap-1.5 px-4 py-2.5 rounded-full bg-rose-500 hover:bg-rose-400 text-white border-2 border-white text-xs font-black transition-all shadow-md active:scale-95 cursor-pointer"
              >
                <span>🧼</span>
                Xóa bảng
              </button>
            </div>
          </div>

          <div className="relative w-full max-w-[460px] mx-auto aspect-square rounded-xl sm:rounded-2xl overflow-hidden shadow-md border-[3px] sm:border-4 border-slate-300 bg-white shrink-0">
            
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
                  {isUppercase ? selectedLetter.toUpperCase() : selectedLetter.toLowerCase()}
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

          {/* Submit action */}
          <div className="text-center shrink-0">
            <button
              onClick={handleSubmit}
              disabled={loading}
              className="px-6 sm:px-8 py-3 sm:py-4.5 rounded-full bg-gradient-to-r from-emerald-400 to-teal-500 hover:from-emerald-300 hover:to-teal-400 text-white font-black text-sm border-2 border-white shadow-md sm:shadow-lg shadow-emerald-200/50 active:scale-95 disabled:opacity-50 inline-flex items-center gap-2 cursor-pointer w-full sm:w-auto justify-center"
            >
              {loading ? (
                <>
                  <div className="w-5 h-5 border-4 border-white/30 border-t-white rounded-full animate-spin"></div>
                  Đang chấm điểm...
                </>
              ) : (
                <>
                  <Check className="w-5 h-5" />
                  Nộp Bài Cho Cô Xem 🌟
                </>
              )}
            </button>
          </div>
        </div>

        {/* Results Modal Popup */}
        {practiceResult && (
          <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4">
            <div className="w-full max-w-[320px] bg-gradient-to-b from-emerald-50 to-teal-50 border-4 border-emerald-200 p-6 rounded-[28px] space-y-4 text-center animate-bounce-short shadow-2xl">
              <div className="mx-auto w-16 h-16 rounded-full bg-emerald-400 border-2 border-white flex items-center justify-center text-3xl shadow-md">
                🏆
              </div>

              <div>
                <h3 className="text-xl sm:text-2xl font-black text-emerald-600 mt-1">
                  {getStarRating(practiceResult.score)}
                </h3>
                <span className="inline-block mt-1 text-[11px] font-bold text-slate-500 bg-white border border-emerald-100 px-3 py-1 rounded-full">
                  Điểm: {practiceResult.score}%
                </span>
              </div>

              <div className="text-sm font-bold text-slate-700 leading-relaxed py-2">
                {practiceResult.msg}
              </div>

              <button
                onClick={clearCanvas}
                className="w-full py-3.5 px-4 rounded-full bg-sky-500 hover:bg-sky-400 text-white font-black border-2 border-white text-xs shadow-md transition-all active:scale-95 cursor-pointer mt-2"
              >
                Tiếp tục tập viết
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default WritingCanvas;
