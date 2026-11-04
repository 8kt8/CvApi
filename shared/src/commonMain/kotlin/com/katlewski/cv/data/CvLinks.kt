package com.katlewski.cv.data

/** Where real CVs are served from. The JSON lives on a private static host, not in this repo. */
object CvLinks {
    const val DEFAULT_BASE_URL = "https://mycv-szk.pages.dev"
    const val HOST = "mycv-szk.pages.dev"
}

/**
 * URLs of the static CV files on a host:
 * - `<baseUrl>/default.json` - the public default CV
 * - `<baseUrl>/v/<code>.json` - a per-company variant
 */
class CvSource(baseUrl: String) {
    private val base = baseUrl.trim().trimEnd('/')

    fun defaultUrl(): String = "$base/default.json"

    /** @throws IllegalArgumentException when [code] is not a valid [VariantCode]. */
    fun variantUrl(code: String): String {
        val valid = requireNotNull(VariantCode.parse(code)) { "Invalid variant code" }
        return "$base/v/$valid.json"
    }
}

/** A variant code is exactly [LENGTH] characters `[A-Za-z0-9]`. */
object VariantCode {
    const val LENGTH = 12

    /** The code when [raw] (surrounding whitespace ignored) is a valid code, otherwise null. */
    fun parse(raw: String?): String? {
        val code = raw?.trim() ?: return null
        if (code.length != LENGTH) return null
        return code.takeIf { it.all(::isCodeChar) }
    }

    /**
     * Code from a share link, or null when the link has no valid code. Accepts
     * `https://<host>/c/<code>` (optionally with a trailing slash, query or fragment),
     * `...?v=<code>` / `...&v=<code>`, and a bare code.
     */
    fun fromLink(link: String?): String? {
        val text = link?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        parse(text)?.let { return it }

        val withoutFragment = text.substringBefore('#')
        val path = withoutFragment.substringBefore('?')
        val query = withoutFragment.substringAfter('?', missingDelimiterValue = "")

        val segments = path.substringAfter("://", path).split('/')
        // segments[0] is the host (or empty for a relative "/c/..." link).
        val cIndex = segments.drop(1).indexOf("c").takeIf { it >= 0 }?.plus(1)
        if (cIndex != null && segments.drop(cIndex + 2).all { it.isEmpty() }) {
            parse(segments.getOrNull(cIndex + 1))?.let { return it }
        }

        return query.split('&')
            .firstOrNull { it.substringBefore('=') == "v" }
            ?.substringAfter('=', missingDelimiterValue = "")
            ?.let(::parse)
    }

    private fun isCodeChar(c: Char): Boolean = c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9'
}

/** Which CV a [CvService] shows. */
sealed interface CvMode {
    /** The public default CV: bundled copy first, then `default.json`. */
    data object Default : CvMode

    /** A per-company variant: cached copy (if any) first, then `v/<code>.json`. Never falls back to the default CV. */
    data class Variant(val code: String) : CvMode
}
