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
constexpr uint32_t kVehicleSignalDebounceMs = 250;
constexpr uint32_t kRainbowStepMs = 15;

struct RgbColor {
    uint8_t red;
    uint8_t green;
    uint8_t blue;
};

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
uint32_t lastRainbowStepMs = 0;
uint8_t rainbowPosition = 0;

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

RgbColor rainbowWheel(uint8_t position) {
    if (position < 85) {
        return {
            static_cast<uint8_t>(255 - position * 3),
            static_cast<uint8_t>(position * 3),
            0,
        };
    }

    if (position < 170) {
        position -= 85;
        return {
            0,
            static_cast<uint8_t>(255 - position * 3),
            static_cast<uint8_t>(position * 3),
        };
    }

    position -= 170;
    return {
        static_cast<uint8_t>(position * 3),
        0,
        static_cast<uint8_t>(255 - position * 3),
    };
}

void renderNormalState() {
    if (!userEnabled) {
        applyAllRings({0, 0, 0});
        return;
    }

    constexpr std::array<uint8_t, 4> kPhaseOffsets{{0, 64, 128, 192}};
    for (size_t ring = 0; ring < ringcontroller::pins::kRings.size(); ++ring) {
        applyColor(
            ringcontroller::pins::kRings[ring],
            rainbowWheel(static_cast<uint8_t>(rainbowPosition + kPhaseOffsets[ring]))
        );
    }

    // The cabin indicator mirrors ring 1 during the bench-test animation.
    applyColor(ringcontroller::pins::kCabinIndicator, rainbowWheel(rainbowPosition));
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
}

void handleInputs(const uint32_t nowMs) {
    if (buttonInput.update(nowMs) && buttonInput.isActive()) {
        userEnabled = !userEnabled;
        preferences.putBool("enabled", userEnabled);
        Serial.printf("Rings %s by physical button\n", userEnabled ? "enabled" : "disabled");
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
        "Configuration: enabled=%s, vehicleAutomation=%s, white=%u\n",
        userEnabled ? "true" : "false",
        vehicleAutomationEnabled ? "true" : "false",
        forcedWhiteBrightness
    );
}

void loop() {
    const uint32_t nowMs = millis();
    handleInputs(nowMs);

    if (nowMs - lastRainbowStepMs >= kRainbowStepMs) {
        lastRainbowStepMs = nowMs;
        ++rainbowPosition;
        renderOutputs();
    }

    delay(1);
}
