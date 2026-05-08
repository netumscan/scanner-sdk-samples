package com.netumscan.scannersdk.demo

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class DemoLocalizationArchitectureTest {
    @Test
    fun raw_display_text_entrypoint_is_explicitly_named() {
        val sources = mainKotlinSources()

        assertNoSourceContains(
            files = sources,
            forbidden = "raw" + "Text(",
            guidance = "Use rawDisplayText(...) for device names, serial numbers, raw responses, and cached command text.",
        )
        assertNoSourceContains(
            files = sources,
            forbidden = "::raw" + "Text",
            guidance = "Use ::rawDisplayText for raw UiText function references.",
        )
    }

    @Test
    fun raw_report_entrypoints_are_explicitly_named() {
        val sources = mainKotlinSources()

        assertNoSourceContains(
            files = sources,
            forbidden = "fun report" + "Action(message: String)",
            guidance = "Use reportAction(provider) for localized text or reportRawAction(message) for raw display text.",
        )
        assertNoSourceContains(
            files = sources,
            forbidden = "fun report" + "Error(prefix: String, error: Throwable)",
            guidance = "Use reportError(prefixProvider, error) for localized text or reportRawError(prefix, error) for raw display text.",
        )
    }

    @Test
    fun localized_ui_state_boundaries_keep_ui_text() {
        val sources = mainKotlinSources()

        assertNoSourceContains(
            files = sources,
            forbidden = "closeDiscoverySession(\n        status: String",
            guidance = "closeDiscoverySession must keep status as UiText so language changes can re-resolve it.",
        )
        assertNoSourceContains(
            files = sources,
            forbidden = "handleSessionClosed(status: String",
            guidance = "handleSessionClosed must keep status as UiText so language changes can re-resolve it.",
        )
        assertNoSourceContains(
            files = sources,
            forbidden = "val info" + "Summary: String",
            guidance = "Device summary UI fields must stay UiText instead of freezing localized labels into raw strings.",
        )
        assertNoSourceContains(
            files = sources,
            forbidden = "val deviceCharset" + "Summary: String",
            guidance = "Device summary UI fields must stay UiText instead of freezing localized labels into raw strings.",
        )
        assertNoSourceContains(
            files = sources,
            forbidden = "val deviceTerminal" + "Summary: String",
            guidance = "Device summary UI fields must stay UiText instead of freezing localized labels into raw strings.",
        )
    }

    @Test
    fun command_groups_do_not_use_localized_titles_as_keys() {
        val sources = mainKotlinSources()

        assertNoSourceContains(
            files = sources,
            forbidden = "val key: String = " + "title",
            guidance = "CommandGroup keys must be explicit stable IDs, not localized titles.",
        )
        assertNoSourceContains(
            files = sources,
            forbidden = "key: String = " + "title",
            guidance = "CommandGroup helper defaults must not fall back to localized titles.",
        )
    }

    private fun assertNoSourceContains(
        files: List<File>,
        forbidden: String,
        guidance: String,
    ) {
        val offenders = files
            .filter { file -> file.readText().contains(forbidden) }
            .map { file -> file.relativeTo(projectRoot()).path }

        assertTrue(
            actual = offenders.isEmpty(),
            message = "$guidance Offenders: ${offenders.joinToString()}",
        )
    }

    private fun mainKotlinSources(): List<File> {
        val directory = sourceCandidates("demo/src/main/java").firstOrNull { it.isDirectory }
            ?: error("Demo main source directory not found")
        return directory
            .walkTopDown()
            .filter { file -> file.isFile && file.extension == "kt" }
            .toList()
    }

    private fun sourceCandidates(relativePath: String): List<File> {
        val root = projectRoot()
        return listOf(
            File(root, relativePath),
            File(root, "wrappers/android-kotlin/$relativePath"),
            File(root.parentFile ?: root, relativePath),
            File(root.parentFile ?: root, "wrappers/android-kotlin/$relativePath"),
        )
    }

    private fun projectRoot(): File = File(System.getProperty("user.dir").orEmpty())
}
