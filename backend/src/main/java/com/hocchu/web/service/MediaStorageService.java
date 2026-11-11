package com.hocchu.web.service;

import com.hocchu.web.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.util.*;

@Service
public class MediaStorageService {
    private final Path root;
    public MediaStorageService(@Value("${app.media.directory:./uploads}") String directory) {root=Paths.get(directory).toAbsolutePath().normalize();}
    public Path root() {return root;}
    public ApiResponse store(MultipartFile file,String folder,String filename) {
        if(!Set.of("alphabet","audio","example_images").contains(folder)||filename==null||!filename.matches("[\\p{L}0-9_-][\\p{L}0-9_.-]{0,150}")||filename.contains(".."))
            return new ApiResponse(false,"Tên tệp chưa hợp lệ.");
        String extension=filename.substring(filename.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
        var allowed=folder.equals("audio")?Set.of("mp3","wav","ogg","m4a","mp4","webm","mpeg"):Set.of("png","jpg","jpeg","webp");
        if(file.isEmpty()||file.getSize()>8*1024*1024||!allowed.contains(extension)) return new ApiResponse(false,"Chọn tệp ảnh/âm thanh phù hợp, tối đa 8 MB.");
        Path target=root.resolve(folder).resolve(filename).normalize();
        if(!target.startsWith(root.resolve(folder))) return new ApiResponse(false,"Tên tệp chưa hợp lệ.");
        try {Files.createDirectories(target.getParent());try(var stream=file.getInputStream()){Files.copy(stream,target,StandardCopyOption.REPLACE_EXISTING);}}
        catch(Exception e){return new ApiResponse(false,"Chưa lưu được tệp. Hãy kiểm tra bộ nhớ máy chủ.");}
        return new ApiResponse(true,"/api/public/media/"+folder+"/"+filename);
    }
    public static String publicUrl(String raw) {
        if(raw==null||raw.isBlank()) return "";
        String value=raw.trim();
        for(String folder:List.of("alphabet","audio","example_images")) {
            String prefix="/"+folder+"/";
            if(value.startsWith(prefix)) return "/api/public/media"+value;
            for(String origin:List.of("http://localhost:5173","http://127.0.0.1:5173"))
                if(value.startsWith(origin+prefix)) return "/api/public/media"+value.substring(origin.length());
        }
        return value;
    }
}
