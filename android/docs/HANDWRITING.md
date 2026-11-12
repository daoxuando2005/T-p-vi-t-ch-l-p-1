# Quy tắc mẫu chữ, ô li và thao tác tập viết

Áp dụng cho `WritingScreen.kt`, `CopybookForm.kt`, `CopybookPaper.kt` và hoạt động
viết trong hành trình. Tuân theo [kiến trúc](ARCHITECTURE.md) và
[hành trình học](LEARNING_JOURNEY.md). Không ghi kết quả thực hiện ở đây.

## Phạm vi mẫu chữ

Giữ đủ 29 mẫu chữ thường cỡ vừa từ font HP001 đã đóng gói trong
`app/src/main/res/font/handwriting.ttf`. Tỉ lệ được phóng to trên màn hình;
không cam kết một ô màn hình có kích thước vật lý bằng ô trên vở giấy.

| Nhóm chữ | Trên chân chữ | Dưới chân chữ | Tổng thân chữ |
| --- | --- | --- | --- |
| a, ă, â, c, e, ê, i, m, n, o, ô, ơ, u, ư, v, x | 2 li | 0 | 2 li |
| r, s | 2,5 li | 0 | 2,5 li |
| t | 3 li | 0 | 3 li |
| d, đ | 4 li | 0 | 4 li |
| b, h, k, l | 5 li | 0 | 5 li |
| p, q | 2 li | 2 li | 4 li |
| g, y | 2 li | 3 li | 5 li |

Dấu phụ và chấm i không phải mốc đo chiều cao thân. Không co toàn bộ hộp chữ
có dấu về cùng chiều cao thân chữ không dấu. Chiều rộng và đường cong lấy từ HP001.

## Hình học và giấy

- `CopybookForm.kt` phải giữ dữ liệu riêng từng chữ: chiều cao trên/dưới dòng,
  chữ tham chiếu, gợi ý đặt bút/hướng đầu và thành phần nét.
- Lấy outline bằng `Paint.getTextPath`. Dùng cực trị từ `Path.approximate` cho
  phép đo thực; không dùng hộp dòng của `Text` hoặc `Path.computeBounds` để thay
  phép đo chiều cao vì có thể bao cả điểm điều khiển Bezier.
- Chuẩn hóa riêng trên/dưới chân chữ. Đo â theo a; ô/ơ theo o; ê theo e; ư theo u;
  i theo u để loại chấm. Giữ lỗ rỗng khi ghép contour, không tô kín a/o/e.
- Giữ nét xiên vào bổ sung của r từ chân dòng. Không suy luận thứ tự viết từ thứ
  tự contour của font: contour chỉ biểu diễn đường viền.
- Giấy có hệ tọa độ 10 × 10 ô; chân chữ ở hàng 6, chừa chỗ cho thân cao 5 li,
  đuôi 3 li và lề. Số đường 1–6 tính từ dưới lên; ngoặc đo đúng thân chữ.
- Mẫu và nét dùng chung hệ tọa độ, độc lập font scale. Tọa độ nét được chuẩn hóa;
  độ dày bút và nét chấm quy theo ô li để giữ tỉ lệ khi đổi kích thước.

## Thao tác viết

- Công cụ chọn độ đậm, hoàn tác và xóa phải ở vị trí dễ bấm, không buộc cuộn
  khỏi vùng viết để thao tác. Thay độ đậm chỉ tác động nét mới.
- Giấy phải nhận vẽ bằng ngón tay/bút; không để thao tác vẽ kéo cuộn trang.
  Giữ nét đã hoàn thành và lựa chọn công cụ khi phục hồi SavedState/đổi tab.
- Xóa toàn bộ cần xác nhận, hủy xóa phải giữ nguyên bản vẽ. Hoàn tác chỉ bỏ nét cuối.
- Chấm/mũi tên gợi đặt bút tự ẩn khi viết. Mở rồi đóng hộp xem mẫu không xóa nét
  hoặc đánh dấu hoàn thành lượt viết.
- Khi hoàn thành, dùng hộp phản hồi với lời động viên, nút viết lại và tiếp tục.
  Có thể dùng TTS khi âm thanh bật; không chặn thao tác nếu không có giọng đọc.
- Nút tiếp tục phải theo đúng ngữ cảnh: lượt viết/học tiếp trong hành trình,
  hoặc chữ kế tiếp khi tự luyện. Không làm mất tiến độ đã xác nhận.
- Khi hoàn thành, chấm độ bám mẫu ngoài main thread rồi hiển thị kết quả và
  hướng dẫn. Chỉ dùng kết quả nếu bản nét/chữ vẫn khớp lần yêu cầu; sửa/xóa nét
  không được nhận điểm trả về muộn của bản trước.

## Đánh giá tự động

- `TraceScoring` và `HandwritingAssessment` dùng cùng hình học HP001 và hệ ô li.
  Chuẩn hóa về 256 × 256 pixel, tách mực tối khỏi nền, cho sai số 5 pixel.
- Độ phủ là phần mẫu nằm gần mực; độ chính xác vị trí là phần mực nằm gần mẫu.
  Điểm 0–100 lấy trung bình điều hòa hai giá trị, hạn chế tô kín bảng để lấy điểm.
  Thành phần rời của mẫu có từ 5 pixel và được phủ dưới 45% được coi là thiếu;
  khi thiếu thành phần, giảm một nửa điểm để xét dấu phụ/chấm i.
- Đây là phép so hình học với mẫu chữ đang luyện, không phải OCR, đánh giá
  thứ tự/hướng từng nét, lực bút hoặc chứng nhận viết đúng theo sư phạm.
  Không dùng điểm này để tự thay đổi điều kiện mở khóa hành trình.
- Tự luyện hiển thị thang 100. Bài lớp học gửi PNG mực thật, backend chấm lại
  từng chữ, lấy trung bình và đổi sang thang 10; không nhận điểm do client gửi.
- Backend giữ `autoScore`, `gradeSource=AUTO`, dữ liệu độ phủ/vị trí/thiếu nét
  trong `assessmentData`. Cô chấm lại thì `gradeSource=TEACHER`, giữ điểm tự động
  ban đầu và ảnh bài làm. Bài cũ không có dữ liệu đánh giá không được tạo điểm giả.
- Giữ đồng nhất font ở Android và backend. Khi đổi hình học, cập nhật fixture
  đối chiếu và kiểm tra cả chữ dấu, chữ có đuôi, mẫu trắng, nét lệch và tô kín.

## Nguồn và giới hạn

Các tỉ lệ là quy ước triển khai của ứng dụng, có tài liệu đối chiếu:

- [Thông tin Quyết định 31/2002/QĐ-BGD&ĐT tại Trường Tiểu học Nguyễn Văn Kịp](https://thnguyenvankip.hcm.edu.vn/quyet-dinh/mau-chu-viet/vbctmb/143786/53695).
- [Tài liệu tập huấn Tiếng Việt 1 Cánh Diều, 2020](https://medialib.qlgd.edu.vn/Uploads/THU_VIEN/shn/1/158/UserFiles/tai-lieu-tap-huan-mon-tieng-viet-sach-canh-dieu-0035868b-8918-489e-8375-df954d4f6350.pdf), phần cỡ chữ và đường kẻ ở trang 10–11.

Không gọi triển khai HP001 này là chứng nhận của Bộ GD&ĐT. Hình dáng font,
vị trí đặt bút và hướng dẫn sư phạm cần giáo viên tiểu học duyệt trước khi dùng
làm tài liệu giảng dạy chính thức. Không nâng gợi ý đặt bút thành thứ tự nét đã được xác nhận.

## Tiêu chí kiểm tra

Kiểm tra đủ 29 chữ, chiều cao thân, dấu phụ, đuôi chữ, lỗ rỗng, lề, công cụ và
giữ nét khi đổi bố cục theo [TESTING.md](TESTING.md). Ảnh kiểm chứng là đầu ra kiểm
thử, không chèn vào tài liệu quy tắc hoặc tự đưa vào Git.
