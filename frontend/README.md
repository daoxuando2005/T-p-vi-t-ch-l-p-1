# Vui Học — Website giáo viên và quản trị viên

Website viết bằng **React + JavaScript/JSX**, dùng Vite để phát triển/build.
Giao diện dùng chung phong cách xanh dương, xanh lá, cam và font Nunito Sans với
app Android. Giáo viên và admin có khu vực riêng theo quyền của tài khoản.

Học sinh/phụ huynh học và theo dõi trên [Android](../android/README.md).
Website vẫn có biểu mẫu đăng ký chung; đăng nhập web chỉ nhận giáo viên/admin.
Xem [tổng quan hệ thống](../README.md) và [cấu hình backend](../backend/README.md).

## Chức năng và đường dẫn

### Giáo viên

| Đường dẫn | Chức năng |
| --- | --- |
| `/teacher`, `/teacher/classes` | Tổng quan lớp; tạo/sửa/xóa lớp theo nghiệp vụ; mã mời và quản lý thành viên |
| `/teacher/assignments` | Chọn lớp, lọc loại bài; tạo/sửa/xóa bài; xem bài nộp; lưu điểm/nhận xét; kho mẫu và trợ lý AI |
| `/teacher/alphabet` | Xem/nghe học liệu 29 chữ; sửa ví dụ và media khi được cấp quyền |

Ba bộ soạn bài:

- **Tập viết chữ:** chọn chữ trong bảng 29 chữ tiếng Việt.
- **Đố vui ghép chữ:** nhập câu hỏi, lựa chọn và đáp án; có upload ảnh/âm thanh.
- **Ghép câu:** nhập các thẻ theo thứ tự đúng, 2–12 thẻ/câu; dùng dấu `|` để giữ
  một cụm từ thành một thẻ.

Bài đã có bài nộp được bảo vệ nội dung đề; chỉ chỉnh các thông tin cho phép như
hướng dẫn/tiêu đề/hạn nộp. Điểm viết tự động thể hiện độ bám mẫu; cô có thể chỉnh
điểm và nhận xét, đồng thời giữ điểm tự động ban đầu.

Kho mẫu được lưu trên trình duyệt theo ID tài khoản. Có thể chủ động nhập kho
mẫu cũ; mẫu không tự giao cho lớp. Xóa dữ liệu trình duyệt có thể làm mất kho mẫu
cục bộ, còn bài đã giao nằm ở backend.

### Quản trị viên

| Đường dẫn | Chức năng |
| --- | --- |
| `/admin` | Tổng quan tài khoản, lớp, bài nộp và hoạt động bảy ngày; biểu đồ kèm bảng số liệu |
| `/admin/users` | Tìm/lọc/phân trang; tạo/sửa tài khoản; đặt mật khẩu; khóa/mở; đổi vai trò và quyền giáo viên |
| `/admin/lessons` | Xem/sửa học liệu 29 chữ; media, trạng thái ẩn và ba cặp từ/câu của mỗi hành trình |
| `/admin/classes` | Tra cứu lớp, giáo viên, thành viên, bài nộp và liên kết gia đình; gỡ liên kết sau xác nhận |
| `/admin/settings` | Trạng thái backend, cấu hình AI và hướng dẫn thiết lập riêng trên máy chủ |

Hệ thống bảo vệ admin cuối cùng và tài khoản có lịch sử. Khóa, đặt mật khẩu hoặc
đổi vai trò/quyền làm phiên cũ hết hiệu lực. Quyền được kiểm tra ở backend, không
chỉ bằng việc ẩn nút trên giao diện. Chi tiết tại [backend](../backend/README.md).

### Đăng nhập và đăng ký

- `/login`: hiện/ẩn mật khẩu, hỗ trợ trình quản lý mật khẩu và trợ giúp đăng nhập.
  Đăng nhập xong tự vào khu vực theo vai trò, không cần chọn vai trò trước.
- `/register`: tạo học sinh/phụ huynh/giáo viên. Chế độ demo kích hoạt ngay,
  không OTP; email chưa xác minh. Không cho đăng ký admin công khai.
- Quên mật khẩu/tài khoản khóa do admin hỗ trợ. HTTP 401 yêu cầu đăng nhập lại;
  HTTP 403 thể hiện thiếu quyền truy cập.

## Chạy local

Cần Node.js 22.12 trở lên, npm và backend đang chạy. Từ thư mục `frontend`:

```powershell
npm ci
npm run dev
```

Mở **http://localhost:5173**. Website dùng `/api` cùng origin; Vite proxy mặc
định đến `http://127.0.0.1:8080`. Để đổi backend phát triển, đặt biến trước khi chạy:

```powershell
$env:VITE_BACKEND_TARGET = 'http://localhost:8081'
npm run dev
```

Đây là cấu hình của tiến trình Vite; thay bằng URL backend thật của bạn. Không
đưa địa chỉ MySQL hoặc mật khẩu DB vào frontend.

## Build và triển khai

```powershell
npm run build
npm run preview
```

Build sinh trong `dist/`; `preview` phục vụ kiểm tra bản build trên máy phát triển.
Khi đưa lên web server:

- Cấu hình SPA fallback về `index.html` cho các đường dẫn `/teacher/*`, `/admin/*`,
  `/login`, `/register`.
- Reverse proxy `/api` tới backend. Nếu API dùng origin riêng, đặt
  `VITE_API_BASE_URL` **trước khi build** và cấu hình HTTPS/CORS phù hợp ở backend.
- Service worker/PWA hỗ trợ tài sản frontend; đăng nhập và nghiệp vụ dùng API
  vẫn cần backend. Không coi cài PWA là có toàn bộ dữ liệu ngoại tuyến.

Ví dụ build với API ở địa chỉ riêng:

```powershell
$env:VITE_API_BASE_URL = 'https://api.example.com/api'
npm run build
```

Mọi biến `VITE_*` có thể đi vào mã gửi tới trình duyệt. API key AI và khóa JWT
chỉ cấu hình tại backend, không đặt trong các biến này.

## Dùng trợ lý Gemini

Cấu hình theo [hướng dẫn Gemini của backend](../backend/README.md#trợ-lý-soạn-bài-gemini),
rồi vào **Bài tập → Trợ lý soạn bài**:

1. Chọn loại bài và số chữ/câu.
2. Mô tả chữ cần luyện, chủ đề hoặc mức độ; có thể trao đổi tiếp để chỉnh bản nháp.
3. Bấm **Xem và chỉnh bản nháp**, kiểm tra nội dung trong bộ soạn bài.
4. Chọn lớp, kiểm tra thông tin và bấm **Giao bài**.

AI không tự giao bài. Không nhập thông tin riêng của học sinh vào lời nhắc.
Khi thiếu cấu hình hoặc dịch vụ lỗi/hết hạn mức, vẫn có thể soạn thủ công và dùng
kho mẫu. Trạng thái “đã cấu hình” chưa chứng minh model/khóa đang phản hồi được.

## API, media và cấu trúc mã

```text
src/
├── App.jsx                 Tuyến trang và bảo vệ theo vai trò
├── context/AuthContext.jsx Đăng nhập, phiên và thông báo hết hiệu lực
├── services/api.js         Axios, API base URL và Bearer token
├── components/             Khung dùng chung, thương hiệu, hộp thoại và trợ lý
├── pages/Auth/             Đăng nhập và đăng ký
├── pages/Teacher/          Lớp, bài tập, bài nộp và học liệu
├── pages/Admin/            Tổng quan, tài khoản, lớp/gia đình và cấu hình
├── utils/                  Kiểm tra/chuẩn hóa dữ liệu biểu mẫu
├── teacher.css             Nền tảng giao diện dùng chung
└── admin.css               Giao diện admin và xác thực
```

Upload mới lưu ở backend, không lưu trong `dist/`. URL công khai có dạng
`/api/public/media/...`. Các đường dẫn media cũ `/alphabet`, `/audio`,
`/example_images` được Vite chuyển tiếp tới backend khi phát triển.

Tài sản `public/` có thể được tham chiếu bằng URL từ DB/backend thay vì import
trong JSX. Kiểm tra các tham chiếu trước khi xóa. Font và giấy phép nằm tại
[public/fonts](public/fonts/).

## Kiểm thử

Chạy từ thư mục frontend:

```powershell
npm test
npm run lint
npm run build
```

Khi web local đang chạy, có thể kiểm tra giao diện bằng Edge/Chrome đã cài:

```powershell
npm run test:browser
```

Bộ browser test dùng profile tạm và API giả lập để kiểm tra đăng nhập/đăng ký,
giáo viên, admin, bản nháp AI và bố cục desktop/mobile; không thay dữ liệu thật.
Có thể đặt `BROWSER_PATH` và `WEB_TEST_URL` trước khi chạy nếu cần đổi trình duyệt
hoặc URL. Kết quả này cần đối chiếu với [kiểm thử API live](../backend/README.md#kiểm-thử)
để kiểm tra nghiệp vụ cùng MySQL thật.

## Xử lý lỗi thường gặp

| Hiện tượng | Kiểm tra |
| --- | --- |
| Trang có nhưng đăng nhập/API lỗi | Backend đã chạy và Vite proxy/API base URL đúng |
| Học sinh/phụ huynh không đăng nhập web | Dùng Android; khu vực web chỉ cấp cho giáo viên/admin |
| Thiếu nút giao bài/sửa học liệu | Vai trò/quyền giáo viên do admin cấp |
| Refresh trang con trả 404 | Web server cần SPA fallback về `index.html` |
| Không có ảnh/âm thanh | URL media, kho upload và đường proxy tới backend |
| Trợ lý báo chưa cấu hình | Đặt khóa riêng ở backend rồi khởi động lại backend |
