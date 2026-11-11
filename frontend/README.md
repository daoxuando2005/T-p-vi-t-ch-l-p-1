# Vui Học — Website

Website React và TypeScript dành cho giáo viên và quản trị viên.
Học sinh và phụ huynh sử dụng ứng dụng Android tại `../android`.
Hai nền tảng dùng chung backend trong `../backend`.

Website không cung cấp trang học sinh/phụ huynh. Đăng nhập và khôi phục phiên
chỉ nhận vai trò giáo viên hoặc admin; các tài khoản dùng Android được hướng
dẫn ngay trên trang đăng nhập. Biểu mẫu đăng ký/OTP chung vẫn tạo được tài
khoản học sinh/phụ huynh vì app chưa có chức năng đăng ký.

## Chạy và kiểm tra

Chạy backend theo [hướng dẫn dự án](../README.md), rồi chạy từ thư mục này:

```sh
npm ci
npm run dev
```

Website local: `http://localhost:5173`. URL API nằm trong `src/services/api.ts`.
Website gọi backend, không kết nối trực tiếp MySQL.

```sh
npm run build
npm run lint
npm test
```

`src/main.tsx` là điểm vào; `src/App.tsx` khai báo tuyến theo vai trò.

Để kiểm tra đăng nhập, khôi phục phiên và chuyển trang trong trình duyệt, chạy
web local rồi `npm run test:browser` (Node.js 22.12 trở lên). Bài kiểm tra dùng Edge/Chrome đã cài, profile
tạm và API giả lập, không dùng dữ liệu thật. Có thể đặt `BROWSER_PATH` và
`WEB_TEST_URL` khi trình duyệt hoặc địa chỉ web khác mặc định.

## API và tài sản dùng chung

Không xóa API học sinh/phụ huynh khi bỏ giao diện web: app Android vẫn dùng
chúng. Giáo viên và admin cũng tải học liệu qua `/api/student/alphabet`.
Mẫu chữ Android nằm trong `android/app/src/main/res/font/handwriting.ttf`;
không phụ thuộc bộ chấm nét hay font cũ của website.

Ảnh/âm thanh từ backend có thể không xuất hiện trong import tĩnh. Trước khi xóa
tài sản trong `public/`, kiểm tra cả URL trong CSS, cấu hình, backend và dữ liệu
học liệu. Giữ các biểu tượng PWA và font đang được tham chiếu.
