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
constexpr char kFirmwareVersion[] = "0.2.0";
constexpr char kProtocolVersion[] = "1.0";
constexpr char kServiceUuid[] = "7d2f0001-9c5a-4f28-b4d7-4b3a6d9a0001";
constexpr char kCommandUuid[] = "7d2f0002-9c5a-4f28-b4d7-4b3a6d9a0001";
constexpr char kStateUuid[] = "7d2f0003-9c5a-4f28-b4d7-4b3a6d9a0001";
constexpr char kInfoUuid[] = "7d2f0004-9c5a-4f28-b4d7-4b3a6d9a0001";

constexpr uint32_t kSerialBaud = 115200;
constexpr uint32_t kPwmFrequencyHz = 1000;
constexpr uint8_t kPwmResolutionBits = 8;
constexpr uint8_t kPwmMaximum = 255;
constexpr bool kModuleInputActiveLow = false;
constexpr uint32_t kButtonDebounceMs = 40;
constexpr uint32_t kButtonLongPressMs = 850;
constexpr uint32_t kVehicleSignalDebounceMs = 250;
constexpr uint32_t kRenderIntervalMs = 15;
constexpr size_t kMaximumFavorites = 12;
constexpr int8_t kNoScene = -1;

enum class Scene : int8_t {
    AmberChase = 0,
    DemonPulse = 1,
    SpectrumWave = 2,
};

struct RgbColor {
    uint8_t red;
    uint8_t green;
    uint8_t blue;
};

constexpr std::array<RgbColor, 6> kDefaultFavoriteColors{{
    {242, 246, 255},
    {255, 106, 0},
    {255, 48, 78},
    {168, 85, 247},
    {0, 229, 229},
    {67, 224, 123},
}};

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

bool userEnabled = true;
bool vehicleAutomationEnabled = true;
uint8_t forcedWhiteBrightness = 255;
uint8_t globalBrightness = 224;
uint8_t favoriteColorIndex = 1;
uint8_t favoriteCount = kDefaultFavoriteColors.size();
int8_t activeScene = kNoScene;
std::array<RgbColor, 4> ringColors{{
    {0, 229, 229}, {0, 229, 229}, {0, 229, 229}, {0, 229, 229},
}};
std::array<RgbColor, kMaximumFavorites> favoriteColors{};

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

RgbColor scaleColor(const RgbColor color, const uint8_t brightness) {
    return {
        scaleChannel(color.red, brightness),
        scaleChannel(color.green, brightness),
        scaleChannel(color.blue, brightness),
    };
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

void applyRenderedColors(const std::array<RgbColor, 4> &colors) {
    for (size_t index = 0; index < colors.size(); ++index) {
        applyColor(ringcontroller::pins::kRings[index], scaleColor(colors[index], globalBrightness));
    }
    applyColor(ringcontroller::pins::kCabinIndicator, scaleColor(colors[0], globalBrightness));
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

std::array<RgbColor, 4> sceneColors(const uint32_t nowMs) {
    const float seconds = (nowMs - sceneStartedAtMs) / 1000.0f;
    std::array<RgbColor, 4> colors{};
    if (activeScene == static_cast<int8_t>(Scene::AmberChase)) {
        const float position = fmodf(seconds * 1.65f, 4.0f);
        for (size_t index = 0; index < colors.size(); ++index) {
            const float direct = fabsf(static_cast<float>(index) - position);
            const float distance = fminf(direct, 4.0f - direct);
            const float intensity = fmaxf(0.08f, 1.0f - distance);
            colors[index] = {
                static_cast<uint8_t>(255.0f * intensity),
                static_cast<uint8_t>(120.0f * intensity),
                static_cast<uint8_t>(4.0f * intensity),
            };
        }
    } else if (activeScene == static_cast<int8_t>(Scene::DemonPulse)) {
        const float wave = (sinf(seconds * (2.0f * PI / 2.8f)) + 1.0f) / 2.0f;
        const uint8_t red = static_cast<uint8_t>((0.25f + wave * 0.75f) * 255.0f);
        colors.fill({red, 5, 10});
    } else {
        for (size_t index = 0; index < colors.size(); ++index) {
            colors[index] = hsvToRgb(seconds * 62.0f + index * 52.0f, 0.82f, 1.0f);
        }
    }
    return colors;
}

void renderOutputs(const uint32_t nowMs) {
    if (vehicleAutomationEnabled && vehicleSignalInput.isActive()) {
        applyAllRings({forcedWhiteBrightness, forcedWhiteBrightness, forcedWhiteBrightness});
    } else if (!userEnabled) {
        applyAllRings({0, 0, 0});
    } else if (activeScene != kNoScene) {
        applyRenderedColors(sceneColors(nowMs));
    } else {
        applyRenderedColors(ringColors);
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
    state += (vehicleAutomationEnabled && vehicleSignalInput.isActive()) ? '1' : '0';
    state += '|';
    state += vehicleSignalInput.isActive() ? '1' : '0';
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
    sceneStartedAtMs = millis();
    if (scene != kNoScene) userEnabled = true;
}

void applyFavoriteToRings(const uint8_t index) {
    if (favoriteCount == 0) return;
    favoriteColorIndex = index % favoriteCount;
    ringColors.fill(favoriteColors[favoriteColorIndex]);
    selectScene(kNoScene);
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
        selectScene(scene >= 0 && scene <= 2 ? static_cast<int8_t>(scene) : kNoScene);
        scheduleLightStatePersistence();
    } else if (command == "FAVORITES") {
        setFavoritesFromCommand(fieldAt(message, 1));
    } else if (command == "VEHICLE") {
        vehicleAutomationEnabled = fieldAt(message, 1).toInt() != 0;
        preferences.putBool("veh-auto", vehicleAutomationEnabled);
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
    forcedWhiteBrightness = preferences.getUChar("white", 255);
    globalBrightness = preferences.getUChar("brightness", 224);
    activeScene = preferences.getChar("scene", kNoScene);
    if (activeScene < kNoScene || activeScene > 2) activeScene = kNoScene;

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
    if (!userEnabled) {
        userEnabled = true;
    } else if (favoriteCount > 0) {
        applyFavoriteToRings((favoriteColorIndex + 1) % favoriteCount);
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

    if (vehicleSignalInput.update(nowMs)) {
        Serial.printf("Vehicle signal %s\n", vehicleSignalInput.isActive() ? "active" : "inactive");
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
    vehicleSignalInput.begin();
    loadConfiguration();
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
