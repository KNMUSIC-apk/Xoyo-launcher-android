package com.xoyo.launcher.renderer

import org.junit.Assert.assertEquals
import org.junit.Test

class RendererRegistryTest {

    private class FakeRenderer(
        override val id: String,
        override val supportedAbis: Set<String> = emptySet(),
        override val requiresVulkan: Boolean = false,
    ) : GameRenderer {
        override val displayName: String = id
    }

    private val vulkanArm = FakeRenderer(
        id = "vk_arm",
        supportedAbis = setOf("arm64-v8a"),
        requiresVulkan = true,
    )
    private val registry = RendererRegistry(listOf(vulkanArm))

    @Test
    fun usesPreferredWhenCompatible() {
        val result = registry.resolve("vk_arm", listOf("arm64-v8a"), hasVulkan = true)
        assertEquals("vk_arm", result.id)
    }

    @Test
    fun fallsBackWhenAbiUnsupported() {
        val result = registry.resolve("vk_arm", listOf("x86_64"), hasVulkan = true)
        assertEquals(DefaultGlRenderer.id, result.id)
    }

    @Test
    fun fallsBackWhenVulkanMissing() {
        val result = registry.resolve("vk_arm", listOf("arm64-v8a"), hasVulkan = false)
        assertEquals(DefaultGlRenderer.id, result.id)
    }

    @Test
    fun fallsBackWhenPreferredUnknownOrNull() {
        assertEquals(DefaultGlRenderer.id, registry.resolve(null, listOf("arm64-v8a"), true).id)
        assertEquals(DefaultGlRenderer.id, registry.resolve("khong_co", listOf("arm64-v8a"), true).id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsFallbackThatIsNotUniversal() {
        RendererRegistry(emptyList(), fallback = vulkanArm)
    }
}
