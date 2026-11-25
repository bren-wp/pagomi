package com.brendigo.pagomi

import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.PdfDocument
import androidx.pdf.PdfPasswordException
import androidx.pdf.PdfRect
import androidx.pdf.SandboxedPdfLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import java.io.File

// The in-app PDF viewer, built on Jetpack PDF (the engine behind the Drive viewer): smooth scrolling and pinch zoom,
// text you can select and copy, search with highlights, links, a fast-scroll page indicator, page thumbnails and
// jump-to-page. The file is parsed in a sandboxed process, so a broken PDF can't take the app down with it.

@Serializable data class ViewerRoute(val path: String, val name: String)

object Viewer {
  /** Jetpack PDF needs Android 12 with the PDF system module at version 13+ (shipped through Play system updates). */
  val supported: Boolean by lazy {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13
  }

  /** Set by the navigator while the app is up: opens a PDF in the viewer. */
  var open: ((path: String, name: String) -> Unit)? = null
}

private const val MATCH = 0x55FFC94D // every search hit: a soft marker yellow
private const val CURRENT = 0x5C6459D8 // the hit you're on: a light pagomi violet (the view darkens it over text)

private class Match(val first: PdfRect, val all: List<PdfRect>)

@OptIn(ExperimentalPdfApi::class)
@Composable
fun PdfViewerScreen(path: String, name: String) {
  val nav = LocalNav.current
  val context = LocalContext.current
  val loader = remember { SandboxedPdfLoader(context) }
  var doc by remember(path) { mutableStateOf<PdfDocument?>(null) }
  var selectedPage by rememberSaveable(path) { mutableIntStateOf(0) }
  val pages = doc?.pageCount ?: 0
  val page = boundedPage(selectedPage.toLong(), pages) + 1
  val goToPage: (Int) -> Unit = { selectedPage = boundedPage(it.toLong(), pages) }
  var problem by remember { mutableStateOf("") }
  var password by remember { mutableStateOf<String?>(null) }
  var asking by remember { mutableStateOf(false) }
  var wrong by remember { mutableStateOf(false) }

  LaunchedEffect(path, password) {
    problem = ""
    try {
      doc = loader.openDocument(Uri.fromFile(File(path)), password)
      asking = false
    } catch (e: CancellationException) { throw e
    } catch (_: PdfPasswordException) {
      wrong = password != null
      asking = true
    } catch (_: Exception) {
      problem = "This PDF couldn’t be opened. It may be damaged."
    }
  }
  val latest by rememberUpdatedState(doc)
  DisposableEffect(Unit) { onDispose { runCatching { latest?.close() } } }

  // Search: every hit softly highlighted, the current one in Tangerine, stepped through with the arrows.
  var searching by remember { mutableStateOf(false) }
  var query by remember { mutableStateOf("") }
  var matches by remember { mutableStateOf<List<Match>>(emptyList()) }
  var at by remember { mutableIntStateOf(0) }
  var searched by remember { mutableStateOf(false) }
  LaunchedEffect(query, doc) {
    val d = doc ?: return@LaunchedEffect
    searched = false
    if (query.isBlank()) { matches = emptyList(); return@LaunchedEffect }
    delay(250) // typing settles first
    val found = try { d.searchDocument(query.trim(), 0 until d.pageCount) }
      catch (e: CancellationException) { throw e }
      catch (_: Exception) { null }
    val list = ArrayList<Match>()
    if (found != null) for (k in 0 until found.size()) {
      val page = found.keyAt(k)
      for (m in found.valueAt(k)) if (m.bounds.isNotEmpty()) list += Match(PdfRect(page, m.bounds[0]), m.bounds.map { PdfRect(page, it) })
    }
    matches = list.sortedWith(compareBy({ it.first.pageNum }, { it.first.top }))
    at = 0
    searched = true
  }
  LaunchedEffect(matches, at, searching, doc) {
    if (searching) matches.getOrNull(at)?.let { goToPage(it.first.pageNum) }
  }
  BackHandler(searching) { searching = false; query = "" }
  val searchFocus = remember { FocusRequester() }
  LaunchedEffect(searching) { if (searching) { delay(150); runCatching { searchFocus.requestFocus() } } } // keyboard up as search opens

  var thumbs by remember { mutableStateOf(false) }
  var jumping by remember { mutableStateOf(false) }
  var editing by remember { mutableStateOf(false) }

  Column(Modifier.fillMaxSize().background(c.bg)) {
    AnimatedContent(searching, Modifier.statusBarsPadding(), label = "viewerBar", transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) }) { s ->
      Row(Modifier.fillMaxWidth().background(c.surface).height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (s) {
          IconBtn(I.arrowLeft, label = "Close search") { searching = false; query = "" }
          Field(query, { query = it }, Modifier.weight(1f), "Search in this PDF", focus = searchFocus)
          T(
            when { query.isBlank() -> ""; !searched -> "…"; matches.isEmpty() -> "0"; else -> "${at + 1}/${matches.size}" },
            Type.num, c.sub, Modifier.padding(horizontal = 6.dp),
          )
          IconBtn(I.chevronUp, label = "Previous match", enabled = matches.isNotEmpty()) { at = (at - 1 + matches.size) % matches.size }
          IconBtn(I.chevronDown, label = "Next match", enabled = matches.isNotEmpty()) { at = (at + 1) % matches.size }
        } else {
          IconBtn(I.arrowLeft, label = "Back") { nav.back() }
          Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Marquee(name, Type.bold, c.ink)
            if (pages > 0) T("Page $page of $pages", Type.num, c.sub)
          }
          IconBtn(I.magnify, label = "Search", enabled = doc != null) { searching = true }
          IconBtn(I.shareVariantOutline, label = "Share") { share(path, name) }
          IconBtn(I.openInNew, label = "Open in another app") { openExternally(path, name) }
        }
      }
    }

    Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp).clip(RoundedCornerShape(Radius.card)).background(c.sunken)) {
      val loaded = doc
      when {
        problem.isNotEmpty() -> T(problem, Type.body, c.sub, Modifier.align(Alignment.Center).padding(32.dp))
        loaded == null -> Spinner(c.accent, modifier = Modifier.align(Alignment.Center))
        pages <= 0 -> T("This document has no pages.", Type.body, c.sub, Modifier.align(Alignment.Center))
        else -> key(loaded, page) {
          val marks = if (!searching) emptyList() else matches.flatMapIndexed { i, m ->
            m.all.filter { it.pageNum == page - 1 }.map { it to (i == at) }
          }
          FixedPdfPage(loaded, page - 1, marks, goToPage, Modifier.fillMaxSize())
        }
      }
    }
    if (pages > 0) {
      Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp).background(c.surface, RoundedCornerShape(Radius.control)),
        verticalAlignment = Alignment.CenterVertically) {
        IconBtn(I.chevronLeft, 24.dp, c.accentInk, "Previous page", enabled = page > 1) { goToPage(page - 2) }
        T("Page $page of $pages", Type.label, c.ink, Modifier.weight(1f)
          .tap(label = "Go to page") { jumping = true }.padding(vertical = 10.dp), align = androidx.compose.ui.text.style.TextAlign.Center)
        IconBtn(I.chevronRight, 24.dp, c.accentInk, "Next page", enabled = page < pages) { goToPage(page) }
        IconBtn(I.viewGridOutline, 24.dp, c.accentInk, "Page thumbnails") { thumbs = true }
      }
    }
    Column(Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
      .navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp)) {
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        T("Edit PDF", Type.bold, c.ink, Modifier.weight(1f))
        T("All tools", Type.label, c.accentInk, Modifier.tap(enabled = doc != null, label = "All editing tools") { editing = true }.padding(10.dp))
      }
      Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val actions = listOf(
          Triple("fillSign", "Text & Sign", I.drawPen),
          Triple("organize", "Pages", I.viewGridOutline),
          Triple("crop", "Crop", I.crop),
          Triple("redact", "Redact", I.eraser),
          Triple("rotate", "Rotate", I.rotateRight),
        )
        actions.forEach { (id, title, glyph) ->
          Column(Modifier.size(width = 76.dp, height = 68.dp)
            .tap(enabled = doc != null, shape = RoundedCornerShape(16.dp), label = title) { nav.tool(id, sourcePath = path, sourceName = name) }
            .background(c.accentSoft, RoundedCornerShape(16.dp)).padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(glyph, 22.dp, if (doc != null) c.accentInk else c.faint)
            Spacer(Modifier.height(4.dp))
            T(title, Type.num, if (doc != null) c.ink else c.faint, align = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2)
          }
        }
      }
    }
  }

  if (editing) AppDialog({ editing = false }, "Edit document", "Choose how you want to edit this PDF.") {
    Column(Modifier.fillMaxWidth()) {
      listOf("fillSign", "organize", "crop", "redact", "rotate", "watermark", "pageNumbers", "compress", "ocr", "password", "metadata").mapNotNull(::toolById).forEach { tool ->
        MenuRow(I.fileDocumentEditOutline, tool.title) {
          editing = false
          nav.tool(tool.id, sourcePath = path, sourceName = name)
        }
      }
    }
  }

  if (jumping) PromptDialog(
    "Go to page", "$page", "1 to $pages", "Page number", "Go", keyboard = KeyboardType.Number, onClose = { jumping = false },
  ) { typed -> typed.toLongOrNull()?.let { n -> selectedPage = boundedPage(if (n <= 1L) 0L else n - 1L, pages) } }

  if (thumbs) ModalBottomSheet({ thumbs = false }, containerColor = c.surface) {
    doc?.let { loaded -> Thumbnails(loaded, pages, page - 1) { i -> thumbs = false; goToPage(i) } }
  }

  if (asking) PasswordPrompt(name, wrong, onCancel = { nav.back() }) { password = it }
}

/** Every page small, three across, the current one ringed; tap one to go there. */
@Composable
private fun Thumbnails(document: PdfDocument, pages: Int, current: Int, onPick: (Int) -> Unit) {
  val perPage = 6
  val count = ((pages + perPage - 1) / perPage).coerceAtLeast(1)
  var group by remember(current) { mutableIntStateOf((current / perPage).coerceIn(0, count - 1)) }
  val indices = (group * perPage until minOf(pages, (group + 1) * perPage)).toList()
  var shots by remember(document, group) { mutableStateOf<Map<Int, FixedPageFrame>>(emptyMap()) }
  LaunchedEffect(document, group) {
    for (i in indices) {
      try { shots = shots + (i to fixedPageFrame(document, i, 320)) }
      catch (e: CancellationException) { throw e }
      catch (_: Exception) { /* the page remains selectable by its number */ }
    }
  }
  PagedColumn(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp)) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      IconBtn(I.chevronLeft, 24.dp, c.accentInk, "Previous thumbnails", enabled = group > 0) { group-- }
      T("Pages ${if (pages == 0) 0 else group * perPage + 1}–${minOf(pages, (group + 1) * perPage)} of $pages",
        Type.label, c.ink, Modifier.weight(1f), align = androidx.compose.ui.text.style.TextAlign.Center)
      IconBtn(I.chevronRight, 24.dp, c.accentInk, "Next thumbnails", enabled = group + 1 < count) { group++ }
    }
    indices.chunked(3).forEach { row ->
      Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        row.forEach { i ->
          val img = shots[i]?.bitmap
          Column(Modifier.weight(1f).tap(scaleTo = 0.94f, label = "Page ${i + 1}") { onPick(i) }, horizontalAlignment = Alignment.CenterHorizontally) {
            val shape = RoundedCornerShape(10.dp)
            Box(Modifier.fillMaxWidth().height(110.dp).clip(shape).background(c.page)
              .border(if (i == current) 2.dp else 1.dp, if (i == current) c.accent else c.lineStrong, shape),
              contentAlignment = Alignment.Center) {
              if (img != null) Image(img, "Page ${i + 1}", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
              else T("${i + 1}", Type.title, c.faint)
            }
            T("${i + 1}", Type.num, if (i == current) c.accent else c.sub, Modifier.padding(top = 4.dp))
          }
        }
        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
      }
    }
  }
}

@Composable
private fun PasswordPrompt(name: String, wrong: Boolean, onCancel: () -> Unit, onSubmit: (String) -> Unit) {
  var typed by remember { mutableStateOf("") }
  AppDialog(onCancel, "Password-protected PDF", "Enter the password for “${stem(name)}” to open it.") {
    Field(typed, { typed = it }, Modifier.padding(top = 14.dp), "Password", secret = true, error = wrong, onDone = { if (typed.isNotEmpty()) onSubmit(typed) })
    if (wrong) T("That password didn’t open it. Try again.", Type.small, c.accent, Modifier.padding(top = 6.dp))
    Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Btn("Cancel", Modifier.weight(1f), kind = BtnKind.Secondary, onClick = onCancel)
      Btn("Open", Modifier.weight(1f), kind = BtnKind.Accent, enabled = typed.isNotEmpty()) { onSubmit(typed) }
    }
  }
}
