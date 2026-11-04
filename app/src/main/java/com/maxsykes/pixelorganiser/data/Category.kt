package com.maxsykes.pixelorganiser.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** The buckets apps are sorted into. Order here is the order shown on screen. */
enum class Category(val label: String, val icon: ImageVector, val accent: Color) {
    SOCIAL("Social", Icons.Rounded.Forum, Color(0xFF0B57D0)),
    COMMUNICATION("Messages & Calls", Icons.Rounded.Chat, Color(0xFF00639B)),
    PRODUCTIVITY("Productivity", Icons.Rounded.WorkOutline, Color(0xFF146C2E)),
    ENTERTAINMENT("Watch & Listen", Icons.Rounded.PlayCircle, Color(0xFFB3261E)),
    GAMES("Games", Icons.Rounded.SportsEsports, Color(0xFF7D5260)),
    PHOTOS("Photos & Camera", Icons.Rounded.PhotoCamera, Color(0xFFC26401)),
    SHOPPING("Wallet & Shopping", Icons.Rounded.ShoppingBag, Color(0xFF8E4585)),
    TRAVEL("Maps & Travel", Icons.Rounded.Map, Color(0xFF006A6A)),
    NEWS("News & Reading", Icons.Rounded.Newspaper, Color(0xFF5B5F97)),
    HEALTH("Health & Fitness", Icons.Rounded.FavoriteBorder, Color(0xFFBA1A1A)),
    UTILITIES("Tools", Icons.Rounded.Build, Color(0xFF52606D)),
    SYSTEM("Google & System", Icons.Rounded.Settings, Color(0xFF4285F4)),
    OTHER("Other", Icons.Rounded.Apps, Color(0xFF6F7979));
}
