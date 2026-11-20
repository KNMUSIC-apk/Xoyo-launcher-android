package com.xoyo.launcher.auth

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

/**
 * Đăng nhập Microsoft bằng luồng device-code chính thức, rồi đổi sang phiên Minecraft.
 *
 * Mọi hàm đều chặn luồng gọi và thực hiện I/O mạng: chỉ gọi từ luồng nền.
 * Không ghi token (Microsoft, Xbox, Minecraft) vào log hoặc vào thông báo lỗi.
 */
class MicrosoftAuthenticator(private val clientId: String) {

    init {
        require(clientId.isNotBlank()) { "Thiếu client ID Microsoft (XOYO_MS_CLIENT_ID)" }
    }

    /** Bước 1: xin mã thiết bị. Người dùng nhập mã đó tại [DeviceCode.verificationUri]. */
    fun requestDeviceCode(): DeviceCode {
        val res = postForm(
            url = "$MS_AUTH/devicecode",
            fields = mapOf("client_id" to clientId, "scope" to SCOPES),
        )
        if (res.status != 200) {
            throw AuthException("Không xin được mã đăng nhập (HTTP ${res.status}).")
        }
        val json = parseObject(res.body)
        return DeviceCode(
            deviceCode = json.requireString("device_code"),
            userCode = json.requireString("user_code"),
            verificationUri = json.requireString("verification_uri"),
            expiresInSeconds = json.optLong("expires_in", DEFAULT_EXPIRES_SECONDS),
            intervalSeconds = json.optLong("interval", MIN_POLL_SECONDS),
        )
    }

    /**
     * Bước 2: chờ người dùng xác nhận trên trình duyệt, rồi nhận token Microsoft.
     * Tôn trọng [DeviceCode.intervalSeconds] và tăng khoảng chờ khi Microsoft yêu cầu "slow_down".
     */
    fun awaitMicrosoftTokens(code: DeviceCode): MicrosoftTokens {
        val deadline = System.currentTimeMillis() + code.expiresInSeconds * 1000
        var intervalMs = code.intervalSeconds.coerceAtLeast(MIN_POLL_SECONDS) * 1000

        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(intervalMs)
            val res = postForm(
                url = "$MS_AUTH/token",
                fields = mapOf(
                    "grant_type" to DEVICE_CODE_GRANT,
                    "client_id" to clientId,
                    "device_code" to code.deviceCode,
                ),
            )
            if (res.status == 200) return parseTokens(res.body)

            when (errorCode(res.body)) {
                "authorization_pending" -> Unit
                "slow_down" -> intervalMs += SLOW_DOWN_STEP_MS
                "authorization_declined" -> throw AuthException("Bạn đã từ chối đăng nhập.")
                "expired_token" -> break
                else -> throw AuthException("Đăng nhập Microsoft thất bại (HTTP ${res.status}).")
            }
        }
        throw AuthException("Hết thời gian chờ đăng nhập. Vui lòng thử lại.")
    }

    /** Làm mới token Microsoft bằng refresh token. Lỗi nghĩa là người dùng phải đăng nhập lại. */
    fun refreshMicrosoftTokens(refreshToken: String): MicrosoftTokens {
        val res = postForm(
            url = "$MS_AUTH/token",
            fields = mapOf(
                "grant_type" to "refresh_token",
                "client_id" to clientId,
                "refresh_token" to refreshToken,
                "scope" to SCOPES,
            ),
        )
        if (res.status != 200) {
            throw AuthException("Không làm mới được phiên Microsoft. Hãy đăng nhập lại.")
        }
        return parseTokens(res.body)
    }

    /** Bước 3: đổi token Microsoft sang phiên Minecraft và kiểm tra quyền sở hữu game. */
    fun authenticateMinecraft(ms: MicrosoftTokens): MinecraftSession {
        val xbl = xblAuthenticate(ms.accessToken)
        val xstsToken = xstsAuthorize(xbl.token)
        val mc = loginMinecraft(XboxProtocol.identityToken(xbl.uhs, xstsToken))
        ensureOwnsMinecraft(mc.accessToken)
        val profile = fetchProfile(mc.accessToken)
        return MinecraftSession(
            profileId = profile.id,
            username = profile.name,
            accessToken = mc.accessToken,
            expiresAtMillis = mc.expiresAtMillis,
        )
    }

    private data class XboxAuth(val token: String, val uhs: String)
    private data class McAuth(val accessToken: String, val expiresAtMillis: Long)
    private data class Profile(val id: String, val name: String)

    private fun xblAuthenticate(msAccessToken: String): XboxAuth {
        val body = JSONObject()
            .put(
                "Properties",
                JSONObject()
                    .put("AuthMethod", "RPS")
                    .put("SiteName", "user.auth.xboxlive.com")
                    .put("RpsTicket", XboxProtocol.rpsTicket(msAccessToken)),
            )
            .put("RelyingParty", "http://auth.xboxlive.com")
            .put("TokenType", "JWT")

        val res = postJson("$XBL_AUTH/user/authenticate", body.toString())
        if (res.status != 200) {
            throw AuthException("Xbox Live từ chối xác thực (HTTP ${res.status}).")
        }
        val json = parseObject(res.body)
        val uhs = json.requireObject("DisplayClaims")
            .requireArray("xui")
            .getJSONObject(0)
            .requireString("uhs")
        return XboxAuth(token = json.requireString("Token"), uhs = uhs)
    }

    private fun xstsAuthorize(xblToken: String): String {
        val body = JSONObject()
            .put(
                "Properties",
                JSONObject()
                    .put("SandboxId", "RETAIL")
                    .put("UserTokens", JSONArray().put(xblToken)),
            )
            .put("RelyingParty", "rp://api.minecraftservices.com/")
            .put("TokenType", "JWT")

        val res = postJson("$XSTS_AUTH/xsts/authorize", body.toString())
        if (res.status != 200) {
            // XSTS trả về mã XErr trong body khi lỗi thuộc về tài khoản.
            val xErr = runCatching {
                parseObject(res.body).optLong("XErr", -1L).takeIf { it > 0 }
            }.getOrNull()
            throw XboxAccountException(XboxProtocol.errorMessage(xErr))
        }
        return parseObject(res.body).requireString("Token")
    }

    private fun loginMinecraft(identityToken: String): McAuth {
        val body = JSONObject().put("identityToken", identityToken)
        val res = postJson("$MC_SERVICES/authentication/login_with_xbox", body.toString())
        when (res.status) {
            200 -> Unit
            429 -> throw AuthException("Yêu cầu quá nhiều. Hãy thử lại sau ít phút.")
            else -> throw AuthException("Đăng nhập Minecraft thất bại (HTTP ${res.status}).")
        }
        val json = parseObject(res.body)
        val expiresIn = json.optLong("expires_in", DEFAULT_MC_EXPIRES_SECONDS)
        return McAuth(
            accessToken = json.requireString("access_token"),
            expiresAtMillis = System.currentTimeMillis() + expiresIn * 1000,
        )
    }

    private fun ensureOwnsMinecraft(mcAccessToken: String) {
        val res = get("$MC_SERVICES/entitlements/mcstore", mcAccessToken)
        if (res.status != 200) {
            throw AuthException("Không kiểm tra được quyền sở hữu Minecraft (HTTP ${res.status}).")
        }
        val items = parseObject(res.body).optJSONArray("items")
        if (items == null || items.length() == 0) throw NotOwnedMinecraftException()
    }

    private fun fetchProfile(mcAccessToken: String): Profile {
        val res = get("$MC_SERVICES/minecraft/profile", mcAccessToken)
        if (res.status != 200) {
            throw AuthException("Không lấy được hồ sơ Minecraft (HTTP ${res.status}).")
        }
        val json = parseObject(res.body)
        return Profile(id = json.requireString("id"), name = json.requireString("name"))
    }

    private fun parseTokens(body: String): MicrosoftTokens {
        val json = parseObject(body)
        val expiresIn = json.optLong("expires_in", DEFAULT_MS_EXPIRES_SECONDS)
        return MicrosoftTokens(
            accessToken = json.requireString("access_token"),
            refreshToken = json.requireString("refresh_token"),
            expiresAtMillis = System.currentTimeMillis() + expiresIn * 1000,
        )
    }

    private fun errorCode(body: String): String? =
        runCatching { JSONObject(body).optString("error", "").ifEmpty { null } }.getOrNull()

    private fun parseObject(body: String): JSONObject =
        try {
            JSONObject(body)
        } catch (e: JSONException) {
            throw AuthException("Phản hồi xác thực không hợp lệ.", e)
        }

    private fun JSONObject.requireString(key: String): String {
        val value = optString(key, "")
        if (value.isEmpty()) throw AuthException("Phản hồi xác thực thiếu trường $key.")
        return value
    }

    private fun JSONObject.requireObject(key: String): JSONObject =
        optJSONObject(key) ?: throw AuthException("Phản hồi xác thực thiếu trường $key.")

    private fun JSONObject.requireArray(key: String): JSONArray =
        optJSONArray(key) ?: throw AuthException("Phản hồi xác thực thiếu trường $key.")

    private class HttpResult(val status: Int, val body: String)

    private fun postForm(url: String, fields: Map<String, String>): HttpResult {
        val encoded = fields.entries.joinToString("&") { (k, v) -> "${enc(k)}=${enc(v)}" }
        return send(url, "POST", "application/x-www-form-urlencoded", encoded, emptyMap())
    }

    private fun postJson(url: String, json: String): HttpResult =
        send(url, "POST", "application/json", json, emptyMap())

    private fun get(url: String, bearerToken: String): HttpResult =
        send(url, "GET", null, null, mapOf("Authorization" to "Bearer $bearerToken"))

    private fun send(
        url: String,
        method: String,
        contentType: String?,
        body: String?,
        headers: Map<String, String>,
    ): HttpResult {
        val conn = URI.create(url).toURL().openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.setRequestProperty("Accept", "application/json")
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (body != null && contentType != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", contentType)
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.use { it.bufferedReader().readText() }.orEmpty()
            return HttpResult(status, text)
        } catch (e: IOException) {
            throw AuthException("Lỗi kết nối mạng khi xác thực. Kiểm tra kết nối rồi thử lại.", e)
        } finally {
            conn.disconnect()
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val MS_AUTH = "https://login.microsoftonline.com/consumers/oauth2/v2.0"
        const val XBL_AUTH = "https://user.auth.xboxlive.com"
        const val XSTS_AUTH = "https://xsts.auth.xboxlive.com"
        const val MC_SERVICES = "https://api.minecraftservices.com"
        const val SCOPES = "XboxLive.signin offline_access"
        const val DEVICE_CODE_GRANT = "urn:ietf:params:oauth:grant-type:device_code"
        const val TIMEOUT_MS = 15_000
        const val DEFAULT_EXPIRES_SECONDS = 900L
        const val MIN_POLL_SECONDS = 5L
        const val SLOW_DOWN_STEP_MS = 5_000L
        const val DEFAULT_MS_EXPIRES_SECONDS = 3600L
        const val DEFAULT_MC_EXPIRES_SECONDS = 86400L
    }
}
