/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.ui.screens.content.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.movtery.zalithlauncher.ui.components.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.movtery.zalithlauncher.ui.components.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.movtery.zalithlauncher.ui.components.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.isLocalAccount
import com.movtery.zalithlauncher.game.account.wardrobe.PlayerNotFoundException
import com.movtery.zalithlauncher.game.account.wardrobe.SkinModelType
import com.movtery.zalithlauncher.game.account.wardrobe.WebImageInvalidException
import com.movtery.zalithlauncher.game.account.wardrobe.WebImageKind
import com.movtery.zalithlauncher.game.account.wardrobe.classifyPngImage
import com.movtery.zalithlauncher.game.account.wardrobe.downloadWebPng
import com.movtery.zalithlauncher.game.account.wardrobe.findPlayerTextures
import com.movtery.zalithlauncher.game.account.wardrobe.isCapeImage
import com.movtery.zalithlauncher.game.account.wardrobe.isSlimModel
import com.movtery.zalithlauncher.game.account.wardrobe.isValidMinecraftName
import com.movtery.zalithlauncher.game.account.wardrobe.toArmModel
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.ui.components.PetModelView
import com.movtery.zalithlauncher.ui.components.rememberDialogMaxHeight
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.itemColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Kết quả đã tải về thư mục cache, sẵn sàng để áp dụng */
private data class WebSkinResult(
    val skinFile: File?,
    val skinBytes: ByteArray?,
    val skinSlim: Boolean,
    val capeFile: File?,
    val capeBytes: ByteArray?,
)

private fun newWebCacheFile(): File =
    File(PathManager.DIR_IMAGE_CACHE, "web_${UUID.randomUUID()}.png")

/** Tải ảnh về cache, chỉ giữ lại nếu đúng loại [expected]. Chạy trên luồng IO. */
private suspend fun fetchWebImage(url: String, expected: WebImageKind): File? {
    val target = newWebCacheFile()
    downloadWebPng(url, target)
    val matches = target.exists() && when (expected) {
        // Cape hiện đại cũng là 64x32 nên phải kiểm tra theo tỉ lệ ảnh
        WebImageKind.CAPE -> target.isCapeImage()
        WebImageKind.SKIN -> target.classifyPngImage() == WebImageKind.SKIN
        WebImageKind.INVALID -> false
    }
    if (!matches) target.delete()
    return target.takeIf { matches }
}

/** Tải skin và cape của người chơi về cache. Chạy trên luồng IO. */
private suspend fun loadPlayerResult(name: String): WebSkinResult {
    val textures = findPlayerTextures(name)
    val skin = textures.skinUrl?.let { fetchWebImage(it, WebImageKind.SKIN) }
    val cape = textures.capeUrl?.let { fetchWebImage(it, WebImageKind.CAPE) }
    return WebSkinResult(
        skinFile = skin,
        skinBytes = skin?.readBytes(),
        skinSlim = textures.skinSlim,
        capeFile = cape,
        capeBytes = cape?.readBytes()
    )
}

/**
 * XOYO: tìm và cài skin hoặc cape ngay trong launcher.
 * - Tìm theo tên người chơi Minecraft (API công khai của Mojang), kèm cả cape nếu có.
 * - Hoặc dán link ảnh PNG https trực tiếp.
 * Skin áp dụng cho cả tài khoản offline và Microsoft.
 * Cape tự chọn chỉ áp dụng cho tài khoản offline, vì Mojang chỉ cho dùng cape đã được cấp.
 */
@Composable
fun WebSkinDialog(
    account: Account,
    onDismissRequest: () -> Unit,
    onApplySkin: (File, SkinModelType) -> Unit,
    onApplyCape: (File) -> Unit,
    onRemoveCape: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localAccount = account.isLocalAccount()
    val hasLocalCape = remember(account.uniqueUUID) { account.getCapeFile().exists() }

    var nameQuery by rememberSaveable { mutableStateOf("") }
    var urlQuery by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<WebSkinResult?>(null) }

    fun runLoad(block: suspend () -> WebSkinResult) {
        scope.launch {
            loading = true
            errorText = null
            try {
                result = block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: PlayerNotFoundException) {
                errorText = context.getString(R.string.web_skin_not_found)
            } catch (e: WebImageInvalidException) {
                errorText = context.getString(R.string.web_skin_invalid_image)
            } catch (e: IllegalArgumentException) {
                errorText = context.getString(R.string.web_skin_invalid_url)
            } catch (e: Exception) {
                errorText = context.getString(R.string.web_skin_network_error)
            } finally {
                loading = false
            }
        }
    }

    fun searchPlayer() {
        val name = nameQuery.trim()
        if (!isValidMinecraftName(name)) {
            errorText = context.getString(R.string.web_skin_invalid_name)
            return
        }
        runLoad {
            withContext(Dispatchers.IO) { loadPlayerResult(name) }
        }
    }

    fun loadLink() {
        val url = urlQuery.trim()
        if (!url.startsWith("https://")) {
            errorText = context.getString(R.string.web_skin_invalid_url)
            return
        }
        runLoad {
            withContext(Dispatchers.IO) {
                val target = newWebCacheFile()
                when (downloadWebPng(url, target)) {
                    WebImageKind.SKIN -> WebSkinResult(
                        skinFile = target,
                        skinBytes = target.readBytes(),
                        skinSlim = target.isSlimModel(),
                        capeFile = null,
                        capeBytes = null
                    )
                    WebImageKind.CAPE -> WebSkinResult(
                        skinFile = null,
                        skinBytes = null,
                        skinSlim = false,
                        capeFile = target,
                        capeBytes = target.readBytes()
                    )
                    WebImageKind.INVALID -> throw WebImageInvalidException()
                }
            }
        }
    }

    // Xem trước: kết quả vừa tải, hoặc skin và cape hiện tại của tài khoản
    val previewSkin by produceState<ByteArray?>(null, result, account.uniqueUUID) {
        value = withContext(Dispatchers.IO) {
            val loaded = result
            if (loaded != null) {
                loaded.skinBytes
            } else {
                account.getSkinFile().takeIf { it.exists() }?.readBytes()
            }
        }
    }
    val previewCape by produceState<ByteArray?>(null, result, account.uniqueUUID) {
        value = withContext(Dispatchers.IO) {
            val loaded = result
            if (loaded != null) {
                loaded.capeBytes
            } else {
                account.getCapeFile().takeIf { it.exists() }?.readBytes()
            }
        }
    }
    val previewModel = result?.let { loaded ->
        if (loaded.skinSlim) SkinModelType.ALEX else SkinModelType.STEVE
    } ?: account.skinModelType
    val previewArm = previewModel.toArmModel()

    /** Xem trước mô hình: khung dọc để thấy toàn bộ skin kèm cape, không bị cắt chân */
    val previewView: @Composable (Modifier) -> Unit = { previewModifier ->
        Box(
            modifier = previewModifier
                .clip(MaterialTheme.shapes.large)
                .background(itemColor(false)),
            contentAlignment = Alignment.Center
        ) {
            PetModelView(
                modifier = Modifier.fillMaxSize(),
                skin = previewSkin,
                cape = previewCape,
                arm = previewArm,
                gazeEnabled = false
            )
        }
    }

    val inputFields: @Composable () -> Unit = {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = nameQuery,
            onValueChange = { nameQuery = it },
            singleLine = true,
            enabled = !loading,
            label = { Text(stringResource(R.string.web_skin_name_hint)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { searchPlayer() })
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { searchPlayer() },
            enabled = !loading && nameQuery.isNotBlank()
        ) {
            Text(stringResource(R.string.web_skin_search))
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = urlQuery,
            onValueChange = { urlQuery = it },
            singleLine = true,
            enabled = !loading,
            label = { Text(stringResource(R.string.web_skin_url_hint)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { loadLink() })
        )
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { loadLink() },
            enabled = !loading && urlQuery.isNotBlank()
        ) {
            Text(stringResource(R.string.web_skin_load_url))
        }

        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        }

        errorText?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        result?.let { loaded ->
            if (loaded.skinFile == null && loaded.capeFile == null) {
                Text(
                    text = stringResource(R.string.web_skin_no_textures),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    val actions: @Composable () -> Unit = {
        val skinFile = result?.skinFile
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = skinFile != null && !loading,
            onClick = {
                val model = if (result?.skinSlim == true) SkinModelType.ALEX else SkinModelType.STEVE
                skinFile?.let { onApplySkin(it, model) }
                //Người chơi có cape thì dùng luôn cape đó cho mô hình
                result?.capeFile?.takeIf { localAccount }?.let(onApplyCape)
                onDismissRequest()
            }
        ) {
            Text(stringResource(R.string.web_skin_apply_skin))
        }

        val capeFile = result?.capeFile
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = localAccount && capeFile != null && !loading,
            onClick = {
                capeFile?.let { onApplyCape(it) }
                onDismissRequest()
            }
        ) {
            Text(stringResource(R.string.web_skin_apply_cape))
        }

        if (localAccount && hasLocalCape) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onRemoveCape()
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.web_skin_remove_cape))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.generic_close))
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            //Màn hình đủ rộng: đưa phần xem trước sang bên phải
            val sidePreview = maxWidth >= 620.dp

            Surface(
                modifier = Modifier
                    .fillMaxWidth(if (sidePreview) 0.85f else 0.92f)
                    .heightIn(max = rememberDialogMaxHeight()),
                shape = MaterialTheme.shapes.extraLarge,
                color = cardColor(false),
                contentColor = onCardColor(),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.web_skin_title),
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (!localAccount) {
                        Text(
                            text = stringResource(R.string.account_cape_offline_only),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (sidePreview) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                inputFields()
                                actions()
                            }
                            Column(modifier = Modifier.width(210.dp)) {
                                previewView(Modifier.fillMaxWidth().aspectRatio(0.62f))
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            inputFields()
                            previewView(Modifier.fillMaxWidth().aspectRatio(0.62f))
                            actions()
                        }
                    }
                }
            }
        }
    }
}
