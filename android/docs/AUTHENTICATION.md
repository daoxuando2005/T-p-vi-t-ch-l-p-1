# Quy tắc xác thực và kết nối backend

Áp dụng cho màn hình đăng nhập, HTTP, lưu phiên và phân vùng dữ liệu học sinh.
Phải tuân theo [kiến trúc](ARCHITECTURE.md). Không ghi kết quả thực hiện ở đây.

## Hợp đồng đăng nhập

1. Gửi `POST /api/auth/signin` bằng JSON `usernameOrEmail` và `password`.
   Bỏ khoảng trắng đầu/cuối tên đăng nhập; không tự sửa mật khẩu.
2. Phản hồi phải có ID dương, username hợp lệ, `roles` chứa `ROLE_STUDENT` hoặc `ROLE_PARENT`,
   `tokenType` bằng `Bearer`, `accessToken` đúng cấu trúc JWT và chưa hết hạn.
3. Gọi `GET /api/auth/me` với `Authorization: Bearer <token>` trước khi lưu phiên
   và mở giao diện đúng vai trò. ID từ `/me` phải khớp tài khoản vừa đăng nhập.
4. `/me` không cấp token mới; giữ token đã xác minh, cập nhật thông tin tài khoản.
   Không tự thay đổi hợp đồng backend để bỏ bước xác minh.

Đọc `exp` chỉ để hẹn hết hạn cục bộ; backend vẫn xác minh chữ ký và quyền.
Không dùng việc giải mã JWT ở client như bằng chứng token hợp lệ.

## Trạng thái và lỗi

- `AuthViewModel` giữ yêu cầu qua xoay màn hình. Dùng các trạng thái `Login`,
  `Loading`, `SignedIn`, `Retry`; không hiện nội dung tài khoản trước khi xác minh.
- Chặn gửi đăng nhập lặp khi yêu cầu đang chạy. Biểu mẫu phải có nhãn, kiểm tra
  trường bắt buộc, nút ẩn/hiện mật khẩu và thông báo lỗi đọc được.
- Khởi động lại phải đọc phiên mã hóa và gọi `/me`. Lỗi mạng giữ phiên để thử lại;
  hết hạn, sai vai trò hoặc cấu hình không hợp lệ phải đưa về đăng nhập phù hợp.
- Đăng xuất phải hủy yêu cầu đang chờ, xóa phiên trên máy và giữ tiến độ học.
  Tuần tự hóa thao tác ghi/xóa để yêu cầu cũ không khôi phục phiên sau đăng xuất.
- Không gọi đăng xuất cục bộ là thu hồi JWT trên mọi thiết bị: backend chưa có API này.
- Chỉ hiển thị lỗi đã ánh xạ an toàn; không đổ response body, stack trace hoặc token ra UI/log.

## Bảo vệ phiên

- `SessionVault` dùng AES-256/GCM và khóa Android Keystore; IV mới ở mỗi lần ghi.
  SharedPreferences `auth.session`, khóa `sealed.v1`, chỉ được giữ bản mã.
- Không lưu mật khẩu. Không đặt JWT vào Bundle, `rememberSaveable`, log, ảnh
  kiểm chứng, tài liệu hay Git. Giữ `android:allowBackup="false"` cho cấu hình hiện tại.
- Bản mã/khóa hỏng phải được xử lý an toàn, xóa phiên hỏng và cho đăng nhập lại.
  Kiểm tra kết quả ghi/xóa bộ nhớ; không báo thành công nếu lưu phiên thất bại.
- Mạng và ghi phiên đồng bộ phải chạy ngoài main thread. Giữ việc hủy coroutine;
  không bắt rồi nuốt `CancellationException`.
- Không theo HTTP redirect khi gửi thông tin xác thực. Transport hiện đặt timeout
  kết nối/đọc 8 giây và giới hạn phản hồi 64 KiB; giữ các giới hạn bảo vệ này khi sửa.

## Phân vùng tiến độ

- Dùng `accountPreferences(server, id)` làm định danh: origin máy chủ chuẩn hóa
  được băm SHA-256 và ID học sinh bất biến. Không dùng tên hiển thị làm khóa.
- Giữ định dạng `learning.user.<sha256-origin>.<student-id>`. Đổi backend origin
  hoặc học sinh phải tách dữ liệu; đăng nhập lại cùng tài khoản phải đọc lại vùng cũ.
- SharedPreferences `learning` của bản xem trước cũ chỉ phục vụ preview/fixture.
  Không tự chuyển dữ liệu này cho tài khoản thật đầu tiên.
- Bài cô giao được gửi theo [CLASSROOM.md](CLASSROOM.md). Hành trình lưu trên máy,
  khôi phục/gộp từ `GET /api/student/learning/progress` trước khi gửi snapshot/outbox
  theo [PARENT.md](PARENT.md). Chỉ gộp cùng origin và ID đã xác minh; không áp
  tiến độ, học liệu cache hay bản nháp của tài khoản khác.
- `UserAccount` dùng chung cho hai vai trò. `AuthGate` kết nối model đúng vai trò;
  đăng xuất/đổi vai trò phải hủy yêu cầu cũ và bỏ báo cáo phụ huynh trong bộ nhớ.
- Android chưa có đăng ký/quên mật khẩu/OTP; không tự bỏ xác thực server để
  tạo tài khoản. Giai đoạn này dùng tài khoản có sẵn hoặc admin cấp.

## Cấu hình debug và release

URL gốc chỉ chấp nhận HTTP/HTTPS phù hợp build, host hợp lệ, cổng hợp lệ và đường
dẫn rỗng, `/`, `/api` hoặc `/api/`. Chuẩn hóa về origin có dấu `/` cuối; từ chối
userinfo, query và fragment. Không ghép lặp `/api`.

| Build | Nguồn URL | Quy tắc |
| --- | --- | --- |
| Debug | `-PapiBaseUrl`, rồi `hocchu.apiBaseUrl` trong `local.properties`, mặc định `http://10.0.2.2:8080/` | Cho HTTP để phát triển local; có Cài đặt kết nối trên màn hình đăng nhập |
| Release | `-PreleaseApiBaseUrl=https://<host>/`; mặc định rỗng nếu chưa cấu hình | Chỉ HTTPS; không hiện cài đặt debug và không dùng URL local mặc định |

Giữ Network Security Config riêng: main chặn cleartext, debug cho HTTP.
Không tắt kiểm tra chứng chỉ/hostname. Điện thoại thật dùng IP máy tính cùng
mạng; máy ảo dùng `10.0.2.2`. Không đưa IP cá nhân cố định vào source dùng chung.

## Backend local

- Tạo `backend/.env.local` theo `backend/.env.example`; điền `DB_USERNAME`,
  `DB_PASSWORD`, `APP_JWT_SECRET`. JWT secret phải đủ ít nhất 64 byte UTF-8 và ngẫu nhiên.
- Working directory khi chạy backend phải là thư mục `backend` để Spring Boot
  nạp `.env.local` qua `spring.config.import`. MySQL phải hoạt động.
- Chỉ chạy một backend trên cổng 8080. Dừng đúng tiến trình dự án khi cần đổi cách chạy.
- `.env.local`, `.idea`, log và `android/local.properties` phải ở ngoài Git.
  Không đưa mật khẩu MySQL/khóa JWT máy chủ vào APK. Môi trường triển khai dùng secret
  của máy chủ, không sao chép file local vào kho mã.

## Tiêu chí kiểm tra

Kiểm tra hợp đồng HTTP, URL release, vai trò, token hết hạn/hỏng, Keystore,
đăng xuất giữa yêu cầu, đổi tài khoản và khôi phục phiên theo [TESTING.md](TESTING.md).
Không thêm đường vào app sản phẩm để bỏ qua đăng nhập nhằm chạy kiểm thử.
