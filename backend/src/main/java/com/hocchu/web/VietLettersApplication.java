package com.hocchu.web;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class VietLettersApplication {

    public static void main(String[] args) {
        SpringApplication.run(VietLettersApplication.class, args);
    }

    @Bean
    public CommandLineRunner initDatabase(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        return args -> {
            System.out.println("\n=== [SEED ACCOUNTS INITIALIZER] ===");
            try {
                // Đảm bảo đủ 4 vai trò & Các quyền hành động chi tiết
                jdbcTemplate.update("INSERT IGNORE INTO roles (id, name) VALUES (1, 'ROLE_STUDENT')");
                jdbcTemplate.update("INSERT IGNORE INTO roles (id, name) VALUES (2, 'ROLE_PARENT')");
                jdbcTemplate.update("INSERT IGNORE INTO roles (id, name) VALUES (3, 'ROLE_TEACHER')");
                jdbcTemplate.update("INSERT IGNORE INTO roles (id, name) VALUES (4, 'ROLE_ADMIN')");
                jdbcTemplate.update("INSERT IGNORE INTO roles (id, name) VALUES (5, 'PERM_ASSIGN_HOMEWORK')");
                jdbcTemplate.update("INSERT IGNORE INTO roles (id, name) VALUES (6, 'PERM_VIEW_ALPHABET')");
                jdbcTemplate.update("INSERT IGNORE INTO roles (id, name) VALUES (7, 'PERM_MANAGE_CLASSES')");
                jdbcTemplate.update("INSERT IGNORE INTO roles (id, name) VALUES (8, 'PERM_CHAT_ACCESS')");
                jdbcTemplate.update("INSERT IGNORE INTO roles (id, name) VALUES (9, 'PERM_PARENT_TRACKING')");
                jdbcTemplate.update("INSERT IGNORE INTO roles (id, name) VALUES (10, 'PERM_EDIT_ALPHABET')");
                jdbcTemplate.update("INSERT IGNORE INTO roles (id, name) VALUES (11, 'PERM_ADD_ALPHABET')");

                // Băm mật khẩu '123456' bằng BCrypt live
                String pw = passwordEncoder.encode("123456");

                // ─── 1. ADMIN ───────────────────────────────────────────────
                int adminExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE username = 'admin'", Integer.class);
                if (adminExists == 0) {
                    jdbcTemplate.update(
                        "INSERT INTO users (username, password, phone_number, email, full_name, enabled) VALUES (?, ?, ?, ?, ?, TRUE)",
                        "admin", pw, "0100000001", "admin@hocchu.com", "Quản trị viên Hệ thống");
                    Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username='admin'", Long.class);
                    jdbcTemplate.update("INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (?, 4)", adminId);
                    System.out.println(">> [TẠO MỚI] admin");
                } else {
                    jdbcTemplate.update("UPDATE users SET password=?, enabled=TRUE WHERE username='admin'", pw);
                    System.out.println(">> [CẬP NHẬT] admin - mật khẩu đồng bộ '123456'");
                }

                // ─── 2. GIÁO VIÊN ───────────────────────────────────────────
                int teacherExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE username = 'teacher1'", Integer.class);
                if (teacherExists == 0) {
                    jdbcTemplate.update(
                        "INSERT INTO users (username, password, phone_number, email, full_name, enabled) VALUES (?, ?, ?, ?, ?, TRUE)",
                        "teacher1", pw, "0100000002", "mai.nguyen@hocchu.com", "Cô Nguyễn Thị Mai");
                }
                Long teacherId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username='teacher1'", Long.class);
                jdbcTemplate.update("INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (?, 3)", teacherId); // ROLE_TEACHER
                jdbcTemplate.update("INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (?, 5)", teacherId); // PERM_ASSIGN_HOMEWORK
                jdbcTemplate.update("INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (?, 7)", teacherId); // PERM_MANAGE_CLASSES
                jdbcTemplate.update("INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (?, 8)", teacherId); // PERM_CHAT_ACCESS
                jdbcTemplate.update("UPDATE users SET password=?, enabled=TRUE WHERE username='teacher1'", pw);
                System.out.println(">> [ĐỒNG BỘ QUYỀN] teacher1");

                // ─── 3. HỌC SINH ────────────────────────────────────────────
                int studentExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE username = 'student1'", Integer.class);
                if (studentExists == 0) {
                    jdbcTemplate.update(
                        "INSERT INTO users (username, password, phone_number, email, full_name, enabled) VALUES (?, ?, ?, ?, ?, TRUE)",
                        "student1", pw, "0100000003", "an.nguyen@hocchu.com", "Nguyễn Văn An");
                }
                Long studentId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username='student1'", Long.class);
                jdbcTemplate.update("INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (?, 1)", studentId); // ROLE_STUDENT
                jdbcTemplate.update("UPDATE users SET password=?, enabled=TRUE WHERE username='student1'", pw);
                System.out.println(">> [ĐỒNG BỘ QUYỀN] student1");

                // ─── 4. PHỤ HUYNH ───────────────────────────────────────────
                int parentExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE username = 'parent1'", Integer.class);
                if (parentExists == 0) {
                    jdbcTemplate.update(
                        "INSERT INTO users (username, password, phone_number, email, full_name, enabled) VALUES (?, ?, ?, ?, ?, TRUE)",
                        "parent1", pw, "0100000004", "binh.nguyen@hocchu.com", "Nguyễn Văn Bình");
                }
                Long parentId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username='parent1'", Long.class);
                jdbcTemplate.update("INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (?, 2)", parentId); // ROLE_PARENT
                jdbcTemplate.update("INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (?, 8)", parentId); // PERM_CHAT_ACCESS
                jdbcTemplate.update("INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (?, 9)", parentId); // PERM_PARENT_TRACKING
                jdbcTemplate.update("UPDATE users SET password=?, enabled=TRUE WHERE username='parent1'", pw);
                System.out.println(">> [ĐỒNG BỘ QUYỀN] parent1");

                // ─── 5. TỰ ĐỘNG LIÊN KẾT PARENT1 VS STUDENT1 ─────────────────────
                try {
                    Long pId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username='parent1'", Long.class);
                    Long sId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username='student1'", Long.class);
                    if (pId != null && sId != null) {
                        int linkExists = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM parent_student_links WHERE student_id = ?", Integer.class, sId);
                        if (linkExists == 0) {
                            jdbcTemplate.update(
                                "INSERT INTO parent_student_links (parent_id, student_id, link_code, status, linked_at) VALUES (?, ?, 'LINK-STD1', 'APPROVED', NOW())",
                                pId, sId);
                            System.out.println(">> [LIÊN KẾT TỰ ĐỘNG] parent1 -> student1 thành công");
                        } else {
                            jdbcTemplate.update(
                                "UPDATE parent_student_links SET parent_id = ?, status = 'APPROVED', linked_at = NOW() WHERE student_id = ?",
                                pId, sId);
                            System.out.println(">> [CẬP NHẬT LIÊN KẾT] parent1 -> student1 thành công");
                        }
                    }
                } catch (Exception ex) {
                    System.err.println(">> Lỗi tự động liên kết: " + ex.getMessage());
                }

                System.out.println("\n╔══════════════════════════════════════════╗");
                System.out.println("║        TÀI KHOẢN MẪU ĐÃ SẴN SÀNG        ║");
                System.out.println("╠══════════════════════════════════════════╣");
                System.out.println("║  Vai trò      Username      Mật khẩu    ║");
                System.out.println("║  ─────────    ─────────    ─────────    ║");
                System.out.println("║  Admin        admin        123456       ║");
                System.out.println("║  Giáo viên    teacher1     123456       ║");
                System.out.println("║  Học sinh     student1     123456       ║");
                System.out.println("║  Phụ huynh    parent1      123456       ║");
                System.out.println("╚══════════════════════════════════════════╝");

            } catch (Exception e) {
                System.err.println(">> Lỗi seed accounts: " + e.getMessage());
            }
            System.out.println("===========================================\n");
        };
    }
}
