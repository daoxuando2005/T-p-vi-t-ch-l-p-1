# Vui Học — Backend

Backend Java/Spring Boot cung cấp API cho Android học sinh/phụ huynh và website
giáo viên/admin. Dữ liệu được lưu trong MySQL; Flyway quản lý phiên bản schema.
Ảnh/âm thanh tải lên được lưu riêng trên filesystem.

Xem [tổng quan dự án](../README.md), [Android](../android/README.md) và
[website](../frontend/README.md).

## Môi trường và cấu hình

- JDK 17 trở lên; có thể dùng JDK 21 chung với Android.
- Maven và MySQL 8 đang chạy.
- Mặc định backend dùng cổng `8080`, database `viet_letters_db` trên `localhost:3306`.

Tạo database bằng tài khoản MySQL có quyền phù hợp:

```sql
CREATE DATABASE IF NOT EXISTS viet_letters_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Từ thư mục `backend`, tạo cấu hình local nếu chưa có:

```powershell
if (!(Test-Path .env.local)) { Copy-Item .env.example .env.local }
```

`.env.local` được Spring nạp dưới dạng **Java properties**, không phải shell script:
không thêm `export` và không bao giá trị bằng dấu nháy. Điền cấu hình riêng theo
[.env.example](.env.example):

| Biến | Ý nghĩa |
| --- | --- |
| `DB_USERNAME` | Người dùng MySQL; mặc định `root` |
| `DB_PASSWORD` | Mật khẩu MySQL trên máy của bạn |
| `APP_JWT_SECRET` | Khóa JWT ngẫu nhiên, bắt buộc ít nhất 64 byte UTF-8 |
| `APP_MEDIA_DIRECTORY` | Thư mục media; mặc định `./uploads`, tính từ thư mục chạy backend |
| `AI_PROVIDER` | Nhà cung cấp trợ lý; mặc định `gemini` |
| `GEMINI_API_KEY` | Khóa Gemini riêng trên backend; để trống nếu chưa dùng trợ lý |
| `AI_BASE_URL` | URL gốc dịch vụ AI |
| `AI_MODEL` | Tên model AI |

Có thể sinh khóa JWT bằng PowerShell rồi điền chuỗi kết quả vào `.env.local`:

```powershell
$jwtBytes = New-Object byte[] 64
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRandom.GetBytes($jwtBytes)
[Convert]::ToBase64String($jwtBytes)
$jwtRandom.Dispose()
```

Không commit `.env.local`, mật khẩu, khóa JWT, API key hoặc log. Cấu hình URL
MySQL và các thuộc tính Spring khác nằm trong
[src/main/resources/application.yml](src/main/resources/application.yml).

## Chạy bằng Maven hoặc IntelliJ IDEA

Chạy **từ thư mục `backend`** để nạp đúng `.env.local`:

```powershell
mvn spring-boot:run
```

Hoặc đóng gói rồi chạy; dừng phiên server trước bằng `Ctrl+C` nếu đang dùng cách trên:

```powershell
mvn clean package
java -jar target/viet-letters-backend-0.0.1-SNAPSHOT.jar
```

Trong IntelliJ, mở `pom.xml`, chọn JDK 17/21, chạy `VietLettersApplication`
với working directory là thư mục `backend`. Chỉ chạy một tiến trình dự án trên
cổng 8080. Thay đổi `.env.local` cần khởi động lại backend.

Flyway tự áp dụng migration trong [db/migration](src/main/resources/db/migration).
Hibernate dùng `ddl-auto=validate`; không dùng nó để tự tái tạo schema. Với DB
đã có, sao lưu trước khi nâng cấp và không sửa/xóa migration đã áp dụng.

## Tài khoản và quản trị ban đầu

- `POST /api/auth/signup` nhận `STUDENT`, `PARENT`, `TEACHER`; tài khoản demo
  được bật ngay, không OTP. Không có đăng ký `ADMIN` công khai.
- Giáo viên mới được cấp quyền mặc định để quản lý lớp, giao bài, xem/sửa học liệu.
  Admin có thể điều chỉnh quyền sau đó.
- Migration khởi tạo có tên mẫu `admin`, `teacher1`, `student1`, `parent1`.
  Không giả định mật khẩu của DB đang dùng vẫn là mật khẩu mẫu. Khởi động backend
  không đặt lại mật khẩu, bật lại tài khoản hoặc tự liên kết phụ huynh với học sinh.
- Mật khẩu cần ít nhất 6 ký tự và tối đa 72 byte UTF-8; backend lưu BCrypt.
  Đổi mật khẩu/khóa/đổi quyền làm JWT cũ hết hiệu lực qua `session_version`.

Nếu khởi tạo **DB local do bạn quản lý** mà chưa có mật khẩu admin sử dụng được,
người vận hành có thể tạo BCrypt hash mới. Từ thư mục backend:

```powershell
mvn dependency:build-classpath "-Dmdep.outputFile=target/runtime-classpath.txt"
$runtimeClasspath = (Get-Content target/runtime-classpath.txt -Raw).Trim()
jshell --class-path $runtimeClasspath
```

Trong JShell, thay chuỗi ví dụ bằng mật khẩu tự chọn, lấy hash rồi thoát `/exit`:

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
new BCryptPasswordEncoder().encode("THAY_BANG_MAT_KHAU_CUA_BAN");
```

Dùng MySQL Workbench/CLI để cập nhật **đúng tài khoản quản trị**; thay placeholder
bằng hash vừa tạo. Không ghi mật khẩu thô vào SQL hoặc sửa migration cũ:

```sql
UPDATE users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
SET u.password = '<BCRYPT_HASH_CUA_BAN>',
    u.session_version = u.session_version + 1
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN';
```

Bước này không tự mở khóa tài khoản. Sau khi đăng nhập admin, dùng **Tài khoản**
trên web để tạo và quản lý người dùng tiếp theo. Không xóa trực tiếp tài khoản
có lịch sử học tập/lớp học để tránh mất dữ liệu.

## API theo nghiệp vụ

Phần lớn API yêu cầu `Authorization: Bearer <JWT>`. Backend kiểm tra vai trò,
quyền và chủ sở hữu dữ liệu; client không được chọn ID học sinh để thay người nộp.

| Nhóm | API tiêu biểu |
| --- | --- |
| Xác thực | `POST /api/auth/signup`, `POST /api/auth/signin`, `GET /api/auth/me` |
| Học liệu | `GET /api/student/alphabet`, `GET /api/student/content` |
| Lớp học sinh | `GET /api/student/class/joined`, `POST /api/student/class/join?inviteCode=...` |
| Bài học sinh | `GET /api/student/assignments`, `POST /api/student/assignments/{id}/submit`, `GET /api/student/assignments/{id}/submission` |
| Hoạt động/tiến độ | `POST /api/student/learning/sync`, `GET /api/student/learning/progress` |
| Phụ huynh | `GET /api/student/link-code`, `POST /api/parent/link?linkCode=...`, `GET /api/parent/students` |
| Báo cáo phụ huynh | `GET /api/parent/students/{id}/dashboard?date=YYYY-MM-DD`, `GET /api/parent/students/{id}/submissions/{submissionId}` |
| Giáo viên | `/api/teacher/class`, `/api/teacher/assignments`, `/api/teacher/submissions/{id}/grade` |
| Trợ lý | `GET /api/teacher/assistant/status`, `POST /api/teacher/assistant/draft` |
| Admin | `/api/admin/users`, `/api/admin/stats`, `/api/admin/classes`, `/api/admin/family-links`, `/api/admin/learning-content`, `/api/admin/settings` |
| Media công khai | `GET /api/public/media/...` |

`contentData` và `submissionData` là **chuỗi JSON** bên trong request/response.
Hợp đồng ba loại bài và cách mã hóa đáp án được mô tả ở
[CLASSROOM.md](../android/docs/CLASSROOM.md). Backend chấm bài viết từ ảnh thật,
chấm lựa chọn/ghép câu theo đáp án; điểm cô chỉnh không xóa điểm tự động ban đầu.

Hoạt động học có UUID để khử trùng khi gửi lại. Báo cáo ngày dùng múi giờ
`Asia/Ho_Chi_Minh`. Liên kết phụ huynh cần mã một lần, hiệu lực 30 phút; mọi lần
đọc báo cáo/bài nộp đều kiểm tra quan hệ được chấp thuận.

## Trợ lý soạn bài Gemini

Tạo khóa tại [Google AI Studio](https://aistudio.google.com/apikey), đặt riêng
trong `.env.local` rồi khởi động lại backend:

```properties
AI_PROVIDER=gemini
AI_BASE_URL=https://generativelanguage.googleapis.com/v1beta
GEMINI_API_KEY=THAY_BANG_KHOA_RIENG_TREN_MAY_CHU
AI_MODEL=gemini-3.5-flash-lite
```

Đây là model mặc định trong mã; model sử dụng thực tế phải còn được nhà cung cấp
hỗ trợ và có quyền/hạn mức trong dự án Google của bạn. Bỏ trống khóa nếu chưa
sử dụng AI; giáo viên vẫn soạn bài thủ công hoặc dùng kho mẫu được.

- Gọi native Gemini `generateContent` qua HTTPS, khóa ở header `x-goog-api-key`.
- Yêu cầu structured JSON riêng cho `WRITING`, `QUIZ`, `SENTENCE`; backend kiểm
  tra lại loại bài, đáp án và số lượng trước khi trả bản nháp.
- Chỉ gửi lời nhắc, lịch sử gần nhất và bản nháp đang chỉnh; không tự gửi danh
  sách học sinh, điểm hoặc ảnh bài nộp. Không nhập dữ liệu riêng của trẻ vào lời nhắc.
- Mỗi giáo viên tối đa một yêu cầu đang chạy và 10 yêu cầu/phút. Không dùng WebSocket.
- Cấu hình có mặt chưa chứng minh khóa/model hoạt động; lỗi hạn mức, quá thời gian
  chờ hoặc dữ liệu sai không được báo thành công. Trợ lý không tự giao bài.

Adapter tương thích Chat Completions cũ vẫn hỗ trợ `AI_PROVIDER=openai-compatible`
với `AI_BASE_URL`, `AI_API_KEY`, `AI_MODEL` tương ứng. Bỏ `GEMINI_API_KEY` khi
chuyển sang adapter đó để tránh lấy nhầm khóa ưu tiên.

## Media và sao lưu

Tệp mới lưu ở `APP_MEDIA_DIRECTORY` (mặc định `backend/uploads` khi chạy từ backend),
không thuộc Git. Giới hạn upload là 8 MB/tệp. Backend còn đọc tài sản cũ trong
`frontend/public`; Vite không cần chạy để Android tải media từ backend.

Sao lưu MySQL cùng thư mục media. Bản sao repository không chứa database, cấu
hình riêng, ảnh/âm thanh đã upload hoặc bản nháp lưu cục bộ trên thiết bị.

## Kiểm thử

Các lệnh sau chạy từ `backend`:

```powershell
mvn test
mvn test "-Dtest=RegistrationLiveTest,ParentTrackingLiveTest,AdminWorkspaceLiveTest" -DliveBackend=true
```

Lệnh đầu chạy kiểm thử mặc định; các bài live bị bỏ qua. Lệnh sau cần backend
local/MySQL đã chạy và `.env.local` hợp lệ. Fixture tạo tài khoản/lớp riêng, dọn
đúng dữ liệu do test tạo; không dùng tài khoản đang sử dụng để ghi bài thử.

Để kiểm tra thật từ Android đã cài APK app/androidTest:

```powershell
adb -s '<serial>' reverse tcp:8080 tcp:8080
mvn test -Dtest=AndroidJourneyLiveTest -DliveAndroid=true "-DandroidSerial=<serial>"
```

Có thể đặt `-DdeviceBackendUrl=http://<IP-LAN>:8080/` để kiểm tra qua Wi-Fi thay
cho ADB reverse. Xem [quy trình Android](../android/docs/TESTING.md).

Gọi Gemini thật chỉ khi chủ động bật cờ; yêu cầu này sử dụng hạn mức của khóa:

```powershell
mvn test -Dtest=GeminiLiveTest -DliveGemini=true
```

## Xử lý lỗi thường gặp

| Hiện tượng | Kiểm tra |
| --- | --- |
| Không kết nối MySQL | MySQL đã chạy, đúng database/cổng và `DB_USERNAME`/`DB_PASSWORD` |
| Thiếu JWT secret | Đã điền `APP_JWT_SECRET`; working directory đúng `backend` |
| Cổng 8080 đang dùng | Chỉ chạy một backend; dừng đúng tiến trình dự án trước khi chạy lại |
| HTTP 401 | Token hết hạn, bị thu hồi hoặc tài khoản bị khóa; đăng nhập lại sau khi xử lý nguyên nhân |
| HTTP 403 | Thiếu vai trò/quyền, chưa thuộc lớp hoặc chưa liên kết con |
| Gemini chưa soạn được | Kiểm tra khóa/model, hạn mức, mạng máy chủ và cấu hình đã được nạp sau restart |
| Mất media sau chuyển máy | Khôi phục cả kho upload cùng DB và đối chiếu `APP_MEDIA_DIRECTORY` |
