package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class LanguageQuote(val text: String)

@Composable
fun WelcomeScreen(
    onGetStartedClick: () -> Unit
) {
    val quotes = remember {
        listOf(
            LanguageQuote("One language sets you in a corridor for life. Two languages open every door along the way."),
            LanguageQuote("To have another language is to possess a second soul."),
            LanguageQuote("Learn a new language and get a new soul."),
            LanguageQuote("The limits of my language mean the limits of my world."),
            LanguageQuote("Language is the road map of a culture."),
            LanguageQuote("A different language is a different vision of life."),
            LanguageQuote("Language expresses the collective mind of a community."),
            LanguageQuote("He who knows no foreign languages knows nothing of his own."),
            LanguageQuote("Change your language and you change your thoughts.")
        )
    }

    var currentQuoteIndex by remember { mutableStateOf(1) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            currentQuoteIndex = (currentQuoteIndex + 1) % quotes.size
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Giant top right circle background segment matching Screenshot 1
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 90.dp, y = (-90).dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFFBEA))
            )

            // Giant bottom left circle background segment matching Screenshot 1
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-95).dp, y = 95.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFFBEA))
            )

            // Screen content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top header label - Centered and elegant
                Text(
                    text = "FunkyTalk",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    letterSpacing = (-1).sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .testTag("welcome_app_title")
                )

                // Central clean graphic area with a premium animated card carousel
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Soft floating glow backdrop
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color(0xFFFFFDF5).copy(alpha = 0.85f))
                            .clickable {
                                currentQuoteIndex = (currentQuoteIndex + 1) % quotes.size
                            }
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = quotes[currentQuoteIndex],
                            transitionSpec = {
                                (slideInVertically(animationSpec = tween(600, delayMillis = 50)) { height -> height } + fadeIn(animationSpec = tween(600, delayMillis = 50)))
                                    .togetherWith(slideOutVertically(animationSpec = tween(400)) { height -> -height } + fadeOut(animationSpec = tween(400)))
                            },
                            label = "quote_carousel"
                        ) { quote ->
                            Text(
                                text = quote.text,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                textAlign = TextAlign.Center,
                                lineHeight = 32.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Call to action button at bottom (solid rounded amber)
                Button(
                    onClick = onGetStartedClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("get_started_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC529),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(29.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "Get Started",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
