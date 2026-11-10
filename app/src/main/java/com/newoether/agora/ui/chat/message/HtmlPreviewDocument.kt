package com.newoether.agora.ui.chat.message

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.UUID

/** Per-card documents and explicit image capabilities; never a general filesystem URL router. */
internal class HtmlPreviewDocument(source: String) {
    val origin = "https://${UUID.randomUUID()}.preview.agora.invalid"
    val rootUrl = "$origin/index.html"
    val frameUrl = "$origin/frame.html"
    val images: Map<String, String>
    val html: String

    init {
        require(source.length <= MAX_SOURCE_CHARS) { "HTML preview is too large" }
        val references = linkedMapOf<String, String>()
        html = rewriteHtmlPreviewImages(source) { link ->
            val existing = references.entries.firstOrNull { it.value == link }?.key
            when {
                existing != null -> existing
                parseShellMediaLink(link) == null || references.size >= MAX_IMAGES -> null
                else -> "$origin/image/${references.size}".also { references[it] = link }
            }
        }
        images = references.toMap()
    }

    val contentPolicy: String = "default-src 'none'; " +
        "script-src 'unsafe-inline'; style-src 'unsafe-inline'; " +
        "img-src data: blob: $origin/image/; font-src data; " +
        "connect-src 'none'; frame-src 'none'; worker-src 'none'; object-src 'none'; " +
        "base-uri 'none'; form-action 'none'; sandbox allow-scripts"

    val wrapperPolicy: String = "default-src 'none'; script-src 'unsafe-inline'; " +
        "style-src 'unsafe-inline'; frame-src $frameUrl; base-uri 'none'; form-action 'none'"

    val connectionPolicy: String = "webrtc=block"

    fun frame(background: String, foreground: String): String = """
        <!doctype html><html><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="referrer" content="no-referrer">
        <style>html,body{margin:0;background:$background;color:$foreground;font-family:system-ui,sans-serif}body{padding:12px;box-sizing:border-box}img,canvas,svg{max-width:100%}</style>
        </head><body>$html</body></html>
    """.trimIndent()

    fun wrapper(background: String): String = """
        <!doctype html><html><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="referrer" content="no-referrer">
        <style>html,body{margin:0;width:100%;height:100%;background:$background}iframe{display:block;width:100%;height:100%;border:0}</style>
        </head><body><iframe sandbox="allow-scripts" referrerpolicy="no-referrer" src="$frameUrl"></iframe></body></html>
    """.trimIndent()

    companion object {
        const val MAX_SOURCE_CHARS = 512 * 1024
        const val MAX_IMAGES = 16
        const val MAX_RESOURCE_BYTES = 8 * 1024 * 1024
        const val MAX_TOTAL_BYTES = 32 * 1024 * 1024
        val LIBRARY_ORIGINS = emptyList<String>()
    }
}

/** Only static library suffixes and numeric OSM tile paths, never arbitrary URLs or credentials. */
internal fun allowedHtmlResource(url: String): Boolean {
    return false
}

internal fun htmlPreviewLanguage(language: String?): Boolean =
    language?.lowercase() in setOf("html", "html-preview")
