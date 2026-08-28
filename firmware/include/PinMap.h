#pragma once

#include <Arduino.h>
#include <array>

namespace ringcontroller::pins {

struct RgbPins {
    uint8_t red;
    uint8_t green;
    uint8_t blue;
};

constexpr std::array<RgbPins, 4> kRings{{
    {25, 26, 27},
    {32, 33, 4},
    {13, 14, 16},
    {17, 18, 19},
}};

constexpr RgbPins kCabinIndicator{21, 22, 23};

constexpr uint8_t kMomentaryButton = 34;
constexpr uint8_t kVehicleSignal = 35;
constexpr uint8_t kDaylightSignal = 36;

constexpr std::array<uint8_t, 15> kPwmOutputs{{
    25, 26, 27,
    32, 33, 4,
    13, 14, 16,
    17, 18, 19,
    21, 22, 23,
}};

}  // namespace ringcontroller::pins
