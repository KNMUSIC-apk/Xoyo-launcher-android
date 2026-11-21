# XOYO Launcher

Launcher Android cho Minecraft: Java Edition, phát triển từ [ZalithLauncher2](https://github.com/ZalithLauncher/ZalithLauncher2).

> Đây là bản chỉnh sửa không chính thức. Xem [LICENSE](LICENSE) và [NOTICE.md](NOTICE.md) để biết điều kiện sử dụng và danh sách thay đổi.

## Trạng thái

Nhánh `zalith-base` đang được chuyển đổi từ mã của ZalithLauncher2:
- Đã bỏ keystore, mật khẩu ký và legal docs của upstream.
- Đã đổi application ID thành `com.xoyo.launcher`.
- Đã có bảng màu XOYO (mặc định) và Monochrome, icon mới, nhãn "Unofficial Modified Version" trên màn hình chính.
- Khi tạo bố cục điều khiển mới có thể chọn mẫu cảm ứng (dựa trên Cubiq Launcher, GPL-3.0; xem [NOTICE.md](NOTICE.md)).
- Giao diện mới: sidebar trên màn hình rộng, thanh dưới và trang "Thêm" trên màn hình hẹp; trang chủ có Tổng quan (thao tác nhanh, lịch sử chơi, khám phá chỉ là liên kết) và nút PLAY lớn.
- Trang mới: Thư viện game, Trung tâm hiệu năng (số liệu thật, phần không đọc được ghi rõ là không khả dụng), Trung tâm máy chủ, Quản lý thế giới, Trung tâm nội dung.
- Nhóm Nội dung trên sidebar có Trò chơi, Tìm theo ID và Yêu thích. Thanh danh mục bên trái và nút Tải xuống trên thanh trên đã được bỏ.
- Nhóm Tải về trên sidebar gồm Tải mod, Tải modpack, Tải gói tài nguyên, Tải shader và Tải thế giới. Nút "Tìm và tải về" trong Trung tâm nội dung và nút "Tải thế giới về" trong Quản lý thế giới đã được bỏ.
- Các nhóm Nội dung, Tải về, Kết nối và Hệ thống thu gọn thành một nút. Bấm vào để mở các mục bên trong, trên sidebar và trong trang "Thêm" của màn hình hẹp.
- Nút quay lại trên thanh trên dùng cùng logic với nút Back của hệ thống, nên không thoát khỏi launcher khi đang ở trang con.
- Ghi hình màn hình trong game: nút trong menu game. Chỉ có hình, không có âm thanh của game. Trên Android 10 trở lên, video nằm ở `Movies/XOYO`.
- Theo dõi giật hình: đếm số đợt FPS tụt đột ngột trong phiên chơi (cần bật hiển thị FPS). Khi giật nhiều lần, launcher gợi ý cách giảm khựng hình.
- Sao lưu thế giới: nén thế giới thành file zip trong thư mục `world_backups` của phiên bản.
- Cấu hình G1 của các preset Cân bằng, Cao và Cực hạn đã được điều chỉnh để giảm khựng hình. Đây chưa phải kết quả đo trên máy thật.
- Skin và cape: tìm theo tên người chơi (API công khai của Mojang) hoặc dán link PNG https. Cape tự chọn từ file chỉ áp dụng cho tài khoản offline; tài khoản Microsoft chỉ dùng cape do Mojang cấp.
- Chưa có: nguồn tin khám phá trực tiếp, mức dùng GPU trong game (launcher không đọc được), nền động, UUID và chi tiết skin, nhập lại thế giới từ bản sao lưu, lưu token Microsoft an toàn.
- Chưa đổi tên toàn bộ package. Chưa kiểm thử trên thiết bị thật.

## Cài đặt từ GitHub Actions

Artifact `xoyo-launcher-debug` luôn được GitHub nén thành file `.zip`. Để cài:

1. Tải file `.zip` về và **giải nén** trước, ví dụ bằng ứng dụng Files hoặc trình quản lý file có chức năng "Giải nén".
2. Mở file `.apk` trong thư mục đã giải nén. **Không** bấm vào `.apk` ngay trong màn hình xem nội dung file zip, vì trình cài sẽ báo "phân tích cú pháp gói".
3. Cho phép ứng dụng quản lý file "cài đặt ứng dụng không xác định" nếu Android hỏi.

APK chỉ có kiến trúc `arm64-v8a`, tức là chỉ cài được trên điện thoại ARM 64-bit (hầu hết máy hiện nay).

## Yêu cầu build

- JDK 17
- Android SDK với compileSdk 37
- NDK 25.2.9519653 (build native bằng ndk-build)

## Build

```bash
# -Darch: arm64 | arm | x86 | x86_64 | all (mặc định, đóng gói cả 4 ABI và ~400 MB)
./gradlew testDebugUnitTest assembleDebug -Darch=arm64
```

## Cấu hình

| Thiết lập | Cách đặt |
|---|---|
| Client ID Microsoft | biến môi trường `OAUTH_CLIENT_ID` hoặc file `.oauth_client_id.txt` (không commit) |
| Khóa phát hành | file `xoyo_release.jks` cạnh `ZalithLauncher/build.gradle.kts`, cùng `STORE_PASSWORD`, `KEY_PASSWORD`, `KEY_ALIAS` |
| CurseForge API key | biến môi trường `CURSEFORGE_API_KEY` hoặc file `.curseforge_api.txt` (tùy chọn) |

Nếu không có khóa phát hành, bản release sẽ không được ký. Bản debug dùng khóa debug mặc định của Android.

## Tài liệu

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): kiến trúc và quy tắc bảo mật của XOYO
- [docs/research/ZALITH_HYPER_REVIEW.md](docs/research/ZALITH_HYPER_REVIEW.md): ghi chú khảo sát hai dự án tham khảo
