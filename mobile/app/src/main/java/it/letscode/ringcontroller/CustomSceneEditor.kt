package it.letscode.ringcontroller

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

private val StudioBackground = Color(0xFF08090C)
private val StudioSurface = Color(0xFF12151A)
private val StudioRaised = Color(0xFF1A1E24)
private val StudioLine = Color(0xFF292E36)
private val StudioOrange = Color(0xFFFF6A00)
private val StudioText = Color(0xFFF4F5F7)
private val StudioMuted = Color(0xFF8D949F)
private val StudioDanger = Color(0xFFFF6B78)

@Composable
internal fun CustomSceneEditorDialog(
    initialScene: CustomScene,
    favoriteColors: List<Color>,
    brightness: Float,
    simplifiedPreview: Boolean,
    isNew: Boolean,
    onCancel: () -> Unit,
    onSave: (CustomScene) -> Unit,
) {
    var draft by remember(initialScene.slot) { mutableStateOf(initialScene.normalized()) }
    var selectedMomentIndex by remember { mutableIntStateOf(0) }
    var selectedTarget by remember { mutableStateOf<Int?>(null) }
    var previewPlaying by remember { mutableStateOf(false) }
    var previewColors by remember { mutableStateOf(draft.moments.first().colors) }
    var colorDialogOpen by remember { mutableStateOf(false) }
    var colorDraft by remember { mutableStateOf(draft.moments.first().colors) }

    val selectedMoment = draft.moments[selectedMomentIndex]

    LaunchedEffect(previewPlaying, draft, selectedMomentIndex) {
        if (!previewPlaying) {
            previewColors = draft.moments[selectedMomentIndex].colors
            return@LaunchedEffect
        }
        var startedAtNanos = 0L
        while (previewPlaying) {
            withFrameNanos { frameTimeNanos ->
                if (startedAtNanos == 0L) startedAtNanos = frameTimeNanos
                val elapsedSeconds = (frameTimeNanos - startedAtNanos) / 1_000_000_000f
                previewColors = colorsForCustomScene(draft, elapsedSeconds)
            }
        }
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(StudioBackground)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel_action), color = StudioMuted, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(if (isNew) R.string.create_scene_title else R.string.edit_scene_title),
                        color = StudioText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        stringResource(R.string.scene_studio_subtitle),
                        color = StudioOrange,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                }
                TextButton(
                    onClick = { onSave(draft.normalized()) },
                    enabled = draft.name.isNotBlank(),
                ) {
                    Text(
                        stringResource(R.string.save_action),
                        color = if (draft.name.isNotBlank()) StudioOrange else StudioMuted,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    SceneStudioPreview(
                        colors = previewColors,
                        brightness = brightness,
                        simplifiedPreview = simplifiedPreview,
                        playing = previewPlaying,
                        durationMs = draft.durationMs,
                        onTogglePlayback = { previewPlaying = !previewPlaying },
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = draft.name,
                            onValueChange = { draft = draft.copy(name = it.take(30)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.scene_name_label)) },
                            placeholder = { Text(stringResource(R.string.scene_name_placeholder)) },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = draft.description,
                            onValueChange = { draft = draft.copy(description = it.take(100)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.scene_description_label)) },
                            placeholder = { Text(stringResource(R.string.scene_description_placeholder)) },
                            minLines = 2,
                            maxLines = 3,
                        )
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.scene_timeline_title),
                                color = StudioText,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                stringResource(R.string.scene_timeline_hint),
                                color = StudioMuted,
                                fontSize = 10.sp,
                            )
                        }
                        Text(
                            "${draft.moments.size}/$MAX_CUSTOM_MOMENTS",
                            color = StudioOrange,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                        )
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        itemsIndexed(draft.moments) { index, moment ->
                            SceneMomentChip(
                                index = index,
                                moment = moment,
                                selected = index == selectedMomentIndex,
                                onClick = {
                                    previewPlaying = false
                                    selectedMomentIndex = index
                                },
                            )
                        }
                        if (draft.moments.size < MAX_CUSTOM_MOMENTS) {
                            item {
                                AddMomentChip {
                                    previewPlaying = false
                                    val inserted = selectedMoment.copy()
                                    val moments = draft.moments.toMutableList().apply {
                                        add(selectedMomentIndex + 1, inserted)
                                    }
                                    draft = draft.copy(moments = moments)
                                    selectedMomentIndex += 1
                                }
                            }
                        }
                    }
                }
                item {
                    MomentEditorCard(
                        index = selectedMomentIndex,
                        momentCount = draft.moments.size,
                        moment = selectedMoment,
                        selectedTarget = selectedTarget,
                        onTargetSelected = { selectedTarget = it },
                        onOpenPalette = {
                            colorDraft = selectedMoment.colors
                            colorDialogOpen = true
                        },
                        onDurationChanged = { durationMs ->
                            draft = draft.updateMoment(selectedMomentIndex) { copy(durationMs = durationMs) }
                        },
                        onTransitionChanged = { transition ->
                            draft = draft.updateMoment(selectedMomentIndex) { copy(transition = transition) }
                        },
                        onMove = { direction ->
                            val destination = (selectedMomentIndex + direction).coerceIn(0, draft.moments.lastIndex)
                            if (destination != selectedMomentIndex) {
                                draft = draft.moveMoment(selectedMomentIndex, destination)
                                selectedMomentIndex = destination
                            }
                        },
                        onDuplicate = {
                            if (draft.moments.size < MAX_CUSTOM_MOMENTS) {
                                val moments = draft.moments.toMutableList().apply {
                                    add(selectedMomentIndex + 1, selectedMoment.copy())
                                }
                                draft = draft.copy(moments = moments)
                                selectedMomentIndex += 1
                            }
                        },
                        onDelete = {
                            if (draft.moments.size > MIN_CUSTOM_MOMENTS) {
                                val moments = draft.moments.toMutableList().apply { removeAt(selectedMomentIndex) }
                                draft = draft.copy(moments = moments)
                                selectedMomentIndex = selectedMomentIndex.coerceAtMost(moments.lastIndex)
                            }
                        },
                    )
                }
                item {
                    Button(
                        onClick = { onSave(draft.normalized()) },
                        enabled = draft.name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioOrange,
                            contentColor = Color.Black,
                            disabledContainerColor = StudioRaised,
                            disabledContentColor = StudioMuted,
                        ),
                    ) {
                        Text(stringResource(R.string.save_custom_scene), fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }

    if (colorDialogOpen) {
        SceneMomentColorDialog(
            colors = colorDraft,
            selectedTarget = selectedTarget,
            favorites = favoriteColors,
            brightness = brightness,
            simplifiedPreview = simplifiedPreview,
            onColorChanged = { color ->
                colorDraft = if (selectedTarget == null) {
                    List(4) { color }
                } else {
                    colorDraft.mapIndexed { index, current ->
                        if (index == selectedTarget) color else current
                    }
                }
            },
            onCancel = { colorDialogOpen = false },
            onSave = {
                draft = draft.updateMoment(selectedMomentIndex) { copy(colors = colorDraft) }
                colorDialogOpen = false
            },
        )
    }
}

@Composable
private fun SceneStudioPreview(
    colors: List<Color>,
    brightness: Float,
    simplifiedPreview: Boolean,
    playing: Boolean,
    durationMs: Int,
    onTogglePlayback: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF16191E), RoundedCornerShape(17.dp))
            .border(1.dp, StudioLine, RoundedCornerShape(17.dp))
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (simplifiedPreview) {
            SimplifiedHaloPreview(true, colors, null, brightness, null)
        } else {
            ChallengerFrontPreview(true, colors, null, brightness, null)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.loop_duration, formatSceneDuration(durationMs)),
                modifier = Modifier.weight(1f),
                color = StudioMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            Button(
                onClick = onTogglePlayback,
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (playing) StudioRaised else StudioOrange,
                    contentColor = if (playing) StudioText else Color.Black,
                ),
                contentPadding = PaddingValues(horizontal = 14.dp),
            ) {
                Text(
                    stringResource(if (playing) R.string.pause_preview else R.string.play_preview),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun SceneMomentChip(
    index: Int,
    moment: CustomSceneMoment,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(90.dp)
            .background(if (selected) Color(0xFF211B17) else StudioSurface, RoundedCornerShape(13.dp))
            .border(1.dp, if (selected) StudioOrange else StudioLine, RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            stringResource(R.string.moment_number, index + 1),
            color = if (selected) StudioOrange else StudioMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            moment.colors.forEach { color ->
                Box(Modifier.size(12.dp).border(2.dp, color, CircleShape))
            }
        }
        Text(formatSceneDuration(moment.durationMs), color = StudioText, fontSize = 10.sp)
    }
}

@Composable
private fun AddMomentChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(76.dp)
            .height(86.dp)
            .background(StudioSurface, RoundedCornerShape(13.dp))
            .border(1.dp, StudioLine, RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("+", color = StudioOrange, fontSize = 24.sp, fontWeight = FontWeight.Light)
            Text(stringResource(R.string.add_moment), color = StudioMuted, fontSize = 8.sp)
        }
    }
}

@Composable
private fun MomentEditorCard(
    index: Int,
    momentCount: Int,
    moment: CustomSceneMoment,
    selectedTarget: Int?,
    onTargetSelected: (Int?) -> Unit,
    onOpenPalette: () -> Unit,
    onDurationChanged: (Int) -> Unit,
    onTransitionChanged: (CustomTransition) -> Unit,
    onMove: (Int) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val sliderDescription = stringResource(R.string.moment_duration_description)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioSurface, RoundedCornerShape(16.dp))
            .border(1.dp, StudioLine, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.moment_title, index + 1),
                    color = StudioText,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    stringResource(R.string.moment_editor_hint),
                    color = StudioMuted,
                    fontSize = 10.sp,
                )
            }
            SmallStudioButton("←", enabled = index > 0) { onMove(-1) }
            Spacer(Modifier.width(6.dp))
            SmallStudioButton("→", enabled = index < momentCount - 1) { onMove(1) }
        }

        Text(stringResource(R.string.rings_target_title), color = StudioMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            SceneTargetChip(
                label = stringResource(R.string.all_short),
                selected = selectedTarget == null,
                modifier = Modifier.weight(1.4f),
            ) { onTargetSelected(null) }
            repeat(4) { ring ->
                SceneTargetChip(
                    label = "${ring + 1}",
                    selected = selectedTarget == ring,
                    color = moment.colors[ring],
                    modifier = Modifier.weight(1f),
                ) { onTargetSelected(ring) }
            }
        }

        Button(
            onClick = onOpenPalette,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(11.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StudioRaised, contentColor = StudioText),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val shownColors = selectedTarget?.let { listOf(moment.colors[it]) } ?: moment.colors
                    shownColors.forEach { color -> Box(Modifier.size(16.dp).background(color, CircleShape)) }
                }
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.choose_moment_color), fontWeight = FontWeight.Bold)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.moment_duration),
                modifier = Modifier.weight(1f),
                color = StudioText,
                fontWeight = FontWeight.Bold,
            )
            Text(formatSceneDuration(moment.durationMs), color = StudioOrange, fontWeight = FontWeight.Black)
        }
        Slider(
            value = moment.durationMs.toFloat(),
            onValueChange = { raw -> onDurationChanged((raw / 50f).roundToInt() * 50) },
            valueRange = MIN_MOMENT_DURATION_MS.toFloat()..MAX_MOMENT_DURATION_MS.toFloat(),
            modifier = Modifier.semantics { contentDescription = sliderDescription },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = StudioOrange,
                inactiveTrackColor = Color(0xFF353A42),
            ),
        )

        Text(stringResource(R.string.transition_title), color = StudioText, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TransitionChoice(
                transition = CustomTransition.Smooth,
                selected = moment.transition == CustomTransition.Smooth,
                modifier = Modifier.weight(1f),
                onClick = onTransitionChanged,
            )
            TransitionChoice(
                transition = CustomTransition.Jump,
                selected = moment.transition == CustomTransition.Jump,
                modifier = Modifier.weight(1f),
                onClick = onTransitionChanged,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onDuplicate,
                enabled = momentCount < MAX_CUSTOM_MOMENTS,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = StudioRaised),
            ) {
                Text(stringResource(R.string.duplicate_moment), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onDelete,
                enabled = momentCount > MIN_CUSTOM_MOMENTS,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioDanger.copy(alpha = 0.12f),
                    contentColor = StudioDanger,
                    disabledContainerColor = StudioRaised,
                    disabledContentColor = StudioMuted,
                ),
            ) {
                Text(stringResource(R.string.delete_moment), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SceneTargetChip(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    color: Color? = null,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .height(46.dp)
            .background(if (selected) StudioRaised else Color.Transparent, RoundedCornerShape(10.dp))
            .border(1.dp, if (selected) StudioOrange else StudioLine, RoundedCornerShape(10.dp))
            .semantics { this.selected = selected }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (color != null) Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(label, color = if (selected) StudioText else StudioMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun TransitionChoice(
    transition: CustomTransition,
    selected: Boolean,
    modifier: Modifier,
    onClick: (CustomTransition) -> Unit,
) {
    val smooth = transition == CustomTransition.Smooth
    Column(
        modifier = modifier
            .background(if (selected) Color(0xFF211B17) else StudioRaised, RoundedCornerShape(12.dp))
            .border(1.dp, if (selected) StudioOrange else StudioLine, RoundedCornerShape(12.dp))
            .clickable { onClick(transition) }
            .padding(11.dp),
    ) {
        Text(
            stringResource(if (smooth) R.string.transition_smooth else R.string.transition_jump),
            color = if (selected) StudioOrange else StudioText,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
        )
        Text(
            stringResource(if (smooth) R.string.transition_smooth_hint else R.string.transition_jump_hint),
            color = StudioMuted,
            fontSize = 9.sp,
        )
    }
}

@Composable
private fun SmallStudioButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(38.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = StudioRaised, contentColor = StudioText),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(label, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SceneMomentColorDialog(
    colors: List<Color>,
    selectedTarget: Int?,
    favorites: List<Color>,
    brightness: Float,
    simplifiedPreview: Boolean,
    onColorChanged: (Color) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val selectedColor = selectedTarget?.let(colors::get) ?: colors.first()
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(StudioBackground)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.choose_moment_color),
                        color = StudioText,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        selectedTarget?.let { stringResource(R.string.ring_label, it + 1).uppercase() }
                            ?: stringResource(R.string.all_rings).uppercase(),
                        color = StudioOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(stringResource(R.string.preview_changes), color = StudioMuted, fontSize = 9.sp)
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .background(Color(0xFF16191E), RoundedCornerShape(18.dp))
                    .border(1.dp, StudioLine, RoundedCornerShape(18.dp))
                    .padding(horizontal = 36.dp, vertical = 8.dp),
            ) {
                if (simplifiedPreview) {
                    SimplifiedHaloPreview(true, colors, selectedTarget, brightness, null)
                } else {
                    ChallengerFrontPreview(true, colors, selectedTarget, brightness, null)
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(
                color = Color(0xFF0D0F13),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(1.dp, StudioLine),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ColorControlPanel(
                        favorites = favorites,
                        selectedColor = selectedColor,
                        onColorSelected = onColorChanged,
                        onFavoriteAdded = {},
                        onFavoriteRemoved = {},
                        favoritesEditable = false,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioRaised,
                                contentColor = StudioText,
                            ),
                        ) {
                            Text(stringResource(R.string.cancel_action), fontWeight = FontWeight.Black)
                        }
                        Button(
                            onClick = onSave,
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioOrange,
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

private fun CustomScene.updateMoment(
    index: Int,
    transform: CustomSceneMoment.() -> CustomSceneMoment,
): CustomScene = copy(
    moments = moments.mapIndexed { momentIndex, moment ->
        if (momentIndex == index) moment.transform().normalized() else moment
    },
)

private fun CustomScene.moveMoment(from: Int, to: Int): CustomScene {
    val reordered = moments.toMutableList()
    val moment = reordered.removeAt(from)
    reordered.add(to, moment)
    return copy(moments = reordered)
}
