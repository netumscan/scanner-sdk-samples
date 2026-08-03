package com.netumscan.scannersdk.demo

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppLogStoreTest {
    @AfterTest
    fun clearStore() {
        AppLogStore.clear()
    }

    @Test
    fun append_keepsLatestFourHundredEventsAcrossSources() {
        repeat(405) { index ->
            AppLogStore.append(
                DebugEvent(
                    source = DebugEventSource.entries[index % DebugEventSource.entries.size],
                    level = DebugEventLevel.Info,
                    message = "event-$index",
                    timestampMs = index.toLong(),
                )
            )
        }

        val events = AppLogStore.events.value
        assertEquals(400, events.size)
        assertEquals("event-5", events.first().message)
        assertEquals("event-404", events.last().message)
    }

    @Test
    fun append_redactsDeviceIdentifiersBeforeStorage() {
        AppLogStore.append(
            DebugEvent(
                source = DebugEventSource.SESSION,
                level = DebugEventLevel.Info,
                message = "deviceId=AA:BB:CC:DD:EE:FF serial=SN123456 " +
                    "AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE",
            )
        )

        val stored = AppLogStore.events.value.single().message
        assertFalse(stored.contains("AA:BB:CC:DD:EE:FF"))
        assertFalse(stored.contains("SN123456"))
        assertFalse(stored.contains("AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE"))
        assertTrue(stored.contains("<redacted>"))
    }
}
