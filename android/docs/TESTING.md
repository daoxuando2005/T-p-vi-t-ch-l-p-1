# Quy tắc kiểm tra thay đổi Android

Tài liệu này quy định cách chọn kiểm tra và tiêu chí cần đạt. Tuân theo
[AGENTS.md](../AGENTS.md) và [kiến trúc](ARCHITECTURE.md).
Không ghi kết quả thực hiện, số test đã đạt, ngày chạy hay thiết bị đã dùng ở đây.

## Chọn kiểm tra theo thay đổi

| Phạm vi sửa | Kiểm tra cần thực hiện |
| --- | --- |
| Chỉ tài liệu | Đường dẫn/link, tham chiếu tên cũ, đối chiếu quy tắc với mã và kiểm tra diff |
| Kotlin hoặc cấu hình debug | Build debug, unit test liên quan, Lint; build APK kiểm thử nếu có ảnh hưởng |
| Tiến độ/học liệu | `LessonProgressTest`; `LetterJourneyTest` khi sửa điều phối hoặc tương tác |
| Hình học mẫu chữ | `CopybookFormTest`; duyệt hình đủ 29 chữ và các chữ đại diện |
| Bảng viết/trạng thái/bố cục | `StudentExperienceTest` và kiểm tra thao tác viết theo thay đổi |
| Xác thực/phiên/kết nối | `AuthModelsTest`, `AuthProtocolTest`, `LoginScreenTest` theo lớp bị ảnh hưởng |
| Lớp học, bản nháp, nộp bài | `ClassroomProtocolTest`, `ClassroomScreenTest`; chạy live có chủ đích bằng `ClassroomLiveBackendTest` |
| Luồng đăng nhập thật | Kiểm thử live có chủ đích, chỉ bật khi backend và tài khoản mẫu sẵn sàng |
| Mạng/cấu hình release | Build và Lint release, kiểm tra HTTPS và tài nguyên mạng trong APK release |

Không tạo bài kiểm thử chỉ lặp lại chi tiết triển khai hoặc chạy toàn bộ kiểm thử
thiết bị cho một thay đổi Markdown. Lỗi hoặc thay đổi mới phải được kiểm tra lại
ở phạm vi liên quan trước khi xác nhận hoàn thành.

## Lệnh cục bộ

Chạy từ thư mục `android`, với JDK 21 đã được chọn:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:assembleDebugAndroidTest
```

Khi thay đổi release, cung cấp URL HTTPS của môi trường cần kiểm tra:

```powershell
.\gradlew.bat :app:assembleRelease :app:lintRelease '-PreleaseApiBaseUrl=https://<host>/'
```

`<host>` là chỗ thay bằng hostname thật; không dùng URL ví dụ làm cấu hình phát hành.
Để chạy instrumentation, cài đè APK app và androidTest cùng bản build; không gỡ
app để cài lại. Chọn đúng serial bằng `adb devices -l`.

```powershell
adb -s <serial> shell am instrument -w -r -e disableAnalytics true `
  -e class com.hocchu.mobile.CopybookFormTest `
  com.hocchu.mobile.test/androidx.test.runner.AndroidJUnitRunner
```

Thay tên lớp theo mục tiêu. `AuthLiveBackendTest` và `AuthLiveLoginTest` cần thêm
`-e liveBackend true`; `ClassroomLiveBackendTest` cũng yêu cầu cờ này. Có thể truyền
`-e backendUrl http://10.0.2.2:8080/` cho bài kiểm tra lớp học trên máy ảo.
Không bật mặc định. Kiểm tra live không được ghi đè phiên
có sẵn và chỉ dùng tài khoản mẫu phục vụ kiểm thử.

## Bảo vệ dữ liệu

- Store, SharedPreferences, khóa Keystore hoặc tài khoản thử nghiệm phải được
  tách khỏi dữ liệu đang sử dụng. Dọn đúng dữ liệu do test tạo sau khi chạy.
- Không gọi `pm clear`, gỡ app, xóa `learning*`, xóa phiên thật hoặc đặt lại MySQL
  để làm test chạy qua. Giữ `leaveApksInstalledAfterRun` trong cấu hình hiện tại.
- Fixture học sinh chỉ nằm trong mã kiểm thử. Không tạo Activity/đường điều hướng
  sản phẩm bỏ qua đăng nhập cho tiện kiểm thử.
- Phân biệt `StateRestorationTester` mô phỏng SavedState với xoay Activity thật.
  Chỉ mô tả đúng loại kiểm tra đã thực hiện.
- Nếu điện thoại chuyển app hoặc mất kết nối, xác định trạng thái trước khi chạy
  lại; không coi lượt đang chờ là đã đạt hay ép tương tác vào ứng dụng khác.

## Tiêu chí duyệt giao diện

- Xem điện thoại và tablet/ngang, cỡ chữ hệ thống lớn, status/navigation bar và
  bàn phím. Nút chính/công cụ viết phải dùng được, không bị che hoặc buộc cuộn bất tiện.
- Đăng nhập/khôi phục/đăng xuất/đổi tài khoản phải giữ đúng phiên và tiến độ riêng.
- Lớp học phải hiển thị dữ liệu thật, giữ bản nháp khi gửi thất bại, chỉ báo đã nộp
  sau xác nhận của backend và trả được điểm/nhận xét của cô. Kiểm tra đủ chữ được
  giao trước khi gửi ảnh; không tạo điểm viết ngẫu nhiên.
- Hành trình phải đi hết bài A, xử lý sai/đúng, lưu giữa chặng, ôn tập, mở Ă và
  kết thúc Y. Không nhảy bước hoặc đếm lại đáp án cũ.
- Viết phải giữ công cụ và nét khi đổi bố cục, mở mẫu; kiểm tra hoàn tác, hủy/xác
  nhận xóa, đổi độ đậm, viết lại/tiếp tục và liên kết từ màn đọc.
- Release phải chặn HTTP, không hiện cài đặt kết nối debug và không dùng URL local
  mặc định. Không suy luận release an toàn chỉ từ kết quả chạy bản debug.

## Báo kết quả

Báo kiểm tra thực sự đã chạy, lỗi/cảnh báo còn lại và phần chưa kiểm chứng trong
phản hồi hoặc mô tả thay đổi. Không sửa tài liệu này thành nhật ký “đã kiểm thử”.
Log/ảnh/đầu ra tạo tự động không thuộc bộ tài liệu quy tắc; không tự đưa vào Git.
