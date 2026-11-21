/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.game.account.wardrobe

import com.google.gson.JsonObject
import com.movtery.zalithlauncher.utils.GSON
import com.movtery.zalithlauncher.utils.network.downloadFile
import com.movtery.zalithlauncher.utils.network.fetchStringFromUrl
import com.movtery.zalithlauncher.utils.string.decodeBase64
import java.io.File
import java.io.IOException

/** Skin và cape của một người chơi, đọc từ API công khai của Mojang */
data class WebPlayerTextures(
    val playerName: String,
    val skinUrl: String?,
    val skinSlim: Boolean,
    val capeUrl: String?,
)

/** Không có người chơi nào mang tên này */
class PlayerNotFoundException(name: String) : Exception("Player not found: $name")

/** Ảnh tải từ link không phải skin hay cape hợp lệ */
class WebImageInvalidException : Exception("Not a skin or cape image")

private const val MOJANG_PROFILE_API = "https://api.mojang.com/users/profiles/minecraft/"
private const val MOJANG_SESSION_API = "https://sessionserver.mojang.com/session/minecraft/profile/"

/** Skin và cape rất nhỏ, ảnh lớn hơn mức này được coi là không hợp lệ */
private const val MAX_WEB_IMAGE_BYTES = 1024L * 1024L

private val MINECRAFT_NAME_REGEX = Regex("[A-Za-z0-9_]{3,16}")

/** Tên người chơi hợp lệ: 3 đến 16 ký tự chữ, số hoặc dấu gạch dưới */
fun isValidMinecraftName(name: String): Boolean = MINECRAFT_NAME_REGEX.matches(name)

/**
 * Tra cứu người chơi theo tên, rồi đọc link skin và cape trong hồ sơ của họ.
 * Chạy trên luồng IO.
 * @throws PlayerNotFoundException khi tên không tồn tại
 * @throws IOException khi lỗi mạng hoặc dữ liệu trả về không đúng định dạng
 */
suspend fun findPlayerTextures(name: String): WebPlayerTextures {
    val profileJson = try {
        fetchStringFromUrl(MOJANG_PROFILE_API + name)
    } catch (e: IOException) {
        // Mojang trả 404 khi không có người chơi này
        if (e.message?.contains("HTTP 404") == true) throw PlayerNotFoundException(name)
        throw e
    }
    // Mojang trả 204 (nội dung rỗng) khi tên không tồn tại
    if (profileJson.isBlank()) throw PlayerNotFoundException(name)

    val profile = GSON.fromJson(profileJson, JsonObject::class.java)
    val uuid = profile.get("id")?.asString ?: throw PlayerNotFoundException(name)
    val canonicalName = profile.get("name")?.asString ?: name

    val sessionJson = fetchStringFromUrl(MOJANG_SESSION_API + uuid)
    val session = GSON.fromJson(sessionJson, JsonObject::class.java)
    val rawTextures = session.getAsJsonArray("properties")
        ?.firstOrNull()
        ?.asJsonObject
        ?.get("value")
        ?.asString
        ?: throw IOException("Missing textures property")

    val textures = GSON.fromJson(decodeBase64(rawTextures), JsonObject::class.java)
        .getAsJsonObject("textures")
    val skin = textures?.getAsJsonObject("SKIN")
    val cape = textures?.getAsJsonObject("CAPE")
    return WebPlayerTextures(
        playerName = canonicalName,
        skinUrl = skin?.get("url")?.asString,
        skinSlim = skin?.getAsJsonObject("metadata")?.get("model")?.asString == "slim",
        capeUrl = cape?.get("url")?.asString
    )
}

/**
 * Tải một ảnh PNG từ link https vào [target], rồi phân loại theo kích thước.
 * Ảnh sai định dạng hoặc quá lớn sẽ bị xoá và trả về INVALID.
 * Chạy trên luồng IO.
 * @throws IllegalArgumentException khi link không phải https
 */
suspend fun downloadWebPng(url: String, target: File): WebImageKind {
    require(url.startsWith("https://")) { "Only https links are allowed" }
    target.parentFile?.mkdirs()
    target.delete()
    downloadFile(url = url, outputFile = target)
    if (!target.exists()) return WebImageKind.INVALID
    if (target.length() > MAX_WEB_IMAGE_BYTES) {
        target.delete()
        return WebImageKind.INVALID
    }
    val kind = target.classifyPngImage()
    if (kind == WebImageKind.INVALID) target.delete()
    return kind
}
