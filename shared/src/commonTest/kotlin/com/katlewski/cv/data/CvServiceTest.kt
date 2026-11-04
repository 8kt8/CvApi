package com.katlewski.cv.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CvServiceTest {
    private val baseUrl = "https://cv.example.com"
    private val code = "Ab3dEf6hIj9L"

    private fun cvJson(name: String) = """{"profile":{"fullName":"$name","headline":"h","about":"a"},"extra":1}"""

    private class Recorder {
        val urls = mutableListOf<String>()
    }

    private fun service(
        mode: CvMode,
        cache: VariantCache = InMemoryVariantCache(),
        recorder: Recorder = Recorder(),
        handler: (url: String) -> Pair<HttpStatusCode, String>?,
    ): CvService {
        val engine = MockEngine { request ->
            val url = request.url.toString()
            recorder.urls += url
            val result = handler(url) ?: throw IllegalStateException("offline")
            if (result.first.value in 200..299) {
                respond(result.second, result.first, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respondError(result.first)
            }
        }
        return CvService(HttpClient(engine), baseUrl, mode, cache)
    }

    @Test
    fun defaultInitialCvIsBundled() = runTest {
        val service = service(CvMode.Default) { null }
        val initial = service.initialCv()
        assertNotNull(initial)
        assertEquals(service.bundledCv(), initial)
        assertEquals(CvMode.Default, service.mode)
    }

    @Test
    fun bundledCvIsSampleData() = runTest {
        // Default build bundles the fictional sample-cv.json, never real data.
        val cv = service(CvMode.Default) { null }.bundledCv()
        assertEquals("Alex Sample", cv.profile.fullName)
        assertTrue(cv.experience.size >= 3)
    }

    @Test
    fun defaultLatestCvLoadsDefaultJson() = runTest {
        val recorder = Recorder()
        val service = service(CvMode.Default, recorder = recorder) { HttpStatusCode.OK to cvJson("Default Person") }
        assertEquals("Default Person", service.latestCv().profile.fullName)
        assertEquals(listOf("$baseUrl/default.json"), recorder.urls)
    }

    @Test
    fun variantInitialCvIsNullWithoutCacheAndNeverBundled() = runTest {
        val service = service(CvMode.Variant(code)) { null }
        assertNull(service.initialCv())
    }

    @Test
    fun variantNetworkFailureThrowsAndDoesNotReturnDefault() = runTest {
        val recorder = Recorder()
        val service = service(CvMode.Variant(code), recorder = recorder) { url ->
            if (url.endsWith("/default.json")) HttpStatusCode.OK to cvJson("Default Person") else null
        }
        assertFails { service.latestCv() }
        assertEquals(listOf("$baseUrl/v/$code.json"), recorder.urls)
        assertNull(service.initialCv())
    }

    @Test
    fun variantHttpErrorThrows() = runTest {
        val cache = InMemoryVariantCache()
        val service = service(CvMode.Variant(code), cache) { url ->
            if (url.endsWith("/default.json")) HttpStatusCode.OK to cvJson("Default Person") else HttpStatusCode.NotFound to ""
        }
        assertFails { service.latestCv() }
        assertNull(cache.get(code))
    }

    @Test
    fun variantInvalidJsonThrowsAndIsNotCached() = runTest {
        val cache = InMemoryVariantCache()
        val service = service(CvMode.Variant(code), cache) { HttpStatusCode.OK to "<html>not json</html>" }
        assertFails { service.latestCv() }
        assertNull(cache.get(code))
    }

    @Test
    fun variantSuccessIsCachedAndReturnedByInitialCv() = runTest {
        val cache = InMemoryVariantCache()
        val recorder = Recorder()
        val json = cvJson("Variant Person")
        val online = service(CvMode.Variant(code), cache, recorder) { HttpStatusCode.OK to json }

        assertNull(online.initialCv())
        assertEquals("Variant Person", online.latestCv().profile.fullName)
        assertEquals(listOf("$baseUrl/v/$code.json"), recorder.urls)
        assertEquals(json, cache.get(code))
        assertEquals("Variant Person", online.initialCv()?.profile?.fullName)

        // A later, offline service with the same cache shows the cached variant, still never the default.
        val offline = service(CvMode.Variant(code), cache) { null }
        assertEquals("Variant Person", offline.initialCv()?.profile?.fullName)
        assertFails { offline.latestCv() }
        assertEquals("Variant Person", offline.initialCv()?.profile?.fullName)
    }

    @Test
    fun corruptCacheEntryIsIgnored() = runTest {
        val cache = InMemoryVariantCache()
        cache.put(code, "{broken")
        assertNull(service(CvMode.Variant(code), cache) { null }.initialCv())
    }

    @Test
    fun swiftConstructorPicksModeFromCode() {
        CvService(baseUrl, code).use { assertEquals(CvMode.Variant(code), it.mode) }
        CvService(baseUrl, null).use { assertEquals(CvMode.Default, it.mode) }
        CvService(baseUrl, "too-short").use { assertEquals(CvMode.Default, it.mode) }
        CvService().use { assertEquals(CvMode.Default, it.mode) }
    }
}
