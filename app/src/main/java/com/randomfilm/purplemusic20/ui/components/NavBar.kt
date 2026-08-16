package com.randomfilm.purplemusic20.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.randomfilm.purplemusic20.R
import com.randomfilm.purplemusic20.ui.theme.*

@Composable
fun BottomNavBar(curr: String?, nav: (String) -> Unit) {
    NavigationBar(containerColor = LocalAppColors.current.navBg) {
        listOf(
            Triple("home", stringResource(R.string.nav_home_label), Icons.Rounded.Home),
            Triple("mixs", stringResource(R.string.nav_mixs_label), Icons.Rounded.LibraryMusic),
            Triple("create", stringResource(R.string.nav_upload_label), Icons.Rounded.AddBox),
            Triple("downloads", stringResource(R.string.label_offline), Icons.Rounded.DownloadForOffline),
            Triple("queue", stringResource(R.string.nav_queue_label), Icons.Rounded.QueueMusic)
        ).forEach { (r, l, i) ->
            NavigationBarItem(
                icon = { Icon(i, null, Modifier.size(26.dp)) }, label = { Text(l, fontSize = 10.sp) },
                selected = curr == r, onClick = { nav(r) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = LocalAppColors.current.accent, selectedTextColor = LocalAppColors.current.accent, indicatorColor = Color.Transparent, unselectedIconColor = LocalAppColors.current.textSecondary, unselectedTextColor = LocalAppColors.current.textSecondary)
            )
        }
    }
}
