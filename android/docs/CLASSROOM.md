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
| `DELETE /api/student/class/leave/{id}` | Rời lớp sau xác nhận; giữ bài nộp và lịch sử học |
| `GET /api/student/assignments` | Bài của các lớp đã tham gia, nội dung, hạn nộp, trạng thái, điểm và nhận xét |
| `POST /api/student/assignments/{id}/submit` | JSON có `submissionData` là một chuỗi JSON; chỉ xác nhận hoàn thành sau phản hồi thành công |
| `GET /api/student/assignments/{id}/submission` | Xem lại bài gần nhất của chính học sinh, ảnh/đáp án/điểm/nhận xét |

Backend phải kiểm tra học sinh thuộc lớp của bài trước khi lưu bài nộp. Phân biệt
401 (phiên không hợp lệ, quay lại đăng nhập) với 403 (không có quyền với lớp/bài).
Lỗi HTTP, JSON hỏng hoặc `success=false` không được biến thành thông báo đã nộp.

## Màn lớp học

- Dùng dữ liệu server; không thay lỗi mạng bằng “Lớp 1A”, “Cô Mai” hoặc bài mẫu.
- Tên lớp và nút tham gia cùng hàng. Ô nhập mã chỉ mở khi bấm; hỗ trợ nhiều lớp,
  chọn lớp, tải lại, trạng thái đang tải/lỗi và trường hợp chưa có lớp/bài tập.
- Danh sách lớp đã tham gia thu gọn mặc định. Bấm tên lớp hiện tại để mở, chọn
  một lớp thì đóng danh sách; chỉ mở một trong hai phần chọn lớp và nhập mã.
  Mũi tên và mô tả trạng thái phải cho biết phần đang mở/đóng. Back đóng phần
  đang mở trước khi quay lại nhóm bài tập. Giữ trạng thái khi phục hồi SavedState.
- Tải lại/rời lớp nằm trong phần chọn lớp mở rộng; khi chưa có lớp vẫn có tải lại.
  Cỡ chữ lớn hoặc màn hẹp cho phép tên lớp và nút tham gia xếp thành hai hàng.
  Dùng màu xanh dương thay màu chọn tím mặc định; tên lớp/thẻ bài tập phải đậm,
  tương phản rõ, cùng font và bảng màu với trang chủ. Không dùng màu để thay nhãn trạng thái.
- Giữ ba thẻ: Tập viết chữ (xanh dương), Đố vui ghép chữ (xanh lá), Ghép câu (cam).
  Bấm thẻ xem bài thuộc mục đó trong lớp đang chọn; Back quay lại ba thẻ trước
  khi rời tab. Đổi lớp phải bỏ lựa chọn mục của lớp trước.
- Huy hiệu đếm bài chưa nộp từ dữ liệu thật: từng mục theo lớp đang chọn, từng
  lớp và tổng trên tab Lớp học. Bài đã nộp không còn trong số đếm; mở xem bài
  chưa nộp không xóa huy hiệu. Tải lại khi quay lại app, không dùng thông báo đẩy.
- Phân mục theo contentType, không đoán từ tên bài: WRITING, QUIZ, SENTENCE. Dữ liệu loại
  khác vẫn phải hiện với thông báo chưa hỗ trợ, không giả nội dung/nút làm bài.
- Hiển thị tiêu đề, mô tả, hạn nộp, trạng thái, điểm và nhận xét nếu có. Hạn nộp
  chỉ hiển thị; backend hiện cho phép nộp sau hạn theo nghiệp vụ có sẵn.
- Không hiển thị “đã có điểm” khi `score=null`; dữ liệu bài cũ có thể chưa được chấm.
  Bài đã nộp không tiếp tục mở như bài chưa làm trong giao diện hiện tại.

## Nội dung và chấm bài

- `QUIZ`: `contentData` là mảng câu hỏi có `question`, `options` và `answer`.
  App hiển thị câu hỏi/lựa chọn, hỗ trợ TTS; gửi `{"answers":[...]}` trong
  `submissionData`. Không tự quyết định điểm ở client; backend kiểm tra đủ đáp án
  hợp lệ và tính trên thang 10.
- Nếu có `animalName`, dùng làm nội dung TTS hỗ trợ. `imageUrl`/`audioUrl` được
  hiển thị/phát từ media công khai; tải lỗi phải có phản hồi và TTS dự phòng.
- `WRITING`: hỗ trợ chữ đơn dạng text và JSON `{"letter":"A"}` hoặc
  `{"letters":["A","B"],"targetScore":70}`. Chỉ nhận chữ thuộc bảng 29 chữ tiếng Việt.
- Dùng lại `WritingBoard` và mẫu ô li hiện có; giữ nét từng chữ khi đổi chữ.
  Mỗi chữ được giao phải có nét trước khi bật nộp bài. Thay độ đậm, hoàn tác và
  xóa phải thao tác được ngay cạnh bảng; xóa cần xác nhận.
- Xuất PNG thật từ nét người dùng, gửi mảng `[{"letter":"A","image":"data:image/png;base64,..."}]`.
  Không gửi điểm client hoặc dùng `targetScore` như kết quả. “Kiểm tra nét” trên
  app là xem trước; backend tính lại từ ảnh theo [HANDWRITING.md](HANDWRITING.md).
- Bài viết mới có điểm bám mẫu tự động; app hiển thị nguồn điểm, độ phủ/vị trí
  và nhận xét từng chữ. Cô được sửa điểm 0–10 và nhận xét; giữ `autoScore` ban đầu
  khi chuyển `gradeSource` sang `TEACHER`. Không chấm lại dữ liệu cũ khi khởi động.
- `SENTENCE`: `contentData` là `{"sentences":[{"prompt":"Ghép câu","words":["Bé","yêu","mẹ."]}]}`.
  Nhận 1–20 câu, mỗi câu 2–12 thẻ từ/cụm từ; thứ tự trong đề là đáp án.
  App đảo thẻ ổn định theo bài, giữ ID vị trí để dùng được từ trùng; cho bỏ thẻ
  hoặc làm lại. Chỉ nộp khi mỗi câu dùng đủ mỗi ID đúng một lần.
- Gửi `{"sentences":[[0,1,2]]}` trong chuỗi `submissionData`. Backend kiểm tra
  hoán vị và so chuỗi từ theo thứ tự mẫu; đổi chỗ hai thẻ có cùng chữ vẫn đúng.
  Điểm bằng tỉ lệ câu khớp mẫu × 10, không đánh giá mọi câu đúng ngữ pháp khác.
- Tổng dữ liệu nộp tối đa 4.000.000 ký tự; cột `submission_data` cần LONGTEXT qua
  migration V10 để không cắt mất bộ ảnh. Nội dung hỏng/loại bài chưa hỗ trợ phải
  có thông báo, không mở bài trống rồi cho nộp.
- Backend kiểm tra ảnh có định dạng PNG/JPEG thực, kích thước không quá 4096 mỗi
  chiều và 8 triệu pixel; bộ ảnh phải khớp đủ chữ được giao, không trùng/sai chữ.
- Khóa bài tập khi nhận bài nộp và dùng transaction READ_COMMITTED để hai yêu cầu
  đồng thời không tạo hai bài. Gửi lại cùng nội dung trả xác nhận cũ, giữ nguyên
  điểm/nhận xét và không gửi thêm thông báo. Bài đã nộp không được đổi nội dung.
- Giáo viên sửa đề phải dùng cùng khóa bài tập. Khi đã có bài nộp, chỉ sửa tiêu
  đề/hướng dẫn/hạn nộp; muốn thay câu hỏi/đáp án thì tạo bài khác để giữ kết quả.
- “Xem bài đã nộp” mở dữ liệu server. Không suy diễn điểm còn null thành 0.
  Đáp án đúng chỉ hiển thị khi đã có điểm; dữ liệu lịch sử không đọc được phải
  có thông báo định dạng cũ. Chấm bài giới hạn 0–10, nhận xét tối đa 4000 ký tự.

## Bản nháp và vòng đời

- `ClassroomViewModel` giữ trạng thái qua đổi cấu hình. Nhóm yêu cầu và ghi nháp
  phải bị hủy khi phiên/tài khoản đổi; không để kết quả cũ cập nhật giao diện tài khoản mới.
- Preferences bản nháp là `<accountPreferences>.classroom`, khóa `assignment.<id>`.
  Dùng SHA-256 của loại/nội dung bài để không áp đáp án bản nháp cũ vào đề đã sửa.
- Lưu lựa chọn, thứ tự ID thẻ từng câu, nét vẽ chuẩn hóa, độ đậm từng nét và chữ đang viết. Đọc/ghi dữ liệu,
  HTTP và xuất ảnh phải chạy ngoài main thread. Nét đang kéo chưa kết thúc không
  phải lượt vẽ đã lưu; báo lỗi nếu không ghi được vào máy.
- Ghi nháp theo thứ tự. Trước khi gửi phải đợi lần ghi còn chờ; chỉ dọn bản nháp
  sau khi backend xác nhận. Nếu mạng lỗi, giữ bài và cho thử lại, không báo hoàn thành.
- Sau khi backend xác nhận, lỗi tải lại/dọn nháp không được mô tả thành lỗi nộp bài.
- Bản nháp vẫn ở trên thiết bị. Bài đã nộp và tiến độ hành trình đã đồng bộ có
  trên server; không gọi đây là đồng bộ nét tự luyện hoặc mọi bản nháp giữa thiết bị.

## Kiểm tra

Thực hiện theo [TESTING.md](TESTING.md): mã lớp, danh sách thật, chọn đáp án,
đủ nét cho từng chữ, phục hồi nháp, đổi tài khoản, lỗi gửi/thử lại, ảnh nhận được
bên giáo viên và điểm/nhận xét trả về. Kiểm tra live phải tạo lớp/bài riêng rồi dọn
đúng dữ liệu thử; không sửa bài hoặc xóa tiến độ thật của người dùng.
