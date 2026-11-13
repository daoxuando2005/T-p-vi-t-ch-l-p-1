# Vui Học — Android học sinh và phụ huynh

Ứng dụng native **Kotlin + Jetpack Compose**, dùng chung backend Spring Boot
với website giáo viên/admin. Android không kết nối MySQL trực tiếp.

Xem [tổng quan dự án](../README.md), [backend](../backend/README.md) và
[website](../frontend/README.md).

## Chức năng

### Học sinh

- Đăng ký/đăng nhập, khôi phục phiên đã mã hóa và đăng xuất. Đăng ký demo kích
  hoạt ngay, không OTP; email chưa được xác minh.
- **Trang chủ:** hành trình khám phá đủ 29 chữ, cuộn lên/xuống và chỉ báo bài đang học.
  Mỗi chữ theo thứ tự đọc → viết 2 lượt → điền 3 từ → ghép 3 câu; hoàn thành để
  mở chữ tiếp theo, có thể ôn lại.
- **Đọc chữ:** chữ hoa/thường, từ ví dụ, nghe chữ/từ bằng audio hoặc TTS tiếng Việt;
  nút chuyển sang tập viết đúng chữ đang chọn. Không thu âm/nhận dạng phát âm.
- **Tập viết:** mẫu chữ thường HP001 trên ô li, điểm bắt đầu/hướng nét, xem mẫu;
  vẽ bằng ngón tay/bút, chỉnh độ đậm, hoàn tác và xóa có xác nhận. Hoàn thành
  để nhận phản hồi độ bám mẫu, viết lại hoặc sang chữ tiếp theo.
- **Lớp học:** tham gia bằng mã, chọn lớp bằng danh sách thu gọn; ba thẻ màu
  Tập viết chữ/Đố vui ghép chữ/Ghép câu và huy hiệu bài chưa nộp.
- Làm và nộp đủ ba loại bài, giữ bản nháp khi gửi lỗi; xem lại bài, nguồn điểm
  tự động/cô chấm, ảnh viết và nhận xét.

### Phụ huynh

- Đăng ký/đăng nhập phụ huynh mở giao diện riêng với ba mục **Hôm nay**, **Tiến độ**,
  **Gia đình**.
- Liên kết con bằng mã một lần, hiệu lực 30 phút, lấy từ **Hồ sơ → Liên kết phụ
  huynh** trên tài khoản học sinh; có xác nhận khi ngắt liên kết.
- Chọn con/ngày để xem lượt đọc, viết, câu luyện tập, bài nộp và dòng hoạt động;
  theo dõi 29 chữ và hoạt động bảy ngày.
- Mở bài đã nộp, điểm và nhận xét của cô. Backend kiểm tra quyền xem theo quan hệ
  liên kết; đổi con/ngày hoặc mất quyền không được giữ báo cáo cũ trên màn hình.

Không có chat realtime hoặc WebSocket. Trợ lý Gemini soạn bài nằm ở website
giáo viên, không phải chatbot trò chuyện với học sinh.

## Môi trường

| Thành phần | Cấu hình của dự án |
| --- | --- |
| Thiết bị | Android 8.0/API 26 trở lên |
| Compile / target SDK | Compile 36.1; target 36 |
| JDK chạy Gradle | 21 |
| Android Gradle Plugin | 9.1.1 |
| Gradle Wrapper | 9.3.1 |
| Compose compiler plugin | 2.2.10 |
| SDK cần có | Platform 36.1, Build-Tools 36.0.0, Platform-Tools |

AGP 9 sử dụng Kotlin tích hợp; không thêm lại plugin `org.jetbrains.kotlin.android`.
Có thể dùng thiết bị thật hoặc máy ảo đã có; không bắt buộc tải máy ảo để chạy app.

## Mở bằng Android Studio

1. Chọn **Open**, mở riêng thư mục `android`.
2. Vào **Settings → Build, Execution, Deployment → Build Tools → Gradle**.
   Chọn Embedded JDK/jbr-21; nếu Studio hiển thị **Gradle JVM criteria**, chọn
   version **21**.
3. Dùng Gradle Wrapper đi kèm, đợi Sync. Cài các SDK còn thiếu qua SDK Manager.
4. Kết nối thiết bị qua USB hoặc Wireless debugging, chọn cấu hình **app** rồi Run.
5. Cấu hình URL backend trên màn hình đăng nhập của bản debug.

`local.properties` thuộc từng máy và được Git bỏ qua. Studio có thể tạo file này;
nếu đặt thủ công, thay đường dẫn ví dụ bằng SDK thật:

```properties
sdk.dir=C\:/path/to/Android/Sdk
```

Không đưa SDK/JDK path hoặc IP máy cá nhân vào cấu hình dùng chung của repository.

## Kết nối backend

Khởi động MySQL và backend theo [backend/README.md](../backend/README.md).
Trong bản debug, bấm **Cài đặt kết nối**, nhập URL gốc, **Lưu kết nối** rồi đăng nhập.
Nếu đang ở trang lỗi khôi phục phiên, nút cài đặt nằm trong phần nội dung cuộn.

| Thiết bị/kết nối | URL |
| --- | --- |
| Android Emulator, backend cùng máy | `http://10.0.2.2:8080/` |
| Điện thoại cùng Wi-Fi/LAN với máy tính | `http://<IP-LAN-của-máy-tính>:8080/` |
| Có ADB reverse cổng 8080 | `http://127.0.0.1:8080/` |

Lấy IP máy tính bằng `ipconfig`. IP có thể đổi sau khi kết nối lại mạng; cần cập
nhật URL trong app. Điện thoại phải truy cập được mạng máy chủ và cổng backend;
`localhost` trên điện thoại không trỏ tới máy tính.

Nếu đã kết nối ADB, có thể chuyển tiếp cổng để phát triển:

```powershell
adb devices -l
adb -s '<serial>' reverse tcp:8080 tcp:8080
```

Thay `<serial>` bằng ID thiết bị thực, rồi dùng URL `http://127.0.0.1:8080/` trong
app. Cách này cần giữ kết nối ADB.

Để cấu hình URL debug lúc build, dùng `-PapiBaseUrl` hoặc thêm vào `local.properties`:

```properties
hocchu.apiBaseUrl=http://10.0.2.2:8080/
```

Thứ tự mặc định lúc build: `-PapiBaseUrl` → `hocchu.apiBaseUrl` →
`http://10.0.2.2:8080/`. Giá trị đã lưu bằng màn hình Cài đặt kết nối của app có
thể thay mặc định đó. Không đưa mật khẩu DB, JWT secret hoặc khóa Gemini vào APK.

## Build và cài đặt

Chạy từ thư mục `android`, dùng JDK 21 trên máy:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
.\gradlew.bat :app:assembleDebug
adb -s '<serial>' install -r app/build/outputs/apk/debug/app-debug.apk
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`. Cài đè bằng `-r` để giữ dữ
liệu ứng dụng; không cần gỡ app để cập nhật bản phát triển.

Bản release cần URL HTTPS riêng và không có màn hình đổi kết nối debug:

```powershell
.\gradlew.bat :app:assembleRelease :app:lintRelease '-PreleaseApiBaseUrl=https://api.example.com/'
```

Thay URL bằng môi trường triển khai. Dự án chưa cấu hình khóa ký phát hành;
cần thiết lập signing riêng trước khi phân phối APK release.

## Phiên, tiến độ và dữ liệu ngoại tuyến

- Đăng nhập gọi `signin`, xác minh tài khoản bằng `/me` rồi mới lưu phiên/mở UI.
  Phiên lưu mã hóa bằng Android Keystore; không lưu mật khẩu trong SavedState.
- Dữ liệu học phân vùng theo **origin máy chủ + ID tài khoản**; không dùng tên
  hiển thị để nhận diện chủ dữ liệu. Đăng xuất giữ tiến độ/bản nháp của tài khoản.
- Tiến độ đã đồng bộ được tải/gộp từ backend, không hạ chặng đã đạt. Học liệu tải
  về có cache và nội dung đóng gói dự phòng.
- Hoạt động thực có UUID và outbox trên máy. Chỉ xóa sự kiện chờ sau khi backend
  xác nhận; gửi lại không được tăng đếm trùng.
- Bản nháp/nét tự luyện vẫn ở thiết bị. Đăng nhập trên máy khác không tự khôi
  phục các bản vẽ cục bộ chưa được gửi lên backend.
- Đổi URL có thể tạo vùng cache khác; dữ liệu cũ vẫn tách riêng, còn tiến độ đã
  đồng bộ được khôi phục từ đúng tài khoản backend.

## Âm thanh và chấm viết

App ưu tiên audio của học liệu/câu hỏi, dùng TextToSpeech tiếng Việt dự phòng.
Thiết bị cần có engine TTS, dữ liệu giọng Việt và âm lượng đa phương tiện phù hợp.
Lỗi thiếu giọng/file âm thanh có phản hồi; chỉ việc gửi lệnh đọc không chứng minh
người dùng đã nghe được.

Mẫu chữ và bảng ô li giữ tỉ lệ theo HP001. Chấm viết đo độ bám mẫu/độ phủ/độ chính
xác, chưa đánh giá thứ tự nét hay thay thế nhận xét sư phạm. Bài cô giao được
backend chấm lại từ PNG; cô có thể sửa điểm và hệ thống giữ điểm tự động gốc.
Xem [quy tắc tập viết](docs/HANDWRITING.md).

## Cấu trúc và quy tắc phát triển

Mã chính nằm trong `app/src/main/java/com/hocchu/mobile`:

| Thành phần | Trách nhiệm |
| --- | --- |
| `auth/` | Hợp đồng đăng ký/đăng nhập, repository, phiên mã hóa và ViewModel |
| `classroom/` | Lớp/bài thật, bản nháp, xuất PNG, nộp bài và xem lại |
| `family/` | Liên kết gia đình, báo cáo và outbox hoạt động |
| `data/` | Học liệu, tiến độ, lưu trữ và cache |
| `assessment/` | Chấm hình học nét viết |
| `ui/` | Màn hình, điều hướng, bảng viết, TTS và thành phần giao diện dùng chung |

Trước khi sửa Android, đọc [AGENTS.md](AGENTS.md) và tài liệu liên quan:
[kiến trúc](docs/ARCHITECTURE.md), [xác thực](docs/AUTHENTICATION.md),
[hành trình](docs/LEARNING_JOURNEY.md), [lớp học](docs/CLASSROOM.md),
[phụ huynh](docs/PARENT.md), [tập viết](docs/HANDWRITING.md),
[kiểm thử](docs/TESTING.md).

Font Nunito Sans được đóng gói cùng app; giấy phép tại
[licenses/NunitoSans-OFL.txt](licenses/NunitoSans-OFL.txt).

## Kiểm thử

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
.\gradlew.bat :app:assembleDebugAndroidTest
```

Kiểm thử thiết bị cần chọn đúng serial, cài app và APK androidTest cùng bản build.
Kiểm thử live phải có backend và tài khoản tạm riêng; dùng host
`AndroidJourneyLiveTest` theo [hướng dẫn](docs/TESTING.md). Không xóa phiên,
tiến độ hoặc dùng tài khoản thật để làm test chạy qua.

Trên thiết bị thực cần kiểm tra thêm bàn phím, cỡ chữ lớn, nghe TTS/audio, vẽ,
hoàn tác/xóa và giữ nét khi xoay máy. Điện thoại/tablet có bố cục riêng; từ 840dp
ứng dụng dùng navigation rail. API/geometry test không thay thế kiểm thử chạm.

## Xử lý lỗi thường gặp

| Hiện tượng | Kiểm tra |
| --- | --- |
| Chưa kết nối được máy chủ | Backend/MySQL đã chạy; URL app là IP hiện tại; điện thoại truy cập được cổng 8080 |
| Sync/Gradle lỗi JDK | Chọn JDK 21 và dùng Wrapper của dự án |
| `INSTALL_FAILED_USER_RESTRICTED` | Mở khóa thiết bị, cho phép cài qua USB/gỡ lỗi và chấp thuận yêu cầu cài nếu máy hiển thị |
| Chỉ tài khoản học sinh vào được app | Kiểm tra APK hiện tại và vai trò trả từ backend; app hỗ trợ cả học sinh/phụ huynh |
| Không có tiếng | Kiểm tra âm lượng media, engine TTS và dữ liệu giọng Tiếng Việt; thử audio học liệu |
| Test chạm báo `INJECT_EVENTS` | Thiết bị đang chặn mô phỏng chạm; kiểm tra quyền gỡ lỗi bảo mật của máy hoặc thực hiện kiểm tra thủ công |

Một số máy Xiaomi cần bật thêm **USB debugging (Security settings)** và khởi
động lại để điều khiển thiết bị qua công cụ phát triển. Tham khảo
[hướng dẫn thiết bị thật của Android](https://developer.android.com/studio/run/device#device-mirroring).
