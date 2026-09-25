package com.katlewski.cv

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.katlewski.cv.data.BundledCvRepository
import com.katlewski.cv.data.RemoteCvRepository
import com.katlewski.cv.ui.CvScreen
import com.katlewski.cv.ui.CvViewModel
import com.katlewski.cv.ui.theme.CvTheme
import io.ktor.client.HttpClient

@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
    }
    CvTheme {
        val viewModel = viewModel {
            // The ViewModel owns the client and closes it in onCleared.
            val client = HttpClient()
            CvViewModel(bundled = BundledCvRepository(), remote = RemoteCvRepository(client), client)
        }
        CvScreen(viewModel)
    }
}
