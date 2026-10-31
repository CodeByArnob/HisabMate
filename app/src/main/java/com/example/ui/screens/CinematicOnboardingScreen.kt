package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HisabTheme
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.util.CurrencyCatalog
import com.example.util.CurrencyFormatter
import com.example.util.HisabHaptics
import com.example.util.HisabNotificationManager
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class OnboardingChapterMeta(
    val stepNumber: String,
    val badgeTitle: String,
    val primaryGlow: Color,
    val secondaryGlow: Color,
    val accentColor: Color
)

private val onboardingChapters = listOf(
    OnboardingChapterMeta(
        stepNumber = "01",
        badgeTitle = "COMMAND CENTER",
        primaryGlow = Color(0xFF6D4AFF),
        secondaryGlow = Color(0xFF00A86B),
        accentColor = Color(0xFF6D4AFF)
    ),
    OnboardingChapterMeta(
        stepNumber = "02",
        badgeTitle = "MULTI-WALLET DECK",
        primaryGlow = Color(0xFFE2136E),
        secondaryGlow = Color(0xFF6D4AFF),
        accentColor = Color(0xFFE2136E)
    ),
    OnboardingChapterMeta(
        stepNumber = "03",
        badgeTitle = "FLOWS & SMART OCR",
        primaryGlow = Color(0xFF0084E9),
        secondaryGlow = Color(0xFF059669),
        accentColor = Color(0xFF0084E9)
    ),
    OnboardingChapterMeta(
        stepNumber = "04",
        badgeTitle = "INTELLIGENT BUDGETS",
        primaryGlow = Color(0xFF059669),
        secondaryGlow = Color(0xFFD97706),
        accentColor = Color(0xFF059669)
    ),
    OnboardingChapterMeta(
        stepNumber = "05",
        badgeTitle = "GOALS & DEBT LEDGER",
        primaryGlow = Color(0xFF7C3AED),
        secondaryGlow = Color(0xFF0891B2),
        accentColor = Color(0xFF7C3AED)
    ),
    OnboardingChapterMeta(
        stepNumber = "06",
        badgeTitle = "GLOBAL FX & CURRENCY",
        primaryGlow = Color(0xFF0284C7),
        secondaryGlow = Color(0xFF4F46E5),
        accentColor = Color(0xFF0284C7)
    ),
    OnboardingChapterMeta(
        stepNumber = "07",
        badgeTitle = "PROACTIVE ALERTS",
        primaryGlow = Color(0xFF059669),
        secondaryGlow = Color(0xFF6D4AFF),
        accentColor = Color(0xFF059669)
    )
)

/**
 * World-class, cinematic 7-step interactive onboarding experience for HisabMate.
 * Every step features a bespoke interactive composition, dynamic financial visualizations,
 * fluid HorizontalPager swipe support, and adaptive safe-area layout.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CinematicHisabOnboardingFlow(
    currentBaseCurrency: String,
    onCompleteOnboarding: (
        selectedCurrencyCode: String,
        languageCode: String,
        regionCode: String,
        notificationsEnabled: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = HisabTheme.colors
    val coroutineScope = rememberCoroutineScope()

    val totalSteps = 7
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { totalSteps })
    val currentStep = pagerState.currentPage

    // Respect Android's system reduced-motion setting
    val animationsEnabled = remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) > 0f
        }.getOrDefault(true)
    }

    var selectedCurrency by remember(currentBaseCurrency) {
        mutableStateOf(currentBaseCurrency.ifBlank { "BDT" })
    }
    var selectedLanguage by remember { mutableStateOf("en") }
    var selectedRegion by remember { mutableStateOf("BD") }
    var currencySearchQuery by remember { mutableStateOf("") }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var permissionRequestedOrGranted by remember {
        mutableStateOf(HisabNotificationManager.hasNotificationPermission(context))
    }
    var permissionStatusNote by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionRequestedOrGranted = granted
        notificationsEnabled = granted
        if (granted) {
            HisabHaptics.performSuccess(context, view)
            permissionStatusNote = "Smart local alerts enabled! Customize channels anytime in Settings."
        } else {
            HisabHaptics.performWarning(context, view)
            permissionStatusNote = "Notifications skipped. HisabMate works 100% normally and you can enable alerts later in Settings."
        }
    }

    // Handle system back gesture across steps
    BackHandler(enabled = currentStep > 0) {
        HisabHaptics.performClick(view)
        coroutineScope.launch {
            pagerState.animateScrollToPage((currentStep - 1).coerceAtLeast(0))
        }
    }

    val activeChapter = onboardingChapters[currentStep.coerceIn(0, onboardingChapters.lastIndex)]

    val primaryGlowColor by animateColorAsState(
        targetValue = activeChapter.primaryGlow,
        animationSpec = tween(550),
        label = "primaryGlowColor"
    )
    val secondaryGlowColor by animateColorAsState(
        targetValue = activeChapter.secondaryGlow,
        animationSpec = tween(550),
        label = "secondaryGlowColor"
    )
    val stepAccentColor by animateColorAsState(
        targetValue = activeChapter.accentColor,
        animationSpec = tween(420),
        label = "stepAccentColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ambientMotion")
    val ambientPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (animationsEnabled) 9000 else 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambientPhase"
    )

    Surface(
        color = colors.canvasBackground,
        modifier = modifier
            .fillMaxSize()
            .testTag("onboarding_flow_screen")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle Cinematic Ambient Mesh & Lighting Canvas
            CinematicOnboardingAmbientCanvas(
                primaryGlow = primaryGlowColor,
                secondaryGlow = secondaryGlowColor,
                phase = ambientPhase,
                isDark = colors.isDark,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
                    .widthIn(max = 640.dp)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                // Top Interactive Journey Header & Skip Control
                CinematicOnboardingTopHeader(
                    currentStep = currentStep,
                    totalSteps = totalSteps,
                    chapter = activeChapter,
                    accentColor = stepAccentColor,
                    onSelectStep = { targetStep ->
                        HisabHaptics.performClick(view)
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(targetStep)
                        }
                    },
                    onSkip = {
                        HisabHaptics.performClick(view)
                        onCompleteOnboarding(
                            selectedCurrency,
                            selectedLanguage,
                            selectedRegion,
                            notificationsEnabled
                        )
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Fluid HorizontalPager for the 7 Interactive Chapters
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (page) {
                            0 -> Step1BrandCommandCenterScene(
                                selectedCurrency = selectedCurrency,
                                onSelectQuickCurrency = { code ->
                                    HisabHaptics.performClick(view)
                                    selectedCurrency = code
                                },
                                ambientPhase = ambientPhase
                            )

                            1 -> Step2FloatingMultiWalletDeckScene(
                                ambientPhase = ambientPhase
                            )

                            2 -> Step3InteractiveFlowsAndOcrScene(
                                ambientPhase = ambientPhase
                            )

                            3 -> Step4IntelligentBudgetsRadarScene(
                                selectedCurrency = selectedCurrency
                            )

                            4 -> Step5GoalsAndDebtLedgerScene(
                                selectedCurrency = selectedCurrency
                            )

                            5 -> Step6GlobalMultiCurrencyEngineScene(
                                selectedCurrency = selectedCurrency,
                                selectedRegion = selectedRegion,
                                currencySearchQuery = currencySearchQuery,
                                onSearchQueryChange = { currencySearchQuery = it },
                                onSelectCurrency = { meta ->
                                    HisabHaptics.performClick(view)
                                    selectedCurrency = meta.code
                                    selectedRegion = meta.flagCode
                                },
                                onSelectRegion = { regionCode ->
                                    HisabHaptics.performClick(view)
                                    selectedRegion = regionCode
                                }
                            )

                            6 -> Step7SmartNotificationStackScene(
                                selectedCurrency = selectedCurrency,
                                notificationsEnabled = notificationsEnabled,
                                permissionRequestedOrGranted = permissionRequestedOrGranted,
                                permissionStatusNote = permissionStatusNote,
                                onToggleNotificationsEnabled = { enabled ->
                                    HisabHaptics.performClick(view)
                                    notificationsEnabled = enabled
                                },
                                onRequestPermission = {
                                    HisabHaptics.performClick(view)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        permissionRequestedOrGranted = true
                                        notificationsEnabled = true
                                        permissionStatusNote = "Local notifications are active on this Android version."
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pinned Safe-Area Bottom Navigation Dock (Never Clipped)
                CinematicOnboardingBottomDock(
                    currentStep = currentStep,
                    totalSteps = totalSteps,
                    selectedCurrency = selectedCurrency,
                    accentColor = stepAccentColor,
                    onBack = {
                        HisabHaptics.performClick(view)
                        coroutineScope.launch {
                            pagerState.animateScrollToPage((currentStep - 1).coerceAtLeast(0))
                        }
                    },
                    onNextOrLaunch = {
                        if (currentStep < totalSteps - 1) {
                            HisabHaptics.performClick(view)
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(currentStep + 1)
                            }
                        } else {
                            HisabHaptics.performSuccess(context, view)
                            onCompleteOnboarding(
                                selectedCurrency,
                                selectedLanguage,
                                selectedRegion,
                                notificationsEnabled
                            )
                        }
                    }
                )
            }
        }
    }
}

// ============================================================================
// AMBIENT LIGHTING CANVAS & HEADER / FOOTER DOCK
// ============================================================================

@Composable
private fun CinematicOnboardingAmbientCanvas(
    primaryGlow: Color,
    secondaryGlow: Color,
    phase: Float,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val glowAlphaPrimary = if (isDark) 0.22f else 0.12f
        val glowAlphaSecondary = if (isDark) 0.16f else 0.09f

        val topOffset = Offset(
            x = w * (0.78f + 0.08f * cos(phase)),
            y = h * (0.14f + 0.04f * sin(phase))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryGlow.copy(alpha = glowAlphaPrimary), Color.Transparent),
                center = topOffset,
                radius = w * 0.72f
            ),
            radius = w * 0.72f,
            center = topOffset
        )

        val bottomOffset = Offset(
            x = w * (0.20f + 0.07f * sin(phase * 0.8f)),
            y = h * (0.76f + 0.05f * cos(phase * 0.8f))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryGlow.copy(alpha = glowAlphaSecondary), Color.Transparent),
                center = bottomOffset,
                radius = w * 0.68f
            ),
            radius = w * 0.68f,
            center = bottomOffset
        )
    }
}

@Composable
private fun CinematicOnboardingTopHeader(
    currentStep: Int,
    totalSteps: Int,
    chapter: OnboardingChapterMeta,
    accentColor: Color,
    onSelectStep: (Int) -> Unit,
    onSkip: () -> Unit
) {
    val colors = HisabTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor
                ) {
                    Text(
                        text = "${chapter.stepNumber} / 0$totalSteps",
                        fontFamily = SpaceGroteskFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = if (colors.isDark) 0.18f else 0.12f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = chapter.badgeTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            TextButton(
                onClick = onSkip,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.testTag("btn_onboarding_skip")
            ) {
                Text(
                    text = if (currentStep == totalSteps - 1) "Finish" else "Skip Tour",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.inkSecondary,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Skip onboarding",
                    tint = colors.inkSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive 7-Segment Journey Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (index in 0 until totalSteps) {
                val isCompleted = index < currentStep
                val isCurrent = index == currentStep
                val weightVal by animateFloatAsState(
                    targetValue = if (isCurrent) 2.2f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "segWeight_$index"
                )
                val segColor by animateColorAsState(
                    targetValue = when {
                        isCurrent -> accentColor
                        isCompleted -> accentColor.copy(alpha = 0.55f)
                        else -> colors.borderStrong.copy(alpha = 0.55f)
                    },
                    animationSpec = tween(280),
                    label = "segColor_$index"
                )

                Box(
                    modifier = Modifier
                        .weight(weightVal)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(segColor)
                        .clickable { onSelectStep(index) }
                        .testTag("onboarding_step_indicator_$index")
                )
            }
        }
    }
}

@Composable
private fun CinematicOnboardingBottomDock(
    currentStep: Int,
    totalSteps: Int,
    selectedCurrency: String,
    accentColor: Color,
    onBack: () -> Unit,
    onNextOrLaunch: () -> Unit
) {
    val colors = HisabTheme.colors
    val isLastStep = currentStep == totalSteps - 1
    val chosenMeta = remember(selectedCurrency) {
        CurrencyCatalog.getMetadata(selectedCurrency)
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderStrong),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, colors.borderStrong),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("btn_onboarding_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous step",
                        tint = colors.inkPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Back",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.inkPrimary
                    )
                }
            }

            Button(
                onClick = onNextOrLaunch,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("btn_onboarding_continue")
            ) {
                Text(
                    text = if (!isLastStep) {
                        "Continue"
                    } else {
                        "Launch HisabMate (${chosenMeta.symbol} ${chosenMeta.code})"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (!isLastStep) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ============================================================================
// STEP 1: BRAND REVEAL & INTERACTIVE LIVE DASHBOARD COMMAND CENTER
// ============================================================================

@Composable
private fun Step1BrandCommandCenterScene(
    selectedCurrency: String,
    onSelectQuickCurrency: (String) -> Unit,
    ambientPhase: Float
) {
    val colors = HisabTheme.colors
    val view = LocalView.current

    // Interactive Dashboard Lens ("NET_WORTH", "CASHFLOW", "HEALTH")
    var selectedLens by remember { mutableStateOf("NET_WORTH") }

    // Animated Balance Counter that responds to currency switches and lens switches
    val baseNetWorthBdt = 240645.80
    val currencyMeta = remember(selectedCurrency) { CurrencyCatalog.getMetadata(selectedCurrency) }
    val targetNetWorthInSelected = remember(selectedCurrency) {
        val rate = currencyMeta.defaultRateToBdt.coerceAtLeast(0.0001)
        (baseNetWorthBdt / rate).toFloat()
    }

    val entryAnim = remember { Animatable(0.15f) }
    LaunchedEffect(Unit) {
        entryAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing)
        )
    }

    val animatedAmount by animateFloatAsState(
        targetValue = targetNetWorthInSelected * entryAnim.value,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "step1AnimatedNetWorth"
    )

    // Hero Brand & Live Command Preview Card
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.2.dp, colors.borderStrong),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Emblem + Brand Story Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Orbital Animated Emblem
                Box(
                    modifier = Modifier.size(68.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val r = size.minDimension / 2f
                        drawCircle(
                            color = Color(0xFF6D4AFF).copy(alpha = 0.16f),
                            radius = r
                        )
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFF6D4AFF),
                                    Color(0xFF0084E9),
                                    Color(0xFF059669),
                                    Color(0xFF6D4AFF)
                                )
                            ),
                            startAngle = (ambientPhase * 45f) % 360f,
                            sweepAngle = 250f,
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF6D4AFF), Color(0xFF0084E9))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "HisabMate Crest",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF059669).copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "HISABMATE • GLOBAL FINTECH OS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF059669),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Master Your Money Across Every Currency",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.inkPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Experience a live interactive preview of your command center below. Switch currencies or tap any telemetry lens to explore.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = colors.inkSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Live Command Center Preview Surface
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF13192B),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "UNIFIED NET WORTH PREVIEW",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyFormatter.formatCurrency(
                                    animatedAmount.toDouble(),
                                    selectedCurrency
                                ),
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 26.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF059669).copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "LIVE FX • ${currencyMeta.symbol} ${currencyMeta.code}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF34D399),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Live Currency Switcher Pills in a dedicated full-width Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("BDT", "USD", "EUR", "GBP").forEach { code ->
                            val isSelected = selectedCurrency == code
                            val meta = CurrencyCatalog.getMetadata(code)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF6D4AFF) else Color.White.copy(alpha = 0.10f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.12f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectQuickCurrency(code) }
                            ) {
                                Text(
                                    text = "${meta.symbol} $code",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Interactive 3-Lens Telemetry Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("NET_WORTH", "Liquidity Mix", Color(0xFF6D4AFF)),
                            Triple("CASHFLOW", "Monthly Pulse", Color(0xFF059669)),
                            Triple("HEALTH", "Health 94/100", Color(0xFF0084E9))
                        ).forEach { (key, label, tint) ->
                            val active = selectedLens == key
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) tint.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.07f),
                                border = BorderStroke(
                                    1.dp,
                                    if (active) tint else Color.White.copy(alpha = 0.12f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        HisabHaptics.performClick(view)
                                        selectedLens = key
                                    }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (active) Color.White else Color.White.copy(alpha = 0.72f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dynamic Animated Content per Lens
                    AnimatedContent(
                        targetState = selectedLens,
                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                        label = "step1LensContent"
                    ) { lens ->
                        when (lens) {
                            "NET_WORTH" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Step1MiniMetricTile(
                                        label = "Bank & Visa",
                                        value = "59% • ৳1.42L",
                                        accent = Color(0xFF8B5CF6),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Step1MiniMetricTile(
                                        label = "bKash & Nagad",
                                        value = "22% • ৳51.9K",
                                        accent = Color(0xFFE2136E),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Step1MiniMetricTile(
                                        label = "Global FX ($/€)",
                                        value = "19% • $385",
                                        accent = Color(0xFF10B981),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            "CASHFLOW" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Income +84,500 ৳",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF34D399),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = "Expenses -32,140 ৳",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFFB7185),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = "Saved 61.9%",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF60A5FA),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    // Mini Sparkline Bar Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        listOf(0.45f, 0.62f, 0.38f, 0.78f, 0.56f, 0.92f, 0.84f).forEachIndexed { i, ratio ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight(ratio)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (i >= 5) Color(0xFF34D399) else Color(0xFF6D4AFF).copy(alpha = 0.75f)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }

                            else -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0084E9).copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = Color(0xFF60A5FA),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "100% Private Offline-First Architecture",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Zero cloud spying. Encrypted Room SQLite ledger + PIN lock + instant JSON/CSV/PDF audit export.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.76f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 3 Interactive Pillars Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OnboardingInteractivePillarTile(
            icon = Icons.Default.Public,
            title = "14+ ISO Currencies",
            subtitle = "৳, $, €, £, ₹, ¥, ر.س, د.إ",
            accent = Color(0xFF6D4AFF),
            modifier = Modifier.weight(1f)
        )
        OnboardingInteractivePillarTile(
            icon = Icons.Default.TrackChanges,
            title = "Smart Budgets",
            subtitle = "Daily to Yearly radar",
            accent = Color(0xFF059669),
            modifier = Modifier.weight(1f)
        )
        OnboardingInteractivePillarTile(
            icon = Icons.Default.Security,
            title = "Offline & Private",
            subtitle = "On-device SQLite vault",
            accent = Color(0xFF0084E9),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun Step1MiniMetricTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                fontFamily = SpaceGroteskFontFamily,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OnboardingInteractivePillarTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderStrong),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = colors.inkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
// STEP 2: FLOATING MULTI-WALLET & INTERNATIONAL ACCOUNT DECK
// ============================================================================

private data class DemoWalletCardSpec(
    val id: String,
    val name: String,
    val categoryTag: String,
    val currencyCode: String,
    val formattedBalance: String,
    val numberMask: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val highlightNote: String
)

private val demoWalletDeck = listOf(
    DemoWalletCardSpec(
        id = "bkash",
        name = "bKash Personal",
        categoryTag = "MFS WALLET • BDT ৳",
        currencyCode = "BDT",
        formattedBalance = "৳28,450.50",
        numberMask = "+880 17•• ••8921",
        primaryColor = Color(0xFFE2136E),
        secondaryColor = Color(0xFF9D0B4B),
        highlightNote = "Instant Send Money, Cash Out & Utility Bill Pay tracking"
    ),
    DemoWalletCardSpec(
        id = "wise",
        name = "Wise Global Borderless",
        categoryTag = "INTERNATIONAL • USD $",
        currencyCode = "USD",
        formattedBalance = "$1,285.00",
        numberMask = "ACH •••• 4419",
        primaryColor = Color(0xFF059669),
        secondaryColor = Color(0xFF065F46),
        highlightNote = "Preserves native $ USD balance with live BDT net-worth conversion"
    ),
    DemoWalletCardSpec(
        id = "brac",
        name = "BRAC Bank Signature",
        categoryTag = "COMMERCIAL BANK • BDT ৳",
        currencyCode = "BDT",
        formattedBalance = "৳1,42,500.00",
        numberMask = "VISA •••• 8821",
        primaryColor = Color(0xFF6D4AFF),
        secondaryColor = Color(0xFF3B1E9E),
        highlightNote = "Salary deposits, automated standing orders & dual-currency card spend"
    ),
    DemoWalletCardSpec(
        id = "nagad",
        name = "Nagad & Rocket MFS",
        categoryTag = "MFS WALLET • BDT ৳",
        currencyCode = "BDT",
        formattedBalance = "৳23,500.30",
        numberMask = "+880 18•• ••3310",
        primaryColor = Color(0xFFF37021),
        secondaryColor = Color(0xFFB44608),
        highlightNote = "Dedicated MFS presets for Nagad, Rocket & Upay with low cash-out fee tags"
    ),
    DemoWalletCardSpec(
        id = "revolut",
        name = "Revolut Euro Vault",
        categoryTag = "FOREIGN ACCOUNT • EUR €",
        currencyCode = "EUR",
        formattedBalance = "€940.00",
        numberMask = "IBAN •••• 7781",
        primaryColor = Color(0xFF0084E9),
        secondaryColor = Color(0xFF034687),
        highlightNote = "Hold €, £, ر.س or د.إ side by side without mixing currency symbols"
    )
)

@Composable
private fun Step2FloatingMultiWalletDeckScene(
    ambientPhase: Float
) {
    val colors = HisabTheme.colors
    val view = LocalView.current
    var activeCardIndex by remember { mutableIntStateOf(0) }
    val selectedWallet = demoWalletDeck[activeCardIndex]

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.2.dp, colors.borderStrong),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(selectedWallet.primaryColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = selectedWallet.primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Every Bank, Card & MFS Wallet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.inkPrimary
                    )
                    Text(
                        text = "Tap any institution chip or layered card below to inspect its live multi-currency telemetry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Institution Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                demoWalletDeck.forEachIndexed { idx, spec ->
                    val isSelected = idx == activeCardIndex
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) spec.primaryColor else colors.containerInteractive,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) spec.primaryColor else colors.borderStrong
                        ),
                        modifier = Modifier.clickable {
                            HisabHaptics.performClick(view)
                            activeCardIndex = idx
                        }
                    ) {
                        Text(
                            text = "${spec.name.substringBefore(" ")} (${spec.currencyCode})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) Color.White else colors.inkPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3D Layered Floating Card Deck Composition
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // Background Ghost Cards for 3D Stack Depth
                val backCard1 = demoWalletDeck[(activeCardIndex + 1) % demoWalletDeck.size]
                val backCard2 = demoWalletDeck[(activeCardIndex + 2) % demoWalletDeck.size]

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = backCard2.primaryColor.copy(alpha = 0.40f),
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .height(148.dp)
                        .offset(y = 34.dp)
                ) {}

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = backCard1.primaryColor.copy(alpha = 0.68f),
                    modifier = Modifier
                        .fillMaxWidth(0.93f)
                        .height(152.dp)
                        .offset(y = 18.dp)
                        .clickable {
                            HisabHaptics.performClick(view)
                            activeCardIndex = (activeCardIndex + 1) % demoWalletDeck.size
                        }
                ) {}

                // Foreground Active Wallet Card with Animated Transition
                AnimatedContent(
                    targetState = selectedWallet,
                    transitionSpec = {
                        (fadeIn(tween(240)) + scaleIn(initialScale = 0.94f, animationSpec = tween(240))) togetherWith
                            fadeOut(tween(180))
                    },
                    label = "activeWalletCard"
                ) { card ->
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(162.dp)
                            .clickable {
                                HisabHaptics.performClick(view)
                                activeCardIndex = (activeCardIndex + 1) % demoWalletDeck.size
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(card.primaryColor, card.secondaryColor)
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color.White.copy(alpha = 0.18f),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Text(
                                            text = card.categoryTag,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = card.numberMask,
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                Column {
                                    Text(
                                        text = card.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White.copy(alpha = 0.92f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = card.formattedBalance,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 28.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = card.highlightNote,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.9f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Tap card ↻",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Account Architecture Highlights
    OnboardingFeatureRowCard(
        icon = Icons.Default.CreditCard,
        accentColor = Color(0xFFE2136E),
        title = "1-Tap Bangladeshi & Global Presets",
        subtitle = "Pre-configured templates for bKash, Nagad, Rocket, BRAC/City/DBBL Banks, Wise, Revolut, and Cash."
    )
    OnboardingFeatureRowCard(
        icon = Icons.Default.CurrencyExchange,
        accentColor = Color(0xFF059669),
        title = "Native Per-Account ISO 4217 Currency",
        subtitle = "A USD account always stays in $ USD and a BDT wallet stays in ৳ BDT — zero forced symbol overrides."
    )
}

// ============================================================================
// STEP 3: INTERACTIVE INCOME, EXPENSE, CROSS-CURRENCY TRANSFER & RECEIPT OCR
// ============================================================================

@Composable
private fun Step3InteractiveFlowsAndOcrScene(
    ambientPhase: Float
) {
    val colors = HisabTheme.colors
    val view = LocalView.current

    // Interactive Transaction Flow Mode ("TRANSFER", "EXPENSE", "INCOME")
    var selectedFlowMode by remember { mutableStateOf("TRANSFER") }
    var ocrScanCount by remember { mutableIntStateOf(0) }

    val sampleReceipts = remember {
        listOf(
            Triple("Unimart Gulshan Superstore", "৳3,480.00 BDT", "Groceries & Dining • Paid via bKash"),
            Triple("Amazon Web Services EMEA", "$42.50 USD", "Cloud & Tech • Paid via Wise USD"),
            Triple("Qatar Airways Ticket", "€615.00 EUR", "Travel & Flights • Paid via Revolut EUR")
        )
    }
    val activeReceipt = sampleReceipts[ocrScanCount % sampleReceipts.size]

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.2.dp, colors.borderStrong),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Precision Cashflow, Transfers & Receipt OCR",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = colors.inkPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Switch between transaction modes below to see how HisabMate routes funds across accounts and currencies.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Mode Switcher Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("TRANSFER", "FX Transfer", Color(0xFF0084E9)),
                    Triple("EXPENSE", "Smart Expense", Color(0xFFE11D48)),
                    Triple("INCOME", "Salary / Inflow", Color(0xFF059669))
                ).forEach { (mode, label, color) ->
                    val isSelected = selectedFlowMode == mode
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) color else colors.containerInteractive,
                        border = BorderStroke(1.dp, if (isSelected) color else colors.borderStrong),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                HisabHaptics.performClick(view)
                                selectedFlowMode = mode
                            }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) Color.White else colors.inkPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Animated Flow Pipeline Visualization
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.containerInteractive,
                border = BorderStroke(1.dp, colors.borderStrong),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val flowSpec = when (selectedFlowMode) {
                        "TRANSFER" -> listOf(
                            "Wise Global ($)",
                            "-$150.00 USD",
                            "bKash Personal (৳)",
                            "+৳18,075.00 BDT",
                            "LIVE FX: 1 USD = ৳120.50"
                        ) to Color(0xFF0084E9)
                        "EXPENSE" -> listOf(
                            "bKash Personal (৳)",
                            "-৳2,450.00 BDT",
                            "Dining & Groceries",
                            "64% of Monthly Budget",
                            "AUTO-TAGGED RECEIPT"
                        ) to Color(0xFFE11D48)
                        else -> listOf(
                            "Remote Client ($)",
                            "+$1,200.00 USD",
                            "BRAC Bank / Wise",
                            "+৳1,44,600 Eqv.",
                            "RECURRING PAYROLL"
                        ) to Color(0xFF059669)
                    }
                    val (leftTitle, leftSub, rightTitle, rightSub, badgeText) = flowSpec.first
                    val tintColor = flowSpec.second

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = leftTitle as String,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.inkSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = leftSub as String,
                                fontFamily = SpaceGroteskFontFamily,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.inkPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Animated Canvas Particle Stream
                        Canvas(
                            modifier = Modifier
                                .width(56.dp)
                                .height(28.dp)
                        ) {
                            val cy = size.height / 2f
                            drawLine(
                                color = tintColor.copy(alpha = 0.28f),
                                start = Offset(4f, cy),
                                end = Offset(size.width - 4f, cy),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            val normalized = ((ambientPhase / (2f * PI.toFloat())) * 3f) % 1f
                            for (i in 0..2) {
                                val pos = ((normalized + i * 0.33f) % 1f) * (size.width - 12f) + 6f
                                drawCircle(
                                    color = tintColor,
                                    radius = 4.5.dp.toPx(),
                                    center = Offset(pos, cy)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = rightTitle as String,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.inkSecondary,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = rightSub as String,
                                fontFamily = SpaceGroteskFontFamily,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = tintColor,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = tintColor.copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = badgeText as String,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = tintColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }

    // Interactive Receipt OCR Scanner Simulator Card (BUG 2 Fixed: Responsive Header + Horizontal Single-Line "Test OCR" Button)
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.2.dp, colors.borderStrong),
        modifier = Modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val useStackedButtonLayout = maxWidth < 330.dp

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF6D4AFF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = Color(0xFF6D4AFF),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "On-Device Receipt OCR Scanner",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.inkPrimary
                            )
                            Text(
                                text = "Extracts merchant, amount & date offline",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.inkSecondary
                            )
                        }
                    }

                    if (!useStackedButtonLayout) {
                        Button(
                            onClick = {
                                HisabHaptics.performClick(view)
                                ocrScanCount++
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6D4AFF),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .widthIn(min = 108.dp)
                                .testTag("btn_test_ocr")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Test OCR",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                if (useStackedButtonLayout) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            HisabHaptics.performClick(view)
                            ocrScanCount++
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6D4AFF),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 42.dp)
                            .testTag("btn_test_ocr")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Test OCR",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.containerInteractive,
                    border = BorderStroke(1.dp, colors.borderStrong),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeReceipt.first,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.inkPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = activeReceipt.third,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.inkSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = activeReceipt.second,
                            fontFamily = SpaceGroteskFontFamily,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF6D4AFF),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// STEP 4: INTELLIGENT BUDGETS, CATEGORIES & LIVE THRESHOLD RADAR
// ============================================================================

@Composable
private fun Step4IntelligentBudgetsRadarScene(
    selectedCurrency: String
) {
    val colors = HisabTheme.colors
    val view = LocalView.current

    var selectedPeriod by remember { mutableStateOf("Monthly") }
    var simulatedRatio by remember { mutableFloatStateOf(0.68f) }

    val animatedRatio by animateFloatAsState(
        targetValue = simulatedRatio,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "budgetSimulatedRatio"
    )

    val statusColor = when {
        animatedRatio >= 1.0f -> Color(0xFFE11D48)
        animatedRatio >= 0.8f -> Color(0xFFD97706)
        else -> Color(0xFF059669)
    }

    val statusLabel = when {
        animatedRatio >= 1.0f -> "OVER BUDGET ALERT (${(animatedRatio * 100).roundToInt()}%)"
        animatedRatio >= 0.8f -> "80% THRESHOLD WARNING (${(animatedRatio * 100).roundToInt()}%)"
        else -> "HEALTHY SPEND PACE (${(animatedRatio * 100).roundToInt()}%)"
    }

    val baseLimit = when (selectedPeriod) {
        "Daily" -> 1200.0
        "Weekly" -> 8500.0
        "Monthly" -> 35000.0
        else -> 420000.0
    }
    val currentSpent = baseLimit * animatedRatio

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.2.dp, colors.borderStrong),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Proactive Budgets & Threshold Radar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = colors.inkPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Switch periods or drag the live spending simulator below to see how HisabMate triggers 80% warning and 100% limit alerts.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Period Selector Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Daily", "Weekly", "Monthly", "Yearly").forEach { period ->
                    val isSelected = selectedPeriod == period
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) statusColor else colors.containerInteractive,
                        border = BorderStroke(1.dp, if (isSelected) statusColor else colors.borderStrong),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                HisabHaptics.performClick(view)
                                selectedPeriod = period
                            }
                    ) {
                        Text(
                            text = period,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) Color.White else colors.inkPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Radial Gauge + Live Budget Telemetry Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(108.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 11.dp.toPx()
                        drawArc(
                            color = statusColor.copy(alpha = 0.16f),
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = statusColor,
                            startAngle = 135f,
                            sweepAngle = (270f * animatedRatio.coerceAtMost(1f)),
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(animatedRatio * 100).roundToInt()}%",
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = statusColor,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = selectedPeriod.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = CurrencyFormatter.formatCurrency(currentSpent, selectedCurrency),
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = colors.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "of ${CurrencyFormatter.formatCurrency(baseLimit, selectedCurrency)} $selectedPeriod Limit",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Spend Simulator Slider
            Slider(
                value = simulatedRatio,
                onValueChange = { simulatedRatio = it },
                valueRange = 0.20f..1.15f,
                colors = SliderDefaults.colors(
                    thumbColor = statusColor,
                    activeTrackColor = statusColor,
                    inactiveTrackColor = colors.containerInteractive
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Safe Pace (50%)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF059669),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.clickable { simulatedRatio = 0.50f }
                )
                Text(
                    text = "Warning (85%)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD97706),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.clickable { simulatedRatio = 0.85f }
                )
                Text(
                    text = "Limit Exceeded (108%)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE11D48),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.clickable { simulatedRatio = 1.08f }
                )
            }
        }
    }

    // Multi-Currency Category Examples
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OnboardingInteractivePillarTile(
            icon = Icons.Default.PieChart,
            title = "Dining & Bazar",
            subtitle = "৳12,400 / ৳18,000",
            accent = Color(0xFF059669),
            modifier = Modifier.weight(1f)
        )
        OnboardingInteractivePillarTile(
            icon = Icons.Default.Bolt,
            title = "Cloud & SaaS",
            subtitle = "$68.00 / $100.00",
            accent = Color(0xFF0084E9),
            modifier = Modifier.weight(1f)
        )
    }
}

// ============================================================================
// STEP 5: SAVINGS GOALS & INTERACTIVE DEBT / LENDING LEDGER
// ============================================================================

@Composable
private fun Step5GoalsAndDebtLedgerScene(
    selectedCurrency: String
) {
    val colors = HisabTheme.colors
    val view = LocalView.current

    var goalBoostClicks by remember { mutableIntStateOf(0) }
    var debtSettled by remember { mutableStateOf(false) }

    val goalProgress = (0.72f + goalBoostClicks * 0.07f).coerceAtMost(1.0f)
    val animatedGoalProgress by animateFloatAsState(
        targetValue = goalProgress,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "goalProgressAnim"
    )

    // Interactive Savings Goal Accelerator Card
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.2.dp, colors.borderStrong),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF7C3AED).copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "SAVINGS GOALS & MILESTONES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF7C3AED),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Emergency Reserve & Target Funds",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.inkPrimary
                    )
                }

                Button(
                    onClick = {
                        HisabHaptics.performClick(view)
                        if (goalBoostClicks < 4) goalBoostClicks++ else goalBoostClicks = 0
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7C3AED),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (animatedGoalProgress >= 1f) "Reset ↻" else "Boost +7%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Goal Progress Card (BUG 1 Fixed: Weighted left Column + Single-Line Horizontal Percentage Pill)
            val accentGoalColor = if (animatedGoalProgress >= 1f) Color(0xFF059669) else Color(0xFF7C3AED)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.containerInteractive,
                border = BorderStroke(1.dp, colors.borderStrong),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "6-Month Family Emergency Fund",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.inkPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (animatedGoalProgress >= 1f) {
                                    "100% Milestone Completed! 🎉"
                                } else {
                                    "Target: ৳2,00,000 BDT • Auto-saved monthly"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (animatedGoalProgress >= 1f) Color(0xFF059669) else colors.inkSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = accentGoalColor.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, accentGoalColor.copy(alpha = 0.35f)),
                            modifier = Modifier.widthIn(min = 66.dp)
                        ) {
                            Text(
                                text = "${(animatedGoalProgress * 100).roundToInt()}%",
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = accentGoalColor,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("txt_savings_goal_percentage")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { animatedGoalProgress },
                        color = accentGoalColor,
                        trackColor = colors.cardSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                    )
                }
            }
        }
    }

    // Interactive Debt, Lending & Recurring Subscriptions Card
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.2.dp, colors.borderStrong),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0891B2).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Handshake,
                        contentDescription = null,
                        tint = Color(0xFF0891B2),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Money Lent, Borrowed & Bills",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.inkPrimary
                    )
                    Text(
                        text = "Tap Settle below to test 1-tap debt collection",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Lent Record Row
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.containerInteractive,
                border = BorderStroke(
                    1.dp,
                    if (debtSettled) Color(0xFF059669) else colors.borderStrong
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lent to Tanvir Ahmed",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.inkPrimary
                        )
                        Text(
                            text = if (debtSettled) "Settled into Wise USD ($120.00)" else "Due in 3 days • $120.00 USD",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (debtSettled) Color(0xFF059669) else colors.inkSecondary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (debtSettled) Color(0xFF059669) else Color(0xFF0891B2),
                        modifier = Modifier.clickable {
                            HisabHaptics.performClick(view)
                            debtSettled = !debtSettled
                        }
                    ) {
                        Text(
                            text = if (debtSettled) "Settled ✓" else "Settle $120",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// STEP 6: GLOBAL MULTI-CURRENCY ENGINE, LIVE FX & BASE CURRENCY SELECTION
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step6GlobalMultiCurrencyEngineScene(
    selectedCurrency: String,
    selectedRegion: String,
    currencySearchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectCurrency: (com.example.util.CurrencyMetadata) -> Unit,
    onSelectRegion: (String) -> Unit
) {
    val colors = HisabTheme.colors
    val selectedMeta = remember(selectedCurrency) {
        CurrencyCatalog.getMetadata(selectedCurrency)
    }

    val filteredCurrencies = remember(currencySearchQuery) {
        val q = currencySearchQuery.trim().lowercase()
        if (q.isEmpty()) {
            CurrencyCatalog.supportedCurrencies
        } else {
            CurrencyCatalog.supportedCurrencies.filter {
                it.code.lowercase().contains(q) ||
                    it.name.lowercase().contains(q) ||
                    it.symbol.lowercase().contains(q) ||
                    it.countryOrRegion.lowercase().contains(q)
            }
        }
    }

    // Live Multi-Currency Conversion & Analytics Preview Header
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.2.dp, colors.borderStrong),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Authentic ISO 4217 Multi-Currency Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.inkPrimary
                    )
                    Text(
                        text = "Choose your default Base Currency below. Foreign accounts convert into ${selectedMeta.symbol} ${selectedMeta.code} for unified analytics.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0284C7).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF0284C7))
                ) {
                    Text(
                        text = "${selectedMeta.symbol} ${selectedMeta.code}",
                        fontFamily = SpaceGroteskFontFamily,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0284C7),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Responsive 2x2 Grid for Live FX Conversion Strip so currency amounts never crowd or wrap
            val fxSamples = remember {
                listOf(
                    Triple("BDT", 25000.0, Color(0xFFE2136E)),
                    Triple("USD", 250.0, Color(0xFF059669)),
                    Triple("EUR", 180.0, Color(0xFF6D4AFF)),
                    Triple("SAR", 500.0, Color(0xFFD97706))
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                fxSamples.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (code, sampleAmt, tint) ->
                            val srcMeta = CurrencyCatalog.getMetadata(code)
                            val amtInBdt = sampleAmt * srcMeta.defaultRateToBdt
                            val converted = amtInBdt / selectedMeta.defaultRateToBdt.coerceAtLeast(0.0001)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.containerInteractive,
                                border = BorderStroke(1.dp, tint.copy(alpha = 0.4f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = CurrencyFormatter.formatCurrency(sampleAmt, code),
                                            fontFamily = SpaceGroteskFontFamily,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colors.inkPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "≈ ${CurrencyFormatter.formatCurrency(converted, selectedCurrency)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = tint,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Searchable Base Currency Selector Card
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.2.dp, colors.borderStrong),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Region Number Grouping Quick Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "BD" to "South Asia (1,42,500)",
                    "US" to "Global (142,500)",
                    "GB" to "UK & Europe",
                    "SA" to "Gulf / MENA"
                ).forEach { (rCode, rLabel) ->
                    val isSelected = selectedRegion == rCode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF0284C7) else colors.containerInteractive,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF0284C7) else colors.borderStrong
                        ),
                        modifier = Modifier.clickable { onSelectRegion(rCode) }
                    ) {
                        Text(
                            text = rLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) Color.White else colors.inkPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = currencySearchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search currency code, symbol, or country (BDT, USD, EUR, SAR...)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search currency"
                    )
                },
                trailingIcon = {
                    if (currencySearchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_currency_search")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                filteredCurrencies.forEach { meta ->
                    val isSelected = meta.code.equals(selectedCurrency, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            Color(0xFF0284C7).copy(alpha = if (colors.isDark) 0.22f else 0.12f)
                        } else {
                            colors.containerInteractive
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF0284C7) else colors.borderStrong
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCurrency(meta) }
                            .testTag("onboard_curr_${meta.code}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(0xFF0284C7) else colors.cardSurface
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = meta.symbol,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = if (isSelected) Color.White else colors.inkPrimary
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${meta.code} • ${meta.name}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.inkPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${meta.countryOrRegion} • ${CurrencyFormatter.formatCurrency(1250.0, meta.code)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.inkSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected currency",
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// STEP 7: SMART NOTIFICATION PREVIEW STACK & OPTIONAL PERMISSION ACTION
// ============================================================================

private data class DemoNotificationCardSpec(
    val channelBadge: String,
    val title: String,
    val body: String,
    val timeAgo: String,
    val accentColor: Color,
    val icon: ImageVector
)

@Composable
private fun Step7SmartNotificationStackScene(
    selectedCurrency: String,
    notificationsEnabled: Boolean,
    permissionRequestedOrGranted: Boolean,
    permissionStatusNote: String?,
    onToggleNotificationsEnabled: (Boolean) -> Unit,
    onRequestPermission: () -> Unit
) {
    val colors = HisabTheme.colors
    val view = LocalView.current
    val chosenMeta = remember(selectedCurrency) {
        CurrencyCatalog.getMetadata(selectedCurrency)
    }

    var highlightedAlertIndex by remember { mutableIntStateOf(0) }
    var billRemindersOn by remember { mutableStateOf(true) }
    var budgetAlertsOn by remember { mutableStateOf(true) }
    var debtRemindersOn by remember { mutableStateOf(true) }

    val demoAlerts = remember(selectedCurrency) {
        listOf(
            DemoNotificationCardSpec(
                channelBadge = "RECURRING BILL REMINDER",
                title = "Carnival Fiber Internet Due Tomorrow",
                body = "Auto-pay reminder • ${CurrencyFormatter.formatCurrency(1500.0, selectedCurrency)} from bKash Personal.",
                timeAgo = "Just now",
                accentColor = Color(0xFF0084E9),
                icon = Icons.Default.EventRepeat
            ),
            DemoNotificationCardSpec(
                channelBadge = "80% BUDGET THRESHOLD ALERT",
                title = "Dining & Groceries reached 82% of Limit",
                body = "You have ${CurrencyFormatter.formatCurrency(3240.0, selectedCurrency)} remaining for the rest of this month.",
                timeAgo = "2m ago",
                accentColor = Color(0xFFD97706),
                icon = Icons.Default.TrackChanges
            ),
            DemoNotificationCardSpec(
                channelBadge = "DEBT REPAYMENT REMINDER",
                title = "Collect $120.00 USD from Tanvir Ahmed",
                body = "Scheduled settlement due in 2 days • Tap to record partial or full payment.",
                timeAgo = "15m ago",
                accentColor = Color(0xFF059669),
                icon = Icons.Default.Payments
            )
        )
    }

    // Animated Notification Stack Showcase Card
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.2.dp, colors.borderStrong),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Proactive Local Financial Alerts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.inkPrimary
                    )
                    Text(
                        text = "100% offline-scheduled notifications for bills, budget thresholds & debt due dates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF059669).copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.45f)),
                    modifier = Modifier.clickable {
                        HisabHaptics.performClick(view)
                        highlightedAlertIndex = (highlightedAlertIndex + 1) % demoAlerts.size
                    }
                ) {
                    Text(
                        text = "Cycle Alert ↻",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF059669),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Stacked Notification Cards
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                demoAlerts.forEachIndexed { idx, spec ->
                    val isFocused = idx == highlightedAlertIndex
                    val scaleVal by animateFloatAsState(
                        targetValue = if (isFocused) 1.0f else 0.97f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "notifScale_$idx"
                    )

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isFocused) {
                            spec.accentColor.copy(alpha = if (colors.isDark) 0.20f else 0.10f)
                        } else {
                            colors.containerInteractive
                        },
                        border = BorderStroke(
                            width = if (isFocused) 1.5.dp else 1.dp,
                            color = if (isFocused) spec.accentColor else colors.borderStrong
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scaleVal)
                            .clickable {
                                HisabHaptics.performClick(view)
                                highlightedAlertIndex = idx
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(spec.accentColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = spec.icon,
                                    contentDescription = null,
                                    tint = spec.accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = spec.channelBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = spec.accentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = spec.timeAgo,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.inkTertiary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = spec.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.inkPrimary
                                )
                                Text(
                                    text = spec.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.inkSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Channel Toggles + Optional Non-Blocking Permission Action Card
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.2.dp, colors.borderStrong),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Smart Local Reminders",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.inkPrimary
                    )
                    Text(
                        text = "Optional — you can also configure or mute alerts anytime in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkSecondary
                    )
                }
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = onToggleNotificationsEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF059669)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Channel Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Step7ChannelChip(
                    label = "Bills",
                    checked = billRemindersOn && notificationsEnabled,
                    onClick = { billRemindersOn = !billRemindersOn },
                    modifier = Modifier.weight(1f)
                )
                Step7ChannelChip(
                    label = "Budgets",
                    checked = budgetAlertsOn && notificationsEnabled,
                    onClick = { budgetAlertsOn = !budgetAlertsOn },
                    modifier = Modifier.weight(1f)
                )
                Step7ChannelChip(
                    label = "Debts",
                    checked = debtRemindersOn && notificationsEnabled,
                    onClick = { debtRemindersOn = !debtRemindersOn },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!permissionRequestedOrGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                OutlinedButton(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, Color(0xFF059669)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_request_notification_permission")
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Grant Android Notification Permission (Optional)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF059669),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF059669).copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Ready to launch with ${chosenMeta.symbol} ${chosenMeta.code} as your Base Currency",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF059669)
                        )
                    }
                }
            }

            permissionStatusNote?.let { note ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkSecondary
                )
            }
        }
    }
}

@Composable
private fun Step7ChannelChip(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (checked) Color(0xFF059669).copy(alpha = 0.15f) else colors.containerInteractive,
        border = BorderStroke(
            1.dp,
            if (checked) Color(0xFF059669) else colors.borderStrong
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = if (checked) Color(0xFF059669) else colors.inkSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (checked) Color(0xFF059669) else colors.inkSecondary,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun OnboardingFeatureRowCard(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    subtitle: String
) {
    val colors = HisabTheme.colors
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderStrong),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = colors.inkSecondary
                )
            }
        }
    }
}
