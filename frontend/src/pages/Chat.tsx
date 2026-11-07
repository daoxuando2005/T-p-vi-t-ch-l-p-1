import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { Send, User, MessageSquare, AlertCircle, Phone, Video, Info, ArrowLeft } from 'lucide-react';

interface Contact {
  id: number;
  username: string;
  fullName: string;
  email: string;
  phoneNumber: string;
  roles?: string[];
  childInfo?: string;
  className?: string;
}

interface MessageItem {
  id: number;
  senderId: number;
  senderName: string;
  receiverId: number;
  content: string;
  sentAt: string;
  isRead: boolean;
}

const Chat: React.FC = () => {
  const { user } = useAuth();
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [selectedContact, setSelectedContact] = useState<Contact | null>(null);
  const [messages, setMessages] = useState<MessageItem[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [sending, setSending] = useState(false);
  const [selectedClassFilter, setSelectedClassFilter] = useState<string>('Tất cả');

  const stompClientRef = useRef<Client | null>(null);
  const chatEndRef = useRef<HTMLDivElement | null>(null);

  const scrollToBottom = () => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // Khởi tạo lấy danh bạ chat
  useEffect(() => {
    const fetchContacts = async () => {
      try {
        const res = await api.get('/chat/contacts');
        setContacts(res.data);
        if (res.data.length > 0) {
          handleSelectContact(res.data[0]);
        }
      } catch (err) {
        console.error('Lỗi lấy danh bạ chat:', err);
      }
    };
    fetchContacts();
  }, []);

  // Polling tự động làm mới lịch sử tin nhắn mỗi 4 giây (lọc trùng lặp)
  useEffect(() => {
    if (!selectedContact) return;

    const interval = setInterval(async () => {
      try {
        const res = await api.get(`/chat/history/${selectedContact.id}`);
        setMessages(res.data);
      } catch (e) {}
    }, 4000);

    return () => clearInterval(interval);
  }, [selectedContact]);

  // Khởi tạo kết nối WebSocket STOMP
  useEffect(() => {
    if (!user) return;

    try {
      const socket = new SockJS('http://localhost:8080/ws');
      const client = new Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      client.onConnect = () => {
        client.subscribe(`/user/${user.id}/queue/messages`, (messageOutput) => {
          const newMsg: MessageItem = JSON.parse(messageOutput.body);
          setMessages((prev) => {
            if (prev.some((m) => m.id === newMsg.id)) return prev;
            if (
              newMsg.senderId === user.id ||
              (selectedContact && newMsg.senderId === selectedContact.id)
            ) {
              return [...prev, newMsg];
            }
            return prev;
          });
        });
      };

      client.activate();
      stompClientRef.current = client;

      return () => {
        if (stompClientRef.current) {
          stompClientRef.current.deactivate();
        }
      };
    } catch (e) {
      console.error('Không thể bật WebSocket:', e);
    }
  }, [user, selectedContact]);

  const handleSelectContact = async (contact: Contact) => {
    setSelectedContact(contact);
    setLoadingHistory(true);
    try {
      const res = await api.get(`/chat/history/${contact.id}`);
      setMessages(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoadingHistory(false);
    }
  };

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputValue.trim() || !selectedContact || !user) return;

    const contentText = inputValue.trim();
    setInputValue('');
    setSending(true);

    const payload = {
      senderId: user.id,
      receiverId: selectedContact.id,
      content: contentText,
    };

    // Tạo message tạm hiển thị ngay lập tức ở client gửi
    const tempId = Date.now();
    const tempMsg: MessageItem = {
      id: tempId,
      senderId: user.id,
      senderName: user.fullName || user.username,
      receiverId: selectedContact.id,
      content: contentText,
      sentAt: new Date().toISOString(),
      isRead: false,
    };

    setMessages((prev) => [...prev, tempMsg]);

    try {
      // Chỉ gửi qua WebSocket NẾU kết nối đang hoạt động, nếu không gửi qua REST API
      if (stompClientRef.current && stompClientRef.current.connected) {
        stompClientRef.current.publish({
          destination: '/app/chat.message',
          body: JSON.stringify(payload),
        });
      } else {
        const res = await api.post('/chat/send', payload);
        if (res.data) {
          setMessages((prev) =>
            prev.map((m) => (m.id === tempId ? res.data : m))
          );
        }
      }
    } catch (err) {
      console.error('Lỗi gửi tin nhắn:', err);
    } finally {
      setSending(false);
    }
  };

  const getRoleBadge = (roles?: string[]) => {
    if (!roles) return null;
    if (roles.includes('ROLE_TEACHER')) {
      return <span className="text-[9px] font-black uppercase px-2 py-0.5 rounded-full bg-purple-100 text-purple-600 border border-purple-200">Giáo viên</span>;
    }
    if (roles.includes('ROLE_PARENT')) {
      return <span className="text-[9px] font-black uppercase px-2 py-0.5 rounded-full bg-sky-100 text-sky-600 border border-sky-200">Phụ huynh</span>;
    }
    return null;
  };

  return (
    <div className="h-[calc(100vh-130px)] sm:h-[calc(100vh-64px)] -m-4 sm:m-0" style={{ fontFamily: "'Nunito', sans-serif" }}>
      {/* Chat Layout Container */}
      <div className="bg-white sm:rounded-3xl border-0 sm:border-4 border-sky-100 sm:shadow-xl overflow-hidden h-full flex relative">
        
        {/* Left Column: Contact List */}
        <div className={`w-full md:w-80 border-r-0 md:border-r-2 border-sky-100 bg-sky-50/40 flex-col shrink-0 ${selectedContact ? 'hidden md:flex' : 'flex'}`}>
          <div className="p-4 border-b-2 border-sky-100 bg-white">
            <h2 className="text-sm font-black text-sky-800 flex items-center gap-2 mb-3">
              <User className="w-4 h-4 text-sky-500" />
              Danh bạ liên hệ ({contacts.length})
            </h2>
            {contacts.length > 0 && (
              <select
                value={selectedClassFilter}
                onChange={(e) => setSelectedClassFilter(e.target.value)}
                className="w-full bg-sky-50 border border-sky-200 text-sky-800 text-xs rounded-xl px-3 py-2 focus:outline-none focus:border-sky-400 font-bold"
              >
                {['Tất cả', ...Array.from(new Set(contacts.map(c => c.className || 'Liên hệ khác')))].map(cls => (
                  <option key={cls} value={cls}>{cls}</option>
                ))}
              </select>
            )}
          </div>

          <div className="flex-1 overflow-y-auto p-2 space-y-3">
            {contacts.length === 0 ? (
              <div className="px-4 py-10 text-center text-slate-400 text-xs font-bold">
                <AlertCircle className="w-8 h-8 text-sky-200 mx-auto mb-2" />
                Chưa có liên hệ nào trong danh bạ. Danh bạ tự động kết nối khi tạo lớp hoặc liên kết phụ huynh.
              </div>
            ) : (
              Object.entries(
                contacts
                  .filter(c => selectedClassFilter === 'Tất cả' || (c.className || 'Liên hệ khác') === selectedClassFilter)
                  .reduce((acc, c) => {
                    const group = c.className || 'Liên hệ khác';
                    if (!acc[group]) acc[group] = [];
                    acc[group].push(c);
                    return acc;
                  }, {} as Record<string, Contact[]>)
              ).map(([groupName, groupContacts]) => (
                <div key={groupName} className="space-y-1.5">
                  <div className="px-2 pt-1 pb-0.5 text-[10px] font-black text-sky-600 uppercase tracking-wider">
                    {groupName}
                  </div>
                  {groupContacts.map((c) => {
                    const isSelected = selectedContact?.id === c.id;
                    return (
                      <button
                        key={c.id}
                        onClick={() => handleSelectContact(c)}
                        className={`w-full text-left p-3 rounded-2xl transition-all duration-150 flex items-center gap-3 cursor-pointer ${
                          isSelected
                            ? 'bg-sky-500 text-white shadow-md shadow-sky-200'
                            : 'bg-white text-slate-700 hover:bg-sky-100/60 border border-sky-100'
                        }`}
                      >
                        <div className={`w-11 h-11 rounded-xl flex items-center justify-center font-black text-sm uppercase shrink-0 border-2 ${
                          isSelected ? 'bg-white/20 text-white border-white/40' : 'bg-sky-100 text-sky-600 border-sky-200'
                        }`}>
                          {c.fullName ? c.fullName.charAt(0).toUpperCase() : c.username.charAt(0).toUpperCase()}
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center justify-between gap-1">
                            <p className={`text-xs font-black truncate ${isSelected ? 'text-white' : 'text-slate-700'}`}>
                              {c.fullName || c.username}
                            </p>
                            {!isSelected && getRoleBadge(c.roles)}
                          </div>
                          
                          {/* Hiển thị rõ tên bé */}
                          {c.childInfo && (
                            <p className={`text-[11px] font-black mt-0.5 truncate ${isSelected ? 'text-amber-200' : 'text-amber-700'}`}>
                              👨‍👩‍👧 {c.childInfo}
                            </p>
                          )}

                          <p className={`text-[10px] font-bold mt-0.5 truncate ${isSelected ? 'text-sky-100' : 'text-slate-400'}`}>
                            {c.phoneNumber || c.email || 'Liên hệ'}
                          </p>
                        </div>
                      </button>
                    );
                  })}
                </div>
              ))
            )}
          </div>
        </div>

        {/* Right Column: Active Conversation */}
        <div className={`flex-1 bg-white flex-col w-full md:w-auto ${!selectedContact ? 'hidden md:flex' : 'flex'}`}>
          {selectedContact ? (
            <>
              {/* Chat Room Header */}
              <div className="h-16 sm:h-18 px-3 sm:px-6 border-b-2 border-sky-100 flex items-center justify-between bg-sky-50/30 shrink-0">
                <div className="flex items-center gap-2 sm:gap-3">
                  <button 
                    className="md:hidden p-1.5 -ml-1 text-sky-500 hover:bg-sky-100 rounded-xl transition-colors"
                    onClick={() => setSelectedContact(null)}
                  >
                    <ArrowLeft className="w-5 h-5" />
                  </button>
                  <div className="w-9 h-9 sm:w-11 sm:h-11 rounded-xl bg-sky-500 text-white flex items-center justify-center font-black text-sm border-2 border-white shadow-sm shrink-0">
                    {selectedContact.fullName ? selectedContact.fullName.charAt(0).toUpperCase() : selectedContact.username.charAt(0).toUpperCase()}
                  </div>
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-1 sm:gap-2">
                      <h3 className="text-xs sm:text-sm font-black text-slate-700 truncate max-w-[120px] sm:max-w-none">{selectedContact.fullName || selectedContact.username}</h3>
                      {getRoleBadge(selectedContact.roles)}
                    </div>
                    
                    {/* Hiển thị dòng thông tin Phụ huynh bé nào - thuộc lớp nào ở Header */}
                    {selectedContact.childInfo && (
                      <p className="text-[10px] font-black text-amber-800 bg-amber-50 px-2 py-0.5 rounded-lg border border-amber-200 inline-block mt-0.5 truncate max-w-[150px] sm:max-w-none">
                        🏫 {selectedContact.childInfo}
                      </p>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-1 sm:gap-2 text-slate-400 shrink-0">
                  <button className="p-1.5 sm:p-2 rounded-xl hover:bg-sky-100 hover:text-sky-600 transition-colors" title="Gọi điện">
                    <Phone className="w-4 h-4 sm:w-5 sm:h-5" />
                  </button>
                  <button className="hidden sm:block p-1.5 sm:p-2 rounded-xl hover:bg-sky-100 hover:text-sky-600 transition-colors" title="Gọi Video">
                    <Video className="w-4 h-4 sm:w-5 sm:h-5" />
                  </button>
                  <button className="p-1.5 sm:p-2 rounded-xl hover:bg-sky-100 hover:text-sky-600 transition-colors" title="Thông tin liên hệ">
                    <Info className="w-4 h-4 sm:w-5 sm:h-5" />
                  </button>
                </div>
              </div>

              {/* Messages Body */}
              <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-4 bg-sky-50/20">
                {loadingHistory ? (
                  <div className="flex items-center justify-center h-full">
                    <div className="w-8 h-8 border-4 border-sky-200 border-t-sky-500 rounded-full animate-spin"></div>
                  </div>
                ) : messages.length === 0 ? (
                  <div className="h-full flex flex-col items-center justify-center text-slate-400 text-xs font-bold gap-2 px-4 text-center">
                    <MessageSquare className="w-10 h-10 text-sky-200" />
                    <span>Hãy gửi tin nhắn đầu tiên để trò chuyện với {selectedContact.fullName || selectedContact.username}!</span>
                  </div>
                ) : (
                  messages.map((msg) => {
                    const isMe = msg.senderId === user?.id;
                    return (
                      <div
                        key={msg.id}
                        className={`flex ${isMe ? 'justify-end' : 'justify-start'}`}
                      >
                        <div className={`max-w-[85%] sm:max-w-md p-3 sm:p-4 rounded-2xl text-xs font-bold leading-relaxed shadow-sm flex flex-col ${
                          isMe
                            ? 'bg-gradient-to-r from-sky-400 to-blue-500 text-white rounded-br-sm'
                            : 'bg-white border-2 border-sky-100 text-slate-700 rounded-bl-sm'
                        }`}>
                          <p className="break-words">{msg.content}</p>
                          <span className={`text-[9px] font-black mt-1 text-right ${isMe ? 'text-sky-100' : 'text-slate-400'}`}>
                            {msg.sentAt ? new Date(msg.sentAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : 'Mới đây'}
                          </span>
                        </div>
                      </div>
                    );
                  })
                )}
                <div ref={chatEndRef} />
              </div>

              {/* Input Form */}
              <form onSubmit={handleSendMessage} className="p-3 sm:p-4 border-t-2 border-sky-100 bg-white flex gap-2 sm:gap-3 shrink-0">
                <input
                  type="text"
                  placeholder="Nhập nội dung..."
                  value={inputValue}
                  onChange={(e) => setInputValue(e.target.value)}
                  className="flex-1 min-w-0 bg-white border-2 border-sky-200 focus:border-sky-400 focus:outline-none rounded-2xl px-3 sm:px-4 py-2.5 sm:py-3 text-[11px] sm:text-xs font-bold text-slate-700 placeholder-slate-400"
                  required
                />
                <button
                  type="submit"
                  disabled={sending}
                  className="px-4 sm:px-5 py-2.5 sm:py-3 rounded-2xl bg-gradient-to-r from-sky-400 to-blue-500 text-white font-black text-[11px] sm:text-xs shadow-md active:scale-95 transition-all flex items-center justify-center gap-1.5 disabled:opacity-50 shrink-0"
                >
                  <Send className="w-4 h-4 shrink-0" />
                  <span className="hidden sm:inline">Gửi</span>
                </button>
              </form>
            </>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center text-slate-400 text-sm font-bold gap-3">
              <div className="w-16 h-16 rounded-2xl bg-sky-50 flex items-center justify-center">
                <User className="w-8 h-8 text-sky-300" />
              </div>
              <span>Chọn một người trong danh bạ bên trái để bắt đầu nhắn tin</span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Chat;
