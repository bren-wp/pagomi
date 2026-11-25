package com.brendigo.pagomi

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.util.Size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as DrawSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.PdfDocument
import androidx.pdf.PdfRect
import androidx.pdf.content.PageSelection
import androidx.pdf.content.PdfPageTextContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal data class FixedPageFrame(val bitmap: ImageBitmap, val width: Int, val height: Int)

/** Render only the requested page through the open sandbox session; no temporary decrypted PDF or raster file. */
@OptIn(ExperimentalPdfApi::class)
internal suspend fun fixedPageFrame(document: PdfDocument, page: Int, maxSide: Int = 2400): FixedPageFrame {
  require(page in 0 until document.pageCount)
  val info = document.getPageInfo(page)
  require(info.width > 0 && info.height > 0)
  val scale = maxSide.toFloat() / maxOf(info.width, info.height)
  val dimensions = Size((info.width * scale).roundToInt().coerceAtLeast(1),
    (info.height * scale).roundToInt().coerceAtLeast(1))
  val bitmap = document.getPageBitmapSource(page).use { it.getBitmap(dimensions, null) }
  return FixedPageFrame(bitmap.asImageBitmap(), info.width, info.height)
}

@OptIn(ExperimentalPdfApi::class)
@Composable
internal fun FixedPdfPage(
  document: PdfDocument,
  page: Int,
  marks: List<Pair<PdfRect, Boolean>>,
  onPage: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var frame by remember(document, page) { mutableStateOf<FixedPageFrame?>(null) }
  var problem by remember(document, page) { mutableStateOf("") }
  var zoom by remember(document, page) { mutableStateOf(1f) }
  var pan by remember(document, page) { mutableStateOf(Offset.Zero) }
  var selecting by remember(document, page) { mutableStateOf(false) }
  var selection by remember(document, page) { mutableStateOf<PageSelection?>(null) }
  var pending by remember(document, page) { mutableStateOf<Job?>(null) }
  var dragStart by remember(document, page) { mutableStateOf<Offset?>(null) }
  var dragEnd by remember(document, page) { mutableStateOf<Offset?>(null) }
  var viewportSize by remember(document, page) { mutableStateOf(IntSize.Zero) }

  LaunchedEffect(document, page) {
    try { frame = fixedPageFrame(document, page) }
    catch (e: CancellationException) { throw e }
    catch (_: Exception) { problem = "This page couldn’t be displayed." }
  }

  fun viewport(w: Int, h: Int, data: FixedPageFrame) =
    pageViewport(w.toFloat(), h.toFloat(), data.width, data.height, zoom, pan.x, pan.y)
  fun clampPan(data: FixedPageFrame, w: Int, h: Int) {
    val v = viewport(w, h, data)
    pan = Offset(v.left - (w - v.width) / 2f, v.top - (h - v.height) / 2f)
  }
  fun point(at: Offset, w: Int, h: Int, data: FixedPageFrame): PointF {
    val v = viewport(w, h, data)
    return PointF(v.pdfX(at.x, data.width), v.pdfY(at.y, data.height))
  }
  fun requestSelection(start: PointF, stop: PointF) {
    pending?.cancel()
    selection = null
    pending = scope.launch {
      try { selection = document.getSelectionBounds(page, start, stop) }
      catch (e: CancellationException) { throw e }
      catch (_: Exception) { problem = "Couldn’t select text on this page." }
    }
  }
  val text = selection?.selectedContents?.filterIsInstance<PdfPageTextContent>()
    ?.joinToString(" ") { it.text }.orEmpty()

  Column(modifier.fillMaxSize()) {
    Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFFF2EFF9)).clipToBounds()) {
      val data = frame
      if (data == null) {
        if (problem.isEmpty()) Spinner(c.accent, modifier = Modifier.align(Alignment.Center))
        else T(problem, Type.body, c.sub, Modifier.align(Alignment.Center).padding(16.dp))
      } else {
        Canvas(Modifier.fillMaxSize().testTag("pdf-page-${page + 1}")
          .semantics { contentDescription = "Page ${page + 1}" }
          .pointerInput(document, page, data, selecting) {
            detectTapGestures(onTap = { at ->
              val v = viewport(size.width, size.height, data)
              if (v.contains(at.x, at.y)) {
                val p = point(at, size.width, size.height, data)
                if (selecting) requestSelection(p, p)
                else scope.launch {
                  try {
                    val links = document.getPageLinks(page)
                    val internal = links.gotoLinks.firstOrNull { link -> link.bounds.any { it.contains(p.x, p.y) } }
                    if (internal != null) onPage(internal.destination.pageNumber)
                    else links.externalLinks.firstOrNull { link -> link.bounds.any { it.contains(p.x, p.y) } }
                      ?.uri?.takeIf { it.scheme?.lowercase() in listOf("https", "http", "mailto", "tel") }
                      ?.let { openUrl(it.toString()) }
                  } catch (e: CancellationException) { throw e }
                  catch (_: Exception) { problem = "Couldn’t open this link." }
                }
              }
            })
          }
          .pointerInput(document, page, data, selecting) {
            if (selecting) detectDragGestures(
              onDragStart = { at -> pending?.cancel(); selection = null; dragStart = at; dragEnd = at },
              onDragCancel = { dragStart = null; dragEnd = null },
              onDragEnd = {
                val a = dragStart; val b = dragEnd
                dragStart = null; dragEnd = null
                if (a != null && b != null) requestSelection(point(a, size.width, size.height, data), point(b, size.width, size.height, data))
              },
            ) { change, amount -> change.consume(); dragEnd = (dragEnd ?: change.position) + amount }
            else detectTransformGestures { _, amount, scale, _ ->
              zoom = (zoom * scale).coerceIn(1f, 4f)
              pan += amount
              clampPan(data, size.width, size.height)
            }
          }) {
          val v = viewport(size.width.toInt(), size.height.toInt(), data)
          drawImage(data.bitmap, dstOffset = IntOffset(v.left.roundToInt(), v.top.roundToInt()),
            dstSize = IntSize(v.width.roundToInt().coerceAtLeast(1), v.height.roundToInt().coerceAtLeast(1)))
          fun mark(left: Float, top: Float, right: Float, bottom: Float, color: Color) {
            val x = v.left + minOf(left, right) / data.width * v.width
            val y = v.top + minOf(top, bottom) / data.height * v.height
            drawRect(color, Offset(x, y), DrawSize(kotlin.math.abs(right - left) / data.width * v.width,
              kotlin.math.abs(bottom - top) / data.height * v.height))
          }
          marks.forEach { (rect, active) -> mark(rect.left, rect.top, rect.right, rect.bottom,
            if (active) Color(0x776459D8) else Color(0x66FFC94D)) }
          selection?.selectedContents?.forEach { item ->
            item.bounds.forEach { rect -> mark(rect.left, rect.top, rect.right, rect.bottom, Color(0x886459D8)) }
          }
          val a = dragStart; val b = dragEnd
          if (a != null && b != null) drawRect(Color(0x336459D8), Offset(minOf(a.x, b.x), minOf(a.y, b.y)),
            DrawSize(kotlin.math.abs(a.x - b.x), kotlin.math.abs(a.y - b.y)))
        }
      }
    }
    if (frame != null) {
      Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        IconBtn(I.formatFontSizeDecrease, 20.dp, c.accentInk, "Zoom out", enabled = zoom > 1f) {
          zoom = (zoom / 1.25f).coerceAtLeast(1f); pan = Offset.Zero
        }
        T("${(zoom * 100).roundToInt()}%", Type.num, c.sub, Modifier.tap(label = "Fit page") { zoom = 1f; pan = Offset.Zero }.padding(4.dp))
        IconBtn(I.formatFontSizeIncrease, 20.dp, c.accentInk, "Zoom in", enabled = zoom < 4f) {
          zoom = (zoom * 1.25f).coerceAtMost(4f); pan = Offset.Zero
        }
        T(if (selecting) "Done" else "Select text", Type.label, c.accentInk,
          Modifier.weight(1f).tap(label = "Select text") {
            selecting = !selecting
            if (!selecting) { pending?.cancel(); selection = null }
          }.padding(vertical = 10.dp), align = androidx.compose.ui.text.style.TextAlign.Center)
        if (selecting) {
          T("All", Type.label, c.accentInk, Modifier.tap(label = "Select all text") {
            val data = frame ?: return@tap
            requestSelection(PointF(0f, 0f), PointF(data.width.toFloat(), data.height.toFloat()))
          }.padding(8.dp))
          T("Copy", Type.label, if (text.isNotBlank()) c.accentInk else c.faint,
            Modifier.tap(enabled = text.isNotBlank(), label = "Copy selected text") {
              (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("PDF text", text))
            }.padding(8.dp))
        }
      }
      if (problem.isNotEmpty()) T(problem, Type.small, c.sub, Modifier.padding(horizontal = 8.dp))
    }
  }
}
