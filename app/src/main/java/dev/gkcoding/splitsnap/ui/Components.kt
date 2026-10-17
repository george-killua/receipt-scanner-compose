package dev.gkcoding.splitsnap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gkcoding.splitsnap.domain.Money
import dev.gkcoding.splitsnap.domain.Person

private val PersonColors = listOf(
    Color(0xFFE4572E),
    Color(0xFF2A9D8F),
    Color(0xFF7B5EA7),
    Color(0xFFE9A23B),
    Color(0xFF3D7DD8),
    Color(0xFFD64F8C),
    Color(0xFF5B8C3A),
    Color(0xFF8D6E63),
)

fun Person.color(): Color = PersonColors[colorIndex.mod(PersonColors.size)]

@Composable
fun SplitSnapTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = Color(0xFFFF8A65),
            onPrimary = Color(0xFF3A0B00),
            primaryContainer = Color(0xFF5C2414),
            onPrimaryContainer = Color(0xFFFFDBCF),
            secondary = Color(0xFF6FD3C5),
            background = Color(0xFF16120F),
            surface = Color(0xFF16120F),
            surfaceContainerLow = Color(0xFF211C18),
            surfaceContainer = Color(0xFF2A2420),
            surfaceContainerHighest = Color(0xFF3A332E),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFFD9481F),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFE3D8),
            onPrimaryContainer = Color(0xFF5C1A06),
            secondary = Color(0xFF1F7A6F),
            background = Color(0xFFFFFBF7),
            surface = Color(0xFFFFFBF7),
            surfaceContainerLow = Color(0xFFFFF4EC),
            surfaceContainer = Color(0xFFFBEDE3),
            surfaceContainerHighest = Color(0xFFF1E1D6),
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
fun PersonAvatar(
    person: Person,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    selected: Boolean = false,
) {
    val ring = if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier
    Box(
        modifier = modifier
            .size(size)
            .then(ring)
            .background(person.color(), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = person.name.trim().take(1).uppercase().ifEmpty { "?" },
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

/** Overlapping row of small avatars. */
@Composable
fun AvatarStack(people: List<Person>, modifier: Modifier = Modifier, size: Dp = 22.dp, max: Int = 5) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        people.take(max).forEachIndexed { index, person ->
            PersonAvatar(
                person = person,
                size = size,
                modifier = Modifier
                    .offset(x = (-6 * index).dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
            )
        }
        if (people.size > max) {
            Text(
                "+${people.size - max}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.offset(x = (-6 * max + 4).dp),
            )
        }
    }
}

/** Money input that keeps the user's typing intact while staying in sync with outside changes. */
@Composable
fun MoneyField(
    cents: Long,
    onChange: (Long) -> Unit,
    label: String,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(if (cents == 0L) "" else Money.toInput(cents)) }
    LaunchedEffect(cents) {
        if ((Money.parseCents(text) ?: 0L) != cents) text = if (cents == 0L) "" else Money.toInput(cents)
    }
    OutlinedTextField(
        value = text,
        onValueChange = { value ->
            text = value
            onChange(Money.parseCents(value) ?: 0L)
        },
        label = { Text(label) },
        prefix = { Text(symbol) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}
