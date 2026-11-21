package com.movtery.zalithlauncher.game.control

import com.movtery.layer_controller.layout.loadLayoutFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kiểm tra mẫu bố cục cảm ứng đi kèm ứng dụng đọc được bằng LayerController.
 * File được copy vào test classpath bởi task copyUnitTestAssets trong build.gradle.kts.
 */
class ControlTemplateTest {
    @Test
    fun touchTemplateParses() {
        val stream = javaClass.getResourceAsStream("/assets/control_templates/touch_template.json")
        assertNotNull("Thiếu mẫu control_templates/touch_template.json trong test classpath", stream)
        val json = stream!!.bufferedReader().use { it.readText() }

        val layout = loadLayoutFromString(json)

        assertTrue("Mẫu phải có ít nhất một layer", layout.layers.isNotEmpty())
        val buttons = layout.layers.flatMap { it.normalButtons }
        assertTrue("Mẫu phải có ít nhất một nút", buttons.isNotEmpty())
        assertEquals("Mỗi nút trong mẫu phải có uuid riêng", buttons.size, buttons.map { it.uuid }.toSet().size)
    }
}
