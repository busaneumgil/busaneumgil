package com.ssafy.e102.eumgil.app.navigation

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class SplashConfigurationTest {
    @Test
    fun `splash image resources use valid Android resource names`() {
        val resourceFiles =
            File("src/main/res")
                .walkTopDown()
                .filter { file -> file.isFile && file.extension == "png" }
                .toList()
        val invalidResourceFiles =
            resourceFiles.filterNot { file ->
                RESOURCE_FILE_NAME.matches(file.name)
            }
        val resourcePaths = resourceFiles.map { file -> file.invariantSeparatorsPath }

        assertTrue(
            "Missing app logo resource.",
            resourcePaths.any { path -> path.endsWith("drawable/app_logo.png") },
        )
        assertTrue(
            "Missing full-screen splash illustration resource.",
            resourcePaths.any { path -> path.endsWith("drawable-nodpi/splash_illustration.png") },
        )
        assertTrue(
            "Missing transparent platform splash icon resource.",
            File("src/main/res/drawable/splash_transparent_icon.xml").exists(),
        )
        assertTrue(
            "Invalid Android resource names: ${invalidResourceFiles.joinToString { it.name }}",
            invalidResourceFiles.isEmpty(),
        )
    }

    @Test
    fun `app module declares AndroidX splash screen dependency`() {
        val buildFile = File("build.gradle.kts").readText()

        assertTrue(
            "AndroidX splash screen dependency must be declared.",
            buildFile.contains("androidx.core:core-splashscreen"),
        )
    }

    @Test
    fun `main activity starts with splash theme`() {
        val manifest = parseXml(File("src/main/AndroidManifest.xml"))
        val application = manifest.documentElement
        val activity =
            manifest
                .getElementsByTagName("activity")
                .asSequence()
                .mapNotNull { node -> node as? Element }
                .single { element ->
                    element.getAttribute("android:name") == ".app.MainActivity"
                }

        assertEquals("@drawable/app_logo", application.getAttribute("android:icon"))
        assertEquals("@drawable/app_logo", application.getAttribute("android:roundIcon"))
        assertEquals("@style/Theme.BusanEumgil.Splash", activity.getAttribute("android:theme"))
    }

    @Test
    fun `splash theme hides platform icon then restores app theme`() {
        val themeStyle = loadStyle(name = "Theme.BusanEumgil.Splash")
        val themeItems = themeStyle.items

        assertEquals("Theme.SplashScreen", themeStyle.parent)
        assertEquals("@color/splash_background", themeItems["windowSplashScreenBackground"])
        assertEquals("@drawable/splash_transparent_icon", themeItems["windowSplashScreenAnimatedIcon"])
        assertEquals("@style/Theme.BusanEumgil", themeItems["postSplashScreenTheme"])
    }

    @Test
    fun `main activity installs platform splash screen before content`() {
        val mainActivity = File("src/main/java/com/ssafy/e102/eumgil/app/MainActivity.kt").readText()
        val installCallIndex = mainActivity.indexOf("installSplashScreen()")
        val setContentIndex = mainActivity.indexOf("setContent")

        assertTrue("MainActivity must call installSplashScreen().", installCallIndex >= 0)
        assertTrue(
            "Splash screen must be installed before Compose content is set.",
            installCallIndex in 0 until setContentIndex,
        )
    }

    @Test
    fun `app entry loading state displays full screen splash illustration`() {
        val appNavHost = File("src/main/java/com/ssafy/e102/eumgil/app/navigation/AppNavHost.kt").readText()

        assertTrue(
            "App entry loading state must display the splash illustration.",
            appNavHost.contains("R.drawable.splash_illustration"),
        )
        assertTrue(
            "Splash illustration must fill the screen without preserving empty bars.",
            appNavHost.contains("ContentScale.Crop"),
        )
    }

    private fun loadStyle(name: String): StyleDefinition {
        val document = parseXml(File("src/main/res/values/themes.xml"))
        val style =
            document
                .getElementsByTagName("style")
                .asSequence()
                .mapNotNull { node -> node as? Element }
                .single { element -> element.getAttribute("name") == name }
        val items =
            style
                .getElementsByTagName("item")
                .asSequence()
                .mapNotNull { node -> node as? Element }
                .associate { element ->
                    element.getAttribute("name") to element.textContent
                }

        return StyleDefinition(
            parent = style.getAttribute("parent"),
            items = items,
        )
    }

    private fun parseXml(file: File) =
        DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(file)

    private fun org.w3c.dom.NodeList.asSequence(): Sequence<org.w3c.dom.Node> =
        sequence {
            for (index in 0 until length) {
                yield(item(index))
            }
        }

    private data class StyleDefinition(
        val parent: String,
        val items: Map<String, String>,
    )

    private companion object {
        val RESOURCE_FILE_NAME = Regex("[a-z0-9_]+\\.png")
    }
}
