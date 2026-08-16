package com.randomfilm.purplemusic20.util

fun formatTime(ms: Long) = "%d:%02d".format(ms / 1000 / 60, ms / 1000 % 60)
