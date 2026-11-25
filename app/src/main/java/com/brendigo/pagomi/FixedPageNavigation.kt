package com.brendigo.pagomi

import kotlin.math.min

/** Zero-based page navigation, independent of zoom, rendering and scroll offsets. */
internal fun boundedPage(requested: Long, count: Int): Int =
  if (count <= 0) 0 else requested.coerceIn(0L, count.toLong() - 1L).toInt()

internal data class PageViewport(val left: Float, val top: Float, val width: Float, val height: Float) {
  fun contains(x: Float, y: Float) = x in left..(left + width) && y in top..(top + height)
  fun pdfX(x: Float, pageWidth: Int) = ((x - left) / width * pageWidth).coerceIn(0f, pageWidth.toFloat())
  fun pdfY(y: Float, pageHeight: Int) = ((y - top) / height * pageHeight).coerceIn(0f, pageHeight.toFloat())
}

/** Fit the complete page first. At fit zoom there is no pan; at higher zoom pan stays inside this page. */
internal fun pageViewport(viewWidth: Float, viewHeight: Float, pageWidth: Int, pageHeight: Int,
                          zoom: Float, panX: Float, panY: Float): PageViewport {
  val vw = viewWidth.coerceAtLeast(1f)
  val vh = viewHeight.coerceAtLeast(1f)
  val pw = pageWidth.coerceAtLeast(1)
  val ph = pageHeight.coerceAtLeast(1)
  val scale = min(vw / pw, vh / ph) * zoom.coerceIn(1f, 4f)
  val w = pw * scale
  val h = ph * scale
  val maxX = ((w - vw) / 2f).coerceAtLeast(0f)
  val maxY = ((h - vh) / 2f).coerceAtLeast(0f)
  return PageViewport((vw - w) / 2 + panX.coerceIn(-maxX, maxX), (vh - h) / 2 + panY.coerceIn(-maxY, maxY), w, h)
}
