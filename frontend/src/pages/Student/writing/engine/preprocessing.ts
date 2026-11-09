import type { Stroke } from './types';

export function removeDuplicates(stroke: Stroke): Stroke {
  if (stroke.length === 0) return [];
  const result: Stroke = [stroke[0]];
  for (let i = 1; i < stroke.length; i++) {
    const prev = result[result.length - 1];
    const curr = stroke[i];
    if (prev.x !== curr.x || prev.y !== curr.y) {
      result.push(curr);
    }
  }
  return result;
}

export function filterEmptyStrokes(strokes: Stroke[]): Stroke[] {
  return strokes.filter(stroke => stroke.length > 1);
}

// Resample a stroke to a fixed number of points
export function resample(stroke: Stroke, numPoints: number = 32): Stroke {
  if (stroke.length === 0) return [];
  if (stroke.length === 1) {
    const p = stroke[0];
    return Array(numPoints).fill(p);
  }

  // Calculate total length
  let totalLength = 0;
  for (let i = 1; i < stroke.length; i++) {
    const dx = stroke[i].x - stroke[i - 1].x;
    const dy = stroke[i].y - stroke[i - 1].y;
    totalLength += Math.sqrt(dx * dx + dy * dy);
  }

  const interval = totalLength / (numPoints - 1);
  let distance = 0;
  const result: Stroke = [{ x: stroke[0].x, y: stroke[0].y, timestamp: stroke[0].timestamp }];
  
  let i = 1;
  let prevPoint = stroke[0];

  while (i < stroke.length && result.length < numPoints - 1) {
    const dx = stroke[i].x - prevPoint.x;
    const dy = stroke[i].y - prevPoint.y;
    const segmentLength = Math.sqrt(dx * dx + dy * dy);

    if (distance + segmentLength >= interval) {
      const ratio = (interval - distance) / segmentLength;
      const nx = prevPoint.x + ratio * dx;
      const ny = prevPoint.y + ratio * dy;
      
      const newPoint = { x: nx, y: ny, timestamp: stroke[i].timestamp };
      result.push(newPoint);
      
      // Update prevPoint to the new interpolated point, do NOT mutate original array
      prevPoint = newPoint;
      distance = 0;
    } else {
      distance += segmentLength;
      prevPoint = stroke[i];
      i++;
    }
  }

  // Ensure the last point is included if we haven't reached numPoints
  while (result.length < numPoints) {
    const lastP = stroke[stroke.length - 1];
    result.push({ x: lastP.x, y: lastP.y, timestamp: lastP.timestamp });
  }

  return result;
}

export function normalizeCoordinates(strokes: Stroke[]): Stroke[] {
  if (strokes.length === 0) return [];
  let minX = Infinity, minY = Infinity;
  let maxX = -Infinity, maxY = -Infinity;

  strokes.forEach(stroke => {
    stroke.forEach(p => {
      if (p.x < minX) minX = p.x;
      if (p.y < minY) minY = p.y;
      if (p.x > maxX) maxX = p.x;
      if (p.y > maxY) maxY = p.y;
    });
  });

  const width = maxX - minX;
  const height = maxY - minY;
  const range = Math.max(width, height);
  if (range === 0) return strokes.map(s => s.map(p => ({ x: 0, y: 0, timestamp: p.timestamp })));

  return strokes.map(stroke => 
    stroke.map(p => ({
      x: (p.x - minX) / range, // Using max range to preserve aspect ratio
      y: (p.y - minY) / range,
      timestamp: p.timestamp
    }))
  );
}

export function preprocessStrokes(strokes: Stroke[], targetPoints: number = 32): Stroke[] {
  let cleaned = strokes.map(removeDuplicates);
  cleaned = filterEmptyStrokes(cleaned);
  cleaned = cleaned.map(s => resample(s, targetPoints));
  return normalizeCoordinates(cleaned);
}
