# Ghi chú khảo sát: ZalithLauncher2 và Hyper Launcher 3

Ngày khảo sát: 2026-11-20. Chỉ đọc mã nguồn. Không build, không chạy, không tải file nhị phân.

## Nguồn tham chiếu

| Dự án | Repo | Commit | Giấy phép | Ghi chú |
|---|---|---|---|---|
| ZalithLauncher2 | github.com/ZalithLauncher/ZalithLauncher2 | `1bbe6e73145f` | GPL-3.0 + điều khoản bổ sung | Module chính `ZalithLauncher`: 878 file .kt, 46 file .java. Toàn repo: 986 .kt, 112 .java |
| Hyper Launcher 3 | github.com/ashmeet-op/Hyper_Launcher_v3 | `46fb6f65d27d` | LGPL-3.0 | 225 file .java, 207 file .kt. Năm submodule (glfw, sdl, mojoexec, horizon-android, NoBlocklist) rỗng trong bản clone |

Bản clone nằm tại `/home/claude/zalithlauncher/zalithlauncher2` và `/home/claude/ashmeet-op/hyper_launcher_v3`.

## Điều kiện giấy phép

**Zalith (GPL-3.0 + điều khoản bổ sung, theo README):**
- Bản sửa đổi khi phát hành phải đổi tên. Không dùng "ZalithLauncher" hoặc "ZL", và không dùng tên gây nhầm lẫn.
- Phải hiển thị "Unofficial Modified Version" trên màn hình khởi động hoặc giao diện chính.
- Không được xóa thông báo bản quyền.

**Hyper (LGPL-3.0):** cho phép sử dụng và liên kết rộng hơn GPL, nhưng vẫn yêu cầu giữ thông báo bản quyền và cung cấp mã nguồn khi phát hành các phần đã sửa.

**Lưu ý chung:** nếu chép đoạn mã GPL vào XOYO, dự án thường phải phát hành theo GPL-3.0. Vì vậy XOYO chỉ tham khảo kiến trúc và endpoint giao thức, và viết mã mới. Đây không phải tư vấn pháp lý. Nếu cần chắc chắn, hãy hỏi chuyên gia pháp lý.

## Phát hiện về bảo mật

1. **Hyper ghi token ra Logcat.** Trong `MicrosoftBackgroundLogin.java`, dòng 198 ghi Xbl Token, dòng 234 ghi Xbl Xsts, dòng 270 ghi `access_token` của Minecraft. Đây là lỗi nghiêm trọng và trái với chính sách XOYO. **Không bắt chước.**
2. **Cả hai lưu access/refresh token dạng văn bản thuần.** Zalith lưu qua `AccountDao` với các trường `accessToken` và `refreshToken`. Hyper lưu trong `Account.java`, dạng JSON. XOYO mã hóa bằng Android Keystore (xem `SecureTokenStore`).
3. **Hyper dùng client ID cố định `00000000402b5328`** với `login.live.com`. Mã này được nhiều launcher khác dùng. Chưa rõ có phải ID đăng ký chính thức của dự án hay không. XOYO dùng client ID đăng ký riêng qua `XOYO_MS_CLIENT_ID`.
4. **Keystore được commit vào repo:** Zalith có `zalith_launcher.jks` và `zalith_launcher_debug.jks`, Hyper có `debug.keystore`. XOYO không dùng lại các keystore này, và không commit keystore release.
5. **Native library commit sẵn:** Zalith có 75 file `.so`, Hyper có 21 file `.so`. Nguồn gốc của các file này chưa được kiểm chứng trong khảo sát. Trước khi dùng, cần kiểm tra ABI, checksum và nguồn gốc.

## Đối chiếu kỹ thuật

| Mảng | Zalith 2 | Hyper 3 |
|---|---|---|
| Engine chạy game | PojavLauncher (JNI, `ctxbridges`, `GL`, `driver_helper`) | PojavLauncher (JNI, `jvm_hooks`, `jre_launcher`, `awt`) |
| UI | Jetpack Compose, Material 3, Miuix | Kết hợp Compose và View |
| Đăng nhập Microsoft | Device-code chính thức, client ID từ `BuildKeys` | Luồng `login.live.com`, client ID cố định |
| Renderer | Interface có cấu hình và plugin. Có Freedreno, Panfrost, VirGL, Zink, MobileGlues, GL4ES | `Renderers.kt` đơn giản hơn (55 dòng) |
| Điều khiển cảm ứng | `ControlManager`, `ControlData`, `ControlEditor`, module `LayerController` | Package `customcontrols` |
| Kiểm thử | Có `src/test` ở `ZalithLauncher`, `CardGrid`, `Guide` | Không có thư mục test |
| CI | 3 workflow: `build.yml`, `push_ci.yml`, `release_ci.yml` | Không có workflow nào (chỉ có `crowdin.yml` cho dịch thuật) |
| SDK | minSdk 26, targetSdk 35 | Chưa đối chiếu |

**Tổ chức mã:** Hyper chia thành `hyperbackend` (Java, xử lý nghiệp vụ như đăng nhập, tải, chạy game, render) và `hyperlauncher` (Kotlin, UI). Zalith tổ chức theo tính năng trong `movtery/zalithlauncher`.

## Việc đã làm dựa trên khảo sát này

- **Module `auth`:** viết mới theo đặc tả công khai của device-code và chuỗi Xbox/Minecraft. Endpoint và tham số được đối chiếu với Zalith. Không sao chép mã.
- **Module `renderer`:** thiết kế registry có phương án dự phòng. Không sao chép mã.
- **Điều khiển cảm ứng:** chỉ ghi thiết kế trong `docs/ARCHITECTURE.md`.
- **Không tiếp nhận** phần đăng nhập hay ghi log của Hyper.
