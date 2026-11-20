# XOYO Launcher

Launcher Android cho Minecraft: Java Edition, phát triển từ [ZalithLauncher2](https://github.com/ZalithLauncher/ZalithLauncher2).

> Đây là bản chỉnh sửa không chính thức. Xem [LICENSE](LICENSE) và [NOTICE.md](NOTICE.md) để biết điều kiện sử dụng và danh sách thay đổi.

## Trạng thái

Nhánh `zalith-base` đang được chuyển đổi từ mã của ZalithLauncher2:
- Đã bỏ keystore, mật khẩu ký và legal docs của upstream.
- Đã đổi application ID thành `com.xoyo.launcher`.
- Đã có bảng màu XOYO (mặc định) và Monochrome, icon mới, nhãn "Unofficial Modified Version" trên màn hình chính.
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
