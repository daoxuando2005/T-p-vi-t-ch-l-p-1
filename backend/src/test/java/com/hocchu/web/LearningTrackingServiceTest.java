package com.hocchu.web;

import com.hocchu.web.entity.*;
import com.hocchu.web.repository.*;
import com.hocchu.web.service.LearningTrackingService;
import com.hocchu.web.service.LearningTrackingService.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LearningTrackingServiceTest {
    @Mock JdbcTemplate jdbc;
    @Mock ParentStudentLinkRepository links;
    @Mock UserRepository users;
    @Mock SubmissionRepository submissions;
    @Mock WritingPracticeRepository practices;
    @InjectMocks LearningTrackingService service;
    List<Progress> emptyProgress() {return LearningTrackingService.LETTERS.stream().map(l->new Progress(l,false,0,0,0)).toList();}

    @Test void pendingAndUnrelatedParentsCannotRead() {
        when(links.findByParentIdAndStudentId(2L,1L)).thenReturn(Optional.of(ParentStudentLink.builder().status("PENDING").build()));
        assertThrows(AccessDeniedException.class,()->service.requireLinked(2L,1L));
        when(links.findByParentIdAndStudentId(2L,1L)).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class,()->service.requireLinked(2L,1L));
        verifyNoInteractions(jdbc,submissions,practices);
    }
    @Test void skippedLessonsAreRejectedBeforeAnyWrite() {
        var progress=new ArrayList<>(emptyProgress());progress.set(1,new Progress("Ă",true,0,0,0));
        assertThrows(IllegalArgumentException.class,()->service.sync(1L,new Sync(List.of(),progress)));
        verifyNoInteractions(jdbc);
    }
    @Test void malformedAndFutureEventsCannotPartiallyWriteProgress() {
        for(Event event:List.of(new Event(UUID.randomUUID().toString(),null,"A","FREE",false,Instant.now()),
            new Event("invalid","READING","A","FREE",false,Instant.now()),
            new Event(UUID.randomUUID().toString(),"READING","A","FREE",false,Instant.now().plusSeconds(600)))) {
            assertThrows(IllegalArgumentException.class,()->service.sync(1L,new Sync(List.of(event),emptyProgress())));
        }
        verifyNoInteractions(jdbc);
    }
    @Test void usernameCannotBeUsedToClaimAChild() {
        assertFalse(service.linkParent(2L,"student1").getSuccess());
        verifyNoInteractions(jdbc,links,users);
    }
}
