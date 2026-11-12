import { useEffect, useId, useRef } from 'react';
import { X } from 'lucide-react';

export default function TeacherDialog({ title, children, onClose, busy = false, wide = false }) {
  const ref = useRef(null); const id = useId();
  useEffect(() => { const dialog = ref.current; dialog.showModal(); return () => dialog.close(); }, []);
  return <dialog ref={ref} className="teacher-dialog" style={wide ? { width: 'min(1060px,94vw)' } : undefined} aria-labelledby={id}
    onCancel={event => { event.preventDefault(); if (!busy) onClose(); }}>
    <div className="teacher-dialog-head"><h2 id={id}>{title}</h2><button type="button" className="teacher-icon" onClick={onClose} disabled={busy} aria-label="Đóng"><X/></button></div>
    <div className="teacher-dialog-body">{children}</div>
  </dialog>;
}
