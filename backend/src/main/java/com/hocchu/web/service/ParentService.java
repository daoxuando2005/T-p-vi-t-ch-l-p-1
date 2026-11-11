package com.hocchu.web.service;

import com.hocchu.web.dto.*;
import com.hocchu.web.repository.ParentStudentLinkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ParentService {
    @Autowired private ParentStudentLinkRepository links;
    @Autowired private LearningTrackingService tracking;
    @Transactional public ApiResponse linkStudent(Long parentId,String code) {return tracking.linkParent(parentId,code);}
    @Transactional(readOnly=true) public List<ParentLinkResponse> getMyStudents(Long parentId) {
        return links.findByParentIdAndStatus(parentId,"APPROVED").stream().map(link->ParentLinkResponse.builder()
            .id(link.getId()).parentId(parentId).parentName(link.getParent().getFullName())
            .studentId(link.getStudent().getId()).studentName(link.getStudent().getFullName()==null?link.getStudent().getUsername():link.getStudent().getFullName())
            .studentUsername(link.getStudent().getUsername()).status(link.getStatus()).linkedAt(link.getLinkedAt()).build()).toList();
    }
}
