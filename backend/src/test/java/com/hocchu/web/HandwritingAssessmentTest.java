package com.hocchu.web;
import com.hocchu.web.service.*;
import org.junit.jupiter.api.Test;
import java.awt.*;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;

class HandwritingAssessmentTest {
    final HandwritingAssessment service=new HandwritingAssessment();
    @Test void all29TemplatesCanBeAssessedWithoutLosingAccentsOrDescenders() {
        for(String letter:LearningTrackingService.LETTERS)assertEquals(100,service.assess(letter,service.template(letter)).score(),letter);
    }
    @Test void blankPageAndFilledBoardDoNotReceiveGoodGrades() {
        var blank=new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB);var g=blank.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,256,256);g.dispose();
        assertEquals(0,service.assess("A",blank).score());
        assertTrue(service.assess("A",new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB)).score()<35);
    }
    @Test void shiftedInkIsPenalizedAndMissingDiacriticsAreReported() {
        var shifted=new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB);var g=shifted.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,256,256);g.drawImage(service.template("A"),70,0,null);g.dispose();
        assertTrue(service.assess("A",shifted).score()<60);
        var missing=service.assess("Ă",service.template("A"));assertTrue(missing.score()<80);assertTrue(missing.missingParts()>0);
    }
}
