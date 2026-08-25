package it.letscode.ringcontroller

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

private val PickerSurface = Color(0xFF12151A)
private val PickerRaised = Color(0xFF1A1E24)
private val PickerLine = Color(0xFF292E36)
private val PickerMuted = Color(0xFF8D949F)
private val PickerText = Color(0xFFF4F5F7)
private val PickerOrange = Color(0xFFFF6A00)

private enum class ColorPanelTab {
    Picker,
    Favorites,
}

@Composable
internal fun ColorControlPanel(
    selectedColor: Color,
    favorites: List<Color>,
    onColorSelected: (Color) -> Unit,
    onFavoriteAdded: (Color) -> Unit,
    onFavoriteRemoved: (Int) -> Unit,
    favoritesEditable: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var currentTab by remember { mutableStateOf(ColorPanelTab.Picker) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PickerSurface, RoundedCornerShape(16.dp))
            .border(1.dp, PickerLine, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ColorPanelTabs(currentTab, onTabSelected = { currentTab = it })

        when (currentTab) {
            ColorPanelTab.Picker -> FullColorPicker(
                color = selectedColor,
                isFavorite = favorites.any { it.rgbEquals(selectedColor) },
                onColorChanged = onColorSelected,
                onFavoriteAdded = onFavoriteAdded,
                showFavoriteAction = favoritesEditable,
            )

            ColorPanelTab.Favorites -> FavoriteEditor(
                favorites = favorites,
                selectedColor = selectedColor,
                onColorSelected = onColorSelected,
                onFavoriteRemoved = onFavoriteRemoved,
                onOpenPicker = { currentTab = ColorPanelTab.Picker },
                favoritesEditable = favoritesEditable,
            )
        }
    }
}

@Composable
private fun ColorPanelTabs(
    selectedTab: ColorPanelTab,
    onTabSelected: (ColorPanelTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0F13), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ColorPanelTab.entries.forEach { tab ->
            val selected = selectedTab == tab
            val label = stringResource(
                if (tab == ColorPanelTab.Picker) R.string.color_picker_tab
                else R.string.favorites_tab,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .background(
                        if (selected) PickerRaised else Color.Transparent,
                        RoundedCornerShape(9.dp),
                    )
                    .border(
                        1.dp,
                        if (selected) Color(0xFF444A54) else Color.Transparent,
                        RoundedCornerShape(9.dp),
                    )
                    .semantics { this.selected = selected }
                    .clickable { onTabSelected(tab) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label.uppercase(),
                    color = if (selected) PickerText else PickerMuted,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                )
            }
        }
    }
}

@Composable
private fun FullColorPicker(
    color: Color,
    isFavorite: Boolean,
    onColorChanged: (Color) -> Unit,
    onFavoriteAdded: (Color) -> Unit,
    showFavoriteAction: Boolean,
) {
    val hsv = color.toHsv()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.full_color_spectrum),
            color = PickerMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.1.sp,
        )

        SaturationValueField(
            hue = hsv.hue,
            saturation = hsv.saturation,
            value = hsv.value,
            onChanged = { saturation, value ->
                onColorChanged(Color.hsv(hsv.hue, saturation, value))
            },
        )

        HueField(
            hue = hsv.hue,
            onHueChanged = { hue ->
                onColorChanged(Color.hsv(hue, hsv.saturation, hsv.value))
            },
        )

        if (showFavoriteAction) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color, RoundedCornerShape(12.dp))
                        .border(2.dp, Color.White.copy(alpha = 0.84f), RoundedCornerShape(12.dp)),
                )
                Button(
                    onClick = { onFavoriteAdded(color) },
                    enabled = !isFavorite,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PickerOrange,
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF30343B),
                        disabledContentColor = PickerMuted,
                    ),
                ) {
                    Text(
                        text = stringResource(if (isFavorite) R.string.favorite_added else R.string.add_favorite),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun SaturationValueField(
    hue: Float,
    saturation: Float,
    value: Float,
    onChanged: (saturation: Float, value: Float) -> Unit,
) {
    fun update(position: Offset, width: Float, height: Float) {
        onChanged(
            (position.x / width).coerceIn(0f, 1f),
            (1f - position.y / height).coerceIn(0f, 1f),
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2.15f)
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
            .pointerInput(hue) {
                detectTapGestures { update(it, size.width.toFloat(), size.height.toFloat()) }
            }
            .pointerInput(hue) {
                detectDragGestures(
                    onDragStart = { update(it, size.width.toFloat(), size.height.toFloat()) },
                    onDrag = { change, _ ->
                        update(change.position, size.width.toFloat(), size.height.toFloat())
                        change.consume()
                    },
                )
            },
    ) {
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
        )
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
        )
        val pointer = Offset(size.width * saturation, size.height * (1f - value))
        drawCircle(Color.Black.copy(alpha = 0.55f), 8.dp.toPx(), pointer)
        drawCircle(Color.White, 7.dp.toPx(), pointer, style = Stroke(2.dp.toPx()))
    }
}

@Composable
private fun HueField(
    hue: Float,
    onHueChanged: (Float) -> Unit,
) {
    fun update(position: Offset, width: Float) {
        onHueChanged((position.x / width).coerceIn(0f, 1f) * 360f)
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { update(it, size.width.toFloat()) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { update(it, size.width.toFloat()) },
                    onDrag = { change, _ ->
                        update(change.position, size.width.toFloat())
                        change.consume()
                    },
                )
            },
    ) {
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(
                    Color.Red,
                    Color.Yellow,
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color.Magenta,
                    Color.Red,
                ),
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
        )
        val x = size.width * (hue / 360f)
        drawCircle(Color.Black.copy(alpha = 0.55f), 9.dp.toPx(), Offset(x, size.height / 2))
        drawCircle(Color.White, 8.dp.toPx(), Offset(x, size.height / 2), style = Stroke(2.dp.toPx()))
    }
}

@Composable
private fun FavoriteEditor(
    favorites: List<Color>,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    onFavoriteRemoved: (Int) -> Unit,
    onOpenPicker: () -> Unit,
    favoritesEditable: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.your_favorites), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.favorite_editor_hint), color = PickerMuted, fontSize = 10.sp)
            }
            Text(
                text = "${favorites.size}/12",
                color = PickerOrange,
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
            )
        }

        if (favorites.isEmpty()) {
            Button(
                onClick = onOpenPicker,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PickerRaised),
            ) {
                Text(stringResource(R.string.choose_first_favorite))
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(favorites) { index, favorite ->
                    val hex = favorite.toHex()
                    val selected = favorite.rgbEquals(selectedColor)
                    val applyDescription = stringResource(R.string.apply_color_description, hex)
                    Column(
                        modifier = Modifier.width(62.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(favorite.copy(alpha = 0.15f), CircleShape)
                                .border(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) Color.White else Color(0xFF3B414A),
                                    CircleShape,
                                )
                                .semantics {
                                    contentDescription = applyDescription
                                    this.selected = selected
                                }
                                .clickable { onColorSelected(favorite) }
                                .padding(8.dp)
                                .background(favorite, CircleShape),
                        )
                        Spacer(Modifier.height(4.dp))
                        if (favoritesEditable && favorites.size > 1) {
                            Text(
                                text = stringResource(R.string.remove_favorite),
                                modifier = Modifier.clickable { onFavoriteRemoved(index) }.padding(3.dp),
                                color = Color(0xFFFF6B78),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class HsvColor(
    val hue: Float,
    val saturation: Float,
    val value: Float,
)

private fun Color.toHsv(): HsvColor {
    val red = red.coerceIn(0f, 1f)
    val green = green.coerceIn(0f, 1f)
    val blue = blue.coerceIn(0f, 1f)
    val maximum = max(red, max(green, blue))
    val minimum = min(red, min(green, blue))
    val delta = maximum - minimum
    val hue = when {
        delta == 0f -> 0f
        maximum == red -> 60f * (((green - blue) / delta) % 6f)
        maximum == green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }
    return HsvColor(
        hue = hue,
        saturation = if (maximum == 0f) 0f else delta / maximum,
        value = maximum,
    )
}

internal fun Color.toHex(): String = "#%02X%02X%02X".format(
    (red * 255f).toInt().coerceIn(0, 255),
    (green * 255f).toInt().coerceIn(0, 255),
    (blue * 255f).toInt().coerceIn(0, 255),
)

internal fun String.parseHexColor(): Color? {
    val value = removePrefix("#")
    if (value.length != 6 || value.any { it !in "0123456789ABCDEFabcdef" }) return null
    return Color(value.toLong(16) or 0xFF000000)
}

private fun Color.rgbEquals(other: Color): Boolean = toHex() == other.toHex()
