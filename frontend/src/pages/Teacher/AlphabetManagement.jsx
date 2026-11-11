import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { BookOpen, X, ShieldAlert, Check, AlertCircle, Volume2, Eye } from 'lucide-react';
import { sortVietnameseAlphabet } from '../../utils/alphabetSort';
import { getExampleEmoji } from '../../utils/getExampleEmoji';
import { useAuth } from '../../context/AuthContext';
const VIETNAMESE_ALPHABET = [
    'a', 'ă', 'â', 'b', 'c', 'd', 'đ', 'e', 'ê', 'g', 'h', 'i', 'k', 'l', 'm',
    'n', 'o', 'ô', 'ơ', 'p', 'q', 'r', 's', 't', 'u', 'ư', 'v', 'x', 'y'
];
const pronunciationMap = {
    'a': 'a', 'ă': 'á', 'â': 'ớ', 'b': 'bờ', 'c': 'cờ', 'd': 'dờ', 'đ': 'đờ',
    'e': 'e', 'ê': 'ê', 'g': 'gờ', 'h': 'hờ', 'i': 'i', 'k': 'cờ', 'l': 'lờ',
    'm': 'mờ', 'n': 'nờ', 'o': 'o', 'ô': 'ô', 'ơ': 'ờ', 'p': 'pờ', 'q': 'quờ',
    'r': 'rờ', 's': 'sờ', 't': 'tờ', 'u': 'u', 'ư': 'ừ', 'v': 'vờ', 'x': 'sờ', 'y': 'y'
};
const speakLetterTTS = (letter) => {
    if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel();
        const textToSpeak = pronunciationMap[letter.toLowerCase()] || letter.toLowerCase();
        const utterance = new SpeechSynthesisUtterance(textToSpeak);
        utterance.lang = 'vi-VN';
        utterance.rate = 0.65;
        window.speechSynthesis.speak(utterance);
    }
};
const speakWordTTS = (word) => {
    if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel();
        const utterance = new SpeechSynthesisUtterance(word.toLowerCase());
        utterance.lang = 'vi-VN';
        utterance.rate = 0.70;
        window.speechSynthesis.speak(utterance);
    }
};
const TeacherAlphabetManagement = () => {
    const { user } = useAuth();
    const [lessons, setLessons] = useState([]);
    const [loading, setLoading] = useState(true);
    // Modal Xem Chi Tiết
    const [showDetailModal, setShowDetailModal] = useState(false);
    const [selectedChar, setSelectedChar] = useState('');
    // Toast notifications state
    const [toast, setToast] = useState(null);
    useEffect(() => {
        if (toast) {
            const timer = setTimeout(() => setToast(null), 3000);
            return () => clearTimeout(timer);
        }
    }, [toast]);
    const fetchLessons = async () => {
        setLoading(true);
        try {
            const res = await api.get('/student/alphabet');
            setLessons(sortVietnameseAlphabet(res.data));
        }
        catch (err) {
            console.error(err);
        }
        finally {
            setLoading(false);
        }
    };
    useEffect(() => {
        fetchLessons();
    }, []);
    const handleOpenDetailModal = (char) => {
        setSelectedChar(char);
        setShowDetailModal(true);
    };
    const hasAlphabetPerm = user?.roles?.includes('PERM_VIEW_ALPHABET') || user?.roles?.includes('ROLE_TEACHER') || user?.roles?.includes('ROLE_ADMIN');
    const playLetterAudio = (char, audioUrl) => {
        if (audioUrl) {
            const audio = new Audio(audioUrl);
            audio.play().catch((err) => {
                console.warn('Lỗi phát âm thanh chữ cái, chuyển sang TTS:', err);
                speakLetterTTS(char);
            });
        }
        else {
            speakLetterTTS(char);
        }
    };
    const playExampleAudio = (exampleWord, exampleAudioUrl) => {
        if (exampleAudioUrl) {
            const audio = new Audio(exampleAudioUrl);
            audio.play().catch((err) => {
                console.warn('Lỗi phát âm thanh từ ví dụ, chuyển sang TTS:', err);
                speakWordTTS(exampleWord || 'Từ ví dụ');
            });
        }
        else {
            speakWordTTS(exampleWord || 'Từ ví dụ');
        }
    };
    if (!hasAlphabetPerm) {
        return (<div className="bg-white border-2 border-rose-200 rounded-3xl p-8 max-w-2xl mx-auto my-12 text-center space-y-4 shadow-lg">
        <div className="w-16 h-16 rounded-full bg-rose-100 text-rose-600 flex items-center justify-center mx-auto">
          <ShieldAlert className="w-8 h-8"/>
        </div>
        <h2 className="text-xl font-black text-slate-800">Chưa Được Cấp Quyền</h2>
        <p className="text-xs text-slate-600 font-medium leading-relaxed">
          Tài khoản của bạn chưa được Admin cấp quyền xem học liệu chữ cái. Vui lòng liên hệ Admin hệ thống để mở quyền này.
        </p>
      </div>);
    }
    if (loading) {
        return (<div className="flex flex-col items-center justify-center min-h-[60vh] gap-3">
        <div className="w-12 h-12 border-4 border-emerald-200 border-t-emerald-600 rounded-full animate-spin"></div>
        <p className="text-sm font-bold text-emerald-800">Đang tải danh sách chữ cái...</p>
      </div>);
    }
    const selectedLesson = lessons.find(l => l.letter.toLowerCase() === selectedChar.toLowerCase());
    return (<div className="space-y-6 max-w-7xl mx-auto pb-12">
      {/* Header section */}
      <div className="bg-white border-2 border-sky-100 rounded-3xl p-6 sm:p-8 shadow-sm space-y-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b-2 border-sky-50">
          <div>
            <h2 className="text-xl sm:text-2xl font-black text-slate-800 flex items-center gap-2">
              <BookOpen className="w-7 h-7 text-emerald-600"/> Bảng Chữ Cái Tiếng Việt
            </h2>
            <p className="text-xs font-bold text-slate-500 mt-1">
              Bấm vào bất kỳ ô chữ nào bên dưới để <strong>Xem thông tin chi tiết & Nghe phát âm mẫu</strong>.
            </p>
          </div>
        </div>

        {/* Grid Display of 29 Vietnamese Letters */}
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-5 pt-2">
          {VIETNAMESE_ALPHABET
            .map((char) => {
            const displayChar = `${char.toUpperCase()}${char.toLowerCase()}`;
            const fileName = `${char.toLowerCase()}.png`;
            const imgUrl = `/alphabet/${fileName}`;
            const foundLesson = lessons.find(l => l.letter.toLowerCase() === char.toLowerCase());
            const emoji = getExampleEmoji(foundLesson?.exampleWord, char); //'🌟';
            return (<div key={char} onClick={() => handleOpenDetailModal(char)} className="bg-white border-2 border-sky-200 hover:border-emerald-500 rounded-2xl p-3 shadow-sm hover:shadow-xl transition-all flex flex-col items-center justify-between text-center space-y-3 relative group cursor-pointer">
                  {/* Badge Tên file */}
                  <div className="w-full flex items-center justify-between text-[10px] font-black text-slate-500 border-b border-sky-100 pb-1.5">
                    <span className="uppercase">Học Liệu Chữ</span>
                    <span className="font-mono text-emerald-700 font-bold">{fileName}</span>
                  </div>

                  {/* Preview Ảnh Vuông Đồng Nhất */}
                  <div className="relative w-24 h-24 rounded-xl border-2 border-sky-300 overflow-hidden bg-white flex items-center justify-center p-1 shadow-inner group-hover:border-emerald-500 transition-colors">
                    
                    {/* Phần Emoji / Chữ dự phòng (Nằm dưới) */}
                    <div className="absolute inset-0 flex flex-col items-center justify-center bg-sky-50/20 group-hover:bg-emerald-50/20 transition-colors">
                      <div className="relative w-16 h-16 flex items-center justify-center">
                        {/* Fallback Emoji (nằm dưới) */}
                        <div className="absolute inset-0 flex flex-col items-center justify-center">
                          <span className="text-2xl leading-none">{emoji}</span>
                          <span className="text-xs font-black text-slate-800 font-mali leading-none mt-0.5">
                            {displayChar}
                          </span>
                        </div>

                        {/* Custom Image */}
                        {foundLesson?.imageUrl && (<img src={foundLesson.imageUrl.startsWith('http') || foundLesson.imageUrl.startsWith('/') ? foundLesson.imageUrl : `/example_images/${foundLesson.imageUrl}`} alt={foundLesson.exampleWord} className="absolute inset-0 w-full h-full object-contain rounded-lg border border-slate-100 bg-white z-10" onError={(e) => {
                        e.target.style.display = 'none';
                    }}/>)}
                      </div>
                    </div>

                    {/* Ảnh nét viết mẫu đè lên trên cùng */}
                    <img src={imgUrl} alt={displayChar} className="absolute inset-0 w-full h-full object-fill crisp-letter-img pointer-events-none rounded-lg z-20 bg-white" onError={(e) => {
                    e.target.style.display = 'none';
                }}/>
                  </div>

                  {/* Hover view details button */}
                  <div className="w-full pt-1 border-t border-sky-50 flex items-center justify-center gap-1.5 text-xs font-black text-sky-700 group-hover:text-emerald-600">
                    <Eye className="w-3.5 h-3.5"/>
                    <span>Xem chữ {displayChar}</span>
                  </div>
                </div>);
        })}
        </div>
      </div>

      {/* MODAL: XEM CHI TIẾT CHỮ CÁI */}
      {showDetailModal && (<div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 animate-fade-in">
          <div className="bg-white border-4 border-emerald-200 rounded-[32px] p-6 sm:p-8 max-w-4xl w-full shadow-2xl space-y-6 relative max-h-[90vh] overflow-y-auto">
            <button onClick={() => setShowDetailModal(false)} className="absolute top-5 right-5 p-2 rounded-full bg-slate-100 text-slate-500 hover:bg-rose-100 hover:text-rose-600 transition-colors">
              <X className="w-5 h-5"/>
            </button>

            <div className="flex items-center gap-3 border-b-2 border-emerald-50 pb-4">
              <div className="w-16 h-12 rounded-2xl bg-gradient-to-tr from-emerald-400 to-teal-600 flex items-center justify-center text-white font-black text-2xl shadow-md uppercase tracking-wider">
                {selectedChar.toUpperCase()}{selectedChar.toLowerCase()}
              </div>
              <div>
                <h3 className="text-lg font-black text-slate-800">
                  Chi Tiết Học Liệu Chữ Cái {selectedChar.toUpperCase()}{selectedChar.toLowerCase()}
                </h3>
                <p className="text-xs font-bold text-emerald-600">
                  Xem thông tin âm thanh, hình ảnh phát âm mẫu
                </p>
              </div>
            </div>

            <div className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                
                {/* Ô CỘT TRÁI: PHẦN 1 - HỌC CHỮ CÁI */}
                <div className="bg-sky-50/40 border-2 border-sky-100 p-5 rounded-[24px] space-y-4">
                  <div className="flex items-center gap-2 border-b border-sky-200 pb-2">
                    <span className="text-base">🅰️</span>
                    <h4 className="text-sm font-black text-sky-850">PHẦN 1: HỌC CHỮ CÁI</h4>
                  </div>

                  <div>
                    <span className="block text-xs font-black text-slate-500 uppercase tracking-wider mb-1">Tên Chữ</span>
                    <div className="w-full bg-white border-2 border-sky-100 rounded-xl px-3 py-2 text-sm font-black text-slate-900">
                      {selectedChar.toUpperCase()}{selectedChar.toLowerCase()}
                    </div>
                  </div>

                  {selectedLesson?.pronunciationGuide && (<div>
                      <span className="block text-xs font-black text-slate-500 uppercase tracking-wider mb-1">Hướng Dẫn Phát Âm</span>
                      <div className="w-full bg-white border-2 border-sky-100 rounded-xl px-3 py-2 text-xs font-bold text-slate-700">
                        {selectedLesson.pronunciationGuide}
                      </div>
                    </div>)}

                  {/* Âm thanh phát âm mẫu */}
                  <div className="bg-white border border-sky-200 rounded-2xl p-4 space-y-3 shadow-sm">
                    <span className="text-[11px] font-black text-sky-850 block">🎙️ Phát Âm Chữ Cái</span>
                    <button type="button" onClick={() => playLetterAudio(selectedChar, selectedLesson?.audioUrl)} className="px-4 py-2.5 bg-sky-600 hover:bg-sky-500 text-white rounded-xl text-xs font-black flex items-center gap-2 active:scale-95 transition-all shadow-md cursor-pointer">
                      <Volume2 className="w-4 h-4"/> Nghe phát âm chữ cái
                    </button>
                  </div>

                  {/* Nét chữ mẫu */}
                  <div className="bg-white border border-sky-200 rounded-2xl p-4 space-y-3 shadow-sm">
                    <span className="text-[11px] font-black text-slate-700 block">📷 Ảnh Nét Chữ Mẫu</span>
                    <div className="w-24 h-24 rounded-xl border-2 border-emerald-400 bg-white overflow-hidden flex items-center justify-center p-1 shadow-inner relative mx-auto">
                      <div className="absolute inset-0 flex items-center justify-center text-2xl font-black text-slate-800 font-mali">
                        {selectedChar.toLowerCase()}
                      </div>
                      <img src={`/alphabet/${selectedChar.toLowerCase()}.png`} alt={selectedChar} className="absolute inset-0 w-full h-full object-fill crisp-letter-img bg-white z-10 p-1" onError={(e) => {
                e.target.style.display = 'none';
            }}/>
                    </div>
                  </div>
                </div>

                {/* Ô CỘT PHẢI: PHẦN 2 - TỪ VÍ DỤ MINH HỌA */}
                <div className="bg-amber-50/40 border-2 border-amber-100 p-5 rounded-[24px] space-y-4">
                  <div className="flex items-center gap-2 border-b border-amber-200 pb-2">
                    <span className="text-base">🎈</span>
                    <h4 className="text-sm font-black text-amber-900">PHẦN 2: TỪ VÍ DỤ MINH HỌA</h4>
                  </div>

                  <div>
                    <span className="block text-xs font-black text-slate-500 uppercase tracking-wider mb-1">Từ Ví Dụ Minh Họa</span>
                    <div className="w-full bg-white border-2 border-amber-100 rounded-xl px-3 py-2 text-sm font-black text-slate-900">
                      {selectedLesson?.exampleWord || `Chữ ${selectedChar}`}
                    </div>
                  </div>

                  {/* Âm thanh từ ví dụ */}
                  <div className="bg-white border border-amber-200 rounded-2xl p-4 space-y-3 shadow-sm">
                    <span className="text-[11px] font-black text-amber-900 block">🎙️ Phát Âm Từ Ví Dụ</span>
                    <button type="button" onClick={() => playExampleAudio(selectedLesson?.exampleWord, selectedLesson?.exampleAudioUrl)} className="px-4 py-2.5 bg-amber-500 hover:bg-amber-400 text-white rounded-xl text-xs font-black flex items-center gap-2 active:scale-95 transition-all shadow-md cursor-pointer">
                      <Volume2 className="w-4 h-4"/> Nghe phát âm từ ví dụ
                    </button>
                  </div>

                  {/* Ảnh từ ví dụ */}
                  <div className="bg-white border border-amber-200 rounded-2xl p-4 space-y-3 shadow-sm">
                    <span className="text-[11px] font-black text-amber-900 block">🖼️ Ảnh Minh Họa Cho Từ Ví Dụ</span>
                    <div className="w-24 h-24 rounded-xl border-2 border-amber-200 bg-white overflow-hidden flex items-center justify-center p-1 shadow-inner relative mx-auto">
                      <div className="absolute inset-0 flex items-center justify-center text-3xl bg-amber-50/10">
                        {getExampleEmoji(selectedLesson?.exampleWord, selectedChar)}
                      </div>

                      {selectedLesson?.imageUrl && (<img src={selectedLesson.imageUrl.startsWith('http') || selectedLesson.imageUrl.startsWith('/') ? selectedLesson.imageUrl : `/example_images/${selectedLesson.imageUrl}`} alt="Example word preview" className="absolute inset-0 w-full h-full object-contain bg-white z-10 p-1" onError={(e) => {
                    e.target.style.display = 'none';
                }}/>)}
                    </div>
                  </div>
                </div>

              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-sky-100">
                <button type="button" onClick={() => setShowDetailModal(false)} className="px-6 py-2.5 rounded-xl bg-slate-100 text-slate-700 font-extrabold text-xs hover:bg-slate-200 transition-colors cursor-pointer">
                  Đóng
                </button>
              </div>
            </div>
          </div>
        </div>)}

      {/* Floating Toast Notification */}
      {toast && (<div className="fixed bottom-6 right-6 z-[9999] flex items-center gap-3 px-5 py-4 rounded-2xl shadow-xl border animate-fade-in bg-white border-sky-100 max-w-sm">
          <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 ${toast.type === 'success' ? 'bg-emerald-100 text-emerald-600' : 'bg-rose-100 text-rose-600'}`}>
            {toast.type === 'success' ? (<Check className="w-4.5 h-4.5"/>) : (<AlertCircle className="w-4.5 h-4.5"/>)}
          </div>
          <div className="space-y-0.5">
            <h4 className="text-xs font-black text-slate-800">
              {toast.type === 'success' ? 'Thành công' : 'Có lỗi xảy ra'}
            </h4>
            <p className="text-[11px] font-bold text-slate-500">{toast.message}</p>
          </div>
          <button onClick={() => setToast(null)} className="p-1 rounded-full text-slate-400 hover:bg-slate-50 transition-colors cursor-pointer self-start">
            <X className="w-3.5 h-3.5"/>
          </button>
        </div>)}
    </div>);
};
export default TeacherAlphabetManagement;
