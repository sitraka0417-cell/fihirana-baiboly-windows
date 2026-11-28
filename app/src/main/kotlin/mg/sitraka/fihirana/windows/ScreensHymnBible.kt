package mg.sitraka.fihirana.windows

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ───────────────────────────── Composants communs ─────────────────────────────

@Composable
fun SlideCard(
    label: String,
    text: String,
    active: Boolean,
    live: Boolean,
    refrain: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (active) Ui.GoldSoft else Ui.Surface2,
        border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) Ui.Gold else Ui.Border),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label.uppercase(), color = if (refrain) Ui.Gold else Ui.Muted,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp
                )
                Spacer(Modifier.weight(1f))
                if (active && live) Text("● MIPOITRA", color = Ui.Ok, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(richText(text), color = Ui.Text, fontSize = 16.sp, lineHeight = 23.sp)
        }
    }
}

@Composable
private fun SongRow(song: Song, selected: Boolean, fav: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Ui.GoldSoft else Color.Transparent,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.width(48.dp).background(if (selected) Ui.Gold else Ui.Surface3, RoundedCornerShape(8.dp))
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${song.number}", color = if (selected) Ui.OnGold else Ui.Gold,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    song.title, color = Ui.Text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (song.author.isNotBlank()) {
                    Text(song.author, color = Ui.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (fav) Text("★", color = Ui.Gold, fontSize = 14.sp)
        }
    }
}

// ───────────────────────────── Fihirana ─────────────────────────────

/**
 * Construit les diapositives d'un chant.
 *
 * Refrain d'un couplet (par priorité) :
 *  1. refrain à portée explicite : « [REFRAIN 4] » ou « [REFRAIN 1-3] »
 *  2. refrain placé juste après le couplet
 *  3. dernier refrain sans portée placé avant le couplet
 *  4. premier refrain sans portée du chant
 * Sans « refrain après chaque couplet », les parties sont simplement affichées dans l'ordre.
 */
private val RepeatMark = Regex(":[;,]:\\s*(.*?)\\s*:[;,]:")

/** « :;: texte :;: » (ou « :,: ») devient « ‖: texte :‖ » pour l'affichage. */
fun tidyLyrics(text: String): String = RepeatMark.replace(text) { "‖: ${it.groupValues[1]} :‖" }

fun buildHymnSlides(parts: List<SongPart>, repeatRefrain: Boolean): List<ProjectionSlide> {
    var n = 0
    val verseNo = parts.map { if (it.isRefrain) 0 else ++n }
    fun refrainLabel(p: SongPart) = if (p.scopeText.isNotBlank()) "Refrain (${p.scopeText})" else "Refrain"

    if (!repeatRefrain) {
        return parts.mapIndexed { i, p ->
            if (p.isRefrain) ProjectionSlide(tidyLyrics(p.text), true, label = refrainLabel(p), scope = p.scope)
            else ProjectionSlide(tidyLyrics(p.text), false, label = "Andininy ${verseNo[i]}", verseNo = verseNo[i])
        }
    }

    val refrains = parts.withIndex().filter { it.value.isRefrain }
    fun refrainFor(i: Int, num: Int): SongPart? {
        refrains.firstOrNull { it.value.scope?.contains(num) == true }?.let { return it.value }
        val next = parts.getOrNull(i + 1)
        if (next != null && next.isRefrain && next.scope == null) return next
        refrains.lastOrNull { it.value.scope == null && it.index < i }?.let { return it.value }
        return refrains.firstOrNull { it.value.scope == null }?.value
    }

    val out = mutableListOf<ProjectionSlide>()
    parts.forEachIndexed { i, p ->
        if (!p.isRefrain) {
            out.add(ProjectionSlide(tidyLyrics(p.text), false, label = "Andininy ${verseNo[i]}", verseNo = verseNo[i]))
            refrainFor(i, verseNo[i])?.let { out.add(ProjectionSlide(tidyLyrics(it.text), true, label = refrainLabel(it), scope = it.scope)) }
        }
    }
    return out
}

/** Une partie du chant en cours d'édition. scope = couplets concernés si refrain particulier (ex. « 4 » ou « 1-3 »). */
data class EditPart(val refrain: Boolean, val text: String, val scope: String = "")

private fun normalizeScope(raw: String): String =
    raw.trim().replace(Regex("\\s*[-–]\\s*"), "-").replace(Regex("[\\s,;]+"), ",").trim(',')

@Composable
fun HymnScreen(ctrl: ProjectionController) {
    val focus = LocalFocusManager.current
    val langs = remember { Database.languages() }
    var lang by remember { mutableStateOf(langs.firstOrNull()?.code ?: "mg") }
    var term by remember { mutableStateOf("") }
    var listVersion by remember { mutableIntStateOf(0) }
    val allSongs = remember(lang, term, listVersion) { Database.songs(lang, term) }
    var favVersion by remember { mutableIntStateOf(0) }
    var onlyFav by remember { mutableStateOf(false) }
    val favs = remember(favVersion) { Database.favorites("hymn") }
    val songs = if (onlyFav) allSongs.filter { "${it.lang}:${it.number}" in favs } else allSongs
    var selected by remember { mutableStateOf<Song?>(null) }
    var parts by remember { mutableStateOf<List<SongPart>>(emptyList()) }
    var repeatRefrain by remember { mutableStateOf(Database.setting("refrainEach", "0") == "1") }
    var stagedIndex by remember { mutableIntStateOf(0) }
    var edit by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    val editParts = remember { mutableStateListOf<EditPart>() }
    var status by remember { mutableStateOf("") }

    val song = selected
    val contentId = if (song != null) "hymn:${song.lang}:${song.number}" else ""
    val slides = remember(parts, repeatRefrain) { buildHymnSlides(parts, repeatRefrain) }
    val activeIndex = if (song != null && ctrl.contentId == contentId) ctrl.index else stagedIndex
    val isLive = song != null && ctrl.live && ctrl.contentId == contentId
    fun heading(s: Song) = "${s.number}. ${s.title}"

    // Retrouver le chant courant en revenant sur cet écran
    LaunchedEffect(Unit) {
        val id = ctrl.contentId
        if (selected == null && id.startsWith("hymn:")) {
            val p = id.split(":")
            val n = p.getOrNull(2)?.toIntOrNull()
            if (p.size == 3 && n != null) {
                lang = p[1]
                Database.songs(p[1]).firstOrNull { it.number == n }?.let {
                    selected = it
                    parts = Database.songParts(it.lang, it.number)
                    title = it.title
                    author = it.author
                    stagedIndex = ctrl.index
                }
            }
        }
    }

    fun pick(s: Song) {
        selected = s
        parts = Database.songParts(s.lang, s.number)
        title = s.title
        author = s.author
        editParts.clear()
        editParts.addAll(parts.map { EditPart(it.isRefrain, it.text, it.scopeText) })
        edit = false
        status = ""
        stagedIndex = 0
        focus.clearFocus()
        if (!ctrl.live) {
            ctrl.load("hymn:${s.lang}:${s.number}", heading(s), "", buildHymnSlides(parts, repeatRefrain), 0, ContentKind.HYMN)
        }
    }

    fun pushCurrent(index: Int) {
        val s = selected ?: return
        ctrl.load(contentId, heading(s), "", slides, index, ContentKind.HYMN)
    }

    val listState = rememberLazyListState()
    LaunchedEffect(activeIndex, contentId, slides.size) {
        if (slides.isNotEmpty()) {
            val i = activeIndex.coerceIn(0, slides.lastIndex)
            if (listState.layoutInfo.visibleItemsInfo.none { it.index == i }) listState.animateScrollToItem(i)
        }
    }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        // ── Liste des chants
        Panel(Modifier.width(330.dp).fillMaxHeight(), padding = 12) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                langs.forEach { l ->
                    Chip(l.short, lang == l.code, { lang = l.code; selected = null; parts = emptyList() })
                }
                Chip("★ Tiana", onlyFav, { onlyFav = !onlyFav })
            }
            Spacer(Modifier.height(10.dp))
            AppField(
                term, { term = it }, "Laharana na lohateny  (Enter = misafidy)",
                Modifier.fillMaxWidth().onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && (e.key == Key.Enter || e.key == Key.NumPadEnter)) {
                        val hit = songs.firstOrNull { it.number.toString() == term.trim() } ?: songs.firstOrNull()
                        if (hit != null) pick(hit)
                        true
                    } else false
                }
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f)) {
                items(songs, key = { it.number }) { s ->
                    SongRow(s, selected?.number == s.number && selected?.lang == s.lang, "${s.lang}:${s.number}" in favs) { pick(s) }
                }
            }
        }

        // ── Détail
        Panel(Modifier.weight(1f).fillMaxHeight(), padding = 16) {
            if (song == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("♪", color = Ui.Gold, fontSize = 44.sp)
                        Text("Safidio ny hira ao amin'ny lisitra", color = Ui.Muted)
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(heading(song), color = Ui.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (song.author.isNotBlank()) Text(song.author, color = Ui.Muted, fontSize = 13.sp)
                    }
                    val favKey = "${song.lang}:${song.number}"
                    GhostButton(if (favKey in favs) "★" else "☆", {
                        Database.toggleFavorite("hymn", favKey)
                        favVersion++
                    })
                    GhostButton(if (edit) "Ajanony" else "Hanitsy", { edit = !edit })
                    GoldButton(if (isLive) "■  Akatona" else "▶  Projeter", {
                        if (isLive) ctrl.stop()
                        else {
                            pushCurrent(activeIndex)
                            ctrl.start()
                        }
                    }, enabled = slides.isNotEmpty())
                }
                Spacer(Modifier.height(10.dp))

                if (edit) {
                    AppField(title, { title = it }, "Lohateny", Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    AppField(author, { author = it }, "Mpanoratra", Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Andininy sy refrain. Ny refrain eo aorian'ny andininy dia an'io andininy io; raha tsy misy dia ilay refrain teo aloha no ampiasaina. " +
                            "Raha misy refrain ho an'andininy sasany ihany, fenoy ny \"Ho an'ny andininy\" (oh: 4 na 1-3).",
                        color = Ui.Muted, fontSize = 11.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyColumn(Modifier.weight(1f)) {
                        itemsIndexed(editParts) { i, ep ->
                            val verseNumber = editParts.take(i + 1).count { !it.refrain }
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    .background(Ui.Surface2, RoundedCornerShape(14.dp))
                                    .border(1.dp, if (ep.refrain) Ui.Gold else Ui.Border, RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        if (ep.refrain) "REFRAIN" else "ANDININY $verseNumber",
                                        color = if (ep.refrain) Ui.Gold else Ui.Muted,
                                        fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Chip("Andininy", !ep.refrain, { editParts[i] = ep.copy(refrain = false) })
                                    Chip("Refrain", ep.refrain, { editParts[i] = ep.copy(refrain = true) })
                                    GhostButton("↑", {
                                        if (i > 0) { val t = editParts[i]; editParts[i] = editParts[i - 1]; editParts[i - 1] = t }
                                    }, enabled = i > 0)
                                    GhostButton("↓", {
                                        if (i < editParts.lastIndex) { val t = editParts[i]; editParts[i] = editParts[i + 1]; editParts[i + 1] = t }
                                    }, enabled = i < editParts.lastIndex)
                                    GhostButton("Fafana", { editParts.removeAt(i) }, danger = true)
                                }
                                Spacer(Modifier.height(8.dp))
                                AppField(
                                    ep.text, { editParts[i] = ep.copy(text = it) }, "Tononkira",
                                    Modifier.fillMaxWidth(), singleLine = false, minLines = 3
                                )
                                if (ep.refrain) {
                                    Spacer(Modifier.height(8.dp))
                                    AppField(
                                        ep.scope, { editParts[i] = ep.copy(scope = it) },
                                        "Ho an'ny andininy (oh: 4 na 1-3) — avelao foana raha ho an'ny rehetra",
                                        Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        GhostButton("＋ Andininy", { editParts.add(EditPart(false, "")) })
                        GhostButton("＋ Refrain", { editParts.add(EditPart(true, "")) })
                        GhostButton("＋ Refrain ho an'andininy manokana", {
                            val last = editParts.count { !it.refrain }
                            editParts.add(EditPart(true, "", if (last > 0) last.toString() else ""))
                        })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GoldButton("Tehirizo ny fanitsiana", {
                            Database.save(
                                "INSERT OR REPLACE INTO song_edits(lang,num,title,author) VALUES(?,?,?,?)",
                                listOf(lang, song.number, title, author)
                            )
                            Database.save("DELETE FROM song_part_edits WHERE lang=? AND num=?", listOf(lang, song.number))
                            editParts.filter { it.text.isNotBlank() }.forEachIndexed { index, ep ->
                                val scopeNorm = normalizeScope(ep.scope)
                                val kind = when {
                                    !ep.refrain -> "v"
                                    scopeNorm.isNotEmpty() && SongPart.parseScope(scopeNorm) != null -> "r:$scopeNorm"
                                    else -> "r"
                                }
                                Database.save(
                                    "INSERT INTO song_part_edits(lang,num,idx,kind,text) VALUES(?,?,?,?,?)",
                                    listOf(lang, song.number, index + 1, kind, ep.text.trim())
                                )
                            }
                            parts = Database.songParts(lang, song.number)
                            selected = Database.songs(lang).firstOrNull { it.number == song.number } ?: song
                            listVersion++
                            status = "Voatahiry ny fanitsiana."
                            edit = false
                            ctrl.load(contentId, "${song.number}. $title", "", buildHymnSlides(parts, repeatRefrain), stagedIndex, ContentKind.HYMN)
                        })
                        Text(status, color = Ui.Ok, fontSize = 13.sp)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppSwitch(repeatRefrain) {
                            repeatRefrain = it
                            Database.setSetting("refrainEach", if (it) "1" else "0")
                            ctrl.load(contentId, heading(song), "", buildHymnSlides(parts, it), 0, ContentKind.HYMN)
                            stagedIndex = 0
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Averina ny refrain aorian'ny andininy rehetra", color = Ui.Text, fontSize = 13.sp)
                            Text("Raha ajanona: 1 → refrain → 2 → 3 araka ny filaharan'ny hira.", color = Ui.Muted, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.weight(1f), state = listState) {
                        itemsIndexed(slides) { i, sl ->
                            SlideCard(sl.label, sl.text, i == activeIndex, isLive, sl.refrain) {
                                stagedIndex = i
                                focus.clearFocus()
                                pushCurrent(i)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────────── Baiboly ─────────────────────────────

private fun norm(s: String): String =
    java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").replace(".", "").trim()

fun matchBooks(books: List<BibleBook>, query: String): List<BibleBook> {
    val q = norm(query)
    if (q.isEmpty()) return books
    val starts = books.filter { norm(it.name).startsWith(q) || norm(it.abbr).startsWith(q) }
    val contains = books.filter { it !in starts && norm(it.name).contains(q) }
    return starts + contains
}

data class BibleRef(val book: BibleBook, val chapter: Int, val from: Int?, val to: Int?)

/** « Sal 139:1-10 », « salamo 23 », « 1 Samoela 3:4 » */
fun parseBibleRef(input: String, books: List<BibleBook>): BibleRef? {
    val m = Regex("^(.*?)\\s+(\\d+)(?:\\s*[:.]\\s*(\\d+)(?:\\s*[-–]\\s*(\\d+))?)?\\s*$").find(input.trim()) ?: return null
    val name = norm(m.groupValues[1])
    if (name.isEmpty()) return null
    val book = books.firstOrNull { norm(it.name) == name || norm(it.abbr) == name }
        ?: matchBooks(books, m.groupValues[1]).firstOrNull() ?: return null
    return BibleRef(
        book, m.groupValues[2].toInt(),
        m.groupValues[3].toIntOrNull(), m.groupValues[4].toIntOrNull()
    )
}

@Composable
private fun NumCell(n: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Ui.Gold else Ui.Surface2,
        contentColor = if (selected) Ui.OnGold else Ui.Text,
        border = BorderStroke(1.dp, if (selected) Ui.Gold else Ui.Border),
        modifier = Modifier.padding(2.dp).fillMaxWidth().height(34.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("$n", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun BibleScreen(ctrl: ProjectionController) {
    val focus = LocalFocusManager.current
    val versions = remember { Database.versions() }
    var versionId by remember {
        mutableIntStateOf(
            Database.setting("bibleVersion", "").toIntOrNull()?.takeIf { id -> versions.any { it.id == id } }
                ?: versions.firstOrNull()?.id ?: 1
        )
    }
    val version = versions.firstOrNull { it.id == versionId }
    val books = remember(versionId) { Database.books(versionId) }
    var bookId by remember { mutableIntStateOf(-1) }
    var chapter by remember { mutableIntStateOf(1) }
    var verses by remember { mutableStateOf<List<BibleVerse>>(emptyList()) }
    var rangeFrom by remember { mutableIntStateOf(0) }
    var rangeTo by remember { mutableIntStateOf(0) }
    var stagedIndex by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var highlight by remember { mutableIntStateOf(0) }
    var changingBook by remember { mutableStateOf(false) }
    var refInput by remember { mutableStateOf("") }
    var refMsg by remember { mutableStateOf("") }
    var versionMenu by remember { mutableStateOf(false) }

    val book = books.firstOrNull { it.id == bookId }
    val bookName = book?.name ?: ""
    val showPicker = book == null || changingBook
    val matches = remember(query, books) { matchBooks(books, query) }
    val shown = remember(verses, rangeFrom, rangeTo) {
        if (rangeFrom > 0) verses.filter { it.verse in rangeFrom..rangeTo } else verses
    }
    val contentId = "bible:$versionId:$bookId:$chapter:$rangeFrom-$rangeTo"
    val activeIndex = if (ctrl.contentId == contentId) ctrl.index else stagedIndex
    val isLive = ctrl.live && ctrl.contentId == contentId
    val activeVerse = shown.getOrNull(activeIndex)?.verse ?: -1

    fun push(ver: BibleVersion?, bId: Int, bName: String, chap: Int, vs: List<BibleVerse>, from: Int, to: Int, index: Int) {
        val list = if (from > 0) vs.filter { it.verse in from..to } else vs
        val sl = list.map { ProjectionSlide(it.text, false, "$bName $chap:${it.verse}", ver?.short ?: "") }
        ctrl.load(
            "bible:${ver?.id ?: 0}:$bId:$chap:$from-$to", "$bName $chap", ver?.name ?: "",
            sl, index.coerceIn(0, (sl.size - 1).coerceAtLeast(0)), ContentKind.BIBLE
        )
    }

    fun openBook(b: BibleBook) {
        bookId = b.id
        chapter = 1
        verses = Database.chapter(versionId, b.id, 1)
        rangeFrom = 0; rangeTo = 0
        stagedIndex = 0
        query = ""; highlight = 0
        changingBook = false
        refMsg = ""
        focus.clearFocus()
        if (!ctrl.live) push(version, b.id, b.name, 1, verses, 0, 0, 0)
    }

    fun openChapter(c: Int) {
        val b = book ?: return
        val n = c.coerceIn(1, b.chapters)
        chapter = n
        verses = Database.chapter(versionId, b.id, n)
        rangeFrom = 0; rangeTo = 0
        stagedIndex = 0
        if (!ctrl.live) push(version, b.id, b.name, n, verses, 0, 0, 0)
    }

    fun selectVerseIndex(i: Int) {
        stagedIndex = i
        focus.clearFocus()
        push(version, bookId, bookName, chapter, verses, rangeFrom, rangeTo, i)
    }

    fun pickVerseNumber(n: Int) {
        val inShown = shown.indexOfFirst { it.verse == n }
        if (inShown >= 0) selectVerseIndex(inShown)
        else {
            rangeFrom = 0; rangeTo = 0
            val idx = verses.indexOfFirst { it.verse == n }.coerceAtLeast(0)
            stagedIndex = idx
            push(version, bookId, bookName, chapter, verses, 0, 0, idx)
        }
    }

    fun applyRef() {
        val r = parseBibleRef(refInput, books)
        if (r == null) {
            refMsg = "Tsy hita. Ohatra: Sal 139:1-10"
            return
        }
        val chap = r.chapter.coerceIn(1, r.book.chapters)
        val vs = Database.chapter(versionId, r.book.id, chap)
        bookId = r.book.id; chapter = chap; verses = vs; changingBook = false
        var from = 0; var to = 0; var index = 0
        if (r.from != null && r.to != null && r.to > r.from) {
            from = r.from; to = r.to
        } else if (r.from != null) {
            index = vs.indexOfFirst { it.verse == r.from }.coerceAtLeast(0)
        }
        rangeFrom = from; rangeTo = to; stagedIndex = index
        refMsg = ""
        focus.clearFocus()
        push(version, r.book.id, r.book.name, chap, vs, from, to, index)
    }

    fun changeVersion(v: BibleVersion) {
        versionId = v.id
        Database.setSetting("bibleVersion", v.id.toString())
        if (bookId >= 0) {
            verses = Database.chapter(v.id, bookId, chapter)
            val name = Database.books(v.id).firstOrNull { it.id == bookId }?.name ?: bookName
            if (ctrl.live && ctrl.contentId.startsWith("bible:")) push(v, bookId, name, chapter, verses, rangeFrom, rangeTo, activeIndex)
            else push(v, bookId, name, chapter, verses, rangeFrom, rangeTo, stagedIndex)
        }
    }

    val bookFocus = remember { FocusRequester() }
    LaunchedEffect(showPicker) {
        if (showPicker) runCatching { bookFocus.requestFocus() }
    }
    val verseListState = rememberLazyListState()
    LaunchedEffect(activeIndex, contentId, shown.size) {
        if (shown.isNotEmpty()) {
            val i = activeIndex.coerceIn(0, shown.lastIndex)
            if (verseListState.layoutInfo.visibleItemsInfo.none { it.index == i }) verseListState.animateScrollToItem(i)
        }
    }
    val matchState = rememberLazyListState()
    LaunchedEffect(highlight) {
        if (matches.isNotEmpty()) matchState.animateScrollToItem(highlight.coerceIn(0, matches.lastIndex))
    }

    Column(Modifier.fillMaxSize()) {
        // ── Barre du haut : version · référence rapide · Projeter
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box {
                GhostButton("${version?.short ?: "Version"}  ▾", { versionMenu = true })
                DropdownMenu(versionMenu, { versionMenu = false }) {
                    versions.forEach { v ->
                        DropdownMenuItem(text = { Text(v.name) }, onClick = { versionMenu = false; changeVersion(v) })
                    }
                }
            }
            AppField(
                refInput, { refInput = it; refMsg = "" }, "Fidirana haingana — oh: Sal 139:1-10  (Enter)",
                Modifier.weight(1f).onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && (e.key == Key.Enter || e.key == Key.NumPadEnter)) {
                        applyRef(); true
                    } else false
                }
            )
            GoldButton(
                if (isLive) "■  Akatona" else "▶  Projeter",
                {
                    if (isLive) ctrl.stop()
                    else {
                        push(version, bookId, bookName, chapter, verses, rangeFrom, rangeTo, activeIndex)
                        ctrl.start()
                    }
                },
                enabled = shown.isNotEmpty()
            )
        }
        if (refMsg.isNotBlank()) Text(refMsg, color = Ui.Danger, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(12.dp))

        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // ── Sélection boky / toko / andininy
            Panel(Modifier.width(340.dp).fillMaxHeight(), padding = 12) {
                if (showPicker) {
                    AppField(
                        query, { query = it; highlight = 0 }, "Mitady boky — soraty oh: Sa",
                        Modifier.fillMaxWidth().focusRequester(bookFocus).onPreviewKeyEvent { e ->
                            if (e.type != KeyEventType.KeyDown) false
                            else when (e.key) {
                                Key.DirectionDown -> { if (matches.isNotEmpty()) highlight = (highlight + 1).coerceAtMost(matches.lastIndex); true }
                                Key.DirectionUp -> { highlight = (highlight - 1).coerceAtLeast(0); true }
                                Key.Enter, Key.NumPadEnter -> { matches.getOrNull(highlight)?.let { openBook(it) }; true }
                                else -> false
                            }
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("↑ ↓ mba hisafidy · Enter mba hanamarina", color = Ui.Muted, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    LazyColumn(Modifier.weight(1f), state = matchState) {
                        itemsIndexed(matches) { i, b ->
                            Surface(
                                onClick = { openBook(b) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (i == highlight) Ui.GoldSoft else Color.Transparent,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                            ) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(b.name, color = if (i == highlight) Ui.Gold else Ui.Text,
                                        fontWeight = if (i == highlight) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f))
                                    Text("${b.chapters} toko", color = Ui.Muted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                } else if (book != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(book.name, color = Ui.Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f))
                        GhostButton("Hanova boky", { changingBook = true; query = "" })
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            SectionLabel("Toko")
                            Spacer(Modifier.height(6.dp))
                            LazyVerticalGrid(GridCells.Adaptive(46.dp), Modifier.weight(1f)) {
                                items(book.chapters) { i -> NumCell(i + 1, chapter == i + 1) { openChapter(i + 1) } }
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            SectionLabel("Andininy")
                            Spacer(Modifier.height(6.dp))
                            LazyVerticalGrid(GridCells.Adaptive(46.dp), Modifier.weight(1f)) {
                                items(verses.size) { i ->
                                    val n = verses[i].verse
                                    NumCell(n, n == activeVerse) { pickVerseNumber(n) }
                                }
                            }
                        }
                    }
                }
            }

            // ── Texte
            Panel(Modifier.weight(1f).fillMaxHeight(), padding = 16) {
                if (book == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✝", color = Ui.Gold, fontSize = 44.sp)
                            Text("Soraty ny anaran'ny boky (oh: Sa) na ny fidirana haingana (oh: Sal 139:1-10)", color = Ui.Muted)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("$bookName $chapter", color = Ui.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif)
                            Text(version?.name ?: "", color = Ui.Muted, fontSize = 12.sp)
                        }
                        if (rangeFrom > 0) Chip("Andininy $rangeFrom–$rangeTo  ✕", true, {
                            rangeFrom = 0; rangeTo = 0; stagedIndex = 0
                            push(version, bookId, bookName, chapter, verses, 0, 0, 0)
                        })
                        GhostButton("‹ Toko", { openChapter(chapter - 1) }, enabled = chapter > 1)
                        GhostButton("Toko ›", { openChapter(chapter + 1) }, enabled = chapter < (book.chapters))
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(Modifier.weight(1f), state = verseListState) {
                        itemsIndexed(shown) { i, v ->
                            val active = i == activeIndex
                            Surface(
                                onClick = { selectVerseIndex(i) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) Ui.GoldSoft else Color.Transparent,
                                border = if (active) BorderStroke(1.5.dp, Ui.Gold) else null,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                    Text("${v.verse}", Modifier.width(36.dp), color = Ui.Gold, fontWeight = FontWeight.Bold)
                                    Column(Modifier.weight(1f)) {
                                        if (!v.heading.isNullOrBlank()) {
                                            Text(v.heading, color = Ui.Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Text(v.text, color = Ui.Text, fontSize = 16.sp, lineHeight = 23.sp)
                                    }
                                    if (active && isLive) Text("●", color = Ui.Ok, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
