# Hệ thống hỗ trợ học sinh lớp 1 - Luyện viết chữ và học chữ cái tiếng Việt

Hệ thống cung cấp một nền tảng học tập trực tuyến toàn diện cho học sinh lớp 1, giúp các em học bảng chữ cái tiếng Việt và tập viết chữ trên bảng ô ly. Hệ thống hỗ trợ sự tương tác chặt chẽ giữa 4 vai trò: **Học sinh**, **Phụ huynh**, **Giáo viên**, và **Quản trị viên (Admin)**.

---

## 📱 Ứng dụng Android

Thư mục [`android/`](android/) chứa ứng dụng học sinh và phụ huynh viết bằng Kotlin và Jetpack Compose, dùng chung phong cách thiết kế Stitch. Mở riêng thư mục này bằng Android Studio. Xem [hướng dẫn cấu hình và chạy Android](android/README.md).

Android đăng nhập theo vai trò học sinh/phụ huynh, lưu phiên mã hóa và tách dữ liệu theo tài khoản. Học liệu, lớp học, bài cô giao dùng API thật. Tiến độ hành trình 29 chữ được khôi phục/gộp từ server; hoạt động có hàng đợi để gửi khi có kết nối. Phụ huynh xem theo ngày/tuần và mở bài đã nộp, điểm/nhận xét của cô. Bản nháp/nét tự luyện vẫn ở thiết bị. Học liệu do dự án biên soạn cần được duyệt sư phạm trước khi dùng chính thức. Xem [luồng phụ huynh](android/docs/PARENT.md), [lớp học Android](android/docs/CLASSROOM.md), [kết nối backend](android/docs/AUTHENTICATION.md) và [quy tắc phát triển](android/AGENTS.md).

Website chỉ dành cho **giáo viên và admin**. Học sinh và phụ huynh dùng Android;
các trang web tương ứng đã được bỏ. Biểu mẫu đăng ký/OTP chung trên web vẫn
cho phép tạo tài khoản trước khi dùng app. Backend và dữ liệu dùng chung được giữ.

Admin dùng giao diện React chung phong cách với giáo viên: tổng quan, tài khoản
và phân quyền, học liệu 29 chữ, tra cứu lớp/liên kết gia đình và trạng thái cấu hình.
Trang đăng nhập web hỗ trợ hiện/ẩn mật khẩu và tự vào đúng vai trò.
Xem [hướng dẫn website và quản trị](frontend/README.md).

---

## 🛠️ Công nghệ sử dụng (Tech Stack)

*   **Backend:** Java 17, Spring Boot 3 (Web, Security, Data JPA, Validation)
*   **Database:** MySQL (Flyway migration quản lý schema tự động)
*   **Xác thực:** JWT (JSON Web Token) + Xác thực OTP
*   **Frontend:** React, JavaScript, Vite, Tailwind CSS, Axios, React Router; biểu đồ React/CSS kèm bảng số liệu
*   **Build Tool:** Maven (Backend), npm (Frontend)

---

## 📋 Tài khoản kiểm thử mặc định (Demo Credentials)

Migration khởi tạo có các tên tài khoản mẫu dưới đây. Sử dụng mật khẩu hiện có
trong cơ sở dữ liệu; backend không đặt lại mật khẩu, bật lại tài khoản hoặc tự
liên kết `parent1` với `student1` khi khởi động. Ưu tiên tài khoản do admin cấp;
đăng ký/OTP native Android được hoãn trong giai đoạn này.

| Vai trò | Tên đăng nhập | Email liên kết | Mô tả chức năng |
| :--- | :--- | :--- | :--- |
| **Quản trị viên** | `admin` | `admin@hocchu.com` | Quản lý tài khoản/quyền, học liệu, tra cứu lớp/gia đình và trạng thái hệ thống |
| **Giáo viên** | `teacher1` | `mai.nguyen@hocchu.com` | Tạo lớp học, quản lý học sinh, giao bài tập, chấm điểm canvas |
| **Học sinh** | `student1` | `an.nguyen@hocchu.com` | Học bảng chữ cái phát âm, luyện viết bảng ô ly, làm bài tập |
| **Phụ huynh** | `parent1` | `binh.nguyen@hocchu.com` | Liên kết con, xem hoạt động hằng ngày và theo dõi tiến độ |

---

## ⚙️ Hướng dẫn cài đặt và khởi chạy dự án

### Môi trường yêu cầu
*   Java Development Kit (JDK) 17 hoặc cao hơn.
*   Node.js 22.12 hoặc cao hơn & npm.
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
    *   `APP_JWT_SECRET` (bắt buộc; dùng chuỗi bí mật ngẫu nhiên dài ít nhất 64 byte UTF-8)

   Ví dụ trên PowerShell:
   ```powershell
   $env:DB_USERNAME = "root"
   $env:DB_PASSWORD = "mat-khau-mysql-cua-ban"
   $env:APP_JWT_SECRET = "thay-bang-khoa-ngau-nhien-it-nhat-64-byte"
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

Frontend gọi `/api` trên cùng origin; Vite chuyển tiếp đến backend local cổng
8080. Khi backend chạy chỗ khác, đặt `VITE_BACKEND_TARGET` trước khi chạy Vite.
Bản triển khai cần reverse proxy `/api` đến backend hoặc cấu hình
`VITE_API_BASE_URL` khi build.

Tệp ảnh/âm thanh mới lưu ở `backend/uploads` (không thuộc Git), phục vụ qua
`/api/public/media/`. Có thể cấu hình `APP_MEDIA_DIRECTORY` thành thư mục riêng
và sao lưu cùng dữ liệu ứng dụng. Backend đọc thêm tài sản cũ trong
`frontend/public`, không cần Vite để phục vụ ảnh/âm thanh cho Android.

---

## 💡 Các tính năng nổi bật để kiểm thử

### 1. Luồng đăng ký tài khoản mới & Xác thực OTP
1.  Truy cập trang đăng ký ở Frontend, điền thông tin và chọn vai trò của bạn (Học sinh/Giáo viên/Phụ huynh).
2.  Sau khi nhấn nút Đăng ký, hệ thống hiển thị trang nhập OTP.
3.  Mở **Console Log** của terminal chạy **Backend Spring Boot** lên. Bạn sẽ thấy dòng log hiển thị mã OTP 6 chữ số:
    `MÃ OTP KÍCH HOẠT TÀI KHOẢN CHO USER '...': XXXXXX`
4.  Nhập mã OTP trên website để kích hoạt. Giáo viên đăng nhập website; học sinh/phụ huynh mở app Android để đăng nhập. Admin cũng có thể tạo các tài khoản này trong Quản lý người dùng.

### 2. Module Luyện viết chữ của Học sinh
*   Học sinh mở app Android, vào mục **Tập viết**, chọn một chữ cái.
*   Bảng ô ly chuẩn tập viết sẽ hiện ra cùng chữ cái mẫu mờ nhạt nằm phía sau.
*   Dùng ngón tay hoặc bút trên điện thoại/tablet Android để luyện theo chữ mẫu.
*   Hoàn thành lượt viết để xem điểm bám mẫu, độ phủ và gợi ý sửa nét. Phép so hình học chưa đánh giá thứ tự nét hoặc thay thế nhận xét của cô.
*   Bài cô giao được backend chấm lại từ ảnh thật. Cô xem bài, sửa điểm/nhận xét; hệ thống giữ điểm tự động ban đầu.
*   Ở màn Đọc chữ, bé nghe mẫu rồi tự đọc theo; không thu âm hoặc nhận dạng giọng nói.

### 3. Liên kết Phụ huynh & Xem biểu đồ học tập của con
*   Đăng nhập tài khoản học sinh trong app, mở **Hồ sơ → Liên kết phụ huynh** để lấy mã dùng một lần có hiệu lực 30 phút.
*   Đăng nhập tài khoản phụ huynh (`parent1`) trên Android, mở **Gia đình** và nhập mã liên kết.
*   Chọn con và ngày ở **Hôm nay** để xem lượt đọc, viết, câu luyện tập, bài đã nộp và dòng hoạt động. **Tiến độ** hiển thị 29 chữ và hoạt động bảy ngày.

Chat đã được gỡ khỏi Android, website và backend. Hệ thống không mở endpoint WebSocket; các migration cũ và lịch sử dữ liệu được giữ nguyên.

### 4. Website giáo viên và trợ lý soạn bài

Website dùng React/JavaScript, màu và font thống nhất với app học sinh. Cô quản lý
lớp, học sinh, học liệu 29 chữ và giao đủ Tập viết chữ, Đố vui ghép chữ, Ghép câu.
Xem [hướng dẫn website](frontend/README.md) để chạy, kiểm thử và cấu hình trợ lý AI.
Trợ lý chỉ tạo bản nháp để cô duyệt; chưa có API key thì soạn thủ công/kho mẫu vẫn dùng được.
