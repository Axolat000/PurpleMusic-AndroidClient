package com.randomfilm.purplemusic20.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.randomfilm.purplemusic20.R
import com.randomfilm.purplemusic20.data.*
import com.randomfilm.purplemusic20.ui.theme.*
import com.randomfilm.purplemusic20.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayer(t: String, a: String, c: String, p: Boolean, prog: Float, pp: () -> Unit, clk: () -> Unit, session: SessionManager) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalAppColors.current.panel), modifier = Modifier.fillMaxWidth().height(85.dp).clickable { clk() }, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = buildImageRequest(LocalContext.current, c.ifEmpty { R.drawable.default_cover }, session.isCoverCacheEnabled()), contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)).background(Color.DarkGray), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(t, color = Color.White, maxLines = 1, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.basicMarquee())
                    Text(a, color = LocalAppColors.current.accent, maxLines = 1, fontSize = 12.sp, modifier = Modifier.basicMarquee())
                }
                IconButton(onClick = pp, Modifier.size(45.dp).background(Color.White, CircleShape)) { Icon(if (p) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = LocalAppColors.current.background, modifier = Modifier.size(28.dp)) }
            }
            LinearProgressIndicator(progress = { prog }, modifier = Modifier.fillMaxWidth().height(2.dp), color = LocalAppColors.current.accent)
        }
    }
}
