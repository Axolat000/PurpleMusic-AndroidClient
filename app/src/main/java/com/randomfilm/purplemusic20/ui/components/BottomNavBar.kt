package com.randomfilm.purplemusic20.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.randomfilm.purplemusic20.ui.theme.AccentPurple
import com.randomfilm.purplemusic20.ui.theme.NavBg
import com.randomfilm.purplemusic20.ui.theme.TextGray

@Composable
fun BottomNavBar(curr: String?, nav: (String) -> Unit) {
    NavigationBar(containerColor = NavBg) {
        listOf(
            Triple("home", "Biblio", Icons.Rounded.Home),
            Triple("mixs", "Mixs", Icons.Rounded.LibraryMusic),
            Triple("create", "Upload", Icons.Rounded.AddBox),
            Triple("downloads", "Hors-ligne", Icons.Rounded.DownloadForOffline),
            Triple("queue", "File", Icons.Rounded.QueueMusic)
        ).forEach { (r, l, i) ->
            NavigationBarItem(
                icon = { Icon(i, null, Modifier.size(26.dp)) }, label = { Text(l, fontSize = 10.sp) },
                selected = curr == r, onClick = { nav(r) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentPurple, selectedTextColor = AccentPurple, indicatorColor = Color.Transparent, unselectedIconColor = TextGray, unselectedTextColor = TextGray)
            )
        }
    }
}
