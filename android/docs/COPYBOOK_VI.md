# Mẫu chữ và ô ly tập viết

## Phạm vi

Bảng tập viết dùng chữ **thường, cỡ vừa**, phóng to trên màn hình. Một đơn vị
chiều cao thân chữ tương ứng **2 ô li nhỏ**. Đây là tỉ lệ trong sơ đồ, không phải
cam kết mỗi ô trên mọi màn hình đều có kích thước vật lý bằng một ô vở giấy.

| Chữ | Trên dòng chân chữ | Dưới dòng | Tổng chiều cao thân |
| --- | --- | --- | --- |
| a, ă, â, c, e, ê, i, m, n, o, ô, ơ, u, ư, v, x | 2 li | 0 | 2 li |
| r, s | 2,5 li | 0 | 2,5 li |
| t | 3 li | 0 | 3 li |
| d, đ | 4 li | 0 | 4 li |
| b, h, k, l | 5 li | 0 | 5 li |
| p, q | 2 li | 2 li | 4 li |
| g, y | 2 li | 3 li | 5 li |

Dấu phụ của ă/â/ê/ô/ơ/ư và chấm trên i không được dùng làm mốc đo chiều cao thân.
Chiều rộng và hình dạng đường cong lấy từ font HP001 có sẵn trong dự án.
Chữ r bổ sung nét xiên vào từ chân dòng do mẫu HP001 bắt đầu ở phía trên dòng;
vùng khuyết được giữ rỗng khi ghép các đường viền.

## Nguồn đối chiếu

- [Quyết định 31/2002/QĐ-BGD&ĐT – thông tin văn bản tại Trường Tiểu học Nguyễn Văn Kịp](https://thnguyenvankip.hcm.edu.vn/quyet-dinh/mau-chu-viet/vbctmb/143786/53695).
- [Tài liệu tập huấn giáo viên Tiếng Việt 1, Cánh Diều, 2020](https://medialib.qlgd.edu.vn/Uploads/THU_VIEN/shn/1/158/UserFiles/tai-lieu-tap-huan-mon-tieng-viet-sach-canh-dieu-0035868b-8918-489e-8375-df954d4f6350.pdf), trang 10–11: cỡ vừa/cỡ nhỏ, đường kẻ, cách viết a/c.
- [Nghiên cứu rèn kĩ năng viết bảng cho sinh viên Giáo dục tiểu học](https://scholar.dlu.edu.vn/thuvienso/bitstream/DLU123456789/275491/1/108322-925-219230-1-2-20250214.pdf), bảng nhóm chiều cao theo đơn vị chữ.
- [Hướng dẫn viết chữ của Trường Tre Việt](https://bambooschool.edu.vn/cach-viet-29-chu-cai-tieng-viet/): đối chiếu thành phần nét và vị trí bắt đầu. Phần tên nét trong app là nhãn ngắn; không sao chép nguyên bài hướng dẫn.

## Cách triển khai

- `CopybookForm.kt`: dữ liệu riêng cho đủ 29 chữ; chiều cao trên/dưới dòng, chữ
  tham chiếu để đo thân, vị trí đặt bút và hướng khởi đầu, các thành phần theo thứ tự.
- Lấy đường viền thật bằng `Paint.getTextPath`, đo cực trị đường cong bằng
  `Path.approximate`, thay cho căn giữa hộp dòng của `Text`. `Path.computeBounds`
  tính cả điểm điều khiển Bezier nên không dùng để đo chiều cao chữ.
- Chuẩn hóa riêng phần trên và dưới chân chữ. Đo thân của â theo a, ô/ơ theo o,
  ê theo e, ư theo u; i cũng dùng u để loại dấu chấm khỏi phép đo.
- `CopybookPaper.kt`: hệ 10 × 10 ô, đường chân chữ ở hàng 6. Chừa 5 li phía trên,
  3 li phía dưới và lề. Đánh số đường 1–6 từ dưới lên; ngoặc bên phải đo thân chữ.
- Chấm và mũi tên xanh gợi vị trí/hướng bắt đầu, tự ẩn khi bé viết. Nút thông tin
  mở mẫu và danh sách thành phần; không thay đổi bài đang viết.
- Hệ tọa độ giấy cố định, độc lập cỡ chữ hệ thống. Nét người dùng vẫn lưu tọa độ
  chuẩn hóa như trước, giữ cùng tỉ lệ khi xoay máy hoặc đổi kích thước cửa sổ.
  Độ dày nét bút cũng tính theo ô li, kể cả nét chấm.

## Kiểm chứng và giới hạn

`CopybookFormTest` kiểm tra chiều cao thực của đường viền, thân chữ có dấu, phần
kéo xuống, vùng khuyết không bị tô kín, lề và đủ 29 chữ. Test xuất `copybook-alphabet-proof.png` vào thư mục
files của app để duyệt cả bảng. `StudentExperienceTest` kiểm tra mở/đóng mẫu giữ
nguyên nét viết và chụp một số chữ đại diện, cùng kiểm tra xoay máy đã có.

Đây là triển khai bám tỉ lệ mẫu chữ và tài sản HP001, chưa phải chứng nhận của
Bộ GD&ĐT. Hình dáng font, vị trí đặt bút và hướng dẫn sư phạm vẫn cần giáo viên
tiểu học duyệt trước khi dùng như tài liệu giảng dạy chính thức. Đường viền font
không chứa thứ tự bút; không suy diễn thứ tự viết từ thứ tự contour của font.
Bản này chưa có hoạt ảnh viết từng nét, nhận dạng hoặc chấm điểm bài viết.

### Kết quả kiểm tra ngày 08/11/2026

- `:app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug`: thành công.
  Lint không có lỗi; còn các cảnh báo toolchain, thư viện và mã có sẵn.
- Thiết bị Xiaomi 23127PN0CC, Android 16: `CopybookFormTest` **5/5 đạt**.
- `StudentExperienceTest`: **3/3 bài liên quan đến thay đổi đạt**:
  `copybookReferenceKeepsTheDraftAndShowsLetterSpecificGuidance`,
  `writingKeepsToolsAndDraftAfterRecreation`,
  `landscapeKeepsBoardAndToolsVisible`.
- Chạy instrumentation trực tiếp với `-e disableAnalytics true` sau khi Gradle
  connected test chờ kết nối mDNS. Không gỡ app hoặc xóa dữ liệu để kiểm thử.
- Đã duyệt ảnh tổng hợp đủ 29 chữ và ảnh giao diện g, h, hộp xem mẫu r.

Ví dụ chạy kiểm thử hình học sau khi cài app và APK androidTest:

```powershell
adb -s <serial> shell am instrument -w -r -e disableAnalytics true `
  -e class com.hocchu.mobile.CopybookFormTest `
  com.hocchu.mobile.test/androidx.test.runner.AndroidJUnitRunner
```
