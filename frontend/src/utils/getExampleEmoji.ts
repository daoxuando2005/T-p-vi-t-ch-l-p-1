export const getExampleEmoji = (word?: string, letter?: string): string => {
  const w = (word || '').toLowerCase().trim();
  const l = (letter || '').toLowerCase().trim();

  // Match by example word
  if (w.includes('ca') || w === 'ca') return '🥛';
  if (w.includes('mắt')) return '👁️';
  if (w.includes('nấm')) return '🍄';
  if (w.includes('bóng')) return '🎈';
  if (w.includes('cá')) return '🐟';
  if (w.includes('dưa')) return '🍉';
  if (w.includes('đu đủ') || w.includes('du du')) return '🥭';
  if (w.includes('xe máy') || w.includes('xe đạp')) return '🚲';
  if (w.includes('xe')) return '🚗';
  if (w.includes('bê')) return '🐂';
  if (w.includes('gà')) return '🐔';
  if (w.includes('hoa')) return '🌸';
  if (w.includes('bi')) return '🔮';
  if (w.includes('kéo')) return '✂️';
  if (w.includes('lá')) return '🍃';
  if (w.includes('mít')) return '🍍';
  if (w.includes('na')) return '🥑';
  if (w.includes('tròn') || w.includes('trứng')) return '🥚';
  if (w.includes('ô') || w.includes('dù')) return '🌂';
  if (w.includes('cờ')) return '🚩';
  if (w.includes('pin')) return '🔋';
  if (w.includes('quýt') || w.includes('cam')) return '🍊';
  if (w.includes('rùa')) return '🐢';
  if (w.includes('sao')) return '⭐';
  if (w.includes('tàu') || w.includes('thuyền')) return '🚢';
  if (w.includes('mũ') || w.includes('nón')) return '🧢';
  if (w.includes('sư tử')) return '🦁';
  if (w.includes('vịt')) return '🦆';
  if (w.includes('yếm')) return '👶';
  if (w.includes('mèo')) return '🐱';
  if (w.includes('chó')) return '🐶';
  if (w.includes('voi')) return '🐘';
  if (w.includes('hổ')) return '🐯';
  if (w.includes('sách')) return '📚';
  if (w.includes('bút')) return '✏️';

  // Fallback by letter
  const letterMap: Record<string, string> = {
    'a': '🥛',
    'ă': '👁️',
    'â': '🍄',
    'b': '🎈',
    'c': '🐟',
    'd': '🍉',
    'đ': '🥭',
    'e': '🚗',
    'ê': '🐂',
    'g': '🐔',
    'h': '🌸',
    'i': '🔮',
    'k': '✂️',
    'l': '🍃',
    'm': '🍍',
    'n': '🥑',
    'o': '🥚',
    'ô': '🌂',
    'ơ': '🚩',
    'p': '🔋',
    'q': '🍊',
    'r': '🐢',
    's': '⭐',
    't': '🚢',
    'u': '🧢',
    'ư': '🦁',
    'v': '🦆',
    'x': '🚲',
    'y': '👶',
  };

  if (l && letterMap[l]) return letterMap[l];
  return '✨';
};
