package com.netumscan.scannersdk.demo

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class TestDemoStringFallbackProvider : DemoStringFallbackProvider {
    private val fallbackStrings: Map<DemoLanguage, Map<String, String>> by lazy {
        mapOf(
            DemoLanguage.EN to loadFallbackStrings("demo/src/main/res/values"),
            DemoLanguage.ZH to loadFallbackStrings("demo/src/main/res/values-zh-rCN"),
        )
    }

    override fun string(language: DemoLanguage, name: String): String? {
        return fallbackStrings[language]?.get(name)
    }

    private fun loadFallbackStrings(relativePath: String): Map<String, String> {
        val directory = fallbackResourceCandidates(relativePath).firstOrNull { it.isDirectory } ?: return emptyMap()
        val files = directory
            .listFiles { file -> file.isFile && file.extension == "xml" }
            .orEmpty()
            .sortedBy { it.name }
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            isExpandEntityReferences = false
        }
        return files.fold(emptyMap()) { accumulated, file ->
            val strings = runCatching {
                val document = factory.newDocumentBuilder().parse(file)
                val nodes = document.getElementsByTagName("string")
                buildMap {
                    for (index in 0 until nodes.length) {
                        val node = nodes.item(index)
                        val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
                        put(name, node.textContent.orEmpty())
                    }
                }
            }.getOrDefault(emptyMap())
            buildMap {
                putAll(accumulated)
                putAll(strings)
            }
        }
    }

    private fun fallbackResourceCandidates(relativePath: String): List<File> {
        val userDir = File(System.getProperty("user.dir").orEmpty())
        return listOf(
            File(userDir, relativePath),
            File(userDir, "wrappers/android-kotlin/$relativePath"),
            File(userDir.parentFile ?: userDir, relativePath),
            File(userDir.parentFile ?: userDir, "wrappers/android-kotlin/$relativePath"),
        )
    }
}
