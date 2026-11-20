package com.xoyo.launcher.auth

/** Các quy ước của Xbox Live, XSTS và Minecraft Services. Không có I/O, dễ kiểm thử. */
object XboxProtocol {

    const val XERR_NO_XBOX_ACCOUNT = 2148916233L
    const val XERR_XBOX_UNAVAILABLE_IN_COUNTRY = 2148916235L
    const val XERR_ADULT_VERIFICATION_KR = 2148916236L
    const val XERR_ADULT_VERIFICATION = 2148916237L
    const val XERR_CHILD_ACCOUNT = 2148916238L

    /** RpsTicket cho token Microsoft v2 cần tiền tố "d=". */
    fun rpsTicket(microsoftAccessToken: String): String = "d=$microsoftAccessToken"

    /** Chuỗi identityToken gửi tới Minecraft Services sau khi có XSTS. */
    fun identityToken(uhs: String, xstsToken: String): String = "XBL3.0 x=$uhs;$xstsToken"

    /** Đổi mã lỗi XErr của XSTS thành thông báo tiếng Việt. */
    fun errorMessage(xErr: Long?): String = when (xErr) {
        XERR_NO_XBOX_ACCOUNT ->
            "Tài khoản Microsoft chưa có hồ sơ Xbox. Hãy tạo hồ sơ Xbox rồi thử lại."
        XERR_XBOX_UNAVAILABLE_IN_COUNTRY ->
            "Xbox Live không khả dụng tại quốc gia hoặc khu vực của tài khoản."
        XERR_ADULT_VERIFICATION, XERR_ADULT_VERIFICATION_KR ->
            "Tài khoản cần xác minh độ tuổi trên trang Xbox."
        XERR_CHILD_ACCOUNT ->
            "Tài khoản trẻ em cần được thêm vào nhóm gia đình để đăng nhập."
        null -> "Xbox Live từ chối đăng nhập."
        else -> "Xbox Live từ chối đăng nhập (mã $xErr)."
    }
}
