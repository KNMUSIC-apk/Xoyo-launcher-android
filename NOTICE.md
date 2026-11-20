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
- [ ] Hiển thị "Unofficial Modified Version" (chưa làm).
- [x] Giữ nguyên thông báo bản quyền trong các file đã import.

## Không kế thừa từ upstream

- Legal docs (điều khoản người dùng, chính sách quyền riêng tư) của upstream. Các tài liệu này không thuộc GPL và không được đưa vào bản chỉnh sửa.
- Keystore phát hành của upstream (`zalith_launcher.jks`, `zalith_launcher_debug.jks`) và mật khẩu đi kèm.
- Client ID OAuth của upstream. XOYO dùng client ID đăng ký riêng.
- Workflow CI của upstream.

## Thay đổi so với upstream

- `ZalithLauncher/gradle.properties`: đổi tên, URL, phiên bản; bỏ mật khẩu keystore và `legal_docs_ref`.
- `ZalithLauncher/build.gradle.kts`: đổi application ID thành `com.xoyo.launcher`; cấu hình ký chuyển sang `xoyo_release.jks` (không bắt buộc); bản debug dùng khóa debug mặc định.
- `.github/workflows/xoyo-ci.yml`: CI riêng của XOYO.

## Thư viện bên thứ ba

Các thư viện và thành phần native đi kèm (thư mục `libs/`, `ZalithLauncher/src/main/jni/`, `LWJGL/`) giữ nguyên giấy phép và thông báo bản quyền của chúng. Cần rà soát lại danh sách này trước khi phát hành.
