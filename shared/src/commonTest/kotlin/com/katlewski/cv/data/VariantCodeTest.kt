package com.katlewski.cv.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class VariantCodeTest {
    private val code = "Ab3dEf6hIj9L"

    @Test
    fun parseAcceptsExactlyTwelveAlphanumerics() {
        assertEquals(code, VariantCode.parse(code))
        assertEquals(code, VariantCode.parse("  $code\n"))
        assertEquals("000000000000", VariantCode.parse("000000000000"))
    }

    @Test
    fun parseRejectsInvalidCodes() {
        assertNull(VariantCode.parse(null))
        assertNull(VariantCode.parse(""))
        assertNull(VariantCode.parse("Ab3dEf6hIj9")) // 11, too short
        assertNull(VariantCode.parse("Ab3dEf6hIj9LM")) // 13, too long
        assertNull(VariantCode.parse("Ab3dEf6hIj9-"))
        assertNull(VariantCode.parse("Ab3dEf6hIj9_"))
        assertNull(VariantCode.parse("Ab3dEf 6hIj9"))
        assertNull(VariantCode.parse("Ab3dEf6hIj9ł")) // non-ASCII letter
        assertNull(VariantCode.parse("../../etc/pa"))
    }

    @Test
    fun fromLinkReadsPathCode() {
        assertEquals(code, VariantCode.fromLink("https://mycv-szk.pages.dev/c/$code"))
        assertEquals(code, VariantCode.fromLink("https://mycv-szk.pages.dev/c/$code/"))
        assertEquals(code, VariantCode.fromLink("https://mycv-szk.pages.dev/c/$code?utm_source=mail"))
        assertEquals(code, VariantCode.fromLink("https://mycv-szk.pages.dev/c/$code#top"))
        assertEquals(code, VariantCode.fromLink("http://mycv-szk.pages.dev/c/$code"))
        assertEquals(code, VariantCode.fromLink("mycv-szk.pages.dev/c/$code"))
        assertEquals(code, VariantCode.fromLink("/c/$code"))
    }

    @Test
    fun fromLinkReadsQueryCode() {
        assertEquals(code, VariantCode.fromLink("https://mycv-szk.pages.dev/?v=$code"))
        assertEquals(code, VariantCode.fromLink("https://mycv-szk.pages.dev?v=$code"))
        assertEquals(code, VariantCode.fromLink("https://mycv-szk.pages.dev/?lang=en&v=$code"))
        assertEquals(code, VariantCode.fromLink("https://mycv-szk.pages.dev/?v=$code&lang=en"))
    }

    @Test
    fun fromLinkAcceptsBareCode() {
        assertEquals(code, VariantCode.fromLink(code))
    }

    @Test
    fun fromLinkRejectsInvalidLinks() {
        assertNull(VariantCode.fromLink(null))
        assertNull(VariantCode.fromLink(""))
        assertNull(VariantCode.fromLink("https://mycv-szk.pages.dev/"))
        assertNull(VariantCode.fromLink("https://mycv-szk.pages.dev/c/"))
        assertNull(VariantCode.fromLink("https://mycv-szk.pages.dev/c/Ab3dEf6hIj9")) // too short
        assertNull(VariantCode.fromLink("https://mycv-szk.pages.dev/c/Ab3dEf6hIj9LM")) // too long
        assertNull(VariantCode.fromLink("https://mycv-szk.pages.dev/c/Ab3dEf6hIj9!"))
        assertNull(VariantCode.fromLink("https://mycv-szk.pages.dev/c/Ab3dEf6hIj9L/extra"))
        assertNull(VariantCode.fromLink("https://mycv-szk.pages.dev/?v=Ab3dEf6hIj9"))
        assertNull(VariantCode.fromLink("https://mycv-szk.pages.dev/?v=Ab3dEf6hIj9LM"))
        assertNull(VariantCode.fromLink("https://mycv-szk.pages.dev/?v=Ab3dEf%6hIj9"))
        assertNull(VariantCode.fromLink("https://mycv-szk.pages.dev/?xv=$code"))
        assertNull(VariantCode.fromLink("https://mycv-szk.pages.dev/x/$code"))
    }
}

class CvSourceTest {
    private val code = "Ab3dEf6hIj9L"

    @Test
    fun buildsUrlsWithoutTrailingSlash() {
        val source = CvSource("https://cv.example.com")
        assertEquals("https://cv.example.com/default.json", source.defaultUrl())
        assertEquals("https://cv.example.com/v/$code.json", source.variantUrl(code))
    }

    @Test
    fun buildsUrlsWithTrailingSlash() {
        val source = CvSource("https://cv.example.com/")
        assertEquals("https://cv.example.com/default.json", source.defaultUrl())
        assertEquals("https://cv.example.com/v/$code.json", source.variantUrl(code))
        assertEquals("https://cv.example.com/default.json", CvSource("https://cv.example.com//").defaultUrl())
    }

    @Test
    fun keepsBasePath() {
        assertEquals("https://example.com/cv/default.json", CvSource("https://example.com/cv/").defaultUrl())
    }

    @Test
    fun defaultBaseUrl() {
        assertEquals("https://mycv-szk.pages.dev/default.json", CvSource(CvLinks.DEFAULT_BASE_URL).defaultUrl())
        assertEquals("https://${CvLinks.HOST}", CvLinks.DEFAULT_BASE_URL)
    }

    @Test
    fun rejectsInvalidVariantCode() {
        assertFailsWith<IllegalArgumentException> { CvSource("https://cv.example.com").variantUrl("../secret") }
    }
}
