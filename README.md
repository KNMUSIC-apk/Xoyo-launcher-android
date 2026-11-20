# XOYO Launcher

Launcher Android cho Minecraft: Java Edition. Dự án đang ở giai đoạn khung ban đầu.

## Yêu cầu

- JDK 17
- Android Studio (khuyến nghị) hoặc Android SDK với `compileSdk` 35
- Android 8.0 (API 26) trở lên

## Build

```bash
./gradlew assembleDebug
```

Lần đầu chạy, Gradle sẽ tải các dependency từ Google Maven và Maven Central.

## Cấu hình đăng nhập Microsoft

Cần một client ID đăng ký trên [Azure Portal](https://portal.azure.com) với:
- Loại tài khoản hỗ trợ: tài khoản Microsoft cá nhân
- Bật "Allow public client flows"
- Không cần client secret

Thêm vào `~/.gradle/gradle.properties` (không commit file này):

```properties
XOYO_MS_CLIENT_ID=<client-id-của-bạn>
```

Nếu để trống, `MicrosoftAuthenticator` sẽ báo lỗi khi khởi tạo.

## Kiểm thử

```bash
./gradlew testDebugUnitTest
```

## Cấu trúc

```
app/src/main/java/com/xoyo/launcher/
  auth/        Đăng nhập Microsoft (device-code), lưu token mã hóa
  renderer/    Đăng ký renderer và chọn renderer có phương án dự phòng
app/src/main/res/                      Layout, chuỗi, theme
docs/ARCHITECTURE.md                   Kiến trúc và quy tắc bảo mật
docs/research/                         Ghi chú khảo sát các dự án tham khảo
gradle/libs.versions.toml              Khai báo version và dependency
```
