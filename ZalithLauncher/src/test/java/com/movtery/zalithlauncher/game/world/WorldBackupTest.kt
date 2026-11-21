/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.game.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.zip.ZipFile

class WorldBackupTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun zipsWorldFolderAndSkipsSessionLock() {
        val world = File(tmp.newFolder("saves"), "Mon Monde").apply { mkdirs() }
        File(world, "level.dat").writeText("level")
        File(world, "region").mkdirs()
        File(world, "region/r.0.0.mca").writeText("region")
        File(world, "session.lock").writeText("lock")

        val backupDir = File(tmp.root, "world_backups")
        val zip = backupWorldToZip(world, backupDir, "20260101_120000")

        assertEquals("Mon Monde_20260101_120000.zip", zip.name)
        assertTrue(zip.isFile)
        assertFalse(File(backupDir, zip.name + ".part").exists())
        ZipFile(zip).use { file ->
            val names = Collections.list(file.entries()).map { it.name }.sorted()
            assertEquals(listOf("Mon Monde/level.dat", "Mon Monde/region/r.0.0.mca"), names)
            val region = file.getInputStream(file.getEntry("Mon Monde/region/r.0.0.mca"))
            assertEquals("region", region.reader().use { it.readText() })
        }
    }

    @Test
    fun backupInSameSecondGetsNewName() {
        val world = tmp.newFolder("W").also { File(it, "level.dat").writeText("x") }
        val dir = File(tmp.root, "out")
        val first = backupWorldToZip(world, dir, "t")
        val second = backupWorldToZip(world, dir, "t")
        assertEquals("W_t.zip", first.name)
        assertEquals("W_t_2.zip", second.name)
    }

    @Test
    fun missingWorldThrows() {
        try {
            backupWorldToZip(File(tmp.root, "absent"), File(tmp.root, "out"), "t")
            fail("Expected IOException for a missing world folder")
        } catch (expected: IOException) {
            // đúng như mong đợi
        }
    }
}
