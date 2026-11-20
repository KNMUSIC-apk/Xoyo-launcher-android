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

## Cấu trúc

```
app/src/main/java/com/xoyo/launcher/   Mã nguồn Kotlin
app/src/main/res/                      Layout, chuỗi, theme
gradle/libs.versions.toml              Khai báo version và dependency
```
