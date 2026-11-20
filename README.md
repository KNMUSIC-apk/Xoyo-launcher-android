# XOYO Launcher

Launcher Android cho Minecraft: Java Edition, phát triển từ [ZalithLauncher2](https://github.com/ZalithLauncher/ZalithLauncher2).

> Đây là bản chỉnh sửa không chính thức. Xem [LICENSE](LICENSE) và [NOTICE.md](NOTICE.md) để biết điều kiện sử dụng và danh sách thay đổi.

## Trạng thái

Nhánh `zalith-base` đang được chuyển đổi từ mã của ZalithLauncher2:
- Đã bỏ keystore, mật khẩu ký và legal docs của upstream.
- Đã đổi application ID thành `com.xoyo.launcher`.
- Chưa đổi tên toàn bộ package, chưa có nhãn "Unofficial Modified Version" trên màn hình khởi động.

## Yêu cầu build

- JDK 17
- Android SDK với compileSdk 37
- NDK 25.2.9519653 (build native bằng ndk-build)

## Build

```bash
./gradlew testDebugUnitTest assembleDebug
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
