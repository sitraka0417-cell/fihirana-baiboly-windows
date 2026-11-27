package mg.sitraka.fihirana.windows

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Palette moderne : encre bleu nuit + or champagne. */
object Ui {
    val Bg = Color(0xFF080B11)
    val Rail = Color(0xFF0C1018)
    val Surface = Color(0xFF121722)
    val Surface2 = Color(0xFF1A2130)
    val Surface3 = Color(0xFF232C3E)
    val Border = Color(0xFF273146)
    val Gold = Color(0xFFD9B36A)
    val GoldSoft = Color(0x26D9B36A)
    val OnGold = Color(0xFF1A1405)
    val Text = Color(0xFFEDEFF5)
    val Muted = Color(0xFF8F98AB)
    val Danger = Color(0xFFE5645A)
    val Ok = Color(0xFF5BC98B)
}

/** Vrai quand un champ texte a le focus : les flèches ne pilotent alors pas la projection. */
object Typing {
    @Volatile
    var active: Boolean = false
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ui.Gold,
            onPrimary = Ui.OnGold,
            secondary = Ui.Gold,
            background = Ui.Bg,
            onBackground = Ui.Text,
            surface = Ui.Surface,
            onSurface = Ui.Text,
            surfaceVariant = Ui.Surface2,
            onSurfaceVariant = Ui.Muted,
            outline = Ui.Border,
            outlineVariant = Ui.Border,
            error = Ui.Danger
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Ui.Bg, contentColor = Ui.Text, content = content)
    }
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    color: Color = Ui.Surface,
    padding: Int = 16,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .background(color, RoundedCornerShape(16.dp))
            .border(1.dp, Ui.Border, RoundedCornerShape(16.dp))
            .padding(padding.dp),
        content = content
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(), modifier, color = Ui.Gold, fontSize = 11.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp
    )
}

@Composable
fun GoldButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Ui.Gold, contentColor = Ui.OnGold,
            disabledContainerColor = Ui.Surface3, disabledContentColor = Ui.Muted
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp)
    ) { Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Ui.Border),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (danger) Ui.Danger else Ui.Text,
            disabledContentColor = Ui.Muted
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
    ) { Text(text, fontSize = 13.sp) }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (selected) Ui.Gold else Ui.Surface2,
        contentColor = if (selected) Ui.OnGold else Ui.Text,
        border = BorderStroke(1.dp, if (selected) Ui.Gold else Ui.Border)
    ) {
        Text(
            text, Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            fontSize = 13.sp, fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AppField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    var focused by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { if (focused) Typing.active = false } }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.onFocusChanged {
            focused = it.isFocused
            Typing.active = it.isFocused
        },
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Ui.Gold,
            unfocusedBorderColor = Ui.Border,
            focusedLabelColor = Ui.Gold,
            unfocusedLabelColor = Ui.Muted,
            cursorColor = Ui.Gold,
            focusedTextColor = Ui.Text,
            unfocusedTextColor = Ui.Text,
            focusedContainerColor = Ui.Surface2,
            unfocusedContainerColor = Ui.Surface2
        )
    )
}

@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {}
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = range,
        onValueChangeFinished = onFinished,
        colors = SliderDefaults.colors(
            thumbColor = Ui.Gold,
            activeTrackColor = Ui.Gold,
            inactiveTrackColor = Ui.Surface3
        )
    )
}

@Composable
fun AppSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Ui.OnGold,
            checkedTrackColor = Ui.Gold,
            uncheckedThumbColor = Ui.Muted,
            uncheckedTrackColor = Ui.Surface2,
            uncheckedBorderColor = Ui.Border
        )
    )
}
