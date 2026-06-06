package com.netumscan.scannersdk.demo

import com.netumscan.scannersdk.model.ModuleFamily
import com.netumscan.scannersdk.model.ModuleParameterCatalog
import com.netumscan.scannersdk.model.Ntc06hSettingCatalog
import com.netumscan.scannersdk.model.enumOptions
import com.netumscan.scannersdk.model.localizedTitleLabel
import com.netumscan.scannersdk.model.numericInputSpec
import com.netumscan.scannersdk.model.quickValueOptions
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertTrue

class DemoLocalizationResourceTest {
    @Test
    fun sdk_label_resources_are_present_in_english_and_chinese() {
        val englishKeys = loadStringNames("demo/src/main/res/values")
            .filter { it.startsWith("nsdk_") }
            .toSet()
        val chineseKeys = loadStringNames("demo/src/main/res/values-zh-rCN")
            .filter { it.startsWith("nsdk_") }
            .toSet()

        assertTrue(englishKeys.isNotEmpty(), "Expected English nsdk_* resources")
        assertTrue(chineseKeys.isNotEmpty(), "Expected Chinese nsdk_* resources")
        assertTrue(
            actual = chineseKeys.containsAll(englishKeys),
            message = "Missing Chinese nsdk_* resources: ${(englishKeys - chineseKeys).sorted().joinToString()}",
        )
        assertTrue(
            actual = englishKeys.containsAll(chineseKeys),
            message = "Missing English nsdk_* resources: ${(chineseKeys - englishKeys).sorted().joinToString()}",
        )
    }

    @Test
    fun sdk_exposed_localization_keys_have_demo_resources() {
        val englishResources = loadStringNames("demo/src/main/res/values")
        val sdkResources = sdkLocalizationKeys()
            .map { it.toResourceName() }
            .toSet()

        assertTrue(sdkResources.isNotEmpty(), "Expected SDK localization keys")

        val missing = (sdkResources - englishResources).sorted()
        assertTrue(
            actual = missing.isEmpty(),
            message = "Missing demo resources for SDK localization keys: ${missing.joinToString()}",
        )
    }

    private fun sdkLocalizationKeys(): Set<String> = buildSet {
        addAll(sdkLiteralLocalizationKeys())
        addAll(dynamicModuleLocalizationKeys())
        addAll(Ntc06hSettingCatalog.all.map { it.localizedTitleLabel().localizationKey })
    }

    private fun dynamicModuleLocalizationKeys(): Set<String> = buildSet {
        ModuleFamily.values()
            .flatMap { family -> ModuleParameterCatalog.allFor(family) }
            .forEach { definition ->
                add(definition.localizedTitleLabel().localizationKey)
                definition.quickValueOptions().forEach { option ->
                    add(option.localizationKey)
                }
                definition.enumOptions().forEach { option ->
                    add(option.localizationKey)
                }
                definition.numericInputSpec()?.let { spec ->
                    add(spec.localizationKey)
                    add(spec.stepHintKey)
                }
            }
    }

    private fun sdkLiteralLocalizationKeys(): Set<String> {
        val directory = resourceCandidates("sdk/src/main/java").firstOrNull { it.isDirectory }
            ?: return emptySet()
        val regex = Regex("\"(nsdk\\.[a-z0-9_.-]+)\"")
        return directory
            .walkTopDown()
            .filter { file -> file.isFile && file.extension == "kt" }
            .flatMap { file -> regex.findAll(file.readText()).map { it.groupValues[1] } }
            .toSet()
    }

    private fun loadStringNames(relativePath: String): Set<String> {
        val directory = resourceCandidates(relativePath).firstOrNull { it.isDirectory }
            ?: error("Resource directory not found: $relativePath")
        val files = directory
            .listFiles { file -> file.isFile && file.extension == "xml" }
            .orEmpty()
            .sortedBy { it.name }
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            isExpandEntityReferences = false
        }
        return files.flatMapTo(mutableSetOf()) { file ->
            val document = factory.newDocumentBuilder().parse(file)
            val nodes = document.getElementsByTagName("string")
            buildSet {
                for (index in 0 until nodes.length) {
                    val name = nodes.item(index).attributes?.getNamedItem("name")?.nodeValue ?: continue
                    add(name)
                }
            }
        }
    }

    private fun resourceCandidates(relativePath: String): List<File> {
        val userDir = File(System.getProperty("user.dir").orEmpty())
        return listOf(
            File(userDir, relativePath),
            File(userDir, "wrappers/android-kotlin/$relativePath"),
            File(userDir.parentFile ?: userDir, relativePath),
            File(userDir.parentFile ?: userDir, "wrappers/android-kotlin/$relativePath"),
        )
    }

    private fun String.toResourceName(): String =
        replace('.', '_')
            .replace('-', '_')
            .lowercase(Locale.US)
}
