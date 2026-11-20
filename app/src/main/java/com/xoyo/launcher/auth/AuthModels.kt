package com.xoyo.launcher.auth

/** Mã thiết bị từ bước device-code của Microsoft. Người dùng nhập [userCode] tại [verificationUri]. */
data class DeviceCode(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresInSeconds: Long,
    val intervalSeconds: Long,
)

/**
 * Token Microsoft. [accessToken] và [refreshToken] là thông tin nhạy cảm:
 * không ghi log, và chỉ lưu qua [SecureTokenStore].
 */
data class MicrosoftTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long,
)

/** Phiên Minecraft đã xác minh quyền sở hữu game. [accessToken] là thông tin nhạy cảm. */
data class MinecraftSession(
    val profileId: String,
    val username: String,
    val accessToken: String,
    val expiresAtMillis: Long,
)

/** Lỗi xác thực. [message] là thông báo an toàn để hiển thị cho người dùng. */
open class AuthException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Tài khoản Xbox có vấn đề (chưa có hồ sơ Xbox, bị hạn chế theo khu vực, v.v.). */
class XboxAccountException(message: String) : AuthException(message)

/** Tài khoản Microsoft đăng nhập được nhưng chưa mua Minecraft Java Edition. */
class NotOwnedMinecraftException :
    AuthException("Tài khoản này chưa sở hữu Minecraft Java Edition.")
