# Đặc tả giao diện Android – Học Chữ Lớp 1

Tài liệu định hướng giao diện Android bằng Kotlin và Jetpack Compose. Học sinh
và phụ huynh sử dụng Android; giáo viên và quản trị viên sử dụng website.
Phải đối chiếu [quy tắc Android](android/AGENTS.md) và các đặc tả trong
`android/docs/` khi triển khai; không dùng bản web cũ làm nguồn chức năng hiện tại.

## 1. Phạm vi và nguyên tắc

- Chỉ xây ứng dụng Android mới; giữ nguyên backend Spring Boot, API và cơ sở dữ liệu MySQL.
- Android gọi backend qua HTTP API. Không kết nối trực tiếp tới MySQL hoặc mở WebSocket.
- Website chỉ có khu vực giáo viên/admin và biểu mẫu đăng ký/OTP chung.
- Giao diện ưu tiên học sinh lớp 1, thao tác bằng ngón tay hoặc bút cảm ứng, tiếng Việt có dấu, ít chữ trên mỗi màn hình.
- Hỗ trợ điện thoại dọc và máy tính bảng ngang/dọc. Trên tablet, tận dụng không gian rộng bằng bố cục hai cột; không phóng lớn nguyên giao diện điện thoại.
- Thiết kế mới có thể lấy cảm hứng từ ứng dụng học tập vui vẻ, nhiều phản hồi tích cực và tiến trình học rõ ràng. Tạo biểu tượng, nhân vật, hình minh họa và màu thương hiệu riêng; không sao chép tài sản thương hiệu của ứng dụng khác.

## 2. Vai trò và điều hướng

Ứng dụng hiện có bốn vai trò: Học sinh, Giáo viên, Phụ huynh và Quản trị viên. Sau đăng nhập, chuyển tới khu vực đúng vai trò.

- **Điện thoại:** dùng thanh điều hướng dưới cho các mục chính; các thao tác phụ đặt trong trang hồ sơ hoặc menu.
- **Máy tính bảng:** dùng thanh điều hướng bên hoặc rail điều hướng; giữ nội dung chính ở vùng rộng, có thể hiển thị danh sách và chi tiết cạnh nhau.
- Luôn hiển thị tên người dùng hoặc lời chào ở trang chủ; đặt đăng xuất trong menu tài khoản.
- Mọi màn hình có trạng thái tải, lỗi có thể thử lại, dữ liệu rỗng và thao tác thành công.

## 3. Màn hình dùng chung

### Đăng nhập

- Tên ứng dụng “Học Chữ Lớp 1”, lời chào thân thiện, trường tên đăng nhập/email và mật khẩu, nút đăng nhập, liên kết đăng ký.
- Sau đăng nhập, điều hướng theo vai trò trả về từ backend.
- Thông báo lỗi ngắn, dễ hiểu; không xóa nội dung người dùng đã nhập khi lỗi.

### Đăng ký và OTP

- Biểu mẫu đăng ký có thông tin tài khoản và chọn một vai trò: Học sinh, Phụ huynh hoặc Giáo viên.
- Sau khi gửi đăng ký, chuyển sang bước nhập OTP; có nút quay lại sửa thông tin.
- Hiển thị trạng thái đang gửi, xác thực thành công hoặc lỗi xác thực.

## 4. Giao diện Học sinh

Thanh điều hướng hiện tại có bốn mục: Trang chủ, Đọc chữ, Tập viết và Bài tập.

### Trang chủ học sinh

- Chào học sinh bằng tên.
- Ba thẻ hành động nổi bật: **Đọc chữ cái**, **Tập viết chữ**, **Vào lớp học**.
- Mỗi thẻ có biểu tượng dễ nhận biết, màu riêng và vùng chạm lớn.
- Có thể bổ sung tiến độ/động viên khi API có dữ liệu; không hiển thị điểm hoặc chuỗi ngày giả lập.

### Đọc chữ cái

- Hiển thị chữ cái đang học ở kích thước lớn, dễ nhìn.
- Có nút nghe phát âm chữ và nút nghe từ ví dụ; hiển thị hình minh họa/từ ví dụ khi có học liệu.
- Có điều hướng chữ trước/sau và phản hồi khi đã học hết danh sách.
- Trên tablet: chữ cái và hình minh họa nằm cạnh nhau; trên điện thoại xếp dọc.

### Tập viết chữ

- Chọn chữ cần luyện; hiển thị mẫu chữ và bảng ô ly làm vùng vẽ chính.
- Các thao tác rõ ràng: **Xóa**, **Viết lại**, **Nộp bài**.
- Sau khi nộp, hiển thị kết quả và lời khích lệ; cho phép luyện lại.
- Vùng vẽ phải nhận pointer/touch/stylus, không cuộn trang khi đang viết; có nét mượt, undo/clear, giữ đúng tỷ lệ khi xoay màn hình.
- Trên tablet, ưu tiên bố cục ngang: bảng viết lớn bên trái, mẫu chữ và công cụ bên phải. Trên điện thoại, bố cục dọc và giữ vùng vẽ đủ lớn.
- Android ghi nhận lượt luyện viết, chưa chấm độ chính xác nét chữ. Bài viết được giao chờ giáo viên chấm; không tái sử dụng điểm minh họa từ bộ so khớp web cũ.

### Lớp học và bài tập

- Hiển thị lớp đã tham gia, bài tập, hạn nộp và trạng thái bài nộp.
- Hỗ trợ hai loại nội dung hiện có: bài viết (`WRITING`) và trắc nghiệm (`QUIZ`).
- Bài trắc nghiệm hiển thị câu hỏi, hình/emoji nếu có, các đáp án lớn dễ chạm; sau đó cho biết kết quả.
- Có khu vực nhập mã tham gia lớp và hiển thị mã liên kết phụ huynh của học sinh.
- Không tạo mục “Game ghép chữ” riêng trong bản đầu vì chức năng game độc lập chưa được triển khai trong web hiện tại.

## 5. Giao diện Giáo viên

### Tổng quan và quản lý lớp

- Hiển thị lớp của giáo viên, mã mời, số học sinh và thống kê hoạt động/bài nộp khi backend có dữ liệu.
- Tạo, sửa, xóa lớp; sao chép mã mời; xem danh sách học sinh; thêm hoặc xóa học sinh.
- Điện thoại dùng danh sách lớp rồi mở chi tiết; tablet có thể dùng danh sách bên trái và chi tiết lớp bên phải.

### Giao bài và chấm bài

- Danh sách bài tập theo lớp, loại bài và hạn nộp.
- Tạo/sửa/xóa bài tập viết hoặc trắc nghiệm.
- Mở danh sách bài nộp, xem câu trả lời/bài viết, nhập điểm và nhận xét.
- Các thao tác xóa hoặc chấm điểm cần xác nhận/hiển thị kết quả rõ ràng.

### Quản lý học liệu

- Danh sách chữ cái và học liệu; thêm/sửa/xóa nội dung, ảnh, audio và từ ví dụ theo API hiện tại.
- Trên điện thoại dùng màn hình riêng cho danh sách và biểu mẫu; tablet có thể hiển thị danh sách cạnh biểu mẫu.

## 6. Giao diện Phụ huynh

- Liên kết con bằng mã do học sinh cung cấp; hiển thị các học sinh đã liên kết.
- Giữ ba mục Hôm nay, Tiến độ, Gia đình theo [đặc tả phụ huynh](android/docs/PARENT.md).
- Chọn con và ngày để xem lượt đọc, viết, câu luyện tập, bài nộp, điểm/nhận xét thật từ backend.
- Hiển thị tiến độ 29 chữ và hoạt động bảy ngày; không tạo điểm viết hoặc số phút học giả.
- Không khôi phục khu vực phụ huynh trên website khi mở rộng báo cáo Android.

## 7. Giao diện Quản trị viên

- Tổng quan số liệu hệ thống.
- Quản lý người dùng: xem, tạo, sửa thông tin, đổi vai trò, trạng thái và quyền theo các API hiện tại.
- Quản lý học liệu chữ cái.
- Giao diện web có các thẻ thống kê và biểu đồ; trên điện thoại xếp thành một cột, trên tablet có thể dùng lưới hai cột.
- Không đưa chức năng xuất Excel/PDF vào phạm vi Android ban đầu vì chưa có luồng xuất tương ứng trong backend hiện tại.

## 8. Phạm vi trao đổi

Không triển khai chat/WebSocket trên Android, website hoặc backend. Phụ huynh
theo dõi hoạt động và nhận xét qua HTTP API; giữ lịch sử dữ liệu cũ trong DB.

## 9. Hướng thiết kế

- Thân thiện với trẻ nhỏ nhưng không quá nhiều chi tiết trang trí.
- Nền sáng, tương phản tốt, chữ lớn; màu xanh có thể dùng cho điều hướng/chung, mỗi khu vực học có màu nhấn riêng.
- Thẻ bo góc, trạng thái tiến độ trực quan, chuyển động ngắn khi hoàn thành thao tác; cho phép tắt âm thanh/hiệu ứng.
- Nút chính cao tối thiểu khoảng 48dp; khoảng cách giữa các nút đủ để tránh chạm nhầm.
- Dùng icon kèm nhãn, không dùng màu làm tín hiệu duy nhất.
- Bảng viết là màn hình trọng tâm: ưu tiên diện tích vẽ, độ trễ thấp, đường kẻ ô ly rõ nhưng nhạt hơn nét viết.

## 10. Đề xuất cấu trúc Android

- `app`: Activity, điều hướng, theme và cấu hình ứng dụng.
- `core/network`: cấu hình HTTP, JWT interceptor, DTO API.
- `core/design`: màu sắc, typography, nút, thẻ và component dùng chung.
- `feature/auth`: đăng nhập, đăng ký, OTP.
- `feature/student`: trang chủ, chữ cái, lớp/bài tập.
- `feature/practice`: bảng viết, mẫu chữ, kết quả và lưu lịch sử.
- `feature/parent`: theo dõi hoạt động và liên kết con. Giáo viên/admin tiếp tục dùng website.

Giao diện không gọi database. Các màn hình gọi repository của Android; repository gọi API Spring Boot. Token được lưu bằng giải pháp lưu trữ an toàn của Android thay vì `localStorage`.

## 11. Prompt ngắn dùng trong Google Stitch

> Thiết kế giao diện ứng dụng Android tiếng Việt “Học Chữ Lớp 1” cho học sinh lớp 1 và phụ huynh. Giáo viên và quản trị viên sử dụng website. Đây là app native ưu tiên điện thoại và máy tính bảng, không phải trang web thu nhỏ. Dùng phong cách vui tươi, thân thiện, dễ đọc, nút lớn, tương phản tốt, thẻ bo góc và tiến trình học trực quan; thiết kế nhận diện riêng, không sao chép logo, nhân vật hay tài sản của ứng dụng khác. Tạo trước luồng học sinh gồm đăng nhập, trang chủ, đọc chữ cái, luyện viết trên bảng ô ly và bài tập trắc nghiệm/bài viết. Bảng luyện viết phải có vùng vẽ lớn, mẫu chữ, xóa, viết lại và nộp bài; hỗ trợ thao tác ngón tay và bút cảm ứng. Tạo phương án bố cục riêng cho điện thoại dọc và tablet ngang. Hiển thị trạng thái tải, rỗng, lỗi và hoàn thành. Dữ liệu sẽ được cung cấp qua API backend hiện có.

## 12. Nguồn đối chiếu trong mã hiện tại

- Điều hướng web giáo viên/admin: `frontend/src/App.tsx`, `frontend/src/components/Layout.tsx`.
- Đăng nhập: `frontend/src/pages/Auth/Login.tsx`, `frontend/src/pages/Auth/Register.tsx`.
- Học sinh Android: `android/app/src/main/java/com/hocchu/mobile/ui/StudentApp.kt`.
- Giáo viên: `frontend/src/pages/Teacher/`.
- Phụ huynh Android: `android/app/src/main/java/com/hocchu/mobile/ui/ParentApp.kt`.
- Quản trị: `frontend/src/pages/Admin/Dashboard.tsx`.
