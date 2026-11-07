import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { ChevronLeft, ChevronRight, Volume2 } from 'lucide-react';
import { sortVietnameseAlphabet } from '../../utils/alphabetSort';
import { getExampleEmoji } from '../../utils/getExampleEmoji';

interface AlphabetItem {
  id: number;
  letter: string;
  pronunciationGuide: string;
  exampleWord: string;
  audioUrl?: string;
  imageUrl?: string;
  exampleAudioUrl?: string;
  isHidden?: boolean;
}

const pronunciationMap: Record<string, string> = {
  'a': 'a', 'ă': 'á', 'â': 'ớ', 'b': 'bờ', 'c': 'cờ', 'd': 'dờ', 'đ': 'đờ',
  'e': 'e', 'ê': 'ê', 'g': 'gờ', 'h': 'hờ', 'i': 'i', 'k': 'cờ', 'l': 'lờ',
  'm': 'mờ', 'n': 'nờ', 'o': 'o', 'ô': 'ô', 'ơ': 'ờ', 'p': 'pờ', 'q': 'quờ',
  'r': 'rờ', 's': 'sờ', 't': 'tờ', 'u': 'u', 'ư': 'ừ', 'v': 'vờ', 'x': 'sờ', 'y': 'y'
};

const Alphabet: React.FC = () => {
  const [lessons, setLessons] = useState<AlphabetItem[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [loading, setLoading] = useState(true);
  const [imageError, setImageError] = useState(false);
  const [isCompletedAll, setIsCompletedAll] = useState(false);
  const [allLessons, setAllLessons] = useState<AlphabetItem[]>([]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [alphaRes, historyRes] = await Promise.all([
          api.get('/student/alphabet'),
          api.get('/student/practice/history')
        ]);
        const visibleAlphabet = alphaRes.data.filter((item: any) => !item.isHidden);
        const sortedAlphabet = sortVietnameseAlphabet(visibleAlphabet);
        setAllLessons(sortedAlphabet);

        const practicedLetters = new Set(historyRes.data.map((item: any) => item.letter));
        const unpracticed = sortedAlphabet.filter(item => !practicedLetters.has(item.letter));

        if (unpracticed.length === 0 && sortedAlphabet.length > 0) {
          setIsCompletedAll(true);
          setLessons(sortedAlphabet);
        } else {
          setLessons(unpracticed);
        }
      } catch (err) {
        console.error('Không thể lấy dữ liệu', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  useEffect(() => {
    setImageError(false);
  }, [currentIndex]);

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

  const playPronunciation = (letter: string, customAudioUrl?: string) => {
    playClickSound();
    if (customAudioUrl) {
      const audio = new Audio(customAudioUrl);
      audio.play().catch(e => {
        console.warn('Lỗi phát âm thanh custom, chuyển sang TTS:', e);
        playTTS(letter);
      });
    } else {
      playTTS(letter);
    }
  };

  const playTTS = (letter: string) => {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const textToSpeak = pronunciationMap[letter.toLowerCase()] || letter.toLowerCase();
      const utterance = new SpeechSynthesisUtterance(textToSpeak);
      utterance.lang = 'vi-VN';
      utterance.rate = 0.65; 
      window.speechSynthesis.speak(utterance);
    }
  };

  const playWordTTS = (word: string) => {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(word.toLowerCase());
      utterance.lang = 'vi-VN';
      utterance.rate = 0.70;
      window.speechSynthesis.speak(utterance);
    }
  };

  const playWordPronunciation = (word: string, customExampleAudioUrl?: string) => {
    playClickSound();
    if (customExampleAudioUrl) {
      const audio = new Audio(customExampleAudioUrl);
      audio.play().catch(e => {
        console.warn('Lỗi phát âm thanh ví dụ custom, chuyển sang TTS:', e);
        playWordTTS(word);
      });
    } else {
      playWordTTS(word);
    }
  };

  const handleNext = () => {
    playClickSound();
    if (lessons.length > 0) {
      const nextIdx = (currentIndex + 1) % lessons.length;
      setCurrentIndex(nextIdx);
      playPronunciation(lessons[nextIdx].letter, lessons[nextIdx].audioUrl);
    }
  };

  const handlePrev = () => {
    playClickSound();
    if (lessons.length > 0) {
      const prevIdx = (currentIndex - 1 + lessons.length) % lessons.length;
      setCurrentIndex(prevIdx);
      playPronunciation(lessons[prevIdx].letter, lessons[prevIdx].audioUrl);
    }
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-full gap-3">
        <div className="w-16 h-16 border-8 border-sky-100 border-t-sky-500 rounded-full animate-spin"></div>
        <p className="text-sky-600 font-extrabold text-sm animate-bounce">Đang mở bài học...</p>
      </div>
    );
  }

  if (lessons.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-center">
        <p className="text-slate-500 font-black">Chưa có bài học chữ cái nào!</p>
      </div>
    );
  }

  if (isCompletedAll) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center h-full bg-slate-50/50 rounded-[30px] p-6 text-center" style={{ fontFamily: "'Nunito', sans-serif" }}>
        <div className="text-6xl mb-4 animate-bounce">🎉</div>
        <h2 className="text-2xl sm:text-3xl font-black text-emerald-600 mb-2">Bạn đã học hết bảng chữ cái!</h2>
        <p className="text-slate-600 mb-6 font-bold text-lg">Chúc mừng bạn đã xuất sắc hoàn thành tất cả các bài học.</p>
        <button 
          onClick={() => setIsCompletedAll(false)}
          className="px-8 py-4 bg-gradient-to-r from-sky-400 to-blue-500 hover:from-sky-500 hover:to-blue-600 text-white rounded-full font-black shadow-lg shadow-sky-200 transform hover:scale-105 active:scale-95 transition-all text-lg"
        >
          Luyện tập lại
        </button>
      </div>
    );
  }

  const currentItem = lessons[currentIndex];
  const charUpper = currentItem.letter.toUpperCase();
  const charLower = currentItem.letter.toLowerCase();

  return (
    <div className="flex-1 flex flex-col h-full bg-slate-50/50 rounded-[30px] overflow-hidden relative" style={{ fontFamily: "'Nunito', sans-serif" }}>
      
      {/* Header */}
      <div className="flex items-center justify-between px-4 sm:px-8 py-3 sm:py-4 shrink-0 bg-white border-b-2 border-slate-100">
        <h1 className="text-lg sm:text-2xl font-black text-sky-600">ĐỌC CHỮ CÁI</h1>
        
        <div className="bg-sky-100 text-sky-700 px-3 py-1 rounded-full text-xs font-black shadow-inner">
          {currentIndex + 1} / {lessons.length}
        </div>
      </div>

      {/* Main Flashcard Area */}
      <div className="flex-1 flex flex-col justify-center items-center px-4 py-2 min-h-0 relative">
        
        {/* L/R Navigation Buttons */}
        <button 
          onClick={handlePrev}
          className="absolute left-2 sm:left-6 z-10 w-12 h-12 sm:w-16 sm:h-16 bg-white border-4 border-sky-200 rounded-full flex items-center justify-center text-sky-500 hover:bg-sky-50 hover:border-sky-300 hover:scale-110 active:scale-90 transition-all shadow-lg"
        >
          <ChevronLeft className="w-8 h-8 sm:w-10 sm:h-10" />
        </button>

        <button 
          onClick={handleNext}
          className="absolute right-2 sm:right-6 z-10 w-12 h-12 sm:w-16 sm:h-16 bg-white border-4 border-emerald-200 rounded-full flex items-center justify-center text-emerald-500 hover:bg-emerald-50 hover:border-emerald-300 hover:scale-110 active:scale-90 transition-all shadow-lg"
        >
          <ChevronRight className="w-8 h-8 sm:w-10 sm:h-10" />
        </button>

        {/* The Card Container */}
        <div className="w-full max-w-sm h-full max-h-[550px] flex flex-col gap-3 sm:gap-4 py-2">
          
          {/* Top Half: Letter Card */}
          <div 
            onClick={() => playPronunciation(currentItem.letter, currentItem.audioUrl)}
            className="flex-1 bg-gradient-to-b from-sky-300 to-blue-400 rounded-[35px] sm:rounded-[40px] border-4 sm:border-8 border-white shadow-xl flex flex-col items-center justify-center relative overflow-hidden cursor-pointer transform hover:-translate-y-2 active:scale-95 transition-all group"
          >
            {/* Play Icon Hint */}
            <div className="absolute top-4 right-4 bg-white/30 backdrop-blur-sm p-2 rounded-full opacity-60 group-hover:opacity-100 transition-opacity">
              <Volume2 className="w-5 h-5 text-white" />
            </div>
            
            <div className="absolute -top-10 -right-10 w-32 h-32 bg-white/10 rounded-full pointer-events-none"></div>
            
            {/* The Letters (Aa) */}
            <div className="flex items-baseline gap-2 sm:gap-4 drop-shadow-lg text-white" style={{ fontFamily: "'Mali', cursive" }}>
              <span className="text-[100px] sm:text-[140px] font-bold leading-none">{charUpper}</span>
              <span className="text-[80px] sm:text-[110px] font-bold leading-none">{charLower}</span>
            </div>
          </div>

          {/* Bottom Half: Image & Word Card */}
          <div 
            onClick={() => playWordPronunciation(currentItem.exampleWord, currentItem.exampleAudioUrl)}
            className="flex-1 bg-gradient-to-b from-amber-200 to-orange-300 rounded-[35px] sm:rounded-[40px] border-4 sm:border-8 border-white shadow-xl flex flex-col items-center justify-center relative overflow-hidden cursor-pointer transform hover:-translate-y-2 active:scale-95 transition-all group p-4"
          >
            {/* Play Icon Hint */}
            <div className="absolute top-4 right-4 bg-white/30 backdrop-blur-sm p-2 rounded-full opacity-60 group-hover:opacity-100 transition-opacity">
              <Volume2 className="w-5 h-5 text-amber-800" />
            </div>

            <div className="absolute -bottom-10 -left-10 w-40 h-40 bg-white/20 rounded-full pointer-events-none"></div>

            {/* Custom Image or Emoji */}
            {currentItem.imageUrl && !imageError ? (
              <img 
                src={currentItem.imageUrl} 
                alt={currentItem.exampleWord} 
                className="w-24 h-24 sm:w-32 sm:h-32 object-contain rounded-2xl drop-shadow-md border-4 border-white bg-white/50"
                onError={() => setImageError(true)}
              />
            ) : (
              <div className="text-[80px] sm:text-[110px] leading-none drop-shadow-md animate-float">
                {getExampleEmoji(currentItem.exampleWord, currentItem.letter)}
              </div>
            )}
            
            {/* The Word */}
            <div className="mt-2 sm:mt-4 text-3xl sm:text-4xl font-black text-amber-900 drop-shadow-sm uppercase tracking-wider text-center px-4">
              {currentItem.exampleWord}
            </div>
          </div>

        </div>
      </div>
    </div>
  );
};

export default Alphabet;
