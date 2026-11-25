package com.brendigo.pagomi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FixedPageNavigationTest {
  @Test fun pageBoundariesNeverOverflow() {
    assertEquals(0, boundedPage(Long.MIN_VALUE, 3))
    assertEquals(2, boundedPage(Long.MAX_VALUE, 3))
    assertEquals(0, boundedPage(0, 0))
    assertEquals(1, boundedPage(1, 3))
  }

  @Test fun fitPageDoesNotPan() {
    val original = pageViewport(360f, 600f, 595, 842, 1f, 0f, 0f)
    assertEquals(original, pageViewport(360f, 600f, 595, 842, 1f, 5000f, -5000f))
    assertTrue(original.height <= 600f && original.width <= 360f)
  }

  @Test fun zoomPanStaysInsidePageAndMapsCoordinates() {
    val v = pageViewport(360f, 600f, 595, 842, 4f, 100000f, -100000f)
    assertEquals(0f, v.left, 0.01f)
    assertEquals(600f, v.top + v.height, 0.01f)
    assertEquals(595f, v.pdfX(v.left + v.width, 595), 0.01f)
    assertEquals(842f, v.pdfY(v.top + v.height, 842), 0.01f)
  }
}
