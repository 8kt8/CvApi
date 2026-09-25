package com.katlewski.cv.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.katlewski.cv.data.Cv
import com.katlewski.cv.ui.components.EducationCard
import com.katlewski.cv.ui.components.ExperienceItem
import com.katlewski.cv.ui.components.Footer
import com.katlewski.cv.ui.components.ProfileHeader
import com.katlewski.cv.ui.components.SectionHeader
import com.katlewski.cv.ui.components.SkillsSection
import com.katlewski.cv.ui.icons.CvIcons
import com.katlewski.cv.ui.theme.MaxContentWidth
import com.katlewski.cv.ui.theme.spacing

@Composable
fun CvScreen(viewModel: CvViewModel) {
    val state by viewModel.state.collectAsState()
    CvScreen(
        state = state,
        onRefresh = viewModel::onRefresh,
        onMessageShown = viewModel::onMessageShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CvScreen(state: CvUiState, onRefresh: () -> Unit, onMessageShown: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    // Edge-to-edge: the header gradient runs under the status bar; insets are applied per element.
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        val cv = state.cv
        if (cv == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            state = pullState,
            modifier = Modifier.fillMaxSize().padding(padding),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = state.isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBars),
                )
            },
        ) {
            CvContent(cv)
        }
    }
}

@Composable
private fun CvContent(cv: Cv) {
    val spacing = MaterialTheme.spacing
    val listState = rememberLazyListState()
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 } }
    val headerBrush = Brush.verticalGradient(
        listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface),
    )

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "header") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(headerBrush),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    ProfileHeader(
                        profile = cv.profile,
                        contacts = cv.contacts,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .contentWidth()
                            .padding(horizontal = spacing.medium)
                            .padding(top = spacing.xLarge),
                    )
                }
            }
            cvSections(cv)
            item(key = "footer") { Footer(Modifier.contentWidth().padding(horizontal = spacing.medium)) }
        }

        // Scrim keeps the status bar legible once content scrolls underneath it.
        AnimatedVisibility(visible = scrolled, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.safeDrawing)
                    .background(MaterialTheme.colorScheme.surface),
            )
        }
    }
}

private fun LazyListScope.cvSections(cv: Cv) {
    if (cv.experience.isNotEmpty()) {
        item(key = "experience-header") { SectionHeader("Experience", CvIcons.Work, Modifier.contentPadding()) }
        itemsIndexed(cv.experience, key = { _, it -> "exp-${it.companyName}-${it.period.start}" }) { index, experience ->
            ExperienceItem(experience, isLast = index == cv.experience.lastIndex, modifier = Modifier.contentPadding())
        }
    }
    if (cv.skills.isNotEmpty()) {
        item(key = "skills-header") { SectionHeader("Skills", CvIcons.Code, Modifier.contentPadding()) }
        item(key = "skills") { SkillsSection(cv.skills, Modifier.contentPadding()) }
    }
    if (cv.education.isNotEmpty()) {
        item(key = "education-header") { SectionHeader("Education", CvIcons.School, Modifier.contentPadding()) }
        itemsIndexed(cv.education, key = { _, it -> "edu-${it.schoolName}-${it.degree}" }) { index, education ->
            EducationCard(
                education,
                Modifier
                    .contentPadding()
                    .padding(bottom = if (index == cv.education.lastIndex) 0.dp else MaterialTheme.spacing.small),
            )
        }
    }
}

/** Centers content at a readable width on tablets / landscape. */
private fun Modifier.contentWidth(): Modifier = widthIn(max = MaxContentWidth).fillMaxWidth()

@Composable
private fun Modifier.contentPadding(): Modifier = contentWidth().padding(horizontal = MaterialTheme.spacing.medium)

