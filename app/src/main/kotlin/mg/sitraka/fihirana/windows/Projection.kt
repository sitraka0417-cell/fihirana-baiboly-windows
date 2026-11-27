package mg.sitraka.fihirana.windows

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.awt.image.ImageObserver
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.scene.paint.Color as FxColor
import javax.imageio.ImageIO
import javax.swing.JWindow
import javax.swing.SwingUtilities
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ───────────────────────────── Modèle ─────────────────────────────

data class ProjectionSlide(
    val text: String,
    val refrain: Boolean = false,
    /** Titre propre à cette diapositive (ex. « Salamo 139:5 »). Sinon le titre du contenu est utilisé. */
    val title: String? = null,
    /** Petit libellé sous le titre (ex. « Andininy 2 », « Refrain »). */
    val label: String = ""
)

enum class ContentKind { HYMN, BIBLE, OTHER }

private fun fontKey(kind: ContentKind) = when (kind) {
    ContentKind.HYMN -> "hymnFont"
    ContentKind.BIBLE -> "bibleFont"
    ContentKind.OTHER -> "tempFont"
}

fun backgroundKey(kind: ContentKind) = if (kind == ContentKind.BIBLE) "bible" else "hymn"

val VIDEO_EXT = setOf("mp4", "m4v", "mov", "avi")
val IMAGE_EXT = setOf("png", "jpg", "jpeg", "bmp", "gif")

data class BackgroundSpec(
    val path: String = "",
    val zoom: Float = 1f,
    val x: Float = 0f,
    val y: Float = 0f,
    val dim: Float = 0.30f
) {
    val isPreset get() = path.startsWith("preset:")
    val ext get() = path.substringAfterLast('.', "").lowercase()
    val isVideo get() = !isPreset && ext in VIDEO_EXT
    val isGif get() = !isPreset && ext == "gif"
}

object Backgrounds {
    fun dir(): File = File(System.getProperty("user.home"), ".fihirana-baiboly/backgrounds").apply { mkdirs() }

    fun load(key: String): BackgroundSpec {
        val legacy = Database.setting("background", "")
        fun f(name: String, def: Float) = Database.setting("bg.$key.$name", "").toFloatOrNull() ?: def
        return BackgroundSpec(
            path = Database.setting("bg.$key", legacy),
            zoom = f("zoom", 1f), x = f("x", 0f), y = f("y", 0f), dim = f("dim", 0.30f)
        )
    }

    fun save(key: String, spec: BackgroundSpec) {
        Database.setSetting("bg.$key", spec.path)
        Database.setSetting("bg.$key.zoom", spec.zoom.toString())
        Database.setSetting("bg.$key.x", spec.x.toString())
        Database.setSetting("bg.$key.y", spec.y.toString())
        Database.setSetting("bg.$key.dim", spec.dim.toString())
    }

    fun library(): List<File> =
        dir().listFiles()?.filter { it.isFile && it.extension.lowercase() in (VIDEO_EXT + IMAGE_EXT) }
            ?.sortedBy { it.name.lowercase() } ?: emptyList()

    /** Copie le fichier choisi dans le dossier de l'application pour qu'il reste disponible. */
    fun import(src: File): File {
        var target = File(dir(), src.name)
        var n = 1
        while (target.exists() && target.length() != src.length()) {
            target = File(dir(), "${src.nameWithoutExtension}_$n.${src.extension}")
            n++
        }
        if (!target.exists()) Files.copy(src.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return target
    }
}

val BackgroundPresets = listOf(
    "preset:onyx" to "Onyx",
    "preset:navy" to "Bleu nuit",
    "preset:forest" to "Forêt",
    "preset:wine" to "Bordeaux",
    "preset:gradient" to "Aurore",
    "preset:dawn" to "Aube"
)

fun presetBrush(key: String): Brush? = when (key) {
    "preset:onyx" -> Brush.verticalGradient(listOf(Color(0xFF0B0B10), Color(0xFF17151C), Color(0xFF050507)))
    "preset:navy" -> Brush.verticalGradient(listOf(Color(0xFF07182F), Color(0xFF102D50), Color(0xFF030A16)))
    "preset:forest" -> Brush.verticalGradient(listOf(Color(0xFF08251D), Color(0xFF174B37), Color(0xFF04130F)))
    "preset:wine" -> Brush.verticalGradient(listOf(Color(0xFF300D20), Color(0xFF641D36), Color(0xFF170610)))
    "preset:gradient" -> Brush.linearGradient(listOf(Color(0xFF10172D), Color(0xFF36204D), Color(0xFF071B27)))
    "preset:dawn" -> Brush.verticalGradient(listOf(Color(0xFF0F1B33), Color(0xFF2D3F6B), Color(0xFF9A7653)))
    else -> null
}

// ───────────────────────────── Contrôleur ─────────────────────────────

@Stable
class ProjectionController {
    var contentId by mutableStateOf("")
    var title by mutableStateOf("Baiboly sy Fihirana")
    var subtitle by mutableStateOf("")
    var slides by mutableStateOf<List<ProjectionSlide>>(emptyList())
    var index by mutableIntStateOf(0)
    var kind by mutableStateOf(ContentKind.HYMN)
    var live by mutableStateOf(false)
    var fontSize by mutableIntStateOf(38)
    var background by mutableStateOf(BackgroundSpec())
    private var returnIndex = -1

    val current: ProjectionSlide? get() = slides.getOrNull(index)

    /** Charge un contenu (aperçu) ; s'il est déjà en direct, l'écran de projection se met à jour. */
    fun load(
        id: String, title: String, subtitle: String,
        slides: List<ProjectionSlide>, index: Int, kind: ContentKind
    ) {
        val changed = id != contentId || kind != this.kind
        this.kind = kind
        if (changed) {
            fontSize = Database.setting(fontKey(kind), "38").toIntOrNull() ?: 38
            returnIndex = -1
        }
        background = Backgrounds.load(backgroundKey(kind))
        contentId = id
        this.title = title
        this.subtitle = subtitle
        this.slides = slides
        this.index = if (slides.isEmpty()) 0 else index.coerceIn(0, slides.lastIndex)
    }

    fun go(delta: Int) {
        if (slides.isEmpty()) return
        returnIndex = -1
        index = (index + delta).coerceIn(0, slides.lastIndex)
    }

    fun select(i: Int) {
        if (slides.isEmpty()) return
        returnIndex = -1
        index = i.coerceIn(0, slides.lastIndex)
    }

    /**
     * Touche R : sur un couplet → saute au refrain ; sur le refrain → continue vers le couplet suivant.
     * Répétable à chaque couplet.
     */
    fun toggleRefrain() {
        val cur = slides.getOrNull(index) ?: return
        if (cur.refrain) {
            val from = maxOf(index, returnIndex)
            var i = from + 1
            while (i <= slides.lastIndex && slides[i].refrain) i++
            index = if (i <= slides.lastIndex) i else if (returnIndex >= 0) returnIndex else index
            returnIndex = -1
        } else {
            val target = slides.indices.firstOrNull { it > index && slides[it].refrain }
                ?: slides.indexOfFirst { it.refrain }
            if (target >= 0) {
                returnIndex = index
                index = target
            }
        }
    }

    fun resizeFont(delta: Int) {
        fontSize = (fontSize + delta).coerceIn(14, 140)
        Database.setSetting(fontKey(kind), fontSize.toString())
    }

    fun reloadStyle() {
        background = Backgrounds.load(backgroundKey(kind))
        fontSize = Database.setting(fontKey(kind), "38").toIntOrNull() ?: fontSize
    }

    fun stop() {
        live = false
    }

    fun start() {
        if (slides.isNotEmpty()) live = true
    }
}

/** Gestion clavier commune à la fenêtre principale et à la fenêtre de projection. */
fun handleProjectionKey(ctrl: ProjectionController, e: KeyEvent, focus: FocusManager?): Boolean {
    if (e.type != KeyEventType.KeyDown) return false
    if (e.key == Key.Escape) {
        if (Typing.active) {
            focus?.clearFocus()
            Typing.active = false
            return true
        }
        if (ctrl.live) {
            ctrl.stop()
            return true
        }
        return false
    }
    if (!ctrl.live || Typing.active) return false
    if (e.isCtrlPressed) {
        return when (e.key) {
            Key.DirectionUp, Key.DirectionRight, Key.Equals, Key.Plus, Key.NumPadAdd -> { ctrl.resizeFont(2); true }
            Key.DirectionDown, Key.DirectionLeft, Key.Minus, Key.NumPadSubtract -> { ctrl.resizeFont(-2); true }
            else -> false
        }
    }
    return when (e.key) {
        Key.DirectionRight, Key.DirectionDown, Key.PageDown -> { ctrl.go(1); true }
        Key.DirectionLeft, Key.DirectionUp, Key.PageUp -> { ctrl.go(-1); true }
        Key.R -> { ctrl.toggleRefrain(); true }
        Key.Home -> { ctrl.select(0); true }
        Key.End -> { ctrl.select(ctrl.slides.lastIndex); true }
        else -> false
    }
}

// ───────────────────────────── Écrans ─────────────────────────────

data class ScreenInfo(val index: Int, val bounds: Rectangle, val primary: Boolean) {
    val label: String
        get() = "Écran ${index + 1} — ${bounds.width}×${bounds.height}" + if (primary) " (principal)" else ""
}

fun listScreens(): List<ScreenInfo> = try {
    if (GraphicsEnvironment.isHeadless()) emptyList()
    else {
        val env = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val primary = env.defaultScreenDevice
        env.screenDevices.mapIndexed { i, d -> ScreenInfo(i, d.defaultConfiguration.bounds, d == primary) }
    }
} catch (_: Throwable) {
    emptyList()
}

/** null = fenêtre flottante (un seul écran ou choix « Fenêtre »). */
fun chooseProjectionTarget(): Rectangle? {
    val screens = listScreens()
    val pref = Database.setting("projectionScreen", "auto")
    if (pref == "window") return null
    pref.toIntOrNull()?.let { i -> screens.firstOrNull { it.index == i }?.let { return it.bounds } }
    return screens.firstOrNull { !it.primary }?.bounds
}

fun describeProjectionTarget(): String {
    val screens = listScreens()
    val target = chooseProjectionTarget()
    return when {
        target == null && screens.size < 2 -> "Écran faharoa tsy hita — hiseho ao amin'ny fenêtre"
        target == null -> "Hiseho ao amin'ny fenêtre"
        else -> screens.firstOrNull { it.bounds == target }?.label?.let { "Hiseho amin'ny $it" } ?: "Écran faharoa"
    }
}

@Composable
fun ProjectionOutput(ctrl: ProjectionController, onKey: (KeyEvent) -> Boolean) {
    val target = remember { chooseProjectionTarget() }
    if (target != null) {
        val video = ctrl.background.isVideo && File(ctrl.background.path).exists()
        key(video) {
            val state = rememberWindowState(
                position = WindowPosition(target.x.dp, target.y.dp),
                size = DpSize(target.width.dp, target.height.dp)
            )
            Window(
                onCloseRequest = { ctrl.stop() },
                state = state,
                title = "Projection",
                undecorated = true,
                resizable = false,
                alwaysOnTop = true,
                transparent = video,
                onPreviewKeyEvent = onKey
            ) {
                LaunchedEffect(Unit) { window.bounds = target }
                val bg = ctrl.background
                DisposableEffect(video, bg) {
                    if (video) VideoLayer.show(bg, target) { window.toFront() }
                    onDispose { if (video) VideoLayer.hide() }
                }
                ProjectionCanvas(ctrl, Modifier.fillMaxSize(), drawBackground = !video)
            }
        }
    } else {
        val state = rememberWindowState(size = DpSize(960.dp, 540.dp), position = WindowPosition(Alignment.Center))
        Window(
            onCloseRequest = { ctrl.stop() },
            state = state,
            title = "Projection — Baiboly sy Fihirana",
            onPreviewKeyEvent = { e ->
                if (e.type == KeyEventType.KeyDown && e.key == Key.F11) {
                    state.placement =
                        if (state.placement == WindowPlacement.Fullscreen) WindowPlacement.Floating
                        else WindowPlacement.Fullscreen
                    true
                } else onKey(e)
            }
        ) {
            ProjectionCanvas(ctrl, Modifier.fillMaxSize())
        }
    }
}

// ───────────────────────────── Rendu ─────────────────────────────

private val ColorTag = Regex("\\{#([0-9A-Fa-f]{6})\\}(.*?)\\{/\\}", RegexOption.DOT_MATCHES_ALL)

fun richText(text: String): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    for (match in ColorTag.findAll(text)) {
        append(text.substring(cursor, match.range.first))
        val rgb = match.groupValues[1].toLongOrNull(16) ?: 0xFFFFFFL
        withStyle(SpanStyle(color = Color(0xFF000000L or rgb))) { append(match.groupValues[2]) }
        cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}

/**
 * Rendu unique utilisé par l'aperçu ET par l'écran de projection (WYSIWYG).
 * La taille de police est exprimée pour une hauteur de 720 px puis mise à l'échelle.
 */
@Composable
fun ProjectionCanvas(
    ctrl: ProjectionController,
    modifier: Modifier = Modifier,
    drawBackground: Boolean = true
) {
    val slide = ctrl.current
    BoxWithConstraints(modifier.background(if (drawBackground) Color.Black else Color.Transparent)) {
        val scale = maxHeight.value / 720f
        if (drawBackground) BackgroundLayer(ctrl.background, Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().padding(horizontal = (64 * scale).dp, vertical = (34 * scale).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val title = slide?.title ?: ctrl.title
            val sub = slide?.label?.takeIf { it.isNotBlank() } ?: ctrl.subtitle
            if (title.isNotBlank()) {
                Text(
                    title, color = Ui.Gold, fontSize = (30 * scale).sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif, textAlign = TextAlign.Center, maxLines = 2,
                    style = TextStyle(shadow = Shadow(Color.Black, Offset(0f, 2f), 8f))
                )
            }
            if (sub.isNotBlank()) {
                Text(
                    sub, color = Ui.Text.copy(alpha = 0.85f), fontSize = (18 * scale).sp,
                    textAlign = TextAlign.Center, maxLines = 1,
                    style = TextStyle(shadow = Shadow(Color.Black, Offset(0f, 2f), 6f))
                )
            }
            Spacer(Modifier.height((14 * scale).dp))
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                FitText(slide?.text ?: "", ctrl.fontSize * scale, maxHeight)
            }
        }
    }
}

/** Réduit automatiquement le texte s'il dépasse la hauteur disponible. */
@Composable
private fun FitText(text: String, baseSp: Float, maxHeight: Dp) {
    var factor by remember(text, baseSp, maxHeight) { mutableFloatStateOf(1f) }
    val annotated = remember(text) { richText(text) }
    val size = (baseSp * factor).coerceAtLeast(6f)
    Text(
        annotated,
        color = Ui.Text,
        fontSize = size.sp,
        lineHeight = (size * 1.28f).sp,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Medium,
        style = TextStyle(shadow = Shadow(Color.Black, Offset(0f, 3f), 10f)),
        onTextLayout = { if (it.didOverflowHeight && factor > 0.3f) factor *= 0.93f }
    )
}

@Composable
fun BackgroundLayer(bg: BackgroundSpec, modifier: Modifier = Modifier, showVideoPlaceholder: Boolean = true) {
    Box(modifier.clipToBounds().background(Color.Black)) {
        val brush = presetBrush(bg.path)
        val exists = brush != null || (bg.path.isNotBlank() && File(bg.path).exists())
        when {
            brush != null -> Box(Modifier.fillMaxSize().background(brush))
            !exists -> {}
            bg.isVideo -> if (showVideoPlaceholder) {
                Box(Modifier.fillMaxSize().background(Color(0xFF0E131C)), contentAlignment = Alignment.Center) {
                    Text("▶  ${File(bg.path).name}", color = Ui.Muted, fontSize = 11.sp, maxLines = 1)
                }
            }
            bg.isGif -> {
                val frame = rememberGifFrame(bg.path)
                if (frame != null) CroppedImage(frame, bg)
            }
            else -> {
                val img = produceState<ImageBitmap?>(null, bg.path) {
                    value = withContext(Dispatchers.IO) {
                        runCatching { ImageIO.read(File(bg.path))?.toComposeImageBitmap() }.getOrNull()
                    }
                }.value
                if (img != null) CroppedImage(img, bg)
            }
        }
        if (exists && brush == null && bg.dim > 0f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = bg.dim.coerceIn(0f, 0.9f))))
        }
    }
}

@Composable
private fun CroppedImage(img: ImageBitmap, bg: BackgroundSpec) {
    Image(
        bitmap = img,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().graphicsLayer {
            val z = bg.zoom.coerceIn(1f, 3f)
            scaleX = z
            scaleY = z
            translationX = bg.x.coerceIn(-1f, 1f) * (z - 1f) * size.width / 2f
            translationY = bg.y.coerceIn(-1f, 1f) * (z - 1f) * size.height / 2f
        }
    )
}

// ───────────────────────────── GIF animé (AWT) ─────────────────────────────

private class GifPlayer(path: String, private val onFrame: (ImageBitmap) -> Unit) : ImageObserver {
    @Volatile private var stopped = false
    private val image = java.awt.Toolkit.getDefaultToolkit().createImage(path)
    private var buffer: BufferedImage? = null

    init {
        java.awt.Toolkit.getDefaultToolkit().prepareImage(image, -1, -1, this)
    }

    fun stop() {
        stopped = true
        image.flush()
    }

    override fun imageUpdate(img: java.awt.Image?, infoflags: Int, x: Int, y: Int, width: Int, height: Int): Boolean {
        if (stopped) return false
        if (infoflags and ImageObserver.ERROR != 0 || infoflags and ImageObserver.ABORT != 0) return false
        val iw = image.getWidth(null)
        val ih = image.getHeight(null)
        if (iw <= 0 || ih <= 0) return true
        if (infoflags and (ImageObserver.FRAMEBITS or ImageObserver.ALLBITS) != 0) {
            var b = buffer
            if (b == null || b.width != iw || b.height != ih) {
                b = BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB)
                buffer = b
            }
            val g = b.createGraphics()
            try {
                g.composite = java.awt.AlphaComposite.Src
                g.drawImage(image, 0, 0, null)
            } finally {
                g.dispose()
            }
            onFrame(b.toComposeImageBitmap())
        }
        return true
    }
}

@Composable
private fun rememberGifFrame(path: String): ImageBitmap? {
    var frame by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    DisposableEffect(path) {
        val player = try {
            GifPlayer(path) { frame = it }
        } catch (_: Throwable) {
            null
        }
        onDispose { player?.stop() }
    }
    return frame
}

// ───────────────────────────── Vidéo (JavaFX) ─────────────────────────────

object JavaFxRuntime {
    private val started = AtomicBoolean(false)

    fun ensureStarted() {
        if (started.compareAndSet(false, true)) {
            try {
                Platform.startup { Platform.setImplicitExit(false) }
            } catch (_: IllegalStateException) {
                Platform.setImplicitExit(false)
            }
        }
    }
}

/**
 * Le fond vidéo est affiché dans une fenêtre JavaFX placée SOUS la fenêtre de projection (transparente).
 * C'est la seule façon fiable d'avoir du texte Compose par-dessus une vidéo.
 */
object VideoLayer {
    private var window: JWindow? = null
    private var player: MediaPlayer? = null

    fun show(bg: BackgroundSpec, bounds: Rectangle, onShown: () -> Unit) {
        hide()
        try {
            JavaFxRuntime.ensureStarted()
        } catch (_: Throwable) {
            return
        }
        SwingUtilities.invokeLater {
            try {
                val w = JWindow()
                w.focusableWindowState = false
                w.isAlwaysOnTop = true
                w.bounds = bounds
                w.background = java.awt.Color.BLACK
                val panel = JFXPanel()
                w.contentPane.add(panel)
                w.isVisible = true
                window = w
                Platform.runLater {
                    try {
                        val media = Media(File(bg.path).toURI().toString())
                        val p = MediaPlayer(media)
                        val view = MediaView(p)
                        view.isPreserveRatio = false
                        val root = StackPane(view)
                        root.style = "-fx-background-color: black;"
                        val z = bg.zoom.coerceIn(1f, 3f).toDouble()
                        view.fitWidthProperty().bind(root.widthProperty().multiply(z))
                        view.fitHeightProperty().bind(root.heightProperty().multiply(z))
                        view.translateXProperty()
                            .bind(root.widthProperty().multiply((z - 1.0) * bg.x.coerceIn(-1f, 1f) / 2.0))
                        view.translateYProperty()
                            .bind(root.heightProperty().multiply((z - 1.0) * bg.y.coerceIn(-1f, 1f) / 2.0))
                        panel.scene = Scene(root, FxColor.BLACK)
                        p.cycleCount = MediaPlayer.INDEFINITE
                        p.isMute = true
                        p.setOnReady { p.play() }
                        player = p
                    } catch (_: Throwable) {
                    }
                }
                SwingUtilities.invokeLater { onShown() }
            } catch (_: Throwable) {
            }
        }
    }

    fun hide() {
        val p = player
        val w = window
        player = null
        window = null
        if (p != null) {
            try {
                Platform.runLater {
                    try {
                        p.stop()
                        p.dispose()
                    } catch (_: Throwable) {
                    }
                }
            } catch (_: Throwable) {
            }
        }
        if (w != null) SwingUtilities.invokeLater {
            w.isVisible = false
            w.dispose()
        }
    }
}
