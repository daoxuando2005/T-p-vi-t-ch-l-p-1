# Quy tắc khu vực phụ huynh và theo dõi hoạt động

Áp dụng cho `family/`, `ui/ParentApp.kt` và API theo dõi tương ứng. Tuân theo
[kiến trúc](ARCHITECTURE.md) và [xác thực](AUTHENTICATION.md).
Không ghi kết quả thực hiện ở đây.

## Vai trò và giao diện

- Tài khoản `ROLE_PARENT` mở khu vực phụ huynh; `ROLE_STUDENT` mở khu vực học sinh.
  Giáo viên và quản trị viên sử dụng website. Không dùng tài khoản học sinh để
  giả lập quyền phụ huynh hoặc trả dữ liệu mẫu khi API thất bại.
- Giữ ba mục Hôm nay, Tiến độ, Gia đình. Dùng theme, Nunito Sans, linh vật và
  thành phần chung với học sinh; vùng bấm tối thiểu 48dp, nhãn và lỗi đọc được.
- Hỗ trợ chọn con đã liên kết, xem ngày trong 90 ngày gần đây, cập nhật thủ công
  và cập nhật khi quay lại app. Bố cục từ 840dp dùng navigation rail.
- Phải có trạng thái đang tải, chưa liên kết, chưa nhận hoạt động, lỗi mạng và
  phiên hết hạn. Khi đổi con/ngày phải bỏ báo cáo cũ trước khi tải báo cáo mới.
- Không hiển thị chat hoặc mở kết nối WebSocket. Website và backend cũng không
  cung cấp chức năng chat. Giữ dữ liệu lịch sử trong DB, không sửa migration đã áp dụng.

## Liên kết và quyền truy cập

- Học sinh lấy mã ở Hồ sơ → Liên kết phụ huynh. Mã ngẫu nhiên phân biệt hoa thường,
  có hiệu lực 30 phút, chỉ sử dụng một lần. Backend chỉ lưu SHA-256 của mã mời.
- Phụ huynh nhập mã tại Gia đình trên Android. Không dùng tên đăng nhập của con
  làm bằng chứng sở hữu; không chuyển học sinh đã liên kết sang phụ huynh khác.
- Mọi API báo cáo phải kiểm tra quan hệ `APPROVED` tại server. Quan hệ `PENDING`,
  ID được sửa thủ công hoặc dữ liệu client đều không cấp quyền xem học sinh.
- Đổi tài khoản hoặc đăng xuất phải hủy yêu cầu, bỏ dữ liệu trẻ và mã mời trong
  bộ nhớ. Không ghi JWT, mã mời hay dữ liệu báo cáo vào SavedState/log.

## Ghi nhận và đồng bộ

- Ghi hoạt động khi xác nhận đọc theo, bấm nghe chữ trong tab đọc, hoàn thành
  lượt viết có nét, trả lời đúng câu từ/câu ghép và hoàn thành bài hành trình.
  Không ghi việc chỉ mở trang thành một lượt học.
- Mỗi hoạt động có UUID và thời điểm UTC lúc thực hiện. Lượt viết thêm/ôn tập
  vẫn được ghi nhận; không đếm lại cùng bản nét khi chỉ đóng/mở thông báo.
- `StudentActivityModel` giữ outbox trên máy theo cùng origin và ID tài khoản
  như tiến độ, ở vùng `.activity`. Không tự chuyển outbox giữa các tài khoản.
- Gửi tối đa 100 hoạt động cùng snapshot 29 chữ mỗi yêu cầu tới
  `POST /api/student/learning/sync`. Chỉ xóa UUID backend xác nhận; lỗi/hết thời
  gian chờ phải giữ lại. Backend khử trùng theo học sinh và UUID, không hạ bộ đếm.
- Khởi động phiên gửi snapshot kể cả khi không có hoạt động mới. Snapshot cũ
  không được tạo thời điểm học giả cho hôm nay. Đây là đồng bộ để báo cáo phụ huynh;
  chưa phải cơ chế khôi phục hành trình từ cloud trên thiết bị mới.

## Ý nghĩa báo cáo

- `GET /api/parent/students/{id}/dashboard?date=YYYY-MM-DD` dùng múi giờ
  `Asia/Ho_Chi_Minh`, bao gồm đầu ngày và loại trừ đầu ngày kế tiếp.
- Hôm nay hiển thị lượt đọc, lượt viết, câu luyện tập, bài đã nộp và dòng hoạt
  động có giờ. Nộp bài/điểm/nhận xét lấy từ dữ liệu thật của backend.
- Tiến độ hiển thị chặng của đủ 29 chữ và số hoạt động trong bảy ngày đến ngày
  được chọn. Tiến độ chữ là tổng đã ghi nhận, không phải ảnh chụp lịch sử từng ngày.
- Không tự tạo điểm viết, số phút học, kết quả nhận dạng giọng nói hay ngày học
  liên tiếp. Chưa nhận hoạt động không chứng minh con chưa học khi thiết bị ngoại tuyến.
- Dòng hoạt động giới hạn 200 mục gần nhất của ngày và phải báo khi bị cắt;
  các bộ đếm ngày vẫn tính toàn bộ hoạt động. Tránh đếm mục hoàn thành bài như
  một câu luyện tập trong tổng hoạt động tuần.

## Kiểm tra bắt buộc

Kiểm tra liên kết một lần/hết hạn, quan hệ chờ, phụ huynh khác, gửi lại UUID,
ba chữ A/Ă/Â, ranh giới ngày Việt Nam, offline/khởi động lại/đổi tài khoản, trạng
thái rỗng/lỗi và điều hướng điện thoại/tablet theo [TESTING.md](TESTING.md).
