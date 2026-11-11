# Vui Học — Website

Website React và TypeScript cho học sinh, phụ huynh, giáo viên và quản trị viên.
Ứng dụng Android ở `../android`, backend dùng chung ở `../backend`.
Giữ các trang còn có tuyến truy cập khi dọn mã.

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
```

`src/main.tsx` là điểm vào; `src/App.tsx` khai báo tuyến theo vai trò.

## Mẫu chữ và tài sản

`src/pages/Student/writing/engine/` được trang tập viết và bài tập học sinh sử dụng.
Giữ `templateData.ts` và font đang dùng tại `public/fonts/HP001.ttf`.
`../generate_templates.py` sinh lại dữ liệu mẫu; chạy từ thư mục gốc với môi
trường Python có NumPy, Pillow và scikit-image. Chỉ sinh lại khi có chủ đích và
duyệt diff trước khi commit.

Ảnh/âm thanh từ backend có thể không xuất hiện trong import tĩnh. Trước khi xóa
tài sản trong `public/`, kiểm tra cả URL trong CSS, cấu hình, backend và dữ liệu
học liệu. Giữ các biểu tượng PWA và font đang được tham chiếu.
