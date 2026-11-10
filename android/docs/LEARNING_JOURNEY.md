# Quy tắc hành trình học chữ và tiến độ

Áp dụng cho học liệu, điều hướng bài học và lưu tiến độ. Phải tuân theo
[kiến trúc](ARCHITECTURE.md) và [phân vùng tài khoản](AUTHENTICATION.md).
Không ghi kết quả thực hiện ở đây.

## Trình tự bài học

Duy trì đủ 29 chữ theo thứ tự: A, Ă, Â, B, C, D, Đ, E, Ê, G, H, I, K, L, M, N,
O, Ô, Ơ, P, Q, R, S, T, U, Ư, V, X, Y. Không dùng bảng 26 chữ tiếng Anh.

| Chặng | Điều kiện tiếp tục |
| --- | --- |
| Đọc | Nghe mẫu rồi xác nhận đã đọc theo; chưa nhận dạng giọng nói |
| Viết | Hoàn thành 2 lượt luyện có nét; được viết thêm sau đó |
| Điền từ | Hoàn thành 3 từ/cụm từ, chọn đúng và tiếp tục từng câu |
| Ghép câu | Hoàn thành 3 câu mẫu bằng các thẻ từ |
| Hoàn thành | Ghi nhận xong bài và mở chữ tiếp theo |

Không coi lượt luyện viết hoặc xác nhận đọc theo là điểm đánh giá năng lực.
Không tăng tiến độ vì chỉ mở màn hình, chọn sai đáp án hoặc lặp sự kiện cũ.

## Quy tắc mở khóa và ôn tập

- A mở mặc định. Chỉ mở chữ sau khi toàn bộ bài trước đó đã hoàn thành;
  kiểm tra ở cả giao diện và `LessonProgressStore`, không chỉ ẩn nút.
- Bài đang học phải có chỉ báo nổi bật/chuyển động; hành trình cuộn được lên xuống.
  Giữ trạng thái cuộn, dấu hoàn thành và chỉ báo khóa có hỗ trợ trợ năng.
- Ôn bài đã hoàn thành dùng tiến độ riêng trong phiên. Không hạ hoặc ghi đè
  tiến độ đã đạt; việc ôn lại không khóa các chữ sau.
- Hết Y phải hiển thị hoàn thành 29 chữ và quay về hành trình, không truy cập chỉ số 29.
- Tab Đọc chữ, Tập viết tự do và bài cô giao trong Lớp học không tự mở khóa hành trình.
- Từ màn đọc, nút luyện viết phải mở đúng chữ đang chọn. Viết tự do cho phép viết
  lại hoặc sang chữ tiếp; đổi chữ bắt đầu bản vẽ mới, không mang nét chữ cũ sang.

## Học liệu

- `DemoContent.kt` giữ danh sách chữ; `LetterCurriculum.kt` cung cấp 3 từ/cụm từ
  và 3 câu mẫu cho mỗi chữ. Câu phải dùng lại từ tương ứng để bé luyện tiếp nội dung vừa học.
- Ô trống phải thay đúng chữ đang học; giữ nguyên dấu thanh/dấu phụ ở vị trí khác.
  Giữ đáp án và thứ tự lựa chọn ổn định khi xoay máy.
- Thẻ ghép câu có ID theo vị trí, không dùng chuỗi từ làm ID vì câu có thể lặp từ.
  Giữ viết hoa đầu câu, dấu câu cuối câu và kiểm tra theo câu mẫu vừa nghe.
- Không mô tả việc so khớp câu mẫu là phân tích mọi câu tiếng Việt đúng ngữ pháp.
- Hỗ trợ nghe từ, nghe câu và nghe thẻ từ. Giữ phản hồi đúng/sai ở hộp thông báo
  với hành động thử lại/tiếp tục; không đưa lời chúc dài vào trang bài tập.
- Học liệu vẫn là minh họa, chưa xếp theo bộ sách giáo khoa cụ thể. Cần duyệt
  sư phạm trước khi dùng như chương trình giảng dạy chính thức.

## Lưu tiến độ và phục hồi

- `LessonProgress.kt` là nơi định nghĩa chuyển chặng `Read → Write → Words →
  Sentences → Done`. Các mốc hiện tại là `writes = 2`, `words = 3`, `sentences = 3`.
- Codec hiện có định dạng `1;<read>;<writes>;<words>;<sentences>`; read là 0/1.
  Khi sửa định dạng phải xử lý phiên bản cũ, không tự bỏ dữ liệu học đã có.
- `LessonProgressStore.kt` nhận SharedPreferences của tài khoản từ `StudentApp`;
  khóa từng chữ là `lesson.v1.<chữ hoa>`. Khóa `letter` chỉ lưu chữ đang chọn của tab đọc.
- Chỉ lưu bước đã xác nhận. Đọc dữ liệu phải giới hạn giá trị theo chặng; dữ liệu
  không hợp lệ phục hồi an toàn, không mở khóa sai hoặc làm app lỗi.
- Không cho ghi lùi các bộ đếm hoặc trạng thái đọc. Không xử lý lại đáp án của
  câu cũ như một lần tăng tiến độ mới.
- Trạng thái lựa chọn, thẻ từ và nét đang vẽ dùng saveable/state holder để giữ khi
  xoay máy và đổi màn hình trong phiên. Bản vẽ dang dở chưa có kho lưu trữ lâu dài riêng.
- Đăng xuất giữ tiến độ tài khoản trên thiết bị. Không tự đồng bộ hoặc gộp vùng dữ
  liệu khi đổi học sinh, tên hiển thị hay backend origin.

## Tiêu chí kiểm tra

Bao phủ đủ 29 bộ nội dung, bài A, bài cuối Y, mở khóa liên tiếp, trả lời sai,
lưu giữa chặng, luyện thêm, ôn tập và chuyển tài khoản. Thực hiện theo
[TESTING.md](TESTING.md), dùng tiến độ riêng cho kiểm thử.
