import type { Point, Stroke, DTWResult } from './types';

function distance(p1: Point, p2: Point): number {
  const dx = p1.x - p2.x;
  const dy = p1.y - p2.y;
  return Math.sqrt(dx * dx + dy * dy);
}

export function calculateDTW(sequence1: Stroke, sequence2: Stroke): DTWResult {
  const n = sequence1.length;
  const m = sequence2.length;
  
  if (n === 0 || m === 0) {
    return { distance: Infinity, normalizedDistance: Infinity, path: [] };
  }

  const dtw: number[][] = Array(n + 1).fill(0).map(() => Array(m + 1).fill(Infinity));
  dtw[0][0] = 0;

  for (let i = 1; i <= n; i++) {
    for (let j = 1; j <= m; j++) {
      const cost = distance(sequence1[i - 1], sequence2[j - 1]);
      dtw[i][j] = cost + Math.min(
        dtw[i - 1][j],    // insertion
        dtw[i][j - 1],    // deletion
        dtw[i - 1][j - 1] // match
      );
    }
  }

  const rawDistance = dtw[n][m];
  
  // Backtracking to find path (for path length normalization)
  let i = n, j = m;
  const path: [number, number][] = [[n - 1, m - 1]];
  while (i > 1 || j > 1) {
    if (i === 1) j--;
    else if (j === 1) i--;
    else {
      const minPrev = Math.min(dtw[i - 1][j - 1], dtw[i - 1][j], dtw[i][j - 1]);
      if (minPrev === dtw[i - 1][j - 1]) { i--; j--; }
      else if (minPrev === dtw[i - 1][j]) i--;
      else j--;
    }
    path.push([i - 1, j - 1]);
  }
  if (i === 1 && j === 1 && path[path.length - 1][0] !== 0) {
     path.push([0,0]);
  }
  
  path.reverse();

  return {
    distance: rawDistance,
    normalizedDistance: rawDistance / path.length,
    path
  };
}
