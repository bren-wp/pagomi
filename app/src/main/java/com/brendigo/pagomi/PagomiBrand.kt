package com.brendigo.pagomi

import androidx.compose.foundation.Image
import androidx.compose.ui.unit.sp
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
  Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Image(painterResource(R.drawable.pagomi_launcher), null, Modifier.size(40.dp))
    Column(Modifier.weight(1f)) {
      T(Brand.name, Type.title.copy(fontSize = 24.sp, lineHeight = 28.sp), c.ink, maxLines = 1)
      T("PDF Scanner & Editor", Type.small.copy(fontSize = 10.sp, lineHeight = 14.sp), c.sub, maxLines = 2)
    }
    T("Local", Type.num, Brand.ink, Modifier.background(Brand.mint, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 5.dp))
  }
}
