# Hệ thống hỗ trợ học sinh lớp 1 - Luyện viết chữ và học chữ cái tiếng Việt

Hệ thống cung cấp một nền tảng học tập trực tuyến toàn diện cho học sinh lớp 1, giúp các em học bảng chữ cái tiếng Việt và tập viết chữ trên bảng ô ly. Hệ thống hỗ trợ sự tương tác chặt chẽ giữa 4 vai trò: **Học sinh**, **Phụ huynh**, **Giáo viên**, và **Quản trị viên (Admin)**.

---

## 🛠️ Công nghệ sử dụng (Tech Stack)

*   **Backend:** Java 17, Spring Boot 3 (Web, Security, Data JPA, Validation, WebSocket/STOMP)
*   **Database:** MySQL (Flyway migration quản lý schema tự động)
*   **Xác thực:** JWT (JSON Web Token) + Xác thực OTP
*   **Thống kê/Báo cáo:** Apache POI (Excel) & OpenPDF (PDF)
*   **Frontend:** React, TypeScript, Vite, Tailwind CSS, Axios, React Router, Recharts, SockJS/STOMP client
*   **Build Tool:** Maven (Backend), npm (Frontend)

---

## 📋 Tài khoản kiểm thử mặc định (Demo Credentials)

Sau khi khởi chạy ứng dụng lần đầu, cơ sở dữ liệu sẽ được tự động seed các vai trò và tài khoản mẫu sau. **Tất cả các tài khoản đều có mật khẩu mặc định là: `123456`**.

| Vai trò | Tên đăng nhập | Email liên kết | Mô tả chức năng |
| :--- | :--- | :--- | :--- |
| **Quản trị viên** | `admin` | `admin@hocchu.com` | Quản lý người dùng, CRUD học liệu, tải báo cáo, duyệt phản hồi |
| **Giáo viên** | `teacher1` | `mai.nguyen@hocchu.com` | Tạo lớp học, quản lý học sinh, giao bài tập, chấm điểm canvas |
| **Học sinh** | `student1` | `an.nguyen@hocchu.com` | Học bảng chữ cái phát âm, chơi game, luyện viết bảng ô ly |
| **Phụ huynh** | `parent1` | `binh.nguyen@hocchu.com` | Liên kết con, theo dõi tiến độ bằng biểu đồ, chat với cô giáo |

---

## ⚙️ Hướng dẫn cài đặt và khởi chạy dự án

### Môi trường yêu cầu
*   Java Development Kit (JDK) 17 hoặc cao hơn.
*   Node.js (phiên bản 18 hoặc cao hơn) & npm.
*   MySQL Server (cổng mặc định 3306).

---

### Bước 1: Cấu hình Cơ sở dữ liệu MySQL

1.  Mở trình quản lý MySQL (như phpMyAdmin, DBeaver, MySQL Workbench) hoặc CLI.
2.  Tạo một cơ sở dữ liệu mới rỗng tên là `viet_letters_db`:
    ```sql
    CREATE DATABASE viet_letters_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    ```
3.  Cấu hình thông tin kết nối bằng biến môi trường trước khi chạy backend:
    *   `DB_USERNAME` (mặc định là `root`)
    *   `DB_PASSWORD` (mặc định để trống)
    *   `APP_JWT_SECRET` (bắt buộc; dùng chuỗi bí mật ngẫu nhiên dài ít nhất 32 ký tự)

   Ví dụ trên PowerShell:
   ```powershell
   $env:DB_USERNAME = "root"
   $env:DB_PASSWORD = "mat-khau-mysql-cua-ban"
   $env:APP_JWT_SECRET = "tao-mot-chuoi-bi-mat-ngau-nhien-dai-it-nhat-32-ky-tu"
   ```

---

### Bước 2: Khởi chạy Spring Boot Backend

1.  Mở terminal tại thư mục `backend/`.
2.  Chạy lệnh compile và khởi chạy server:
    ```bash
    mvn spring-boot:run
    ```
3.  Khi server khởi chạy thành công:
    *   Flyway sẽ tự động chạy các file migration để tạo bảng và nạp dữ liệu mẫu vào DB.
    *   Ứng dụng lắng nghe ở cổng: `http://localhost:8080`.
    *   Cổng WebSocket STOMP kết nối qua: `ws://localhost:8080/ws`.

---

### Bước 3: Khởi chạy React Frontend

1.  Mở một cửa sổ terminal mới tại thư mục `frontend/`.
2.  Cài đặt các gói thư viện (nếu chưa cài):
    ```bash
    npm install
    ```
3.  Chạy server phát triển (Development Server):
    ```bash
    npm run dev
    ```
4.  Mở trình duyệt truy cập: `http://localhost:5173`.

---

## 💡 Các tính năng nổi bật để kiểm thử

### 1. Luồng đăng ký tài khoản mới & Xác thực OTP
1.  Truy cập trang đăng ký ở Frontend, điền thông tin và chọn vai trò của bạn (Học sinh/Giáo viên/Phụ huynh).
2.  Sau khi nhấn nút Đăng ký, hệ thống hiển thị trang nhập OTP.
3.  Mở **Console Log** của terminal chạy **Backend Spring Boot** lên. Bạn sẽ thấy dòng log hiển thị mã OTP 6 chữ số:
    `MÃ OTP KÍCH HOẠT TÀI KHOẢN CHO USER '...': XXXXXX`
4.  Nhập mã OTP đó trên màn hình frontend để kích hoạt tài khoản thành công và đăng nhập.

### 2. Module Luyện viết chữ của Học sinh
*   Học sinh vào mục **Luyện viết chữ**, chọn một chữ cái.
*   Bảng ô ly chuẩn tập viết sẽ hiện ra cùng chữ cái mẫu mờ nhạt nằm phía sau.
*   Dùng chuột hoặc vẽ bằng tay (trên iPad/điện thoại) để viết đè lên chữ mẫu.
*   Bấm **Nộp Bài** để hệ thống tự động chấm điểm % độ chính xác nét vẽ của bé.

### 3. Trò chơi nhận diện chữ cái tiếng Việt (Game ghép chữ)
*   Học sinh vào mục **Game ghép chữ**.
*   Màn hình hiển thị emoji (ví dụ: quả táo 🍎, con cá 🐟) cùng 3 đáp án.
*   Khi bé chọn đáp án: hệ thống tổng hợp âm thanh (Beep đúng/sai bằng Web Audio API) kèm hiệu ứng bông hoa pháo hoa ăn mừng khi bé trả lời chính xác, cộng 10 điểm thưởng và tự chuyển câu hỏi.

### 4. Liên kết Phụ huynh & Xem biểu đồ học tập của con
*   Đăng nhập tài khoản Học sinh (`student1`), vào mục **Lớp học & Bài tập** để lấy mã liên kết của con (Ví dụ: `F8D92A`).
*   Đăng nhập tài khoản Phụ huynh (`parent1`), nhấn **Liên kết con mới**, nhập mã liên kết này để hoàn tất kết nối.
*   Sau khi kết nối thành công, biểu đồ đường thể hiện lịch sử điểm số luyện viết chữ của bé và biểu đồ cột điểm số bài tập sẽ hiển thị trực quan thông qua thư viện Recharts.

### 5. Chat trực tuyến thời gian thực Phụ huynh - Giáo viên
*   Mở hai trình duyệt khác nhau (hoặc một trình duyệt thường và một ẩn danh).
*   Đăng nhập một bên là Giáo viên (`teacher1`), bên kia là Phụ huynh (`parent1` - tài khoản phụ huynh đã liên kết thành công với bé nằm trong lớp học của giáo viên đó).
*   Vào mục **Trao đổi tin nhắn**, bạn sẽ thấy tên đối phương trong danh sách chat.
*   Tiến hành gửi tin nhắn qua lại. Tin nhắn sẽ xuất hiện thời gian thực (Real-time) trên màn hình đối phương thông qua kết nối WebSocket/STOMP.
