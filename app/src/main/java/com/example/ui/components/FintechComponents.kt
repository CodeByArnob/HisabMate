package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionEntity
import com.example.ui.theme.AmberGoldDark
import com.example.ui.theme.HisabTheme
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.getCategoryPalette
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.CategorySpendSummary
import com.example.ui.viewmodel.DailySpendPoint
import com.example.ui.viewmodel.MonthlyCashflowPoint
import com.example.util.CurrencyCatalog
import com.example.util.CurrencyFormatter

fun resolveCategoryIcon(iconKey: String): ImageVector {
    return when (iconKey.uppercase()) {
        "GROCERIES" -> Icons.Default.ShoppingBag
        "TRANSPORT" -> Icons.Default.DirectionsBus
        "ENTERTAINMENT" -> Icons.Default.Movie
        "RENT" -> Icons.Default.Home
        "DINING" -> Icons.Default.LocalCafe
        "SHOPPING" -> Icons.Default.Checkroom
        "HEALTH" -> Icons.Default.MedicalServices
        "SALARY" -> Icons.Default.Payments
        "FREELANCE" -> Icons.Default.Work
        "BANK" -> Icons.Default.AccountBalance
        "MFS" -> Icons.Default.Smartphone
        "CARD" -> Icons.Default.CreditCard
        "CASH" -> Icons.Default.AccountBalanceWallet
        "TRANSFER" -> Icons.AutoMirrored.Filled.CompareArrows
        "SAVINGS" -> Icons.Default.Savings
        "TECH" -> Icons.Default.LaptopMac
        "TRAVEL" -> Icons.Default.FlightTakeoff
        "FAMILY" -> Icons.Default.Favorite
        "FX" -> Icons.Default.CurrencyExchange
        else -> Icons.AutoMirrored.Filled.TrendingUp
    }
}

/**
 * Centralized Top Header Surface with crisp structural border and contrast separation.
 */
@Composable
fun HisabHeaderSurface(
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 28.dp,
    horizontalOuterPadding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = HisabTheme.colors
    val shape = if (horizontalOuterPadding > 0.dp) {
        RoundedCornerShape(shapeRadius)
    } else {
        RoundedCornerShape(bottomStart = shapeRadius, bottomEnd = shapeRadius)
    }
    Surface(
        color = colors.cardSurface,
        shape = shape,
        shadowElevation = if (colors.isDark) 4.dp else 3.dp,
        border = BorderStroke(1.dp, colors.borderStrong),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalOuterPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            content = content
        )
    }
}

/**
 * Centralized Section Card with explicit border (`#A6B7CC` in Light, `#2A3449` in Dark)
 * so cards never blend into the background canvas.
 */
@Composable
fun HisabSectionCard(
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 28.dp,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 18.dp,
    shadowElevation: Dp = 3.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = HisabTheme.colors
    val shape = RoundedCornerShape(shapeRadius)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            color = colors.cardSurface,
            shape = shape,
            shadowElevation = if (colors.isDark) 3.5.dp else shadowElevation,
            border = BorderStroke(1.1.dp, colors.borderStrong),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                content = content
            )
        }
    } else {
        Surface(
            color = colors.cardSurface,
            shape = shape,
            shadowElevation = if (colors.isDark) 3.5.dp else shadowElevation,
            border = BorderStroke(1.1.dp, colors.borderStrong),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                content = content
            )
        }
    }
}

/**
 * Prominent Elevated Financial Panel for the Main Balance Card.
 * Uses a subtle surface gradient (Light: #FFFFFF to #F4F7FB, Dark: #1B2332 to #141923),
 * refined 1.2.dp border, and calibrated elevation for distinctive dimensional depth.
 */
@Composable
fun HisabBalanceHeroCard(
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 28.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = HisabTheme.colors
    val shape = RoundedCornerShape(shapeRadius)
    val backgroundBrush = if (colors.isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1B2332),
                Color(0xFF141923)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF4F7FB)
            )
        )
    }

    Surface(
        shape = shape,
        color = Color.Transparent,
        shadowElevation = if (colors.isDark) 4.5.dp else 4.dp,
        border = BorderStroke(1.2.dp, colors.borderStrong),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundBrush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                content = content
            )
        }
    }
}

/**
 * Interactive Base Currency Badge Pill for headers.
 */
@Composable
fun BaseCurrencyBadgeButton(
    currencyCode: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "btn_base_currency_selector"
) {
    val meta = CurrencyCatalog.getMetadata(currencyCode)
    val colors = HisabTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = colors.containerInteractive,
        border = BorderStroke(1.dp, colors.borderStrong),
        modifier = modifier
            .height(38.dp)
            .minimumInteractiveComponentSize()
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 11.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = meta.symbol,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
            Text(
                text = meta.code,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.5.sp,
                color = colors.inkPrimary
            )
        }
    }
}

/**
 * Hero Currency Amount Display supporting all ISO 4217 currencies with full Unicode symbol glyph support.
 */
@Composable
fun StylizedBdtAmount(
    amount: Double,
    useBangladeshiFormat: Boolean,
    isHidden: Boolean = false,
    currencyCode: String = "BDT",
    mainFontSize: TextUnit = 34.sp,
    symbolFontSize: TextUnit = 19.sp,
    decimalFontSize: TextUnit = 23.sp,
    textColor: Color = HisabTheme.colors.inkPrimary,
    modifier: Modifier = Modifier
) {
    val meta = CurrencyCatalog.getMetadata(currencyCode)
    if (isHidden) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = modifier
        ) {
            Text(
                text = meta.symbol,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = symbolFontSize,
                color = textColor,
                modifier = Modifier.padding(top = 3.dp, end = 4.dp)
            )
            Text(
                text = "••••••",
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = mainFontSize,
                color = textColor,
                maxLines = 1
            )
        }
        return
    }

    val (symbolStr, wholeStr, decStr) = CurrencyFormatter.splitCurrencyParts(
        amount = amount,
        currencyCode = currencyCode,
        useSouthAsianGrouping = useBangladeshiFormat
    )

    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
    ) {
        Text(
            text = symbolStr,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = symbolFontSize,
            color = textColor,
            modifier = Modifier.padding(top = 3.dp, end = 4.dp)
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = wholeStr,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = mainFontSize,
                letterSpacing = (-0.7).sp,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (decStr.isNotEmpty()) {
                Text(
                    text = decStr,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = decimalFontSize,
                    letterSpacing = (-0.3).sp,
                    color = textColor.copy(alpha = 0.90f),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Signature Stacked Wallet Folder Card with vibrant solid card colors, balanced proportions,
 * crisp icon alignment, readable category names, non-truncated amounts, and visual progress bar.
 */
@Composable
fun WalletFolderCard(
    summary: CategorySpendSummary,
    baseCurrencyCode: String,
    useBangladeshiFormat: Boolean,
    isBalanceHidden: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = getCategoryPalette(summary.category.colorHex)
    val icon = resolveCategoryIcon(summary.category.iconKey)
    val colors = HisabTheme.colors
    val progressRatio = summary.budgetProgress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .height(162.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .testTag("wallet_folder_${summary.category.id}")
    ) {
        // Outer Back Rim / Handle Arch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(palette.rim)
        ) {
            // Inner slot cutout strip matching card surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(colors.cardSurface)
            )
        }

        // Front Pocket Body (Crisp vibrant solid color)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(top = 17.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(palette.main)
                .padding(horizontal = 15.dp, vertical = 13.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row: Circular icon + high-contrast percentage indicator pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.24f))
                            .border(1.dp, Color.White.copy(alpha = 0.34f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = summary.category.name,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    if (summary.budgetLimitInBase > 0) {
                        val pct = (summary.budgetProgress * 100).toInt().coerceAtMost(999)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.28f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.5.sp,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Bottom section: Category Name + Formatted Spending Amount + Progress Indicator
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = summary.category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.5.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.96f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (isBalanceHidden) {
                        val sym = CurrencyCatalog.getMetadata(baseCurrencyCode).symbol
                        Text(
                            text = "$sym ••••",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            color = Color.White,
                            maxLines = 1
                        )
                    } else {
                        val (sym, whole, dec) = CurrencyFormatter.splitCurrencyParts(
                            amount = summary.totalSpentInBase,
                            currencyCode = baseCurrencyCode,
                            useSouthAsianGrouping = useBangladeshiFormat
                        )
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = sym,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.95f),
                                modifier = Modifier.padding(top = 2.dp, end = 3.dp)
                            )
                            Text(
                                text = "$whole$dec",
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.5.sp,
                                letterSpacing = (-0.4).sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(7.dp))
                    // Subtle integrated budget utilization bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.26f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressRatio.coerceAtLeast(0.06f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Miniature 3D Wallet Folder Graphic for the Wallet Details header.
 */
@Composable
fun MiniWalletFolderIcon(
    colorHex: Long,
    iconKey: String,
    modifier: Modifier = Modifier
) {
    val palette = getCategoryPalette(colorHex)
    val icon = resolveCategoryIcon(iconKey)
    val colors = HisabTheme.colors

    Box(
        modifier = modifier
            .width(74.dp)
            .height(58.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(palette.rim)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .padding(top = 5.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                    .background(colors.cardSurface)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 11.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.main)
                .padding(horizontal = 8.dp, vertical = 7.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .width(24.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.rim)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * Responsive Pill Action Button (Add, Move, Send, Details) for the Upper Balance Card.
 */
@Composable
fun CircularQuickActionButton(
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(22.dp),
            color = colors.containerInteractive,
            border = BorderStroke(1.dp, colors.borderStrong),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .minimumInteractiveComponentSize()
                .testTag(testTag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = colors.inkPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = colors.inkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Interactive Daily Spend Line Chart with high-contrast grid & multi-currency tooltip.
 */
@Composable
fun InteractiveSpendLineChart(
    points: List<DailySpendPoint>,
    accentColor: Color,
    useBangladeshiFormat: Boolean,
    currencyCode: String = "BDT",
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val colors = HisabTheme.colors
    var selectedIndex by remember(points) {
        mutableIntStateOf((points.size / 2).coerceIn(0, points.lastIndex))
    }

    var appearProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(points) {
        appearProgress = 1f
    }
    val animatedScale by animateFloatAsState(
        targetValue = appearProgress,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "chartScale"
    )

    val maxAmount = (points.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(10.0) * 1.25
    val gridColor = colors.borderStrong
    val selectedPoint = points[selectedIndex.coerceIn(0, points.lastIndex)]

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(185.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val stepX = size.width.toFloat() / (points.size - 1).coerceAtLeast(1)
                        val tappedIdx = ((offset.x + stepX / 2f) / stepX)
                            .toInt()
                            .coerceIn(0, points.lastIndex)
                        selectedIndex = tappedIdx
                    }
                }
                .testTag("interactive_spend_chart")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height - 16.dp.toPx()
                val topPad = 16.dp.toPx()
                val usableHeight = (h - topPad).coerceAtLeast(1f)

                for (i in 0..3) {
                    val y = topPad + (usableHeight / 3f) * i
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val stepX = w / (points.size - 1).coerceAtLeast(1)
                val selX = selectedIndex * stepX

                val bandHalfWidth = (stepX * 1.15f).coerceAtLeast(18.dp.toPx())
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.04f),
                            accentColor.copy(alpha = 0.18f),
                            accentColor.copy(alpha = 0.28f)
                        ),
                        startY = topPad,
                        endY = h
                    ),
                    topLeft = Offset((selX - bandHalfWidth).coerceAtLeast(0f), topPad),
                    size = Size(
                        width = (bandHalfWidth * 2f).coerceAtMost(w - (selX - bandHalfWidth).coerceAtLeast(0f)),
                        height = usableHeight
                    )
                )

                drawLine(
                    color = accentColor,
                    start = Offset(selX, topPad),
                    end = Offset(selX, h),
                    strokeWidth = 1.8.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                val linePath = Path()
                points.forEachIndexed { idx, pt ->
                    val x = idx * stepX
                    val normalized = (pt.amount / maxAmount).toFloat().coerceIn(0.05f, 0.95f) * animatedScale
                    val y = h - (normalized * usableHeight)
                    if (idx == 0) {
                        linePath.moveTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                    }
                }

                drawPath(
                    path = linePath,
                    color = accentColor,
                    style = Stroke(
                        width = 2.8.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                val selNorm = (selectedPoint.amount / maxAmount).toFloat().coerceIn(0.05f, 0.95f) * animatedScale
                val selY = h - (selNorm * usableHeight)

                drawCircle(
                    color = accentColor,
                    radius = 7.dp.toPx(),
                    center = Offset(selX, selY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.5.dp.toPx(),
                    center = Offset(selX, selY)
                )
            }

            val fraction = selectedIndex.toFloat() / (points.size - 1).coerceAtLeast(1)
            val alignmentBias = when {
                fraction < 0.28f -> Alignment.TopStart
                fraction > 0.72f -> Alignment.TopEnd
                else -> Alignment.TopCenter
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(alignmentBias)
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = CurrencyFormatter.formatCurrency(
                            amount = selectedPoint.amount,
                            currencyCode = currencyCode,
                            useSouthAsianGrouping = useBangladeshiFormat
                        ),
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = selectedPoint.fullDateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = points.first().dayLabel,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.inkSecondary
            )
            Text(
                text = selectedPoint.dayLabel,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = colors.inkPrimary
            )
            Text(
                text = points.last().dayLabel,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.inkSecondary
            )
        }
    }
}

/**
 * Proportional Multi-Segment Horizontal Pill Bar.
 */
@Composable
fun MultiSegmentSpendBar(
    summaries: List<CategorySpendSummary>,
    modifier: Modifier = Modifier
) {
    val active = summaries.filter { it.totalSpentInBase > 0.0 }.take(5)
    if (active.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(22.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        active.forEachIndexed { index, item ->
            val weight = item.shareOfSpend.coerceAtLeast(0.10f)
            val shape = when {
                active.size == 1 -> RoundedCornerShape(50)
                index == 0 -> RoundedCornerShape(
                    topStart = 50.dp,
                    bottomStart = 50.dp,
                    topEnd = 6.dp,
                    bottomEnd = 6.dp
                )
                index == active.lastIndex -> RoundedCornerShape(
                    topStart = 6.dp,
                    bottomStart = 6.dp,
                    topEnd = 50.dp,
                    bottomEnd = 50.dp
                )
                else -> RoundedCornerShape(6.dp)
            }
            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(Color(item.category.colorHex))
            )
        }
    }
}

/**
 * Smart Category Callout Banner with strong WCAG AA contrast border & typography.
 */
@Composable
fun SmartCategoryBanner(
    title: String = "Smart category",
    description: String = "We've categorized your transactions automatically. Tap any transaction to reassign or fine-tune budgets.",
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.insightBannerBg,
        border = BorderStroke(1.dp, colors.insightBannerBorder),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onActionClick != null) Modifier.clickable { onActionClick() } else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmberGoldDark.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = if (colors.isDark) Color(0xFFFBBF24) else AmberGoldDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colors.inkSecondary
            )
        }
    }
}

/**
 * Monthly Income vs Expense Comparison Bar Chart with distinct colors and high-contrast labels.
 */
@Composable
fun MonthlyCashflowBarChart(
    points: List<MonthlyCashflowPoint>,
    useBangladeshiFormat: Boolean,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val colors = HisabTheme.colors
    val maxVal = (points.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1.0).coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(142.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            points.forEach { pt ->
                val incFraction = (pt.income / maxVal).toFloat().coerceIn(0.14f, 1f)
                val expFraction = (pt.expense / maxVal).toFloat().coerceIn(0.12f, 1f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.height(110.dp)
                    ) {
                        // Income Bar
                        Box(
                            modifier = Modifier
                                .width(11.dp)
                                .fillMaxHeight(incFraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (pt.isCurrent) Color(0xFF059669)
                                    else Color(0xFF059669).copy(alpha = 0.65f)
                                )
                        )
                        // Expense Bar
                        Box(
                            modifier = Modifier
                                .width(11.dp)
                                .fillMaxHeight(expFraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (pt.isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.62f)
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = pt.monthShort,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (pt.isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (pt.isCurrent) colors.inkPrimary else colors.inkSecondary
                    )
                }
            }
        }
    }
}

/**
 * High-Contrast Transaction Row Item with Multi-Currency Conversion Display.
 * Preserves and displays the original currency & amount whenever it differs from [baseCurrencyCode].
 */
@Composable
fun TransactionRowItem(
    transaction: TransactionEntity,
    useBangladeshiFormat: Boolean,
    isBalanceHidden: Boolean = false,
    baseCurrencyCode: String = "BDT",
    ratesToBdtMap: Map<String, Double> = emptyMap(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors
    val palette = getCategoryPalette(transaction.categoryColorHex)
    val icon = resolveCategoryIcon(transaction.categoryIconKey)
    val isIncome = transaction.type == "INCOME"
    val isTransfer = transaction.type == "TRANSFER"

    val isForeignCurrency = !transaction.currencyCode.equals(baseCurrencyCode, ignoreCase = true)
    val convertedAmount = if (isForeignCurrency && ratesToBdtMap.isNotEmpty()) {
        CurrencyFormatter.convert(
            amount = transaction.amount,
            fromCurrency = transaction.currencyCode,
            toCurrency = baseCurrencyCode,
            ratesToBdt = ratesToBdtMap
        )
    } else {
        transaction.amount
    }
    val displayCurrencyCode = if (ratesToBdtMap.isNotEmpty()) baseCurrencyCode else transaction.currencyCode

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag("tx_item_${transaction.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (colors.isDark) palette.softBgDark else palette.softBgLight)
                    .border(1.2.dp, palette.main.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = transaction.categoryName,
                    tint = if (colors.isDark) palette.main else palette.highContrastTextLight,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.padding(end = 10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 15.5.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isForeignCurrency) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.containerInteractive,
                            border = BorderStroke(1.dp, colors.borderStrong)
                        ) {
                            Text(
                                text = transaction.currencyCode,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.inkPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                val extraTags = buildList {
                    if (transaction.isFavorite) add("★ Fav")
                    if (transaction.receiptUri.isNotBlank()) add("Receipt")
                    if (transaction.splitDetails.isNotBlank()) add("Split")
                }.joinToString(" • ")
                val subtitleStr = if (extraTags.isNotEmpty()) {
                    "${CurrencyFormatter.formatDateShort(transaction.timestamp)} • ${transaction.categoryName} • $extraTags"
                } else {
                    "${CurrencyFormatter.formatDateShort(transaction.timestamp)} • ${transaction.categoryName}"
                }
                Text(
                    text = subtitleStr,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.inkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            val formattedPrimary = if (isBalanceHidden) {
                "${CurrencyCatalog.getMetadata(displayCurrencyCode).symbol} ••••"
            } else {
                val raw = CurrencyFormatter.formatCurrency(
                    amount = convertedAmount,
                    currencyCode = displayCurrencyCode,
                    useSouthAsianGrouping = useBangladeshiFormat
                )
                when {
                    isIncome -> "+$raw"
                    isTransfer -> "⇄ $raw"
                    else -> "-$raw"
                }
            }

            Text(
                text = formattedPrimary,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                maxLines = 1,
                color = when {
                    isIncome -> colors.incomeText
                    isTransfer -> colors.inkPrimary
                    else -> colors.inkPrimary
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (isForeignCurrency && !isBalanceHidden) {
                val origFormatted = CurrencyFormatter.formatCurrency(
                    amount = transaction.amount,
                    currencyCode = transaction.currencyCode,
                    useSouthAsianGrouping = false,
                    includeCodeSuffix = true
                )
                Text(
                    text = "Orig: $origFormatted",
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = transaction.accountLabel,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.inkSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Floating Obsidian Pill Bottom Navigation Dock.
 */
@Composable
fun FloatingFintechDock(
    currentScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HisabTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(36.dp),
            color = colors.dockBackground,
            shadowElevation = 16.dp,
            border = BorderStroke(1.2.dp, if (colors.isDark) Color(0xFF2E3A50) else Color(0xFF334155)),
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DockIconButton(
                    selected = currentScreen == AppScreen.DASHBOARD || currentScreen == AppScreen.WALLET_DETAILS,
                    activeIcon = Icons.Filled.Home,
                    inactiveIcon = Icons.Outlined.Home,
                    contentDesc = "Dashboard",
                    testTag = "nav_dashboard",
                    onClick = { onSelectScreen(AppScreen.DASHBOARD) }
                )

                DockIconButton(
                    selected = currentScreen == AppScreen.PLANNING_HUB,
                    activeIcon = Icons.Filled.AccountBalanceWallet,
                    inactiveIcon = Icons.Outlined.AccountBalanceWallet,
                    contentDesc = "Budgets & Planning",
                    testTag = "nav_planning",
                    onClick = { onSelectScreen(AppScreen.PLANNING_HUB) }
                )

                Surface(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .width(68.dp)
                        .height(46.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("nav_add_transaction")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Transaction",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                DockIconButton(
                    selected = currentScreen == AppScreen.SPEND_ANALYSIS,
                    activeIcon = Icons.Filled.PieChart,
                    inactiveIcon = Icons.Outlined.PieChart,
                    contentDesc = "Spend Analysis",
                    testTag = "nav_analytics",
                    onClick = { onSelectScreen(AppScreen.SPEND_ANALYSIS) }
                )

                DockIconButton(
                    selected = currentScreen == AppScreen.ACCOUNTS ||
                        currentScreen == AppScreen.PROFILE_SETTINGS ||
                        currentScreen == AppScreen.CURRENCY_MANAGER,
                    activeIcon = Icons.Filled.Settings,
                    inactiveIcon = Icons.Outlined.Settings,
                    contentDesc = "Accounts & Settings",
                    testTag = "nav_accounts_settings",
                    onClick = { onSelectScreen(AppScreen.ACCOUNTS) }
                )
            }
        }
    }
}

@Composable
private fun DockIconButton(
    selected: Boolean,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    contentDesc: String,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (selected) Color.White.copy(alpha = 0.20f) else Color.Transparent)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (selected) activeIcon else inactiveIcon,
            contentDescription = contentDesc,
            tint = if (selected) Color.White else Color.White.copy(alpha = 0.72f),
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Top Circular Icon Button with explicit border and high-contrast icon tint.
 */
@Composable
fun HeaderCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    badgeCount: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val colors = HisabTheme.colors
    Box(modifier = modifier) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = colors.containerInteractive,
            border = BorderStroke(1.dp, colors.borderStrong),
            modifier = Modifier
                .size(size)
                .minimumInteractiveComponentSize()
                .testTag(testTag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = colors.inkPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDC2626))
                    .border(1.5.dp, colors.cardSurface, CircleShape)
            )
        }
    }
}
