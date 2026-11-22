package com.punto.app.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.punto.app.R

private data class Page(val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: Int, val desc: Int)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        Page(Icons.Outlined.Place, R.string.onboarding_1_title, R.string.onboarding_1_desc),
        Page(Icons.Outlined.Map, R.string.onboarding_2_title, R.string.onboarding_2_desc),
        Page(Icons.Outlined.NotificationsActive, R.string.onboarding_3_title, R.string.onboarding_3_desc),
    )
    var index by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinish) { Text(stringResource(R.string.welcome_skip)) }
        }
        Spacer(Modifier.weight(1f))
        AnimatedContent(targetState = index, label = "onboarding") { i ->
            val page = pages[i]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(140.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                    Icon(page.icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(64.dp))
                }
                Spacer(Modifier.height(32.dp))
                Text(stringResource(page.title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(page.desc), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.Center) {
            pages.indices.forEach { i ->
                Box(Modifier.padding(4.dp).size(if (i == index) 24.dp else 8.dp, 8.dp).clip(CircleShape).background(if (i == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { if (index < pages.lastIndex) index++ else onFinish() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(stringResource(if (index < pages.lastIndex) R.string.onboarding_next else R.string.onboarding_done))
        }
    }
}
