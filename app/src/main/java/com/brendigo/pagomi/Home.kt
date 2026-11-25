package com.brendigo.pagomi

import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

// Tokenized search: every word in the query must appear somewhere in the target. "split text" finds "Split PDF by Text".
private fun matches(target: String, tokens: List<String>) = target.lowercase().let { t -> tokens.all { it in t } }

@Composable
fun ToolsHub() {
  val nav = LocalNav.current
  var q by rememberSaveable { mutableStateOf("") }
  var section by rememberSaveable { mutableStateOf(-1) }
  var categoryOpen by remember { mutableStateOf(false) }
  var showFiles by rememberSaveable { mutableStateOf(false) }
  val files = remember(Store.version) { listAllFiles() }
  val tokens = q.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
  val searching = tokens.isNotEmpty()
  val featured = listOf("merge", "split", "fillSign", "ocr", "compress", "rotate", "watermark", "password", "crop", "metadata")
  val pinned = Pins.ids.mapNotNull(::toolById)
  val ordered = (pinned + featured.mapNotNull(::toolById) + TOOLS).distinctBy { it.id }
  val tools = if (searching) ordered.filter { matches("${it.title} ${it.desc}", tokens) } else when (section) {
    -2 -> pinned
    -1 -> ordered.filter { it.id != "scan" }
    else -> ordered.filter { it.section == section }
  }
  val found = if (searching) files.filter { matches(it.name, tokens) } else emptyList()
  val filesSelected = searching && found.isNotEmpty() && (showFiles || tools.isEmpty())
  BoxWithConstraints(Modifier.fillMaxSize().background(c.bg).statusBarsPadding().imePadding().padding(horizontal = 14.dp, vertical = 2.dp)) {
    val compact = maxHeight < 470.dp
    Column(Modifier.fillMaxSize()) {
      if (maxHeight >= 280.dp) PagomiHeader()
      LargeTitle("Tools", if (compact) null else "Powerful PDF tools to edit, organize, and work smarter.")
      SearchField(q, { q = it; showFiles = false }, "Search tools or files…", Modifier.testTag("tools-search"))
      if (!searching) {
        if (!compact) {
          Row(Modifier.padding(top = 10.dp).fillMaxWidth().background(Brush.linearGradient(c.hero), RoundedCornerShape(Radius.hero)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
              T("Powerful PDF tools", Type.hero, Color.White)
              T("Merge, split, sign and organize.", Type.small, Color.White, Modifier.padding(top = 4.dp))
            }
            ToolArt("merge", 44.dp)
          }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          Chip("All tools", section == -1) { section = -1 }
          Chip("Pinned", section == -2) { section = -2 }
          Row(Modifier.weight(1f).tap(label = "Categories") { categoryOpen = true }.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            T(if (section >= 0) SECTIONS[section] else "Categories", Type.small, c.accentInk, Modifier.weight(1f), maxLines = 2)
            Icon(I.chevronRight, 16.dp, c.accentInk)
          }
        }
      } else if (found.isNotEmpty() && tools.isNotEmpty()) {
        Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Chip("Tools (${tools.size})", !filesSelected) { showFiles = false }
          Chip("Files (${found.size})", filesSelected) { showFiles = true }
        }
      }
      BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
        val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
        val rowTarget = if (filesSelected) 68.dp * scale else 88.dp * scale
        val total = if (filesSelected) found.size else tools.size
        val columns = if (filesSelected || maxWidth < 300.dp || scale > 1.35f) 1 else 2
        val fullRows = (maxHeight / rowTarget).toInt().coerceIn(1, 5)
        val needsPager = total > fullRows * columns
        val available = (maxHeight - if (needsPager) 52.dp else 0.dp).coerceAtLeast(1.dp)
        val rows = (available / rowTarget).toInt().coerceIn(1, 5)
        val capacity = rows * columns
        val count = ((total + capacity - 1) / capacity).coerceAtLeast(1)
        var current by rememberSaveable(q, section, filesSelected, capacity) { mutableStateOf(0) }
        val page = current.coerceIn(0, count - 1)
        val tileHeight = ((available - (rows - 1) * 8.dp) / rows).coerceAtLeast(1.dp)
        Column(Modifier.fillMaxSize()) {
          if (total == 0) {
            T(if (searching) "No matching files or tools." else "Pin your favourite tools to find them here.",
              Type.body, c.sub, Modifier.fillMaxWidth().padding(16.dp), align = TextAlign.Center)
          } else if (filesSelected) {
            DashboardFiles(found.drop(page * capacity).take(capacity), tileHeight)
          } else {
            ToolGrid(tools.drop(page * capacity).take(capacity), shared = false, columns = columns,
              fixedHeight = tileHeight, gap = 8.dp) { nav.tool(it.id) }
          }
          Spacer(Modifier.weight(1f))
          SectionPager(page, count) { current = it }
        }
      }
    }
  }
  if (categoryOpen) AppDialog({ categoryOpen = false }, "Tool categories") {
    SECTIONS.forEachIndexed { index, label ->
      MenuRow(I.viewGridOutline, label) { section = index; categoryOpen = false }
    }
  }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
  in 5..11 -> "Good morning!"
  in 12..16 -> "Good afternoon!"
  else -> "Good evening!"
}

// Two cards a row, each the height of the taller one, so titles and descriptions line up across the grid.
@Composable
private fun ToolGrid(items: List<Tool>, shared: Boolean = true, columns: Int = 2, fixedHeight: Dp? = null, gap: Dp = 10.dp, onOpen: (Tool) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(gap)) {
    items.chunked(columns).forEach { pair ->
      Row(Modifier.fillMaxWidth().then(if (fixedHeight != null) Modifier.height(fixedHeight) else Modifier.height(IntrinsicSize.Min)), horizontalArrangement = Arrangement.spacedBy(gap)) {
        pair.forEach { t -> ToolCard(t, Modifier.weight(1f), shared, fixedHeight != null && fixedHeight < 80.dp) { onOpen(t) } }
        repeat(columns - pair.size) { Spacer(Modifier.weight(1f)) }
      }
    }
  }
}

@Composable
private fun ToolCard(t: Tool, modifier: Modifier, shared: Boolean, compact: Boolean, onClick: () -> Unit) {
  val shape = RoundedCornerShape(Radius.group)
  val pinned = t.id in Pins.ids
  Box(
    modifier.fillMaxHeight().testTag("tool-${t.id}").tap(shape = shape, scaleTo = 0.96f, label = t.title, onClick = onClick)
      .background(c.surface, shape).border(0.6.dp, c.lineStrong, shape),
  ) {
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 26.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
      ArtTile(t.id, 30.dp, if (shared) Modifier.sharedArt(t.id) else Modifier)
      Spacer(Modifier.size(8.dp))
      Column(Modifier.weight(1f)) {
        T(t.title, Type.bold.copy(fontSize = 12.sp, lineHeight = 16.sp), maxLines = 3)
        if (!compact) T(t.desc, Type.small.copy(fontSize = 11.sp, lineHeight = 14.sp), c.sub, Modifier.padding(top = 2.dp), maxLines = 2)
      }
    }
    // Always there, so pinning is found without a hidden long-press: faint outline off, solid Tangerine on.
    Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
      IconBtn(if (pinned) I.pin else I.pinOutline, 18.dp, if (pinned) c.accent else c.faint, if (pinned) "Unpin ${t.title}" else "Pin ${t.title} to the top") { Pins.toggle(t.id) }
    }
  }
}

/** Native home dashboard: the document library and every action use live data. */
@Composable
fun Home() {
  val nav = LocalNav.current
  val scope = rememberCoroutineScope()
  val files = remember(Store.version) { listAllFiles() }
  var query by rememberSaveable { mutableStateOf("") }
  val tokens = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
  val actions = listOf("merge" to "Merge", "fillSign" to "Sign", "ocr" to "OCR", "compress" to "Compress", "split" to "Split")
  Screen {
    PagomiHeader()
    LargeTitle(greeting(), SimpleDateFormat("'Today is' EEE, MMM d, yyyy", LocalLocale.current.platformLocale).format(Date()))
    SearchField(query, { query = it }, "Search files, tools, or documents…")
    if (tokens.isEmpty()) {
      val shape = RoundedCornerShape(26.dp)
      Row(Modifier.padding(top = 12.dp).fillMaxWidth().tap(shape = shape, label = "Scan Document") { nav.scan() }
        .background(Brush.linearGradient(listOf(Color(0xFF8175EE), Brand.primary, Color(0xFF403978))), shape).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(I.lineScan, 27.dp, Color.White)
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
          T("Scan Document", Type.hero.copy(fontSize = 17.sp, lineHeight = 23.sp), Color.White)
          T("Turn paper and photos into clean, high-quality PDFs.", Type.small, Color.White)
        }
        ToolArt("scan", 42.dp)
        Icon(I.arrowRight, 18.dp, Color.White)
      }
      val importShape = RoundedCornerShape(Radius.card)
      Row(Modifier.padding(top = 8.dp).fillMaxWidth().tap(shape = importShape, label = "Import PDF") {
        scope.launch { pickPdfs(false).firstOrNull()?.let { openFile(it.path, it.name) } }
      }.background(c.surface, importShape).border(1.dp, c.line, importShape).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(I.filePdfBox, 28.dp, c.accent)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
          T("Import PDF", Type.item)
          T("Add a PDF from your device", Type.small, c.sub)
        }
        Icon(I.chevronRight, 20.dp, c.sub)
      }
      Row(Modifier.padding(top = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        actions.forEach { (id, label) ->
          val tile = RoundedCornerShape(18.dp)
          Column(Modifier.weight(1f).heightIn(min = 56.dp).tap(shape = tile, label = label) { nav.tool(id) }
            .background(if (id == "ocr" && !c.dark) Brand.mint else c.accentSoft, tile).padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            ToolArt(id, 24.dp)
            T(label, Type.num.copy(fontSize = 10.sp, lineHeight = 13.sp), c.ink, maxLines = 2, align = TextAlign.Center)
          }
        }
      }
      GroupLabel("Recent files", "See All", { nav.show(Tab.Files) }, caps = false)
      if (files.isEmpty()) T("Your scanned and imported documents will appear here.", Type.body, c.sub, Modifier.padding(vertical = 16.dp))
      else DashboardFiles(files.take(3))
    } else {
      val found = files.filter { matches(it.name, tokens) }
      val tools = TOOLS.filter { matches("${it.title} ${it.desc}", tokens) }
      if (found.isNotEmpty()) {
        GroupLabel("Files", caps = false)
        DashboardFiles(found)
      }
      if (tools.isNotEmpty()) {
        GroupLabel("Tools", caps = false)
        ToolGrid(tools) { nav.tool(it.id) }
      }
      if (found.isEmpty() && tools.isEmpty()) T("No matching files or tools.", Type.body, c.sub, Modifier.padding(top = 24.dp))
    }
  }
}

@Composable
private fun DashboardFiles(files: List<Saved>, fixedHeight: Dp? = null) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    files.forEach { f ->
      val shape = RoundedCornerShape(Radius.group)
      Row(Modifier.fillMaxWidth().then(if (fixedHeight != null) Modifier.height(fixedHeight) else Modifier).testTag("document-${f.name}").tap(shape = shape, label = "Open ${f.name}") { openFile(f.path, f.name) }
        .background(c.surface, shape).border(1.dp, c.line, shape).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).background(c.accentSoft, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { DocGlyph(kindOf(f.name)) }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
          Marquee(f.name, Type.item, c.ink)
          T("${fmtSize(f.size)} · ${fmtDate(f.modified)}", Type.small, c.sub, maxLines = 1)
        }
        IconBtn(I.shareVariantOutline, 20.dp, c.accent, "Share") { share(f.path, f.name) }
      }
    }
  }
}
