package com.brendigo.pagomi

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun PagomiHeader() {
  Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    Image(painterResource(R.drawable.pagomi_launcher), null, Modifier.size(52.dp))
    Column(Modifier.weight(1f)) {
      T(Brand.name, Type.largeTitle, c.ink, maxLines = 1)
      T("PDF Scanner & Editor", Type.small, c.sub, maxLines = 2)
    }
    T("Local", Type.num, Brand.ink, Modifier.background(Brand.mint, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 8.dp))
  }
}
