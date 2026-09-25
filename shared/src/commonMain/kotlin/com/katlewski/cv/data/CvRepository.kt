package com.katlewski.cv.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

interface CvRepository {
    suspend fun getCv(): Cv
}

internal val CvJson = Json { ignoreUnknownKeys = true }

/** CV compiled into the app from cv.json - always available offline. */
class BundledCvRepository : CvRepository {
    private val cv by lazy { CvJson.decodeFromString<Cv>(BUNDLED_CV_JSON) }

    override suspend fun getCv(): Cv = cv
}

/** Latest cv.json from the CvApi GitHub repo, so the CV updates without an app release. */
class RemoteCvRepository(
    private val client: HttpClient,
    private val url: String = DEFAULT_URL,
) : CvRepository {

    // withTimeoutOrNull so a timeout surfaces as a normal failure, not as cancellation.
    override suspend fun getCv(): Cv {
        val body = withTimeoutOrNull(TIMEOUT_MS) {
            val response = client.get(url)
            check(response.status.isSuccess()) { "HTTP ${response.status.value}" }
            response.bodyAsText()
        } ?: error("Timed out loading $url")
        return CvJson.decodeFromString<Cv>(body)
    }

    companion object {
        const val DEFAULT_URL = "https://raw.githubusercontent.com/8kt8/CvApi/main/cv.json"
        private const val TIMEOUT_MS = 8_000L
    }
}

/**
 * Entry point for both apps (Android ViewModel and the SwiftUI view model).
 * Offline-first: show [bundledCv] immediately, then replace it with [latestCv].
 * Owns its HTTP client - call [close] when done.
 */
class CvService(
    private val client: HttpClient,
    url: String = RemoteCvRepository.DEFAULT_URL,
) : AutoCloseable {

    /** Default engine per platform (OkHttp on Android, Darwin on iOS); also the Swift-friendly constructor. */
    constructor() : this(HttpClient())

    private val bundled = BundledCvRepository()
    private val remote = RemoteCvRepository(client, url)

    // @Throws makes failures reach Swift as thrown errors instead of crashing the app.
    @Throws(Exception::class)
    suspend fun bundledCv(): Cv = bundled.getCv()

    @Throws(Exception::class)
    suspend fun latestCv(): Cv = remote.getCv()

    override fun close() = client.close()
}
