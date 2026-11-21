/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.game.account.wardrobe

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MinecraftNameTest {

    @Test
    fun acceptsValidNames() {
        assertTrue(isValidMinecraftName("Notch"))
        assertTrue(isValidMinecraftName("a_b_1"))
        assertTrue(isValidMinecraftName("abcdefghijklmnop"))
    }

    @Test
    fun rejectsInvalidNames() {
        assertFalse(isValidMinecraftName(""))
        assertFalse(isValidMinecraftName("ab"))
        assertFalse(isValidMinecraftName("abcdefghijklmnopq"))
        assertFalse(isValidMinecraftName("no spaces"))
        assertFalse(isValidMinecraftName("dau_tieng_viet_ă"))
    }
}
