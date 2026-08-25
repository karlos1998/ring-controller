package it.letscode.ringcontroller

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin

private val AppBackground = Color(0xFF080A0E)
private val AppSurface = Color(0xFF13161C)
private val AppSurfaceRaised = Color(0xFF1A1E26)
private val AppOrange = Color(0xFFFF6A00)
private val AppText = Color(0xFFF6F7F9)
private val AppMuted = Color(0xFF9299A6)

private val AppColors = darkColorScheme(
    primary = AppOrange,
    secondary = Color(0xFFFFB26F),
    background = AppBackground,
    surface = AppSurface,
    onPrimary = Color.Black,
    onBackground = AppText,
    onSurface = AppText,
)

private data class FavoriteColor(
    val name: String,
    val color: Color,
)

private enum class ScenePreset(
    val title: String,
    val subtitle: String,
    val badge: String,
) {
    AmberChase("Amber chase", "One halo at a time", "CHASE"),
    DemonPulse("Demon pulse", "Deep red breathing", "PULSE"),
    SpectrumWave("Spectrum wave", "Color rolls across all four", "WAVE"),
}

private val favoriteColors = listOf(
    FavoriteColor("Ice", Color(0xFFF2F6FF)),
    FavoriteColor("Amber", Color(0xFFFF6A00)),
    FavoriteColor("Red", Color(0xFFFF304E)),
    FavoriteColor("Violet", Color(0xFFA855F7)),
    FavoriteColor("Blue", Color(0xFF3388FF)),
    FavoriteColor("Green", Color(0xFF43E07B)),
)

@Composable
fun RingControllerApp() {
    var ringsEnabled by remember { mutableStateOf(true) }
    var selectedRing by remember { mutableStateOf<Int?>(null) }
    var ringColors by remember { mutableStateOf(List(4) { favoriteColors[1].color }) }
    var brightness by remember { mutableFloatStateOf(0.88f) }
    var activeScene by remember { mutableStateOf<ScenePreset?>(null) }
    var sceneStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(activeScene, ringsEnabled) {
        sceneStep = 0
        while (activeScene != null && ringsEnabled) {
            val step = sceneStep
            ringColors = when (activeScene) {
                ScenePreset.AmberChase -> List(4) { index ->
                    if (index == step % 4) Color(0xFFFF7900) else Color(0xFF351503)
                }

                ScenePreset.DemonPulse -> {
                    val wave = ((sin(step / 2.0) + 1.0) / 2.0).toFloat()
                    val red = 0.28f + wave * 0.72f
                    List(4) { Color(red, 0.025f, 0.045f) }
                }

                ScenePreset.SpectrumWave -> List(4) { index ->
                    Color.hsv((step * 16f + index * 52f) % 360f, 0.82f, 1f)
                }

                null -> ringColors
            }
            sceneStep++
            delay(260)
        }
    }

    fun applyFavorite(color: Color) {
        activeScene = null
        ringColors = if (selectedRing == null) {
            List(4) { color }
        } else {
            ringColors.mapIndexed { index, current ->
                if (index == selectedRing) color else current
            }
        }
    }

    MaterialTheme(colorScheme = AppColors) {
        Scaffold(
            containerColor = AppBackground,
            bottomBar = { DashboardNavigation() },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { AppHeader() }

                item {
                    HaloDashboard(
                        enabled = ringsEnabled,
                        colors = ringColors,
                        selectedRing = selectedRing,
                        activeScene = activeScene,
                        onRingSelected = { selectedRing = it },
                        onPowerClick = { ringsEnabled = !ringsEnabled },
                    )
                }

                item {
                    RingTargetSelector(
                        selectedRing = selectedRing,
                        onSelected = { selectedRing = it },
                    )
                }

                item {
                    FavoritePalette(
                        colors = favoriteColors,
                        selectedColor = selectedRing?.let(ringColors::get)
                            ?: ringColors.firstOrNull().takeIf { ringColors.distinct().size == 1 },
                        onColorSelected = ::applyFavorite,
                    )
                }

                item {
                    BrightnessControl(
                        brightness = brightness,
                        onBrightnessChanged = { brightness = it },
                    )
                }

                item {
                    SectionHeader(
                        title = "Show modes",
                        trailing = "PARKED ONLY",
                    )
                }

                item {
                    SceneCarousel(
                        activeScene = activeScene,
                        onSceneSelected = { scene ->
                            activeScene = scene
                            ringsEnabled = true
                        },
                    )
                }

                item { PhysicalButtonCard() }
                item { VehicleAutomationCard() }
            }
        }
    }
}

@Composable
private fun AppHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "D4WID",
                fontSize = 30.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.4.sp,
            )
            Text(
                text = "RING CONTROLLER",
                color = AppMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.2.sp,
            )
        }
        Surface(
            color = Color(0xFF19231F),
            shape = RoundedCornerShape(50),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2D493C)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(Color(0xFF51E395), CircleShape),
                )
                Text("DEMO", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun HaloDashboard(
    enabled: Boolean,
    colors: List<Color>,
    selectedRing: Int?,
    activeScene: ScenePreset?,
    onRingSelected: (Int) -> Unit,
    onPowerClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF20242C), Color(0xFF101218)),
                    ),
                )
                .padding(18.dp),
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AppOrange.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.2f),
                        radius = size.width * 0.62f,
                    ),
                    radius = size.width * 0.62f,
                    center = Offset(size.width * 0.5f, size.height * 0.2f),
                )
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeScene?.title?.uppercase() ?: "LIVE HALOS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp,
                            color = if (activeScene != null) AppOrange else AppMuted,
                        )
                        Text(
                            text = if (enabled) "Tap a halo to tune it" else "Lighting is off",
                            fontSize = 13.sp,
                            color = AppText,
                        )
                    }
                    Button(
                        onClick = onPowerClick,
                        modifier = Modifier.height(38.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (enabled) AppOrange else Color(0xFF30343D),
                            contentColor = if (enabled) Color.Black else AppText,
                        ),
                        contentPadding = PaddingValues(horizontal = 15.dp),
                    ) {
                        Text(
                            text = if (enabled) "ON" else "OFF",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    RingHalo(0, colors[0], enabled, selectedRing == 0, onRingSelected)
                    Spacer(Modifier.width(7.dp))
                    RingHalo(1, colors[1], enabled, selectedRing == 1, onRingSelected)
                    Spacer(Modifier.width(18.dp))
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(4.dp)
                            .background(Color(0xFF4A4E57), RoundedCornerShape(50)),
                    )
                    Spacer(Modifier.width(18.dp))
                    RingHalo(2, colors[2], enabled, selectedRing == 2, onRingSelected)
                    Spacer(Modifier.width(7.dp))
                    RingHalo(3, colors[3], enabled, selectedRing == 3, onRingSelected)
                }

                Spacer(Modifier.height(13.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "LEFT                     RIGHT",
                    color = Color(0xFF686F7C),
                    textAlign = TextAlign.Center,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
            }
        }
    }
}

@Composable
private fun RingHalo(
    index: Int,
    color: Color,
    enabled: Boolean,
    selected: Boolean,
    onSelected: (Int) -> Unit,
) {
    val activeColor = if (enabled) color else Color(0xFF343842)
    Box(
        modifier = Modifier
            .size(57.dp)
            .clip(CircleShape)
            .semantics {
                contentDescription = "Ring ${index + 1}"
                this.selected = selected
            }
            .clickable { onSelected(index) },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = activeColor.copy(alpha = if (enabled) 0.13f else 0.06f),
                radius = size.minDimension * 0.48f,
            )
            drawCircle(
                color = activeColor.copy(alpha = if (enabled) 0.24f else 0.08f),
                radius = size.minDimension * 0.39f,
                style = Stroke(width = 11.dp.toPx()),
            )
            drawCircle(
                color = activeColor,
                radius = size.minDimension * 0.36f,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )
            drawCircle(
                color = Color(0xFF07080B),
                radius = size.minDimension * 0.25f,
            )
            if (selected) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = size.minDimension * 0.48f,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }
        Text(
            text = "${index + 1}",
            color = Color(0xFF737A87),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun RingTargetSelector(
    selectedRing: Int?,
    onSelected: (Int?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        SectionHeader("Editing", if (selectedRing == null) "ALL RINGS" else "RING ${selectedRing + 1}")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppSurface, RoundedCornerShape(18.dp))
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            TargetChip(
                label = "ALL",
                selected = selectedRing == null,
                modifier = Modifier.weight(1.4f),
                onClick = { onSelected(null) },
            )
            repeat(4) { index ->
                TargetChip(
                    label = "${index + 1}",
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
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .background(
                if (selected) AppSurfaceRaised else Color.Transparent,
                RoundedCornerShape(13.dp),
            )
            .border(
                width = 1.dp,
                color = if (selected) Color(0xFF414753) else Color.Transparent,
                shape = RoundedCornerShape(13.dp),
            )
            .semantics {
                contentDescription = if (label == "ALL") "Edit all rings" else "Edit ring $label"
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
private fun FavoritePalette(
    colors: List<FavoriteColor>,
    selectedColor: Color?,
    onColorSelected: (Color) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Favorites", "TAP TO APPLY")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            items(colors) { favorite ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(
                                elevation = if (selectedColor == favorite.color) 10.dp else 0.dp,
                                shape = CircleShape,
                                ambientColor = favorite.color,
                                spotColor = favorite.color,
                            )
                            .background(favorite.color.copy(alpha = 0.18f), CircleShape)
                            .border(
                                width = if (selectedColor == favorite.color) 2.dp else 1.dp,
                                color = if (selectedColor == favorite.color) Color.White else Color(0xFF3B414C),
                                shape = CircleShape,
                            )
                            .semantics {
                                contentDescription = "Apply ${favorite.name}"
                                this.selected = selectedColor == favorite.color
                            }
                            .clickable { onColorSelected(favorite.color) }
                            .padding(8.dp)
                            .background(favorite.color, CircleShape),
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(favorite.name, color = AppMuted, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun BrightnessControl(
    brightness: Float,
    onBrightnessChanged: (Float) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Brightness", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    text = "${(brightness * 100).toInt()}%",
                    color = AppOrange,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                )
            }
            Slider(
                value = brightness,
                onValueChange = onBrightnessChanged,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = AppOrange,
                    inactiveTrackColor = Color(0xFF353A45),
                ),
            )
        }
    }
}

@Composable
private fun SceneCarousel(
    activeScene: ScenePreset?,
    onSceneSelected: (ScenePreset) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        items(ScenePreset.entries) { scene ->
            SceneCard(
                scene = scene,
                selected = scene == activeScene,
                onClick = { onSceneSelected(scene) },
            )
        }
    }
}

@Composable
private fun SceneCard(
    scene: ScenePreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val previewColors = when (scene) {
        ScenePreset.AmberChase -> listOf(AppOrange, Color(0xFF512203), Color(0xFF512203), Color(0xFF512203))
        ScenePreset.DemonPulse -> List(4) { Color(0xFFFF2545) }
        ScenePreset.SpectrumWave -> listOf(
            Color(0xFFFF3D67),
            Color(0xFFFFB000),
            Color(0xFF4EE08A),
            Color(0xFF478CFF),
        )
    }
    Card(
        modifier = Modifier
            .width(188.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF27211C) else AppSurface,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) AppOrange.copy(alpha = 0.75f) else Color(0xFF292E37),
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = scene.badge,
                color = if (selected) AppOrange else AppMuted,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                previewColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .border(4.dp, color, CircleShape),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(scene.title, fontWeight = FontWeight.Bold)
            Text(scene.subtitle, color = AppMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PhysicalButtonCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFF242A33), CircleShape)
                        .border(1.dp, Color(0xFF414A58), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("●", color = AppOrange, fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Cabin button", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Works even without the phone", color = AppMuted, fontSize = 11.sp)
                }
                Text(
                    "READY",
                    color = Color(0xFF51E395),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
            }
            Spacer(Modifier.height(14.dp))
            ButtonRule("TAP", "Turn on / next favorite")
            ButtonRule("HOLD", "Turn all rings off")
            ButtonRule("RGB LED", "Mirror Ring 1 / shared color")
        }
    }
}

@Composable
private fun ButtonRule(action: String, result: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = action,
            modifier = Modifier.width(62.dp),
            color = AppOrange,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
        )
        Text(result, color = AppText, fontSize = 12.sp)
    }
}

@Composable
private fun VehicleAutomationCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSurface, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(Color.White, CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Vehicle light signal", fontWeight = FontWeight.Bold)
            Text("When +12 V is active: bright white", color = AppMuted, fontSize = 11.sp)
        }
        Text("AUTO", color = AppOrange, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(
            trailing,
            color = AppMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp,
        )
    }
}

@Composable
private fun DashboardNavigation() {
    Surface(
        color = Color(0xF2111419),
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            NavigationItem("●", "Drive", selected = true)
            NavigationItem("◈", "Scenes", selected = false)
            NavigationItem("⚙", "Setup", selected = false)
        }
    }
}

@Composable
private fun NavigationItem(symbol: String, label: String, selected: Boolean) {
    Column(
        modifier = Modifier.width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            symbol,
            color = if (selected) AppOrange else AppMuted,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            label,
            color = if (selected) AppText else AppMuted,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun RingControllerPreview() {
    RingControllerApp()
}
