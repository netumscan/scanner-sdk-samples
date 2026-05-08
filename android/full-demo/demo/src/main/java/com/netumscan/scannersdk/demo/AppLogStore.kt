package com.netumscan.scannersdk.demo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppLogStore {
    private const val MAX_EVENTS = 400

    private val _events = MutableStateFlow<List<DebugEvent>>(emptyList())
    val events: StateFlow<List<DebugEvent>> = _events.asStateFlow()

    fun append(event: DebugEvent) {
        _events.value = (_events.value + event).takeLast(MAX_EVENTS)
    }

    fun clear() {
        _events.value = emptyList()
    }
}
