package com.hocchu.web.service;

import com.hocchu.web.dto.*;
import com.hocchu.web.entity.*;
import com.hocchu.web.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.nio.charset.StandardCharsets;

@Service
public class AdminService {
    public static final Set<String> PRIMARY_ROLES=Set.of("ROLE_ADMIN","ROLE_TEACHER","ROLE_PARENT","ROLE_STUDENT");
    public static final Set<String> TEACHER_PERMISSIONS=Set.of("PERM_ASSIGN_HOMEWORK","PERM_MANAGE_CLASSES","PERM_VIEW_ALPHABET","PERM_EDIT_ALPHABET");
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AlphabetLessonRepository alphabetLessonRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MediaStorageService mediaStorage;
    @Autowired private AdminOperationsService operations;

    @Transactional(readOnly=true)
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream().map(u->UserProfileResponse.builder().id(u.getId())
            .username(u.getUsername()).email(u.getEmail()).fullName(u.getFullName()).phoneNumber(u.getPhoneNumber())
            .enabled(Boolean.TRUE.equals(u.getEnabled())).roles(u.getRoles().stream().map(Role::getName).sorted().toList()).build()).toList();
    }
    private User user(Long id) {return userRepository.findForAdminUpdate(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy tài khoản."));}
    private static boolean admin(User u){return u.getRoles().stream().anyMatch(r->r.getName().equals("ROLE_ADMIN"));}
    private void lockAdministrators() {
        jdbc.queryForList("SELECT id FROM users WHERE id IN (SELECT ur.user_id FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE r.name='ROLE_ADMIN') ORDER BY id FOR UPDATE");
    }
    private void protectAdministrator(Long actorId,User target) {
        if(Objects.equals(actorId,target.getId()))throw new IllegalArgumentException("Không thể tự khóa, xóa hoặc đổi vai trò tài khoản đang đăng nhập.");
        if(admin(target)&&Boolean.TRUE.equals(target.getEnabled())) {
            long active=jdbc.queryForObject("SELECT COUNT(DISTINCT u.id) FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id WHERE r.name='ROLE_ADMIN' AND u.enabled=TRUE",Long.class);
            if(active<=1)throw new IllegalArgumentException("Phải giữ ít nhất một quản trị viên đang hoạt động.");
        }
    }
    @Transactional
    public ApiResponse toggleUserStatus(Long actorId,Long id) {
        lockAdministrators();User target=user(id);
        if(Boolean.TRUE.equals(target.getEnabled()))protectAdministrator(actorId,target);
        target.setEnabled(!Boolean.TRUE.equals(target.getEnabled()));target.setSessionVersion(target.getSessionVersion()+1);
        userRepository.save(target);return new ApiResponse(true,target.getEnabled()?"Đã mở khóa. Người dùng đăng nhập lại để tiếp tục.":"Đã khóa tài khoản và ngừng hiệu lực các phiên cũ.");
    }
    public static String roleName(String input) {
        if(input==null)throw new IllegalArgumentException("Chọn vai trò tài khoản.");
        String role=input.trim().toUpperCase(Locale.ROOT);if(!role.startsWith("ROLE_"))role="ROLE_"+role;
        if(!PRIMARY_ROLES.contains(role))throw new IllegalArgumentException("Vai trò chưa hợp lệ.");return role;
    }
    public static List<String> permissionsFor(String role,List<String> requested) {
        if(!role.equals("ROLE_TEACHER")) {
            if(requested!=null&&!requested.isEmpty())throw new IllegalArgumentException("Chỉ cấp quyền quản lý chi tiết cho giáo viên.");
            return role.equals("ROLE_PARENT")?List.of("PERM_PARENT_TRACKING"):List.of();
        }
        List<String> values=requested==null?List.of("PERM_ASSIGN_HOMEWORK","PERM_MANAGE_CLASSES","PERM_VIEW_ALPHABET","PERM_EDIT_ALPHABET"):requested;
        if(values.stream().anyMatch(Objects::isNull)||!TEACHER_PERMISSIONS.containsAll(values)||new HashSet<>(values).size()!=values.size())throw new IllegalArgumentException("Quyền giáo viên chưa hợp lệ.");
        if(values.contains("PERM_EDIT_ALPHABET")&&!values.contains("PERM_VIEW_ALPHABET"))throw new IllegalArgumentException("Cần quyền xem học liệu trước khi cấp quyền chỉnh sửa.");
        return List.copyOf(values);
    }
    private Set<Role> roles(String role,List<String> permissions) {
        Set<Role> result=new HashSet<>();var names=new ArrayList<>(permissions);names.add(role);
        for(String name:names)result.add(roleRepository.findByName(name).orElseThrow(()->new IllegalStateException("Thiếu cấu hình vai trò hệ thống.")));
        return result;
    }
    @Transactional
    public ApiResponse changeUserRole(Long actorId,Long id,String requested) {
        String role=roleName(requested);lockAdministrators();User target=user(id);
        if(target.getRoles().stream().anyMatch(r->r.getName().equals(role)))return new ApiResponse(true,"Tài khoản đang có vai trò này.");
        protectAdministrator(actorId,target);
        if(hasDomainData(id))throw new IllegalArgumentException("Tài khoản đã có dữ liệu lớp/học tập/gia đình. Giữ vai trò hiện tại và tạo tài khoản khác để bảo toàn lịch sử.");
        target.setRoles(roles(role,permissionsFor(role,null)));target.setSessionVersion(target.getSessionVersion()+1);userRepository.save(target);
        return new ApiResponse(true,"Đã đổi vai trò. Người dùng cần đăng nhập lại.");
    }
    public static void validatePassword(String password) {
        if(password==null||password.length()<6||password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new IllegalArgumentException("Mật khẩu cần ít nhất 6 ký tự và tối đa 72 byte UTF-8.");
    }
    @Transactional
    public ApiResponse updateUserInfo(Long id,Map<String,String> payload) {
        if(payload==null)throw new IllegalArgumentException("Nhập thông tin tài khoản cần cập nhật.");
        User target=user(id);String name=payload.get("fullName"),email=payload.get("email"),phone=payload.get("phoneNumber"),password=payload.get("newPassword");
        if(name!=null&&(name.isBlank()||name.trim().length()>100))throw new IllegalArgumentException("Họ tên cần từ 1 đến 100 ký tự.");
        if(email!=null) {
            email=email.trim();if(email.length()>100||!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))throw new IllegalArgumentException("Email chưa hợp lệ.");
            var existing=userRepository.findByEmail(email);if(existing.isPresent()&&!existing.get().getId().equals(id))throw new IllegalArgumentException("Email đã được tài khoản khác sử dụng.");
        }
        if(phone!=null&&phone.trim().length()>20)throw new IllegalArgumentException("Số điện thoại tối đa 20 ký tự.");
        if(password!=null&&!password.isEmpty()){validatePassword(password);target.setPassword(passwordEncoder.encode(password));target.setSessionVersion(target.getSessionVersion()+1);}
        if(name!=null)target.setFullName(name.trim());if(email!=null)target.setEmail(email);if(phone!=null)target.setPhoneNumber(phone.trim());
        userRepository.save(target);return new ApiResponse(true,"Đã cập nhật tài khoản.");
    }
    @Transactional
    public ApiResponse resetPassword(Long id,String password) {
        validatePassword(password);User target=user(id);target.setPassword(passwordEncoder.encode(password));target.setSessionVersion(target.getSessionVersion()+1);
        userRepository.save(target);return new ApiResponse(true,"Đã đặt mật khẩu mới. Các phiên đăng nhập cũ hết hiệu lực.");
    }
    @Transactional
    public ApiResponse updateUserPermissions(Long id,List<String> permissions) {
        User target=user(id);if(!target.getRoles().stream().anyMatch(r->r.getName().equals("ROLE_TEACHER"))||admin(target))
            throw new IllegalArgumentException("Chỉ điều chỉnh các quyền chi tiết của tài khoản giáo viên.");
        if(permissions==null)throw new IllegalArgumentException("Chọn danh sách quyền giáo viên.");
        target.setRoles(roles("ROLE_TEACHER",permissionsFor("ROLE_TEACHER",permissions)));target.setSessionVersion(target.getSessionVersion()+1);
        userRepository.save(target);return new ApiResponse(true,"Đã lưu quyền. Giáo viên cần đăng nhập lại để cập nhật menu.");
    }
    private boolean hasDomainData(Long id) {
        return jdbc.queryForObject("SELECT (EXISTS(SELECT 1 FROM classes WHERE teacher_id=?) OR EXISTS(SELECT 1 FROM class_members WHERE student_id=?) OR EXISTS(SELECT 1 FROM submissions WHERE student_id=?) OR EXISTS(SELECT 1 FROM writing_practices WHERE student_id=?) OR EXISTS(SELECT 1 FROM learning_events WHERE student_id=?) OR EXISTS(SELECT 1 FROM learning_progress WHERE student_id=?) OR EXISTS(SELECT 1 FROM parent_student_links WHERE parent_id=? OR student_id=?) OR EXISTS(SELECT 1 FROM messages WHERE sender_id=? OR receiver_id=?) OR EXISTS(SELECT 1 FROM feedback_reports WHERE user_id=?) OR EXISTS(SELECT 1 FROM parent_link_invites WHERE student_id=?))",Boolean.class,id,id,id,id,id,id,id,id,id,id,id,id);
    }
    @Transactional
    public ApiResponse deleteUser(Long actorId,Long id) {
        lockAdministrators();User target=user(id);protectAdministrator(actorId,target);
        if(hasDomainData(id))throw new IllegalArgumentException("Tài khoản đã có dữ liệu liên quan. Hãy khóa tài khoản thay vì xóa để giữ lịch sử.");
        userRepository.delete(target);return new ApiResponse(true,"Đã xóa tài khoản chưa có dữ liệu học tập liên quan.");
    }
    @Transactional
    public ApiResponse createUser(AdminCreateUserRequest request) {
        String role=roleName(request.getRole());List<String> permissions=permissionsFor(role,request.getPermissions());validatePassword(request.getPassword());
        String username=request.getUsername().trim(),email=request.getEmail().trim();
        if(!username.matches("[a-zA-Z0-9_.-]{3,20}"))throw new IllegalArgumentException("Tên đăng nhập dùng 3–20 chữ, số, dấu chấm, gạch dưới hoặc gạch ngang.");
        if(userRepository.existsByUsername(username))throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        if(userRepository.existsByEmail(email))throw new IllegalArgumentException("Email đã được sử dụng.");
        User target=User.builder().username(username).email(email).fullName(request.getFullName().trim())
            .enabled(true).password(passwordEncoder.encode(request.getPassword())).roles(roles(role,permissions)).build();
        userRepository.save(target);return new ApiResponse(true,"Đã tạo tài khoản. Người dùng có thể đăng nhập ngay.");
    }
    @Transactional
    public AlphabetLesson saveAlphabetLesson(AlphabetLesson lesson) {
        if(lesson==null)throw new IllegalArgumentException("Chọn học liệu cần sửa.");
        if(lesson.getId()==null)throw new IllegalArgumentException("Bảng chữ đã cố định 29 chữ; chọn chữ hiện có để sửa.");
        AlphabetLesson saved=alphabetLessonRepository.findById(lesson.getId()).orElseThrow(()->new IllegalArgumentException("Không tìm thấy chữ."));
        if(!Objects.equals(saved.getLetter(),lesson.getLetter())||!LearningTrackingService.LETTERS.contains(lesson.getLetter().toUpperCase(Locale.ROOT)))
            throw new IllegalArgumentException("Không đổi ký tự của bảng 29 chữ.");
        if(lesson.getExampleWord()==null||lesson.getExampleWord().isBlank()||lesson.getExampleWord().length()>100||lesson.getPronunciationGuide()!=null&&lesson.getPronunciationGuide().length()>250)
            throw new IllegalArgumentException("Kiểm tra từ ví dụ và hướng dẫn đọc.");
        for(String url:List.of(Objects.toString(lesson.getAudioUrl(),""),Objects.toString(lesson.getImageUrl(),""),Objects.toString(lesson.getExampleAudioUrl(),"")))
            if(url.length()>255||(!url.isBlank()&&!(url.startsWith("/")||url.matches("https?://.*")||url.matches("[\\p{L}\\p{N}_.-]+"))))throw new IllegalArgumentException("Đường dẫn học liệu chưa hợp lệ.");
        saved.setExampleWord(lesson.getExampleWord().trim());saved.setPronunciationGuide(lesson.getPronunciationGuide());saved.setImageUrl(lesson.getImageUrl());
        saved.setAudioUrl(lesson.getAudioUrl());saved.setExampleAudioUrl(lesson.getExampleAudioUrl());saved.setIsHidden(Boolean.TRUE.equals(lesson.getIsHidden()));
        return alphabetLessonRepository.save(saved);
    }
    public void deleteAlphabetLesson(Long id){throw new IllegalArgumentException("Không xóa chữ khỏi hành trình 29 chữ. Có thể tạm ẩn khi cập nhật học liệu.");}
    public ApiResponse uploadAlphabetImage(org.springframework.web.multipart.MultipartFile file,String name){return mediaStorage.store(file,"alphabet",name);}
    public ApiResponse uploadAlphabetAudio(org.springframework.web.multipart.MultipartFile file,String name){return mediaStorage.store(file,"audio",name);}
    public ApiResponse uploadAlphabetExampleImage(org.springframework.web.multipart.MultipartFile file,String name){return mediaStorage.store(file,"example_images",name);}
    public Map<String,Object> getSystemStats(){return operations.overview();}
}
