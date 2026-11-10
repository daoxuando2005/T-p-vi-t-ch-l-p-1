# Quy tắc làm việc với Android

Áp dụng cho toàn bộ thư mục `android/`. Đọc file này trước khi sửa mã, cấu hình
hoặc tài liệu Android. Các quy tắc phải được tuân theo trong phạm vi công việc,
phù hợp với chỉ dẫn cấp cao hơn và yêu cầu trực tiếp của người dùng.

## Tài liệu bắt buộc

- Luôn đọc [kiến trúc và phạm vi](docs/ARCHITECTURE.md).
- Sửa hành trình, học liệu hoặc tiến độ: đọc
  [hành trình học](docs/LEARNING_JOURNEY.md).
- Sửa mẫu chữ, bảng ô li hoặc thao tác viết: đọc
  [tập viết](docs/HANDWRITING.md).
- Trước khi xác nhận hoàn thành: chọn kiểm tra phù hợp theo
  [quy định kiểm thử](docs/TESTING.md).

## Nguyên tắc bắt buộc

- Giữ tiến độ hiện có trong SharedPreferences `learning` trên thiết bị. Không
  xóa dữ liệu thật khi kiểm thử hoặc tự gán dữ liệu local cho tài khoản khi nối API.
- Nhánh `main` hiện chưa tích hợp đăng nhập/backend. Không sao chép mô tả tính
  năng từ nhánh khác như thể mã tương ứng đã có trên nhánh đang sửa.
- Không trình bày học liệu/lớp minh họa thành dữ liệu thật; không tạo điểm viết,
  kết quả nhận dạng phát âm hoặc trạng thái đồng bộ khi chưa có triển khai.
- Không đưa mật khẩu, JWT, khóa máy chủ, cấu hình máy cá nhân hoặc log vào Git.
- Khi yêu cầu mới thay đổi một quy tắc, sửa quy tắc liên quan trong cùng thay đổi.
  Không viết tài liệu như thể tính năng dự kiến đã được triển khai.
- Tên nhánh và commit phải mô tả chức năng, ví dụ `feature/student-login`,
  `fix/writing-canvas`, `docs/android-guidelines`. Không đặt tên theo công cụ thực hiện.

## Quy ước tài liệu

- `docs/` chứa đặc tả và quy tắc lâu dài, viết tiếng Việt, tên file tiếng Anh
  dạng `UPPER_SNAKE_CASE.md`. `README.md` hướng dẫn mở/chạy; `AGENTS.md` là điểm vào
  của bộ quy tắc. Dùng liên kết tương đối giữa các tài liệu.
- Không ghi kết quả thực hiện, nhật ký công việc, số bài kiểm thử đã đạt,
  ngày hoàn thành, ảnh kiểm chứng hoặc thông tin thiết bị cá nhân vào các file này.
  Báo kết quả trong phản hồi hoặc phần mô tả commit/PR khi có yêu cầu.
- Giữ các bước kiểm tra và tiêu chí cần đạt; không thay chúng bằng báo cáo đã đạt.
- Không tạo thêm file Markdown báo cáo cho mỗi lần sửa. Đổi tên tài liệu phải
  cập nhật tất cả liên kết và chú thích mã nguồn có tham chiếu đến tên cũ.
