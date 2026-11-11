package com.hocchu.web.config;

import com.hocchu.web.service.MediaStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import java.nio.file.Path;

@Configuration
public class MediaConfiguration implements WebMvcConfigurer {
    private final MediaStorageService storage;
    public MediaConfiguration(MediaStorageService storage) {this.storage=storage;}
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/public/media/**")
            .addResourceLocations(directory(storage.root()),directory(Path.of("../frontend/public").toAbsolutePath().normalize()))
            .setCachePeriod(0).resourceChain(true);
    }
    private static String directory(Path path) {String uri=path.toUri().toString();return uri.endsWith("/")?uri:uri+"/";}
}
