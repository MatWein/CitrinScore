package de.citrinscore.resources

import androidx.compose.ui.graphics.vector.PathParser
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertTrue

class DrawableResourcesTest {

    @Test
    fun `all vector drawables have valid path data`() {
        val drawables = File("src/commonMain/composeResources/drawable").listFiles { file -> file.extension == "xml" }.orEmpty()
        assertTrue(drawables.isNotEmpty())
        drawables.forEach { drawable ->
            val paths = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(drawable).getElementsByTagName("path")
            repeat(paths.length) { index ->
                val data = paths.item(index).attributes.getNamedItem("android:pathData").nodeValue
                runCatching { PathParser().parsePathString(data) }.onFailure { throw AssertionError("${drawable.name}: invalid path data", it) }
            }
        }
    }
}
