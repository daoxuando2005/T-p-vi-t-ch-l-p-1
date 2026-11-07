import type { EvaluationResult, Stroke, Point } from './types';
import { calculateDTW } from './dtw';
import { resample } from './preprocessing';

// Tính khoảng cách đối xứng Chamfer và điểm ngoại lai (Outlier)
function calculateSpatialOverlap(studentSeq: Point[], templateSeq: Point[]) {
  // 1. Khoảng cách từ từng điểm học sinh tới điểm gần nhất trên nét mẫu (Độ chính xác)
  let sumStoT = 0;
  let maxOutlier = 0;
  for (let i = 0; i < studentSeq.length; i++) {
    let minDist = Infinity;
    for (let j = 0; j < templateSeq.length; j++) {
      const dx = studentSeq[i].x - templateSeq[j].x;
      const dy = studentSeq[i].y - templateSeq[j].y;
      const d = Math.sqrt(dx * dx + dy * dy);
      if (d < minDist) minDist = d;
    }
    sumStoT += minDist;
    if (minDist > maxOutlier) maxOutlier = minDist;
  }
  const meanStoT = sumStoT / studentSeq.length;

  // 2. Khoảng cách từ từng điểm mẫu tới điểm gần nhất của học sinh (Độ bao phủ trọn vẹn nét chữ)
  let sumTtoS = 0;
  for (let j = 0; j < templateSeq.length; j++) {
    let minDist = Infinity;
    for (let i = 0; i < studentSeq.length; i++) {
      const dx = templateSeq[j].x - studentSeq[i].x;
      const dy = templateSeq[j].y - studentSeq[i].y;
      const d = Math.sqrt(dx * dx + dy * dy);
      if (d < minDist) minDist = d;
    }
    sumTtoS += minDist;
  }
  const meanTtoS = sumTtoS / templateSeq.length;

  return {
    meanStoT,
    meanTtoS,
    chamfer: (meanStoT + meanTtoS) / 2,
    maxOutlier
  };
}

export function evaluateStrokes(studentStrokes: Stroke[], templateStrokes: Stroke[]): EvaluationResult {
  if (studentStrokes.length === 0) {
    return { status: 'NO_INPUT', finalScore: null };
  }
  if (templateStrokes.length === 0) {
    return { status: 'TEMPLATE_NOT_FOUND', finalScore: null };
  }

  // Kết hợp toàn bộ điểm thành chuỗi đơn
  const rawStudentSequence = studentStrokes.flat();
  const rawTemplateSequence = templateStrokes.flat();

  if (rawStudentSequence.length === 0 || rawTemplateSequence.length === 0) {
    return { status: 'NO_INPUT', finalScore: 0 };
  }

  // Chuẩn hóa mật độ điểm 48 points
  const studentSequence = resample(rawStudentSequence, 48);
  const templateSequence = resample(rawTemplateSequence, 48);

  // 1. So khớp hình học không gian (Chamfer Distance & Outlier Detection)
  const spatial = calculateSpatialOverlap(studentSequence, templateSequence);

  // 2. So khớp dòng nét tuần tự 2 chiều (DTW)
  const dtwForward = calculateDTW(studentSequence, templateSequence);
  const reversedStudent = [...studentSequence].reverse();
  const dtwReversed = calculateDTW(reversedStudent, templateSequence);

  const bestDtw = dtwForward.normalizedDistance <= dtwReversed.normalizedDistance ? dtwForward : dtwReversed;
  const dtwDist = bestDtw.normalizedDistance;

  // Tính sai số tổng hợp (Composite Error):
  // - 45% độ bao phủ nét chữ (Chamfer)
  // - 25% quỹ đạo tuần tự nét (DTW)
  // - 30% phạt nặng các nét vẽ bậy/vẽ sai vùng (Outlier)
  const compositeError = 0.45 * spatial.chamfer + 0.25 * dtwDist + 0.30 * spatial.maxOutlier;

  let similarity: number;
  if (compositeError <= 0.05) {
    // Xuất sắc: 95 - 100 điểm
    similarity = 1.0 - (compositeError / 0.05) * 0.05;
  } else if (compositeError <= 0.08) {
    // Rất đẹp, bám sát chữ mẫu: 85 - 95 điểm
    similarity = 0.95 - ((compositeError - 0.05) / 0.03) * 0.10;
  } else if (compositeError <= 0.12) {
    // Khá tốt, nét hơi lệch nhẹ: 70 - 85 điểm
    similarity = 0.85 - ((compositeError - 0.08) / 0.04) * 0.15;
  } else if (compositeError <= 0.16) {
    // Đạt trung bình, nét còn vụng: 50 - 70 điểm
    similarity = 0.70 - ((compositeError - 0.12) / 0.04) * 0.20;
  } else if (compositeError <= 0.20) {
    // Nét vẽ sai lệch nhiều: 20 - 45 điểm
    similarity = 0.45 - ((compositeError - 0.16) / 0.04) * 0.25;
  } else if (compositeError <= 0.25) {
    // Rất tệ, vẽ lệch nhiều: 5 - 20 điểm
    similarity = 0.20 - ((compositeError - 0.20) / 0.05) * 0.15;
  } else {
    // Sai hình dáng hoàn toàn (vẽ dấu X, gạch bừa, vẽ hình khác): 0 - 5 điểm
    similarity = Math.max(0, 0.05 - ((compositeError - 0.25) / 0.15) * 0.05);
  }

  const finalScore = Math.min(100, Math.max(0, Math.round(similarity * 100)));

  return {
    status: 'SUCCESS',
    finalScore,
    dtwDistance: compositeError
  };
}
