package com.movtery.zalithlauncher.game.performance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class LauncherDiagnosticsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val plenty = 10L * 1024 * 1024 * 1024

    private fun input(
        log: String? = null,
        free: Long? = plenty,
        allocated: Int? = 2048,
        total: Long? = 8192,
        broken: List<String> = emptyList()
    ) = DiagnosticInput(
        logText = log,
        freeDiskBytes = free,
        allocatedRamMb = allocated,
        deviceTotalMb = total,
        brokenModJars = broken
    )

    private fun codes(findings: List<DiagnosticFinding>) = findings.map { it.code }

    @Test
    fun cleanInputReportsNoIssues() {
        val findings = LauncherDiagnostics.analyze(input(log = "[Render thread/INFO] started"))

        assertEquals(listOf(DiagnosticCode.NO_ISSUES), codes(findings))
    }

    @Test
    fun logRulesMatchKnownErrors() {
        val cases = mapOf(
            "java.lang.OutOfMemoryError: Java heap space" to DiagnosticCode.OUT_OF_MEMORY,
            "UnsupportedClassVersionError: class file version 65.0" to DiagnosticCode.JAVA_VERSION_MISMATCH,
            "java.lang.NoClassDefFoundError: net/x/Y" to DiagnosticCode.MISSING_CLASS,
            "Found duplicate mods: sodium" to DiagnosticCode.DUPLICATE_MOD,
            "org.spongepowered.asm.mixin.injection.InvalidInjectionException" to DiagnosticCode.MIXIN_FAILURE,
            "java.util.zip.ZipException: invalid LOC header" to DiagnosticCode.CORRUPTED_FILE,
            "java.lang.UnsatisfiedLinkError: liblwjgl.so" to DiagnosticCode.NATIVE_LIBRARY,
            "Incompatible mod set: fabric-api requires any version of loader" to DiagnosticCode.MISSING_DEPENDENCY
        )
        for ((line, expected) in cases) {
            val found = codes(LauncherDiagnostics.analyze(input(log = line)))
            assertTrue("Thiếu luật $expected cho dòng: $line", expected in found)
        }
    }

    @Test
    fun lowDiskSpaceHasTwoLevels() {
        val critical = LauncherDiagnostics.analyze(input(free = 100L * 1024 * 1024))
            .single { it.code == DiagnosticCode.LOW_DISK_SPACE }
        val warning = LauncherDiagnostics.analyze(input(free = 500L * 1024 * 1024))
            .single { it.code == DiagnosticCode.LOW_DISK_SPACE }

        assertEquals(DiagnosticSeverity.CRITICAL, critical.severity)
        assertEquals(DiagnosticSeverity.WARNING, warning.severity)
        assertTrue(codes(LauncherDiagnostics.analyze(input(free = 5L * 1024 * 1024 * 1024))).none {
            it == DiagnosticCode.LOW_DISK_SPACE
        })
    }

    @Test
    fun highRamAllocationWarnsAboveEightyPercent() {
        assertTrue(DiagnosticCode.HIGH_RAM_ALLOCATION in codes(
            LauncherDiagnostics.analyze(input(allocated = 7000, total = 8000))
        ))
        assertTrue(DiagnosticCode.HIGH_RAM_ALLOCATION !in codes(
            LauncherDiagnostics.analyze(input(allocated = 4000, total = 8000))
        ))
    }

    @Test
    fun missingInputsAreSkipped() {
        val findings = LauncherDiagnostics.analyze(
            DiagnosticInput(logText = null, freeDiskBytes = null, allocatedRamMb = null, deviceTotalMb = null, brokenModJars = emptyList())
        )

        assertEquals(listOf(DiagnosticCode.NO_ISSUES), codes(findings))
    }

    @Test
    fun criticalFindingsComeFirst() {
        val findings = LauncherDiagnostics.analyze(
            input(log = "NoClassDefFoundError and OutOfMemoryError", free = 50L * 1024 * 1024)
        )

        assertEquals(DiagnosticSeverity.CRITICAL, findings.first().severity)
    }

    @Test
    fun brokenJarNamesAreReported() {
        val finding = LauncherDiagnostics.analyze(input(broken = listOf("a.jar", "b.jar")))
            .single { it.code == DiagnosticCode.CORRUPTED_MOD_JAR }

        assertEquals("a.jar, b.jar", finding.detail)
    }

    @Test
    fun findBrokenJarsDetectsCorruptFilesOnly() {
        val mods = tmp.newFolder("mods")
        writeValidJar(File(mods, "good.jar"))
        File(mods, "cut.jar").writeBytes(byteArrayOf(0x50, 0x4b, 0x03, 0x04, 0x00))
        File(mods, "readme.txt").writeText("not a jar")

        assertEquals(listOf("cut.jar"), LauncherDiagnostics.findBrokenJars(mods))
    }

    @Test
    fun readLogTailReturnsOnlyTheEnd() {
        val log = File(tmp.root, "latest.log")
        log.writeText("0123456789")

        assertEquals("789", LauncherDiagnostics.readLogTail(log, maxBytes = 3L))
        assertNull(LauncherDiagnostics.readLogTail(File(tmp.root, "missing.log")))
        assertNotNull(LauncherDiagnostics.readLogTail(log))
    }

    private fun writeValidJar(target: File) {
        ZipOutputStream(target.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("fabric.mod.json"))
            zip.write("{}".toByteArray())
            zip.closeEntry()
        }
    }
}
