package mg.sitraka.fihirana.windows

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.JPanel
import javax.swing.filechooser.FileNameExtensionFilter
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color as FxColor
import javax.swing.SwingUtilities

private val Dark = Color(0xFF0A0A0F)
private val Panel = Color(0xFF15151E)
private val CardColor = Color(0xFF20202B)
private val Gold = Color(0xFFD4A853)
private val White = Color(0xFFF0EFF4)
private val Muted = Color(0xFFAAA8B4)

data class ProjectionSlide(val text: String, val refrain: Boolean = false)
data class Projection(
    val title: String,
    val subtitle: String,
    val slides: List<ProjectionSlide>,
    val font: Int = 38,
    val background: String = "",
    val backgroundZoom: Float = 1f,
    val backgroundX: Float = 0f,
    val backgroundY: Float = 0f
) {
    val safeSlides get() = slides.ifEmpty { listOf(ProjectionSlide("")) }
}

private fun logStartupError(t: Throwable) {
    try {
        val dir = File(System.getProperty("user.home"), ".fihirana-baiboly").apply { mkdirs() }
        File(dir, "error.log").appendText("${java.time.LocalDateTime.now()}\n${t.stackTraceToString()}\n\n")
    } catch (_: Throwable) {}
    try {
        javax.swing.JOptionPane.showMessageDialog(
            null,
            "Tsy afaka nanomboka ny rindrambaiko:\n${t::class.simpleName}: ${t.message}\n\nLog: %USERPROFILE%\\.fihirana-baiboly\\error.log",
            "Baiboly sy Fihirana Advantista", javax.swing.JOptionPane.ERROR_MESSAGE
        )
    } catch (_: Throwable) {}
}

fun main() {
    try {
        Database.open()
    } catch (t: Throwable) {
        logStartupError(t)
        return
    }
    Thread.setDefaultUncaughtExceptionHandler { _, e -> logStartupError(e) }
    application { App() }
}

@Composable
private fun ApplicationScope.App() {
    var projection by remember {
        mutableStateOf(Projection("Projection prête", "Baiboly sy Fihirana Advantista",
            listOf(ProjectionSlide("Safidio ny hira na andininy.")), 36, Database.setting("background")))
    }
    var projectionOpen by remember { mutableStateOf(false) }
    var splash by remember { mutableStateOf(true) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Baiboly sy Fihirana Advantista",
        state = WindowState(width = 1450.dp, height = 900.dp)
    ) {
        MaterialTheme(colorScheme = darkColorScheme(primary = Gold, background = Dark, surface = Panel)) {
            MainWindow(
                projection = projection,
                updateProjection = {
                    projection = it.copy(
                        backgroundZoom = Database.setting("backgroundZoom", "1.0").toFloatOrNull() ?: 1f,
                        backgroundX = Database.setting("backgroundX", "0.0").toFloatOrNull() ?: 0f,
                        backgroundY = Database.setting("backgroundY", "0.0").toFloatOrNull() ?: 0f
                    )
                },
                openProjection = { projectionOpen = true },
                isProjectionOpen = projectionOpen,
                onAbout = { splash = true }
            )
            if (splash) {
                DialogWindow(onCloseRequest = { splash = false }, title = "Mombamomba") {
                    MaterialTheme(colorScheme = darkColorScheme(primary = Gold, background = Dark, surface = Panel)) {
                        Column(
                            Modifier.width(430.dp).background(Panel, RoundedCornerShape(18.dp)).padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Text("BAIBOLY SY FIHIRANA", color = Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("ADVANTISTA", color = White, fontSize = 17.sp)
                            Divider(color = Gold)
                            Text("Novolavolain'i Sitraka Nambinintsoa", color = White)
                            Text("sitraka0417@gmail.com", color = Muted)
                            Text("034 80 175 59", color = Muted)
                            Text(
                                "Ahitanao ny Baiboly version samihafa sy ny fihirana Advantista ary ny fihirana an'ny ankizy.",
                                color = White
                            )
                            Button(onClick = { splash = false }) { Text("Hanomboka") }
                        }
                    }
                }
            }
        }
    }

    if (projectionOpen) {
        val screens = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
        val second = screens.getOrNull(1)?.defaultConfiguration?.bounds
        // remember: sinon la fenêtre revient à sa taille/position initiale à chaque changement de projection
        val projectionState = remember {
            WindowState(
                width = if (second != null) (second.width / 1.5).dp else 1100.dp,
                height = if (second != null) (second.height / 1.5).dp else 700.dp,
                position = if (second != null) WindowPosition(second.x.dp, second.y.dp) else WindowPosition(Alignment.Center)
            )
        }
        Window(
            onCloseRequest = { projectionOpen = false },
            title = "Projection — Baiboly sy Fihirana",
            state = projectionState
        ) {
            ProjectionWindow(
                projection,
                onToggleFullscreen = {
                    projectionState.placement =
                        if (projectionState.placement == WindowPlacement.Fullscreen) WindowPlacement.Floating
                        else WindowPlacement.Fullscreen
                },
                onClose = { projectionOpen = false }
            )
        }
    }
}

private fun projectionIndex(slides: List<ProjectionSlide>, subtitle: String): Int = 0

@Composable
private fun MainWindow(
    projection: Projection,
    updateProjection: (Projection) -> Unit,
    openProjection: () -> Unit,
    isProjectionOpen: Boolean,
    onAbout: () -> Unit
) {
    var section by remember { mutableStateOf("Fihirana") }
    Row(Modifier.fillMaxSize().background(Dark)) {
        Column(Modifier.width(215.dp).fillMaxHeight().background(Panel).padding(15.dp)) {
            Text("BAIBOLY", color = Gold, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("& FIHIRANA", color = White, fontSize = 18.sp)
            Spacer(Modifier.height(22.dp))
            listOf("Fihirana", "Baiboly", "Hira manokana", "Projection vonjimaika", "Tantara", "Réglages").forEach { item ->
                Surface(
                    onClick = { section = item },
                    color = if (section == item) CardColor else Color.Transparent,
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    Text(item, Modifier.padding(12.dp), color = if (section == item) Gold else White)
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onAbout) { Text("Mombamomba", color = White) }
        }

        Column(Modifier.weight(1f).fillMaxHeight().padding(18.dp)) {
            Text(section, color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            when (section) {
                "Fihirana" -> HymnScreen(updateProjection, openProjection, isProjectionOpen)
                "Baiboly" -> BibleScreen(updateProjection, openProjection, isProjectionOpen)
                "Hira manokana" -> CustomSongsScreen(updateProjection, openProjection)
                "Projection vonjimaika" -> TemporaryScreen(updateProjection, openProjection)
                "Tantara" -> HistoryScreen(updateProjection, openProjection)
                else -> SettingsScreen(projection, updateProjection)
            }
        }

        Column(Modifier.width(350.dp).fillMaxHeight().background(Panel).padding(14.dp)) {
            Text("APERÇU PROJECTION", color = Gold, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            ProjectionPreview(projection)
            Spacer(Modifier.height(10.dp))
            Button(onClick = openProjection, modifier = Modifier.fillMaxWidth()) { Text("PROJETER SUR ÉCRAN") }
            Text(if (isProjectionOpen) "Projection active" else "Projection mbola tsy mandeha",
                color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ProjectionPreview(p: Projection) {
    Column(
        Modifier.fillMaxWidth().height(300.dp).background(Color.Black, RoundedCornerShape(9.dp)).padding(15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(p.title, color = Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(p.subtitle, color = White, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        RichProjectionText(p.safeSlides.first().text, fontSize = (p.font / 2).sp)
    }
}

@Composable
private fun HymnScreen(update: (Projection) -> Unit, open: () -> Unit, active: Boolean) {
    val langs = remember { Database.languages() }
    var lang by remember { mutableStateOf(langs.firstOrNull()?.code ?: "mg") }
    var term by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Song?>(null) }
    var parts by remember { mutableStateOf<List<SongPart>>(emptyList()) }
    var edit by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var lyricText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var listVersion by remember { mutableIntStateOf(0) }
    val songList = remember(lang, term, listVersion) { Database.songs(lang, term) }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.width(320.dp).fillMaxHeight()) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                langs.forEach { l ->
                    FilterChip(selected = lang == l.code, onClick = { lang = l.code; selected = null },
                        label = { Text(l.short) })
                }
            }
            OutlinedTextField(term, { term = it }, Modifier.fillMaxWidth(),
                label = { Text("Laharana / lohateny") }, singleLine = true)
            LazyColumn(Modifier.weight(1f)) {
                items(songList, key = { it.number }) { song ->
                    Column(Modifier.fillMaxWidth().clickable {
                        selected = song
                        parts = Database.songParts(lang, song.number)
                        title = song.title
                        author = song.author
                        lyricText = parts.joinToString("\n\n") {
                            (if (it.isRefrain) "[REFRAIN]\n" else "") + it.text
                        }
                        edit = false
                    }.padding(9.dp)) {
                        Text("${song.number}. ${song.title}", color = White, fontWeight = FontWeight.SemiBold)
                        Text(song.author, color = Muted, fontSize = 12.sp)
                    }
                    HorizontalDivider(color = CardColor)
                }
            }
        }

        Column(Modifier.weight(1f).fillMaxHeight()) {
            if (selected == null) {
                Text("Misafidiana hira", color = White)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${selected!!.number}. ${selected!!.title}", Modifier.weight(1f),
                        color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { edit = !edit }) { Text(if (edit) "Ajanony" else "Hanitsy") }
                    Button(onClick = {
                        val currentParts = Database.songParts(lang, selected!!.number)
                        val slides = currentParts.map { ProjectionSlide(it.text, it.isRefrain) }
                        update(Projection(title, "Fihirana ${selected!!.number}", slides, Database.setting("hymnFont", "38").toIntOrNull() ?: 38,
                            Database.setting("background", "")))
                        open()
                    }) { Text("Projeter") }
                }

                if (edit) {
                    OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Lohateny") })
                    OutlinedTextField(author, { author = it }, Modifier.fillMaxWidth(), label = { Text("Mpanoratra") })
                    OutlinedTextField(lyricText, { lyricText = it }, Modifier.weight(1f).fillMaxWidth(),
                        label = { Text("Tononkira — asio [REFRAIN] alohan'ny refrain") })
                    Button(onClick = {
                        Database.save("INSERT OR REPLACE INTO song_edits(lang,num,title,author) VALUES(?,?,?,?)",
                            listOf(lang, selected!!.number, title, author))
                        Database.save("DELETE FROM song_part_edits WHERE lang=? AND num=?", listOf(lang, selected!!.number))
                        lyricText.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() }.forEachIndexed { index, block ->
                            val refrain = block.trimStart().startsWith("[REFRAIN]", true)
                            val clean = block.trim().replaceFirst(Regex("^\\[REFRAIN\\]\\s*", RegexOption.IGNORE_CASE), "")
                            Database.save("INSERT INTO song_part_edits(lang,num,idx,kind,text) VALUES(?,?,?,?,?)",
                                listOf(lang, selected!!.number, index + 1, if (refrain) "r" else "v", clean))
                        }
                        parts = Database.songParts(lang, selected!!.number)
                        listVersion++
                        status = "Voatahiry ny fanitsiana."
                        edit = false
                    }) { Text("Tehirizo ny fanitsiana") }
                    Text(status, color = Gold)
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        items(parts) { part ->
                            Column(Modifier.fillMaxWidth().padding(7.dp)
                                .background(CardColor, RoundedCornerShape(8.dp)).padding(12.dp)) {
                                if (part.isRefrain) Text("REFRAIN / ISAN'ANDININY", color = Gold, fontWeight = FontWeight.Bold)
                                Text(part.text, color = White, fontSize = 17.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BibleScreen(update: (Projection) -> Unit, open: () -> Unit, active: Boolean) {
    val versions = remember { Database.versions() }
    var version by remember { mutableIntStateOf(versions.firstOrNull()?.id ?: 1) }
    val books = remember(version) { Database.books(version) }
    var book by remember { mutableIntStateOf(books.firstOrNull()?.id ?: 1) }
    var chapter by remember { mutableIntStateOf(1) }
    var chapterInput by remember { mutableStateOf("1") }
    var bookSearch by remember { mutableStateOf("") }
    var term by remember { mutableStateOf("") }
    var verses by remember { mutableStateOf(Database.chapter(version, book, chapter)) }
    var selected by remember { mutableStateOf<BibleVerse?>(null) }
    var vMenu by remember { mutableStateOf(false) }
    var bMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                OutlinedButton(onClick = { vMenu = true }) {
                    Text(versions.firstOrNull { it.id == version }?.short ?: "Version")
                }
                DropdownMenu(vMenu, { vMenu = false }) {
                    versions.forEach { v ->
                        DropdownMenuItem(text = { Text(v.name) }, onClick = {
                            version = v.id; vMenu = false
                            verses = Database.chapter(version, book, chapter)
                            selected?.let { old ->
                                val replacement = Database.verse(version, old.book, old.chapter, old.verse)
                                if (replacement != null) {
                                    selected = replacement
                                    val bookName = books.firstOrNull { it.id == replacement.book }?.name ?: replacement.book.toString()
                                    update(Projection("$bookName ${replacement.chapter}:${replacement.verse}", v.name,
                                        listOf(ProjectionSlide(replacement.text)),
                                        Database.setting("bibleFont", "38").toIntOrNull() ?: 38,
                                        Database.setting("background", "")))
                                    if (active) open()
                                }
                            }
                        })
                    }
                }
            }
            OutlinedTextField(bookSearch, { bookSearch = it }, Modifier.width(185.dp),
                label = { Text("Mitady boky") }, singleLine = true)
            Box {
                OutlinedButton(onClick = { bMenu = true }) {
                    Text(books.firstOrNull { it.id == book }?.name ?: "Boky")
                }
                DropdownMenu(bMenu, { bMenu = false }) {
                    books.filter { it.name.contains(bookSearch, true) || it.abbr.contains(bookSearch, true) }
                        .forEach { b ->
                            DropdownMenuItem(text = { Text(b.name) }, onClick = {
                                book = b.id; chapter = 1; chapterInput = "1"; bMenu = false
                                verses = Database.chapter(version, book, chapter)
                            })
                        }
                }
            }
            OutlinedTextField(chapterInput, { value ->
                chapterInput = value.filter { it.isDigit() }.take(3)
                chapterInput.toIntOrNull()?.let { n ->
                    val bounded = n.coerceIn(1, books.firstOrNull { it.id == book }?.chapters ?: 1)
                    if (bounded != chapter) {
                        chapter = bounded
                        verses = Database.chapter(version, book, chapter)
                    }
                }
            }, Modifier.width(75.dp), label = { Text("Toko") }, singleLine = true)
            OutlinedTextField(term, { term = it }, Modifier.weight(1f), label = { Text("Recherche verset") }, singleLine = true)
            Button(onClick = {
                verses = if (term.isBlank()) Database.chapter(version, book, chapter)
                else Database.searchBible(version, term)
            }) { Text("Hikaroka") }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = {
                chapter = (chapter - 1).coerceAtLeast(1)
                chapterInput = chapter.toString()
                verses = Database.chapter(version, book, chapter)
            }) { Text("← Toko teo aloha") }
            TextButton(onClick = {
                val max = books.firstOrNull { it.id == book }?.chapters ?: chapter
                chapter = (chapter + 1).coerceAtMost(max)
                chapterInput = chapter.toString()
                verses = Database.chapter(version, book, chapter)
            }) { Text("Toko manaraka →") }
        }

        LazyColumn(Modifier.weight(1f)) {
            items(verses, key = { "${it.book}:${it.chapter}:${it.verse}" }) { verse ->
                val bName = books.firstOrNull { it.id == verse.book }?.name ?: verse.book.toString()
                val ref = "$bName ${verse.chapter}:${verse.verse}"
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (selected == verse) CardColor else Color.Transparent)
                        .clickable {
                            selected = verse
                            val p = Projection(ref, versions.firstOrNull { it.id == version }?.name ?: "",
                                listOf(ProjectionSlide(verse.text)), Database.setting("bibleFont", "38").toIntOrNull() ?: 38,
                                Database.setting("background", ""))
                            update(p)
                            if (active) open()
                        }.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("${verse.verse}", Modifier.width(38.dp), color = Gold, fontWeight = FontWeight.Bold)
                    Column(Modifier.weight(1f)) {
                        if (!verse.heading.isNullOrBlank()) Text(verse.heading, color = Gold, fontWeight = FontWeight.Bold)
                        Text(verse.text, color = White, fontSize = 15.sp)
                    }
                    TextButton(onClick = {
                        val p = Projection(ref, versions.firstOrNull { it.id == version }?.name ?: "",
                            listOf(ProjectionSlide(verse.text)), Database.setting("bibleFont", "38").toIntOrNull() ?: 38,
                            Database.setting("background", ""))
                        update(p)
                        Database.save("INSERT INTO history(kind,title,body,created) VALUES(?,?,?,?)",
                            listOf("Bible", ref, verse.text, System.currentTimeMillis()))
                        open()
                    }) { Text("Projeter") }
                }
                HorizontalDivider(color = CardColor)
            }
        }
    }
}

@Composable
private fun CustomSongsScreen(update: (Projection) -> Unit, open: () -> Unit) {
    var category by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var lyrics by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var editingId by remember { mutableIntStateOf(-1) }
    val categories = remember { mutableStateListOf<String>().also { list ->
        Database.user.prepareStatement("SELECT name FROM categories ORDER BY name").use { p ->
            p.executeQuery().use { r -> while (r.next()) list.add(r.getString(1)) }
        }
    } }
    LaunchedEffect(categories.size) {
        if (category.isBlank() && categories.isNotEmpty()) category = categories.first()
    }
    var savedSongs by remember { mutableStateOf(emptyList<Triple<Int, String, String>>()) }
    fun reloadSavedSongs() {
        savedSongs = Database.user.prepareStatement(
            "SELECT id,title,lyrics FROM custom_songs WHERE category=? ORDER BY id DESC"
        ).use { p ->
            p.setString(1, category)
            p.executeQuery().use { r ->
                val rows = mutableListOf<Triple<Int, String, String>>()
                while (r.next()) rows.add(Triple(r.getInt(1), r.getString(2), r.getString(3)))
                rows
            }
        }
    }

    LaunchedEffect(category) {
        if (category.isNotBlank()) reloadSavedSongs() else savedSongs = emptyList()
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Mamorona sokajy sy hira manokana", color = White, fontSize = 20.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            categories.forEach { FilterChip(selected = category == it, onClick = { category = it; reloadSavedSongs() }, label = { Text(it) }) }
        }
        OutlinedTextField(category, { category = it }, Modifier.fillMaxWidth(), label = { Text("Sokajy (oh: Hiran'ny ankizy)") })
        OutlinedTextField(number, { number = it }, Modifier.fillMaxWidth(), label = { Text("Laharana (tsy voatery)") })
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Lohateny") })
        OutlinedTextField(lyrics, { lyrics = it }, Modifier.weight(1f).fillMaxWidth(), label = { Text("Tononkira") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (category.isNotBlank() && title.isNotBlank()) {
                    Database.save("INSERT OR IGNORE INTO categories(name) VALUES(?)", listOf(category.trim()))
                    val cleanCategory = category.trim()
                    if (editingId >= 0) {
                        Database.save("UPDATE custom_songs SET category=?,number=?,title=?,lyrics=? WHERE id=?",
                            listOf(cleanCategory, number, title, lyrics, editingId))
                    } else {
                        Database.save("INSERT INTO custom_songs(category,number,title,lyrics) VALUES(?,?,?,?)",
                            listOf(cleanCategory, number, title, lyrics))
                    }
                    category = cleanCategory
                    if (category !in categories) categories.add(category)
                    editingId = -1
                    reloadSavedSongs()
                    status = "Voatahiry."
                    number = ""; title = ""; lyrics = ""
                } else status = "Fenoy ny sokajy sy ny lohateny."
            }) { Text("Tehirizo") }
            Button(onClick = {
                if (title.isNotBlank()) {
                    update(Projection(title, category, listOf(ProjectionSlide(lyrics))))
                    open()
                }
            }) { Text("Projeter") }
        }
        Text(status, color = Gold)
        Text("Hira voatahiry ao amin'ity sokajy ity", color = Gold, fontWeight = FontWeight.Bold)
        LazyColumn(Modifier.weight(0.7f)) {
            items(savedSongs, key = { it.first }) { song ->
                ListItem(
                    headlineContent = { Text(song.second, color = White) },
                    supportingContent = { Text(song.third.take(100), color = Muted) },
                    trailingContent = {
                        Row {
                            TextButton(onClick = {
                                editingId = song.first
                                title = song.second; lyrics = song.third
                                Database.user.prepareStatement("SELECT number FROM custom_songs WHERE id=?").use { p ->
                                    p.setInt(1, song.first)
                                    p.executeQuery().use { r -> number = if (r.next()) r.getString(1) ?: "" else "" }
                                }
                            }) { Text("Hanitsy") }
                            TextButton(onClick = {
                                update(Projection(song.second, category, listOf(ProjectionSlide(song.third)),
                                    Database.setting("hymnFont", "38").toIntOrNull() ?: 38,
                                    Database.setting("background", "")))
                                open()
                            }) { Text("Projeter") }
                            TextButton(onClick = {
                                Database.save("DELETE FROM custom_songs WHERE id=?", listOf(song.first))
                                if (editingId == song.first) { editingId = -1; title = ""; number = ""; lyrics = "" }
                                reloadSavedSongs()
                            }) { Text("Fafana") }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TemporaryScreen(update: (Projection) -> Unit, open: () -> Unit) {
    var group by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var page by remember { mutableIntStateOf(1) }
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var tempFont by remember { mutableFloatStateOf(Database.setting("tempFont", "38").toFloatOrNull() ?: 38f) }
    var status by remember { mutableStateOf("") }

    fun savePage() {
        Database.save("DELETE FROM temp_slides WHERE group_id=? AND page=?", listOf(group, page))
        Database.save("INSERT INTO temp_slides(group_id,page,title,subtitle,body) VALUES(?,?,?,?,?)",
            listOf(group, page, title, subtitle, body))
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Projection vonjimaika — pejy maromaro", color = White, fontSize = 20.sp)
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Titre") })
        OutlinedTextField(subtitle, { subtitle = it }, Modifier.fillMaxWidth(), label = { Text("Sous-titre") })
        OutlinedTextField(body, { body = it }, Modifier.weight(1f).fillMaxWidth(), label = { Text("Contenu — pejy $page") })
        Text("Loko isaky ny teny: soraty {#FF0000}teny{/} (ohatra mena). Loko hafa: #D4A853 volamena, #4A9EFF manga, #FFFFFF fotsy.", color = Muted, fontSize = 12.sp)
        Text("Haben'ny soratra: ${tempFont.toInt()} pt", color = White)
        Slider(tempFont, { tempFont = it }, valueRange = 20f..72f)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                if (body.isNotBlank()) savePage()
                page = (page - 1).coerceAtLeast(1)
                body = Database.user.prepareStatement(
                    "SELECT body FROM temp_slides WHERE group_id=? AND page=? ORDER BY id DESC LIMIT 1"
                ).use { p ->
                    p.setString(1, group); p.setInt(2, page)
                    p.executeQuery().use { r -> if (r.next()) r.getString(1) else "" }
                }
            }) { Text("← Pejy") }
            Text("Pejy $page", color = White, modifier = Modifier.align(Alignment.CenterVertically))
            OutlinedButton(onClick = {
                savePage()
                page++
                body = ""
                status = "Voatahiry ny pejy teo aloha."
            }) { Text("Pejy manaraka →") }
            Button(onClick = {
                savePage()
                val all = Database.user.prepareStatement(
                    "SELECT body FROM temp_slides WHERE group_id=? ORDER BY page,id"
                ).use { p ->
                    p.setString(1, group)
                    p.executeQuery().use { r ->
                        val list = mutableListOf<ProjectionSlide>()
                        while (r.next()) list.add(ProjectionSlide(r.getString(1)))
                        list
                    }
                }
                Database.setSetting("tempFont", tempFont.toInt().toString())
                update(Projection(title, subtitle, all, tempFont.toInt(), Database.setting("background", "")))
                Database.save("INSERT INTO history(kind,title,body,created) VALUES(?,?,?,?)",
                    listOf("Projection temporaire", title, all.joinToString("\n\n") { it.text }, System.currentTimeMillis()))
                open()
                status = "Voatahiry ary nalefa projection."
            }) { Text("Tehirizo & Projeter") }
            OutlinedButton(onClick = {
                group = UUID.randomUUID().toString(); page = 1
                title = ""; subtitle = ""; body = ""; status = ""
            }) { Text("Vaovao") }
        }
        Text(status, color = Gold)
    }
}

@Composable
private fun HistoryScreen(update: (Projection) -> Unit, open: () -> Unit) {
    val rows = remember {
        Database.user.prepareStatement("SELECT title,body FROM history ORDER BY id DESC LIMIT 200").use { p ->
            p.executeQuery().use { r ->
                val list = mutableListOf<Pair<String, String>>()
                while (r.next()) list.add(r.getString(1) to r.getString(2))
                list
            }
        }
    }
    LazyColumn {
        items(rows) { row ->
            ListItem(
                headlineContent = { Text(row.first, color = White) },
                supportingContent = { Text(row.second.take(120), color = Muted) },
                trailingContent = {
                    TextButton(onClick = { update(Projection(row.first, "Tantara", listOf(ProjectionSlide(row.second)))); open() }) {
                        Text("Projeter")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsScreen(projection: Projection, update: (Projection) -> Unit) {
    var hymnFont by remember { mutableFloatStateOf(Database.setting("hymnFont", "38").toFloatOrNull() ?: 38f) }
    var bibleFont by remember { mutableFloatStateOf(Database.setting("bibleFont", "38").toFloatOrNull() ?: 38f) }
    var bg by remember { mutableStateOf(Database.setting("background", "")) }
    var zoom by remember { mutableFloatStateOf(Database.setting("backgroundZoom", "1.0").toFloatOrNull() ?: 1f) }
    var cropX by remember { mutableFloatStateOf(Database.setting("backgroundX", "0.0").toFloatOrNull() ?: 0f) }
    var cropY by remember { mutableFloatStateOf(Database.setting("backgroundY", "0.0").toFloatOrNull() ?: 0f) }
    var msg by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Réglages de projection", color = White, fontSize = 20.sp)
        Text("Haben'ny soratra hira: ${hymnFont.toInt()} pt", color = White)
        Slider(hymnFont, { hymnFont = it }, valueRange = 20f..72f)
        Text("Haben'ny soratra Baiboly: ${bibleFont.toInt()} pt", color = White)
        Slider(bibleFont, { bibleFont = it }, valueRange = 20f..72f)
        Text("Fonds intégrés", color = White, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Bleu nuit" to "preset:navy", "Forêt" to "preset:forest", "Bordeaux" to "preset:wine", "Dégradé" to "preset:gradient").forEach { (label, value) ->
                OutlinedButton(onClick = { bg = value; Database.setSetting("background", value); msg = "Fond $label sélectionné." }) {
                    Text(label, fontSize = 11.sp)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val chooser = JFileChooser()
                chooser.dialogTitle = "Misafidiana sary, GIF na vidéo"
                chooser.fileFilter = FileNameExtensionFilter(
                    "Images / GIF / Vidéo (PNG, JPG, BMP, GIF, MP4, M4V, MOV, AVI)",
                    "png", "jpg", "jpeg", "bmp", "gif", "mp4", "m4v", "mov", "avi"
                )
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    bg = chooser.selectedFile.absolutePath
                    Database.setSetting("background", bg)
                    msg = "Voafidy ny fond."
                }
            }) { Text("Misafidy fond image/GIF/vidéo") }
            OutlinedButton(onClick = { bg = ""; Database.setSetting("background", ""); msg = "Nesorina ny fond." }) {
                Text("Fond noir")
            }
        }
        Text(if (bg.isBlank()) "Fond: noir" else "Fond: ${File(bg).name}", color = Muted)
        Text("Recadrage — zoom: ${"%.2f".format(zoom)}×", color = White)
        Slider(zoom, { zoom = it }, valueRange = 1f..2.5f)
        Text("Déplacement horizontal", color = White)
        Slider(cropX, { cropX = it }, valueRange = -1f..1f)
        Text("Déplacement vertical", color = White)
        Slider(cropY, { cropY = it }, valueRange = -1f..1f)
        Button(onClick = {
            Database.setSetting("hymnFont", hymnFont.toInt().toString())
            Database.setSetting("bibleFont", bibleFont.toInt().toString())
            Database.setSetting("backgroundZoom", zoom.toString())
            Database.setSetting("backgroundX", cropX.toString())
            Database.setSetting("backgroundY", cropY.toString())
            update(projection.copy(background = bg, backgroundZoom = zoom, backgroundX = cropX, backgroundY = cropY))
            msg = "Voatahiry ny réglages."
        }) { Text("Tehirizo ny réglages") }
        Text(msg, color = Gold)
        Text(
            "Sary/GIF: zoom sy déplacement crop. Vidéo: MP4/M4V/MOV/AVI, mandeha miverimberina (loop). Miankina amin'ny codecs tohanan'ny Windows ny famakiana AVI/MOV.",
            color = Muted
        )
    }
}

@Composable
private fun ProjectionWindow(projection: Projection, onToggleFullscreen: () -> Unit = {}, onClose: () -> Unit = {}) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    var index by remember(projection.title, projection.slides) { mutableIntStateOf(0) }
    var showingRefrain by remember { mutableStateOf(false) }
    val slides = projection.safeSlides
    val current = slides.getOrElse(index) { slides.first() }

    Box(
        Modifier.fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.F11, Key.F -> { onToggleFullscreen(); true }
                    Key.Escape -> { onClose(); true }
                    Key.DirectionDown, Key.PageDown, Key.Spacebar -> {
                        showingRefrain = false
                        index = nextVerseIndex(slides, index, 1)
                        true
                    }
                    Key.DirectionUp, Key.PageUp -> {
                        showingRefrain = false
                        index = nextVerseIndex(slides, index, -1)
                        true
                    }
                    Key.DirectionRight -> {
                        showingRefrain = false
                        index = nextVerseIndex(slides, index, 1)
                        true
                    }
                    Key.DirectionLeft -> {
                        showingRefrain = false
                        index = nextVerseIndex(slides, index, -1)
                        true
                    }
                    Key.R -> {
                        if (showingRefrain) {
                            showingRefrain = false
                            index = nextVerseIndex(slides, index, 1)
                        } else {
                            val next = slides.getOrNull(index + 1)
                            if (next?.refrain == true) showingRefrain = true
                        }
                        true
                    }
                    else -> false
                }
            }
    ) {
        ProjectionBackground(projection.background, projection.backgroundZoom, projection.backgroundX, projection.backgroundY)
        Column(
            Modifier.fillMaxSize().padding(55.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(projection.title, color = Gold, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            if (projection.subtitle.isNotBlank()) Text(projection.subtitle, color = White, fontSize = 21.sp)
            Spacer(Modifier.height(24.dp))
            val displayed = if (showingRefrain) slides.getOrNull(index + 1)?.text ?: current.text else current.text
            RichProjectionText(displayed, fontSize = projection.font.sp, lineHeight = (projection.font + 12).sp)
        }
        Text(
            "← / → : andininy     R : refrain     F11 : plein écran     Échap : akatona",
            Modifier.align(Alignment.BottomCenter).padding(12.dp),
            color = Color(0xFF777777), fontSize = 11.sp
        )
    }
}

private fun nextVerseIndex(slides: List<ProjectionSlide>, current: Int, direction: Int): Int {
    var i = current + direction
    while (i in slides.indices && slides[i].refrain) i += direction
    return i.coerceIn(0, slides.lastIndex)
}


@Composable
private fun RichProjectionText(text: String, fontSize: TextUnit, lineHeight: TextUnit = TextUnit.Unspecified) {
    val annotated = remember(text) {
        val pattern = Regex("\\{#([0-9A-Fa-f]{6})\\}(.*?)\\{/\\}", setOf(RegexOption.DOT_MATCHES_ALL))
        buildAnnotatedString {
            var cursor = 0
            for (match in pattern.findAll(text)) {
                append(text.substring(cursor, match.range.first))
                val rgb = match.groupValues[1].toLongOrNull(16) ?: 0xFFFFFFL
                val content = match.groupValues[2]
                withStyle(SpanStyle(color = Color(0xFF000000L or rgb))) { append(content) }
                cursor = match.range.last + 1
            }
            append(text.substring(cursor))
        }
    }
    Text(annotated, color = White, fontSize = fontSize, lineHeight = lineHeight)
}

@Composable
private fun ProjectionBackground(path: String, zoom: Float, cropX: Float, cropY: Float) {
    val presetBrush = when (path) {
        "preset:navy" -> Brush.verticalGradient(listOf(Color(0xFF07182F), Color(0xFF102D50), Color(0xFF030A16)))
        "preset:forest" -> Brush.verticalGradient(listOf(Color(0xFF08251D), Color(0xFF174B37), Color(0xFF04130F)))
        "preset:wine" -> Brush.verticalGradient(listOf(Color(0xFF300D20), Color(0xFF641D36), Color(0xFF170610)))
        "preset:gradient" -> Brush.linearGradient(listOf(Color(0xFF10172D), Color(0xFF36204D), Color(0xFF071B27)))
        else -> null
    }
    if (presetBrush != null) {
        Box(Modifier.fillMaxSize().background(presetBrush))
    } else if (path.isBlank() || !File(path).exists()) {
        Box(Modifier.fillMaxSize().background(Color.Black))
    } else if (path.substringAfterLast('.', "").lowercase() in setOf("mp4", "m4v", "mov", "avi")) {
        VideoProjectionBackground(path, zoom, cropX, cropY)
    } else {
        SwingPanel(
            modifier = Modifier.fillMaxSize(),
            update = { it.repaint() },
            factory = {
                object : JPanel() {
                    private val image = javax.swing.ImageIcon(path)
                    override fun paintComponent(g: Graphics) {
                        super.paintComponent(g)
                        val img = image.image ?: return
                        val iw = img.getWidth(this).coerceAtLeast(1)
                        val ih = img.getHeight(this).coerceAtLeast(1)
                        val scale = maxOf(width.toDouble() / iw, height.toDouble() / ih) * zoom.coerceIn(1f, 2.5f)
                        val w = (iw * scale).toInt()
                        val h = (ih * scale).toInt()
                        val dx = width - w
                        val dy = height - h
                        val x = (dx / 2.0 + dx * cropX.coerceIn(-1f, 1f) / 2.0).toInt()
                        val y = (dy / 2.0 + dy * cropY.coerceIn(-1f, 1f) / 2.0).toInt()
                        (g as Graphics2D).drawImage(img, x, y, w, h, this)
                    }
                }
            }
        )
    }
}

private object JavaFxRuntime {
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

@Composable
private fun VideoProjectionBackground(path: String, zoom: Float, cropX: Float, cropY: Float) {
    key(path, zoom, cropX, cropY) {
        SwingPanel(
            modifier = Modifier.fillMaxSize(),
            factory = {
                try { JavaFxRuntime.ensureStarted() } catch (t: Throwable) { logStartupError(t) }
                val panel = JFXPanel()
                panel.background = java.awt.Color.BLACK
                Platform.runLater {
                    try {
                        val media = Media(File(path).toURI().toString())
                        val player = MediaPlayer(media)
                        val view = MediaView(player)
                        view.isPreserveRatio = false
                        val root = StackPane(view)
                        root.setStyle("-fx-background-color: black;")
                        root.widthProperty().addListener { _, _, value ->
                            val w = value.toDouble()
                            view.fitWidth = w * zoom.coerceIn(1f, 2.5f)
                            view.translateX = (w - view.fitWidth) * cropX.coerceIn(-1f, 1f) / 2.0
                        }
                        root.heightProperty().addListener { _, _, value ->
                            val h = value.toDouble()
                            view.fitHeight = h * zoom.coerceIn(1f, 2.5f)
                            view.translateY = (h - view.fitHeight) * cropY.coerceIn(-1f, 1f) / 2.0
                        }
                        view.fitWidth = 1280.0 * zoom.coerceIn(1f, 2.5f)
                        view.fitHeight = 720.0 * zoom.coerceIn(1f, 2.5f)
                        view.translateX = (1280.0 - view.fitWidth) * cropX.coerceIn(-1f, 1f) / 2.0
                        view.translateY = (720.0 - view.fitHeight) * cropY.coerceIn(-1f, 1f) / 2.0
                        panel.scene = Scene(root, FxColor.BLACK)
                        player.cycleCount = MediaPlayer.INDEFINITE
                        player.setOnReady { player.play() }
                        player.setOnError { player.dispose() }
                    } catch (_: Exception) {
                        panel.background = java.awt.Color.BLACK
                    }
                }
                panel
            }
        )
    }
}
