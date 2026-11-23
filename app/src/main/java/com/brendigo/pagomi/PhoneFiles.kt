package com.brendigo.pagomi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Android's Storage Access Framework replaces broad "all files access".
 * Users select individual PDFs explicitly; the document editor keeps working
 * on the app-owned copy returned by pickPdfs(), without exposing shared storage.
 */
@Composable
fun PhoneFiles(query: String) {
  val scope = rememberCoroutineScope()
  Column(Modifier.padding(top = 16.dp)) {
    Empty(
      { ToolArt("files", 112.dp) },
      "Open a PDF from your phone",
      "Browse Downloads, Documents and cloud providers using Android's secure file picker. Only documents you choose are opened by pagomi.",
    ) {
      Btn("Browse PDF files", Modifier.fillMaxWidth(), I.folderOutline) {
        scope.launch {
          pickPdfs(false).firstOrNull()?.let { openFile(it.path, it.name) }
        }
      }
    }
    if (query.isNotBlank()) {
      T(
        "Search your saved documents from the Saved tab. To find another PDF, use Browse PDF files.",
        Type.small, c.sub,
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        align = TextAlign.Center,
      )
    }
  }
}
