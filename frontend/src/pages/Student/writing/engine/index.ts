import type { Stroke, EvaluationResult } from './types';
import { preprocessStrokes } from './preprocessing';
import { evaluateStrokes } from './scoring';
import { getTemplateForLetter } from './templateData';

export class WritingEvaluationEngine {
  public static evaluate(studentRawStrokes: Stroke[], letter: string): EvaluationResult {
    const rawTemplate = getTemplateForLetter(letter);
    
    if (!rawTemplate) {
      console.warn(`[WritingEngine] No template data found for letter '${letter}'.`);
      return { status: 'TEMPLATE_NOT_FOUND', finalScore: null };
    }

    // Preprocess both student and template strokes to ensure they are on the same normalized scale
    // We resample them to standard lengths (e.g., 32 points per stroke)
    const normalizedStudent = preprocessStrokes(studentRawStrokes, 32);
    const normalizedTemplate = preprocessStrokes(rawTemplate, 32);

    const result = evaluateStrokes(normalizedStudent, normalizedTemplate);
    
    console.debug(`[WritingEngine] Evaluation for '${letter}': Status = ${result.status}, Final = ${result.finalScore}`);
    
    return result;
  }
}
