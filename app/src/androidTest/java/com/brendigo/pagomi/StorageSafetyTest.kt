package com.brendigo.pagomi

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class StorageSafetyTest {
  @Before fun setup() {
    appContext = InstrumentationRegistry.getInstrumentation().targetContext
  }

  @Test fun dotDirectoryNamesCannotEscapeStorage() {
    assertEquals("", cleanName("."))
    assertEquals("", cleanName(" .. "))
    assertEquals("a_b", cleanName("a/b"))
  }

  @Test fun outsideFileCannotBeMovedRenamedOrDeleted() {
    val source = File.createTempFile("storage-safety-", ".pdf", appContext.cacheDir)
    val original = byteArrayOf(1, 2, 3, 4)
    source.writeBytes(original)
    try {
      val saved = Saved(source.name, source.path, source.length(), source.lastModified())
      assertEquals(saved, renameFile(saved, "renamed"))
      moveFileToRoot(source.path)
      removeFile(source.path)
      assertTrue(source.isFile)
      assertTrue(source.readBytes().contentEquals(original))
      assertTrue(listFiles(appContext.cacheDir.path).isEmpty())
      assertThrows(IllegalArgumentException::class.java) {
        saveBytes("storage-safety", "pdf", original, appContext.cacheDir.path)
      }
    } finally {
      source.delete()
    }
  }

  @Test fun missingFileRenameDoesNotPublishInventedPath() {
    val name = "missing-${UUID.randomUUID()}.pdf"
    val file = File(rootFolder(), name)
    val saved = Saved(name, file.path, 0, 0)
    assertFalse(file.exists())
    assertEquals(saved, renameFile(saved, "another-name"))
  }
}
