package mg.sitraka.fihirana.windows

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

data class Song(val lang: String, val number: Int, val title: String, val author: String, val key: String, val theme: Int, val sub: Int)
data class SongPart(val index: Int, val kind: String, val text: String) {
    val isRefrain: Boolean get() = kind.startsWith("r")

    /** Refrain réservé à certains couplets : kind = "r:1-3,5". Sans portée : "r". */
    val scope: Set<Int>? get() = parseScope(kind.substringAfter(':', ""))
    val scopeText: String get() = kind.substringAfter(':', "")

    companion object {
        fun parseScope(text: String): Set<Int>? {
            val out = mutableSetOf<Int>()
            text.split(',', ' ').map { it.trim() }.filter { it.isNotEmpty() }.forEach { token ->
                val range = token.split('-', '–')
                val a = range.getOrNull(0)?.toIntOrNull()
                val b = range.getOrNull(1)?.toIntOrNull() ?: a
                if (a != null && b != null && b >= a && b - a < 200) for (n in a..b) out.add(n)
            }
            return out.ifEmpty { null }
        }
    }
}
data class HymnLanguage(val code: String, val name: String, val short: String)
data class HymnTheme(val id: Int, val name: String, val parent: String?)
data class BibleVersion(val id: Int, val code: String, val name: String, val short: String, val lang: String, val rtl: Boolean)
data class BibleBook(val id: Int, val abbr: String, val color: String, val chapters: Int, val order: Int, val name: String)
data class BibleVerse(val book: Int, val chapter: Int, val verse: Int, val text: String, val heading: String?, val note: String?)

object Database {
    lateinit var content: Connection
        private set
    lateinit var user: Connection
        private set

    private fun appDir(): File = File(System.getProperty("user.home"), ".fihirana-baiboly").apply { mkdirs() }

    fun open() {
        Class.forName("org.sqlite.JDBC")
        val contentFile = File(appDir(), "content.db")
        if (!contentFile.exists() || contentFile.length() < 10_000_000L) {
            val temp = File(appDir(), "content.tmp")
            val stream = javaClass.getResourceAsStream("/fihirana_baiboly.db")
                ?: error("Tsy hita ny base de données. Avereno amin'ny Internet ny build voalohany.")
            stream.use { input -> temp.outputStream().use { input.copyTo(it) } }
            if (contentFile.exists()) contentFile.delete()
            check(temp.renameTo(contentFile)) { "Tsy afaka nametraka ny base de données." }
        }
        content = DriverManager.getConnection("jdbc:sqlite:${contentFile.absolutePath}")
        content.createStatement().use { it.execute("PRAGMA query_only=ON") }

        user = DriverManager.getConnection("jdbc:sqlite:${File(appDir(), "user.db").absolutePath}")
        user.createStatement().use { s ->
            s.executeUpdate("CREATE TABLE IF NOT EXISTS song_edits(lang TEXT,num INTEGER,title TEXT,author TEXT,PRIMARY KEY(lang,num))")
            s.executeUpdate("CREATE TABLE IF NOT EXISTS song_part_edits(lang TEXT,num INTEGER,idx INTEGER,kind TEXT,text TEXT,PRIMARY KEY(lang,num,idx))")
            s.executeUpdate("CREATE TABLE IF NOT EXISTS categories(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT UNIQUE)")
            s.executeUpdate("CREATE TABLE IF NOT EXISTS custom_songs(id INTEGER PRIMARY KEY AUTOINCREMENT,category TEXT,number TEXT,title TEXT,lyrics TEXT)")
            s.executeUpdate("CREATE TABLE IF NOT EXISTS favorites(kind TEXT,item_key TEXT,PRIMARY KEY(kind,item_key))")
            s.executeUpdate("CREATE TABLE IF NOT EXISTS verse_marks(item_key TEXT PRIMARY KEY,color TEXT,note TEXT)")
            s.executeUpdate("CREATE TABLE IF NOT EXISTS history(id INTEGER PRIMARY KEY AUTOINCREMENT,kind TEXT,title TEXT,body TEXT,created INTEGER)")
            s.executeUpdate("CREATE TABLE IF NOT EXISTS temp_slides(id INTEGER PRIMARY KEY AUTOINCREMENT,group_id TEXT,page INTEGER,title TEXT,subtitle TEXT,body TEXT)")
            s.executeUpdate("CREATE TABLE IF NOT EXISTS settings(key TEXT PRIMARY KEY,value TEXT)")
        }
    }

    fun <T> query(sql: String, args: List<Any?> = emptyList(), map: (ResultSet) -> T): List<T> {
        content.prepareStatement(sql).use { ps ->
            args.forEachIndexed { i, value -> ps.setObject(i + 1, value) }
            ps.executeQuery().use { rs ->
                val rows = ArrayList<T>()
                while (rs.next()) rows.add(map(rs))
                return rows
            }
        }
    }

    fun save(sql: String, args: List<Any?> = emptyList()) {
        user.prepareStatement(sql).use { ps ->
            args.forEachIndexed { i, value -> ps.setObject(i + 1, value) }
            ps.executeUpdate()
        }
    }

    fun setting(key: String, fallback: String = ""): String =
        user.prepareStatement("SELECT value FROM settings WHERE key=?").use { p ->
            p.setString(1, key)
            p.executeQuery().use { r -> if (r.next()) r.getString(1) else fallback }
        }

    fun setSetting(key: String, value: String) =
        save("INSERT OR REPLACE INTO settings(key,value) VALUES(?,?)", listOf(key, value))

    fun languages(): List<HymnLanguage> =
        query("SELECT code,name,short FROM hymn_langs ORDER BY ord") {
            HymnLanguage(it.getString(1), it.getString(2), it.getString(3))
        }

    fun songs(lang: String, term: String = ""): List<Song> {
        val sql = if (term.isBlank()) {
            "SELECT lang,num,title,author,key,theme,sub FROM hymns WHERE lang=? ORDER BY num"
        } else {
            "SELECT lang,num,title,author,key,theme,sub FROM hymns WHERE lang=? AND (title LIKE ? OR CAST(num AS TEXT) LIKE ?) ORDER BY num LIMIT 300"
        }
        val args = if (term.isBlank()) listOf(lang) else listOf(lang, "%$term%", "%$term%")
        val edits = HashMap<Int, Pair<String?, String?>>()
        user.prepareStatement("SELECT num,title,author FROM song_edits WHERE lang=?").use { p ->
            p.setString(1, lang)
            p.executeQuery().use { e -> while (e.next()) edits[e.getInt(1)] = e.getString(2) to e.getString(3) }
        }
        return query(sql, args) { r ->
            val original = Song(r.getString(1), r.getInt(2), r.getString(3), r.getString(4) ?: "",
                r.getString(5) ?: "", r.getInt(6), r.getInt(7))
            val e = edits[original.number]
            if (e != null) original.copy(title = e.first ?: original.title, author = e.second ?: original.author)
            else original
        }
    }

    fun song(lang: String, number: Int): Song? = songs(lang).firstOrNull { it.number == number }

    fun songParts(lang: String, number: Int): List<SongPart> {
        val changed = user.prepareStatement(
            "SELECT idx,kind,text FROM song_part_edits WHERE lang=? AND num=? ORDER BY idx"
        ).use { p ->
            p.setString(1, lang); p.setInt(2, number)
            p.executeQuery().use { r ->
                val out = mutableListOf<SongPart>()
                while (r.next()) out.add(SongPart(r.getInt(1), r.getString(2), r.getString(3)))
                out
            }
        }
        if (changed.isNotEmpty()) return changed
        return query("SELECT idx,kind,text FROM stanzas WHERE lang=? AND num=? ORDER BY idx", listOf(lang, number)) {
            SongPart(it.getInt(1), it.getString(2), it.getString(3))
        }
    }

    fun hymnThemes(lang: String): List<HymnTheme> =
        query("SELECT id,name,parent FROM hymn_themes WHERE lang=? ORDER BY id", listOf(lang)) {
            HymnTheme(it.getInt(1), it.getString(2), it.getString(3))
        }

    fun versions(): List<BibleVersion> =
        query("SELECT id,code,name,short,lang,rtl FROM bible_tr ORDER BY ord") {
            BibleVersion(it.getInt(1), it.getString(2), it.getString(3), it.getString(4),
                it.getString(5), it.getInt(6) == 1)
        }

    fun books(version: Int): List<BibleBook> {
        val names = query("SELECT book,name FROM bible_book_names WHERE tr=?", listOf(version)) {
            it.getInt(1) to it.getString(2)
        }.toMap()
        return query("SELECT book,abbr,color,chapters,ord FROM bible_books ORDER BY ord") {
            val id = it.getInt(1)
            BibleBook(id, it.getString(2), it.getString(3), it.getInt(4), it.getInt(5),
                names[id] ?: it.getString(2))
        }
    }

    fun chapter(version: Int, book: Int, chapter: Int): List<BibleVerse> =
        query(
            "SELECT book,chapter,verse,text,heading,note FROM bible_verses WHERE tr=? AND book=? AND chapter=? ORDER BY verse",
            listOf(version, book, chapter)
        ) {
            BibleVerse(it.getInt(1), it.getInt(2), it.getInt(3), it.getString(4),
                it.getString(5), it.getString(6))
        }

    fun verse(version: Int, book: Int, chapter: Int, verse: Int): BibleVerse? =
        query(
            "SELECT book,chapter,verse,text,heading,note FROM bible_verses WHERE tr=? AND book=? AND chapter=? AND verse=?",
            listOf(version, book, chapter, verse)
        ) {
            BibleVerse(it.getInt(1), it.getInt(2), it.getInt(3), it.getString(4),
                it.getString(5), it.getString(6))
        }.firstOrNull()

    fun searchBible(version: Int, term: String, limit: Int = 300): List<BibleVerse> {
        if (term.isBlank()) return emptyList()
        return query(
            "SELECT book,chapter,verse,text,heading,note FROM bible_verses WHERE tr=? AND text LIKE ? ORDER BY book,chapter,verse LIMIT ?",
            listOf(version, "%$term%", limit)
        ) {
            BibleVerse(it.getInt(1), it.getInt(2), it.getInt(3), it.getString(4),
                it.getString(5), it.getString(6))
        }
    }

    fun favorite(kind: String, key: String): Boolean =
        user.prepareStatement("SELECT 1 FROM favorites WHERE kind=? AND item_key=?").use { p ->
            p.setString(1, kind); p.setString(2, key)
            p.executeQuery().use { it.next() }
        }

    fun favorites(kind: String): Set<String> =
        user.prepareStatement("SELECT item_key FROM favorites WHERE kind=?").use { p ->
            p.setString(1, kind)
            p.executeQuery().use { r ->
                val out = HashSet<String>()
                while (r.next()) out.add(r.getString(1))
                out
            }
        }

    fun toggleFavorite(kind: String, key: String) {
        if (favorite(kind, key)) save("DELETE FROM favorites WHERE kind=? AND item_key=?", listOf(kind, key))
        else save("INSERT OR IGNORE INTO favorites(kind,item_key) VALUES(?,?)", listOf(kind, key))
    }
}
