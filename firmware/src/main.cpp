#include <Arduino.h>
#include <BLE2902.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <Preferences.h>
#include <esp_arduino_version.h>

#include <array>
#include <cmath>
#include <string>

#include "PinMap.h"

namespace {

using ringcontroller::pins::RgbPins;

constexpr char kDeviceName[] = "D4WID-Ring";
constexpr char kFirmwareVersion[] = "0.5.2";
constexpr char kProtocolVersion[] = "1.2";
constexpr char kServiceUuid[] = "7d2f0001-9c5a-4f28-b4d7-4b3a6d9a0001";
constexpr char kCommandUuid[] = "7d2f0002-9c5a-4f28-b4d7-4b3a6d9a0001";
constexpr char kStateUuid[] = "7d2f0003-9c5a-4f28-b4d7-4b3a6d9a0001";
constexpr char kInfoUuid[] = "7d2f0004-9c5a-4f28-b4d7-4b3a6d9a0001";

constexpr uint32_t kSerialBaud = 115200;
// The inexpensive HW-153 optocoupler/IRF540 gate path is considerably slower
// than a direct logic-level MOSFET driver. A 500 Hz carrier gives it a full
// millisecond even at 50% duty while remaining visually flicker-free.
constexpr uint32_t kPwmFrequencyHz = 500;
constexpr uint8_t kPwmResolutionBits = 8;
constexpr uint8_t kPwmMaximum = 255;
constexpr bool kModuleInputActiveLow = false;
constexpr uint32_t kButtonDebounceMs = 40;
constexpr uint32_t kButtonLongPressMs = 850;
constexpr uint32_t kVehicleSignalDebounceMs = 250;
// Temporary bench mode: GPIO35 and GPIO36 are left physically unconnected until
// the PC817 modules arrive. Keep them ignored so floating inputs cannot trigger
// the forced-white or daytime-brightness automations. Change this to true after
// both inputs have verified external 3.3 V pull-ups and optoisolator wiring.
constexpr bool kVehicleInputsConnected = false;
constexpr uint32_t kRenderIntervalMs = 15;
constexpr uint32_t kCabinWarningCycleMs = 1700;
constexpr uint32_t kCabinWarningFlashMs = 140;
constexpr uint32_t kCabinWarningSecondFlashAtMs = 280;
constexpr size_t kMaximumFavorites = 12;
constexpr size_t kMaximumCustomScenes = 8;
constexpr size_t kMaximumCustomMoments = 12;
constexpr uint16_t kMinimumCustomMomentDurationMs = 150;
constexpr uint16_t kMaximumCustomMomentDurationMs = 5000;
constexpr int8_t kNoScene = -1;
constexpr int8_t kNoCustomScene = -1;
constexpr uint8_t kDefaultDaylightBrightnessPercent = 50;

enum class Scene : int8_t {
    AmberChase = 0,
    DemonPulse = 1,
    SpectrumWave = 2,
    HazardFlash = 3,
    HazardDouble = 4,
    InnerOuterAmber = 5,
    LeftAmber = 6,
    RightAmber = 7,
    InwardSweep = 8,
    OutwardSweep = 9,
    BrightWhite = 10,
    IceWhite = 11,
    ChallengerAmber = 12,
    CourtesyFade = 13,
    AmberBreathing = 14,
    RedlineChase = 15,
    CyanScanner = 16,
    SplitHorizon = 17,
    MirrorRainbow = 18,
    FavoriteCarousel = 19,
};

constexpr int8_t kMaximumSceneId = static_cast<int8_t>(Scene::FavoriteCarousel);

struct RgbColor {
    uint8_t red;
    uint8_t green;
    uint8_t blue;
};

enum class CustomTransition : uint8_t {
    Jump = 0,
    Smooth = 1,
};

struct CustomMoment {
    std::array<RgbColor, 4> colors;
    uint16_t durationMs;
    CustomTransition transition;
};

struct CustomSceneData {
    uint8_t momentCount;
    std::array<CustomMoment, kMaximumCustomMoments> moments;
};

constexpr std::array<RgbColor, 6> kDefaultFavoriteColors{{
    {242, 246, 255},
    {255, 106, 0},
    {255, 48, 78},
    {168, 85, 247},
    {0, 229, 229},
    {67, 224, 123},
}};
constexpr RgbColor kButtonFallbackWhite{242, 246, 255};
constexpr RgbColor kCabinWarningAmber{255, 106, 0};
constexpr RgbColor kOff{0, 0, 0};

class DebouncedInput {
public:
    DebouncedInput(const uint8_t pin, const bool activeLow, const uint32_t debounceMs)
        : pin_(pin), activeLow_(activeLow), debounceMs_(debounceMs) {}

    void begin() {
        pinMode(pin_, INPUT);
        rawState_ = digitalRead(pin_);
        stableState_ = rawState_;
        changedAtMs_ = millis();
    }

    bool update(const uint32_t nowMs) {
        const bool reading = digitalRead(pin_);
        if (reading != rawState_) {
            rawState_ = reading;
            changedAtMs_ = nowMs;
        }
        if (stableState_ != rawState_ && nowMs - changedAtMs_ >= debounceMs_) {
            stableState_ = rawState_;
            return true;
        }
        return false;
    }

    bool isActive() const {
        return activeLow_ ? stableState_ == LOW : stableState_ == HIGH;
    }

private:
    uint8_t pin_;
    bool activeLow_;
    uint32_t debounceMs_;
    bool rawState_{HIGH};
    bool stableState_{HIGH};
    uint32_t changedAtMs_{0};
};

Preferences preferences;
DebouncedInput buttonInput(ringcontroller::pins::kMomentaryButton, true, kButtonDebounceMs);
DebouncedInput vehicleSignalInput(ringcontroller::pins::kVehicleSignal, true, kVehicleSignalDebounceMs);
DebouncedInput daylightSignalInput(ringcontroller::pins::kDaylightSignal, true, kVehicleSignalDebounceMs);

bool userEnabled = true;
bool vehicleAutomationEnabled = true;
bool daylightAutomationEnabled = true;
uint8_t forcedWhiteBrightness = 255;
uint8_t globalBrightness = 224;
uint8_t daylightBrightnessPercent = kDefaultDaylightBrightnessPercent;
uint8_t favoriteColorIndex = 1;
uint8_t favoriteCount = kDefaultFavoriteColors.size();
int8_t activeScene = kNoScene;
int8_t activeCustomScene = kNoCustomScene;
std::array<RgbColor, 4> ringColors{{
    {0, 229, 229}, {0, 229, 229}, {0, 229, 229}, {0, 229, 229},
}};
std::array<RgbColor, kMaximumFavorites> favoriteColors{};
std::array<CustomSceneData, kMaximumCustomScenes> customScenes{};
CustomSceneData customUpload{};
int8_t customUploadSlot = kNoCustomScene;
uint16_t customUploadMask = 0;

uint32_t lastRenderMs = 0;
uint32_t sceneStartedAtMs = 0;
uint32_t buttonPressedAtMs = 0;
bool buttonPressTracked = false;
bool buttonLongPressHandled = false;
bool bleConnected = false;
bool lightStatePersistencePending = false;
uint32_t lightStateChangedAtMs = 0;
BLECharacteristic *stateCharacteristic = nullptr;

uint8_t outputDuty(const uint8_t brightness) {
    return kModuleInputActiveLow ? static_cast<uint8_t>(kPwmMaximum - brightness) : brightness;
}

uint8_t scaleChannel(const uint8_t value, const uint8_t brightness) {
    return static_cast<uint8_t>((static_cast<uint16_t>(value) * brightness + 127) / 255);
}

constexpr uint8_t brightnessFromPercent(const uint8_t percent) {
    return static_cast<uint8_t>((static_cast<uint16_t>(percent) * 255U + 50U) / 100U);
}

static_assert(brightnessFromPercent(0) == 0, "Zero percent must turn PWM brightness off");
static_assert(brightnessFromPercent(50) == 128, "Fifty percent must round to midpoint PWM");
static_assert(brightnessFromPercent(100) == 255, "One hundred percent must use full PWM");

RgbColor scaleColor(const RgbColor color, const uint8_t brightness) {
    return {
        scaleChannel(color.red, brightness),
        scaleChannel(color.green, brightness),
        scaleChannel(color.blue, brightness),
    };
}

bool colorsEqual(const RgbColor left, const RgbColor right) {
    return left.red == right.red && left.green == right.green && left.blue == right.blue;
}

bool allColorsEqual(const std::array<RgbColor, 4> &colors) {
    for (size_t index = 1; index < colors.size(); ++index) {
        if (!colorsEqual(colors[0], colors[index])) return false;
    }
    return true;
}

constexpr bool cabinWarningPulseOn(const uint32_t nowMs) {
    return nowMs % kCabinWarningCycleMs < kCabinWarningFlashMs ||
        (nowMs % kCabinWarningCycleMs >= kCabinWarningSecondFlashAtMs &&
         nowMs % kCabinWarningCycleMs < kCabinWarningSecondFlashAtMs + kCabinWarningFlashMs);
}

static_assert(cabinWarningPulseOn(0), "Cabin warning starts with a flash");
static_assert(!cabinWarningPulseOn(150), "Cabin warning separates the flashes");
static_assert(cabinWarningPulseOn(300), "Cabin warning contains a second flash");
static_assert(!cabinWarningPulseOn(500), "Cabin warning pauses after the double flash");

void attachPwmOutputs() {
    for (size_t index = 0; index < ringcontroller::pins::kPwmOutputs.size(); ++index) {
        const uint8_t pin = ringcontroller::pins::kPwmOutputs[index];
#if ESP_ARDUINO_VERSION_MAJOR >= 3
        if (!ledcAttach(pin, kPwmFrequencyHz, kPwmResolutionBits)) {
            Serial.printf("Failed to attach LEDC to GPIO%u\n", pin);
        }
#else
        ledcSetup(index, kPwmFrequencyHz, kPwmResolutionBits);
        ledcAttachPin(pin, index);
#endif
    }
}

void writePwm(const uint8_t pin, const uint8_t brightness) {
    const uint8_t duty = outputDuty(brightness);
#if ESP_ARDUINO_VERSION_MAJOR >= 3
    ledcWrite(pin, duty);
#else
    for (size_t index = 0; index < ringcontroller::pins::kPwmOutputs.size(); ++index) {
        if (ringcontroller::pins::kPwmOutputs[index] == pin) {
            ledcWrite(index, duty);
            return;
        }
    }
#endif
}

void applyColor(const RgbPins &pins, const RgbColor color) {
    writePwm(pins.red, color.red);
    writePwm(pins.green, color.green);
    writePwm(pins.blue, color.blue);
}

void applyRenderedColors(
    const std::array<RgbColor, 4> &colors,
    const uint32_t nowMs,
    const bool effectActive
) {
    for (size_t index = 0; index < colors.size(); ++index) {
        applyColor(ringcontroller::pins::kRings[index], scaleColor(colors[index], globalBrightness));
    }
    const bool warning = effectActive || !allColorsEqual(colors);
    const RgbColor indicator = warning
        ? (cabinWarningPulseOn(nowMs) ? kCabinWarningAmber : kOff)
        : scaleColor(colors[0], globalBrightness);
    applyColor(ringcontroller::pins::kCabinIndicator, indicator);
}

void applyAllRings(const RgbColor color) {
    for (const auto &ringPins : ringcontroller::pins::kRings) applyColor(ringPins, color);
    applyColor(ringcontroller::pins::kCabinIndicator, color);
}

RgbColor hsvToRgb(float hue, const float saturation, const float value) {
    hue = fmodf(hue, 360.0f);
    if (hue < 0.0f) hue += 360.0f;
    const float chroma = value * saturation;
    const float x = chroma * (1.0f - fabsf(fmodf(hue / 60.0f, 2.0f) - 1.0f));
    const float match = value - chroma;
    float red = 0.0f;
    float green = 0.0f;
    float blue = 0.0f;
    if (hue < 60.0f) { red = chroma; green = x; }
    else if (hue < 120.0f) { red = x; green = chroma; }
    else if (hue < 180.0f) { green = chroma; blue = x; }
    else if (hue < 240.0f) { green = x; blue = chroma; }
    else if (hue < 300.0f) { red = x; blue = chroma; }
    else { red = chroma; blue = x; }
    return {
        static_cast<uint8_t>((red + match) * 255.0f),
        static_cast<uint8_t>((green + match) * 255.0f),
        static_cast<uint8_t>((blue + match) * 255.0f),
    };
}

RgbColor sceneColor(const RgbColor color, const float intensity) {
    const float level = constrain(intensity, 0.0f, 1.0f);
    return {
        static_cast<uint8_t>(color.red * level),
        static_cast<uint8_t>(color.green * level),
        static_cast<uint8_t>(color.blue * level),
    };
}

RgbColor blendSceneColors(const RgbColor from, const RgbColor to, const float progress) {
    const float amount = constrain(progress, 0.0f, 1.0f);
    return {
        static_cast<uint8_t>(from.red + (to.red - from.red) * amount),
        static_cast<uint8_t>(from.green + (to.green - from.green) * amount),
        static_cast<uint8_t>(from.blue + (to.blue - from.blue) * amount),
    };
}

float smoothProgress(const float progress) {
    const float value = constrain(progress, 0.0f, 1.0f);
    return value * value * (3.0f - 2.0f * value);
}

float breathingLevel(const float seconds, const float periodSeconds, const float minimum) {
    const float wave = (sinf(seconds * (2.0f * PI / periodSeconds) - PI / 2.0f) + 1.0f) / 2.0f;
    return minimum + wave * (1.0f - minimum);
}

float pingPongPosition(const float seconds, const float ringsPerSecond) {
    const float phase = fmodf(seconds * ringsPerSecond, 6.0f);
    return phase <= 3.0f ? phase : 6.0f - phase;
}

std::array<RgbColor, 4> runnerColors(
    const float position,
    const RgbColor color,
    const float minimum,
    const bool wraps
) {
    std::array<RgbColor, 4> colors{};
    for (size_t index = 0; index < colors.size(); ++index) {
        float distance = fabsf(static_cast<float>(index) - position);
        if (wraps) distance = fminf(distance, 4.0f - distance);
        colors[index] = sceneColor(color, fmaxf(minimum, 1.0f - distance * 0.78f));
    }
    return colors;
}

std::array<RgbColor, 4> sceneColors(const uint32_t nowMs) {
    const float seconds = (nowMs - sceneStartedAtMs) / 1000.0f;
    std::array<RgbColor, 4> colors{};
    constexpr RgbColor off{0, 0, 0};
    constexpr RgbColor amber{255, 106, 0};
    constexpr RgbColor demonRed{255, 8, 18};
    constexpr RgbColor cyan{0, 229, 229};
    constexpr RgbColor violet{168, 85, 247};

    switch (static_cast<Scene>(activeScene)) {
        case Scene::AmberChase:
            return runnerColors(fmodf(seconds * 1.65f, 4.0f), amber, 0.08f, true);

        case Scene::DemonPulse:
            colors.fill(sceneColor(demonRed, breathingLevel(seconds, 2.8f, 0.25f)));
            break;

        case Scene::SpectrumWave:
            for (size_t index = 0; index < colors.size(); ++index) {
                colors[index] = hsvToRgb(seconds * 62.0f + index * 52.0f, 0.82f, 1.0f);
            }
            break;

        case Scene::HazardFlash:
            colors.fill(fmodf(seconds, 1.0f) < 0.46f ? amber : off);
            break;

        case Scene::HazardDouble: {
            const float phase = fmodf(seconds, 1.5f);
            const bool illuminated = phase < 0.16f || (phase >= 0.31f && phase < 0.47f);
            colors.fill(illuminated ? amber : off);
            break;
        }

        case Scene::InnerOuterAmber: {
            const float phase = fmodf(seconds, 1.0f);
            if (phase < 0.42f) {
                colors = {amber, off, off, amber};
            } else if (phase >= 0.50f && phase < 0.92f) {
                colors = {off, amber, amber, off};
            }
            break;
        }

        case Scene::LeftAmber:
            if (fmodf(seconds, 1.0f) < 0.5f) colors = {amber, amber, off, off};
            break;

        case Scene::RightAmber:
            if (fmodf(seconds, 1.0f) < 0.5f) colors = {off, off, amber, amber};
            break;

        case Scene::InwardSweep: {
            const uint8_t step = static_cast<uint8_t>(fmodf(seconds, 1.45f) / 0.24f);
            if (step == 0 || step == 2) colors = {amber, off, off, amber};
            else if (step == 1 || step == 3) colors = {off, amber, amber, off};
            break;
        }

        case Scene::OutwardSweep: {
            const uint8_t step = static_cast<uint8_t>(fmodf(seconds, 1.45f) / 0.24f);
            if (step == 0 || step == 2) colors = {off, amber, amber, off};
            else if (step == 1 || step == 3) colors = {amber, off, off, amber};
            break;
        }

        case Scene::BrightWhite:
            colors.fill({255, 255, 255});
            break;

        case Scene::IceWhite:
            colors.fill({205, 232, 255});
            break;

        case Scene::ChallengerAmber:
            colors.fill(amber);
            break;

        case Scene::CourtesyFade:
            colors.fill(sceneColor({246, 250, 255}, breathingLevel(seconds, 4.8f, 0.12f)));
            break;

        case Scene::AmberBreathing:
            colors.fill(sceneColor(amber, breathingLevel(seconds, 3.4f, 0.10f)));
            break;

        case Scene::RedlineChase:
            return runnerColors(pingPongPosition(seconds, 1.85f), demonRed, 0.025f, false);

        case Scene::CyanScanner:
            return runnerColors(pingPongPosition(seconds, 2.35f), cyan, 0.035f, false);

        case Scene::SplitHorizon: {
            const float left = breathingLevel(seconds, 3.2f, 0.32f);
            const float right = breathingLevel(seconds + 1.6f, 3.2f, 0.32f);
            colors = {
                sceneColor(cyan, left), sceneColor(cyan, left),
                sceneColor(violet, right), sceneColor(violet, right),
            };
            break;
        }

        case Scene::MirrorRainbow: {
            const float hue = fmodf(seconds * 42.0f, 360.0f);
            const RgbColor outer = hsvToRgb(hue, 0.82f, 1.0f);
            const RgbColor inner = hsvToRgb(hue + 105.0f, 0.82f, 1.0f);
            colors = {outer, inner, inner, outer};
            break;
        }

        case Scene::FavoriteCarousel: {
            if (favoriteCount == 0) break;
            const float position = seconds / 2.6f;
            const uint8_t current = static_cast<uint8_t>(floorf(position)) % favoriteCount;
            const uint8_t next = (current + 1) % favoriteCount;
            const float blend = smoothProgress(position - floorf(position));
            colors.fill(blendSceneColors(favoriteColors[current], favoriteColors[next], blend));
            break;
        }
    }
    return colors;
}

std::array<RgbColor, 4> customSceneColors(const uint32_t nowMs) {
    std::array<RgbColor, 4> colors{};
    if (activeCustomScene < 0 || activeCustomScene >= static_cast<int8_t>(customScenes.size())) return colors;
    const CustomSceneData &scene = customScenes[activeCustomScene];
    if (scene.momentCount < 2 || scene.momentCount > kMaximumCustomMoments) return colors;

    uint32_t totalDurationMs = 0;
    for (uint8_t index = 0; index < scene.momentCount; ++index) {
        totalDurationMs += scene.moments[index].durationMs;
    }
    if (totalDurationMs == 0) return colors;

    uint32_t elapsedMs = (nowMs - sceneStartedAtMs) % totalDurationMs;
    uint8_t currentIndex = 0;
    while (
        currentIndex + 1 < scene.momentCount &&
        elapsedMs >= scene.moments[currentIndex].durationMs
    ) {
        elapsedMs -= scene.moments[currentIndex].durationMs;
        ++currentIndex;
    }
    const CustomMoment &current = scene.moments[currentIndex];
    if (current.transition == CustomTransition::Jump) return current.colors;

    const CustomMoment &next = scene.moments[(currentIndex + 1) % scene.momentCount];
    const float progress = smoothProgress(
        static_cast<float>(elapsedMs) / static_cast<float>(current.durationMs)
    );
    for (size_t ring = 0; ring < colors.size(); ++ring) {
        colors[ring] = blendSceneColors(current.colors[ring], next.colors[ring], progress);
    }
    return colors;
}

void renderOutputs(const uint32_t nowMs) {
    if (kVehicleInputsConnected && vehicleAutomationEnabled && vehicleSignalInput.isActive()) {
        applyAllRings({forcedWhiteBrightness, forcedWhiteBrightness, forcedWhiteBrightness});
    } else if (!userEnabled) {
        applyAllRings({0, 0, 0});
    } else if (activeCustomScene != kNoCustomScene) {
        applyRenderedColors(customSceneColors(nowMs), nowMs, true);
    } else if (activeScene != kNoScene) {
        applyRenderedColors(sceneColors(nowMs), nowMs, true);
    } else {
        applyRenderedColors(ringColors, nowMs, false);
    }
}

String colorHex(const RgbColor color) {
    char buffer[7];
    snprintf(buffer, sizeof(buffer), "%02X%02X%02X", color.red, color.green, color.blue);
    return String(buffer);
}

bool parseHexColor(const String &value, RgbColor &color) {
    if (value.length() != 6) return false;
    char *end = nullptr;
    const unsigned long parsed = strtoul(value.c_str(), &end, 16);
    if (end == value.c_str() || *end != '\0') return false;
    color = {
        static_cast<uint8_t>((parsed >> 16) & 0xFF),
        static_cast<uint8_t>((parsed >> 8) & 0xFF),
        static_cast<uint8_t>(parsed & 0xFF),
    };
    return true;
}

bool parseRingColors(const String &payload, std::array<RgbColor, 4> &colors) {
    int start = 0;
    for (size_t index = 0; index < colors.size(); ++index) {
        const int end = payload.indexOf(',', start);
        const String value = end < 0 ? payload.substring(start) : payload.substring(start, end);
        if (!parseHexColor(value, colors[index])) return false;
        if (index + 1 < colors.size() && end < 0) return false;
        if (index + 1 == colors.size() && end >= 0) return false;
        start = end + 1;
    }
    return true;
}

String customSceneKey(const uint8_t slot) {
    return String("custom-") + String(slot);
}

String fieldAt(const String &message, const int index) {
    int start = 0;
    for (int current = 0; current <= index; ++current) {
        const int end = message.indexOf('|', start);
        if (current == index) return end < 0 ? message.substring(start) : message.substring(start, end);
        if (end < 0) return String();
        start = end + 1;
    }
    return String();
}

void persistLightState() {
    preferences.putBool("enabled", userEnabled);
    preferences.putUChar("brightness", globalBrightness);
    preferences.putChar("scene", activeScene);
    preferences.putChar("custom", activeCustomScene);
    preferences.putBytes("colors", ringColors.data(), sizeof(ringColors));
}

void scheduleLightStatePersistence() {
    lightStatePersistencePending = true;
    lightStateChangedAtMs = millis();
}

void persistFavorites() {
    preferences.putUChar("fav-count", favoriteCount);
    preferences.putUChar("favorite", favoriteColorIndex);
    preferences.putBytes("favorites", favoriteColors.data(), sizeof(favoriteColors));
}

String buildControllerState() {
    String state = "STATE|";
    state += kProtocolVersion;
    state += '|';
    state += kFirmwareVersion;
    state += '|';
    state += userEnabled ? '1' : '0';
    state += '|';
    state += String(globalBrightness);
    state += '|';
    state += String(static_cast<int>(activeScene));
    state += '|';
    state += (kVehicleInputsConnected && vehicleAutomationEnabled && vehicleSignalInput.isActive()) ? '1' : '0';
    state += '|';
    state += (kVehicleInputsConnected && vehicleSignalInput.isActive()) ? '1' : '0';
    state += '|';
    state += vehicleAutomationEnabled ? '1' : '0';
    state += '|';
    for (size_t index = 0; index < ringColors.size(); ++index) {
        if (index > 0) state += ',';
        state += colorHex(ringColors[index]);
    }
    state += '|';
    for (uint8_t index = 0; index < favoriteCount; ++index) {
        if (index > 0) state += ',';
        state += colorHex(favoriteColors[index]);
    }
    state += '|';
    state += String(static_cast<int>(activeCustomScene));
    state += '|';
    state += (kVehicleInputsConnected && daylightSignalInput.isActive()) ? '1' : '0';
    state += '|';
    state += daylightAutomationEnabled ? '1' : '0';
    state += '|';
    state += String(daylightBrightnessPercent);
    return state;
}

void publishControllerState() {
    if (stateCharacteristic == nullptr) return;
    const String state = buildControllerState();
    stateCharacteristic->setValue(state.c_str());
    if (bleConnected) stateCharacteristic->notify();
    Serial.println(state);
}

void selectScene(const int8_t scene) {
    activeScene = scene;
    activeCustomScene = kNoCustomScene;
    sceneStartedAtMs = millis();
    if (scene != kNoScene) userEnabled = true;
}

void selectCustomScene(const int8_t slot) {
    activeScene = kNoScene;
    activeCustomScene = slot;
    sceneStartedAtMs = millis();
    if (slot != kNoCustomScene) userEnabled = true;
}

void applyFavoriteToRings(const uint8_t index) {
    if (favoriteCount == 0) return;
    favoriteColorIndex = index % favoriteCount;
    ringColors.fill(favoriteColors[favoriteColorIndex]);
    selectScene(kNoScene);
}

int favoriteIndexForColor(const RgbColor color) {
    for (uint8_t index = 0; index < favoriteCount; ++index) {
        if (colorsEqual(favoriteColors[index], color)) return index;
    }
    return -1;
}

void applyButtonFallbackWhite() {
    ringColors.fill(kButtonFallbackWhite);
    selectScene(kNoScene);
    userEnabled = true;

    const int matchingFavorite = favoriteIndexForColor(kButtonFallbackWhite);
    favoriteColorIndex = matchingFavorite >= 0
        ? static_cast<uint8_t>(matchingFavorite)
        : static_cast<uint8_t>(favoriteCount - 1);
}

void setFavoritesFromCommand(const String &payload) {
    uint8_t count = 0;
    int start = 0;
    while (start <= payload.length() && count < kMaximumFavorites) {
        const int end = payload.indexOf(',', start);
        const String value = end < 0 ? payload.substring(start) : payload.substring(start, end);
        RgbColor color{};
        if (parseHexColor(value, color)) favoriteColors[count++] = color;
        if (end < 0) break;
        start = end + 1;
    }
    if (count > 0) {
        favoriteCount = count;
        favoriteColorIndex %= favoriteCount;
        persistFavorites();
    }
}

bool customSceneSlotValid(const int slot) {
    return slot >= 0 && slot < static_cast<int>(kMaximumCustomScenes);
}

void beginCustomSceneUpload(const int slot, const int momentCount) {
    if (!customSceneSlotValid(slot) || momentCount < 2 || momentCount > static_cast<int>(kMaximumCustomMoments)) {
        customUploadSlot = kNoCustomScene;
        customUploadMask = 0;
        return;
    }
    customUpload = {};
    customUpload.momentCount = static_cast<uint8_t>(momentCount);
    customUploadSlot = static_cast<int8_t>(slot);
    customUploadMask = 0;
}

void setCustomSceneUploadMoment(
    const int slot,
    const int index,
    const int durationMs,
    const int transition,
    const String &colorsPayload
) {
    if (
        slot != customUploadSlot || index < 0 || index >= customUpload.momentCount ||
        durationMs < kMinimumCustomMomentDurationMs || durationMs > kMaximumCustomMomentDurationMs ||
        (transition != static_cast<int>(CustomTransition::Jump) &&
         transition != static_cast<int>(CustomTransition::Smooth))
    ) return;

    CustomMoment moment{};
    if (!parseRingColors(colorsPayload, moment.colors)) return;
    moment.durationMs = static_cast<uint16_t>(durationMs);
    moment.transition = static_cast<CustomTransition>(transition);
    customUpload.moments[index] = moment;
    customUploadMask |= static_cast<uint16_t>(1U << index);
}

bool commitCustomSceneUpload(const int slot) {
    if (slot != customUploadSlot || customUpload.momentCount < 2) return false;
    const uint16_t expectedMask = static_cast<uint16_t>((1U << customUpload.momentCount) - 1U);
    if (customUploadMask != expectedMask) return false;

    customScenes[slot] = customUpload;
    const String key = customSceneKey(static_cast<uint8_t>(slot));
    preferences.putBytes(key.c_str(), &customScenes[slot], sizeof(CustomSceneData));
    customUploadSlot = kNoCustomScene;
    customUploadMask = 0;
    return true;
}

void deleteCustomScene(const int slot) {
    if (!customSceneSlotValid(slot)) return;
    customScenes[slot] = {};
    const String key = customSceneKey(static_cast<uint8_t>(slot));
    preferences.remove(key.c_str());
    if (activeCustomScene == slot) selectCustomScene(kNoCustomScene);
}

void handleBleCommand(String message) {
    message.trim();
    const String command = fieldAt(message, 0);
    if (command == "GET") {
        publishControllerState();
        return;
    }
    if (command == "POWER") {
        userEnabled = fieldAt(message, 1).toInt() != 0;
        scheduleLightStatePersistence();
    } else if (command == "BRIGHTNESS") {
        globalBrightness = static_cast<uint8_t>(constrain(fieldAt(message, 1).toInt(), 0, 255));
        scheduleLightStatePersistence();
    } else if (command == "COLOR") {
        const int target = fieldAt(message, 1).toInt();
        RgbColor color{};
        if (parseHexColor(fieldAt(message, 2), color)) {
            selectScene(kNoScene);
            userEnabled = true;
            if (target == 255) ringColors.fill(color);
            else if (target >= 0 && target < static_cast<int>(ringColors.size())) ringColors[target] = color;
            scheduleLightStatePersistence();
        }
    } else if (command == "SCENE") {
        const int scene = fieldAt(message, 1).toInt();
        selectScene(scene >= 0 && scene <= kMaximumSceneId ? static_cast<int8_t>(scene) : kNoScene);
        scheduleLightStatePersistence();
    } else if (command == "FAVORITES") {
        setFavoritesFromCommand(fieldAt(message, 1));
    } else if (command == "VEHICLE") {
        vehicleAutomationEnabled = fieldAt(message, 1).toInt() != 0;
        preferences.putBool("veh-auto", vehicleAutomationEnabled);
    } else if (command == "DAYLIGHT") {
        daylightAutomationEnabled = fieldAt(message, 1).toInt() != 0;
        daylightBrightnessPercent = static_cast<uint8_t>(constrain(fieldAt(message, 2).toInt(), 0, 100));
        preferences.putBool("day-auto", daylightAutomationEnabled);
        preferences.putUChar("day-level", daylightBrightnessPercent);
        if (kVehicleInputsConnected && daylightAutomationEnabled && daylightSignalInput.isActive()) {
            globalBrightness = brightnessFromPercent(daylightBrightnessPercent);
            scheduleLightStatePersistence();
        }
    } else if (command == "CUSTOM_BEGIN") {
        beginCustomSceneUpload(fieldAt(message, 1).toInt(), fieldAt(message, 2).toInt());
    } else if (command == "CUSTOM_STEP") {
        setCustomSceneUploadMoment(
            fieldAt(message, 1).toInt(),
            fieldAt(message, 2).toInt(),
            fieldAt(message, 3).toInt(),
            fieldAt(message, 4).toInt(),
            fieldAt(message, 5)
        );
    } else if (command == "CUSTOM_COMMIT") {
        commitCustomSceneUpload(fieldAt(message, 1).toInt());
    } else if (command == "CUSTOM_PLAY") {
        const int slot = fieldAt(message, 1).toInt();
        if (customSceneSlotValid(slot) && customScenes[slot].momentCount >= 2) {
            selectCustomScene(static_cast<int8_t>(slot));
            scheduleLightStatePersistence();
        }
    } else if (command == "CUSTOM_DELETE") {
        deleteCustomScene(fieldAt(message, 1).toInt());
        scheduleLightStatePersistence();
    } else {
        Serial.printf("Unsupported BLE command: %s\n", message.c_str());
        return;
    }
    publishControllerState();
}

class ControllerServerCallbacks final : public BLEServerCallbacks {
    void onConnect(BLEServer *) override {
        bleConnected = true;
        Serial.println("BLE client connected");
    }

    void onDisconnect(BLEServer *) override {
        bleConnected = false;
        BLEDevice::startAdvertising();
        Serial.println("BLE client disconnected; advertising restarted");
    }
};

class CommandCallbacks final : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *characteristic) override {
        const std::string value = characteristic->getValue();
        handleBleCommand(String(value.c_str()));
    }
};

void beginBle() {
    BLEDevice::init(kDeviceName);
    BLEDevice::setMTU(185);
    BLEServer *server = BLEDevice::createServer();
    server->setCallbacks(new ControllerServerCallbacks());
    BLEService *service = server->createService(kServiceUuid);
    BLECharacteristic *command = service->createCharacteristic(
        kCommandUuid,
        BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR
    );
    command->setCallbacks(new CommandCallbacks());
    stateCharacteristic = service->createCharacteristic(
        kStateUuid,
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY
    );
    stateCharacteristic->addDescriptor(new BLE2902());
    BLECharacteristic *info = service->createCharacteristic(kInfoUuid, BLECharacteristic::PROPERTY_READ);
    String infoValue = String("INFO|") + kProtocolVersion + '|' + kFirmwareVersion + '|' + kDeviceName;
    info->setValue(infoValue.c_str());
    service->start();
    publishControllerState();

    BLEAdvertising *advertising = BLEDevice::getAdvertising();
    advertising->addServiceUUID(kServiceUuid);
    advertising->setScanResponse(true);
    BLEDevice::startAdvertising();
    Serial.println("BLE advertising as D4WID-Ring");
}

void loadConfiguration() {
    preferences.begin("ringctrl", false);
    userEnabled = preferences.getBool("enabled", true);
    vehicleAutomationEnabled = preferences.getBool("veh-auto", true);
    daylightAutomationEnabled = preferences.getBool("day-auto", true);
    daylightBrightnessPercent = static_cast<uint8_t>(constrain(
        preferences.getUChar("day-level", kDefaultDaylightBrightnessPercent),
        static_cast<uint8_t>(0),
        static_cast<uint8_t>(100)
    ));
    forcedWhiteBrightness = preferences.getUChar("white", 255);
    globalBrightness = preferences.getUChar("brightness", 224);
    activeScene = preferences.getChar("scene", kNoScene);
    if (activeScene < kNoScene || activeScene > kMaximumSceneId) activeScene = kNoScene;

    customScenes.fill({});
    for (uint8_t slot = 0; slot < kMaximumCustomScenes; ++slot) {
        const String key = customSceneKey(slot);
        if (preferences.getBytesLength(key.c_str()) != sizeof(CustomSceneData)) continue;
        preferences.getBytes(key.c_str(), &customScenes[slot], sizeof(CustomSceneData));
        CustomSceneData &scene = customScenes[slot];
        bool valid = scene.momentCount >= 2 && scene.momentCount <= kMaximumCustomMoments;
        for (uint8_t index = 0; valid && index < scene.momentCount; ++index) {
            const CustomMoment &moment = scene.moments[index];
            valid = moment.durationMs >= kMinimumCustomMomentDurationMs &&
                moment.durationMs <= kMaximumCustomMomentDurationMs &&
                (moment.transition == CustomTransition::Jump ||
                 moment.transition == CustomTransition::Smooth);
        }
        if (!valid) scene = {};
    }
    activeCustomScene = preferences.getChar("custom", kNoCustomScene);
    if (
        activeCustomScene < 0 || activeCustomScene >= static_cast<int8_t>(customScenes.size()) ||
        customScenes[activeCustomScene].momentCount < 2
    ) {
        activeCustomScene = kNoCustomScene;
    } else {
        activeScene = kNoScene;
    }

    if (preferences.getBytesLength("colors") == sizeof(ringColors)) {
        preferences.getBytes("colors", ringColors.data(), sizeof(ringColors));
    }
    favoriteColors.fill({0, 0, 0});
    for (size_t index = 0; index < kDefaultFavoriteColors.size(); ++index) {
        favoriteColors[index] = kDefaultFavoriteColors[index];
    }
    if (preferences.getBytesLength("favorites") == sizeof(favoriteColors)) {
        preferences.getBytes("favorites", favoriteColors.data(), sizeof(favoriteColors));
    }
    favoriteCount = constrain(
        preferences.getUChar("fav-count", kDefaultFavoriteColors.size()),
        static_cast<uint8_t>(1),
        static_cast<uint8_t>(kMaximumFavorites)
    );
    favoriteColorIndex = preferences.getUChar("favorite", 1) % favoriteCount;
    sceneStartedAtMs = millis();
}

void handleShortButtonPress() {
    const bool effectActive = activeScene != kNoScene || activeCustomScene != kNoCustomScene;
    const bool mixedColors = !allColorsEqual(ringColors);

    if (effectActive || mixedColors) {
        applyButtonFallbackWhite();
    } else if (!userEnabled) {
        userEnabled = true;
    } else if (favoriteCount > 0) {
        const int currentFavorite = favoriteIndexForColor(ringColors[0]);
        const uint8_t cycleFrom = currentFavorite >= 0
            ? static_cast<uint8_t>(currentFavorite)
            : favoriteColorIndex;
        applyFavoriteToRings((cycleFrom + 1) % favoriteCount);
    }
    persistLightState();
    persistFavorites();
    publishControllerState();
}

void handleInputs(const uint32_t nowMs) {
    if (buttonInput.update(nowMs)) {
        if (buttonInput.isActive()) {
            buttonPressedAtMs = nowMs;
            buttonPressTracked = true;
            buttonLongPressHandled = false;
        } else if (buttonPressTracked) {
            if (!buttonLongPressHandled) handleShortButtonPress();
            buttonPressTracked = false;
        }
    }

    if (
        buttonPressTracked && buttonInput.isActive() && !buttonLongPressHandled &&
        nowMs - buttonPressedAtMs >= kButtonLongPressMs
    ) {
        userEnabled = false;
        buttonLongPressHandled = true;
        persistLightState();
        publishControllerState();
    }

    if (kVehicleInputsConnected && vehicleSignalInput.update(nowMs)) {
        Serial.printf("Vehicle signal %s\n", vehicleSignalInput.isActive() ? "active" : "inactive");
        publishControllerState();
    }

    if (kVehicleInputsConnected && daylightSignalInput.update(nowMs)) {
        Serial.printf("Daylight signal %s\n", daylightSignalInput.isActive() ? "active" : "inactive");
        if (daylightAutomationEnabled && daylightSignalInput.isActive()) {
            globalBrightness = brightnessFromPercent(daylightBrightnessPercent);
            scheduleLightStatePersistence();
        }
        publishControllerState();
    }
}

}  // namespace

void setup() {
    Serial.begin(kSerialBaud);
    delay(150);
    Serial.println("D4WID Ring firmware starting");

    attachPwmOutputs();
    applyAllRings({0, 0, 0});
    buttonInput.begin();
    if (kVehicleInputsConnected) {
        vehicleSignalInput.begin();
        daylightSignalInput.begin();
    }
    loadConfiguration();
    if (kVehicleInputsConnected && daylightAutomationEnabled && daylightSignalInput.isActive()) {
        globalBrightness = brightnessFromPercent(daylightBrightnessPercent);
        scheduleLightStatePersistence();
    }
    beginBle();
}

void loop() {
    const uint32_t nowMs = millis();
    handleInputs(nowMs);
    if (lightStatePersistencePending && nowMs - lightStateChangedAtMs >= 750) {
        lightStatePersistencePending = false;
        persistLightState();
    }
    if (nowMs - lastRenderMs >= kRenderIntervalMs) {
        lastRenderMs = nowMs;
        renderOutputs(nowMs);
    }
    delay(1);
}
