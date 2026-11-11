package com.hocchu.web.controller;

import com.hocchu.web.service.LearningContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
public class LearningContentController {
    @Autowired private LearningContentService content;
    @GetMapping("/content")
    public List<Map<String,Object>> content() {return content.content();}
}
