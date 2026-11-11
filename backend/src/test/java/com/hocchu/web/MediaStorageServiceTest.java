package com.hocchu.web;

import com.hocchu.web.service.MediaStorageService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class MediaStorageServiceTest {
    @TempDir Path directory;
    @Test void storesUnderConfiguredRootAndReturnsPublicApiUrl() throws Exception {
        var storage=new MediaStorageService(directory.toString());var file=new MockMultipartFile("file","a.png","image/png",new byte[]{1,2,3});
        var response=storage.store(file,"example_images","a.png");assertTrue(response.getSuccess());
        assertEquals("/api/public/media/example_images/a.png",response.getMessage());
        assertArrayEquals(file.getBytes(),Files.readAllBytes(directory.resolve("example_images/a.png")));
    }
    @Test void traversalAndExecutableExtensionsAreRejected() {
        var storage=new MediaStorageService(directory.toString());var file=new MockMultipartFile("file","x","image/png",new byte[]{1});
        for(String name:new String[]{"../escape.png","a/../../escape.png","a.html","a.svg","a.png/.."})assertFalse(storage.store(file,"alphabet",name).getSuccess());
        assertFalse(storage.store(file,"../other","a.png").getSuccess());
    }
}
