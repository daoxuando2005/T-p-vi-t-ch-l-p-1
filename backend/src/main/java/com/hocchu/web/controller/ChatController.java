package com.hocchu.web.controller;

import com.hocchu.web.dto.*;
import com.hocchu.web.entity.*;
import com.hocchu.web.repository.*;
import com.hocchu.web.security.CurrentUser;
import com.hocchu.web.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class ChatController {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ParentStudentLinkRepository parentStudentLinkRepository;

    @Autowired
    private ClassMemberRepository classMemberRepository;

    @Autowired
    private ClazzRepository clazzRepository;

    @MessageMapping("/chat.message")
    public void processMessage(@Payload ChatMessagePayload payload) {
        User sender = userRepository.findById(payload.getSenderId()).orElse(null);
        User receiver = userRepository.findById(payload.getReceiverId()).orElse(null);

        if (sender != null && receiver != null) {
            Message message = Message.builder()
                    .sender(sender)
                    .receiver(receiver)
                    .content(payload.getContent())
                    .isRead(false)
                    .build();

            Message saved = messageRepository.save(message);

            MessageResponse response = MessageResponse.builder()
                    .id(saved.getId())
                    .senderId(sender.getId())
                    .senderName(sender.getFullName() != null ? sender.getFullName() : sender.getUsername())
                    .receiverId(receiver.getId())
                    .receiverName(receiver.getFullName() != null ? receiver.getFullName() : receiver.getUsername())
                    .content(saved.getContent())
                    .sentAt(LocalDateTime.now())
                    .isRead(false)
                    .build();

            // Gửi duy nhất cho người nhận để tránh trùng lặp tin nhắn
            simpMessagingTemplate.convertAndSendToUser(
                    String.valueOf(receiver.getId()),
                    "/queue/messages",
                    response
            );
        }
    }

    // REST API gửi tin nhắn (Fallback khi WebSocket đang kết nối hoặc lỗi)
    @PostMapping("/api/chat/send")
    @PreAuthorize("hasAnyRole('TEACHER', 'PARENT')")
    @Transactional
    public ResponseEntity<MessageResponse> sendChatMessage(
            @CurrentUser UserPrincipal currentUser,
            @RequestBody ChatMessagePayload payload) {
        User sender = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người gửi"));
        User receiver = userRepository.findById(payload.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người nhận"));

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(payload.getContent())
                .isRead(false)
                .build();

        Message saved = messageRepository.save(message);

        MessageResponse response = MessageResponse.builder()
                .id(saved.getId())
                .senderId(sender.getId())
                .senderName(sender.getFullName() != null ? sender.getFullName() : sender.getUsername())
                .receiverId(receiver.getId())
                .receiverName(receiver.getFullName() != null ? receiver.getFullName() : receiver.getUsername())
                .content(saved.getContent())
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();

        try {
            simpMessagingTemplate.convertAndSendToUser(
                    String.valueOf(receiver.getId()),
                    "/queue/messages",
                    response
            );
        } catch (Exception e) {}

        return ResponseEntity.ok(response);
    }

    // REST API lấy lịch sử trò chuyện
    @GetMapping("/api/chat/history/{contactId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'PARENT')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MessageResponse>> getChatHistory(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long contactId) {
        List<MessageResponse> history = messageRepository.findChatHistory(currentUser.getId(), contactId).stream()
                .map(m -> MessageResponse.builder()
                        .id(m.getId())
                        .senderId(m.getSender().getId())
                        .senderName(m.getSender().getFullName() != null ? m.getSender().getFullName() : m.getSender().getUsername())
                        .receiverId(m.getReceiver().getId())
                        .receiverName(m.getReceiver().getFullName() != null ? m.getReceiver().getFullName() : m.getReceiver().getUsername())
                        .content(m.getContent())
                        .sentAt(m.getSentAt())
                        .isRead(m.getIsRead())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    // REST API lấy danh sách liên hệ chat DÀNH RIÊNG CHO GIÁO VIÊN & PHỤ HUYNH (Kèm tên bé và lớp)
    @GetMapping("/api/chat/contacts")
    @PreAuthorize("hasAnyRole('TEACHER', 'PARENT')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<UserProfileResponse>> getChatContacts(@CurrentUser UserPrincipal currentUser) {
        Map<Long, UserProfileResponse> contactMap = new LinkedHashMap<>();
        
        boolean isTeacher = currentUser.getAuthorities().stream().anyMatch(a -> "ROLE_TEACHER".equals(a.getAuthority()));
        boolean isParent = currentUser.getAuthorities().stream().anyMatch(a -> "ROLE_PARENT".equals(a.getAuthority()));

        if (isTeacher) {
            // Giáo viên: Lấy danh sách Phụ huynh của các học sinh trong các lớp mình dạy (Kèm tên bé và tên lớp)
            List<Clazz> myClasses = clazzRepository.findByTeacherId(currentUser.getId());
            for (Clazz c : myClasses) {
                List<ClassMember> members = classMemberRepository.findByClazzId(c.getId());
                for (ClassMember cm : members) {
                    parentStudentLinkRepository.findByStudentId(cm.getStudent().getId()).ifPresent(link -> {
                        if ("APPROVED".equals(link.getStatus())) {
                            User parent = link.getParent();
                            String studentName = cm.getStudent().getFullName() != null ? cm.getStudent().getFullName() : cm.getStudent().getUsername();
                            String info = "Phụ huynh bé " + studentName + " (Lớp " + c.getName() + ")";
                            
                            UserProfileResponse dto = UserProfileResponse.builder()
                                    .id(parent.getId())
                                    .username(parent.getUsername())
                                    .email(parent.getEmail())
                                    .fullName(parent.getFullName() != null ? parent.getFullName() : parent.getUsername())
                                    .phoneNumber(parent.getPhoneNumber())
                                    .roles(parent.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                                    .childInfo(info)
                                    .className(c.getName())
                                    .build();
                            contactMap.put(parent.getId(), dto);
                        }
                    });
                }
            }
        } else if (isParent) {
            // Phụ huynh: Lấy danh sách Giáo viên chủ nhiệm của các lớp học mà con mình đang học
            List<ParentStudentLink> myLinks = parentStudentLinkRepository.findByParentIdAndStatus(currentUser.getId(), "APPROVED");
            for (ParentStudentLink link : myLinks) {
                String studentName = link.getStudent().getFullName() != null ? link.getStudent().getFullName() : link.getStudent().getUsername();
                List<ClassMember> memberships = classMemberRepository.findByStudentId(link.getStudent().getId());
                for (ClassMember cm : memberships) {
                    User teacher = cm.getClazz().getTeacher();
                    String info = "GVCN bé " + studentName + " (Lớp " + cm.getClazz().getName() + ")";
                    
                    UserProfileResponse dto = UserProfileResponse.builder()
                            .id(teacher.getId())
                            .username(teacher.getUsername())
                            .email(teacher.getEmail())
                            .fullName(teacher.getFullName() != null ? teacher.getFullName() : teacher.getUsername())
                            .phoneNumber(teacher.getPhoneNumber())
                            .roles(teacher.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                            .childInfo(info)
                            .build();
                    contactMap.put(teacher.getId(), dto);
                }
            }
        }

        return ResponseEntity.ok(new ArrayList<>(contactMap.values()));
    }

    public static class ChatMessagePayload {
        private Long senderId;
        private Long receiverId;
        private String content;

        public Long getSenderId() { return senderId; }
        public void setSenderId(Long senderId) { this.senderId = senderId; }
        public Long getReceiverId() { return receiverId; }
        public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
