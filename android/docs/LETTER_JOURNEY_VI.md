# Hành trình học chữ theo bài

## Trải nghiệm

Mỗi chữ có bốn chặng: nghe và xác nhận đọc theo, viết hai lượt, điền ba từ/cụm
từ, ghép ba câu mẫu. Bé có thể viết thêm sau lượt thứ hai hoặc sang đố vui ngay.
Phần viết chỉ ghi nhận lượt luyện; không đánh giá đúng nét. Phần đọc cũng chưa
nhận dạng giọng nói. Từ và câu được kiểm tra đáp án, chọn sai thì thử lại tại câu đó.

Bài đầu A được mở mặc định. Hoàn thành tất cả hoạt động mới mở chữ kế tiếp.
Các bài đã hoàn thành có dấu tích và luôn có thể ôn lại. Ôn tập dùng tiến độ riêng
trong phiên, không xóa hay thay đổi tiến độ đã đạt. Khi hết Y, hiển thị hoàn thành
29 chữ và trở lại hành trình; không truy cập bài thứ 30.

Thanh trên cùng hiển thị bốn chặng và các bộ đếm 0/2, 0/3, 0/3. Có nút quay lại
hành trình giữa bài. Các tab Đọc chữ, Tập viết và bài minh họa trong Lớp học vẫn
là hoạt động tự luyện; không tự động mở khóa hành trình.

## Nội dung

`LetterCurriculum.kt` có 29 bộ nội dung viết riêng. Mỗi bộ gồm ba từ/cụm từ chứa
chữ đang học và ba câu ngắn dùng lại từ tương ứng. Ô trống thay đúng một chữ;
dấu phụ/dấu thanh của các chữ khác giữ nguyên. Các đáp án nhiễu có thứ tự cố
định để tránh đổi vị trí khi xoay máy.

Ghép câu dùng các thẻ có định danh theo vị trí, không dùng chính chuỗi làm ID.
Thẻ đầu có viết hoa, thẻ cuối có dấu câu; đề yêu cầu ghép đúng **câu mẫu vừa nghe**.
Đây là so khớp câu mẫu, không phải bộ phân tích mọi câu tiếng Việt đúng ngữ pháp.
Có tranh, nghe từ, nghe câu và nghe từng thẻ để hỗ trợ những chữ đầu. Nội dung
minh họa này chưa được xếp theo một bộ sách giáo khoa cụ thể; cần duyệt sư phạm
trước khi dùng thành chương trình giảng dạy chính thức.

## Tiến độ và trạng thái

- `LessonProgress.kt`: quy tắc chuyển chặng, kiểm tra chỉ số câu, mở khóa liên tiếp,
  codec có phiên bản và phục hồi an toàn khi bản ghi không hợp lệ.
- `LessonProgressStore.kt`: lưu từng chữ trong SharedPreferences `learning`,
  khóa `lesson.v1.<chữ hoa>`. Tách với khóa `letter` của tab Đọc chữ cũ.
- Tiến độ đã xác nhận được lưu sau mỗi lượt viết và mỗi đáp án đúng được tiếp tục.
  Thoát hoặc mở app lại sẽ đến hoạt động chưa hoàn thành. Lượt viết thêm là tùy chọn.
- Trạng thái hiển thị, thẻ đang chọn và bản vẽ dùng `rememberSaveable`/state holder
  để giữ khi xoay màn hình hoặc quay về hành trình trong cùng phiên. Bản vẽ dang dở
  chưa có lưu trữ lâu dài độc lập với trạng thái Activity.
- Tiến độ hiện lưu trên thiết bị, chưa gắn tài khoản học sinh hoặc đồng bộ backend.
- `LetterLessonScreen.kt`: điều phối bài học; tái dùng bảng viết, mẫu ô ly, thẻ từ và
  hộp phản hồi đã có. `StudentApp.kt` nhận store tùy chọn để kiểm thử với dữ liệu riêng.

## Kiểm tra

- `LessonProgressTest`: bao phủ đủ 29 bộ từ/câu, không nhảy chặng, không tính đáp án
  cũ hai lần, điều kiện mở khóa và mã hóa/khôi phục tiến độ.
- `LetterJourneyTest`: hoàn thành trọn bài A, thử đáp án sai, quay lại giữa bài,
  lượt viết thêm, mở Ă, ôn lại và hoàn thành bài cuối Y. Kiểm thử dùng vùng lưu trữ
  riêng với tên ngẫu nhiên và xóa vùng đó sau khi chạy; không xóa tiến độ thật.
- Các kiểm tra ô ly và tự luyện cũ vẫn nằm trong `CopybookFormTest` và
  `StudentExperienceTest`.

### Kết quả ngày 09/11/2026

- Build APK app và androidTest thành công. Android Lint: 0 lỗi; 6 cảnh báo có sẵn
  về phiên bản thư viện/toolchain, cấu hình backup và cách ghi SharedPreferences cũ.
- `LessonProgressTest`: 4/4 đạt, bao phủ toàn bộ 29 bộ từ/câu và quy tắc tiến độ.
- `LetterJourneyTest`: cả 4 trường hợp đã đạt trên Xiaomi 23127PN0CC / Android 16
  qua hai lượt chạy: trọn bài A và ôn lại; viết thêm/lưu câu đang làm; bài cuối Y;
  từ chối mở bài khóa hoặc ghi đè tiến độ cũ.
- Kiểm tra cũ `copybookReferenceKeepsTheDraftAndShowsLetterSpecificGuidance` và
  `landscapeKeepsBoardAndToolsVisible` đạt.
- `writingKeepsToolsAndDraftAfterRecreation` đã xác nhận lại khi nối đăng nhập:
  kiểm tra khôi phục SavedState bằng `StateRestorationTester` trong composition riêng.
  Kiểm tra bố cục xoay ngang vẫn chạy trên Activity thực. Xem [kết quả xác thực](AUTH_API_VI.md).
- Đã duyệt ảnh ghép câu, hoàn thành bài và hành trình mở khóa. Đã cập nhật APK
  lên điện thoại bằng cài đè, không gỡ app hoặc xóa tiến độ thật.
