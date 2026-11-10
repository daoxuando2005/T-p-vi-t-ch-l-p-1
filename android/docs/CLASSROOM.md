# Quy tắc lớp học, bài cô giao và nộp bài

Áp dụng cho `classroom/`, `ui/ClassroomScreen.kt` và API học sinh liên quan.
Tuân theo [kiến trúc](ARCHITECTURE.md) và [xác thực](AUTHENTICATION.md).
Không ghi kết quả thực hiện ở đây.

## Hợp đồng backend

Mọi yêu cầu học sinh phải dùng Bearer token của phiên đã xác minh. Không gửi ID
học sinh từ client để chọn chủ sở hữu: backend lấy ID từ principal đăng nhập.

| API | Dữ liệu và ý nghĩa |
| --- | --- |
| `GET /api/student/class/joined` | Danh sách lớp thật: ID, tên lớp, tên giáo viên |
| `POST /api/student/class/join?inviteCode=...` | Mã được trim và URL-encode; chỉ báo tham gia thành công khi `success=true` |
| `GET /api/student/assignments` | Bài của các lớp đã tham gia, nội dung, hạn nộp, trạng thái, điểm và nhận xét |
| `POST /api/student/assignments/{id}/submit` | JSON có `submissionData` là một chuỗi JSON; chỉ xác nhận hoàn thành sau phản hồi thành công |

Backend phải kiểm tra học sinh thuộc lớp của bài trước khi lưu bài nộp. Phân biệt
401 (phiên không hợp lệ, quay lại đăng nhập) với 403 (không có quyền với lớp/bài).
Lỗi HTTP, JSON hỏng hoặc `success=false` không được biến thành thông báo đã nộp.

## Màn lớp học

- Dùng dữ liệu server; không thay lỗi mạng bằng “Lớp 1A”, “Cô Mai” hoặc bài mẫu.
- Tên lớp và nút tham gia cùng hàng. Ô nhập mã chỉ mở khi bấm; hỗ trợ nhiều lớp,
  chọn lớp, tải lại, trạng thái đang tải/lỗi và trường hợp chưa có lớp/bài tập.
- Hiển thị tiêu đề, mô tả, hạn nộp, trạng thái, điểm và nhận xét nếu có. Hạn nộp
  chỉ hiển thị; backend hiện cho phép nộp sau hạn theo nghiệp vụ có sẵn.
- Không hiển thị “đã có điểm” khi `score=null`; bài viết đã gửi phải hiện chờ cô chấm.
  Bài đã nộp không tiếp tục mở như bài chưa làm trong giao diện hiện tại.

## Nội dung và chấm bài

- `QUIZ`: `contentData` là mảng câu hỏi có `question`, `options` và `answer`.
  App hiển thị câu hỏi/lựa chọn, hỗ trợ TTS; gửi `{"answers":[...]}` trong
  `submissionData`. Không tự quyết định điểm ở client; backend kiểm tra đủ đáp án
  hợp lệ và tính trên thang 10.
- Nếu có `animalName`, dùng làm nội dung TTS hỗ trợ. Bản hiện tại chưa tải ảnh
  hoặc phát file âm thanh riêng của câu hỏi; không mô tả các media này là đã hỗ trợ.
- `WRITING`: hỗ trợ chữ đơn dạng text và JSON `{"letter":"A"}` hoặc
  `{"letters":["A","B"],"targetScore":70}`. Chỉ nhận chữ thuộc bảng 29 chữ tiếng Việt.
- Dùng lại `WritingBoard` và mẫu ô li hiện có; giữ nét từng chữ khi đổi chữ.
  Mỗi chữ được giao phải có nét trước khi bật nộp bài. Thay độ đậm, hoàn tác và
  xóa phải thao tác được ngay cạnh bảng; xóa cần xác nhận.
- Xuất PNG thật từ nét người dùng, gửi mảng `[{"letter":"A","image":"data:image/png;base64,..."}]`.
  Không giả ảnh, giả điểm hoặc dùng `targetScore` như kết quả tự chấm khi chưa có thuật toán.
- Bài viết lưu với `score=null` và nhận xét chờ giáo viên. Website giáo viên dùng
  API bài nộp và chấm điểm hiện có; app tải lại điểm/nhận xét từ backend.
- Tổng dữ liệu nộp tối đa 4.000.000 ký tự; cột `submission_data` cần LONGTEXT qua
  migration V10 để không cắt mất bộ ảnh. Nội dung hỏng/loại bài chưa hỗ trợ phải
  có thông báo, không mở bài trống rồi cho nộp.

## Bản nháp và vòng đời

- `ClassroomViewModel` giữ trạng thái qua đổi cấu hình. Nhóm yêu cầu và ghi nháp
  phải bị hủy khi phiên/tài khoản đổi; không để kết quả cũ cập nhật giao diện tài khoản mới.
- Preferences bản nháp là `<accountPreferences>.classroom`, khóa `assignment.<id>`.
  Dùng SHA-256 của loại/nội dung bài để không áp đáp án bản nháp cũ vào đề đã sửa.
- Lưu lựa chọn, nét vẽ chuẩn hóa, độ đậm từng nét và chữ đang viết. Đọc/ghi dữ liệu,
  HTTP và xuất ảnh phải chạy ngoài main thread. Nét đang kéo chưa kết thúc không
  phải lượt vẽ đã lưu; báo lỗi nếu không ghi được vào máy.
- Ghi nháp theo thứ tự. Trước khi gửi phải đợi lần ghi còn chờ; chỉ dọn bản nháp
  sau khi backend xác nhận. Nếu mạng lỗi, giữ bài và cho thử lại, không báo hoàn thành.
- Sau khi backend xác nhận, lỗi tải lại/dọn nháp không được mô tả thành lỗi nộp bài.
- Bản nháp và tiến độ hành trình vẫn ở trên thiết bị. Bài đã nộp mới nằm trên
  server; không gọi đây là đồng bộ toàn bộ tiến độ giữa các thiết bị.

## Kiểm tra

Thực hiện theo [TESTING.md](TESTING.md): mã lớp, danh sách thật, chọn đáp án,
đủ nét cho từng chữ, phục hồi nháp, đổi tài khoản, lỗi gửi/thử lại, ảnh nhận được
bên giáo viên và điểm/nhận xét trả về. Kiểm tra live phải tạo lớp/bài riêng rồi dọn
đúng dữ liệu thử; không sửa bài hoặc xóa tiến độ thật của người dùng.
