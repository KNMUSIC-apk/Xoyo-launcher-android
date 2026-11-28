/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.ui.screens.content.performance

import android.os.StatFs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.performance.CustomPreset
import com.movtery.zalithlauncher.game.performance.DeviceStats
import com.movtery.zalithlauncher.game.performance.DiagnosticCode
import com.movtery.zalithlauncher.game.performance.DiagnosticFinding
import com.movtery.zalithlauncher.game.performance.DiagnosticInput
import com.movtery.zalithlauncher.game.performance.DiagnosticSeverity
import com.movtery.zalithlauncher.game.performance.LauncherDiagnostics
import com.movtery.zalithlauncher.game.performance.MIN_RAM_MB
import com.movtery.zalithlauncher.game.performance.PerformancePreset
import com.movtery.zalithlauncher.game.performance.encodeCustomPresets
import com.movtery.zalithlauncher.game.performance.parseCustomPresets
import com.movtery.zalithlauncher.game.performance.recommendedRamMb
import com.movtery.zalithlauncher.game.performance.suggestPreset
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.setting.LauncherSettings
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.main.NavDestination
import com.movtery.zalithlauncher.ui.screens.main.navigateToDestination
import com.movtery.zalithlauncher.utils.file.formatFileSize
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import com.movtery.zalithlauncher.viewmodel.sendToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/** Một lần lấy số liệu thiết bị */
private data class DeviceSample(
    val ram: DeviceStats.RamSnapshot?,
    val cpu: Float?,
    val cpuReadable: Boolean,
    val battery: Float?,
    val thermal: Float?,
    val cpuTotals: DeviceStats.CpuTotals?,
)

/**
 * Trung tâm hiệu năng.
 * - Số liệu thiết bị là số đo thật. Nếu Android không cho đọc, hiển thị "không khả dụng".
 * - GPU và FPS trong game không đọc được từ launcher, nên luôn ghi rõ là không khả dụng.
 * - Gợi ý RAM và preset là ước tính theo RAM máy, không phải kết quả benchmark.
 */
@Composable
fun PerformanceCenterScreen(
    backStackViewModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel,
) {
    BaseScreen(
        screenKey = NormalNavKey.PerformanceCenter,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { isVisible ->
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val currentVersion by VersionsManager.currentVersion.collectAsStateWithLifecycle()

        var ram by remember { mutableStateOf<DeviceStats.RamSnapshot?>(null) }
        var cpuPercent by remember { mutableStateOf<Float?>(null) }
        var cpuReadable by remember { mutableStateOf<Boolean?>(null) }
        var batteryTemp by remember { mutableStateOf<Float?>(null) }
        var thermal by remember { mutableStateOf<Float?>(null) }

        // Chỉ lấy số liệu khi màn hình đang hiển thị, để không tốn pin ở nền
        //同时要求启动器位于前台：游戏覆盖启动器（ON_STOP）期间停止采样，回到前台时自动恢复
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(isVisible, lifecycleOwner) {
            if (!isVisible) return@LaunchedEffect
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                var previousCpu: DeviceStats.CpuTotals? = null
                while (true) {
                    val sample = withContext(Dispatchers.IO) {
                        val currentCpu = DeviceStats.readCpuTotals()
                        val before = previousCpu
                        DeviceSample(
                            ram = DeviceStats.readRam(context),
                            cpu = if (before != null && currentCpu != null) {
                                DeviceStats.cpuUsagePercent(before, currentCpu)
                            } else {
                                null
                            },
                            cpuReadable = currentCpu != null,
                            battery = DeviceStats.readBatteryTempCelsius(context),
                            thermal = DeviceStats.readMaxThermalCelsius(),
                            cpuTotals = currentCpu
                        )
                    }
                    previousCpu = sample.cpuTotals
                    ram = sample.ram
                    cpuPercent = sample.cpu
                    cpuReadable = sample.cpuReadable
                    batteryTemp = sample.battery
                    thermal = sample.thermal
                    delay(2_000L)
                }
            }
        }

        val totalMb = ram?.totalBytes?.let { it / (1024L * 1024L) }
        val unavailable = stringResource(R.string.perf_unavailable)
        val measuring = stringResource(R.string.perf_device_measuring)

        val applyPreset: (Int, String) -> Unit = { ramMb, jvmArgs ->
            val version = currentVersion
            if (version != null) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val config = version.getVersionConfig()
                        config.ramAllocation = ramMb
                        config.jvmArgs = jvmArgs
                        config.save()
                    }
                    eventViewModel.sendToast(androidText(R.string.perf_applied))
                }
            }
        }

        val rawCustom = LauncherSettings.performanceCustomPresets.state
        val customPresets = remember(rawCustom) { parseCustomPresets(rawCustom) }
        var showAddCustom by remember { mutableStateOf(false) }

        var findings by remember { mutableStateOf<List<DiagnosticFinding>?>(null) }
        var diagnosing by remember { mutableStateOf(false) }
        LaunchedEffect(currentVersion) { findings = null }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                BackgroundCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = stringResource(R.string.perf_notice),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                BackgroundCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SectionTitle(stringResource(R.string.perf_device_title))
                        MetricRow(
                            label = stringResource(R.string.perf_device_ram),
                            value = ram?.let {
                                val used = (it.totalBytes - it.availableBytes).coerceAtLeast(0L)
                                "${formatFileSize(used)} / ${formatFileSize(it.totalBytes)}"
                            } ?: unavailable
                        )
                        ram?.let {
                            val fraction = ((it.totalBytes - it.availableBytes).toFloat() /
                                    it.totalBytes.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        MetricRow(
                            label = stringResource(R.string.perf_device_cpu),
                            value = when {
                                cpuReadable == null -> measuring
                                cpuReadable == false -> unavailable
                                cpuPercent == null -> measuring
                                else -> "${cpuPercent!!.roundToInt()}%"
                            }
                        )
                        MetricRow(
                            label = stringResource(R.string.perf_device_battery),
                            value = batteryTemp?.let { "${it.roundToInt()} °C" } ?: unavailable
                        )
                        MetricRow(
                            label = stringResource(R.string.perf_device_thermal),
                            value = thermal?.let { "${it.roundToInt()} °C" } ?: unavailable
                        )
                    }
                }
            }

            item {
                BackgroundCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SectionTitle(stringResource(R.string.perf_ingame_title))
                        MetricRow(
                            label = stringResource(R.string.perf_gpu_title),
                            value = unavailable
                        )
                        Text(
                            text = stringResource(R.string.perf_gpu_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        MetricRow(
                            label = stringResource(R.string.perf_fps_title),
                            value = unavailable
                        )
                        Text(
                            text = stringResource(R.string.perf_fps_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                ProfileCard(
                    version = currentVersion,
                    onOpenVersions = {
                        backStackViewModel.navigateToDestination(NavDestination.VERSIONS)
                    }
                )
            }

            item { SectionTitle(stringResource(R.string.perf_presets_title)) }

            val suggested = totalMb?.let { suggestPreset(it) }
            items(PerformancePreset.entries) { preset ->
                PresetCard(
                    preset = preset,
                    deviceTotalMb = totalMb,
                    suggested = preset == suggested,
                    enabled = currentVersion != null,
                    onApply = { ramMb -> applyPreset(ramMb, preset.jvmArgs) }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle(
                        text = stringResource(R.string.perf_custom_title),
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { showAddCustom = true }) {
                        Text(stringResource(R.string.perf_custom_add))
                    }
                }
            }

            if (customPresets.isEmpty()) {
                item {
                    Text(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        text = stringResource(R.string.perf_custom_empty),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            items(customPresets) { custom ->
                CustomPresetCard(
                    preset = custom,
                    enabled = currentVersion != null,
                    onApply = { applyPreset(custom.ramMb, custom.jvmArgs) },
                    onDelete = {
                        LauncherSettings.performanceCustomPresets.save(
                            encodeCustomPresets(customPresets - custom)
                        )
                    }
                )
            }

            item {
                BackgroundCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SectionTitle(stringResource(R.string.perf_diag_title))
                        Text(
                            text = stringResource(R.string.perf_diag_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            enabled = currentVersion != null && !diagnosing,
                            onClick = {
                                val version = currentVersion ?: return@Button
                                diagnosing = true
                                scope.launch {
                                    findings = withContext(Dispatchers.IO) {
                                        diagnoseVersion(version, totalMb)
                                    }
                                    diagnosing = false
                                }
                            }
                        ) {
                            Text(
                                text = stringResource(
                                    if (diagnosing) R.string.perf_diag_running else R.string.perf_diag_run
                                )
                            )
                        }
                        findings?.forEach { finding ->
                            FindingRow(finding)
                        }
                    }
                }
            }
        }

        if (showAddCustom) {
            CustomPresetDialog(
                defaultRam = (totalMb?.let { recommendedRamMb(PerformancePreset.BALANCED, it) } ?: 4096).coerceAtLeast(MIN_RAM_MB),
                onDismiss = { showAddCustom = false },
                onSave = { preset ->
                    LauncherSettings.performanceCustomPresets.save(
                        encodeCustomPresets(customPresets + preset)
                    )
                    showAddCustom = false
                }
            )
        }
    }
}

/** Chạy chẩn đoán trên luồng IO: log gần nhất, mod hỏng, dung lượng trống và RAM đã cấp */
private fun diagnoseVersion(version: Version, deviceTotalMb: Long?): List<DiagnosticFinding> {
    val gameDir = version.getGameDir()
    val log = LauncherDiagnostics.readLogTail(version.getLatestLog())
    val broken = LauncherDiagnostics.findBrokenJars(File(gameDir, VersionFolders.MOD.folderName))
    val freeBytes = runCatching { StatFs(gameDir.path).availableBytes }.getOrNull()
    return LauncherDiagnostics.analyze(
        DiagnosticInput(
            logText = log,
            freeDiskBytes = freeBytes,
            allocatedRamMb = version.getRamAllocation(),
            deviceTotalMb = deviceTotalMb,
            brokenModJars = broken
        )
    )
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(horizontal = 4.dp),
        text = text,
        style = MaterialTheme.typography.titleSmall
    )
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProfileCard(
    version: Version?,
    onOpenVersions: () -> Unit,
) {
    BackgroundCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionTitle(stringResource(R.string.perf_profile_title))
            if (version == null) {
                Text(
                    text = stringResource(R.string.perf_profile_none),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = onOpenVersions) {
                    Text(stringResource(R.string.perf_profile_open_versions))
                }
            } else {
                val defaultValue = stringResource(R.string.perf_default_value)
                Text(
                    text = version.getVersionName(),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                MetricRow(
                    label = stringResource(R.string.perf_profile_ram),
                    value = "${version.getRamAllocation()} MB"
                )
                MetricRow(
                    label = stringResource(R.string.perf_profile_java),
                    value = version.getJavaRuntime().ifBlank { defaultValue }
                )
                MetricRow(
                    label = stringResource(R.string.perf_profile_renderer),
                    value = version.getRenderer().ifBlank { defaultValue }
                )
                Text(
                    text = stringResource(R.string.perf_profile_jvm),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = version.getJvmArgs().ifBlank { defaultValue },
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun PerformancePreset.titleRes(): Int = when (this) {
    PerformancePreset.LOW -> R.string.perf_preset_low
    PerformancePreset.BALANCED -> R.string.perf_preset_balanced
    PerformancePreset.HIGH -> R.string.perf_preset_high
    PerformancePreset.EXTREME -> R.string.perf_preset_extreme
}

@Composable
private fun PresetCard(
    preset: PerformancePreset,
    deviceTotalMb: Long?,
    suggested: Boolean,
    enabled: Boolean,
    onApply: (Int) -> Unit,
) {
    val ramMb = deviceTotalMb?.let { recommendedRamMb(preset, it) }
    BackgroundCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(preset.titleRes()),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (suggested) {
                    Text(
                        text = stringResource(R.string.perf_preset_suggested),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = if (ramMb != null) {
                    stringResource(R.string.perf_preset_ram_estimate, ramMb)
                } else {
                    stringResource(R.string.perf_preset_need_device_ram)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = preset.jvmArgs,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                enabled = enabled && ramMb != null,
                onClick = { ramMb?.let { onApply(it) } }
            ) {
                Text(stringResource(R.string.perf_preset_apply))
            }
        }
    }
}

@Composable
private fun CustomPresetCard(
    preset: CustomPreset,
    enabled: Boolean,
    onApply: () -> Unit,
    onDelete: () -> Unit,
) {
    BackgroundCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = preset.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_outlined),
                        contentDescription = stringResource(R.string.generic_delete)
                    )
                }
            }
            Text(
                text = stringResource(R.string.perf_custom_ram_value, preset.ramMb),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (preset.jvmArgs.isNotBlank()) {
                Text(
                    text = preset.jvmArgs,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(enabled = enabled, onClick = onApply) {
                Text(stringResource(R.string.perf_preset_apply))
            }
        }
    }
}

@Composable
private fun CustomPresetDialog(
    defaultRam: Int,
    onDismiss: () -> Unit,
    onSave: (CustomPreset) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var ram by remember { mutableStateOf(defaultRam.toString()) }
    var jvm by remember { mutableStateOf("") }

    val ramValue = ram.toIntOrNull()
    val valid = name.isNotBlank() && ramValue != null && ramValue >= MIN_RAM_MB

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.perf_custom_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.perf_custom_name)) }
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = ram,
                    onValueChange = { ram = it.filter(Char::isDigit).take(6) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(stringResource(R.string.perf_custom_ram)) }
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = jvm,
                    onValueChange = { jvm = it },
                    label = { Text(stringResource(R.string.perf_custom_jvm)) }
                )
                if (!valid) {
                    Text(
                        text = stringResource(R.string.perf_custom_invalid, MIN_RAM_MB),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    if (ramValue != null && valid) {
                        onSave(CustomPreset(name = name.trim(), ramMb = ramValue, jvmArgs = jvm.trim()))
                    }
                }
            ) {
                Text(stringResource(R.string.generic_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.generic_cancel))
            }
        }
    )
}

private fun DiagnosticCode.descriptionRes(): Int = when (this) {
    DiagnosticCode.OUT_OF_MEMORY -> R.string.diag_out_of_memory
    DiagnosticCode.JAVA_VERSION_MISMATCH -> R.string.diag_java_version
    DiagnosticCode.MISSING_CLASS -> R.string.diag_missing_class
    DiagnosticCode.DUPLICATE_MOD -> R.string.diag_duplicate_mod
    DiagnosticCode.MIXIN_FAILURE -> R.string.diag_mixin_failure
    DiagnosticCode.CORRUPTED_FILE -> R.string.diag_corrupted_file
    DiagnosticCode.NATIVE_LIBRARY -> R.string.diag_native_library
    DiagnosticCode.MISSING_DEPENDENCY -> R.string.diag_missing_dependency
    DiagnosticCode.LOW_DISK_SPACE -> R.string.diag_low_disk
    DiagnosticCode.HIGH_RAM_ALLOCATION -> R.string.diag_high_ram
    DiagnosticCode.CORRUPTED_MOD_JAR -> R.string.diag_corrupted_mod_jar
    DiagnosticCode.NO_ISSUES -> R.string.diag_no_issues
}

@Composable
private fun FindingRow(finding: DiagnosticFinding) {
    val tint = when (finding.severity) {
        DiagnosticSeverity.CRITICAL -> MaterialTheme.colorScheme.error
        DiagnosticSeverity.WARNING -> MaterialTheme.colorScheme.tertiary
        DiagnosticSeverity.INFO -> MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(
                if (finding.severity == DiagnosticSeverity.INFO) {
                    R.drawable.ic_info_outlined
                } else {
                    R.drawable.ic_warning_filled
                }
            ),
            contentDescription = null,
            tint = tint
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(finding.code.descriptionRes()),
                style = MaterialTheme.typography.bodySmall
            )
            finding.detail?.let { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
