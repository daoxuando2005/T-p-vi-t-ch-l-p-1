package com.hocchu.web.config;

import com.hocchu.web.entity.*;
import com.hocchu.web.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Random;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ClazzRepository clazzRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ParentStudentLinkRepository parentStudentLinkRepository;
    private final AssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      RoleRepository roleRepository,
                      ClazzRepository clazzRepository,
                      ClassMemberRepository classMemberRepository,
                      ParentStudentLinkRepository parentStudentLinkRepository,
                      AssignmentRepository assignmentRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.clazzRepository = clazzRepository;
        this.classMemberRepository = classMemberRepository;
        this.parentStudentLinkRepository = parentStudentLinkRepository;
        this.assignmentRepository = assignmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userRepository.existsByUsername("mock3_teacher1")) {
            System.out.println("Mock data v3 already exists. Skipping DataSeeder.");
            return;
        }

        System.out.println("Starting to seed mock data v3 (3 Teachers, 6 Classes, 90 Students)...");

        Role teacherRole = getOrCreateRole("ROLE_TEACHER");
        Role studentRole = getOrCreateRole("ROLE_STUDENT");
        Role parentRole = getOrCreateRole("ROLE_PARENT");

        // 1. 3 Giáo viên
        User[] teachers = new User[3];
        for (int i = 0; i < 3; i++) {
            teachers[i] = createUser("mock3_teacher" + (i+1), "Giáo viên " + (i+1), teacherRole);
        }

        // 2. 6 Lớp học (mỗi giáo viên 2 lớp)
        Clazz[] classes = new Clazz[6];
        for (int i = 0; i < 6; i++) {
            User t = teachers[i / 2];
            classes[i] = createClass("Lớp " + (i+1) + " (GV" + (i/2 + 1) + ")", "Lớp học mock", t);
            
            // Giao bài tập cho mỗi lớp (1 Tập viết, 1 Trắc nghiệm)
            Assignment writeAsm = Assignment.builder()
                .title("Tập viết chữ A, B, C")
                .description("Luyện nét cơ bản")
                .contentType("WRITING")
                .contentData("{\"letters\": [\"A\", \"B\", \"C\"], \"targetScore\": 50}")
                .dueDate(LocalDateTime.now().plusDays(7))
                .clazz(classes[i])
                .assignedBy("TEACHER")
                .build();
            assignmentRepository.save(writeAsm);

            Assignment quizAsm = Assignment.builder()
                .title("Trắc nghiệm bảng chữ cái")
                .description("Chọn đáp án đúng")
                .contentType("QUIZ")
                .contentData("[{\"question\": \"\", \"animalName\": \"Con Gà\", \"animalEmoji\": \"🐓\", \"options\": [\"G\", \"B\", \"C\"], \"answer\": \"G\"}, {\"question\": \"\", \"animalName\": \"Con Mèo\", \"animalEmoji\": \"🐱\", \"options\": [\"M\", \"D\", \"N\"], \"answer\": \"M\"}]")
                .dueDate(LocalDateTime.now().plusDays(7))
                .clazz(classes[i])
                .assignedBy("TEACHER")
                .build();
            assignmentRepository.save(quizAsm);
        }

        // 3. 90 Học sinh (15 hs / lớp) và Phụ huynh
        Random rand = new Random();
        int parentCounter = 1;
        User currentParent = null;
        int childrenCountForCurrentParent = 0;
        int childrenAssignedToCurrentParent = 0;

        for (int i = 0; i < 90; i++) {
            User student = createUser("mock3_student" + (i+1), "Học sinh " + (i+1), studentRole);
            
            // Vào lớp
            int classIndex = i / 15;
            ClassMember classMember = ClassMember.builder()
                    .clazz(classes[classIndex])
                    .student(student)
                    .build();
            classMemberRepository.save(classMember);

            // Gán cho phụ huynh (mỗi phụ huynh 1-3 con)
            if (currentParent == null || childrenAssignedToCurrentParent >= childrenCountForCurrentParent) {
                currentParent = createUser("mock3_parent" + parentCounter, "Phụ huynh " + parentCounter, parentRole);
                parentCounter++;
                childrenCountForCurrentParent = rand.nextInt(3) + 1; // 1, 2, or 3
                childrenAssignedToCurrentParent = 0;
            }

            ParentStudentLink link = ParentStudentLink.builder()
                    .parent(currentParent)
                    .student(student)
                    .linkCode(UUID.randomUUID().toString().substring(0, 10))
                    .status("APPROVED")
                    .linkedAt(LocalDateTime.now())
                    .build();
            parentStudentLinkRepository.save(link);
            childrenAssignedToCurrentParent++;
        }

        System.out.println("Mock data v3 seeded successfully.");
    }

    private Role getOrCreateRole(String roleName) {
        return roleRepository.findByName(roleName).orElseGet(() -> {
            Role role = new Role();
            role.setName(roleName);
            return roleRepository.save(role);
        });
    }

    private User createUser(String username, String fullName, Role role) {
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode("123456"))
                .fullName(fullName)
                .email(username + "@example.com")
                .enabled(true)
                .roles(roles)
                .build();
        return userRepository.save(user);
    }

    private Clazz createClass(String name, String description, User teacher) {
        Clazz clazz = Clazz.builder()
                .name(name)
                .description(description)
                .teacher(teacher)
                .inviteCode(UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .build();
        return clazzRepository.save(clazz);
    }
}
