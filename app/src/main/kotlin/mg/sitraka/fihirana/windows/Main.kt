package mg.sitraka.fihirana.windows

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.window.rememberWindowState
import java.io.File
import javax.swing.UIManager
import kotlin.system.exitProcess

private fun logError(t: Throwable) {
    try {
        val dir = File(System.getProperty("user.home"), ".fihirana-baiboly").apply { mkdirs() }
        File(dir, "error.log").appendText("${java.time.LocalDateTime.now()}\n${t.stackTraceToString()}\n\n")
    } catch (_: Throwable) {
    }
}

private fun fatal(t: Throwable) {
    logError(t)
    try {
        javax.swing.JOptionPane.showMessageDialog(
            null,
            "Tsy afaka nanomboka ny rindrambaiko:\n${t::class.simpleName}: ${t.message}\n\nLog: %USERPROFILE%\\.fihirana-baiboly\\error.log",
            "Baiboly sy Fihirana Advantista", javax.swing.JOptionPane.ERROR_MESSAGE
        )
    } catch (_: Throwable) {
    }
}

fun main() {
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (_: Throwable) {
    }
    try {
        Database.open()
    } catch (t: Throwable) {
        fatal(t)
        return
    }
    Thread.setDefaultUncaughtExceptionHandler { _, e -> logError(e) }
    application { App() }
    exitProcess(0)
}

private data class NavItem(val key: String, val glyph: String, val hint: String)

private val NAV = listOf(
    NavItem("Fihirana", "♪", "Hira sy tononkira"),
    NavItem("Baiboly", "✝", "Andininy avy amin'ny Baiboly"),
    NavItem("Hira manokana", "★", "Hira napetrakao"),
    NavItem("Projection vonjimaika", "▤", "Pejy soratana avy hatrany"),
    NavItem("Tantara", "↺", "Projection farany"),
    NavItem("Réglages", "⚙", "Fond, soratra, écran")
)

@Composable
private fun ApplicationScope.App() {
    val ctrl = remember { ProjectionController() }
    var section by remember { mutableStateOf("Fihirana") }
    var about by remember { mutableStateOf(true) }
    val focusHolder = remember { arrayOfNulls<FocusManager>(1) }

    Window(
        onCloseRequest = { exitApplication() },
        title = "Baiboly sy Fihirana Advantista",
        state = rememberWindowState(size = DpSize(1400.dp, 880.dp), position = WindowPosition(Alignment.Center)),
        onPreviewKeyEvent = { handleProjectionKey(ctrl, it, focusHolder[0]) }
    ) {
        LaunchedEffect(Unit) { window.minimumSize = java.awt.Dimension(1180, 720) }
        AppTheme {
            val focus = LocalFocusManager.current
            SideEffect { focusHolder[0] = focus }
            LaunchedEffect(section) { Typing.active = false }

            Row(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    // Un clic en dehors d'un champ texte rend la main aux flèches de navigation
                    awaitPointerEventScope {
                        while (true) {
                            val ev = awaitPointerEvent(PointerEventPass.Initial)
                            if (ev.type == PointerEventType.Press) focus.clearFocus()
                        }
                    }
                }
            ) {
                Rail(section, ctrl, onSelect = { section = it }, onAbout = { about = true })

                Column(Modifier.weight(1f).fillMaxHeight().padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 20.dp)) {
                    val item = NAV.first { it.key == section }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(section, color = Ui.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                        Spacer(Modifier.width(14.dp))
                        Text(item.hint, color = Ui.Muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 5.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (section) {
                            "Fihirana" -> HymnScreen(ctrl)
                            "Baiboly" -> BibleScreen(ctrl)
                            "Hira manokana" -> CustomSongsScreen(ctrl)
                            "Projection vonjimaika" -> TemporaryScreen(ctrl)
                            "Tantara" -> HistoryScreen(ctrl)
                            else -> SettingsScreen(ctrl)
                        }
                    }
                }

                PreviewPanel(ctrl)
            }

            if (about) {
                DialogWindow(
                    onCloseRequest = { about = false },
                    title = "Mombamomba",
                    resizable = false,
                    state = rememberDialogState(size = DpSize(480.dp, 470.dp))
                ) {
                    AppTheme { AboutCard { about = false } }
                }
            }
        }
    }

    if (ctrl.live) {
        ProjectionOutput(ctrl) { handleProjectionKey(ctrl, it, null) }
    }
}

@Composable
private fun Rail(section: String, ctrl: ProjectionController, onSelect: (String) -> Unit, onAbout: () -> Unit) {
    Column(Modifier.width(226.dp).fillMaxHeight().background(Ui.Rail).padding(horizontal = 14.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).background(Ui.Gold, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("B", color = Ui.OnGold, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Baiboly", color = Ui.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Text("sy Fihirana Advantista", color = Ui.Muted, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(26.dp))
        NAV.forEach { item ->
            val active = section == item.key
            Surface(
                onClick = { onSelect(item.key) },
                shape = RoundedCornerShape(12.dp),
                color = if (active) Ui.Surface2 else androidx.compose.ui.graphics.Color.Transparent,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(28.dp).background(if (active) Ui.Gold else Ui.Surface2, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.glyph, color = if (active) Ui.OnGold else Ui.Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        item.key, color = if (active) Ui.Gold else Ui.Text, fontSize = 14.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth().background(Ui.Surface, RoundedCornerShape(12.dp))
                .border(1.dp, Ui.Border, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(9.dp).background(if (ctrl.live) Ui.Ok else Ui.Muted, RoundedCornerShape(50)))
            Spacer(Modifier.width(10.dp))
            Text(if (ctrl.live) "Projection mandeha" else "Projection tsy mandeha", color = Ui.Text, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        GhostButton("Mombamomba", onAbout, Modifier.fillMaxWidth())
    }
}

@Composable
private fun PreviewPanel(ctrl: ProjectionController) {
    val hasSlides = ctrl.slides.isNotEmpty()
    Column(Modifier.width(350.dp).fillMaxHeight().background(Ui.Rail).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Aperçu projection", Modifier.weight(1f))
            Text(
                if (ctrl.live) "● EN DIRECT" else "○ HORS LIGNE",
                color = if (ctrl.live) Ui.Ok else Ui.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(12.dp))
        ProjectionCanvas(
            ctrl,
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))
                .border(if (ctrl.live) 2.dp else 1.dp, if (ctrl.live) Ui.Gold else Ui.Border, RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (hasSlides) "Pejy ${ctrl.index + 1} / ${ctrl.slides.size}   ·   soratra ${ctrl.fontSize} pt" else "Tsy misy voafidy",
            color = Ui.Muted, fontSize = 12.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("‹ Teo aloha", { ctrl.go(-1) }, Modifier.weight(1f), enabled = hasSlides && ctrl.index > 0)
            GhostButton("Manaraka ›", { ctrl.go(1) }, Modifier.weight(1f), enabled = hasSlides && ctrl.index < ctrl.slides.lastIndex)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("Refrain (R)", { ctrl.toggleRefrain() }, Modifier.weight(1f), enabled = ctrl.slides.any { it.refrain })
            GhostButton("A−", { ctrl.resizeFont(-2) }, enabled = hasSlides)
            GhostButton("A+", { ctrl.resizeFont(2) }, enabled = hasSlides)
        }
        Spacer(Modifier.height(8.dp))
        GhostButton(
            if (ctrl.blank) "◼ Asehoy indray ny soratra (B)" else "◻ Afenina ny soratra — fond ihany (B)",
            { ctrl.blank = !ctrl.blank }, Modifier.fillMaxWidth(), enabled = hasSlides
        )
        Spacer(Modifier.height(14.dp))
        GoldButton(
            if (ctrl.live) "■  Akatona ny projection" else "▶  Alefaso ny projection",
            { if (ctrl.live) ctrl.stop() else ctrl.start() },
            Modifier.fillMaxWidth(), enabled = hasSlides || ctrl.live
        )
        Spacer(Modifier.height(6.dp))
        val target = remember(ctrl.live) { describeProjectionTarget() }
        Text(target, color = Ui.Muted, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        Panel(Modifier.fillMaxWidth(), padding = 12) {
            SectionLabel("Kitendry")
            Spacer(Modifier.height(6.dp))
            Text("← →  pejy teo aloha / manaraka", color = Ui.Text, fontSize = 12.sp)
            Text("R  refrain (indray = andininy manaraka)", color = Ui.Text, fontSize = 12.sp)
            Text("B  afenina ny soratra (fond ihany)", color = Ui.Text, fontSize = 12.sp)
            Text("Ctrl + ↑ ↓  haben'ny soratra", color = Ui.Text, fontSize = 12.sp)
            Text("Échap  hampitsahatra", color = Ui.Text, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AboutCard(onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
    ) {
        Box(Modifier.size(64.dp).background(Ui.Gold, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
            Text("B", color = Ui.OnGold, fontSize = 38.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        }
        Text("BAIBOLY SY FIHIRANA", color = Ui.Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        Text("ADVANTISTA", color = Ui.Text, fontSize = 15.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(4.dp))
        Text("Novolavolain'i Sitraka Nambinintsoa", color = Ui.Text)
        Text("sitraka0417@gmail.com", color = Ui.Muted, fontSize = 13.sp)
        Text("034 80 175 59", color = Ui.Muted, fontSize = 13.sp)
        Text(
            "Ahitanao ny Baiboly version samihafa sy ny fihirana Advantista ary ny fihirana an'ny ankizy.",
            color = Ui.Text, fontSize = 13.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        GoldButton("Hanomboka", onClose)
    }
}
