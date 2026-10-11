package com.example.action

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class AppInfo(
    val name: String,
    val packageName: String,
    val isGame: Boolean = false,
    val isInstalled: Boolean = true,
    val category: String = "App",
    val defaultActivityPrompt: String = ""
)

class AppLauncherManager(private val context: Context) {

    // Pre-mapped popular apps and games with standard packages and suggested activities
    private val popularDirectory = listOf(
        // Games
        AppInfo(
            name = "Chess",
            packageName = "com.chess",
            isGame = true,
            category = "Game",
            defaultActivityPrompt = "Solve 3 daily chess puzzles & play 1 match"
        ),
        AppInfo(
            name = "Subway Surfers",
            packageName = "com.kiloo.subwaysurf",
            isGame = true,
            category = "Game",
            defaultActivityPrompt = "Complete daily high-score run"
        ),
        AppInfo(
            name = "Minecraft",
            packageName = "com.mojang.minecraftpe",
            isGame = true,
            category = "Game",
            defaultActivityPrompt = "Explore world and gather building materials"
        ),
        AppInfo(
            name = "Roblox",
            packageName = "com.roblox.client",
            isGame = true,
            category = "Game",
            defaultActivityPrompt = "Play community mini-game session"
        ),
        AppInfo(
            name = "Genshin Impact",
            packageName = "com.miHoYo.GenshinImpact",
            isGame = true,
            category = "Game",
            defaultActivityPrompt = "Complete 4 daily adventure commissions"
        ),
        AppInfo(
            name = "Wordle",
            packageName = "com.nytimes.crossword",
            isGame = true,
            category = "Game",
            defaultActivityPrompt = "Solve daily word challenge"
        ),
        AppInfo(
            name = "Candy Crush",
            packageName = "com.king.candycrushsaga",
            isGame = true,
            category = "Game",
            defaultActivityPrompt = "Beat 2 puzzle levels"
        ),

        // Productivity, Learning & Media Apps
        AppInfo(
            name = "Spotify",
            packageName = "com.spotify.music",
            isGame = false,
            category = "Media",
            defaultActivityPrompt = "Listen to Deep Focus concentration playlist"
        ),
        AppInfo(
            name = "YouTube",
            packageName = "com.google.android.youtube",
            isGame = false,
            category = "Media",
            defaultActivityPrompt = "Watch 1 educational or workout video"
        ),
        AppInfo(
            name = "Duolingo",
            packageName = "com.duolingo",
            isGame = false,
            category = "Learning",
            defaultActivityPrompt = "Complete 1 daily language lesson & keep streak"
        ),
        AppInfo(
            name = "Chrome",
            packageName = "com.android.chrome",
            isGame = false,
            category = "Productivity",
            defaultActivityPrompt = "Read research article or documentation"
        ),
        AppInfo(
            name = "Discord",
            packageName = "com.discord",
            isGame = false,
            category = "Social",
            defaultActivityPrompt = "Check community & team channels"
        ),
        AppInfo(
            name = "Notion",
            packageName = "notion.id",
            isGame = false,
            category = "Productivity",
            defaultActivityPrompt = "Update daily sprint board & journal"
        ),
        AppInfo(
            name = "Calculator",
            packageName = "com.google.android.calculator",
            isGame = false,
            category = "Tool",
            defaultActivityPrompt = "Calculate budget or productivity numbers"
        ),
        AppInfo(
            name = "Clock",
            packageName = "com.google.android.deskclock",
            isGame = false,
            category = "Tool",
            defaultActivityPrompt = "Set alarms or countdown timers"
        ),
        AppInfo(
            name = "Camera",
            packageName = "com.google.android.GoogleCamera",
            isGame = false,
            category = "Tool",
            defaultActivityPrompt = "Capture photo of whiteboards or workout progress"
        ),
        AppInfo(
            name = "Settings",
            packageName = "com.android.settings",
            isGame = false,
            category = "System",
            defaultActivityPrompt = "Configure device preferences"
        )
    )

    suspend fun getInstalledOrAvailableApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedMap = mutableMapOf<String, String>()

        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            for (info in resolveInfos) {
                val pkg = info.activityInfo.packageName
                val label = info.loadLabel(pm).toString()
                installedMap[pkg] = label
            }
        } catch (e: Exception) {
            // Ignore if restricted
        }

        val result = mutableListOf<AppInfo>()

        // Add from installed map
        for ((pkg, label) in installedMap) {
            val isKnownGame = label.contains("game", ignoreCase = true) ||
                    label.contains("play", ignoreCase = true) ||
                    pkg.contains("game", ignoreCase = true)
            result.add(
                AppInfo(
                    name = label,
                    packageName = pkg,
                    isGame = isKnownGame,
                    isInstalled = true,
                    category = if (isKnownGame) "Game" else "App",
                    defaultActivityPrompt = "Launch $label activity"
                )
            )
        }

        // Merge with popular directory (if not already added)
        for (item in popularDirectory) {
            val alreadyPresent = result.any { it.name.equals(item.name, ignoreCase = true) || it.packageName == item.packageName }
            if (!alreadyPresent) {
                val isInstalled = isPackageInstalled(item.packageName)
                result.add(item.copy(isInstalled = isInstalled))
            }
        }

        result.sortedWith(compareBy({ !it.isInstalled }, { !it.isGame }, { it.name }))
    }

    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    fun findApp(nameQuery: String): AppInfo? {
        val clean = nameQuery.trim().lowercase(Locale.getDefault())

        // 1. Check popular directory first
        val popularMatch = popularDirectory.firstOrNull {
            it.name.lowercase(Locale.getDefault()) == clean ||
                    it.name.lowercase(Locale.getDefault()).contains(clean) ||
                    clean.contains(it.name.lowercase(Locale.getDefault()))
        }
        if (popularMatch != null) return popularMatch

        // 2. Check installed launcher activities
        val pm = context.packageManager
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val list = pm.queryIntentActivities(mainIntent, 0)
            for (info in list) {
                val label = info.loadLabel(pm).toString()
                if (label.lowercase(Locale.getDefault()).contains(clean) || clean.contains(label.lowercase(Locale.getDefault()))) {
                    return AppInfo(
                        name = label,
                        packageName = info.activityInfo.packageName,
                        isGame = label.contains("game", ignoreCase = true),
                        isInstalled = true,
                        category = "App",
                        defaultActivityPrompt = "Launch $label activity"
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        return null
    }

    fun launchAppByPackage(packageName: String): Boolean {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return true
        }

        // Fallback: Open in Google Play Store or Web
        return try {
            val playIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(playIntent)
            true
        } catch (e: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    fun launchAppByName(name: String): Pair<Boolean, String> {
        val found = findApp(name)
        if (found != null) {
            val launched = launchAppByPackage(found.packageName)
            return Pair(launched, found.name)
        }

        // Generic search on Play Store or browser if app is not found
        return try {
            val searchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${Uri.encode(name)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(searchIntent)
            Pair(true, name)
        } catch (e: Exception) {
            Pair(false, name)
        }
    }
}
