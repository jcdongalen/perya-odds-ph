package com.jcdongalen.peryaodds.android.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Red suits and black suits available
private val RED_SUITS   = listOf("hearts", "diamonds")
private val BLACK_SUITS = listOf("spades", "clubs")

/**
 * Randomly assigns a suit to each outcome.
 * Exactly half (rounded down) get red suits, the rest get black suits.
 * For 6 outcomes: 3 red, 3 black.
 */
fun assignSuits(outcomes: List<String>): Map<String, String> {
    if (outcomes.isEmpty()) return emptyMap()
    val shuffled = outcomes.shuffled()
    val half = outcomes.size / 2
    return shuffled.mapIndexed { index, outcome ->
        val suit = if (index < half) RED_SUITS.random() else BLACK_SUITS.random()
        outcome to suit
    }.toMap()
}

/**
 * Maps a card outcome name + suit to its drawable resource ID.
 */
fun cardDrawableResId(context: android.content.Context, outcome: String, suit: String): Int? {
    val suffix = when (outcome.lowercase()) {
        "ace"   -> "a"
        "king"  -> "k"
        "queen" -> "q"
        "jack"  -> "j"
        else    -> outcome.lowercase()
    }
    val resId = context.resources.getIdentifier("${suit}_$suffix", "drawable", context.packageName)
    return if (resId != 0) resId else null
}

/** Formats "Ace" + "hearts" → "Ace of Hearts" */
private fun cardLabel(outcome: String, suit: String): String =
    "${outcome.replaceFirstChar { it.uppercase() }} of ${suit.replaceFirstChar { it.uppercase() }}"

@Composable
fun CardSelector(
    outcomes: List<String>,
    selectedCards: List<String>,
    suitMap: Map<String, String>,
    onSelectionChange: (List<String>) -> Unit,
    maxSelection: Int = 3,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(outcomes) { card ->
            val count  = selectedCards.count { it == card }
            val canAdd = selectedCards.size < maxSelection
            val suit   = suitMap[card] ?: "hearts"

            CardButton(
                label      = cardLabel(card, suit),
                imageResId = cardDrawableResId(context, card, suit),
                suit       = suit,
                selectionCount = count,
                canAdd     = canAdd,
                onAdd      = { if (canAdd) onSelectionChange(selectedCards + card) }
            )
        }
    }
}

@Composable
private fun CardButton(
    label: String,
    imageResId: Int?,
    suit: String,
    selectionCount: Int,
    canAdd: Boolean,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected  = selectionCount > 0
    val isRed       = suit in RED_SUITS
    val borderColor = when {
        isSelected && isRed  -> Color(0xFFE53935)
        isSelected && !isRed -> Color(0xFF1565C0)
        else                 -> Color.Transparent
    }
    val overlayGradient = if (isSelected) {
        if (isRed)
            Brush.verticalGradient(listOf(Color(0x00E53935), Color(0xCCE53935)))
        else
            Brush.verticalGradient(listOf(Color(0x001565C0), Color(0xCC1565C0)))
    } else {
        Brush.verticalGradient(listOf(Color(0x00000000), Color(0x99000000)))
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.69f),          // standard playing card ratio
            shape = MaterialTheme.shapes.medium,
            border = if (isSelected)
                androidx.compose.foundation.BorderStroke(3.dp, borderColor)
            else null,
            onClick = onAdd,
            enabled = canAdd,
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isSelected) 8.dp else 2.dp
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Full-bleed card image
                if (imageResId != null) {
                    Image(
                        painter        = painterResource(id = imageResId),
                        contentDescription = label,
                        contentScale   = ContentScale.Crop,
                        modifier       = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback solid background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }

                // Gradient scrim + label at the bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.38f)
                        .align(Alignment.BottomCenter)
                        .background(overlayGradient),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text       = label,
                        style      = MaterialTheme.typography.labelMedium.copy(
                            fontWeight  = FontWeight.SemiBold,
                            fontStyle   = FontStyle.Italic,
                            fontSize    = 11.sp,
                            letterSpacing = 0.3.sp
                        ),
                        color      = Color.White,
                        textAlign  = TextAlign.Center,
                        modifier   = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Count badge — top-right corner
        if (isSelected) {
            Badge(
                modifier       = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp),
                containerColor = borderColor
            ) {
                Text(
                    text  = selectionCount.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
