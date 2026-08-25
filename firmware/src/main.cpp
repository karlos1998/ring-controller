#include <Arduino.h>
#include <Preferences.h>
#include <esp_arduino_version.h>

#include <array>

#include "PinMap.h"

namespace {

using ringcontroller::pins::RgbPins;

constexpr uint32_t kSerialBaud = 115200;
constexpr uint32_t kPwmFrequencyHz = 1000;
constexpr uint8_t kPwmResolutionBits = 8;
constexpr uint8_t kPwmMaximum = 255;

// The currently tested HW-153 behavior is active-high at S. Set this to true
// only if a specific board is verified to use inverted control logic.
constexpr bool kModuleInputActiveLow = false;

constexpr uint32_t kButtonDebounceMs = 40;
constexpr uint32_t kButtonLongPressMs = 850;
constexpr uint32_t kVehicleSignalDebounceMs = 250;
constexpr uint32_t kRenderIntervalMs = 15;

struct RgbColor {
    uint8_t red;
    uint8_t green;
    uint8_t blue;
};

constexpr std::array<RgbColor, 6> kFavoriteColors{{
    {242, 246, 255},  // Ice
    {255, 106, 0},    // Amber
    {255, 48, 78},    // Red
    {168, 85, 247},   // Violet
    {51, 136, 255},   // Blue
    {67, 224, 123},   // Green
}};

class DebouncedInput {
public:
    DebouncedInput(const uint8_t pin, const bool activeLow, const uint32_t debounceMs)
        : pin_(pin), activeLow_(activeLow), debounceMs_(debounceMs) {}

    void begin() {
        // GPIO34 and GPIO35 have no internal pull-up/down resistors.
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

bool userEnabled = true;
bool vehicleAutomationEnabled = true;
uint8_t forcedWhiteBrightness = 255;
uint8_t favoriteColorIndex = 1;
uint32_t lastRenderMs = 0;
uint32_t buttonPressedAtMs = 0;
bool buttonPressTracked = false;
bool buttonLongPressHandled = false;

uint8_t outputDuty(const uint8_t brightness) {
    return kModuleInputActiveLow ? static_cast<uint8_t>(kPwmMaximum - brightness) : brightness;
}

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

void applyAllRings(const RgbColor color) {
    for (const auto &ringPins : ringcontroller::pins::kRings) {
        applyColor(ringPins, color);
    }
    applyColor(ringcontroller::pins::kCabinIndicator, color);
}

void renderNormalState() {
    if (!userEnabled) {
        applyAllRings({0, 0, 0});
        return;
    }

    applyAllRings(kFavoriteColors[favoriteColorIndex]);
}

void renderOutputs() {
    if (vehicleAutomationEnabled && vehicleSignalInput.isActive()) {
        applyAllRings({forcedWhiteBrightness, forcedWhiteBrightness, forcedWhiteBrightness});
        return;
    }

    renderNormalState();
}

void loadConfiguration() {
    preferences.begin("ringctrl", false);
    userEnabled = preferences.getBool("enabled", true);
    vehicleAutomationEnabled = preferences.getBool("veh-auto", true);
    forcedWhiteBrightness = preferences.getUChar("white", 255);
    favoriteColorIndex = static_cast<uint8_t>(
        preferences.getUChar("favorite", 1) % kFavoriteColors.size()
    );
}

void handleShortButtonPress() {
    if (!userEnabled) {
        userEnabled = true;
        Serial.printf("Rings enabled with favorite %u\n", favoriteColorIndex);
    } else {
        favoriteColorIndex = static_cast<uint8_t>(
            (favoriteColorIndex + 1) % kFavoriteColors.size()
        );
        preferences.putUChar("favorite", favoriteColorIndex);
        Serial.printf("Selected favorite %u\n", favoriteColorIndex);
    }

    preferences.putBool("enabled", userEnabled);
}

void handleInputs(const uint32_t nowMs) {
    if (buttonInput.update(nowMs)) {
        if (buttonInput.isActive()) {
            buttonPressedAtMs = nowMs;
            buttonPressTracked = true;
            buttonLongPressHandled = false;
        } else if (buttonPressTracked) {
            if (!buttonLongPressHandled) {
                handleShortButtonPress();
            }
            buttonPressTracked = false;
        }
    }

    if (
        buttonPressTracked &&
        buttonInput.isActive() &&
        !buttonLongPressHandled &&
        nowMs - buttonPressedAtMs >= kButtonLongPressMs
    ) {
        userEnabled = false;
        buttonLongPressHandled = true;
        preferences.putBool("enabled", false);
        Serial.println("Rings disabled by button long press");
    }

    if (vehicleSignalInput.update(nowMs)) {
        Serial.printf("Vehicle signal %s\n", vehicleSignalInput.isActive() ? "active" : "inactive");
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
    vehicleSignalInput.begin();
    loadConfiguration();

    Serial.printf(
        "Configuration: enabled=%s, vehicleAutomation=%s, white=%u, favorite=%u\n",
        userEnabled ? "true" : "false",
        vehicleAutomationEnabled ? "true" : "false",
        forcedWhiteBrightness,
        favoriteColorIndex
    );
}

void loop() {
    const uint32_t nowMs = millis();
    handleInputs(nowMs);

    if (nowMs - lastRenderMs >= kRenderIntervalMs) {
        lastRenderMs = nowMs;
        renderOutputs();
    }

    delay(1);
}
