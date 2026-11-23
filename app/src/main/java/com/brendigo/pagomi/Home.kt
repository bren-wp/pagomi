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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

// Tokenized search: every word in the query must appear somewhere in the target. "split text" finds "Split PDF by Text".
private fun matches(target: String, tokens: List<String>) = target.lowercase().let { t -> tokens.all { it in t } }

@Composable
fun ToolsHub() {
  val nav = LocalNav.current
  var q by rememberSaveable { mutableStateOf("") }
  val files = remember(Store.version) { listAllFiles() }
  val tokens = q.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
  val searching = tokens.isNotEmpty()
  val tools = if (searching) TOOLS.filter { matches("${it.title} ${it.desc}", tokens) } else TOOLS
  val found = if (searching) files.filter { matches(it.name, tokens) } else emptyList()
  // Sections with something to show besides the camera (the hero card already opens it).
  val sections = remember { SECTIONS.indices.filter { si -> TOOLS.any { it.section == si && it.id != "scan" } } }
  var section by rememberSaveable { mutableStateOf(-1) } // -1: every section

  Screen {
    PagomiHeader()
    LargeTitle("Tools", "Powerful PDF tools to edit, organize, and work smarter.")
    SearchField(q, { q = it }, "Search tools or files…")

    if (!searching) {
      Column(Modifier.padding(top = 16.dp).fillMaxWidth().background(Brush.linearGradient(c.hero), RoundedCornerShape(Radius.hero)).padding(18.dp)) {
        T("Powerful PDF tools", Type.hero, Color.White)
        T("Merge, split, sign and organize documents on your device.", Type.small, Color.White, Modifier.padding(top = 6.dp))
      }
      Row(
        Modifier.padding(top = 22.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Chip("All tools", section == -1) { section = -1 }
        sections.forEach { si -> Chip(SECTIONS[si], section == si) { section = si } }
      }
    }

    if (found.isNotEmpty()) {
      GroupLabel("Files", "All files", { nav.show(Tab.Files) })
      Group {
        found.take(5).forEachIndexed { i, f ->
          ListRow(
            stem(f.name), first = i == 0, lead = { DocGlyph(kindOf(f.name)) }, marquee = true,
            subtitle = "${fmtSize(f.size)} · ${fmtDate(f.modified)}", onClick = { openFile(f.path, f.name) },
          ) { IconBtn(I.shareVariantOutline, 18.dp, c.sub, "Share") { share(f.path, f.name) } }
        }
      }
    }

    // Pinned tools first, in the order they were pinned (hold any tool, or use the pin on its screen).
    val pinned = Pins.ids.mapNotNull { toolById(it) }
    if (!searching && section == -1 && pinned.isNotEmpty()) {
      Stagger(2) {
        Column {
          GroupLabel("Pinned · ${pinned.size}")
          ToolGrid(pinned, shared = false) { nav.tool(it.id) } // the section below owns the flying art
        }
      }
    }

    SECTIONS.forEachIndexed { si, name ->
      if (!searching && section != -1 && section != si) return@forEachIndexed
      val items = tools.filter { it.section == si && (searching || it.id != "scan") }
      if (items.isEmpty()) return@forEachIndexed
      Stagger(si + 3) {
        Column {
          GroupLabel("$name · ${items.size}")
          ToolGrid(items) { nav.tool(it.id) }
        }
      }
    }

    if (tools.isEmpty() && found.isEmpty()) {
      T("No tools or files match \"${q.trim()}\". Try \"merge\", \"split\" or \"compress\".", Type.body, c.sub, Modifier.fillMaxWidth().padding(top = 40.dp, start = 24.dp, end = 24.dp), align = TextAlign.Center)
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
private fun ToolGrid(items: List<Tool>, shared: Boolean = true, onOpen: (Tool) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    items.chunked(2).forEach { pair ->
      Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        pair.forEach { t -> ToolCard(t, Modifier.weight(1f), shared) { onOpen(t) } }
        if (pair.size == 1) Spacer(Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun ToolCard(t: Tool, modifier: Modifier, shared: Boolean, onClick: () -> Unit) {
  val shape = RoundedCornerShape(Radius.group)
  val pinned = t.id in Pins.ids
  Box(
    modifier.fillMaxHeight().tap(shape = shape, scaleTo = 0.96f, label = t.title, onClick = onClick)
      .background(c.surface, shape).border(0.6.dp, c.lineStrong, shape),
  ) {
    Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 26.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
      ArtTile(t.id, 36.dp, if (shared) Modifier.sharedArt(t.id) else Modifier)
      Spacer(Modifier.size(8.dp))
      Column(Modifier.weight(1f)) {
        T(t.title, Type.bold, maxLines = 3)
        T(t.desc, Type.small.copy(fontSize = 11.sp, lineHeight = 15.sp), c.sub, Modifier.padding(top = 4.dp), maxLines = 3)
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
      Row(Modifier.padding(top = 18.dp).fillMaxWidth().tap(shape = shape, label = "Scan Document") { nav.scan() }
        .background(Brush.linearGradient(listOf(Color(0xFF8175EE), Brand.primary, Color(0xFF403978))), shape).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(I.lineScan, 32.dp, Color.White)
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
          T("Scan Document", Type.hero, Color.White)
          T("Turn paper and photos into clean, high-quality PDFs.", Type.small, Color.White)
        }
        Icon(I.arrowRight, 22.dp, Color.White)
      }
      val importShape = RoundedCornerShape(Radius.card)
      Row(Modifier.padding(top = 12.dp).fillMaxWidth().tap(shape = importShape, label = "Import PDF") {
        scope.launch { pickPdfs(false).firstOrNull()?.let { openFile(it.path, it.name) } }
      }.background(c.surface, importShape).border(1.dp, c.line, importShape).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(I.filePdfBox, 28.dp, c.accent)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
          T("Import PDF", Type.item)
          T("Add a PDF from your device", Type.small, c.sub)
        }
        Icon(I.chevronRight, 20.dp, c.sub)
      }
      Row(Modifier.padding(top = 18.dp).fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.forEach { (id, label) ->
          val tile = RoundedCornerShape(18.dp)
          Column(Modifier.size(width = 65.dp, height = 76.dp).tap(shape = tile, label = label) { nav.tool(id) }
            .background(if (id == "ocr" && !c.dark) Brand.mint else c.accentSoft, tile).padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            ToolArt(id, 28.dp)
            T(label, Type.num, c.ink, maxLines = 1)
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
private fun DashboardFiles(files: List<Saved>) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    files.forEach { f ->
      val shape = RoundedCornerShape(Radius.group)
      Row(Modifier.fillMaxWidth().tap(shape = shape, label = "Open ${f.name}") { openFile(f.path, f.name) }
        .background(c.surface, shape).border(1.dp, c.line, shape).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        DocGlyph(kindOf(f.name))
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
          Marquee(f.name, Type.item, c.ink)
          T("${fmtSize(f.size)} · ${fmtDate(f.modified)}", Type.small, c.sub, maxLines = 1)
        }
        IconBtn(I.shareVariantOutline, 20.dp, c.accent, "Share") { share(f.path, f.name) }
      }
    }
  }
}
