package com.netumscan.scannersdk.demo

fun escapeControlText(text: String): String {
    val builder = StringBuilder(text.length)
    text.forEach { ch ->
        when {
            ch.code < 0x20 || ch.code == 0x7F -> builder.append("\\x%02X".format(ch.code))
            else -> builder.append(ch)
        }
    }
    return builder.toString()
}
