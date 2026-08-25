package it.letscode.ringcontroller

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

internal enum class SceneCategory(@param:StringRes val titleRes: Int) {
    Signals(R.string.scene_category_signals),
    Everyday(R.string.scene_category_everyday),
    Show(R.string.scene_category_show),
}

internal enum class ScenePreset(
    val id: Int,
    val category: SceneCategory,
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int,
    @param:StringRes val badgeRes: Int,
    val previewSeconds: Float,
) {
    AmberChase(
        0, SceneCategory.Signals,
        R.string.scene_amber_chase, R.string.scene_amber_chase_subtitle, R.string.scene_badge_chase, 0f,
    ),
    DemonPulse(
        1, SceneCategory.Show,
        R.string.scene_demon_pulse, R.string.scene_demon_pulse_subtitle, R.string.scene_badge_pulse, 1.4f,
    ),
    SpectrumWave(
        2, SceneCategory.Show,
        R.string.scene_spectrum_wave, R.string.scene_spectrum_wave_subtitle, R.string.scene_badge_wave, 0.6f,
    ),
    HazardFlash(
        3, SceneCategory.Signals,
        R.string.scene_hazard_flash, R.string.scene_hazard_flash_subtitle, R.string.scene_badge_hazard, 0.1f,
    ),
    HazardDouble(
        4, SceneCategory.Signals,
        R.string.scene_hazard_double, R.string.scene_hazard_double_subtitle, R.string.scene_badge_double, 0.34f,
    ),
    InnerOuterAmber(
        5, SceneCategory.Signals,
        R.string.scene_inner_outer, R.string.scene_inner_outer_subtitle, R.string.scene_badge_alternate, 0f,
    ),
    LeftAmber(
        6, SceneCategory.Signals,
        R.string.scene_left_amber, R.string.scene_left_amber_subtitle, R.string.scene_badge_left, 0.1f,
    ),
    RightAmber(
        7, SceneCategory.Signals,
        R.string.scene_right_amber, R.string.scene_right_amber_subtitle, R.string.scene_badge_right, 0.1f,
    ),
    InwardSweep(
        8, SceneCategory.Signals,
        R.string.scene_inward_sweep, R.string.scene_inward_sweep_subtitle, R.string.scene_badge_inward, 0f,
    ),
    OutwardSweep(
        9, SceneCategory.Signals,
        R.string.scene_outward_sweep, R.string.scene_outward_sweep_subtitle, R.string.scene_badge_outward, 0f,
    ),
    BrightWhite(
        10, SceneCategory.Everyday,
        R.string.scene_bright_white, R.string.scene_bright_white_subtitle, R.string.scene_badge_white, 0f,
    ),
    IceWhite(
        11, SceneCategory.Everyday,
        R.string.scene_ice_white, R.string.scene_ice_white_subtitle, R.string.scene_badge_ice, 0f,
    ),
    ChallengerAmber(
        12, SceneCategory.Everyday,
        R.string.scene_challenger_amber, R.string.scene_challenger_amber_subtitle, R.string.scene_badge_amber, 0f,
    ),
    CourtesyFade(
        13, SceneCategory.Everyday,
        R.string.scene_courtesy_fade, R.string.scene_courtesy_fade_subtitle, R.string.scene_badge_fade, 2.4f,
    ),
    AmberBreathing(
        14, SceneCategory.Everyday,
        R.string.scene_amber_breathing, R.string.scene_amber_breathing_subtitle, R.string.scene_badge_breathe, 1.7f,
    ),
    RedlineChase(
        15, SceneCategory.Show,
        R.string.scene_redline_chase, R.string.scene_redline_chase_subtitle, R.string.scene_badge_redline, 0.55f,
    ),
    CyanScanner(
        16, SceneCategory.Show,
        R.string.scene_cyan_scanner, R.string.scene_cyan_scanner_subtitle, R.string.scene_badge_scan, 0.55f,
    ),
    SplitHorizon(
        17, SceneCategory.Show,
        R.string.scene_split_horizon, R.string.scene_split_horizon_subtitle, R.string.scene_badge_split, 0.8f,
    ),
    MirrorRainbow(
        18, SceneCategory.Show,
        R.string.scene_mirror_rainbow, R.string.scene_mirror_rainbow_subtitle, R.string.scene_badge_mirror, 1.2f,
    ),
    FavoriteCarousel(
        19, SceneCategory.Show,
        R.string.scene_favorite_carousel, R.string.scene_favorite_carousel_subtitle, R.string.scene_badge_favorites, 1.3f,
    );

    companion object {
        fun fromId(id: Int): ScenePreset? = entries.firstOrNull { it.id == id }
    }
}

private val SceneOff = Color.Black
private val SceneAmber = Color(0xFFFF6A00)
private val SceneDemonRed = Color(0xFFFF0812)
private val SceneCyan = Color(0xFF00E5E5)
private val SceneViolet = Color(0xFFA855F7)

internal fun colorsForScene(
    scene: ScenePreset,
    elapsedSeconds: Float,
    favorites: List<Color>,
): List<Color> = when (scene) {
    ScenePreset.AmberChase -> runnerColors(
        position = (elapsedSeconds * 1.65f) % 4f,
        color = SceneAmber,
        minimum = 0.08f,
        wraps = true,
    )

    ScenePreset.DemonPulse -> List(4) {
        SceneDemonRed.withIntensity(breathingLevel(elapsedSeconds, 2.8f, 0.25f))
    }

    ScenePreset.SpectrumWave -> List(4) { index ->
        Color.hsv((elapsedSeconds * 62f + index * 52f) % 360f, 0.82f, 1f)
    }

    ScenePreset.HazardFlash -> List(4) {
        if (elapsedSeconds % 1f < 0.46f) SceneAmber else SceneOff
    }

    ScenePreset.HazardDouble -> {
        val phase = elapsedSeconds % 1.5f
        val illuminated = phase < 0.16f || phase in 0.31f..<0.47f
        List(4) { if (illuminated) SceneAmber else SceneOff }
    }

    ScenePreset.InnerOuterAmber -> when (elapsedSeconds % 1f) {
        in 0f..<0.42f -> listOf(SceneAmber, SceneOff, SceneOff, SceneAmber)
        in 0.50f..<0.92f -> listOf(SceneOff, SceneAmber, SceneAmber, SceneOff)
        else -> List(4) { SceneOff }
    }

    ScenePreset.LeftAmber -> if (elapsedSeconds % 1f < 0.5f) {
        listOf(SceneAmber, SceneAmber, SceneOff, SceneOff)
    } else {
        List(4) { SceneOff }
    }

    ScenePreset.RightAmber -> if (elapsedSeconds % 1f < 0.5f) {
        listOf(SceneOff, SceneOff, SceneAmber, SceneAmber)
    } else {
        List(4) { SceneOff }
    }

    ScenePreset.InwardSweep -> pairedSweep(elapsedSeconds, inward = true)
    ScenePreset.OutwardSweep -> pairedSweep(elapsedSeconds, inward = false)
    ScenePreset.BrightWhite -> List(4) { Color.White }
    ScenePreset.IceWhite -> List(4) { Color(0xFFCDE8FF) }
    ScenePreset.ChallengerAmber -> List(4) { SceneAmber }

    ScenePreset.CourtesyFade -> List(4) {
        Color(0xFFF6FAFF).withIntensity(breathingLevel(elapsedSeconds, 4.8f, 0.12f))
    }

    ScenePreset.AmberBreathing -> List(4) {
        SceneAmber.withIntensity(breathingLevel(elapsedSeconds, 3.4f, 0.10f))
    }

    ScenePreset.RedlineChase -> runnerColors(
        position = pingPongPosition(elapsedSeconds, 1.85f),
        color = SceneDemonRed,
        minimum = 0.025f,
        wraps = false,
    )

    ScenePreset.CyanScanner -> runnerColors(
        position = pingPongPosition(elapsedSeconds, 2.35f),
        color = SceneCyan,
        minimum = 0.035f,
        wraps = false,
    )

    ScenePreset.SplitHorizon -> {
        val left = breathingLevel(elapsedSeconds, 3.2f, 0.32f)
        val right = breathingLevel(elapsedSeconds + 1.6f, 3.2f, 0.32f)
        listOf(
            SceneCyan.withIntensity(left), SceneCyan.withIntensity(left),
            SceneViolet.withIntensity(right), SceneViolet.withIntensity(right),
        )
    }

    ScenePreset.MirrorRainbow -> {
        val hue = (elapsedSeconds * 42f) % 360f
        val outer = Color.hsv(hue, 0.82f, 1f)
        val inner = Color.hsv((hue + 105f) % 360f, 0.82f, 1f)
        listOf(outer, inner, inner, outer)
    }

    ScenePreset.FavoriteCarousel -> {
        val palette = favorites.ifEmpty { listOf(Color.White) }
        val position = elapsedSeconds / 2.6f
        val current = floor(position).toInt().mod(palette.size)
        val next = (current + 1) % palette.size
        val blend = smoothProgress(position - floor(position))
        List(4) { blendColors(palette[current], palette[next], blend) }
    }
}

private fun pairedSweep(elapsedSeconds: Float, inward: Boolean): List<Color> {
    val step = ((elapsedSeconds % 1.45f) / 0.24f).toInt()
    val outer = listOf(SceneAmber, SceneOff, SceneOff, SceneAmber)
    val inner = listOf(SceneOff, SceneAmber, SceneAmber, SceneOff)
    return when (step) {
        0, 2 -> if (inward) outer else inner
        1, 3 -> if (inward) inner else outer
        else -> List(4) { SceneOff }
    }
}

private fun runnerColors(position: Float, color: Color, minimum: Float, wraps: Boolean): List<Color> =
    List(4) { index ->
        var distance = abs(index - position)
        if (wraps) distance = min(distance, 4f - distance)
        color.withIntensity((1f - distance * 0.78f).coerceAtLeast(minimum))
    }

private fun pingPongPosition(seconds: Float, ringsPerSecond: Float): Float {
    val phase = (seconds * ringsPerSecond) % 6f
    return if (phase <= 3f) phase else 6f - phase
}

private fun breathingLevel(seconds: Float, periodSeconds: Float, minimum: Float): Float {
    val wave = (sin(seconds * (2f * PI.toFloat() / periodSeconds) - PI.toFloat() / 2f) + 1f) / 2f
    return minimum + wave * (1f - minimum)
}

private fun smoothProgress(progress: Float): Float {
    val value = progress.coerceIn(0f, 1f)
    return value * value * (3f - 2f * value)
}

private fun blendColors(from: Color, to: Color, progress: Float): Color {
    val amount = progress.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * amount,
        green = from.green + (to.green - from.green) * amount,
        blue = from.blue + (to.blue - from.blue) * amount,
    )
}

private fun Color.withIntensity(intensity: Float): Color {
    val level = intensity.coerceIn(0f, 1f)
    return Color(red * level, green * level, blue * level, alpha)
}
