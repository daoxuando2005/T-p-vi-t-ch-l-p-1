package com.hocchu.web;
import com.hocchu.web.service.AssignmentRules;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AssignmentRulesTest {
    final String content="{\"sentences\":[{\"words\":[\"Bé\",\"yêu\",\"mẹ.\"]},{\"words\":[\"ba\",\"ba\",\"ơi\"]}]}";
    @Test void sentenceScoringUsesActualOrderAndHandlesIdenticalWordTiles() {
        assertEquals(10f,AssignmentRules.scoreSentences(content,"{\"sentences\":[[0,1,2],[1,0,2]]}"));
        assertEquals(5f,AssignmentRules.scoreSentences(content,"{\"sentences\":[[1,0,2],[0,1,2]]}"));
    }
    @Test void duplicateAndOutOfRangeTilesCannotBeSubmitted() {
        for(String row:new String[]{"[0,0,2]","[0,1,3]","[0,1]","[0,1.0,2]"})
            assertThrows(IllegalArgumentException.class,()->AssignmentRules.scoreSentences(content,"{\"sentences\":["+row+",[0,1,2]]}"));
    }
    @Test void teacherAndGeneratedDraftsMustHaveValidAnswersAndLetters() {
        assertThrows(IllegalArgumentException.class,()->AssignmentRules.validate("QUIZ","[{\"question\":\"Chọn a\",\"options\":[\"a\",\"b\"],\"answer\":\"c\"}]"));
        assertThrows(IllegalArgumentException.class,()->AssignmentRules.validate("WRITING","{\"letters\":[\"A\",\"F\"]}"));
        assertThrows(IllegalArgumentException.class,()->AssignmentRules.validate("SENTENCE","{\"sentences\":[{\"words\":[\"Bé\"]}]}"));
    }
}
