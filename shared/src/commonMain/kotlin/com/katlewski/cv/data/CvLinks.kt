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
     * Code from a share link, or null when the link has no valid code. Accepts:
     * - a bare code;
     * - a link whose path is exactly `/c/<code>` (optional trailing slash; query and fragment ignored).
     *   When the path starts with `/c/`, only that segment counts - `?v=` is not consulted;
     * - a link whose path is `/` or empty, with the code in `?v=<code>` / `&v=<code>`.
     *
     * Anything else (`/a/c/<code>`, `/C/<code>`, `/c/<code>/extra`, ...) returns null.
     * The host is not checked.
     */
    fun fromLink(link: String?): String? {
        val text = link?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        parse(text)?.let { return it }

        val withoutFragment = text.substringBefore('#')
        val beforeQuery = withoutFragment.substringBefore('?')
        val query = withoutFragment.substringAfter('?', missingDelimiterValue = "")

        // "host/path", "host", or a relative "/path".
        val afterScheme = beforeQuery.substringAfter("://", beforeQuery)
        val path = if (afterScheme.startsWith('/')) afterScheme else "/" + afterScheme.substringAfter('/', missingDelimiterValue = "")

        if (path.startsWith("/c/")) {
            val segment = path.removePrefix("/c/").removeSuffix("/")
            return if ('/' in segment) null else exact(segment)
        }
        if (path != "/") return null

        return query.split('&')
            .firstOrNull { it.substringBefore('=') == "v" }
            ?.substringAfter('=', missingDelimiterValue = "")
            ?.let(::exact)
    }

    /** Like [parse] but without trimming: [raw] itself must be the code. */
    private fun exact(raw: String): String? = raw.takeIf { it.length == LENGTH && it.all(::isCodeChar) }

    private fun isCodeChar(c: Char): Boolean = c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9'
}

/** Which CV a [CvService] shows. */
sealed interface CvMode {
    /** The public default CV: bundled copy first, then `default.json`. */
    data object Default : CvMode

    /** A per-company variant: cached copy (if any) first, then `v/<code>.json`. Never falls back to the default CV. */
    data class Variant(val code: String) : CvMode
}
