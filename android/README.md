# Vui Học Lớp 1 — Android

Ứng dụng **native Kotlin + Jetpack Compose**, dựng lại khu vực học sinh từ bộ thiết kế Stitch do người dùng cung cấp. Website và backend hiện có tiếp tục nằm ở `../frontend` và `../backend`.

## Phạm vi bản giao diện

- Trang chủ: thẻ tự luyện và hành trình 29 chữ theo thứ tự. Mỗi chữ: đọc theo → viết 2 lượt (có thể luyện thêm) → điền 3 từ → ghép 3 câu mẫu. Tiến độ lưu trên thiết bị, hoàn thành mới mở chữ kế tiếp; bài đã học có thể ôn lại. Xem [luồng bài học và lưu tiến độ](docs/LETTER_JOURNEY_VI.md).
- Đọc chữ: 29 chữ cái, chữ hoa/thường, từ ví dụ, đọc bằng TextToSpeech tiếng Việt trên thiết bị.
- Tập viết: 29 mẫu chữ thường cỡ vừa từ HP001, căn theo chân chữ và tỉ lệ ô li; dấu phụ và đuôi chữ được chừa chỗ riêng. Có chấm đặt bút, hướng khởi đầu và hộp xem mẫu. Nét vẽ bằng ngón tay/bút, chọn độ đậm, hoàn tác, xác nhận xóa. Tọa độ chuẩn hóa và lưu trạng thái khi xoay máy/chuyển tab. Đổi chữ sẽ bắt đầu bảng viết mới. Chi tiết và nguồn đối chiếu: [Mẫu chữ và ô ly](docs/COPYBOOK_VI.md).
- Lớp học: lớp minh họa, nhập mã lớp, các thẻ bài tập.
- Trắc nghiệm: chọn đáp án, kiểm tra, thử lại, tiếp tục và làm lại.
- Ghép câu: chọn/bỏ thẻ từ, nghe câu mẫu, kiểm tra thứ tự.
- Điện thoại dùng thanh điều hướng dưới; từ 840dp dùng navigation rail. Nội dung đọc chữ và tập viết chuyển sang hai cột khi đủ rộng.

**Bản này chưa nối backend, chưa có đăng nhập, không tham gia lớp thật, không gửi/lưu bài lên máy chủ và không chấm điểm viết.** `DemoContent.kt` và `LetterCurriculum.kt` chứa nội dung minh họa. Hành trình lưu các hoạt động đã hoàn thành trên thiết bị; chưa nhận dạng phát âm hoặc đánh giá nét viết. Không cộng sao, tạo chuỗi ngày học hoặc giả lập điểm viết. Đồng bộ tài khoản, chụp ảnh nộp bài và gửi tiến độ lên máy chủ nằm ngoài phạm vi.

## Mở bằng Android Studio

1. Chọn **Open**, mở thư mục `android` này (không chọn thư mục gốc chứa backend/frontend).
2. Trong **Settings → Build, Execution, Deployment → Build Tools → Gradle**, chọn **Gradle JDK = Embedded JDK / jbr-21** đi kèm Studio. Không dùng Java 8 mặc định trên máy.
3. Dùng Gradle Wrapper đã kèm theo và đợi Gradle Sync hoàn tất.
4. SDK Manager cần **Android SDK Platform 36.1**, **Build-Tools 36.0.0** và Platform-Tools. Các gói này đã có trên máy ở thời điểm cấu hình.
5. Device Manager → Create Virtual Device: chọn một điện thoại và một tablet; hoặc kết nối thiết bị Android thật đã bật USB debugging. Chọn cấu hình `app` rồi Run.

`local.properties` chứa đường dẫn SDK của từng máy, được Git bỏ qua. Android Studio có thể tạo lại file này khi mở dự án. Nếu cần thiết lập thủ công:

```properties
sdk.dir=C\:/Users/ASUS/AppData/Local/Android/Sdk
```

Toolchain cố định: Android Gradle Plugin **9.1.1**, Gradle **9.3.1**, Compose compiler plugin **2.2.10**. AGP 9 có Kotlin tích hợp, vì vậy không thêm plugin `org.jetbrains.kotlin.android`. API tối thiểu **26** (Android 8), target **36**.

## Build trên PowerShell

Chạy trong thư mục `android`:

```powershell
$env:JAVA_HOME = 'D:\Programs\Android\Android Studio1\jbr'
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Preview điện thoại và tablet có sẵn ở cuối `StudentApp.kt`; mở file trong Studio và chọn **Split / Design**. Để kiểm tra viết tay, cần Run trên máy ảo hoặc thiết bị thật. Nghe đọc cần thiết bị có engine TTS và dữ liệu giọng tiếng Việt.

## Cấu trúc mã

```text
app/src/main/java/com/hocchu/mobile/
  MainActivity.kt
  data/DemoContent.kt
  ui/DesignSystem.kt       Màu, font, nút, thẻ, nhân vật vẽ bằng code
  ui/StudentApp.kt         Khung giao diện, điều hướng, trạng thái màn hình
  ui/LearningScreens.kt    Trang chủ, đọc chữ, lớp học
  ui/WritingScreen.kt      Bảng ô ly và dữ liệu nét vẽ
  ui/CopybookForm.kt       Tỉ lệ, chân chữ và dữ liệu 29 mẫu chữ thường
  ui/CopybookPaper.kt      Giấy ô ly, mẫu chữ và điểm bắt đầu
  ui/ExerciseScreens.kt    Trắc nghiệm và ghép câu
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
