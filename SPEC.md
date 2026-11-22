# SPEC.md: hợp đồng phát triển chung cho XOYO Launcher

- Phiên bản: 0.1 (bản nháp, chờ người phụ trách duyệt)
- Ngày: 2026-11-22
- Nhánh tham chiếu: `zalith-base` tại commit `60117b7`
- Quy tắc sửa SPEC: chỉ người phụ trách dự án (con người) được duyệt thay đổi. Hai A.I đề xuất thay đổi qua mục 7.

---

## 0. Trạng thái repo (đã quét)

| Thành phần | Nội dung |
|---|---|
| Ngôn ngữ và UI | Kotlin 2.4.21, Jetpack Compose (BOM 2026.09.00), Material 3, Navigation3 1.2.0. Không dùng XML view. `res/layout/` chỉ có `player_texture_view.xml`. |
| Build | AGP 9.3.0, JDK 17, compileSdk 37 (minor 2), minSdk 26, targetSdk 34, NDK 25.2.9519653, có Rust cho `PetLib`. |
| Module Gradle | `:ZalithLauncher`, `:LWJGL` (gồm `patches`, `lwjgl-3.3.3`, `lwjgl-3.4.1`), `:LayerController`, `:ColorPicker`, `:CardGrid`, `:Terracotta`, `:InputMap`, `:Guide`, `:PetLib`. |
| Namespace (package Kotlin) | `com.movtery.zalithlauncher`. Giữ nguyên. |
| applicationId | `com.xoyo.launcher`. Bản debug có hậu tố `.debug`. |
| Branding | `ZalithLauncher/gradle.properties`: `launcher_name=XoyoLauncher`, `launcher_app_name=XOYO Launcher`, `launcher_short_name=XOYO`, `url_home`, `launcher_version_code=1`, `launcher_version_name=0.1.0`. |
| Quy mô mã | `ZalithLauncher/src/main/java`: 912 file. `ui/screens`: 169 (`content` 102, `main` 35, `game` 19, `splash` 6, gốc 7). `ui/components` 34, `ui/control` 34, `ui/theme` 23, `ui/activities` 6, `ui/base` 5, `ui/guide` 4, `ui/code_editor` 4. Logic: `game/` 351, `setting/` 33, `utils/` 44, `viewmodel/` 18. |
| Test | `ZalithLauncher/src/test/java`: 41 file. `src/test/resources`: 14 file. |
| Chuỗi | `values/strings.xml`: 1780 dòng khóa. `values-vi/strings.xml`: 1780 dòng khóa. Còn thiếu 0 khóa tiếng Việt (cập nhật sau khi gộp `ui/vi-localization`). |
| CI | `.github/workflows/xoyo-ci.yml`. Chạy khi push vào `main` và `zalith-base`, khi có PR, và thủ công với `publish=true`. |

### Điểm lệch cần biết trước khi làm

1. `docs/ARCHITECTURE.md` đã cũ. Nó mô tả các package `auth`, `renderer`, `launch`, `settings`, `touchcontrols` như đã có, nhưng code thật nằm ở `game/account`, `game/renderer`, `game/launch`, `setting`, `ui/control`. Không dùng tài liệu này làm căn cứ kiến trúc, và cần cập nhật. Tài liệu này nằm trên nhánh `main` (bản khung ban đầu), không có trên `zalith-base`.
2. Client ID Microsoft: tài liệu ghi `XOYO_MS_CLIENT_ID`, nhưng Gradle và code đọc `OAUTH_CLIENT_ID` (env, hoặc `.oauth_client_id.txt`, hoặc `oauth_client_id` trong `gradle.properties`). CI ánh xạ secret `XOYO_MS_CLIENT_ID` thành env `OAUTH_CLIENT_ID`. Trong code luôn dùng tên `OAUTH_CLIENT_ID`.
3. `addonPlatformSource` lưu dưới khóa cũ `assetPlatformSource`. Giữ nguyên để tương thích dữ liệu cũ.
4. Kho cài đặt MMKV dùng `BuildKeys.LAUNCHER_IDENTIFIER` làm ID (`setting/_MMKV.kt`), và giá trị này lấy từ `launcher_name`. Đổi `launcher_name` sẽ làm người dùng mất toàn bộ cài đặt đã lưu.
5. Có logic nằm trong file UI: `scanInstalled` (`ui/screens/content/hub/ContentCenterScreen.kt`) và `backupWorld` (`ui/screens/content/hub/WorldManagerScreen.kt`). Xem mục 6.
6. Nhánh `main` (nhánh mặc định trên GitHub) là một khung dự án khác: có module `app/`, package `com.xoyo.launcher`, không chứa code Zalith. Nếu build hoặc chạy từ `main`, bạn đang chạy sai dự án. Hãy dùng `zalith-base` hoặc nhánh tích hợp `integrate/spec-merge`.

---

## 1. Phân vai

| | A.I #1: UI/UX và tài nguyên | A.I #2: logic và lõi |
|---|---|---|
| Việc được giao | Giao diện Compose, theme, màu, icon, branding hiển thị, Việt hóa `strings.xml`. | Logic khởi chạy game, tài khoản, cài đặt, tải về, điều hướng, CI và Gradle khi được duyệt. |
| Phạm vi file | Mục 2.1 | Mục 2.2 |
| Gọi logic | Chỉ qua API đã công bố ở mục 3. | Công bố API ở mục 3 trước khi UI dùng. Không đổi chữ ký đã công bố. |

Nguyên tắc nối UI với logic: A.I #2 viết hàm backend và công bố ở mục 3 trước. A.I #1 chỉ gắn nút vào hàm đã có trong mục 3. Nếu nút cần một hàm chưa có, A.I #1 ghi yêu cầu ở mục 7 và dừng phần đó.

Người duyệt (con người) quyết định khi hai A.I xung đột, và duyệt mọi thay đổi về Gradle, native, `NOTICE.md`, `LICENSE`.

---

## 2. Phạm vi file (chốt cứng)

Quy ước: đường dẫn tính từ gốc repo. `**` nghĩa là toàn bộ thư mục con. File không nằm trong danh sách nào dưới đây là **không được sửa**. File mới phải được thêm vào SPEC trước khi tạo.

### 2.1 A.I #1 được sửa

Tài nguyên:
- `ZalithLauncher/src/main/res/drawable*/**`
- `ZalithLauncher/src/main/res/mipmap-*/**`
- `ZalithLauncher/src/main/res/values*/**` (màu, kiểu, kích thước, chuỗi). Không đổi khóa chuỗi đang được code tham chiếu, xem mục 3.5.
- `ZalithLauncher/src/main/ic_launcher-playstore.png`
- `README.md`, `README_EN_US.md`, `README_ZH_TW.md`
- `ZalithLauncher/gradle.properties`: chỉ đổi giá trị `launcher_app_name` và `launcher_short_name`. Các khóa khác thuộc mục 2.3.

Mã Compose:
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/components/**`
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/theme/**`
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/guide/**`
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/code_editor/scheme/**`
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/**`, **trừ** các file liệt kê ở mục 2.2.
- Module: `Guide/**`, `ColorPicker/**`, `CardGrid/**`
- Test: `ZalithLauncher/src/test/java/com/movtery/zalithlauncher/ui/theme/**`

Ghi chú về `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/main/AppNavigation.kt`: file này dùng chung. A.I #1 sở hữu phần giao diện (sidebar, thanh dưới, sheet "Thêm", các composable của nhóm). Ba hàm `navDestinationOf`, `navigateToDestination`, `downloadDestination`, cùng `NavDestination` và `NavGroup`, có đích là NavKey. Đổi đích điều hướng là đổi logic, nên chỉ làm khi A.I #2 đồng ý và mục 3.4 không đổi. Trong một thời điểm chỉ một A.I sửa file này, và PR ghi rõ điều đó.

Quyết định khi gộp (`integrate/spec-merge`): mục Mod trên sidebar vẫn mở danh sách mod đã cài (`ContentKind.MODS`), không chuyển thẳng sang trang tải như nhánh `fix/ui-scroll-skin-mod`. Lý do: nếu chuyển thẳng, danh sách mod đã cài không còn lối vào nào. Đường tải mod là mục "Tải mod" trong nhóm Tải về. Muốn đổi thì sửa đúng một dòng trong `navigateToDestination`, nhưng cần người duyệt ghi lại trong mục 7.

### 2.2 A.I #2 được sửa

Logic, tức toàn bộ `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/` **trừ** các đường dẫn đã giao cho A.I #1 ở mục 2.1. Ví dụ: `game/**`, `viewmodel/**`, `setting/**`, `utils/**`, `notification/**`, `keepalive/**`, `database/**`, `coroutine/**`, `bridge/**`, `contract/**`, `context/**`, `crashlogs/**`, `filemanager/**`, `legal/**`, `library/**`, `path/**`, `provider/**`, `sponsor/**`, `terracotta/**`, `components/**`.

Các file UI mà A.I #2 sở hữu, dù nằm trong thư mục giao cho A.I #1:
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/activities/**`
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/base/**`
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/control/**`
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/code_editor/*.kt` (không gồm `scheme/`)
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/_Navigation.kt`
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/NavStateHost.kt`
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/BackStackNavKey.kt`
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/NestedNavKey.kt`
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/NormalNavKey.kt`
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/TitledNavKey.kt`
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/DownloadScreen.kt` (chứa `navigateToDownload`)

Khác:
- `ZalithLauncher/src/main/AndroidManifest.xml`
- `ZalithLauncher/src/main/res/xml/**`, `ZalithLauncher/src/main/res/layout/**` (có code tham chiếu)
- `ZalithLauncher/src/test/**`, trừ các test thuộc mục 2.1
- `.github/workflows/**`
- `docs/**`
- `NOTICE.md`: chỉ thêm mục mới, không sửa hay xóa mục cũ. Người duyệt phải xác nhận.

### 2.3 Không ai được sửa nếu chưa có phê duyệt của con người

- `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/**`, `gradlew`, `gradlew.bat`
- `ZalithLauncher/build.gradle.kts`, `ZalithLauncher/proguard-rules.pro`, `ZalithLauncher/.gitignore`
- `LICENSE`, `.gitignore`, `.zcodeignore`
- `LWJGL/**`, `LayerController/**`, `Terracotta/**`, `InputMap/**`, `PetLib/**` (chưa giao, xem mục 7)
- `ZalithLauncher/src/main/jni/**`, `ZalithLauncher/src/main/jniLibs/**`, `ZalithLauncher/libs/**`

### 2.4 Quy tắc cấm

- Không sửa file ngoài phạm vi. Nếu cần, ghi vào mục 7 và dừng.
- Không đổi namespace `com.movtery.zalithlauncher` hay applicationId `com.xoyo.launcher`.
- Không đổi `launcher_name`. MMKV và tên file APK đều phụ thuộc vào nó.
- Không đổi cấu trúc `build.gradle.kts`. Không thêm, bớt hay đổi phiên bản dependency, plugin, `minSdk`, `targetSdk`, `compileSdk`, `ndkVersion`, `signingConfig` nếu chưa được duyệt.
- Không xóa tính năng người dùng đã yêu cầu. Chỉ bỏ những gì người dùng nêu tên rõ. Danh sách giữ lại ở mục 5.
- Không đổi tên, kiểu, giá trị mặc định của khóa cài đặt, lớp NavKey, tên sự kiện trong mục 3. Đổi tên làm mất back stack đã lưu và cài đặt cũ của người dùng.
- Không ghi token (Microsoft, Xbox, Minecraft), mật khẩu hay nội dung phản hồi mạng vào log. Không dùng `android.util.Log` trong module auth.
- Không yêu cầu hay lưu mật khẩu người dùng. Chỉ dùng OAuth chính thức.
- Chỉ dùng HTTPS. Không tắt xác thực chứng chỉ.
- Native library (`.so`, `.aar`, `.jar`): chỉ thêm sau khi kiểm tra ABI, checksum và nguồn gốc, và có người duyệt.
- Không commit file bí mật: `.oauth_client_id.txt`, `.store_password.txt`, `.key_password.txt`, `.curseforge_api.txt`, `xoyo_release.jks`.
- Không push trực tiếp lên `main` hoặc `zalith-base`. Mọi thay đổi đi qua nhánh và PR (mục 8).

---

## 3. Hợp đồng UI ↔ Logic

Dự án dùng Compose nên không có `id` view. Hợp đồng gồm bốn phần: API ViewModel, sự kiện `EventViewModel`, khóa cài đặt, và lớp NavKey. Mọi tên dưới đây là **bắt buộc giữ**. Chữ ký chi tiết của những hàm không ghi đủ tham số: xem file đã nêu.

### 3.1 Sự kiện: `EventViewModel` (`viewmodel/EventViewModel.kt`)

- Gửi sự kiện: `EventViewModel.sendEvent(event: Event)`. Có thêm các extension `sendKeepScreen`, `sendToast`, `sendDLPlugin` trong cùng file.
- Người xử lý sự kiện: A.I #2 (ViewModel hoặc Activity). A.I #1 chỉ gửi.

| Nhóm | Sự kiện và tham số |
|---|---|
| Key | `Key.StartKeyCapture`, `Key.StopKeyCapture`, `Key.OnKeyDown(key: KeyEvent)` |
| Game | `Game.KeyHandle(handle: Boolean)`, `Game.SwitchIme(mode: TextInputMode?)`, `Game.RefreshSize`, `Game.OnBack`, `Game.OnResume`, `Game.ToggleScreenRecording` |
| Terracotta | `Terracotta.RequestVPN`, `Terracotta.VPNUpdateState(stringRes: Int)`, `Terracotta.StopVPN` |
| Launch | `Launch.Game(version: Version?)`, `Launch.PlayServer(version: Version, address: String)`, `Launch.PlaySave(version: Version, saveName: String)` |
| Chung | `CheckUpdate`, `ShowSponsors`, `OpenLink(url: String)`, `KeepScreen(on: Boolean)`, `ImportControls(uris: List<Uri>)` |
| Plugin | `DownloadPlugins(link: Links)`, trong đó `Links(github: String, cloudDrives: List<CloudDrive>)` và `CloudDrive(language: String, link: String)` |
| Log | `LogShare.ShareGameLog(logFile: File)` |
| Vulkan | `VulkanCheck(version: Version)` |
| Thông báo | `ShowToast(text: AndroidStringText, duration: Int)`, `OpenFileManager(rootPath: String, currentPath: String?)` |
| Hướng dẫn | `Guide.StartGuide(group: GuideKeys.Keys)`, `Guide.StartGuideOnce(group: GuideKeys.Keys)` |

Thêm hoặc bớt sự kiện: sửa mục này trước, rồi mới sửa code.

### 3.2 API ViewModel và object (UI được gọi)

- **`LaunchGameViewModel`** (`viewmodel/LaunchGameViewModel.kt`)
  - `start(activity: Activity, version: Version, exitActivity: () -> Unit, submitError: (ErrorViewModel.ThrowableMessage) -> Unit, quickPlay: QuickPlay?, skipAccountRefresh: Boolean, ensureVulkanSupported: suspend (Version) -> Boolean = { true })`
  - `tryLaunch(version: Version? = null)`
  - `quickPlaySave(version: Version, saveName: String)`
  - `tryPlayServer(address: String)`, `quickPlayServer(version: Version, address: String)`
  - `cancel()`, `confirmUnsupportedRenderer()`, `dismissUnsupportedRenderer()`
  - Flow chỉ đọc: `launchFlow`, `launchGameOperation`

- **`AccountsManager`** (object, `game/account/AccountsManager.kt`)
  - Flow chỉ đọc: `accountsFlow`, `currentAccountFlow`, `authServersFlow`, `refreshWardrobe`
  - `initialize(context: Context)`, `reloadAccounts()`, `reloadAuthServers()`, `refreshWardrobe()`
  - `performLogin(context: Context, account: Account, onSuccess, onFailed)`
  - `refreshAccount(context: Context, account: Account, onFailed: (Throwable) -> Unit = {})`
  - `setCurrentAccount(account: Account)`, `saveAccount(account: Account)`, `deleteAccount(account: Account)`, `deleteAuthServer(server: AuthServer)`
  - `loadFromProfileID(profileId: String, accountType: String? = null): Account?`
  - `isAccountExists(uniqueUUID: String): Boolean`, `isAuthServerExists(baseUrl: String): Boolean`

- **`AccountManageViewModel`** (`viewmodel/AccountManageViewModel.kt`)
  - `onIntent(intent: AccountManageIntent)`. Intent đã có gồm `ApplyLocalCapeFile(account, uri)` và `ApplyLocalCape(account, file)`.
  - Flow chỉ đọc: `loginUiState`, `profileUiState`, `operationUiState`, `effect`.
  - Factory: `create(eventViewModel: EventViewModel)`.

- **`ScreenBackStackViewModel`** (`viewmodel/ScreenBackStackViewModel.kt`)
  - Thuộc tính điều hướng: `mainScreen`, `settingsScreen`, `downloadScreen`, `downloadGameScreen`, `downloadModPackScreen`, `downloadModScreen`, `downloadResourcePackScreen`, `downloadSavesScreen`, `downloadShadersScreen`, `downloadFavoritesScreen`, `clearBeforeNavKeys`.
  - Extension `ScreenBackStackViewModel.navigateToDownload(targetScreen: TitledNavKey? = null)` trong `ui/screens/content/DownloadScreen.kt`. Thuộc A.I #2.
  - `onBack(currentBackStack: NavBackStack<E>)` trong `ui/screens/_Navigation.kt`. Thuộc A.I #2. Đây là logic dùng chung cho nút Back hệ thống và nút quay lại trên thanh trên.

- **Preset hiệu năng** (`game/performance/PerformancePresets.kt`)
  - `enum PerformancePreset { LOW, BALANCED, HIGH, EXTREME }`
  - `recommendedRamMb(preset: PerformancePreset, deviceTotalMb: Long): Int`
  - `suggestPreset(deviceTotalMb: Long): PerformancePreset`
  - `data class CustomPreset(name: String, ramMb: Int, jvmArgs: String)`
  - `parseCustomPresets(json: String): List<CustomPreset>`, `encodeCustomPresets(presets: List<CustomPreset>): String`

- **Đăng nhập skin và cape từ web** (`game/account/wardrobe/`, thuộc A.I #2):
  - `findPlayerTextures(name: String): WebPlayerTextures` (suspend). Ném `PlayerNotFoundException(name)` khi không tìm thấy.
  - `downloadWebPng(url: String, target: File): WebImageKind` (suspend, chạy IO). Link `http://` được nâng lên `https://` trước khi tải. Link khác `http`/`https` ném `IllegalArgumentException`.
  - `enum class WebImageKind { SKIN, CAPE, INVALID }`
  - Hàm mở rộng trên `File` trong `WebSkinImages.kt`: `isPngFile()`, `isCapeImage()`, `classifyPngImage(): WebImageKind`.
  - `isValidMinecraftName(name: String): Boolean`. Ngoại lệ `WebImageInvalidException`.
  - UI gọi các hàm này trong `ui/screens/content/elements/WebSkinDialog.kt`.
- **Ghi hình, sao lưu**: UI chỉ gửi `Event.Game.ToggleScreenRecording`. `ScreenRecordService` chạy ở tiến trình `:game` và không được gọi trực tiếp từ UI. `WorldBackup` (`game/world/WorldBackup.kt`, hàm `backupWorldToZip`) là logic của A.I #2.

### 3.3 Khóa cài đặt (`setting/LauncherSettings.kt`)

Lưu bằng MMKV. UI đọc bằng `LauncherSettings.<thuộc tính>.state` và ghi bằng `.save(giá trị)`. Cột "khóa lưu" là chuỗi thật ghi trong kho dữ liệu. Không đổi.

| Thuộc tính | Khóa lưu | Kiểu | Mặc định |
|---|---|---|---|
| `launcherColorTheme` | `launcherColorTheme` | enum `ColorThemeType` | `XOYO` |
| `launcherCustomColor` | `launcherCustomColor` | Int | `Color.Blue.toArgb()` |
| `launcherCustomPaletteStyle` | `launcherCustomPaletteStyle` | enum `PaletteStyle` | `TonalSpot` |
| `launcherDarkMode` | `launcherDarkMode` | enum `DarkMode` | `FollowSystem` |
| `launcherLanguage` | `launcherLanguage` | enum `AppLanguage` | `FOLLOW_SYSTEM` |
| `launcherFullScreen` | `launcherFullScreen` | Boolean | `true` |
| `launcherFestivalEffects` | `launcherFestivalEffects` | Boolean | `true` |
| `launcherSwapAnimEnabled` | `launcherSwapAnimEnabled` | Boolean | `true` |
| `launcherAnimateSpeedLevel` | `launcherAnimateSpeedLevel` | Int, 1..30 | `10` |
| `launcherActionMenuSide` | `launcherActionMenuSide` | enum `ActionMenuSide` | `END` |
| `launcherTaskMenuExpanded` | `launcherTaskMenuExpanded` | Boolean | `true` |
| `launcherSidebarExpanded` | `launcherSidebarExpanded` | Boolean | `true` |
| `launcherReducedMotion` | `launcherReducedMotion` | Boolean | `false` |
| `launcherQuickActions` | `launcherQuickActions` | String, danh sách CSV | `CREATE_PROFILE,MANAGE_VERSIONS,INSTALL_MODS,INSTALL_SHADERS,INSTALL_RESOURCE_PACKS,MANAGE_WORLDS,PERFORMANCE,OPEN_GAME_FOLDER` |
| `performanceCustomPresets` | `performanceCustomPresets` | String, JSON | `[]` |
| `launcherBackgroundOpacity` | `launcherBackgroundOpacity` | Int, 20..100 | `80` |
| `videoBackgroundVolume` | `videoBackgroundVolume` | Int, 0..100 | `0` |
| `backgroundBlur` | `backgroundBlur` | Int, 0..40 | `0` |
| `backgroundBlurType` | `backgroundBlurType` | enum `BackgroundBlur` | `Background` |
| `launcherLogRetentionDays` | `launcherLogRetentionDays` | Int, 1..14 | `7` |
| `gameDownloadSource` | `gameDownloadSource` | enum `MirrorSourceType` (đọc tên cũ qua `LEGACY_NAMES`) | `AUTO` |
| `addonPlatformSource` | **`assetPlatformSource`** (khóa cũ) | enum `MirrorSourceType` (đọc tên cũ qua `LEGACY_NAMES`) | `AUTO` |

Thêm khóa mới: thêm vào bảng này trước, dùng giá trị mặc định an toàn. Không tái sử dụng khóa đã từng có.

### 3.4 Điều hướng: lớp NavKey (`ui/screens/`)

Các lớp này được `@Serializable`. Tên và tham số của chúng được lưu vào back stack. Đổi hoặc xóa sẽ làm mất back stack đã lưu.

- `NestedNavKey`: `Splash`, `Main`, `Settings`, `VersionSettings(version)`, `VersionExport(version)`, `Download`, `DownloadGame`, `DownloadModPack`, `DownloadMod`, `DownloadResourcePack`, `DownloadSaves`, `DownloadShaders`, `DownloadFavorites`, và lớp có tham số `platform`, `projectId`, `classes`.
- `NormalNavKey` (sealed interface): `LauncherMain`, `GameLibrary`, `VersionsManager`, `ContentCenter(kind: ContentKind)`, `WorldManager`, `ServerHub`, `AccountManager`, `PerformanceCenter`, `SearchId`, `Favorites`, `SearchMod`, `SearchModPack`, `SearchResourcePack`, `SearchSaves`, `SearchShaders`, `Settings.Renderer`, `Versions.OverView`, `VersionExports.SelectType`, `DownloadGame.SelectGameVersion`.

Thêm lớp mới được phép. Đổi hoặc xóa lớp đã có phải sửa mục này trước.

### 3.5 Khóa chuỗi (`res/values*/strings.xml`)

- Code không được hard-code chuỗi hiển thị. Dùng `R.string.<khóa>`.
- Trước khi xóa hoặc đổi tên một khóa, A.I #1 phải `grep` toàn bộ `R.string.<khóa>` trong `java/` và `ui/` để xác nhận không còn tham chiếu.
- Mục tiêu: mọi khóa trong `values/` đều có trong `values-vi/`.

---

## 4. Native, build, CI

- Native library: mục 2.4.
- CI, `.github/workflows/xoyo-ci.yml`:
  - Job build chạy `./gradlew testDebugUnitTest assembleDebug -Darch=arm64 --stacktrace`.
  - Secret `XOYO_MS_CLIENT_ID` được truyền thành env `OAUTH_CLIENT_ID`.
  - Job build có bước kiểm tra APK: không có entry trùng tên, `apksigner verify`, `aapt2 dump badging`.
  - Chạy `gh workflow run xoyo-ci.yml --ref <nhánh> -f publish=true` để đăng pre-release. Tag có dạng `debug-<run>-<sha7>`.
- Mọi PR phải xanh CI trước khi merge.
- Cấu hình ký: `xoyo_release.jks` đặt cạnh `ZalithLauncher/build.gradle.kts`, không commit. Nếu không có file này, bản release không được ký.

---

## 5. Tính năng hiện có phải giữ

Đối chiếu với code tại commit `60117b7`, ngày 2026-11-22.

- Đăng nhập Microsoft và các loại tài khoản khác (`game/account/`).
- Menu tài khoản: chọn cape từ file (chỉ tài khoản offline), chọn skin hoặc cape từ web (`ApplyLocalCapeFile`, `ApplyLocalCape`).
- Ghi hình màn hình trong game (`Game.ToggleScreenRecording`, `ScreenRecordService`).
- Phát hiện giật hình và gợi ý preset (`StutterDetector`).
- Preset hiệu năng có sẵn và preset tùy chỉnh (`PerformancePresets`, `performanceCustomPresets`).
- Sao lưu thế giới thành zip (`WorldBackup`, `WorldManagerScreen`).
- Sidebar:
  - Chính: Trang chủ, Thư viện, Phiên bản.
  - Nội dung: Trò chơi, Mod, Modpack, Gói tài nguyên, Shader, Thế giới, Tìm kiếm theo ID, Yêu thích.
  - Tải về: Tải mod, Tải modpack, Tải gói tài nguyên, Tải shader, Tải thế giới.
  - Kết nối: Máy chủ, Tài khoản.
  - Hệ thống: Hiệu năng, Cài đặt.
  - Các nhóm Nội dung, Tải về, Kết nối, Hệ thống thu gọn thành một nút. Cùng cách này áp dụng cho sheet "Thêm" trên màn hình hẹp.
- Nút quay lại trên thanh trên dùng `onBack`, không thoát launcher.
- Thao tác nhanh trên trang chủ (`launcherQuickActions`).
- Cài đặt ngôn ngữ, chủ đề, hiệu ứng lễ hội, giảm chuyển động (`launcherReducedMotion`).
- Import điều khiển cảm ứng (`ImportControls`).
- Thanh cuộn cho sidebar khi danh sách dài (`verticalScrollWithBar`).
- Ảnh nền theo thế hệ phiên bản trong danh sách phiên bản (`drawable-nodpi/img_ver_*.jpg`), và xem trước skin nằm bên phải hộp thoại.
- Tìm skin theo tên: link `http` từ Mojang được nâng lên `https`.
- Danh sách mod đã cài, truy cập từ mục Mod.

Đã bỏ có chủ ý, theo yêu cầu người dùng:
- Nút "Tìm và tải về" trong Trung tâm nội dung và "Tải thế giới về" trong Quản lý thế giới (commit `60117b7`).
- Nút Tải xuống trên thanh trên và thanh danh mục bên trái (từ vòng trước).

---

## 6. Nợ kỹ thuật đã biết

Không phải lỗi phải sửa ngay. Ghi để hai A.I không làm trùng hoặc làm hỏng.

- Đã xong: `values-vi` đủ khóa so với `values` (merge `ui/vi-localization`, commit `3dc88eb`).
- Đã xong: `content_install` và `world_download_action` đã xóa, sau khi `grep` xác nhận không còn tham chiếu.
- `docs/ARCHITECTURE.md` mô tả cấu trúc chưa tồn tại. Cập nhật theo code thật, và sửa tên biến client ID thành `OAUTH_CLIENT_ID` (A.I #2, người duyệt).
- Logic còn nằm trong file UI: `scanInstalled` và `backupWorld`. Chuyển sang `game/` là việc của A.I #2, và phải công bố qua mục 3. Cho đến khi chuyển, A.I #1 không đổi logic bên trong hai hàm này.
- Khóa `content_modpack_note` đã đổi nội dung để trỏ đến mục "Tải modpack" trên sidebar.

---

## 7. Yêu cầu chuyển giao và câu hỏi mở

Ghi theo mẫu: `[ngày] [A.I #n] cần: ... lý do: ...`. Người duyệt trả lời ngay dưới mục.

Câu hỏi mở cho người duyệt:
1. Menu tài khoản: người dùng nói "3 tính năng" nhưng mới nêu 2. Tính năng thứ ba là gì?
2. `PetLib` (module Rust): mục đích là gì và giao cho A.I nào? Hiện để ở mục 2.3.
3. Có chấp nhận quy trình nhánh và PR ở mục 8 không?
4. Nhánh tích hợp `integrate/spec-merge` (đã xanh CI, gồm `ui/vi-localization`, `fix/ui-scroll-skin-mod`, `feat/version-banners-skin-preview`) có nên thành `zalith-base` không?
5. Mục Mod: giữ danh sách mod đã cài (đang chọn), hay chuyển thẳng sang trang tải như nhánh `fix/ui-scroll-skin-mod`?
6. Có nên đổi nhánh mặc định `main` (khung dự án khác) để trỏ về nhánh có code Zalith không?

---

## 8. Quy trình làm việc

1. Đọc SPEC trước khi sửa code. Nếu việc cần đổi mục 2 hoặc mục 3, đề xuất đổi SPEC trước bằng một PR riêng với tiền tố commit `[SPEC]`. Người duyệt merge, rồi mới làm code.
2. Đặt tên nhánh: A.I #1 dùng `ui/<mô-tả>`, A.I #2 dùng `logic/<mô-tả>`. Merge vào `zalith-base` qua PR.
3. Tiền tố commit bắt buộc: `[UI]`, `[LOGIC]`, `[SPEC]`.
4. Trước khi mở PR: `./gradlew testDebugUnitTest assembleDebug -Darch=arm64` phải xanh. Mô tả PR liệt kê file đã sửa và xác nhận không chạm ngoài mục 2.
5. Khi cả hai A.I cần cùng một file ở mục 2.1 (`AppNavigation.kt`): báo nhau qua mục 7 trước khi sửa. Chỉ một người sửa tại một thời điểm.
6. Khi không chắc: dừng, ghi mục 7, không đoán.

---

## Phụ lục A: cấu trúc đã quét (commit `60117b7`)

```
/
├── ZalithLauncher/          app chính (Compose, Kotlin, JNI)
│   ├── src/main/java/com/movtery/zalithlauncher/
│   │   ├── game/            351 file: account, launch, download, version, renderer, record, performance, world, ...
│   │   ├── ui/              screens 169, components 34, control 34, theme 23, activities 6, base 5, guide 4, code_editor 4
│   │   ├── setting/         33 file (LauncherSettings, MMKV)
│   │   ├── viewmodel/       18 file
│   │   └── utils/           44 file
│   ├── src/main/res/        drawable*, mipmap-*, values* (vi, en, ar, es, ja, ko, ru, zh, ...), layout (1 file), xml
│   ├── src/main/jni/        80 file (C, H, mk, cpp; ndk-build)
│   ├── src/main/jniLibs/    43 file
│   ├── src/test/            41 file test, 14 file resource
│   └── libs/                .aar và .jar (lwjgl, mobileglues, sdl, openal, opennbt, ...)
├── LWJGL/                   94 file (patches, 3.3.3, 3.4.1)
├── LayerController/         58 file
├── PetLib/                  30 file (có Rust)
├── Guide/                   26 file
├── CardGrid/, ColorPicker/, InputMap/, Terracotta/
├── docs/                    ARCHITECTURE.md (cũ, xem mục 0), research/
├── .github/workflows/       xoyo-ci.yml
└── settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, gradle.properties
```
