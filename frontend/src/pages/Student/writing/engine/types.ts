export interface Point {
  x: number;
  y: number;
  timestamp?: number;
}

export type Stroke = Point[];

export interface DTWResult {
  distance: number;
  normalizedDistance: number;
  path: [number, number][];
}

export interface EvaluationResult {
  status: 'SUCCESS' | 'TEMPLATE_NOT_FOUND' | 'NO_INPUT';
  finalScore: number | null;
  dtwDistance?: number;
}
