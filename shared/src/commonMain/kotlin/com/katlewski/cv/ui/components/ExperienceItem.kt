package com.katlewski.cv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.katlewski.cv.data.AppLink
import com.katlewski.cv.data.Experience
import com.katlewski.cv.data.Store
import com.katlewski.cv.ui.format
import com.katlewski.cv.ui.icons.CvIcons
import com.katlewski.cv.ui.isCurrent
import com.katlewski.cv.ui.logoFor
import com.katlewski.cv.ui.theme.spacing

private val LogoSize = 48.dp

/** One entry of the experience timeline: logo on a vertical rail, details in a card. */
@Composable
fun ExperienceItem(experience: Experience, isLast: Boolean, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val railColor = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                if (!isLast) {
                    val x = LogoSize.toPx() / 2
                    drawLine(railColor, Offset(x, LogoSize.toPx() + 4.dp.toPx()), Offset(x, size.height), 2.dp.toPx())
                }
            },
    ) {
        Logo(url = logoFor(experience.logoUrl, experience.companyUrl), name = experience.companyName, size = LogoSize)
        Spacer(Modifier.width(spacing.small))
        ExperienceCard(
            experience = experience,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else spacing.medium),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExperienceCard(experience: Experience, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val uriHandler = LocalUriHandler.current
    val hasDetails = experience.highlights.isNotEmpty() || experience.techStack.isNotEmpty()
    var expanded by rememberSaveable(experience.companyName, experience.period.start) { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
    ) {
        Column(Modifier.padding(start = spacing.medium, end = spacing.xSmall, top = spacing.medium, bottom = spacing.xSmall)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(end = spacing.xSmall)) {
                    Text(experience.role, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        experience.companyName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                experience.companyUrl?.let { url ->
                    IconButton(onClick = { uriHandler.openUri(url) }) {
                        Icon(
                            CvIcons.OpenInNew,
                            contentDescription = "Open ${experience.companyName} website",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(spacing.xSmall))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
                verticalArrangement = Arrangement.spacedBy(spacing.xxSmall),
                itemVerticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = spacing.xSmall),
            ) {
                if (experience.period.isCurrent) {
                    Tag(
                        "Current",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                MetaText(listOfNotNull(experience.period.format(), experience.location).joinToString("  ·  "))
            }
            experience.summary?.let {
                Spacer(Modifier.height(spacing.small))
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = spacing.xSmall))
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = spacing.small, end = spacing.xSmall)) {
                    experience.highlights.forEach { highlight ->
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(spacing.xSmall))
                            Text(highlight, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (experience.techStack.isNotEmpty()) {
                        Spacer(Modifier.height(spacing.small))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
                            verticalArrangement = Arrangement.spacedBy(spacing.xSmall),
                        ) {
                            experience.techStack.forEach { Tag(it) }
                        }
                    }
                }
            }
            if (experience.apps.isNotEmpty()) {
                Spacer(Modifier.height(spacing.small))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
                    modifier = Modifier.padding(end = spacing.xSmall),
                ) {
                    experience.apps.forEach { app -> StoreButton(app) { uriHandler.openUri(app.url) } }
                }
            }
            if (hasDetails) {
                val rotation by animateFloatAsState(if (expanded) 180f else 0f)
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.semantics {
                        stateDescription = if (expanded) "Expanded" else "Collapsed"
                    },
                ) {
                    Text(if (expanded) "Hide details" else "Show details")
                    Spacer(Modifier.width(spacing.xxSmall))
                    Icon(CvIcons.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp).rotate(rotation))
                }
            } else {
                Spacer(Modifier.height(spacing.xSmall))
            }
        }
    }
}

@Composable
private fun MetaText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun StoreButton(app: AppLink, onClick: () -> Unit) {
    val storeName = app.store.title()
    OutlinedButton(
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Get ${app.name} on $storeName" },
    ) {
        Icon(app.store.icon(), contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Column {
            Text(storeName, style = MaterialTheme.typography.labelSmall)
            Text(app.name, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun Store.title(): String = when (this) {
    Store.GooglePlay -> "Google Play"
    Store.AppStore -> "App Store"
    Store.Web -> "Web"
}

private fun Store.icon(): ImageVector = when (this) {
    Store.GooglePlay -> CvIcons.GooglePlay
    Store.AppStore -> CvIcons.AppStore
    Store.Web -> CvIcons.Language
}
