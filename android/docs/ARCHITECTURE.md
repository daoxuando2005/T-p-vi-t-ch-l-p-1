# Kiến trúc và quy tắc phát triển Android

Tài liệu này quy định cách duy trì ứng dụng Android hiện tại. Phải giữ các ràng
buộc dưới đây khi thay đổi chức năng liên quan. Không ghi kết quả thực hiện ở đây.
Điểm vào của bộ quy tắc: [AGENTS.md](../AGENTS.md).

## Phạm vi sản phẩm

- Ứng dụng học sinh native Kotlin và Jetpack Compose; namespace/application ID
  là `com.hocchu.mobile`. Không thay giao diện native bằng trang web nhúng.
- Backend Spring Boot và website nằm ở `../../backend` và `../../frontend`.
  Android gọi API backend, không kết nối MySQL trực tiếp.
- Xác thực, tham gia lớp, nhận bài và nộp bài dùng backend thật. Bài viết chờ
  giáo viên chấm; trắc nghiệm được backend tính điểm, nhận xét tải lại từ server.
- Học liệu hành trình 29 chữ vẫn là nội dung mẫu. Tiến độ hành trình và bản nháp
  bài tập lưu trên thiết bị theo tài khoản, chưa đồng bộ tiến độ hành trình giữa thiết bị.
- Không mô tả đăng ký/OTP, nhận dạng phát âm, chấm nét viết tự động hoặc đồng bộ
  tiến độ hành trình là chức năng đã có của Android.

## Nền tảng xây dựng

| Thành phần | Cấu hình của dự án |
| --- | --- |
| JDK chạy Gradle | 21; dùng Embedded JDK/jbr-21 của Android Studio nếu có |
| Gradle Wrapper | 9.3.1 |
| Android Gradle Plugin | 9.1.1; Kotlin tích hợp, không thêm `org.jetbrains.kotlin.android` |
| Compose compiler plugin | 2.2.10 |
| Compose BOM | 2025.08.01 |
| SDK biên dịch | 36.1 |
| minSdk / targetSdk | 26 / 36 |
| Java/Kotlin JVM target | 17 |

Dùng Wrapper trong dự án. Không tự nâng phiên bản toolchain hoặc thư viện ngoài
phạm vi yêu cầu. Không đưa đường dẫn SDK/JDK riêng của máy vào cấu hình dùng chung.
`local.properties` phải được Git bỏ qua.

## Phân công mã nguồn

Các đường dẫn sau nằm trong `app/src/main/java/com/hocchu/mobile/`.

| Thành phần | Trách nhiệm |
| --- | --- |
| `MainActivity.kt` | Bật edge-to-edge, tạo `AuthViewModel`, dựng theme và `AuthGate` |
| `auth/AuthModels.kt`, `AuthApi.kt` | Mô hình phiên, chuẩn hóa URL, hợp đồng HTTP |
| `auth/AuthRepository.kt`, `SessionVault.kt`, `AuthViewModel.kt` | Xác minh, lưu phiên mã hóa và điều phối trạng thái xác thực |
| `ui/LoginScreen.kt` | Cổng đăng nhập, biểu mẫu, trạng thái chờ/lỗi và đổi kết nối debug |
| `classroom/ClassroomModels.kt`, `ClassroomApi.kt` | Mô hình lớp/bài tập và hợp đồng API dùng Bearer token |
| `classroom/ClassroomViewModel.kt`, `AssignmentDraftStore.kt`, `SubmissionImages.kt` | Trạng thái theo tài khoản, bản nháp và ảnh bài viết |
| `ui/ClassroomScreen.kt` | Tham gia/chọn lớp, làm bài, nộp bài và xem điểm/nhận xét |
| `ui/StudentApp.kt` | Khung học sinh, điều hướng, vùng lưu trạng thái và tiến độ tài khoản |
| `data/DemoContent.kt`, `LetterCurriculum.kt` | Danh sách 29 chữ và học liệu minh họa |
| `data/LessonProgress.kt`, `LessonProgressStore.kt` | Quy tắc bài học và lưu tiến độ |
| `ui/LearningScreens.kt`, `LetterLessonScreen.kt` | Trang chủ, đọc chữ, lớp minh họa và hành trình |
| `ui/WritingScreen.kt`, `CopybookForm.kt`, `CopybookPaper.kt` | Nét vẽ, hình học mẫu chữ và giấy ô li |
| `ui/ExerciseScreens.kt` | Đố vui, ghép câu và phản hồi |
| `ui/DesignSystem.kt`, `JourneyDecoration.kt`, `Speech.kt` | Giao diện dùng chung, trang trí và vòng đời TTS |

Giữ HTTP và lưu phiên ngoài composable. Luồng API mới phải có lớp dữ liệu và
trạng thái chờ/lỗi rõ ràng, không đặt yêu cầu mạng trực tiếp trong phần dựng UI.

## Giao diện và trạng thái

- Giữ bốn tab: Trang chủ, Đọc chữ, Tập viết, Lớp học. Nút Back phải trở về từ
  hoạt động đang mở trước khi rời khung học sinh.
- Dùng `VuiHocTheme` và thành phần dùng chung; giữ chữ tiếng Việt có dấu,
  font Nunito Sans đóng gói cùng app và giấy phép đi kèm.
- Header gọn, không khôi phục lời chào và các dòng phụ đã bỏ. Trang chủ không
  thêm lại “Tự luyện hoặc học theo…”, “Đến chữ…” hay nhãn “Chưa mở” cạnh mỗi chữ.
  Bài khóa vẫn phải có chỉ báo khóa và mô tả trạng thái cho trợ năng.
- Tiêu đề lớp là “Lớp học của bé” trên header. Tên lớp và nút tham gia cùng hàng;
  ô nhập mã mở khi bấm. Không lặp lại lời giới thiệu hoặc số hoạt động dưới tiêu đề.
- Ở chiều rộng dưới 840dp dùng điều hướng dưới; từ 840dp dùng navigation rail.
  Không khóa xoay màn hình để che lỗi bố cục. Kiểm tra riêng bàn phím và cỡ chữ lớn.
- Giữ trạng thái tab, hoạt động, lựa chọn và nét viết bằng cơ chế saveable phù hợp.
  Không nhầm SavedState với lưu trữ bản vẽ lâu dài sau mọi lần đóng app.
- Đổi học sinh phải tạo cây trạng thái riêng theo vùng tiến độ tài khoản. Không
  dùng chung bản vẽ, đáp án đang chọn hoặc tiến độ của học sinh trước.
- Âm thanh dùng TTS tiếng Việt của thiết bị. Lỗi hoặc thiếu giọng đọc phải có
  phản hồi; vẫn cho phép sử dụng phần không cần âm thanh. Giải phóng TTS khi hết vòng đời.

## Khi sửa chức năng

Tuân theo [xác thực](AUTHENTICATION.md), [lớp học](CLASSROOM.md), [hành trình](LEARNING_JOURNEY.md),
[tập viết](HANDWRITING.md) và chọn kiểm tra trong [TESTING.md](TESTING.md).
Không xóa cấu hình local, dữ liệu học hoặc tài sản đang dùng như một phần dọn mã.
