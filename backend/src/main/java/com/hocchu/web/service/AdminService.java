package com.hocchu.web.service;

import com.hocchu.web.dto.*;
import com.hocchu.web.entity.*;
import com.hocchu.web.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ClazzRepository clazzRepository;

    @Autowired
    private AlphabetLessonRepository alphabetLessonRepository;

    @Autowired
    private FeedbackReportRepository feedbackReportRepository;

    @Autowired
    private WritingPracticeRepository writingPracticeRepository;

    @Autowired
    private com.hocchu.web.repository.NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> UserProfileResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .email(u.getEmail())
                        .fullName(u.getFullName())
                        .phoneNumber(u.getPhoneNumber())
                        .enabled(u.getEnabled() != null && u.getEnabled())
                        .roles(u.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public ApiResponse toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        // Tránh khoá chính mình (Admin)
        if ("admin".equals(user.getUsername())) {
            return new ApiResponse(false, "Không thể khóa tài khoản Admin tối cao!");
        }

        user.setEnabled(!user.getEnabled());
        userRepository.save(user);

        String status = user.getEnabled() ? "mở khóa" : "tạm khóa";

        // Gửi thông báo đến Quả chuông hệ thống của người dùng
        try {
            com.hocchu.web.entity.Notification notification = com.hocchu.web.entity.Notification.builder()
                    .user(user)
                    .title(user.getEnabled() ? "🔓 Thông Báo Mở Khóa Tài Khoản" : "🔒 Thông Báo Tạm Khóa Tài Khoản")
                    .content("Tài khoản của bạn đã được Quản trị viên hệ thống " + status + ".")
                    .type("SYSTEM")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ApiResponse(true, "Đã " + status + " tài khoản '" + user.getUsername() + "' thành công");
    }

    @Transactional
    public ApiResponse changeUserRole(Long userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if ("admin".equals(user.getUsername())) {
            return new ApiResponse(false, "Tài khoản Admin tối cao giữ quyền Quản trị viên hệ thống!");
        }

        // Giữ lại các quyền hành động chi tiết (PERM_...), chỉ thay đổi Vai trò chính (ROLE_...)
        Set<Role> updatedRoles = user.getRoles().stream()
                .filter(r -> !r.getName().startsWith("ROLE_"))
                .collect(Collectors.toSet());

        Role newRoleEntity = roleRepository.findByName(newRole)
                .orElseGet(() -> roleRepository.save(new Role(null, newRole)));
        updatedRoles.add(newRoleEntity);

        user.setRoles(updatedRoles);
        userRepository.save(user);

        // Gửi thông báo đến Quả chuông hệ thống của người dùng
        try {
            com.hocchu.web.entity.Notification notification = com.hocchu.web.entity.Notification.builder()
                    .user(user)
                    .title("🛡️ Thông Báo Cập Nhật Vai Trò Tài Khoản")
                    .content("Vai trò tài khoản của bạn vừa được Quản trị viên cập nhật thành: " + newRole)
                    .type("SYSTEM")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ApiResponse(true, "Đã cập nhật vai trò tài khoản '" + user.getUsername() + "' thành " + newRole);
    }

    @Transactional
    public ApiResponse updateUserInfo(Long userId, Map<String, String> payload) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        String fullName = payload.get("fullName");
        String email = payload.get("email");
        String newPassword = payload.get("newPassword");

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        if (email != null && !email.isBlank()) {
            user.setEmail(email.trim());
        }
        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < 6) {
                return new ApiResponse(false, "Mật khẩu mới phải có ít nhất 6 ký tự!");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        userRepository.save(user);
        return new ApiResponse(true, "Đã cập nhật thông tin tài khoản '" + user.getUsername() + "' thành công!");
    }

    @Transactional
    public ApiResponse updateUserPermissions(Long userId, List<String> permissions) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if ("admin".equals(user.getUsername())) {
            return new ApiResponse(false, "Tài khoản Admin tối cao luôn có toàn bộ quyền hệ thống!");
        }

        // Tìm vai trò chính hiện tại của user (ROLE_TEACHER, ROLE_PARENT, ROLE_STUDENT...)
        String primaryRoleName = user.getRoles().stream()
                .map(Role::getName)
                .filter(name -> name.startsWith("ROLE_"))
                .findFirst()
                .orElse("ROLE_STUDENT");

        // Đặc biệt: Tài khoản Học Sinh chỉ dùng quyền học tập mặc định, không được cấp thêm các quyền khác!
        if ("ROLE_STUDENT".equals(primaryRoleName)) {
            return new ApiResponse(false, "Tài khoản Học sinh chỉ có quyền học tập & làm bài mặc định, không thể cấp thêm các quyền quản lý!");
        }

        Set<Role> roles = new HashSet<>();
        
        // Đảm bảo luôn giữ Vai trò chính
        Role primaryRole = roleRepository.findByName(primaryRoleName)
                .orElseGet(() -> roleRepository.save(new Role(null, primaryRoleName)));
        roles.add(primaryRole);

        // Thêm tất cả các quyền được tick chọn
        for (String permName : permissions) {
            Role role = roleRepository.findByName(permName)
                    .orElseGet(() -> roleRepository.save(new Role(null, permName)));
            roles.add(role);
        }

        user.setRoles(roles);
        userRepository.save(user);

        // Gửi thông báo đến Quả chuông hệ thống của người dùng
        try {
            Map<String, String> permLabels = Map.of(
                "PERM_ASSIGN_HOMEWORK", "📝 Quyền Giao Bài Tập",
                "PERM_VIEW_ALPHABET", "📚 Quyền Xem Học Liệu Chữ Cái",
                "PERM_EDIT_ALPHABET", "✏️ Quyền Sửa Học Liệu Chữ Cái",
                "PERM_ADD_ALPHABET", "➕ Quyền Thêm/Xóa Chữ Cái Mới",
                "PERM_MANAGE_CLASSES", "🏫 Quyền Quản Lý Lớp Học",
                "PERM_PARENT_TRACKING", "👨‍👩‍👧 Quyền Theo Dõi Tiến Độ Con"
            );

            List<String> grantedTitles = permissions.stream()
                    .map(p -> permLabels.getOrDefault(p, p))
                    .collect(Collectors.toList());

            String permsSummary = grantedTitles.isEmpty() 
                    ? "Quyền sử dụng cơ bản." 
                    : String.join(", ", grantedTitles);

            com.hocchu.web.entity.Notification notification = com.hocchu.web.entity.Notification.builder()
                    .user(user)
                    .title("🔑 Thông Báo Cập Nhật Phân Quyền Chi Tiết")
                    .content("Quản trị viên vừa cập nhật danh sách các quyền hành động của bạn: " + permsSummary)
                    .type("SYSTEM")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ApiResponse(true, "Cập nhật phân quyền thành công cho tài khoản '" + user.getUsername() + "'!");
    }

    @Transactional
    public ApiResponse deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if ("admin".equals(user.getUsername())) {
            return new ApiResponse(false, "Không thể xóa tài khoản Admin tối cao!");
        }

        userRepository.delete(user);
        return new ApiResponse(true, "Đã xóa tài khoản '" + user.getUsername() + "' thành công");
    }

    // CRUD Bảng chữ cái
    @Transactional
    public AlphabetLesson saveAlphabetLesson(AlphabetLesson lesson) {
        return alphabetLessonRepository.save(lesson);
    }

    @Transactional
    public void deleteAlphabetLesson(Long id) {
        alphabetLessonRepository.deleteById(id);
    }

    @Autowired
    private MediaStorageService mediaStorage;

    public ApiResponse uploadAlphabetImage(org.springframework.web.multipart.MultipartFile file,String filename) {
        return mediaStorage.store(file,"alphabet",filename);
    }
    public ApiResponse uploadAlphabetAudio(org.springframework.web.multipart.MultipartFile file,String filename) {
        return mediaStorage.store(file,"audio",filename);
    }
    public ApiResponse uploadAlphabetExampleImage(org.springframework.web.multipart.MultipartFile file,String filename) {
        return mediaStorage.store(file,"example_images",filename);
    }

    // Thống kê hệ thống
    public Map<String, Object> getSystemStats() {
        long totalUsers = userRepository.count();
        long totalClasses = clazzRepository.count();
        long totalPractices = writingPracticeRepository.count();
        long totalFeedbacks = feedbackReportRepository.count();

        // Đếm theo role
        long studentCount = userRepository.findAll().stream().filter(u -> u.getRoles().stream().anyMatch(r -> "ROLE_STUDENT".equals(r.getName()))).count();
        long teacherCount = userRepository.findAll().stream().filter(u -> u.getRoles().stream().anyMatch(r -> "ROLE_TEACHER".equals(r.getName()))).count();
        long parentCount = userRepository.findAll().stream().filter(u -> u.getRoles().stream().anyMatch(r -> "ROLE_PARENT".equals(r.getName()))).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalClasses", totalClasses);
        stats.put("totalPractices", totalPractices);
        stats.put("totalFeedbacks", totalFeedbacks);
        stats.put("studentCount", studentCount);
        stats.put("teacherCount", teacherCount);
        stats.put("parentCount", parentCount);
        return stats;
    }

    @Transactional
    public ApiResponse createUser(AdminCreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return new ApiResponse(false, "Tên đăng nhập '" + request.getUsername() + "' đã tồn tại!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return new ApiResponse(false, "Email '" + request.getEmail() + "' đã được sử dụng!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setEnabled(true);

        Set<Role> roles = new HashSet<>();
        String roleName = "ROLE_" + request.getRole().toUpperCase();
        Role primaryRole = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(null, roleName)));
        roles.add(primaryRole);

        if (request.getPermissions() != null) {
            for (String perm : request.getPermissions()) {
                Role role = roleRepository.findByName(perm)
                        .orElseGet(() -> roleRepository.save(new Role(null, perm)));
                roles.add(role);
            }
        } else {
            if ("TEACHER".equals(request.getRole().toUpperCase())) {
                List<String> defaultTeacherPerms = List.of(
                        "PERM_ASSIGN_HOMEWORK", "PERM_MANAGE_CLASSES",
                        "PERM_VIEW_ALPHABET", "PERM_EDIT_ALPHABET"
                );
                for (String perm : defaultTeacherPerms) {
                    Role role = roleRepository.findByName(perm)
                            .orElseGet(() -> roleRepository.save(new Role(null, perm)));
                    roles.add(role);
                }
            } else if ("PARENT".equals(request.getRole().toUpperCase())) {
                List<String> defaultParentPerms = List.of(
                        "PERM_PARENT_TRACKING"
                );
                for (String perm : defaultParentPerms) {
                    Role role = roleRepository.findByName(perm)
                            .orElseGet(() -> roleRepository.save(new Role(null, perm)));
                    roles.add(role);
                }
            }
        }

        user.setRoles(roles);
        userRepository.save(user);

        return new ApiResponse(true, "Tạo người dùng '" + request.getUsername() + "' thành công!");
    }
}
