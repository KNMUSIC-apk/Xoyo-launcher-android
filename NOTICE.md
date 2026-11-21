# NOTICE

## Nguồn gốc

XOYO Launcher là bản chỉnh sửa của **ZalithLauncher2**:

- Repo: https://github.com/ZalithLauncher/ZalithLauncher2
- Commit gốc: `1bbe6e73145f8c1918cb0b858894c22ae7e431f2`
- Giấy phép: GNU GPL v3.0, kèm điều khoản bổ sung (xem [LICENSE](LICENSE) và README của upstream)

Điều khoản bổ sung của upstream yêu cầu bản chỉnh sửa:
- Không dùng tên "ZalithLauncher" hoặc "ZL" trong tên sản phẩm, và không dùng tên gây nhầm lẫn.
- Hiển thị "Unofficial Modified Version" trên màn hình khởi động hoặc giao diện chính.
- Giữ nguyên thông báo bản quyền.

**Trạng thái tuân thủ:**
- [x] Không dùng tên "ZalithLauncher" làm tên sản phẩm (tên hiển thị là "XOYO Launcher").
- [x] Hiển thị "Unofficial Modified Version" trên màn hình chính (lớp phủ đáy trái của `LauncherScreen`, chuỗi `launcher_unofficial_label`).
- [x] Giữ nguyên thông báo bản quyền trong các file đã import.

## Không kế thừa từ upstream

- Legal docs (điều khoản người dùng, chính sách quyền riêng tư) của upstream. Các tài liệu này không thuộc GPL và không được đưa vào bản chỉnh sửa.
- Keystore phát hành của upstream (`zalith_launcher.jks`, `zalith_launcher_debug.jks`) và mật khẩu đi kèm.
- Client ID OAuth của upstream. XOYO dùng client ID đăng ký riêng.
- Workflow CI của upstream.

## Thay đổi so với upstream

- `ZalithLauncher/gradle.properties`: đổi tên, URL, phiên bản; bỏ mật khẩu keystore và `legal_docs_ref`.
- `ZalithLauncher/build.gradle.kts`: đổi application ID thành `com.xoyo.launcher`; cấu hình ký chuyển sang `xoyo_release.jks` (không bắt buộc); bản debug dùng khóa debug mặc định.
- `ZalithLauncher/build.gradle.kts`: thêm task copy `assets/game/versions.txt`, `version-alias.csv` và `control_templates/touch_template.json` vào test resources, để test đọc được `/assets/...`.
- `ZalithLauncher/src/main/java/.../ui/screens/content/settings/ControlManageScreen.kt`: thêm ô chọn mẫu (bố cục trống hoặc bố cục cảm ứng) khi tạo bố cục mới; bỏ hàm `createNew` không còn dùng. Mẫu được nạp trên IO thread, lỗi nạp thì báo lỗi, không tạo bố cục trống thay thế.
- `ZalithLauncher/src/main/res/values/strings.xml`, `values-vi/strings.xml`: chuỗi cho ô chọn mẫu.
- `ZalithLauncher/src/test/.../game/control/ControlTemplateTest.kt` (mới): kiểm tra mẫu cảm ứng được LayerController đọc, và uuid của nút không trùng.
- `ZalithLauncher/src/test/.../FileTest.kt`, `MurmurHash2IncrementalTest.kt`: đánh dấu `@Ignore`, vì hai test này đọc file trên máy Windows của upstream.
- `VersionCompareTest.kt`: đánh dấu `@Ignore` cho `testCompareLegacySnapshots` và `testCompareNewSnapshots`. Kỳ vọng của hai test này chưa được xác nhận (dùng alias placeholder và hai assert mâu thuẫn). Cần kiểm tra lại với upstream trước khi bỏ `@Ignore`.
- `ZalithLauncher/src/main/java/.../ui/theme/XoyoPalettes.kt` (mới): bảng màu XOYO (mặc định) và Monochrome. Monochrome lấy ý tưởng nền đen OLED từ Hyper Launcher 3 (LGPL-3.0), không sao chép mã.
- `ColorTheme.kt`, `Theme.kt`, `NativeThemeUtils.kt`, `LauncherSettingsScreen.kt`, `LauncherSettings.kt`: thêm hai giá trị `XOYO`, `MONOCHROME`; đặt `XOYO` làm mặc định.
- `ZalithLauncher/src/main/res/drawable/ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`, `values/ic_launcher_background.xml`: icon mới (chữ X hai màu trên nền mực). Icon khối của upstream đã bị thay thế.
- `ZalithLauncher/src/main/java/.../ui/screens/content/LauncherScreen.kt`: thêm nhãn "Unofficial Modified Version" trên màn hình chính.
- `.github/workflows/xoyo-ci.yml`: CI riêng của XOYO; build với `-Darch=arm64` và in thống kê APK.

## Giao diện XOYO (đợt chỉnh sửa lớn)

Các file mới hoặc sửa đổi trong `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/`:

- `ui/screens/main/AppNavigation.kt` (mới): sidebar 12 mục, thanh điều hướng dưới và trang "Thêm" cho màn hình hẹp.
- `ui/screens/main/MainScreen.kt` (sửa): bố cục sidebar/thanh dưới, nối các trang mới, xử lý thao tác nhanh.
- `ui/screens/content/LauncherScreen.kt` (sửa): hai chế độ trang chủ (Tổng quan và Thẻ), nút PLAY lớn, nút xoay mô hình, RAM và tần số màn hình trong Hero.
- `ui/screens/content/home/DashboardElements.kt` (mới): thống kê, thao tác nhanh có thể sắp xếp, hoạt động gần đây, khám phá (chỉ liên kết).
- `ui/screens/content/library/GameLibraryScreen.kt` (mới): thư viện game dạng thẻ/danh sách, tìm kiếm, sắp xếp.
- `ui/screens/content/performance/PerformanceCenterScreen.kt` (mới): số liệu thiết bị, cấu hình sẵn và tuỳ chỉnh, chẩn đoán.
- `ui/screens/content/hub/ServerHubScreen.kt`, `WorldManagerScreen.kt`, `ContentCenterScreen.kt` (mới).
- `ui/screens/NormalNavKey.kt` (sửa): thêm các khoá màn hình mới và `ContentKind`.
- `ui/screens/content/settings/LauncherSettingsScreen.kt` (sửa): công tắc "Giảm chuyển động".
- `setting/LauncherSettings.kt` (sửa): thêm `launcherSidebarExpanded`, `launcherReducedMotion`, `launcherQuickActions`, `performanceCustomPresets`.
- `game/playhistory/PlayHistory.kt` (mới) và `ui/activities/VMActivity.kt` (sửa): ghi nhận thời gian chơi khi game chạy và thoát.
- `game/performance/DeviceStats.kt`, `PerformancePresets.kt`, `LauncherDiagnostics.kt` (mới): đọc số liệu thiết bị, gợi ý cấu hình và luật chẩn đoán dựa trên log.
- `res/values/strings.xml`, `res/values-vi/strings.xml`: chuỗi cho các trang mới.

Đợt chỉnh sửa tiếp theo (skin, cape và sidebar):

- `ui/screens/content/DownloadScreen.kt` (sửa): bỏ thanh danh mục bên trái, các danh mục chuyển sang sidebar.
- `ui/screens/main/AppNavigation.kt`, `MainScreen.kt` (sửa): bỏ nút Tải xuống trên thanh trên; thêm Trò chơi, Tìm theo ID, Yêu thích vào nhóm Nội dung.
- `ui/screens/content/hub/WorldManagerScreen.kt` (sửa): nút tải thế giới về.
- `game/account/wardrobe/WebSkinImages.kt`, `WebSkinSource.kt` (mới): tra cứu skin và cape theo tên người chơi qua API công khai của Mojang, tải và kiểm tra ảnh PNG.
- `ui/screens/content/elements/WebSkinDialog.kt` (mới); `AccountElements.kt`, `AccountManageScreen.kt`, `viewmodel/AccountManageViewModel.kt` (sửa): menu 3 chấm có "Chọn cape từ file" và "Chọn skin hoặc cape từ web".
- `game/account/offline/OfflineYggdrasilServer.kt`, `game/launch/LaunchArgs.kt` (sửa): gửi cape tự chọn của tài khoản offline cho game.
- `src/test/.../game/account/wardrobe/MinecraftNameTest.kt` (mới): kiểm tra quy tắc tên người chơi.

Dữ liệu skin và cape lấy từ `api.mojang.com` và `sessionserver.mojang.com` (API công khai, không cần khoá). Link ảnh do người dùng dán chỉ được chấp nhận khi là https và đúng định dạng PNG.

Giao diện được thiết kế theo đặc tả của dự án. Không sao chép mã hay tài nguyên của Dawn Client hoặc FastClient.

## Mẫu và dữ liệu từ dự án khác

- `ZalithLauncher/src/main/assets/control_templates/touch_template.json`: bố cục điều khiển cảm ứng, lấy từ [Cubiq Launcher](https://github.com/VivxKumar07/Cubiq-Launcher). Cubiq Launcher cũng là bản fork GPL-3.0 của ZalithLauncher2.
  - Giữ nguyên dữ liệu bố cục: vị trí, kích thước, nút và phím.
  - Đổi phần thông tin hiển thị (tên, tác giả, mô tả) để không dùng tên dự án gốc trong XOYO. Khi tạo bố cục mới, tên và tác giả còn bị thay bằng giá trị người dùng nhập.
  - Không sao chép giao diện, biểu tượng hay tài nguyên đồ họa nào của Cubiq.
  - Việc ghi nguồn này là điều kiện GPL-3.0 đối với mẫu.

## Thư viện bên thứ ba

Các thư viện và thành phần native đi kèm (thư mục `libs/`, `ZalithLauncher/src/main/jni/`, `LWJGL/`) giữ nguyên giấy phép và thông báo bản quyền của chúng. Cần rà soát lại danh sách này trước khi phát hành.
