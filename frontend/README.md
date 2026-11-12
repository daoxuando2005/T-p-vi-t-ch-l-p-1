# Vui Học — Website

Website React và JavaScript dành cho giáo viên và quản trị viên.
Học sinh và phụ huynh sử dụng ứng dụng Android tại `../android`.
Hai nền tảng dùng chung backend trong `../backend`.

Frontend dùng JavaScript/JSX, không dùng TypeScript hoặc script Python.
Android tiếp tục dùng Kotlin; backend dùng Java. Giữ mã giao diện trong `.jsx`
và các hàm/API dùng chung trong `.js`; build trực tiếp bằng Vite.

Website không cung cấp trang học sinh/phụ huynh. Đăng nhập và khôi phục phiên
chỉ nhận vai trò giáo viên hoặc admin; tài khoản khác nhận thông báo quyền truy
cập, không hiển thị lời nhắc chuyển sang Android. Biểu mẫu đăng ký/OTP chung vẫn tạo được tài
khoản học sinh/phụ huynh vì app chưa có chức năng đăng ký.

## Chạy và kiểm tra

Chạy backend theo [hướng dẫn dự án](../README.md), rồi chạy từ thư mục này:

```sh
npm ci
npm run dev
```

Website local: `http://localhost:5173`. `src/services/api.js` dùng `/api` cùng
origin; Vite proxy đến `http://127.0.0.1:8080`. Có thể đặt biến môi trường
`VITE_BACKEND_TARGET` trước khi khởi động Vite để đổi đích backend. Bản build
cần reverse proxy `/api` hoặc đặt `VITE_API_BASE_URL` khi build. Website không
kết nối trực tiếp MySQL.

```sh
npm run build
npm run lint
npm test
```

`src/main.jsx` là điểm vào; `src/App.jsx` khai báo tuyến theo vai trò.

Để kiểm tra đăng nhập, khôi phục phiên và chuyển trang trong trình duyệt, chạy
web local rồi `npm run test:browser` (Node.js 22.12 trở lên). Bài kiểm tra dùng Edge/Chrome đã cài, profile
tạm và API giả lập, không dùng dữ liệu thật. Có thể đặt `BROWSER_PATH` và
`WEB_TEST_URL` khi trình duyệt hoặc địa chỉ web khác mặc định.

## API và tài sản dùng chung

Không xóa API học sinh/phụ huynh khi bỏ giao diện web: app Android vẫn dùng
chúng. Giáo viên và admin cũng tải học liệu qua `/api/student/alphabet`.
Mẫu chữ Android nằm trong `android/app/src/main/res/font/handwriting.ttf`;
backend dùng cùng font trong `src/main/resources/fonts` để chấm độ bám mẫu từ
ảnh nộp. Website xem điểm, nguồn chấm và cho cô điều chỉnh.

Ảnh/âm thanh từ backend có thể không xuất hiện trong import tĩnh. Trước khi xóa
tài sản trong `public/`, kiểm tra cả URL trong CSS, cấu hình, backend và dữ liệu
học liệu. Giữ các biểu tượng PWA và font đang được tham chiếu.

Upload mới lưu trong thư mục backend được cấu hình bởi `APP_MEDIA_DIRECTORY`
(mặc định `backend/uploads`, không commit), tối đa 8 MB/tệp. URL trả về là
`/api/public/media/...`, dùng được cho Android và web. Các đường dẫn cũ
`/alphabet`, `/audio`, `/example_images` cũng được Vite chuyển tiếp đến backend.

## Khu vực giáo viên

`TeacherLayout` và `teacher.css` dùng màu xanh dương, xanh lá, cam và font Nunito
đóng gói tại `public/fonts` (kèm giấy phép). Giao diện admin dùng khung riêng.
Quyền xem học liệu, quản lý lớp và giao bài vẫn do backend cấp.

- Tổng quan/lớp học: tạo, sửa, xóa lớp sau xác nhận, mã mời, thêm/xóa học sinh.
- Bài tập: chọn lớp, lọc ba loại, tạo/sửa/xóa, xem bài nộp và lưu điểm/nhận xét.
  Tập viết chọn trong 29 chữ; đố vui có đáp án và upload ảnh/âm thanh; ghép câu
  dùng 2–12 thẻ/câu, hỗ trợ dấu `|` để ghép cụm từ thành một thẻ.
- Kho mẫu lưu theo ID tài khoản trên trình duyệt; bấm Nhập kho mẫu cũ để sao chép
  dữ liệu cũ, không tự giao cho lớp hoặc xóa kho cũ. Mất dữ liệu trình duyệt sẽ mất
  kho mẫu cục bộ; bài đã giao vẫn ở server.
- Học liệu: xem/nghe đủ 29 chữ, sửa mô tả/ví dụ và upload media khi có quyền.
- Điểm viết tự động là độ bám mẫu, chưa xét thứ tự nét. Cô sửa điểm thì giữ điểm
  tự động ban đầu. Đề đã có bài nộp chỉ sửa tiêu đề/hướng dẫn/hạn nộp.

## Cấu hình trợ lý AI

Trợ lý dùng HTTP thông thường ở `/api/teacher/assistant/status` và `/draft`,
không dùng WebSocket. Cần vai trò giáo viên. Không gửi danh sách học sinh, điểm
hoặc ảnh bài làm cho AI; chỉ gửi yêu cầu soạn bài, lịch sử trao đổi gần nhất và
bản nháp đang chỉnh. Không nhập thông tin riêng của trẻ vào lời nhắc.

Đặt trong `backend/.env.local` riêng trên máy chủ rồi khởi động lại backend:

```dotenv
AI_BASE_URL=https://api.openai.com/v1
AI_API_KEY=<khóa của máy chủ>
AI_MODEL=<tên model hỗ trợ Chat Completions và JSON mode>
```

Không thêm khóa vào frontend, biến `VITE_*`, Git hoặc chat. Không cần cấu hình
khóa ở giai đoạn chuẩn bị: giao diện báo chưa kết nối và giữ đầy đủ soạn thủ công.
Adapter gọi `/chat/completions`, nhận JSON và kiểm tra lại hợp đồng ba loại bài;
không dùng nội dung AI sai định dạng hoặc liên kết media do AI tự tạo.
Tham khảo [API Chat Completions chính thức](https://developers.openai.com/api/reference/resources/chat/subresources/completions/methods/create).

Cô chọn loại/số câu, trao đổi để chỉnh yêu cầu, bấm Xem và chỉnh bản nháp để
kiểm tra trong biểu mẫu, chọn lớp rồi mới bấm Giao bài. Không tự giao từ chatbot.
Lỗi khóa/hạn mức/mạng phải có thông báo và cho soạn thủ công. Mỗi giáo viên tối đa
một yêu cầu đang chạy, 10 yêu cầu/phút; lịch sử web ở bộ nhớ phiên, không ghi DB.

`npm run test:browser` còn kiểm tra ba bộ soạn bài, chấm điểm, duyệt bản nháp và
bố cục điện thoại/máy tính với API giả lập. Kiểm thử giao thức backend dùng
`TeacherAssistantServiceTest`; muốn xác nhận model thật phải cấu hình riêng và
chủ động thử, không dùng API có phí trong kiểm thử mặc định.
