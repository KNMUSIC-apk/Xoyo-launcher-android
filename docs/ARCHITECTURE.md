# Kiến trúc XOYO Launcher

Tài liệu này mô tả cấu trúc hiện tại và hướng phát triển. Phần nào chưa cài đặt được ghi rõ là **thiết kế**.

## Trạng thái các lớp

| Lớp | Trách nhiệm | Trạng thái |
|---|---|---|
| `auth` | Đăng nhập Microsoft (device-code), lưu token mã hóa | Đã cài, có test logic thuần |
| `renderer` | Đăng ký renderer, chọn renderer có phương án dự phòng | Đã cài, có test |
| `ui` | Màn hình, điều hướng | Chưa có (chỉ có `MainActivity` tối thiểu) |
| `launch` | Chuẩn bị và khởi chạy game | Chưa có |
| `settings` | Tùy chọn người dùng | Chưa có |
| `touchcontrols` | Layout và editor điều khiển cảm ứng | Thiết kế (xem bên dưới) |

## Đăng nhập Microsoft (`auth`)

Luồng dùng device-code chính thức của Microsoft. Ứng dụng không bao giờ nhận mật khẩu người dùng.

1. `requestDeviceCode()`: lấy `userCode` và `verificationUri`. Hiển thị cho người dùng.
2. `awaitMicrosoftTokens(code)`: chạy trên luồng nền. Tự polling theo `interval`, xử lý `authorization_pending`, `slow_down`, `authorization_declined`, `expired_token`.
3. Lưu `refreshToken` bằng `SecureTokenStore`.
4. `authenticateMinecraft(tokens)`: chuỗi đổi token gồm Xbox Live (`RpsTicket` với tiền tố `d=`), XSTS (`RETAIL`, `rp://api.minecraftservices.com/`), đăng nhập Minecraft (`login_with_xbox`), kiểm tra `entitlements/mcstore`, rồi lấy `minecraft/profile`.
5. Khi token hết hạn: `refreshMicrosoftTokens(refreshToken)`. Nếu thất bại, yêu cầu người dùng đăng nhập lại.

Lỗi XSTS được ánh xạ sang thông báo tiếng Việt qua `XboxProtocol.errorMessage` (ví dụ: chưa có hồ sơ Xbox, tài khoản trẻ em).

**Lưu trữ token:** `SecureTokenStore` mã hóa bằng AES-256-GCM. Khóa nằm trong Android Keystore và không rời thiết bị. Nếu không giải mã được (ví dụ khóa đã mất sau khi cài lại), mục đó bị xóa và trả về `null`.

## Renderer (`renderer`)

- `GameRenderer` mô tả renderer: `id`, ABI CPU được hỗ trợ, có cần Vulkan không.
- `RendererRegistry.resolve(preferredId, deviceAbis, hasVulkan)` trả về renderer người dùng chọn nếu tương thích. Nếu không, trả về `DefaultGlRenderer`.
- Renderer dự phòng phải chạy được trên mọi thiết bị. Điều này được kiểm tra khi khởi tạo registry.

**Chưa nạp native library nào.** Khi thêm renderer có file `.so`, cần kiểm tra ABI, checksum và nguồn gốc trước (chính sách XOYO, mục 6).

## Điều khiển cảm ứng (thiết kế)

Mục tiêu: người dùng tự sắp xếp nút và cần điều khiển trên màn hình, rồi lưu lại và dùng lại.

- **Layout** là danh sách `Control`. Mỗi control có: `id`, loại (nút, cần analog, vùng chuột...), vị trí và kích thước theo **tỷ lệ 0..1** so với màn hình, lớp hiển thị, và hành động gắn với phím hoặc chuột.
- Tọa độ theo tỷ lệ giúp một layout chạy được trên nhiều độ phân giải.
- **Lưu trữ:** JSON trong thư mục dữ liệu của app, có trường `schemaVersion` để nâng cấp về sau.
- **Editor** (chưa làm): kéo thả, tự lưu, kiểm tra control chồng lấn hoặc nằm ngoài màn hình.
- Ý tưởng tham khảo từ Zalith (`ControlManager`, `ControlData`, module `LayerController`). Mã được viết mới, không sao chép.

## Quy tắc bảo mật

Các quy tắc này lấy từ chính sách XOYO và đã được áp dụng trong code:

- Không ghi token (Microsoft, Xbox, Minecraft) vào log. Module `auth` không dùng `android.util.Log`.
- Không lưu hay yêu cầu mật khẩu. Chỉ dùng OAuth chính thức.
- Token lưu bằng AES-GCM với khóa trong Android Keystore.
- Chỉ dùng HTTPS. Không tắt xác thực chứng chỉ.
- Thông báo lỗi không chứa nội dung phản hồi mạng, vì có thể lộ token.
- Client ID đọc từ `XOYO_MS_CLIENT_ID` (không cố định trong code). Client ID là public, không có secret.
- Native library chỉ nạp sau khi kiểm tra ABI, checksum và nguồn gốc.

## Việc tiếp theo

1. Màn hình đăng nhập: hiển thị mã, gọi luồng nền, lưu kết quả.
2. Gắn `SecureTokenStore` vào vòng đời app và làm mới token khi khởi động.
3. Lớp `launch`: tải phiên bản game, khởi chạy JVM.
4. Editor điều khiển cảm ứng theo mô hình trên.
