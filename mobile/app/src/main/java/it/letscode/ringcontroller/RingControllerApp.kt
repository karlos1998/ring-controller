package it.letscode.ringcontroller

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.pow
import kotlin.math.roundToInt

private val AppBackground = Color(0xFF08090C)
private val AppSurface = Color(0xFF12151A)
private val AppSurfaceRaised = Color(0xFF1A1E24)
private val AppOrange = Color(0xFFFF6A00)
private val AppCyan = Color(0xFF00E5E5)
private val AppText = Color(0xFFF4F5F7)
private val AppMuted = Color(0xFF8D949F)
private val AppLine = Color(0xFF292E36)

private val AppColors = darkColorScheme(
    primary = AppOrange,
    secondary = AppCyan,
    background = AppBackground,
    surface = AppSurface,
    onPrimary = Color.Black,
    onBackground = AppText,
    onSurface = AppText,
)

private enum class DashboardTab(
    @param:StringRes val labelRes: Int,
    @param:StringRes val descriptionRes: Int,
) {
    Drive(R.string.nav_drive, R.string.nav_drive_description),
    Scenes(R.string.nav_scenes, R.string.nav_scenes_description),
    Config(R.string.nav_config, R.string.nav_config_description),
}

private val defaultFavoriteColors = listOf(
    Color(0xFFF2F6FF),
    Color(0xFFFF6A00),
    Color(0xFFFF304E),
    Color(0xFFA855F7),
    AppCyan,
    Color(0xFF43E07B),
)

@Composable
internal fun RingControllerApp(
    bleManager: RingBleManager? = null,
    initialSimplifiedPreview: Boolean = false,
    initialCustomScenes: List<CustomScene> = emptyList(),
    onSimplifiedPreviewChanged: (Boolean) -> Unit = {},
    onCustomScenesChanged: (List<CustomScene>) -> Unit = {},
) {
    val connectionState = bleManager?.connectionState ?: BleConnectionState.LocalDemo
    val controllerSnapshot = bleManager?.snapshot
    var currentTab by remember { mutableStateOf(DashboardTab.Drive) }
    var ringsEnabled by remember { mutableStateOf(true) }
    var selectedRing by remember { mutableStateOf<Int?>(null) }
    var ringColors by remember { mutableStateOf(List(4) { AppCyan }) }
    var solidRingColors by remember { mutableStateOf(ringColors) }
    var brightness by remember { mutableFloatStateOf(0.88f) }
    var activeScene by remember { mutableStateOf<ScenePreset?>(null) }
    var activeCustomSceneSlot by remember { mutableStateOf<Int?>(null) }
    var customScenes by remember { mutableStateOf(initialCustomScenes) }
    var vehicleAutomationEnabled by remember { mutableStateOf(true) }
    var daylightSignalActive by remember { mutableStateOf(false) }
    var daylightAutomationEnabled by remember { mutableStateOf(true) }
    var daylightBrightnessPercent by remember { mutableStateOf(50) }
    var favoriteColors by remember { mutableStateOf(defaultFavoriteColors) }
    var simplifiedPreview by rememberSaveable { mutableStateOf(initialSimplifiedPreview) }
    var colorEditorOpen by remember { mutableStateOf(false) }
    var draftRingColors by remember { mutableStateOf(ringColors) }
    var draftFavorites by remember { mutableStateOf(favoriteColors) }
    var draftBrightness by remember { mutableFloatStateOf(brightness) }
    var customSceneEditor by remember { mutableStateOf<CustomScene?>(null) }
    var customSceneEditorIsNew by remember { mutableStateOf(false) }
    var customScenePendingDelete by remember { mutableStateOf<CustomScene?>(null) }

    LaunchedEffect(controllerSnapshot) {
        val controller = controllerSnapshot ?: return@LaunchedEffect
        ringsEnabled = controller.enabled || controller.vehicleOverrideActive
        brightness = controller.brightness / 255f
        activeScene = if (controller.vehicleOverrideActive) null
        else controller.scene?.let(ScenePreset::fromId)
        activeCustomSceneSlot = if (controller.vehicleOverrideActive) null else controller.customSceneSlot
        solidRingColors = controller.ringColors
        ringColors = if (controller.vehicleOverrideActive) List(4) { Color.White }
        else controller.ringColors
        vehicleAutomationEnabled = controller.vehicleAutomationEnabled
        daylightSignalActive = controller.daylightSignalActive
        daylightAutomationEnabled = controller.daylightAutomationEnabled
        daylightBrightnessPercent = controller.daylightBrightnessPercent
        if (controller.favorites.isNotEmpty()) favoriteColors = controller.favorites
    }

    LaunchedEffect(activeScene, activeCustomSceneSlot, customScenes, ringsEnabled, favoriteColors) {
        val builtInScene = activeScene
        val customScene = activeCustomSceneSlot?.let { slot -> customScenes.firstOrNull { it.slot == slot } }
        if (builtInScene == null && customScene == null) return@LaunchedEffect
        if (!ringsEnabled) return@LaunchedEffect
        var startedAtNanos = 0L
        while (ringsEnabled) {
            withFrameNanos { frameTimeNanos ->
                if (startedAtNanos == 0L) startedAtNanos = frameTimeNanos
                val elapsedSeconds = (frameTimeNanos - startedAtNanos) / 1_000_000_000f
                ringColors = if (customScene != null) {
                    colorsForCustomScene(customScene, elapsedSeconds)
                } else {
                    colorsForScene(builtInScene!!, elapsedSeconds, favoriteColors)
                }
            }
        }
    }

    MaterialTheme(colorScheme = AppColors) {
        Scaffold(
            containerColor = AppBackground,
            bottomBar = {
                DashboardNavigation(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                )
            },
        ) { padding ->
            when (currentTab) {
                DashboardTab.Drive -> DriveScreen(
                    modifier = Modifier.padding(padding),
                    ringsEnabled = ringsEnabled,
                    ringColors = ringColors,
                    selectedRing = selectedRing,
                    brightness = brightness,
                    activeScene = activeScene,
                    activeCustomSceneName = activeCustomSceneSlot
                        ?.let { slot -> customScenes.firstOrNull { it.slot == slot }?.name },
                    connectionState = connectionState,
                    simplifiedPreview = simplifiedPreview,
                    onRingSelected = { selectedRing = it },
                    onPowerClick = {
                        ringsEnabled = !ringsEnabled
                        bleManager?.setPower(ringsEnabled)
                    },
                    onTargetSelected = { selectedRing = it },
                    onOpenColorEditor = {
                        draftRingColors = ringColors
                        draftFavorites = favoriteColors
                        draftBrightness = brightness
                        colorEditorOpen = true
                    },
                )

                DashboardTab.Scenes -> ScenesScreen(
                    modifier = Modifier.padding(padding),
                    ringsEnabled = ringsEnabled,
                    ringColors = ringColors,
                    selectedRing = selectedRing,
                    brightness = brightness,
                    activeScene = activeScene,
                    activeCustomSceneSlot = activeCustomSceneSlot,
                    customScenes = customScenes,
                    favoriteColors = favoriteColors,
                    connectionState = connectionState,
                    simplifiedPreview = simplifiedPreview,
                    onRingSelected = { selectedRing = it },
                    onSceneSelected = { scene ->
                        if (activeScene == null && activeCustomSceneSlot == null) solidRingColors = ringColors
                        activeScene = scene
                        activeCustomSceneSlot = null
                        ringsEnabled = true
                        bleManager?.setScene(scene.id)
                    },
                    onCustomSceneSelected = { scene ->
                        if (activeScene == null && activeCustomSceneSlot == null) solidRingColors = ringColors
                        activeScene = null
                        activeCustomSceneSlot = scene.slot
                        ringsEnabled = true
                        bleManager?.uploadCustomScene(scene, playAfterUpload = true)
                    },
                    onCreateCustomScene = {
                        nextCustomSceneSlot(customScenes)?.let { slot ->
                            customSceneEditor = CustomScene.create(slot, solidRingColors)
                            customSceneEditorIsNew = true
                        }
                    },
                    onEditCustomScene = { scene ->
                        customSceneEditor = scene
                        customSceneEditorIsNew = false
                    },
                    onDeleteCustomScene = { customScenePendingDelete = it },
                    onStopScene = {
                        activeScene = null
                        activeCustomSceneSlot = null
                        ringColors = solidRingColors
                        bleManager?.setScene(null)
                    },
                )

                DashboardTab.Config -> ConfigScreen(
                    modifier = Modifier.padding(padding),
                    connectionState = connectionState,
                    deviceName = bleManager?.deviceName ?: BleProtocol.DEVICE_NAME,
                    firmwareVersion = controllerSnapshot?.firmwareVersion,
                    onReconnect = { bleManager?.start() },
                    simplifiedPreview = simplifiedPreview,
                    onSimplifiedPreviewChanged = { enabled ->
                        simplifiedPreview = enabled
                        onSimplifiedPreviewChanged(enabled)
                    },
                    vehicleAutomationEnabled = vehicleAutomationEnabled,
                    onVehicleAutomationChanged = {
                        vehicleAutomationEnabled = it
                        bleManager?.setVehicleAutomation(it)
                    },
                    daylightSignalActive = daylightSignalActive,
                    daylightAutomationEnabled = daylightAutomationEnabled,
                    daylightBrightnessPercent = daylightBrightnessPercent,
                    onDaylightAutomationChanged = { enabled ->
                        daylightAutomationEnabled = enabled
                        bleManager?.setDaylightAutomation(enabled, daylightBrightnessPercent)
                    },
                    onDaylightBrightnessChanged = { daylightBrightnessPercent = it },
                    onDaylightBrightnessCommitted = {
                        bleManager?.setDaylightAutomation(
                            daylightAutomationEnabled,
                            daylightBrightnessPercent,
                        )
                    },
                )
            }
        }

        if (colorEditorOpen) {
            ColorAdjustmentDialog(
                enabled = ringsEnabled,
                colors = draftRingColors,
                selectedRing = selectedRing,
                brightness = draftBrightness,
                favorites = draftFavorites,
                simplifiedPreview = simplifiedPreview,
                onColorChanged = { color ->
                    draftRingColors = if (selectedRing == null) {
                        List(4) { color }
                    } else {
                        draftRingColors.mapIndexed { index, current ->
                            if (index == selectedRing) color else current
                        }
                    }
                },
                onFavoriteAdded = { color ->
                    if (draftFavorites.size < 12 && draftFavorites.none { it.toHex() == color.toHex() }) {
                        draftFavorites = draftFavorites + color
                    }
                },
                onFavoriteRemoved = { index ->
                    if (draftFavorites.size > 1) {
                        draftFavorites = draftFavorites.filterIndexed { itemIndex, _ -> itemIndex != index }
                    }
                },
                onBrightnessChanged = { draftBrightness = it },
                onCancel = { colorEditorOpen = false },
                onSave = {
                    val committedColor = selectedRing?.let(draftRingColors::get) ?: draftRingColors.first()
                    val favoritesChanged = draftFavorites.map(Color::toHex) != favoriteColors.map(Color::toHex)
                    if (activeScene != null) bleManager?.setScene(null)
                    activeScene = null
                    activeCustomSceneSlot = null
                    ringColors = draftRingColors
                    solidRingColors = draftRingColors
                    favoriteColors = draftFavorites
                    brightness = draftBrightness
                    if (favoritesChanged) bleManager?.setFavorites(draftFavorites)
                    bleManager?.setColorAndBrightness(selectedRing, committedColor, draftBrightness)
                    colorEditorOpen = false
                },
            )
        }

        customSceneEditor?.let { scene ->
            CustomSceneEditorDialog(
                initialScene = scene,
                favoriteColors = favoriteColors,
                brightness = brightness,
                simplifiedPreview = simplifiedPreview,
                isNew = customSceneEditorIsNew,
                onCancel = { customSceneEditor = null },
                onSave = { saved ->
                    val normalized = saved.normalized()
                    val updatedScenes = (customScenes.filterNot { it.slot == normalized.slot } + normalized)
                        .sortedBy(CustomScene::slot)
                    customScenes = updatedScenes
                    onCustomScenesChanged(updatedScenes)
                    bleManager?.uploadCustomScene(
                        normalized,
                        playAfterUpload = activeCustomSceneSlot == normalized.slot,
                    )
                    customSceneEditor = null
                },
            )
        }

        customScenePendingDelete?.let { scene ->
            AlertDialog(
                onDismissRequest = { customScenePendingDelete = null },
                title = { Text(stringResource(R.string.delete_scene_title)) },
                text = { Text(stringResource(R.string.delete_scene_message, scene.name)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (activeCustomSceneSlot == scene.slot) {
                                activeCustomSceneSlot = null
                                ringColors = solidRingColors
                                bleManager?.setScene(null)
                            }
                            val updatedScenes = customScenes.filterNot { it.slot == scene.slot }
                            customScenes = updatedScenes
                            onCustomScenesChanged(updatedScenes)
                            bleManager?.deleteCustomScene(scene.slot)
                            customScenePendingDelete = null
                        },
                    ) {
                        Text(stringResource(R.string.delete_action), color = Color(0xFFFF6B78))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { customScenePendingDelete = null }) {
                        Text(stringResource(R.string.cancel_action))
                    }
                },
            )
        }
    }
}

internal fun Color.scaledBy(brightness: Float): Color {
    val level = brightness.coerceIn(0f, 1f)
    return Color(red * level, green * level, blue * level, alpha)
}

internal fun perceptualPreviewBrightness(brightness: Float): Float =
    brightness.coerceIn(0f, 1f).pow(0.42f)

internal fun Color.asPreviewEmission(brightness: Float): Color =
    scaledBy(perceptualPreviewBrightness(brightness))

@Composable
private fun DriveScreen(
    modifier: Modifier,
    ringsEnabled: Boolean,
    ringColors: List<Color>,
    selectedRing: Int?,
    brightness: Float,
    activeScene: ScenePreset?,
    activeCustomSceneName: String?,
    connectionState: BleConnectionState,
    simplifiedPreview: Boolean,
    onRingSelected: (Int) -> Unit,
    onPowerClick: () -> Unit,
    onTargetSelected: (Int?) -> Unit,
    onOpenColorEditor: () -> Unit,
) {
    ScreenColumn(modifier) {
        item { AppHeader(connectionState = connectionState) }
        item {
            HaloDashboard(
                enabled = ringsEnabled,
                colors = ringColors,
                selectedRing = selectedRing,
                activeScene = activeScene,
                activeCustomSceneName = activeCustomSceneName,
                brightness = brightness,
                simplifiedPreview = simplifiedPreview,
                onRingSelected = onRingSelected,
                onPowerClick = onPowerClick,
            )
        }
        item { RingTargetSelector(selectedRing = selectedRing, onSelected = onTargetSelected) }
        item { ColorSummaryCard(ringColors, selectedRing, brightness, onOpenColorEditor) }
    }
}

@Composable
private fun ScenesScreen(
    modifier: Modifier,
    ringsEnabled: Boolean,
    ringColors: List<Color>,
    selectedRing: Int?,
    brightness: Float,
    activeScene: ScenePreset?,
    activeCustomSceneSlot: Int?,
    customScenes: List<CustomScene>,
    favoriteColors: List<Color>,
    connectionState: BleConnectionState,
    simplifiedPreview: Boolean,
    onRingSelected: (Int) -> Unit,
    onSceneSelected: (ScenePreset) -> Unit,
    onCustomSceneSelected: (CustomScene) -> Unit,
    onCreateCustomScene: () -> Unit,
    onEditCustomScene: (CustomScene) -> Unit,
    onDeleteCustomScene: (CustomScene) -> Unit,
    onStopScene: () -> Unit,
) {
    val activeCustomScene = activeCustomSceneSlot?.let { slot -> customScenes.firstOrNull { it.slot == slot } }
    ScreenColumn(modifier) {
        item { AppHeader(sectionRes = R.string.nav_scenes, connectionState = connectionState) }
        item {
            CompactCarPreview(
                enabled = ringsEnabled,
                colors = ringColors,
                selectedRing = selectedRing,
                activeScene = activeScene,
                activeCustomSceneName = activeCustomScene?.name,
                brightness = brightness,
                simplifiedPreview = simplifiedPreview,
                onRingSelected = onRingSelected,
            )
        }
        item {
            SectionHeader(
                title = stringResource(R.string.show_modes),
                trailing = stringResource(R.string.parked_only),
            )
        }
        item {
            CustomScenesHeader(
                sceneCount = customScenes.size,
                canAdd = customScenes.size < MAX_CUSTOM_SCENES,
                onCreate = onCreateCustomScene,
            )
        }
        if (customScenes.isEmpty()) {
            item { EmptyCustomScenesCard(onCreate = onCreateCustomScene) }
        } else {
            items(customScenes, key = { "custom-${it.slot}" }) { scene ->
                CustomSceneCard(
                    scene = scene,
                    selected = scene.slot == activeCustomSceneSlot,
                    onPlay = { onCustomSceneSelected(scene) },
                    onEdit = { onEditCustomScene(scene) },
                    onDelete = { onDeleteCustomScene(scene) },
                )
            }
        }
        SceneCategory.entries.forEach { category ->
            val scenes = ScenePreset.entries.filter { it.category == category }
            item { SceneCategoryHeader(category, scenes.size) }
            items(scenes, key = { it.id }) { scene ->
                SceneCard(
                    scene = scene,
                    favoriteColors = favoriteColors,
                    selected = scene == activeScene,
                    onClick = { onSceneSelected(scene) },
                )
            }
        }
        if (activeScene != null || activeCustomSceneSlot != null) {
            item {
                Button(
                    onClick = onStopScene,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppSurfaceRaised,
                        contentColor = AppText,
                    ),
                ) {
                    Text(stringResource(R.string.stop_scene), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ConfigScreen(
    modifier: Modifier,
    connectionState: BleConnectionState,
    deviceName: String,
    firmwareVersion: String?,
    onReconnect: () -> Unit,
    simplifiedPreview: Boolean,
    onSimplifiedPreviewChanged: (Boolean) -> Unit,
    vehicleAutomationEnabled: Boolean,
    onVehicleAutomationChanged: (Boolean) -> Unit,
    daylightSignalActive: Boolean,
    daylightAutomationEnabled: Boolean,
    daylightBrightnessPercent: Int,
    onDaylightAutomationChanged: (Boolean) -> Unit,
    onDaylightBrightnessChanged: (Int) -> Unit,
    onDaylightBrightnessCommitted: () -> Unit,
) {
    ScreenColumn(modifier) {
        item { AppHeader(sectionRes = R.string.nav_config, connectionState = connectionState) }
        item {
            ControllerStatusCard(
                connectionState = connectionState,
                deviceName = deviceName,
                firmwareVersion = firmwareVersion,
                onReconnect = onReconnect,
            )
        }
        item {
            SectionHeader(
                title = stringResource(R.string.preview_settings),
                trailing = stringResource(R.string.local_setting),
            )
        }
        item {
            PreviewModeCard(
                simplified = simplifiedPreview,
                onSimplifiedChanged = onSimplifiedPreviewChanged,
            )
        }
        item {
            SectionHeader(
                title = stringResource(R.string.input_rules),
                trailing = stringResource(R.string.runs_on_controller),
            )
        }
        item { PhysicalButtonCard() }
        item {
            VehicleAutomationCard(
                enabled = vehicleAutomationEnabled,
                onEnabledChanged = onVehicleAutomationChanged,
            )
        }
        item {
            DaylightAutomationCard(
                signalActive = daylightSignalActive,
                enabled = daylightAutomationEnabled,
                brightnessPercent = daylightBrightnessPercent,
                onEnabledChanged = onDaylightAutomationChanged,
                onBrightnessChanged = onDaylightBrightnessChanged,
                onBrightnessChangeFinished = onDaylightBrightnessCommitted,
            )
        }
    }
}

@Composable
private fun ScreenColumn(
    modifier: Modifier,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
private fun AppHeader(
    @StringRes sectionRes: Int? = null,
    connectionState: BleConnectionState,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "D4WID",
                    fontSize = 28.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                )
                if (sectionRes != null) {
                    Text(
                        text = " / ${stringResource(sectionRes).uppercase()}",
                        color = AppOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                }
            }
            Text(
                text = stringResource(R.string.controller_subtitle),
                color = AppMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.vehicle_name),
                fontSize = 10.sp,
                color = AppText,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
            )
            Text(
                text = connectionState.label().uppercase(),
                color = connectionState.color(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
            )
        }
    }
}

@Composable
private fun HaloDashboard(
    enabled: Boolean,
    colors: List<Color>,
    selectedRing: Int?,
    activeScene: ScenePreset?,
    activeCustomSceneName: String?,
    brightness: Float,
    simplifiedPreview: Boolean,
    onRingSelected: (Int) -> Unit,
    onPowerClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, AppLine),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, top = 13.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeCustomSceneName?.uppercase()
                            ?: activeScene?.let { stringResource(it.titleRes).uppercase() }
                            ?: stringResource(R.string.live_halos),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = if (activeScene != null || activeCustomSceneName != null) AppOrange else AppMuted,
                    )
                    Text(
                        text = if (enabled) stringResource(R.string.tap_halo_hint)
                        else stringResource(R.string.lighting_off),
                        fontSize = 13.sp,
                        color = AppText,
                    )
                }
                Button(
                    onClick = onPowerClick,
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (enabled) AppOrange else Color(0xFF30343B),
                        contentColor = if (enabled) Color.Black else AppText,
                    ),
                    contentPadding = PaddingValues(horizontal = 15.dp),
                ) {
                    Text(
                        text = stringResource(if (enabled) R.string.power_on else R.string.power_off),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                    )
                }
            }
            HorizontalDivider(color = AppLine)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF25292E), Color(0xFF16191E)),
                        ),
                    )
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            ) {
                if (simplifiedPreview) {
                    SimplifiedHaloPreview(enabled, colors, selectedRing, brightness, onRingSelected)
                } else {
                    ChallengerFrontPreview(enabled, colors, selectedRing, brightness, onRingSelected)
                }
            }
        }
    }
}

@Composable
private fun CompactCarPreview(
    enabled: Boolean,
    colors: List<Color>,
    selectedRing: Int?,
    activeScene: ScenePreset?,
    activeCustomSceneName: String?,
    brightness: Float,
    simplifiedPreview: Boolean,
    onRingSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF16191E), RoundedCornerShape(16.dp))
            .border(1.dp, AppLine, RoundedCornerShape(16.dp))
            .padding(10.dp),
    ) {
        if (simplifiedPreview) {
            SimplifiedHaloPreview(enabled, colors, selectedRing, brightness, onRingSelected)
        } else {
            ChallengerFrontPreview(enabled, colors, selectedRing, brightness, onRingSelected)
        }
        Text(
            text = activeCustomSceneName?.let { stringResource(R.string.scene_running, it) }
                ?: activeScene?.let { stringResource(R.string.scene_running, stringResource(it.titleRes)) }
                ?: stringResource(R.string.no_scene_running),
            modifier = Modifier.padding(start = 6.dp, top = 3.dp, bottom = 3.dp),
            color = if (activeScene == null && activeCustomSceneName == null) AppMuted else AppOrange,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
        )
    }
}

@Composable
internal fun SimplifiedHaloPreview(
    enabled: Boolean,
    colors: List<Color>,
    selectedRing: Int?,
    brightness: Float,
    onRingSelected: ((Int) -> Unit)?,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(822f / 390f),
    ) {
        val previewDescription = stringResource(R.string.simplified_preview_description)
        val centers = listOf(0.145f, 0.355f, 0.645f, 0.855f)

        Canvas(
            Modifier
                .matchParentSize()
                .semantics { contentDescription = previewDescription },
        ) {
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF1D2229), Color(0xFF090B0F))),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
            )
            drawLine(
                color = Color(0xFF343A43),
                start = Offset(size.width * 0.46f, size.height * 0.50f),
                end = Offset(size.width * 0.54f, size.height * 0.50f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            centers.forEachIndexed { index, centerX ->
                val perceivedBrightness = if (enabled) perceptualPreviewBrightness(brightness) else 0f
                val color = if (enabled) colors[index].asPreviewEmission(brightness) else Color(0xFF353A42)
                val emphasized = selectedRing == null || selectedRing == index
                val center = Offset(size.width * centerX, size.height * 0.48f)
                val radius = size.minDimension * 0.20f
                val glowAlpha = when {
                    !enabled -> 0.08f
                    emphasized -> 0.20f + perceivedBrightness * 0.72f
                    else -> 0.10f + perceivedBrightness * 0.36f
                }

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = glowAlpha * 0.62f),
                            color.copy(alpha = glowAlpha * 0.28f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = radius * 1.58f,
                    ),
                    center = center,
                    radius = radius * 1.58f,
                )
                drawCircle(
                    color = color.copy(
                        alpha = if (enabled) 0.18f + perceivedBrightness * 0.30f else 0.08f,
                    ),
                    center = center,
                    radius = radius * 1.05f,
                )
                drawCircle(
                    color = color,
                    center = center,
                    radius = radius,
                    style = Stroke(width = radius * 0.26f),
                )
                drawCircle(
                    color = Color(0xFF050609),
                    center = center,
                    radius = radius * 0.70f,
                )
                drawCircle(
                    color = Color.White.copy(
                        alpha = if (enabled) 0.20f + perceivedBrightness * 0.42f else 0.10f,
                    ),
                    center = center,
                    radius = radius * 0.88f,
                    style = Stroke(width = 1.dp.toPx()),
                )
                if (selectedRing == index) {
                    drawCircle(
                        color = AppOrange,
                        center = center,
                        radius = radius * 1.20f,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
        }

        val targetSize = maxHeight * 0.68f
        val centerY = maxHeight * 0.48f
        if (onRingSelected != null) {
            centers.forEachIndexed { index, centerX ->
                RingHitTarget(
                    index = index,
                    selected = selectedRing == index,
                    onSelected = onRingSelected,
                    modifier = Modifier
                        .offset(
                            x = maxWidth * centerX - targetSize / 2,
                            y = centerY - targetSize / 2,
                        )
                        .size(targetSize),
                )
            }
        }
    }
}

@Composable
internal fun ChallengerFrontPreview(
    enabled: Boolean,
    colors: List<Color>,
    selectedRing: Int?,
    brightness: Float,
    onRingSelected: ((Int) -> Unit)?,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(822f / 502f),
    ) {
        val carBitmap = ImageBitmap.imageResource(R.drawable.challenger_front_reference)
        val haloMaskBitmap = ImageBitmap.imageResource(R.drawable.challenger_halo_mask)
        val haloCenters = listOf(0.169f, 0.250f, 0.750f, 0.831f)
        val maskClipBounds = listOf(
            floatArrayOf(0.105f, 0.395f, 0.205f, 0.565f),
            floatArrayOf(0.205f, 0.395f, 0.315f, 0.565f),
            floatArrayOf(0.685f, 0.395f, 0.795f, 0.565f),
            floatArrayOf(0.795f, 0.395f, 0.895f, 0.565f),
        )

        Canvas(Modifier.matchParentSize()) {
            drawOval(
                color = Color.Black.copy(alpha = 0.32f),
                topLeft = Offset(size.width * 0.12f, size.height * 0.842f),
                size = Size(size.width * 0.76f, size.height * 0.052f),
            )
            drawOval(
                color = Color.Black.copy(alpha = 0.16f),
                topLeft = Offset(size.width * 0.08f, size.height * 0.83f),
                size = Size(size.width * 0.84f, size.height * 0.082f),
            )
        }

        Image(
            bitmap = carBitmap,
            contentDescription = stringResource(R.string.challenger_preview_description),
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds,
            alpha = 0.76f,
            filterQuality = FilterQuality.High,
            colorFilter = ColorFilter.colorMatrix(
                ColorMatrix().apply { setToSaturation(0.62f) },
            ),
        )

        Canvas(Modifier.matchParentSize()) {
            clipRect(
                left = size.width * 0.095f,
                top = size.height * 0.405f,
                right = size.width * 0.905f,
                bottom = size.height * 0.56f,
            ) {
                haloCenters.forEachIndexed { index, centerX ->
                    val perceivedBrightness = if (enabled) perceptualPreviewBrightness(brightness) else 0f
                    val haloColor = if (enabled) colors[index].asPreviewEmission(brightness) else Color(0xFF343842)
                    val emphasized = selectedRing == null || selectedRing == index
                    val glowAlpha = when {
                        !enabled -> 0.04f
                        emphasized -> 0.18f + perceivedBrightness * 0.72f
                        else -> 0.08f + perceivedBrightness * 0.38f
                    }
                    val center = Offset(size.width * centerX, size.height * 0.472f)
                    val radius = size.width * 0.062f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                haloColor.copy(alpha = glowAlpha * 0.82f),
                                haloColor.copy(alpha = glowAlpha * 0.46f),
                                haloColor.copy(alpha = glowAlpha * 0.18f),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = radius * 1.02f,
                        ),
                        center = center,
                        radius = radius * 1.02f,
                    )
                }
            }
        }

        maskClipBounds.forEachIndexed { index, bounds ->
            val perceivedBrightness = if (enabled) perceptualPreviewBrightness(brightness) else 0f
            val haloColor = if (enabled) colors[index].asPreviewEmission(brightness) else Color(0xFF343842)
            listOf(0.62f, 1f).forEach { layerAlpha ->
                Image(
                    bitmap = haloMaskBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .drawWithContent {
                            clipRect(
                                left = size.width * bounds[0],
                                top = size.height * bounds[1],
                                right = size.width * bounds[2],
                                bottom = size.height * bounds[3],
                            ) {
                                this@drawWithContent.drawContent()
                            }
                        },
                    contentScale = ContentScale.FillBounds,
                    alpha = layerAlpha * if (enabled) (0.30f + perceivedBrightness * 0.70f) else 1f,
                    filterQuality = FilterQuality.High,
                    colorFilter = ColorFilter.tint(
                        color = haloColor,
                        blendMode = BlendMode.SrcIn,
                    ),
                )
            }
        }

        val hitWidth = maxWidth * 0.085f
        val hitHeight = maxHeight * 0.135f
        val centerY = maxHeight * 0.472f

        if (onRingSelected != null) {
            haloCenters.forEachIndexed { index, centerX ->
                RingHitTarget(
                    index = index,
                    selected = selectedRing == index,
                    onSelected = onRingSelected,
                    modifier = Modifier.offset(
                        x = maxWidth * centerX - hitWidth / 2,
                        y = centerY - hitHeight / 2,
                    )
                        .width(hitWidth)
                        .height(hitHeight),
                )
            }
        }
    }
}

@Composable
private fun RingHitTarget(
    index: Int,
    selected: Boolean,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ringDescription = stringResource(R.string.ring_description, index + 1)
    Box(
        modifier = modifier
            .semantics {
                contentDescription = ringDescription
                this.selected = selected
            }
            .clickable { onSelected(index) },
    )
}

@Composable
private fun RingTargetSelector(
    selectedRing: Int?,
    onSelected: (Int?) -> Unit,
) {
    val editing = if (selectedRing == null) {
        stringResource(R.string.all_rings)
    } else {
        stringResource(R.string.ring_label, selectedRing + 1)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(stringResource(R.string.editing), editing.uppercase())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppSurface, RoundedCornerShape(14.dp))
                .border(1.dp, AppLine, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TargetChip(
                label = stringResource(R.string.all_short),
                description = stringResource(R.string.edit_all_rings_description),
                selected = selectedRing == null,
                modifier = Modifier.weight(1.4f),
                onClick = { onSelected(null) },
            )
            repeat(4) { index ->
                TargetChip(
                    label = "${index + 1}",
                    description = stringResource(R.string.edit_ring_description, index + 1),
                    selected = selectedRing == index,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelected(index) },
                )
            }
        }
    }
}

@Composable
private fun TargetChip(
    label: String,
    description: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .background(
                if (selected) AppSurfaceRaised else Color.Transparent,
                RoundedCornerShape(10.dp),
            )
            .border(
                width = 1.dp,
                color = if (selected) Color(0xFF444A54) else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            )
            .semantics {
                contentDescription = description
                this.selected = selected
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (selected) AppText else AppMuted,
            letterSpacing = 0.7.sp,
        )
    }
}

@Composable
private fun ColorSummaryCard(
    colors: List<Color>,
    selectedRing: Int?,
    brightness: Float,
    onClick: () -> Unit,
) {
    val description = stringResource(R.string.open_color_editor_description)
    val shownColors = selectedRing?.let { listOf(colors[it]) } ?: colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSurface, RoundedCornerShape(14.dp))
            .border(1.dp, AppLine, RoundedCornerShape(14.dp))
            .semantics { contentDescription = description }
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.color_and_brightness), fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.color_brightness_summary, (brightness * 100).toInt()),
                color = AppMuted,
                fontSize = 11.sp,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            shownColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(if (shownColors.size == 1) 38.dp else 27.dp)
                        .shadow(8.dp, CircleShape, ambientColor = color, spotColor = color)
                        .background(color, CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.38f), CircleShape),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.change_action),
            color = AppOrange,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
        )
    }
}

@Composable
private fun ColorAdjustmentDialog(
    enabled: Boolean,
    colors: List<Color>,
    selectedRing: Int?,
    brightness: Float,
    favorites: List<Color>,
    simplifiedPreview: Boolean,
    onColorChanged: (Color) -> Unit,
    onFavoriteAdded: (Color) -> Unit,
    onFavoriteRemoved: (Int) -> Unit,
    onBrightnessChanged: (Float) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val selectedColor = selectedRing?.let(colors::get) ?: colors.first()
    val sliderDescription = stringResource(R.string.brightness_slider_description)
    AdjustmentDialogFrame(
        title = stringResource(R.string.color_editor_title),
        subtitle = selectedRing?.let { stringResource(R.string.ring_label, it + 1) }
            ?: stringResource(R.string.all_rings),
        enabled = enabled,
        colors = colors,
        selectedRing = selectedRing,
        brightness = brightness,
        simplifiedPreview = simplifiedPreview,
        onCancel = onCancel,
        onSave = onSave,
    ) {
        ColorControlPanel(
            favorites = favorites,
            selectedColor = selectedColor,
            onColorSelected = onColorChanged,
            onFavoriteAdded = onFavoriteAdded,
            onFavoriteRemoved = onFavoriteRemoved,
        )
        Slider(
            value = brightness,
            onValueChange = onBrightnessChanged,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = sliderDescription },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = AppOrange,
                inactiveTrackColor = Color(0xFF353A42),
            ),
        )
    }
}

@Composable
private fun AdjustmentDialogFrame(
    title: String,
    subtitle: String,
    enabled: Boolean,
    colors: List<Color>,
    selectedRing: Int?,
    brightness: Float,
    simplifiedPreview: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    controls: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF08090C))
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = AppText, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text(subtitle.uppercase(), color = AppOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    stringResource(R.string.preview_changes),
                    color = AppMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                )
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .background(Color(0xFF16191E), RoundedCornerShape(18.dp))
                    .border(1.dp, AppLine, RoundedCornerShape(18.dp))
                    .padding(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp),
                ) {
                    if (simplifiedPreview) {
                        SimplifiedHaloPreview(enabled, colors, selectedRing, brightness, null)
                    } else {
                        ChallengerFrontPreview(enabled, colors, selectedRing, brightness, null)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Surface(
                color = Color(0xFF0D0F13),
                contentColor = AppText,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(1.dp, AppLine),
                shadowElevation = 18.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    controls()
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppSurfaceRaised,
                                contentColor = AppText,
                            ),
                        ) {
                            Text(stringResource(R.string.cancel_action), fontWeight = FontWeight.Black)
                        }
                        Button(
                            onClick = onSave,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppOrange,
                                contentColor = Color.Black,
                            ),
                        ) {
                            Text(stringResource(R.string.save_action), fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomScenesHeader(
    sceneCount: Int,
    canAdd: Boolean,
    onCreate: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.your_scenes).uppercase(),
                color = AppOrange,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.1.sp,
            )
            Text(
                text = stringResource(R.string.your_scenes_hint),
                color = AppMuted,
                fontSize = 9.sp,
            )
        }
        Text(
            text = "$sceneCount/$MAX_CUSTOM_SCENES",
            color = AppMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
        Button(
            onClick = onCreate,
            enabled = canAdd,
            modifier = Modifier.height(36.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppOrange,
                contentColor = Color.Black,
                disabledContainerColor = AppSurfaceRaised,
                disabledContentColor = AppMuted,
            ),
            contentPadding = PaddingValues(horizontal = 13.dp),
        ) {
            Text(stringResource(R.string.new_scene), fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun EmptyCustomScenesCard(onCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSurface, RoundedCornerShape(14.dp))
            .border(1.dp, AppLine, RoundedCornerShape(14.dp))
            .clickable(onClick = onCreate)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(AppOrange.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", color = AppOrange, fontSize = 23.sp, fontWeight = FontWeight.Light)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.create_first_scene), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.create_first_scene_hint), color = AppMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun CustomSceneCard(
    scene: CustomScene,
    selected: Boolean,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val previewColors = customScenePreviewColors(scene)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) Color(0xFF211B17) else AppSurface, RoundedCornerShape(14.dp))
            .border(1.dp, if (selected) AppOrange else AppLine, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                previewColors.forEach { color ->
                    Box(Modifier.size(20.dp).border(3.dp, color, CircleShape))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(scene.name, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(
                        R.string.custom_scene_meta,
                        scene.moments.size,
                        formatSceneDuration(scene.durationMs),
                    ),
                    color = AppOrange,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                stringResource(if (selected) R.string.scene_active else R.string.custom_badge),
                modifier = Modifier
                    .background(
                        if (selected) AppOrange.copy(alpha = 0.14f) else AppSurfaceRaised,
                        RoundedCornerShape(7.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                color = if (selected) AppOrange else AppMuted,
                fontWeight = FontWeight.Black,
                fontSize = 8.sp,
                letterSpacing = 0.7.sp,
            )
        }
        if (scene.description.isNotBlank()) {
            Text(scene.description, color = AppMuted, fontSize = 11.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onPlay,
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) AppSurfaceRaised else AppOrange,
                    contentColor = if (selected) AppText else Color.Black,
                ),
            ) {
                Text(
                    stringResource(if (selected) R.string.restart_scene else R.string.play_scene),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Button(
                onClick = onEdit,
                modifier = Modifier.height(40.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppSurfaceRaised),
                contentPadding = PaddingValues(horizontal = 13.dp),
            ) {
                Text(stringResource(R.string.edit_action), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onDelete,
                modifier = Modifier.height(40.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x1FFF6B78),
                    contentColor = Color(0xFFFF6B78),
                ),
                contentPadding = PaddingValues(horizontal = 13.dp),
            ) {
                Text(stringResource(R.string.delete_action), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SceneCategoryHeader(category: SceneCategory, sceneCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(category.titleRes).uppercase(),
            color = AppOrange,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.1.sp,
        )
        Box(modifier = Modifier.height(1.dp).weight(1f).background(AppLine))
        Text(
            text = stringResource(R.string.scene_count, sceneCount),
            color = AppMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SceneCard(
    scene: ScenePreset,
    favoriteColors: List<Color>,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val previewColors = colorsForScene(scene, scene.previewSeconds, favoriteColors)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) Color(0xFF211B17) else AppSurface, RoundedCornerShape(14.dp))
            .border(1.dp, if (selected) AppOrange else AppLine, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                previewColors.forEach { color ->
                    Box(modifier = Modifier.size(20.dp).border(3.dp, color, CircleShape))
                }
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = stringResource(scene.titleRes),
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(scene.badgeRes),
                modifier = Modifier
                    .background(
                        if (selected) AppOrange.copy(alpha = 0.14f) else AppSurfaceRaised,
                        RoundedCornerShape(7.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                color = if (selected) AppOrange else AppMuted,
                fontWeight = FontWeight.Black,
                fontSize = 8.sp,
                letterSpacing = 0.8.sp,
            )
        }
        Text(
            text = stringResource(scene.subtitleRes),
            color = AppMuted,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ControllerStatusCard(
    connectionState: BleConnectionState,
    deviceName: String,
    firmwareVersion: String?,
    onReconnect: () -> Unit,
) {
    val connected = connectionState == BleConnectionState.Connected
    val subtitle = when (connectionState) {
        BleConnectionState.Connected -> stringResource(
            R.string.controller_status_connected,
            deviceName,
            firmwareVersion ?: "–",
        )
        BleConnectionState.Scanning -> stringResource(R.string.controller_status_scanning)
        BleConnectionState.Connecting -> stringResource(R.string.controller_status_connecting)
        BleConnectionState.PermissionRequired -> stringResource(R.string.controller_status_permission)
        BleConnectionState.BluetoothOff -> stringResource(R.string.controller_status_bluetooth_off)
        BleConnectionState.Unsupported -> stringResource(R.string.controller_status_unsupported)
        BleConnectionState.Disconnected -> stringResource(R.string.controller_status_disconnected)
        BleConnectionState.LocalDemo -> stringResource(R.string.controller_status_demo)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSurface, RoundedCornerShape(14.dp))
            .border(1.dp, AppLine, RoundedCornerShape(14.dp))
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(connectionState.color(), CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.controller_status_title), fontWeight = FontWeight.Bold)
            Text(subtitle, color = AppMuted, fontSize = 11.sp)
        }
        if (connected || connectionState == BleConnectionState.LocalDemo) {
            Text(
                connectionState.label().uppercase(),
                color = connectionState.color(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
            )
        } else {
            Button(
                onClick = onReconnect,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppOrange,
                    contentColor = Color.Black,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Text(stringResource(R.string.connect_retry), fontWeight = FontWeight.Black, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun BleConnectionState.label(): String = stringResource(
    when (this) {
        BleConnectionState.LocalDemo -> R.string.connection_local
        BleConnectionState.PermissionRequired -> R.string.connection_permission
        BleConnectionState.BluetoothOff -> R.string.connection_bluetooth_off
        BleConnectionState.Unsupported -> R.string.connection_unsupported
        BleConnectionState.Scanning -> R.string.connection_scanning
        BleConnectionState.Connecting -> R.string.connection_connecting
        BleConnectionState.Connected -> R.string.connection_connected
        BleConnectionState.Disconnected -> R.string.connection_disconnected
    },
)

private fun BleConnectionState.color(): Color = when (this) {
    BleConnectionState.Connected, BleConnectionState.LocalDemo -> Color(0xFF52D995)
    BleConnectionState.Scanning, BleConnectionState.Connecting -> Color(0xFFFFB347)
    BleConnectionState.PermissionRequired,
    BleConnectionState.BluetoothOff,
    BleConnectionState.Unsupported,
    BleConnectionState.Disconnected,
    -> Color(0xFFFF6B78)
}

@Composable
private fun PhysicalButtonCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppLine),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ButtonGlyph()
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cabin_button), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(stringResource(R.string.cabin_button_subtitle), color = AppMuted, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = AppLine)
            Spacer(Modifier.height(6.dp))
            ButtonRule(stringResource(R.string.action_tap), stringResource(R.string.action_tap_result))
            ButtonRule(stringResource(R.string.action_hold), stringResource(R.string.action_hold_result))
            ButtonRule(stringResource(R.string.action_rgb_led), stringResource(R.string.action_rgb_led_result))
        }
    }
}

@Composable
private fun ButtonGlyph() {
    Canvas(
        modifier = Modifier
            .size(42.dp)
            .background(Color(0xFF24282F), CircleShape)
            .border(1.dp, Color(0xFF414750), CircleShape)
            .padding(10.dp),
    ) {
        drawCircle(color = Color(0xFF08090C), radius = size.minDimension * 0.42f)
        drawCircle(
            color = AppOrange,
            radius = size.minDimension * 0.32f,
            style = Stroke(width = 2.dp.toPx()),
        )
        drawLine(
            color = AppOrange,
            start = Offset(size.width / 2, size.height * 0.06f),
            end = Offset(size.width / 2, size.height * 0.42f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun ButtonRule(action: String, result: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = action,
            modifier = Modifier.width(68.dp),
            color = AppOrange,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.7.sp,
        )
        Text(result, color = AppText, fontSize = 12.sp)
    }
}

@Composable
private fun VehicleAutomationCard(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSurface, RoundedCornerShape(14.dp))
            .border(1.dp, AppLine, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(if (enabled) Color.White else Color(0xFF4A4F57), CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.vehicle_light_signal), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.vehicle_light_signal_result), color = AppMuted, fontSize = 11.sp)
        }
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChanged,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = AppOrange,
                uncheckedThumbColor = AppMuted,
                uncheckedTrackColor = Color(0xFF30343B),
            ),
        )
    }
}

@Composable
private fun DaylightAutomationCard(
    signalActive: Boolean,
    enabled: Boolean,
    brightnessPercent: Int,
    onEnabledChanged: (Boolean) -> Unit,
    onBrightnessChanged: (Int) -> Unit,
    onBrightnessChangeFinished: () -> Unit,
) {
    val sliderDescription = stringResource(R.string.daylight_brightness_slider_description)
    Card(
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppLine),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            if (signalActive) Color(0xFF52D995) else Color(0xFF4A4F57),
                            CircleShape,
                        ),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.daylight_signal_title), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.daylight_signal_subtitle), color = AppMuted, fontSize = 11.sp)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = AppOrange,
                        uncheckedThumbColor = AppMuted,
                        uncheckedTrackColor = Color(0xFF30343B),
                    ),
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = AppLine)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.daylight_brightness_label),
                        color = AppText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(
                            if (signalActive) R.string.daylight_signal_active else R.string.daylight_signal_inactive,
                        ),
                        color = if (signalActive) Color(0xFF52D995) else AppMuted,
                        fontSize = 10.sp,
                    )
                }
                Text(
                    text = "$brightnessPercent%",
                    color = AppOrange,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Slider(
                value = brightnessPercent.toFloat(),
                onValueChange = { onBrightnessChanged((it / 5f).roundToInt() * 5) },
                onValueChangeFinished = onBrightnessChangeFinished,
                valueRange = 0f..100f,
                steps = 19,
                modifier = Modifier.semantics { contentDescription = sliderDescription },
                colors = SliderDefaults.colors(
                    thumbColor = AppOrange,
                    activeTrackColor = AppOrange,
                    inactiveTrackColor = Color(0xFF30343B),
                ),
            )
            Text(
                text = stringResource(R.string.daylight_event_hint),
                color = AppMuted,
                fontSize = 10.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

@Composable
private fun PreviewModeCard(
    simplified: Boolean,
    onSimplifiedChanged: (Boolean) -> Unit,
) {
    val toggleDescription = stringResource(R.string.simplified_preview_toggle_description)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSurface, RoundedCornerShape(14.dp))
            .border(1.dp, AppLine, RoundedCornerShape(14.dp))
            .clickable { onSimplifiedChanged(!simplified) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.width(58.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(2) {
                Canvas(Modifier.size(11.dp)) {
                    drawCircle(
                        color = if (simplified) AppCyan else AppMuted,
                        radius = size.minDimension * 0.38f,
                        style = Stroke(width = 1.8.dp.toPx()),
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            repeat(2) {
                Canvas(Modifier.size(11.dp)) {
                    drawCircle(
                        color = if (simplified) AppCyan else AppMuted,
                        radius = size.minDimension * 0.38f,
                        style = Stroke(width = 1.8.dp.toPx()),
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.simplified_preview), fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.simplified_preview_subtitle),
                color = AppMuted,
                fontSize = 11.sp,
            )
        }
        Switch(
            checked = simplified,
            onCheckedChange = onSimplifiedChanged,
            modifier = Modifier.semantics { contentDescription = toggleDescription },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = AppCyan,
                uncheckedThumbColor = AppMuted,
                uncheckedTrackColor = Color(0xFF30343B),
            ),
        )
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(
            trailing,
            color = AppMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.1.sp,
        )
    }
}

@Composable
private fun DashboardNavigation(
    currentTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit,
) {
    Column {
        HorizontalDivider(color = AppLine.copy(alpha = 0.72f))
        NavigationBar(
            containerColor = Color(0xFF101216),
            contentColor = AppText,
            tonalElevation = 0.dp,
        ) {
            DashboardTab.entries.forEach { tab ->
                val selected = currentTab == tab
                val description = stringResource(tab.descriptionRes)
                NavigationBarItem(
                    selected = selected,
                    onClick = { onTabSelected(tab) },
                    icon = { NavigationGlyph(tab = tab, selected = selected) },
                    label = {
                        Text(
                            text = stringResource(tab.labelRes),
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                    modifier = Modifier.semantics {
                        contentDescription = description
                        this.selected = selected
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppOrange,
                        selectedTextColor = AppText,
                        indicatorColor = AppOrange.copy(alpha = 0.14f),
                        unselectedIconColor = AppMuted,
                        unselectedTextColor = AppMuted,
                    ),
                )
            }
        }
    }
}

@Composable
private fun NavigationGlyph(tab: DashboardTab, selected: Boolean) {
    val color = if (selected) AppOrange else AppMuted
    Canvas(Modifier.size(23.dp)) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        when (tab) {
            DashboardTab.Drive -> {
                drawCircle(color, radius = size.minDimension * 0.36f, style = stroke)
                drawCircle(color, radius = size.minDimension * 0.09f)
                repeat(3) { index ->
                    val angle = index * 120f
                    rotate(angle) {
                        drawLine(
                            color,
                            Offset(size.width / 2, size.height * 0.14f),
                            Offset(size.width / 2, size.height * 0.39f),
                            strokeWidth = 1.8.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
            DashboardTab.Scenes -> {
                val path = Path().apply {
                    moveTo(size.width * 0.28f, size.height * 0.18f)
                    lineTo(size.width * 0.78f, size.height * 0.50f)
                    lineTo(size.width * 0.28f, size.height * 0.82f)
                    close()
                }
                drawPath(path, color, style = stroke)
            }
            DashboardTab.Config -> {
                drawLine(color, Offset(size.width * 0.18f, size.height * 0.28f), Offset(size.width * 0.82f, size.height * 0.28f), strokeWidth = 1.8.dp.toPx())
                drawLine(color, Offset(size.width * 0.18f, size.height * 0.72f), Offset(size.width * 0.82f, size.height * 0.72f), strokeWidth = 1.8.dp.toPx())
                drawCircle(color, radius = size.minDimension * 0.10f, center = Offset(size.width * 0.38f, size.height * 0.28f))
                drawCircle(color, radius = size.minDimension * 0.10f, center = Offset(size.width * 0.64f, size.height * 0.72f))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun RingControllerPreview() {
    RingControllerApp()
}
