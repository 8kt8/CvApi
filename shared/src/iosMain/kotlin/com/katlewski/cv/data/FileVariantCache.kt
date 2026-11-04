@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.katlewski.cv.data

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

/**
 * [VariantCache] that keeps each variant as `<directoryPath>/<code>.json`, so a variant survives app restarts.
 * Codes are validated with [VariantCode.parse] before any path is built; an invalid code is a cache miss
 * (`get` returns null, `put`/`remove` do nothing). The directory is created on the first `put`.
 * Writes are atomic. File errors are not thrown - the cache is best-effort and never fails a CV load.
 */
class FileVariantCache(private val directoryPath: String) : VariantCache {
    private val mutex = Mutex()
    private val directory = directoryPath.trimEnd('/')

    private fun fileFor(code: String): String? = VariantCode.parse(code)?.let { "$directory/$it.json" }

    override suspend fun get(code: String): String? {
        val path = fileFor(code) ?: return null
        return io { NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null) }
    }

    override suspend fun put(code: String, json: String) {
        val path = fileFor(code) ?: return
        io {
            NSFileManager.defaultManager.createDirectoryAtPath(directory, true, null, null)
            NSString.create(string = json).writeToFile(path, true, NSUTF8StringEncoding, null)
        }
    }

    override suspend fun remove(code: String) {
        val path = fileFor(code) ?: return
        io {
            val files = NSFileManager.defaultManager
            if (files.fileExistsAtPath(path)) files.removeItemAtPath(path, null)
        }
    }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { mutex.withLock { block() } }
}

/**
 * A [FileVariantCache] in `<Application Support>/variants` (Application Support is created if missing).
 * Falls back to an [InMemoryVariantCache] if the directory cannot be resolved.
 *
 * Swift: `CvService(baseUrl: ..., code: ..., cache: FileVariantCacheKt.defaultIosVariantCache())`.
 */
fun defaultIosVariantCache(): VariantCache {
    val appSupport = NSFileManager.defaultManager
        .URLForDirectory(NSApplicationSupportDirectory, NSUserDomainMask, null, true, null)
        ?.path
        ?: return InMemoryVariantCache()
    return FileVariantCache("$appSupport/variants")
}
