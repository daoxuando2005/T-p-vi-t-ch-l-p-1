# Vui Học Lớp 1 — Android

Ứng dụng **native Kotlin + Jetpack Compose**, dựng lại khu vực học sinh từ bộ thiết kế Stitch do người dùng cung cấp. Website và backend hiện có tiếp tục nằm ở `../frontend` và `../backend`.

## Quy tắc phát triển

Trước khi sửa Android, đọc [AGENTS.md](AGENTS.md) và các tài liệu liên quan:

- [Kiến trúc và phạm vi](docs/ARCHITECTURE.md)
- [Xác thực và kết nối](docs/AUTHENTICATION.md)
- [Lớp học và bài cô giao](docs/CLASSROOM.md)
- [Hành trình học và tiến độ](docs/LEARNING_JOURNEY.md)
- [Mẫu chữ và tập viết](docs/HANDWRITING.md)
- [Quy định kiểm thử](docs/TESTING.md)

Các tài liệu này là quy tắc duy trì dự án, không phải nhật ký. Không ghi kết quả
thực hiện ở đây; chỉ cập nhật khi quy tắc hoặc chức năng tương ứng thay đổi.

## Phạm vi ứng dụng

- Đăng nhập học sinh bằng backend Spring Boot, xác minh/khôi phục phiên, đăng xuất và lưu tiến độ riêng theo tài khoản. Xem [cấu hình backend local và API xác thực](docs/AUTHENTICATION.md).
- Trang chủ: thẻ tự luyện và hành trình 29 chữ theo thứ tự. Mỗi chữ: đọc theo → viết 2 lượt (có thể luyện thêm) → điền 3 từ → ghép 3 câu mẫu. Tiến độ lưu trên thiết bị, hoàn thành mới mở chữ kế tiếp; bài đã học có thể ôn lại. Xem [luồng bài học và lưu tiến độ](docs/LEARNING_JOURNEY.md).
- Đọc chữ: 29 chữ cái, chữ hoa/thường, từ ví dụ, đọc bằng TextToSpeech tiếng Việt trên thiết bị.
- Tập viết: 29 mẫu chữ thường cỡ vừa từ HP001, căn theo chân chữ và tỉ lệ ô li; dấu phụ và đuôi chữ được chừa chỗ riêng. Có chấm đặt bút, hướng khởi đầu và hộp xem mẫu. Nét vẽ bằng ngón tay/bút, chọn độ đậm, hoàn tác, xác nhận xóa. Tọa độ chuẩn hóa và lưu trạng thái khi xoay máy/chuyển tab. Đổi chữ sẽ bắt đầu bảng viết mới. Chi tiết và nguồn đối chiếu: [Mẫu chữ và ô ly](docs/HANDWRITING.md).
- Lớp học: nhập mã để tham gia lớp thật, chọn lớp, tải bài cô giao, làm trắc nghiệm hoặc viết các chữ được giao và nộp bài. Bản nháp lưu riêng theo tài khoản; gửi lỗi giữ bài để thử lại. Điểm/nhận xét được tải từ server. Xem [quy tắc lớp học](docs/CLASSROOM.md).
- Trắc nghiệm trong hành trình: chọn đáp án, kiểm tra, thử lại và tiếp tục. Bài cô giao được backend chấm sau khi nộp.
- Ghép câu: chọn/bỏ thẻ từ, nghe câu mẫu, kiểm tra thứ tự.
- Điện thoại dùng thanh điều hướng dưới; từ 840dp dùng navigation rail. Nội dung đọc chữ và tập viết chuyển sang hai cột khi đủ rộng.

**Đăng nhập, lớp học và bài cô giao đã dùng backend.** Học liệu hành trình trong `DemoContent.kt` và `LetterCurriculum.kt` vẫn là nội dung mẫu. Tiến độ hành trình và bản nháp lưu trên thiết bị theo từng tài khoản; bài nộp được gửi lên server. Chưa đồng bộ hành trình giữa thiết bị, chưa nhận dạng phát âm hoặc chấm nét viết tự động. Không tạo điểm viết ngẫu nhiên; bài viết chờ cô chấm. Dữ liệu bản xem trước cũ được giữ riêng.

## Mở bằng Android Studio

1. Chọn **Open**, mở thư mục `android` này (không chọn thư mục gốc chứa backend/frontend).
2. Trong **Settings → Build, Execution, Deployment → Build Tools → Gradle**, chọn **Gradle JDK = Embedded JDK / jbr-21** đi kèm Studio. Không dùng Java 8 mặc định trên máy.
3. Dùng Gradle Wrapper đã kèm theo và đợi Gradle Sync hoàn tất.
4. SDK Manager cần **Android SDK Platform 36.1**, **Build-Tools 36.0.0** và Platform-Tools.
5. Device Manager → Create Virtual Device: chọn một điện thoại và một tablet; hoặc kết nối thiết bị Android thật đã bật USB debugging. Chọn cấu hình `app` rồi Run.

`local.properties` chứa đường dẫn SDK của từng máy, được Git bỏ qua. Android Studio có thể tạo lại file này khi mở dự án. Nếu cần thiết lập thủ công:

```properties
sdk.dir=C\:/path/to/Android/Sdk
```

Toolchain cố định: Android Gradle Plugin **9.1.1**, Gradle **9.3.1**, Compose compiler plugin **2.2.10**. AGP 9 có Kotlin tích hợp, vì vậy không thêm plugin `org.jetbrains.kotlin.android`. API tối thiểu **26** (Android 8), target **36**.

## Build trên PowerShell

Chạy trong thư mục `android`:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

Thay đường dẫn SDK/JDK ví dụ bằng đường dẫn thật trên máy.
APK: `app/build/outputs/apk/debug/app-debug.apk`.

Preview điện thoại và tablet có sẵn ở cuối `StudentApp.kt`; mở file trong Studio và chọn **Split / Design**. Để kiểm tra viết tay, cần Run trên máy ảo hoặc thiết bị thật. Nghe đọc cần thiết bị có engine TTS và dữ liệu giọng tiếng Việt.

## Cấu trúc mã

```text
app/src/main/java/com/hocchu/mobile/
  MainActivity.kt
  auth/                   API xác thực, phiên mã hóa, repository và ViewModel
  classroom/              Lớp/bài thật, ViewModel, bản nháp và xuất ảnh bài viết
  ui/ClassroomScreen.kt    Tham gia lớp, làm/nộp bài, xem kết quả
  data/DemoContent.kt      Danh sách 29 chữ và bài minh họa
  data/LetterCurriculum.kt Học liệu theo chữ
  data/LessonProgress.kt   Quy tắc tiến độ và codec
  data/LessonProgressStore.kt Lưu tiến độ trong vùng tài khoản
  ui/LoginScreen.kt       Cổng đăng nhập và biểu mẫu
  ui/DesignSystem.kt       Màu, font, nút, thẻ, nhân vật vẽ bằng code
  ui/StudentApp.kt         Khung giao diện, điều hướng, trạng thái màn hình
  ui/LearningScreens.kt    Trang chủ, đọc chữ, lớp học
  ui/LetterLessonScreen.kt Điều phối đọc → viết → từ → câu
  ui/WritingScreen.kt      Bảng ô ly và dữ liệu nét vẽ
  ui/CopybookForm.kt       Tỉ lệ, chân chữ và dữ liệu 29 mẫu chữ thường
  ui/CopybookPaper.kt      Giấy ô ly, mẫu chữ và điểm bắt đầu
  ui/ExerciseScreens.kt    Trắc nghiệm và ghép câu
  ui/JourneyDecoration.kt  Trang trí hành trình
  ui/Speech.kt             Quản lý vòng đời TextToSpeech
```

Font Nunito Sans được đóng gói để không cần tải font khi chạy; giấy phép nằm ở `licenses/NunitoSans-OFL.txt`. HP001 được dùng lại từ tài sản sẵn có của dự án; bản ZIP thiết kế và ảnh tham khảo không được đưa vào Git.

## Kiểm tra khi duyệt giao diện

- Đi qua bốn tab, dùng Back từ bài tập để trở lại lớp.
- Đọc chữ: đổi chữ, nghe chữ/từ; kiểm tra chữ đầu/cuối danh sách.
- Tập viết: vẽ cả nét dọc/ngang, kiểm tra trang không cuộn khi viết; hoàn tác; hủy và xác nhận xóa; đổi độ đậm cho nét mới.
- Xoay máy khi đang viết và chuyển tab rồi quay lại: nét đã hoàn thành vẫn còn.
- Trắc nghiệm: chọn đúng/sai, thử lại, hoàn thành cả ba câu, làm lại.
- Ghép câu: chọn sai thứ tự, bỏ thẻ, xếp lại và kiểm tra.
- Kiểm tra màn hình nhỏ, tablet ngang/dọc, bàn phím khi nhập mã lớp và cỡ chữ hệ thống lớn.

Khi nối API, bổ sung data repository/ViewModel theo từng luồng và thay fixture bằng dữ liệu thật; Android gọi Spring Boot, không truy cập MySQL trực tiếp.
