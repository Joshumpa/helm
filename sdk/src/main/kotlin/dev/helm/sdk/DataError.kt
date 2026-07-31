package dev.helm.sdk

sealed class DataError {
    data class Unavailable(val cause: Throwable? = null) : DataError()
    data class ParseFailure(val code: Int, val cause: Throwable? = null) : DataError()
    data class WriteFailure(val cause: Throwable? = null) : DataError()
}
