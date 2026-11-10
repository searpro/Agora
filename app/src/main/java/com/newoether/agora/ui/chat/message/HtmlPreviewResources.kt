package com.newoether.agora.ui.chat.message

import android.webkit.WebResourceResponse
import com.newoether.agora.model.ToolImageAttachment
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Serves generated HTML and explicitly attached local images; never opens network connections. */
internal class HtmlPreviewResources(
    private val document: HtmlPreviewDocument,
    private val images: Map<String, ToolImageAttachment>,
    private val background: String,
    private val foreground: String,
) : Closeable {
    private val closed = AtomicBoolean()
    private val consumed = AtomicInteger()
    private val attempts = AtomicInteger()
    private val streams = ConcurrentHashMap.newKeySet<InputStream>()

    fun response(url: String, method: String, mainFrame: Boolean): WebResourceResponse {
        if (closed.get() || method != "GET" || attempts.incrementAndGet() > MAX_REQUESTS) return blocked()
        if (url == document.rootUrl && mainFrame) return html(document.wrapper(background), document.wrapperPolicy)
        if (mainFrame) return blocked()
        if (url == document.frameUrl) return html(document.frame(background, foreground), document.contentPolicy)
        return images[url]?.let(::image) ?: blocked()
    }

    private fun image(image: ToolImageAttachment): WebResourceResponse = try {
        val file = File(image.path)
        val length = file.length()
        if (image.mimeType !in RASTER_TYPES || !file.isFile || length <= 0 ||
            length > HtmlPreviewDocument.MAX_TOTAL_BYTES || !reserve(length.toInt())) {
            blocked()
        } else {
            val stream = object : FilterInputStream(file.inputStream()) {
                private var remaining = length
                override fun read(): Int {
                    if (closed.get() || remaining <= 0) return -1
                    return super.read().also { if (it >= 0) remaining-- }
                }
                override fun read(bytes: ByteArray, offset: Int, count: Int): Int {
                    if (closed.get() || remaining <= 0) return -1
                    return `in`.read(bytes, offset, minOf(count.toLong(), remaining).toInt())
                        .also { if (it > 0) remaining -= it }
                }
                override fun close() {
                    streams.remove(this)
                    super.close()
                }
            }
            streams.add(stream)
            if (closed.get()) {
                stream.close()
                blocked()
            } else {
                WebResourceResponse(image.mimeType, null, 200, "OK", headers(), stream)
            }
        }
    } catch (_: IOException) {
        blocked()
    }

    private fun reserve(bytes: Int): Boolean {
        while (true) {
            val current = consumed.get()
            if (bytes > HtmlPreviewDocument.MAX_TOTAL_BYTES - current) return false
            if (consumed.compareAndSet(current, current + bytes)) return true
        }
    }

    override fun close() {
        closed.set(true)
        streams.forEach { runCatching { it.close() } }
        streams.clear()
    }

    private fun html(text: String, policy: String, connection: String? = document.connectionPolicy) =
        WebResourceResponse(
            "text/html", "UTF-8", 200, "OK",
            headers() + mapOf(
                "Content-Security-Policy" to policy,
                "Permissions-Policy" to "camera=(), microphone=(), geolocation=(), payment=(), usb=(), clipboard-read=(), clipboard-write=()",
            ) + if (connection == null) emptyMap() else mapOf("Connection-Allowlist" to connection),
            ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)),
        )

    companion object {
        const val MAX_REQUESTS = 128
        val RASTER_TYPES = setOf("image/png", "image/jpeg", "image/webp", "image/gif", "image/avif")

        private fun headers() = mapOf(
            "Cache-Control" to "no-store",
            "X-Content-Type-Options" to "nosniff",
            "Referrer-Policy" to "no-referrer",
        )

        fun blocked() = WebResourceResponse(
            "text/plain", "UTF-8", 403, "Blocked", headers(), ByteArrayInputStream(ByteArray(0)),
        )
    }
}
