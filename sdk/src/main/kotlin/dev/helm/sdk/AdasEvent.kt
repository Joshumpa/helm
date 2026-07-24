package dev.helm.sdk

// Key names match the kp.jar JSON config (car_effect_adas_warning_*)
enum class AdasEvent {
    STOP_GO,
    CLOSE_DISTANCE,
    LANE_DEPARTURE,
    LANE_DEPARTURE_LEFT,
    LANE_DEPARTURE_RIGHT,
}
