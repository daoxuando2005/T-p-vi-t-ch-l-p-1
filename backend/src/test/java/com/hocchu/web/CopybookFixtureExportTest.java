package com.hocchu.web;
import com.hocchu.web.service.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit regeneration of small golden masks used to compare Android and Java geometry. */
@EnabledIfSystemProperty(named="exportCopybook",matches="true")
class CopybookFixtureExportTest {
    @Test void exportTemplates() throws Exception {
        Path root=Path.of("..").toAbsolutePath().normalize();
        Path directory=root.resolve("android/app/src/androidTest/assets/copybook").normalize();
        assertTrue(directory.startsWith(root.resolve("android/app/src/androidTest/assets")));
        Files.createDirectories(directory);var service=new HandwritingAssessment();
        for(String letter:LearningTrackingService.LETTERS)assertTrue(ImageIO.write(service.template(letter),"png",directory.resolve(letter+".png").toFile()));
    }
}
