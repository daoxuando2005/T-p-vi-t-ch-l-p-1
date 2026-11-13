package com.hocchu.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/** Host-owned fixture lifecycle; the phone never receives real account credentials or changes its saved session. */
@EnabledIfSystemProperty(named="liveAndroid", matches="true")
class AndroidJourneyLiveTest extends LocalBackendFixture {
    @Test void nativeStudentSubmitsThreeTypesAndParentSeesTeacherFeedback() throws Exception {
        String serial=System.getProperty("androidSerial","");assertFalse(serial.isBlank(),"Set androidSerial explicitly");
        var student=account("ROLE_STUDENT");var teacher=account("ROLE_TEACHER");var parent=account("ROLE_PARENT");
        var adb=System.getProperty("adbPath",System.getenv("LOCALAPPDATA")+"/Android/Sdk/platform-tools/adb.exe");
        var args=new ArrayList<>(List.of(adb,"-s",serial,"shell","am","instrument","-w","-r","-e","disableAnalytics","true",
            "-e","class","com.hocchu.mobile.ClassroomLiveBackendTest","-e","liveBackend","true",
            "-e","backendUrl",System.getProperty("deviceBackendUrl","http://127.0.0.1:8080/")));
        for(var entry:Map.of("student",student,"teacher",teacher,"parent",parent).entrySet()) {
            args.addAll(List.of("-e",entry.getKey()+"Username",entry.getValue().username(),"-e",entry.getKey()+"Password",entry.getValue().password()));
        }
        args.add("com.hocchu.mobile.test/androidx.test.runner.AndroidJUnitRunner");
        Path output=Path.of("target/android-journey-instrumentation.log");
        Process process=null;
        try {
            process=new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(output.toFile()).start();
            boolean ended=process.waitFor(90,TimeUnit.SECONDS);
            if(!ended){process.destroyForcibly();new ProcessBuilder(adb,"-s",serial,"shell","am","force-stop","com.hocchu.mobile").start().waitFor(5,TimeUnit.SECONDS);}
            assertTrue(ended,"Android live check exceeded 90 seconds; see ignored target log");
            String result=Files.readString(output);
            assertTrue(result.contains("OK (1 test)"),"Android live check failed; inspect target/android-journey-instrumentation.log");
        } finally {
            if(process!=null&&process.isAlive())process.destroyForcibly();
            // Collect all classes owned by this disposable teacher even when the device fails mid-request.
            try(var p=db.prepareStatement("SELECT id FROM classes WHERE teacher_id=?")) {
                p.setLong(1,teacher.id());try(var rows=p.executeQuery()){while(rows.next())createdClasses.add(rows.getLong(1));}
            }
        }
    }
}
