package com.katlewski.cv.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Stores the raw JSON of the last successfully loaded variant, keyed by code, so a variant
 * can be shown offline. Apps can provide a persistent implementation (DataStore, files, UserDefaults);
 * on iOS use `FileVariantCache` / `defaultIosVariantCache()`.
 */
interface VariantCache {
    suspend fun get(code: String): String?
    suspend fun put(code: String, json: String)

    /** Drops the entry for [code], if any (used when the variant is revoked). */
    suspend fun remove(code: String)
}

/** Process-lifetime cache. Lost when the app is killed. */
class InMemoryVariantCache : VariantCache {
    private val mutex = Mutex()
    private val entries = mutableMapOf<String, String>()

    override suspend fun get(code: String): String? = mutex.withLock { entries[code] }

    override suspend fun put(code: String, json: String) {
        mutex.withLock { entries[code] = json }
    }

    override suspend fun remove(code: String) {
        mutex.withLock { entries.remove(code) }
    }
}
