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
| Cấu hình release | Build và Lint release, kiểm tra cấu hình và tài nguyên trong APK release |

Nhánh `main` chưa có luồng đăng nhập/backend và các lớp kiểm thử xác thực. Khi
tích hợp tính năng này, bổ sung kiểm tra tương ứng trước khi cập nhật danh mục.

Không tạo bài kiểm thử chỉ lặp lại chi tiết triển khai hoặc chạy toàn bộ kiểm thử
thiết bị cho một thay đổi Markdown. Lỗi hoặc thay đổi mới phải được kiểm tra lại
ở phạm vi liên quan trước khi xác nhận hoàn thành.

## Lệnh cục bộ

Chạy từ thư mục `android`, với JDK 21 đã được chọn:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:assembleDebugAndroidTest
```

Khi thay đổi cấu hình release:

```powershell
.\gradlew.bat :app:assembleRelease :app:lintRelease
```

Để chạy instrumentation, cài đè APK app và androidTest cùng bản build; không gỡ
app để cài lại. Chọn đúng serial bằng `adb devices -l`.

```powershell
adb -s <serial> shell am instrument -w -r -e disableAnalytics true `
  -e class com.hocchu.mobile.CopybookFormTest `
  com.hocchu.mobile.test/androidx.test.runner.AndroidJUnitRunner
```

Thay tên lớp theo mục tiêu: `CopybookFormTest`, `StudentExperienceTest` hoặc
`LetterJourneyTest`. Kiểm tra tiến độ thuần Kotlin dùng `LessonProgressTest`.

## Bảo vệ dữ liệu

- Store và SharedPreferences kiểm thử phải tách khỏi dữ liệu đang sử dụng.
  Dọn đúng dữ liệu do test tạo sau khi chạy.
- Không gọi `pm clear`, gỡ app, xóa `learning*`, xóa phiên thật hoặc đặt lại MySQL
  để làm test chạy qua. Giữ `leaveApksInstalledAfterRun` trong cấu hình hiện tại.
- Fixture phục vụ kiểm thử phải nằm trong mã kiểm thử. Khi thêm xác thực, không
  tạo Activity/đường điều hướng sản phẩm bỏ qua đăng nhập cho tiện chạy test.
- Phân biệt `StateRestorationTester` mô phỏng SavedState với xoay Activity thật.
  Chỉ mô tả đúng loại kiểm tra đã thực hiện.
- Nếu điện thoại chuyển app hoặc mất kết nối, xác định trạng thái trước khi chạy
  lại; không coi lượt đang chờ là đã đạt hay ép tương tác vào ứng dụng khác.

## Tiêu chí duyệt giao diện

- Xem điện thoại và tablet/ngang, cỡ chữ hệ thống lớn, status/navigation bar và
  bàn phím. Nút chính/công cụ viết phải dùng được, không bị che hoặc buộc cuộn bất tiện.
- Mở lại app phải giữ tiến độ đã xác nhận. Khi bổ sung đăng nhập, phải kiểm tra
  đăng xuất/đổi tài khoản không làm lẫn tiến độ và phiên của học sinh.
- Hành trình phải đi hết bài A, xử lý sai/đúng, lưu giữa chặng, ôn tập, mở Ă và
  kết thúc Y. Không nhảy bước hoặc đếm lại đáp án cũ.
- Viết phải giữ công cụ và nét khi đổi bố cục, mở mẫu; kiểm tra hoàn tác, hủy/xác
  nhận xóa, đổi độ đậm, viết lại/tiếp tục và liên kết từ màn đọc.
- Nếu tích hợp mạng, phải kiểm tra chính sách HTTPS và sự tách biệt cấu hình
  debug/release. Không suy luận release an toàn chỉ từ kết quả chạy bản debug.

## Báo kết quả

Báo kiểm tra thực sự đã chạy, lỗi/cảnh báo còn lại và phần chưa kiểm chứng trong
phản hồi hoặc mô tả thay đổi. Không sửa tài liệu này thành nhật ký “đã kiểm thử”.
Log/ảnh/đầu ra tạo tự động không thuộc bộ tài liệu quy tắc; không tự đưa vào Git.
