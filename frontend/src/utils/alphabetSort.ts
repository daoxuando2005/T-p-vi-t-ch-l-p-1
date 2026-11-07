export const VIETNAMESE_ALPHABET_ORDER = [
  'a', 'ă', 'â', 'b', 'c', 'd', 'đ', 'e', 'ê', 'g', 'h', 'i', 'k', 'l', 'm', 
  'n', 'o', 'ô', 'ơ', 'p', 'q', 'r', 's', 't', 'u', 'ư', 'v', 'x', 'y'
];

export const sortVietnameseAlphabet = (data: any[], key = 'letter') => {
  return [...data].sort((a, b) => {
    const letterA = (a[key] || '').toLowerCase();
    const letterB = (b[key] || '').toLowerCase();
    const indexA = VIETNAMESE_ALPHABET_ORDER.indexOf(letterA);
    const indexB = VIETNAMESE_ALPHABET_ORDER.indexOf(letterB);
    return (indexA === -1 ? 999 : indexA) - (indexB === -1 ? 999 : indexB);
  });
};
