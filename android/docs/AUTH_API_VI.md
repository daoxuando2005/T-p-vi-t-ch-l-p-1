# Đăng nhập Android với backend local

## Phạm vi

- Mở app qua màn hình đăng nhập. Dùng tài khoản **học sinh** đã có trong backend.
- Gửi `POST /api/auth/signin` với `usernameOrEmail`, `password`; sau đó gọi
  `GET /api/auth/me` bằng Bearer token trước khi vào giao diện học sinh.
- Khi mở lại app, đọc phiên mã hóa và xác minh lại `/me`. Lỗi mạng giữ phiên để
  thử lại; token hết hạn/không còn hợp lệ hoặc sai vai trò thì yêu cầu đăng nhập lại.
- Đăng xuất xóa phiên trên thiết bị. Backend hiện không có API thu hồi JWT;
  không mô tả đây là thu hồi token trên mọi thiết bị.
- Chưa thêm đăng ký/OTP, lớp học thật, đồng bộ học liệu hoặc tiến độ lên backend.

## Lưu phiên và tiến độ

`AuthApi` xử lý hợp đồng HTTP, timeout, giới hạn kích thước phản hồi và không theo
redirect khi gửi thông tin đăng nhập. `AuthRepository` xác minh `/me` trước khi lưu;
ghi/xóa phiên được tuần tự hóa để logout không bị một yêu cầu cũ khôi phục ngược lại.

`SessionVault` dùng AES-256/GCM với khóa Android Keystore, IV mới cho mỗi lần ghi.
SharedPreferences chỉ giữ bản mã; không lưu mật khẩu. Token không đi vào
`rememberSaveable`, Bundle hoặc log. Lỗi bản mã/khóa thì xóa phiên hỏng và đăng nhập lại.
Các thao tác mạng và ghi lưu trữ đồng bộ chạy trên `Dispatchers.IO`.

`AuthViewModel` giữ yêu cầu đang chạy khi xoay máy, kiểm tra phiên lúc mở app và
chuyển về đăng nhập khi JWT đến hạn. Thời hạn lấy từ JWT chỉ dùng để hẹn giờ cục bộ;
backend vẫn là nơi xác minh token và vai trò.

Tiến độ lưu theo **origin máy chủ + ID học sinh**, không theo tên hiển thị.
Đổi tài khoản tạo cây giao diện/state holder khác; đăng xuất không xóa bài đã hoàn thành.
Dữ liệu bản xem trước cũ trong `learning` được giữ riêng, không tự gán cho tài khoản
đăng nhập đầu tiên. Đổi sang một địa chỉ backend khác tạo vùng tiến độ khác.

## Chạy backend trong IntelliJ

1. Mở project `backend`, dùng JDK 21 đã cấu hình.
2. Tạo `backend/.env.local` dựa theo `.env.example`, điền `DB_USERNAME`, `DB_PASSWORD`
   và `APP_JWT_SECRET`. Khóa JWT cần ít nhất 64 byte UTF-8, nên dùng chuỗi ngẫu nhiên.
3. Working directory của cấu hình `VietLettersApplication` phải là thư mục `backend`.
   Spring Boot sẽ nạp `.env.local` qua `spring.config.import`.
4. MySQL phải đang chạy. Run `com.hocchu.web.VietLettersApplication`.
5. Chỉ chạy một backend trên cổng 8080. Nếu backend đang chạy từ terminal, dừng
   đúng tiến trình đó trước khi Run thêm một bản trong IntelliJ.

File `.env.local`, cấu hình `.idea`, log và `android/local.properties` được Git bỏ qua.
Không đặt mật khẩu MySQL hoặc khóa JWT vào source, APK hoặc README. Trong môi trường
triển khai, truyền các giá trị bằng biến môi trường/secret manager của máy chủ.

## Địa chỉ kết nối Android

Trong `android/local.properties`, thêm cấu hình riêng của máy:

```properties
hocchu.apiBaseUrl=http://IP-MAY-TINH:8080/
```

Máy ảo Android dùng `http://10.0.2.2:8080/`; điện thoại thật dùng IP máy tính cùng
mạng Wi-Fi. Nếu IP thay đổi, bản debug có **Cài đặt kết nối** trên màn hình đăng nhập.
Cũng có thể build với `-PapiBaseUrl=http://...:8080/` để ghi đè giá trị local.

HTTP chỉ được cấu hình cho source set **debug**. Bản release có cấu hình mạng chặn
cleartext và kiểm tra URL chỉ chấp nhận HTTPS; đặt `-PreleaseApiBaseUrl=https://.../`
khi build release. Debug settings không xuất hiện trong release.

## Kiểm tra

- JVM: `AuthModelsTest` kiểm tra URL, HTTPS release và phân vùng dữ liệu tài khoản.
- Android: `AuthProtocolTest` kiểm tra payload, `/me`, vai trò, hết hạn, dữ liệu hỏng,
  ghi/xóa phiên và mã hóa thật bằng Keystore với bộ nhớ kiểm thử riêng.
- UI: `LoginScreenTest` kiểm tra thông tin bắt buộc, ẩn/hiện mật khẩu, trạng thái
  đang gửi, lỗi server và đổi học sinh không lẫn tiến độ.
- Live opt-in: `AuthLiveBackendTest`, `AuthLiveLoginTest`, chạy với
  `-e liveBackend true`. Dùng tài khoản mẫu công khai trong README. Test UI không
  thay thế phiên người dùng đã có, không xóa tiến độ; phiên demo do test tạo sẽ được xóa.
- Các kiểm thử học sinh cũ dùng composition riêng trong APK androidTest, không có
  Activity bỏ qua đăng nhập trong APK sản phẩm.

### Kết quả kiểm tra ngày 09/11/2026

- Build debug/release và Lint debug/release thành công, 0 lỗi Lint. Còn cảnh báo
  phiên bản thư viện, cấu hình HTTP chỉ ở debug và gợi ý API ghi SharedPreferences.
  Đã kiểm tra trực tiếp tài nguyên trong APK release: `cleartextTrafficPermitted=false`,
  `BuildConfig.DEBUG=false`; không dùng URL local mặc định của debug.
- 7 kiểm tra JVM đạt: 3 kiểm tra xác thực/phân vùng tài khoản và 4 kiểm tra tiến độ.
- 8 kiểm tra Android đạt: 7 giao thức/Keystore và 1 gọi API thật từ điện thoại qua Wi-Fi.
- 2 kiểm tra UI đạt: biểu mẫu và chuyển học sinh giữ đúng tiến độ riêng.
- 1 kiểm tra UI với backend thật đạt: đăng nhập tài khoản mẫu, tạo ViewModel mới
  khôi phục phiên từ bộ nhớ mã hóa, gọi `/me` và đăng xuất xóa phiên.
- 3 kiểm tra giao diện cũ đạt: xem mẫu giữ nét, phục hồi SavedState của bài viết và
  bố cục ngang. Phục hồi SavedState dùng `StateRestorationTester` trong fixture riêng;
  kiểm tra bố cục ngang chạy trên Activity thực.
- Đã duyệt ảnh màn hình đăng nhập và trang chủ sau đăng nhập. Đã cài bản debug lên
  Xiaomi 23127PN0CC / Android 16. Không tự gán dữ liệu xem trước cũ cho tài khoản mới.
- Backend chạy được bằng cấu hình local mới. File chứa mật khẩu/khóa JWT được Git
  bỏ qua và đã kiểm tra không xuất hiện trong các file thay đổi sẽ đưa vào Git.
