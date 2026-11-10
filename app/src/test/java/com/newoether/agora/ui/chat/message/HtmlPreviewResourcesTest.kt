package com.newoether.agora.ui.chat.message

import android.app.Application
import android.webkit.WebResourceResponse
import com.newoether.agora.model.ToolImageAttachment
import java.io.File
import java.util.Base64
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class HtmlPreviewResourcesTest {
    @get:Rule val temporary = TemporaryFolder()
    private val document = HtmlPreviewDocument("<p>Generated content</p>")

    @Test
    fun servesGeneratedDocumentLocallyAndStopsAfterClose() {
        resources().use { loader ->
            val wrapper = loader.response(document.rootUrl, "GET", true)
            val frame = loader.response(document.frameUrl, "GET", false)
            assertEquals(200, wrapper.statusCode)
            assertEquals(200, frame.statusCode)
            assertEquals(document.wrapperPolicy, wrapper.responseHeaders["Content-Security-Policy"])
            assertEquals(document.contentPolicy, frame.responseHeaders["Content-Security-Policy"])
            assertTrue(wrapper.readText().contains("sandbox=\"allow-scripts\""))
            assertTrue(frame.readText().contains("<p>Generated content</p>"))
            loader.close()
            assertBlocked(loader.response(document.rootUrl, "GET", true))
        }
    }

    @Test
    fun externalUrlsAndInvalidRoutesAreBlocked() {
        resources().use { loader ->
            listOf(
                Triple("https://example.com/image.png", "GET", false),
                Triple("https://cdn.jsdelivr.net/pkg.js", "GET", false),
                Triple(document.rootUrl, "GET", false),
                Triple(document.frameUrl, "GET", true),
                Triple(document.frameUrl, "POST", false),
                Triple("${document.origin}/unknown", "GET", false),
            ).forEach { (url, method, mainFrame) ->
                assertBlocked(loader.response(url, method, mainFrame))
            }
        }
    }

    @Test
    fun servesOnlyAttachedLocalRasterImagesAndClosesStreams() {
        val file = temporary.newFile("pixel.png")
        val png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScLbtAAAAABJRU5ErkJggg==")
        file.writeBytes(png)
        val url = "${document.origin}/image/0"
        resources(mapOf(url to attachment(file))).use { loader ->
            val result = loader.response(url, "GET", false)
            assertEquals(200, result.statusCode)
            assertEquals("image/png", result.mimeType)
            assertNull(result.responseHeaders["Access-Control-Allow-Origin"])
            assertArrayEquals(png, result.data.use { it.readBytes() })
            val pending = loader.response(url, "GET", false).data
            assertTrue(pending.read() >= 0)
            loader.close()
            assertEquals(-1, pending.read())
        }
    }

    @Test
    fun rejectsSvgMissingAndCrossPreviewImages() {
        val url = "${document.origin}/image/0"
        val svg = temporary.newFile("vector.svg").apply { writeText("<svg/>") }
        val missing = File(temporary.root, "missing.png")
        listOf(attachment(svg, "image/svg+xml"), attachment(missing)).forEach { image ->
            resources(mapOf(url to image)).use { assertBlocked(it.response(url, "GET", false)) }
        }
        val local = temporary.newFile("image.png").apply { writeBytes(byteArrayOf(1)) }
        resources(mapOf(url to attachment(local))).use { loader ->
            assertBlocked(loader.response("${HtmlPreviewDocument("").origin}/image/0", "GET", false))
            assertBlocked(loader.response("$url?path=other.png", "GET", false))
        }
    }

    @Test
    fun capsLocalRequestsAndAggregateImageBytes() {
        resources().use { loader ->
            repeat(HtmlPreviewResources.MAX_REQUESTS) {
                assertEquals(200, loader.response(document.frameUrl, "GET", false).statusCode)
            }
            assertBlocked(loader.response(document.frameUrl, "GET", false))
        }
    }

    private fun resources(images: Map<String, ToolImageAttachment> = emptyMap()) =
        HtmlPreviewResources(document, images, "#000000", "#FFFFFF")

    private fun attachment(file: File, mime: String = "image/png") =
        ToolImageAttachment(file.absolutePath, mime, file.length())

    private fun assertBlocked(response: WebResourceResponse) {
        assertEquals(403, response.statusCode)
        assertEquals(0, response.data.use { it.readBytes().size })
    }

    private fun WebResourceResponse.readText() = data.bufferedReader(Charsets.UTF_8).use { it.readText() }
}
