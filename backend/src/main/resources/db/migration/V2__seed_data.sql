-- V2__seed_data.sql
-- Thêm dữ liệu mẫu vào cơ sở dữ liệu

-- 1. Chèn vai trò (Roles)
INSERT INTO roles (id, name) VALUES (1, 'ROLE_STUDENT');
INSERT INTO roles (id, name) VALUES (2, 'ROLE_PARENT');
INSERT INTO roles (id, name) VALUES (3, 'ROLE_TEACHER');
INSERT INTO roles (id, name) VALUES (4, 'ROLE_ADMIN');

-- 2. Chèn người dùng mẫu (Mật khẩu mặc định: '123456' được mã hóa bằng BCrypt)
-- Hash BCrypt: '$2a$10$R9h/QIPXY35.lHhS2tG0QO.0Q4P2i5U4Xy4e4Rj2S6a4mG4JcZ8qy'
-- Tài khoản Admin
INSERT INTO users (id, username, password, phone_number, email, full_name, enabled) 
VALUES (1, 'admin', '$2a$10$R9h/QIPXY35.lHhS2tG0QO.0Q4P2i5U4Xy4e4Rj2S6a4mG4JcZ8qy', '0123456789', 'admin@hocchu.com', 'Hệ thống Quản trị', TRUE);
INSERT INTO user_roles (user_id, role_id) VALUES (1, 4);

-- Tài khoản Giáo viên
INSERT INTO users (id, username, password, phone_number, email, full_name, enabled) 
VALUES (2, 'teacher1', '$2a$10$R9h/QIPXY35.lHhS2tG0QO.0Q4P2i5U4Xy4e4Rj2S6a4mG4JcZ8qy', '0987654321', 'mai.nguyen@hocchu.com', 'Cô Nguyễn Thị Mai', TRUE);
INSERT INTO user_roles (user_id, role_id) VALUES (2, 3);

-- Tài khoản Học sinh
INSERT INTO users (id, username, password, phone_number, email, full_name, enabled) 
VALUES (3, 'student1', '$2a$10$R9h/QIPXY35.lHhS2tG0QO.0Q4P2i5U4Xy4e4Rj2S6a4mG4JcZ8qy', '0345678901', 'an.nguyen@hocchu.com', 'Nguyễn Văn An', TRUE);
INSERT INTO user_roles (user_id, role_id) VALUES (3, 1);

-- Tài khoản Phụ huynh
INSERT INTO users (id, username, password, phone_number, email, full_name, enabled) 
VALUES (4, 'parent1', '$2a$10$R9h/QIPXY35.lHhS2tG0QO.0Q4P2i5U4Xy4e4Rj2S6a4mG4JcZ8qy', '0367890123', 'binh.nguyen@hocchu.com', 'Nguyễn Văn Bình', TRUE);
INSERT INTO user_roles (user_id, role_id) VALUES (4, 2);


-- 3. Chèn bảng chữ cái tiếng Việt cơ bản (29 chữ cái)
INSERT INTO alphabet_lessons (letter, pronunciation_guide, audio_url, image_url, example_word) VALUES
('A', 'Chữ a phát âm như tiếng ca', 'a.mp3', 'apple.png', 'Ca'),
('Ă', 'Chữ ă phát âm như mắt bé', 'a_breve.mp3', 'eye.png', 'Mắt'),
('Â', 'Chữ â phát âm như cây nấm', 'a_circumflex.mp3', 'mushroom.png', 'Nấm'),
('B', 'Chữ b phát âm như quả bóng', 'b.mp3', 'ball.png', 'Bóng'),
('C', 'Chữ c phát âm như con cá', 'c.mp3', 'fish.png', 'Cá'),
('D', 'Chữ d phát âm như quả dưa', 'd.mp3', 'melon.png', 'Dưa'),
('Đ', 'Chữ đ phát âm như quả đu đủ', 'dd.mp3', 'papaya.png', 'Đu đủ'),
('E', 'Chữ e phát âm như chiếc xe', 'e.mp3', 'car.png', 'Xe'),
('Ê', 'Chữ ê phát âm như chú bê', 'e_circumflex.mp3', 'calf.png', 'Bê'),
('G', 'Chữ g phát âm như con gà', 'g.mp3', 'chicken.png', 'Gà'),
('H', 'Chữ h phát âm như bông hoa', 'h.mp3', 'flower.png', 'Hoa'),
('I', 'Chữ i phát âm như viên bi', 'i.mp3', 'marble.png', 'Bi'),
('K', 'Chữ k phát âm như cái kéo', 'k.mp3', 'scissors.png', 'Kéo'),
('L', 'Chữ l phát âm như cái lá', 'l.mp3', 'leaf.png', 'Lá'),
('M', 'Chữ m phát âm như quả mít', 'm.mp3', 'jackfruit.png', 'Mít'),
('N', 'Chữ n phát âm như quả na', 'n.mp3', 'custard_apple.png', 'Na'),
('O', 'Chữ o phát âm như quả trứng tròn', 'o.mp3', 'egg.png', 'Tròn'),
('Ô', 'Chữ ô phát âm như cái ô che đầu', 'o_circumflex.mp3', 'umbrella.png', 'Ô'),
('Ơ', 'Chữ ơ phát âm như lá cờ bay', 'o_horn.mp3', 'flag.png', 'Cờ'),
('P', 'Chữ p phát âm như đèn pin', 'p.mp3', 'flashlight.png', 'Pin'),
('Q', 'Chữ q phát âm như quả quýt', 'q.mp3', 'tangerine.png', 'Quýt'),
('R', 'Chữ r phát âm như con rùa', 'r.mp3', 'turtle.png', 'Rùa'),
('S', 'Chữ s phát âm như ngôi sao', 's.mp3', 'star.png', 'Sao'),
('T', 'Chữ t phát âm như con tàu', 't.mp3', 'train.png', 'Tàu'),
('U', 'Chữ u phát âm như chiếc mũ', 'u.mp3', 'hat.png', 'Mũ'),
('Ư', 'Chữ ư phát âm như con sư tử', 'u_horn.mp3', 'lion.png', 'Sư tử'),
('V', 'Chữ v phát âm như con vịt', 'v.mp3', 'duck.png', 'Vịt'),
('X', 'Chữ x phát âm như chiếc xe', 'x.mp3', 'bicycle.png', 'Xe'),
('Y', 'Chữ y phát âm như quả yếm', 'y.mp3', 'bib.png', 'Yếm');
