package dev.helm.sdk

data class DoorState(
    val driverOpen: Boolean = false,
    val passengerOpen: Boolean = false,
    val rearLeftOpen: Boolean = false,
    val rearRightOpen: Boolean = false,
    val trunkOpen: Boolean = false,
) {
    val anyOpen: Boolean get() = driverOpen || passengerOpen || rearLeftOpen || rearRightOpen || trunkOpen
}
