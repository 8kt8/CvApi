package com.katlewski.cv.ui

import com.katlewski.cv.data.Period

private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** "2015-06" -> "Jun 2015", "2016" -> "2016". */
internal fun formatYearMonth(value: String): String {
    val parts = value.split("-")
    val month = parts.getOrNull(1)?.toIntOrNull()?.let { months.getOrNull(it - 1) }
    return if (month != null) "$month ${parts[0]}" else parts[0]
}

internal val Period.isCurrent: Boolean get() = end == null

internal fun Period.format(): String = "${formatYearMonth(start)} – ${end?.let(::formatYearMonth) ?: "Present"}"

/** Falls back to a favicon of the company site when no explicit logo is set. */
internal fun logoFor(logoUrl: String?, siteUrl: String?): String? {
    if (!logoUrl.isNullOrBlank()) return logoUrl
    val host = siteUrl?.substringAfter("://")?.substringBefore("/")?.takeIf { it.isNotBlank() } ?: return null
    return "https://www.google.com/s2/favicons?domain=$host&sz=128"
}
