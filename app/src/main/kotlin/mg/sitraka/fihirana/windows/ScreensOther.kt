package mg.sitraka.fihirana.windows

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.util.UUID
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private fun pagesOf(text: String): List<ProjectionSlide> =
    text.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
        .mapIndexed { i, t -> ProjectionSlide(t, false, label = "Pejy ${i + 1}") }

@Composable
private fun ListRow(
    title: String,
    sub: String,
    live: Boolean,
    onProject: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .background(Ui.Surface2, RoundedCornerShape(12.dp))
            .border(1.dp, if (live) Ui.Gold else Ui.Border, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(title, color = Ui.Text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (sub.isNotBlank()) {
            Text(sub.replace("\n", " ").take(110), color = Ui.Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GoldButton(if (live) "■ Akatona" else "▶ Projeter", onProject)
            if (onEdit != null) GhostButton("Hanitsy", onEdit)
            if (onDelete != null) GhostButton("Fafana", onDelete, danger = true)
        }
    }
}

// ───────────────────────────── Hira manokana ─────────────────────────────

private data class CustomSong(val id: Int, val number: String, val title: String, val lyrics: String)

@Composable
fun CustomSongsScreen(ctrl: ProjectionController) {
    val focus = LocalFocusManager.current
    var category by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var lyrics by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var editingId by remember { mutableIntStateOf(-1) }
    val categories = remember {
        mutableStateListOf<String>().also { list ->
            Database.user.prepareStatement("SELECT name FROM categories ORDER BY name").use { p ->
                p.executeQuery().use { r -> while (r.next()) list.add(r.getString(1)) }
            }
        }
    }
    LaunchedEffect(categories.size) {
        if (category.isBlank() && categories.isNotEmpty()) category = categories.first()
    }
    var saved by remember { mutableStateOf(emptyList<CustomSong>()) }
    fun reload() {
        saved = if (category.isBlank()) emptyList() else Database.user.prepareStatement(
            "SELECT id,number,title,lyrics FROM custom_songs WHERE category=? ORDER BY id DESC"
        ).use { p ->
            p.setString(1, category.trim())
            p.executeQuery().use { r ->
                val rows = mutableListOf<CustomSong>()
                while (r.next()) rows.add(CustomSong(r.getInt(1), r.getString(2) ?: "", r.getString(3) ?: "", r.getString(4) ?: ""))
                rows
            }
        }
    }
    LaunchedEffect(category) { reload() }

    fun toggle(id: String, t: String, text: String, sub: String) {
        if (ctrl.live && ctrl.contentId == id) ctrl.stop()
        else {
            ctrl.load(id, t, sub, pagesOf(text), 0, ContentKind.OTHER)
            ctrl.start()
        }
    }

    val draftId = "custom:draft"
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Panel(Modifier.weight(1f).fillMaxHeight()) {
            SectionLabel("Sokajy")
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { c -> Chip(c, category == c, { category = c }) }
            }
            Spacer(Modifier.height(8.dp))
            AppField(category, { category = it }, "Sokajy (oh: Hiran'ny ankizy)", Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppField(number, { number = it }, "Laharana (tsy voatery)", Modifier.width(190.dp))
                AppField(title, { title = it }, "Lohateny", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            AppField(
                lyrics, { lyrics = it }, "Tononkira — ataovy misaraka amin'ny andalana banga ny pejy tsirairay",
                Modifier.weight(1f).fillMaxWidth(), singleLine = false, minLines = 6
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldButton(if (editingId >= 0) "Tehirizo ny fanovana" else "Tehirizo", {
                    if (category.isNotBlank() && title.isNotBlank()) {
                        val cat = category.trim()
                        Database.save("INSERT OR IGNORE INTO categories(name) VALUES(?)", listOf(cat))
                        if (editingId >= 0) {
                            Database.save(
                                "UPDATE custom_songs SET category=?,number=?,title=?,lyrics=? WHERE id=?",
                                listOf(cat, number, title, lyrics, editingId)
                            )
                        } else {
                            Database.save(
                                "INSERT INTO custom_songs(category,number,title,lyrics) VALUES(?,?,?,?)",
                                listOf(cat, number, title, lyrics)
                            )
                        }
                        category = cat
                        if (cat !in categories) categories.add(cat)
                        editingId = -1
                        number = ""; title = ""; lyrics = ""
                        reload()
                        status = "Voatahiry."
                    } else status = "Fenoy ny sokajy sy ny lohateny."
                })
                GhostButton(
                    if (ctrl.live && ctrl.contentId == draftId) "■ Akatona" else "▶ Projeter ity",
                    { if (title.isNotBlank() || lyrics.isNotBlank()) toggle(draftId, title, lyrics, category) },
                    enabled = lyrics.isNotBlank()
                )
                if (editingId >= 0) GhostButton("Foana", { editingId = -1; number = ""; title = ""; lyrics = "" })
                Text(status, color = Ui.Ok, fontSize = 13.sp)
            }
        }

        Panel(Modifier.width(340.dp).fillMaxHeight(), padding = 12) {
            SectionLabel("Hira voatahiry — ${category.ifBlank { "…" }}")
            Spacer(Modifier.height(6.dp))
            LazyColumn(Modifier.weight(1f)) {
                items(saved, key = { it.id }) { s ->
                    val id = "custom:${s.id}"
                    ListRow(
                        (if (s.number.isNotBlank()) "${s.number}. " else "") + s.title, s.lyrics,
                        ctrl.live && ctrl.contentId == id,
                        onProject = {
                            focus.clearFocus()
                            toggle(id, s.title, s.lyrics, category)
                        },
                        onEdit = {
                            editingId = s.id; number = s.number; title = s.title; lyrics = s.lyrics
                        },
                        onDelete = {
                            Database.save("DELETE FROM custom_songs WHERE id=?", listOf(s.id))
                            if (editingId == s.id) { editingId = -1; title = ""; number = ""; lyrics = "" }
                            reload()
                        }
                    )
                }
            }
        }
    }
}

// ───────────────────────────── Projection vonjimaika ─────────────────────────────

@Composable
fun TemporaryScreen(ctrl: ProjectionController) {
    var group by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var page by remember { mutableIntStateOf(1) }
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    fun savePage() {
        Database.save("DELETE FROM temp_slides WHERE group_id=? AND page=?", listOf(group, page))
        Database.save(
            "INSERT INTO temp_slides(group_id,page,title,subtitle,body) VALUES(?,?,?,?,?)",
            listOf(group, page, title, subtitle, body)
        )
    }

    fun pageBody(p: Int): String = Database.user.prepareStatement(
        "SELECT body FROM temp_slides WHERE group_id=? AND page=? ORDER BY id DESC LIMIT 1"
    ).use { st ->
        st.setString(1, group); st.setInt(2, p)
        st.executeQuery().use { r -> if (r.next()) r.getString(1) ?: "" else "" }
    }

    fun allSlides(): List<ProjectionSlide> = Database.user.prepareStatement(
        "SELECT page,body FROM temp_slides WHERE group_id=? ORDER BY page,id"
    ).use { st ->
        st.setString(1, group)
        st.executeQuery().use { r ->
            val latest = linkedMapOf<Int, String>()
            while (r.next()) latest[r.getInt(1)] = r.getString(2) ?: ""
            latest.entries.sortedBy { it.key }.filter { it.value.isNotBlank() }
                .map { ProjectionSlide(it.value, false, label = "Pejy ${it.key}") }
        }
    }

    val id = "temp:$group"
    val isLive = ctrl.live && ctrl.contentId == id

    Panel(Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppField(title, { title = it }, "Lohateny", Modifier.weight(1f))
            AppField(subtitle, { subtitle = it }, "Lohateny faharoa", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        AppField(
            body, { body = it }, "Votoaty — pejy $page", Modifier.weight(1f).fillMaxWidth(),
            singleLine = false, minLines = 6
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Loko isaky ny teny: {#FF0000}teny{/}  ·  #D4A853 volamena · #4A9EFF manga · #FFFFFF fotsy.  Haben'ny soratra: Ctrl + ↑ / ↓ rehefa mandeha ny projection.",
            color = Ui.Muted, fontSize = 12.sp
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("‹ Pejy", {
                if (body.isNotBlank()) savePage()
                page = (page - 1).coerceAtLeast(1)
                body = pageBody(page)
            }, enabled = page > 1)
            Text("Pejy $page", color = Ui.Text, fontWeight = FontWeight.SemiBold)
            GhostButton("Pejy manaraka ›", {
                savePage()
                page++
                body = pageBody(page)
                status = "Voatahiry ny pejy teo aloha."
            })
            GoldButton(if (isLive) "■ Akatona" else "Tehirizo & ▶ Projeter", {
                if (isLive) ctrl.stop()
                else {
                    savePage()
                    val all = allSlides()
                    Database.save(
                        "INSERT INTO history(kind,title,body,created) VALUES(?,?,?,?)",
                        listOf("Projection temporaire", title, all.joinToString("\n\n") { it.text }, System.currentTimeMillis())
                    )
                    ctrl.load(id, title, subtitle, all, 0, ContentKind.OTHER)
                    ctrl.start()
                    status = "Voatahiry ary nalefa."
                }
            })
            GhostButton("Vaovao", {
                group = UUID.randomUUID().toString(); page = 1
                title = ""; subtitle = ""; body = ""; status = ""
            })
            Text(status, color = Ui.Ok, fontSize = 13.sp)
        }
    }
}

// ───────────────────────────── Tantara ─────────────────────────────

@Composable
fun HistoryScreen(ctrl: ProjectionController) {
    var version by remember { mutableIntStateOf(0) }
    val rows = remember(version) {
        Database.user.prepareStatement("SELECT id,title,body FROM history ORDER BY id DESC LIMIT 200").use { p ->
            p.executeQuery().use { r ->
                val list = mutableListOf<Triple<Int, String, String>>()
                while (r.next()) list.add(Triple(r.getInt(1), r.getString(2) ?: "", r.getString(3) ?: ""))
                list
            }
        }
    }
    Panel(Modifier.fillMaxSize(), padding = 14) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Projection farany (${rows.size})", Modifier.weight(1f))
            GhostButton("Fafana daholo", {
                Database.save("DELETE FROM history")
                version++
            }, enabled = rows.isNotEmpty(), danger = true)
        }
        Spacer(Modifier.height(8.dp))
        if (rows.isEmpty()) Text("Mbola tsy misy tantara.", color = Ui.Muted)
        LazyColumn(Modifier.weight(1f)) {
            items(rows, key = { it.first }) { row ->
                val id = "history:${row.first}"
                ListRow(row.second, row.third, ctrl.live && ctrl.contentId == id, onProject = {
                    if (ctrl.live && ctrl.contentId == id) ctrl.stop()
                    else {
                        ctrl.load(id, row.second, "Tantara", pagesOf(row.third).ifEmpty { listOf(ProjectionSlide(row.third)) }, 0, ContentKind.OTHER)
                        ctrl.start()
                    }
                })
            }
        }
    }
}

// ───────────────────────────── Réglages ─────────────────────────────

@Composable
fun SettingsScreen(ctrl: ProjectionController) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        FontPanel(ctrl)
        ScreenPanel()
        BackgroundCard("hymn", "Fond — Fihirana sy pejy hafa", ctrl)
        BackgroundCard("bible", "Fond — Baiboly", ctrl)
        Panel(Modifier.fillMaxWidth()) {
            SectionLabel("Kitendry vonjimaika (rehefa mandeha ny projection)")
            Spacer(Modifier.height(8.dp))
            listOf(
                "← → (na ↑ ↓, PageUp / PageDown)" to "Pejy teo aloha / manaraka",
                "R" to "Mankany amin'ny refrain · tsindrio indray = andininy manaraka",
                "B  (na .)" to "Afenina ny soratra, mijanona ny fond · tsindrio indray hanehoana",
                "Home / End" to "Pejy voalohany / farany",
                "Ctrl + ↑ ↓  (na Ctrl + ← →)" to "Hanalehibe / hampiay ny soratra",
                "Échap" to "Hampitsahatra ny projection",
                "Tsindrio ny andininy / pejy" to "Hanasongadina azy eo amin'ny efijery avy hatrany"
            ).forEach { (k, v) ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text(k, Modifier.width(260.dp), color = Ui.Gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(v, color = Ui.Text, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun FontPanel(ctrl: ProjectionController) {
    var hymn by remember { mutableFloatStateOf(Database.setting("hymnFont", "38").toFloatOrNull() ?: 38f) }
    var bible by remember { mutableFloatStateOf(Database.setting("bibleFont", "38").toFloatOrNull() ?: 38f) }
    var temp by remember { mutableFloatStateOf(Database.setting("tempFont", "38").toFloatOrNull() ?: 38f) }
    Panel(Modifier.fillMaxWidth()) {
        SectionLabel("Haben'ny soratra")
        Spacer(Modifier.height(8.dp))
        @Composable
        fun row(label: String, value: Float, key: String, set: (Float) -> Unit) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$label — ${value.toInt()} pt", Modifier.width(230.dp), color = Ui.Text, fontSize = 13.sp)
                AppSlider(value, set, 20f..90f, Modifier.weight(1f), onFinished = {
                    Database.setSetting(key, value.toInt().toString())
                    ctrl.reloadStyle()
                })
            }
        }
        row("Fihirana", hymn, "hymnFont") { hymn = it }
        row("Baiboly", bible, "bibleFont") { bible = it }
        row("Pejy hafa (vonjimaika, hira manokana)", temp, "tempFont") { temp = it }
    }
}

@Composable
private fun ScreenPanel() {
    var choice by remember { mutableStateOf(Database.setting("projectionScreen", "auto")) }
    val screens = remember { listScreens() }
    Panel(Modifier.fillMaxWidth()) {
        SectionLabel("Écran de projection")
        Spacer(Modifier.height(6.dp))
        Text(
            if (screens.size > 1) "${screens.size} écrans hita. « Auto » dia mampiasa ny écran faharoa (projecteur / TV)."
            else "Écran iray ihany no hita. Mifandray amin'ny projecteur / TV dia hiseho ho azy ao amin'ny « Auto ».",
            color = Ui.Muted, fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip("Auto", choice == "auto", { choice = "auto"; Database.setSetting("projectionScreen", "auto") })
            Chip("Fenêtre", choice == "window", { choice = "window"; Database.setSetting("projectionScreen", "window") })
            screens.forEach { s ->
                Chip(s.label, choice == s.index.toString(), {
                    choice = s.index.toString(); Database.setSetting("projectionScreen", s.index.toString())
                })
            }
        }
        if (choice == screens.firstOrNull { it.primary }?.index?.toString()) {
            Spacer(Modifier.height(6.dp))
            Text("Tandremo: ity no écran misy ny rindrambaiko, ka hosarahany ny projection.", color = Ui.Danger, fontSize = 12.sp)
        }
    }
}

@Composable
private fun BackgroundCard(key: String, heading: String, ctrl: ProjectionController) {
    var spec by remember { mutableStateOf(Backgrounds.load(key)) }
    var library by remember { mutableStateOf(Backgrounds.library()) }
    var msg by remember { mutableStateOf("") }
    fun commit(s: BackgroundSpec) {
        spec = s
        Backgrounds.save(key, s)
        ctrl.reloadStyle()
    }

    Panel(Modifier.fillMaxWidth()) {
        SectionLabel(heading)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            // Aperçu
            Box(
                Modifier.width(300.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Ui.Border, RoundedCornerShape(12.dp))
            ) {
                BackgroundLayer(spec, Modifier.fillMaxSize())
                Column(
                    Modifier.fillMaxSize().padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Text("Andininy 1", color = Ui.Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Voninahitra ho an'Andriamanitra\neny amin'ny avo indrindra", color = Ui.Text, fontSize = 14.sp,
                        fontWeight = FontWeight.Medium, lineHeight = 19.sp)
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Fond efa vonona", color = Ui.Muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BackgroundPresets.forEach { (value, name) ->
                        val brush = presetBrush(value)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.width(58.dp).height(36.dp).clip(RoundedCornerShape(8.dp))
                                    .then(if (brush != null) Modifier.background(brush) else Modifier)
                                    .border(
                                        if (spec.path == value) 2.dp else 1.dp,
                                        if (spec.path == value) Ui.Gold else Ui.Border, RoundedCornerShape(8.dp)
                                    )
                                    .clickable { commit(spec.copy(path = value)) }
                            )
                            Text(name, color = Ui.Muted, fontSize = 10.sp)
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.width(58.dp).height(36.dp).clip(RoundedCornerShape(8.dp)).background(androidx.compose.ui.graphics.Color.Black)
                                .border(if (spec.path.isBlank()) 2.dp else 1.dp, if (spec.path.isBlank()) Ui.Gold else Ui.Border, RoundedCornerShape(8.dp))
                                .clickable { commit(spec.copy(path = "")) }
                        )
                        Text("Mainty", color = Ui.Muted, fontSize = 10.sp)
                    }
                }

                Text("Sary · GIF · Vidéo napetrako", color = Ui.Muted, fontSize = 12.sp)
                if (library.isEmpty()) Text("Mbola tsy misy. Tsindrio ny bokotra eto ambany.", color = Ui.Muted, fontSize = 11.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    library.forEach { f ->
                        Chip(f.name.take(28), spec.path == f.absolutePath, { commit(spec.copy(path = f.absolutePath)) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoldButton("＋ Hampiditra sary / GIF / vidéo", {
                        val chooser = JFileChooser()
                        chooser.dialogTitle = "Misafidiana sary, GIF na vidéo"
                        chooser.fileFilter = FileNameExtensionFilter(
                            "Sary / GIF / Vidéo (PNG, JPG, BMP, GIF, MP4, M4V, MOV, AVI)",
                            "png", "jpg", "jpeg", "bmp", "gif", "mp4", "m4v", "mov", "avi"
                        )
                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            try {
                                val copied = Backgrounds.import(chooser.selectedFile)
                                library = Backgrounds.library()
                                commit(spec.copy(path = copied.absolutePath))
                                msg = "Nampidirina: ${copied.name}"
                            } catch (t: Throwable) {
                                msg = "Tsy nety: ${t.message}"
                            }
                        }
                    })
                    val inLibrary = library.any { it.absolutePath == spec.path }
                    GhostButton("Fafana ity rakitra ity", {
                        File(spec.path).delete()
                        library = Backgrounds.library()
                        commit(spec.copy(path = ""))
                    }, enabled = inLibrary, danger = true)
                    Text(msg, color = Ui.Ok, fontSize = 12.sp)
                }
            }
        }

        if (spec.path.isNotBlank() && !spec.isPreset) {
            Spacer(Modifier.height(10.dp))
            if (spec.isVideo) {
                Text("Vidéo: hiseho amin'ny écran faharoa (projecteur) fotsiny, ary tsy misy feo.", color = Ui.Muted, fontSize = 12.sp)
            }
            @Composable
            fun slider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, set: (Float) -> Unit) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, Modifier.width(170.dp), color = Ui.Text, fontSize = 13.sp)
                    AppSlider(value, set, range, Modifier.weight(1f), onFinished = { commit(spec) })
                }
            }
            slider("Zoom ${"%.2f".format(spec.zoom)}×", spec.zoom, 1f..3f) { spec = spec.copy(zoom = it) }
            slider("Fihetsehana mitsangana", spec.x, -1f..1f) { spec = spec.copy(x = it) }
            slider("Fihetsehana mitsivalana", spec.y, -1f..1f) { spec = spec.copy(y = it) }
            if (!spec.isVideo) slider("Haizina ${(spec.dim * 100).toInt()}%", spec.dim, 0f..0.85f) { spec = spec.copy(dim = it) }
        }
    }
}
