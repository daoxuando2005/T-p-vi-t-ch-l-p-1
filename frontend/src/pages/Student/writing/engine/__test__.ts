import { WritingEvaluationEngine } from './index';
import { getTemplateForLetter } from './templateData';

function runTests() {
  console.log("=== BẮT ĐẦU CHẠY UNIT TEST ===");
  
  const charsToTest = ['a', 'b', 'c'];
  let foundCount = 0;
  
  for (const char of charsToTest) {
    const template = getTemplateForLetter(char);
    if (!template) {
      console.log(`❌ Template for '${char}' NOT FOUND`);
    } else {
      console.log(`✅ Template for '${char}' FOUND (Points: ${template.flat().length})`);
      foundCount++;
      
      // Test evaluation using the template against itself (perfect match)
      const eval1 = WritingEvaluationEngine.evaluate(template, char);
      console.log(`   TEST '${char}' against itself: Status = ${eval1.status}, Distance = ${eval1.dtwDistance?.toFixed(4)}, Score = ${eval1.finalScore}`);
    }
  }

  console.log(`\nTổng kết: ${foundCount}/${charsToTest.length} template mẫu.`);
  
  // Test missing template
  const evalMissing = WritingEvaluationEngine.evaluate([], 'xyz123');
  console.log(`TEST Missing Template: Status = ${evalMissing.status}, Score = ${evalMissing.finalScore}`);

  console.log("=== HOÀN THÀNH UNIT TEST ===");
}

runTests();
