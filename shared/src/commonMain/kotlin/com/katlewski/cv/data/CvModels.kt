package com.katlewski.cv.data

import kotlinx.serialization.Serializable

@Serializable
data class Cv(
    val profile: Profile,
    val contacts: List<ContactLink> = emptyList(),
    val experience: List<Experience> = emptyList(),
    val education: List<Education> = emptyList(),
    val skills: List<String> = emptyList(),
)

@Serializable
data class Profile(
    val fullName: String,
    val headline: String,
    val about: String,
    val photoUrl: String? = null,
    val location: String? = null,
)

@Serializable
enum class ContactType { GitHub, LinkedIn, Email, Phone, Website }

@Serializable
data class ContactLink(
    val type: ContactType,
    val label: String,
    val url: String,
)

@Serializable
data class Experience(
    val companyName: String,
    val role: String,
    val period: Period,
    val companyUrl: String? = null,
    /** Direct logo URL. When null, a logo is derived from [companyUrl]. */
    val logoUrl: String? = null,
    val location: String? = null,
    val summary: String? = null,
    val highlights: List<String> = emptyList(),
    val techStack: List<String> = emptyList(),
    val apps: List<AppLink> = emptyList(),
) {
    /** [logoUrl], or the favicon of [companyUrl] when no logo is set. */
    val resolvedLogoUrl: String? get() = resolveLogoUrl(logoUrl, companyUrl)
}

/** Dates as "YYYY-MM" or "YYYY". A null [end] means the role is current. */
@Serializable
data class Period(val start: String, val end: String? = null) {
    val isCurrent: Boolean get() = end == null

    /** "Jun 2015 – Sep 2017", "Apr 2018 – Present". */
    val displayText: String get() = "${formatYearMonth(start)} – ${end?.let(::formatYearMonth) ?: "Present"}"
}

@Serializable
enum class Store { GooglePlay, AppStore, Web }

@Serializable
data class AppLink(
    val name: String,
    val store: Store,
    val url: String,
)

@Serializable
data class Education(
    val schoolName: String,
    val degree: String,
    val fieldOfStudy: String,
    val period: Period,
    val url: String? = null,
    val logoUrl: String? = null,
) {
    val resolvedLogoUrl: String? get() = resolveLogoUrl(logoUrl, url)
}

private val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** "2015-06" -> "Jun 2015", "2016" -> "2016". */
internal fun formatYearMonth(value: String): String {
    val parts = value.split("-")
    val month = parts.getOrNull(1)?.toIntOrNull()?.let { monthNames.getOrNull(it - 1) }
    return if (month != null) "$month ${parts[0]}" else parts[0]
}

private fun resolveLogoUrl(logoUrl: String?, siteUrl: String?): String? {
    if (!logoUrl.isNullOrBlank()) return logoUrl
    val host = siteUrl?.substringAfter("://")?.substringBefore("/")?.takeIf { it.isNotBlank() } ?: return null
    return "https://www.google.com/s2/favicons?domain=$host&sz=128"
}
