package com.katlewski.cv.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

interface CvRepository {
    suspend fun getCv(): Cv
}

internal val CvJson = Json { ignoreUnknownKeys = true }

internal fun decodeCv(json: String): Cv = CvJson.decodeFromString<Cv>(json)

/** CV compiled into the app at build time (`-Pcv.bundledJson`, sample data by default) - always available offline. */
class BundledCvRepository : CvRepository {
    private val cv by lazy { decodeCv(BUNDLED_CV_JSON) }

    override suspend fun getCv(): Cv = cv
}

/** CV JSON downloaded from [url], so the CV updates without an app release. */
class RemoteCvRepository(
    private val client: HttpClient,
    private val url: String,
) : CvRepository {

    override suspend fun getCv(): Cv = decodeCv(fetchJson())

    /**
     * Raw response body. Throws [VariantGoneException] on HTTP 404/410, [IllegalStateException]
     * on other HTTP errors and after an 8 s timeout, and the engine's exception when offline.
     */
    suspend fun fetchJson(): String {
        // withTimeoutOrNull so a timeout surfaces as a normal failure, not as cancellation.
        return withTimeoutOrNull(TIMEOUT_MS) {
            val response = client.get(url)
            val status = response.status.value
            if (status == 404 || status == 410) throw VariantGoneException("HTTP $status loading $url")
            check(response.status.isSuccess()) { "HTTP $status loading $url" }
            response.bodyAsText()
        } ?: error("Timed out loading $url")
    }

    companion object {
        internal const val TIMEOUT_MS = 8_000L
    }
}

/**
 * The requested CV no longer exists on the host (HTTP 404 or 410) - e.g. a revoked variant link.
 * [CvService.latestCv] removes the cached copy of a variant before throwing this.
 */
class VariantGoneException(message: String) : Exception(message)

/**
 * Swift-friendly error checks. Swift receives Kotlin exceptions as `NSError` with the original in
 * `userInfo["KotlinException"]`, exposed as `kotlinException`:
 *
 * ```swift
 * } catch {
 *     let kotlinError = (error as NSError).kotlinException as? KotlinThrowable
 *     if CvErrors.shared.isVariantGone(throwable: kotlinError) { /* link revoked: clear UI, show message */ }
 * }
 * ```
 */
object CvErrors {
    /** True when [throwable] is a [VariantGoneException] (null-safe). */
    fun isVariantGone(throwable: Throwable?): Boolean = throwable is VariantGoneException
}

/**
 * Entry point for both apps (Android ViewModel and the SwiftUI view model).
 *
 * - [CvMode.Default]: show [initialCv] (the bundled CV) immediately, then replace it with [latestCv] (`default.json`).
 * - [CvMode.Variant]: show [initialCv] (the cached copy, or null - never the bundled CV), then [latestCv]
 *   (`v/<code>.json`). A failed variant load throws; it never falls back to the default CV.
 *   HTTP 404/410 means the variant was revoked: its cached copy is removed and [VariantGoneException] is thrown.
 *   Any other failure (offline, timeout, 5xx, invalid JSON) throws and keeps the cached copy.
 *
 * Owns its HTTP client - call [close] when done.
 */
class CvService(
    private val client: HttpClient,
    baseUrl: String = CvLinks.DEFAULT_BASE_URL,
    val mode: CvMode = CvMode.Default,
    private val cache: VariantCache = InMemoryVariantCache(),
) : AutoCloseable {

    /** Default CV from the default host, default engine per platform (OkHttp on Android, Darwin on iOS). Swift-friendly. */
    constructor() : this(HttpClient())

    /** Swift-friendly: [CvMode.Variant] when [code] is a valid [VariantCode], otherwise [CvMode.Default]. */
    constructor(baseUrl: String, code: String?) : this(
        client = HttpClient(),
        baseUrl = baseUrl,
        mode = VariantCode.parse(code)?.let { CvMode.Variant(it) } ?: CvMode.Default,
    )

    /** Swift-friendly, with a persistent [cache] (e.g. `defaultIosVariantCache()`). Mode as in `(baseUrl, code)`. */
    constructor(baseUrl: String, code: String?, cache: VariantCache) : this(
        client = HttpClient(),
        baseUrl = baseUrl,
        mode = VariantCode.parse(code)?.let { CvMode.Variant(it) } ?: CvMode.Default,
        cache = cache,
    )

    private val source = CvSource(baseUrl)
    private val bundled = BundledCvRepository()

    // @Throws makes failures reach Swift as thrown errors instead of crashing the app.

    /** What to show before the network answers: bundled CV (Default) or cached variant / null (Variant). */
    @Throws(Exception::class)
    suspend fun initialCv(): Cv? = when (mode) {
        CvMode.Default -> bundled.getCv()
        is CvMode.Variant -> cache.get(mode.code)?.let { json ->
            // A corrupt cache entry is treated as "nothing cached" (SerializationException is an IllegalArgumentException).
            try {
                decodeCv(json)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    /**
     * Downloads the CV for [mode]. A variant is cached on success; on failure this throws.
     * A revoked variant (HTTP 404/410) is removed from the cache and [VariantGoneException] is thrown.
     */
    @Throws(Exception::class)
    suspend fun latestCv(): Cv = when (mode) {
        CvMode.Default -> RemoteCvRepository(client, source.defaultUrl()).getCv()
        is CvMode.Variant -> {
            val json = try {
                RemoteCvRepository(client, source.variantUrl(mode.code)).fetchJson()
            } catch (e: VariantGoneException) {
                cache.remove(mode.code)
                throw e
            }
            val cv = decodeCv(json) // only cache JSON that parses
            cache.put(mode.code, json)
            cv
        }
    }

    /** The CV compiled into the app. Kept for backwards compatibility; prefer [initialCv]. */
    @Throws(Exception::class)
    suspend fun bundledCv(): Cv = bundled.getCv()

    override fun close() = client.close()
}
