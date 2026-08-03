package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.ScannerException
import com.netumscan.scannersdk.model.DiscoveryFailure

internal object DemoErrorFormatter {
    fun detail(error: Throwable): String {
        return when (error) {
            is ScannerException -> {
                val nativeError = DemoStrings.format(
                    R.string.sdk_native_error_detail,
                    nativeErrorReason(error.errorCode),
                    error.operation,
                    error.errorCode
                )
                error.discoveryFailure?.let { failure ->
                    "$nativeError; ${discoveryFailureDetail(failure)}"
                } ?: nativeError
            }
            else -> error.message ?: error::class.java.simpleName
        }
    }

    private fun discoveryFailureDetail(failure: DiscoveryFailure): String = buildString {
        append("discovery=")
        append(failure.code.name)
        append(" message=")
        append(failure.message)
        failure.bleScanIssue?.let { issue ->
            append(" issue=")
            append(issue.name)
        }
        failure.platformErrorCode?.let { code ->
            append(" raw=")
            append(code)
        }
        append(" recoverable=")
        append(failure.recoverable)
    }

    fun nativeErrorReason(errorCode: Int): String {
        return when (errorCode) {
            0 -> DemoStrings.text(R.string.sdk_error_success)
            1 -> DemoStrings.text(R.string.sdk_error_invalid_argument)
            2 -> DemoStrings.text(R.string.sdk_error_not_initialized)
            3 -> DemoStrings.text(R.string.sdk_error_not_supported)
            4 -> DemoStrings.text(R.string.sdk_error_busy)
            5 -> DemoStrings.text(R.string.sdk_error_timeout)
            6 -> DemoStrings.text(R.string.sdk_error_transport_open_failed)
            7 -> DemoStrings.text(R.string.sdk_error_transport_write_failed)
            8 -> DemoStrings.text(R.string.sdk_error_discovery_failed)
            9 -> DemoStrings.text(R.string.sdk_error_connect_failed)
            10 -> DemoStrings.text(R.string.sdk_error_disconnect_failed)
            11 -> DemoStrings.text(R.string.sdk_error_protocol_error)
            12 -> DemoStrings.text(R.string.sdk_error_device_not_ready)
            else -> DemoStrings.text(R.string.sdk_error_internal)
        }
    }
}

internal fun Throwable.demoErrorDetail(): String = DemoErrorFormatter.detail(this)
