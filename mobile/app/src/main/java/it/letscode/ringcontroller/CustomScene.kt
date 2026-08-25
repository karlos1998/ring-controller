package it.letscode.ringcontroller

import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.floor

internal const val MAX_CUSTOM_SCENES = 8
internal const val MAX_CUSTOM_MOMENTS = 12
internal const val MIN_CUSTOM_MOMENTS = 2
internal const val MIN_MOMENT_DURATION_MS = 150
internal const val MAX_MOMENT_DURATION_MS = 5_000

internal enum class CustomTransition(val protocolValue: Int) {
    Jump(0),
    Smooth(1),
    ;

    companion object {
        fun fromProtocolValue(value: Int): CustomTransition =
            entries.firstOrNull { it.protocolValue == value } ?: Smooth
    }
}

internal data class CustomSceneMoment(
    val colors: List<Color>,
    val durationMs: Int,
    val transition: CustomTransition,
) {
    fun normalized(): CustomSceneMoment = copy(
        colors = colors.take(4).let { existing ->
            if (existing.size == 4) existing else existing + List(4 - existing.size) { Color.Black }
        },
        durationMs = durationMs.coerceIn(MIN_MOMENT_DURATION_MS, MAX_MOMENT_DURATION_MS),
    )
}

internal data class CustomScene(
    val slot: Int,
    val name: String,
    val description: String,
    val moments: List<CustomSceneMoment>,
) {
    val durationMs: Int
        get() = moments.sumOf(CustomSceneMoment::durationMs)

    fun normalized(): CustomScene = copy(
        slot = slot.coerceIn(0, MAX_CUSTOM_SCENES - 1),
        name = name.trim().take(30),
        description = description.trim().take(100),
        moments = moments
            .take(MAX_CUSTOM_MOMENTS)
            .map(CustomSceneMoment::normalized)
            .let { normalized ->
                when {
                    normalized.size >= MIN_CUSTOM_MOMENTS -> normalized
                    normalized.isEmpty() -> defaultMoments()
                    else -> normalized + normalized.first().copy()
                }
            },
    )

    companion object {
        fun create(slot: Int, baseColors: List<Color>): CustomScene {
            val colors = baseColors.take(4).let { existing ->
                if (existing.size == 4) existing else List(4) { Color(0xFF00E5E5) }
            }
            return CustomScene(
                slot = slot,
                name = "",
                description = "",
                moments = listOf(
                    CustomSceneMoment(colors, 700, CustomTransition.Smooth),
                    CustomSceneMoment(List(4) { Color.Black }, 450, CustomTransition.Jump),
                ),
            )
        }

        private fun defaultMoments(): List<CustomSceneMoment> = listOf(
            CustomSceneMoment(List(4) { Color(0xFF00E5E5) }, 700, CustomTransition.Smooth),
            CustomSceneMoment(List(4) { Color.Black }, 450, CustomTransition.Jump),
        )
    }
}

internal fun colorsForCustomScene(scene: CustomScene, elapsedSeconds: Float): List<Color> {
    val moments = scene.normalized().moments
    val totalDuration = moments.sumOf(CustomSceneMoment::durationMs).coerceAtLeast(1)
    var elapsedMs = ((elapsedSeconds * 1_000f) % totalDuration + totalDuration) % totalDuration
    var currentIndex = 0
    while (currentIndex < moments.lastIndex && elapsedMs >= moments[currentIndex].durationMs) {
        elapsedMs -= moments[currentIndex].durationMs
        currentIndex += 1
    }
    val current = moments[currentIndex]
    if (current.transition == CustomTransition.Jump) return current.colors

    val next = moments[(currentIndex + 1) % moments.size]
    val progress = smoothCustomProgress(elapsedMs / current.durationMs.toFloat())
    return current.colors.zip(next.colors) { from, to -> blendCustomColors(from, to, progress) }
}

internal fun customScenePreviewColors(scene: CustomScene): List<Color> {
    val seconds = (scene.durationMs * 0.28f) / 1_000f
    return colorsForCustomScene(scene, seconds)
}

private fun smoothCustomProgress(progress: Float): Float {
    val value = progress.coerceIn(0f, 1f)
    return value * value * (3f - 2f * value)
}

private fun blendCustomColors(from: Color, to: Color, progress: Float): Color {
    val amount = progress.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * amount,
        green = from.green + (to.green - from.green) * amount,
        blue = from.blue + (to.blue - from.blue) * amount,
    )
}

internal object CustomSceneJson {
    fun encode(scenes: List<CustomScene>): String {
        val root = JSONArray()
        scenes
            .map(CustomScene::normalized)
            .sortedBy(CustomScene::slot)
            .take(MAX_CUSTOM_SCENES)
            .forEach { scene ->
                root.put(JSONObject().apply {
                    put("slot", scene.slot)
                    put("name", scene.name)
                    put("description", scene.description)
                    put("moments", JSONArray().apply {
                        scene.moments.forEach { moment ->
                            put(JSONObject().apply {
                                put("durationMs", moment.durationMs)
                                put("transition", moment.transition.protocolValue)
                                put("colors", JSONArray(moment.colors.map(Color::toHex)))
                            })
                        }
                    })
                })
            }
        return root.toString()
    }

    fun decode(raw: String?): List<CustomScene> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val root = JSONArray(raw)
            buildList {
                for (sceneIndex in 0 until root.length()) {
                    val json = root.getJSONObject(sceneIndex)
                    val momentsJson = json.getJSONArray("moments")
                    val moments = buildList {
                        for (momentIndex in 0 until momentsJson.length()) {
                            val moment = momentsJson.getJSONObject(momentIndex)
                            val colorsJson = moment.getJSONArray("colors")
                            val colors = buildList {
                                for (colorIndex in 0 until colorsJson.length()) {
                                    colorsJson.optString(colorIndex).parseHexColor()?.let(::add)
                                }
                            }
                            add(
                                CustomSceneMoment(
                                    colors = colors,
                                    durationMs = moment.optInt("durationMs", 700),
                                    transition = CustomTransition.fromProtocolValue(
                                        moment.optInt("transition", CustomTransition.Smooth.protocolValue),
                                    ),
                                ),
                            )
                        }
                    }
                    add(
                        CustomScene(
                            slot = json.getInt("slot"),
                            name = json.optString("name"),
                            description = json.optString("description"),
                            moments = moments,
                        ).normalized(),
                    )
                }
            }
                .filter { it.slot in 0 until MAX_CUSTOM_SCENES && it.name.isNotBlank() }
                .distinctBy(CustomScene::slot)
                .take(MAX_CUSTOM_SCENES)
        }.getOrDefault(emptyList())
    }
}

internal fun nextCustomSceneSlot(scenes: List<CustomScene>): Int? =
    (0 until MAX_CUSTOM_SCENES).firstOrNull { slot -> scenes.none { it.slot == slot } }

internal fun formatSceneDuration(durationMs: Int): String {
    val seconds = durationMs / 1_000f
    return if (floor(seconds) == seconds) "${seconds.toInt()} s" else "%.1f s".format(seconds)
}
