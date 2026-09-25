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
)

/** Dates as "YYYY-MM" or "YYYY". A null [end] means the role is current. */
@Serializable
data class Period(val start: String, val end: String? = null)

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
)
