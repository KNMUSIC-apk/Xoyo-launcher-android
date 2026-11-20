package com.xoyo.launcher.renderer

/**
 * Danh sách renderer và logic chọn renderer.
 *
 * Nếu renderer người dùng chọn không tồn tại hoặc không tương thích với thiết bị,
 * registry luôn quay về [fallback]. Renderer dự phòng phải chạy được trên mọi thiết bị.
 */
class RendererRegistry(
    private val renderers: List<GameRenderer>,
    private val fallback: GameRenderer = DefaultGlRenderer,
) {
    init {
        require(renderers.map { it.id }.toSet().size == renderers.size) {
            "Có renderer trùng id"
        }
        require(fallback.supportedAbis.isEmpty() && !fallback.requiresVulkan) {
            "Renderer dự phòng phải tương thích mọi thiết bị"
        }
    }

    fun resolve(preferredId: String?, deviceAbis: List<String>, hasVulkan: Boolean): GameRenderer {
        val preferred = renderers.firstOrNull { it.id == preferredId }
        return if (preferred != null && preferred.isCompatible(deviceAbis, hasVulkan)) {
            preferred
        } else {
            fallback
        }
    }
}
