package com.xoyo.launcher.renderer

/**
 * Một renderer có thể chọn để chạy game.
 * Renderer chỉ được dùng khi thiết bị khớp ABI CPU và các yêu cầu khác (ví dụ Vulkan).
 */
interface GameRenderer {
    val id: String
    val displayName: String

    /** ABI CPU được hỗ trợ. Tập rỗng nghĩa là mọi ABI. */
    val supportedAbis: Set<String>

    val requiresVulkan: Boolean

    fun isCompatible(deviceAbis: List<String>, hasVulkan: Boolean): Boolean {
        val abiOk = supportedAbis.isEmpty() || deviceAbis.any { it in supportedAbis }
        val vulkanOk = !requiresVulkan || hasVulkan
        return abiOk && vulkanOk
    }
}

/** Renderer mặc định, chạy được trên mọi thiết bị. Luôn là phương án dự phòng. */
object DefaultGlRenderer : GameRenderer {
    override val id: String = "gl_default"
    override val displayName: String = "OpenGL ES (mặc định)"
    override val supportedAbis: Set<String> = emptySet()
    override val requiresVulkan: Boolean = false
}
