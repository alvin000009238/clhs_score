package com.clhs.score.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: String
)

val onboardingPages = listOf(
    OnboardingPage(
        title = "即時掌握成績",
        description = "隨時隨地查看您的最新成績與班級排名，不再錯過任何重要資訊。",
        icon = "person"
    ),
    OnboardingPage(
        title = "深度成績分析",
        description = "透過視覺化圖表追蹤歷次考試趨勢，掌握學習狀況與注意的科目。",
        icon = "science"
    ),
    OnboardingPage(
        title = "個人課表查詢",
        description = "整合學校課表，快速查看上課時間與地點，並支援桌面小工具。",
        icon = "newsstand"
    )
)

@Composable
fun IntroScreen(
    showSkipButton: Boolean = false,
    onSkipClick: () -> Unit = {},
    onLoginClick: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val isDark = isSystemInDarkTheme()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background decorations
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = if (isDark) 0.15f else 0.1f), Color.Transparent),
                        center = Offset(size.width * 0.8f, size.height * 0.2f),
                        radius = size.width * 0.8f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tertiaryColor.copy(alpha = if (isDark) 0.15f else 0.1f), Color.Transparent),
                        center = Offset(size.width * 0.2f, size.height * 0.7f),
                        radius = size.width * 0.6f
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top bar with optional skip button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    if (showSkipButton) {
                        TextButton(onClick = onSkipClick) {
                            Text(
                                text = "跳過",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                ) { page ->
                    val item = onboardingPages[page]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedRoundedSymbol(
                            icon = item.icon,
                            size = 80.dp,
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.height(48.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Pager Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pagerState.pageCount) { iteration ->
                        val color = if (pagerState.currentPage == iteration) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        }
                        val width = if (pagerState.currentPage == iteration) 24.dp else 8.dp
                        Box(
                            modifier = Modifier
                                .size(width = width, height = 8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "使用學校帳號登入",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
