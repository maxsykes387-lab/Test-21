package com.maxsykes.pixelorganiser.data

import android.content.pm.ApplicationInfo

/**
 * Decides which [Category] an app belongs to.
 *
 * Order of precedence:
 *  1. A category the user picked by long-pressing the app (handled by the repository).
 *  2. Known package names (covers the common apps on a Pixel precisely).
 *  3. The category the developer declared in the Play Store / manifest (ApplicationInfo.category).
 *  4. Keyword rules on the app's name and package name.
 *  5. Other.
 */
object Categorizer {

    private val knownPackages: Map<String, Category> = mapOf(
        // Google & system
        "com.android.settings" to Category.SYSTEM,
        "com.google.android.googlequicksearchbox" to Category.SYSTEM,
        "com.android.vending" to Category.SYSTEM,
        "com.google.android.apps.pixel.support" to Category.SYSTEM,
        "com.google.android.apps.wellbeing" to Category.SYSTEM,
        "com.google.android.apps.safetyhub" to Category.SYSTEM,
        "com.google.android.apps.tips" to Category.SYSTEM,
        "com.google.android.apps.subscriptions.red" to Category.SYSTEM,
        "com.google.android.apps.bard" to Category.SYSTEM,
        // Communication
        "com.google.android.apps.messaging" to Category.COMMUNICATION,
        "com.google.android.dialer" to Category.COMMUNICATION,
        "com.google.android.contacts" to Category.COMMUNICATION,
        "com.google.android.gm" to Category.COMMUNICATION,
        "com.google.android.apps.tachyon" to Category.COMMUNICATION,
        "com.whatsapp" to Category.COMMUNICATION,
        "org.telegram.messenger" to Category.COMMUNICATION,
        "org.thoughtcrime.securesms" to Category.COMMUNICATION,
        "com.facebook.orca" to Category.COMMUNICATION,
        "com.discord" to Category.COMMUNICATION,
        // Social
        "com.instagram.android" to Category.SOCIAL,
        "com.facebook.katana" to Category.SOCIAL,
        "com.twitter.android" to Category.SOCIAL,
        "com.zhiliaoapp.musically" to Category.SOCIAL,
        "com.snapchat.android" to Category.SOCIAL,
        "com.reddit.frontpage" to Category.SOCIAL,
        "com.linkedin.android" to Category.SOCIAL,
        "com.instagram.barcelona" to Category.SOCIAL,
        "com.bsky.app" to Category.SOCIAL,
        // Productivity
        "com.google.android.calendar" to Category.PRODUCTIVITY,
        "com.google.android.keep" to Category.PRODUCTIVITY,
        "com.google.android.apps.docs" to Category.PRODUCTIVITY,
        "com.google.android.apps.docs.editors.docs" to Category.PRODUCTIVITY,
        "com.google.android.apps.docs.editors.sheets" to Category.PRODUCTIVITY,
        "com.google.android.apps.docs.editors.slides" to Category.PRODUCTIVITY,
        "com.google.android.apps.tasks" to Category.PRODUCTIVITY,
        "com.google.android.deskclock" to Category.PRODUCTIVITY,
        "com.google.android.calculator" to Category.PRODUCTIVITY,
        "com.google.android.apps.recorder" to Category.PRODUCTIVITY,
        "com.microsoft.teams" to Category.PRODUCTIVITY,
        "com.Slack" to Category.PRODUCTIVITY,
        "notion.id" to Category.PRODUCTIVITY,
        "com.anthropic.claude" to Category.PRODUCTIVITY,
        // Entertainment
        "com.google.android.youtube" to Category.ENTERTAINMENT,
        "com.google.android.apps.youtube.music" to Category.ENTERTAINMENT,
        "com.spotify.music" to Category.ENTERTAINMENT,
        "com.netflix.mediaclient" to Category.ENTERTAINMENT,
        "com.disney.disneyplus" to Category.ENTERTAINMENT,
        "com.amazon.avod.thirdpartyclient" to Category.ENTERTAINMENT,
        "tv.twitch.android.app" to Category.ENTERTAINMENT,
        "com.google.android.videos" to Category.ENTERTAINMENT,
        "bbc.iplayer.android" to Category.ENTERTAINMENT,
        // Photos
        "com.google.android.GoogleCamera" to Category.PHOTOS,
        "com.google.android.apps.photos" to Category.PHOTOS,
        "com.google.android.apps.photosgo" to Category.PHOTOS,
        "com.adobe.lrmobile" to Category.PHOTOS,
        "com.pinterest" to Category.PHOTOS,
        // Shopping & money
        "com.google.android.apps.walletnfcrel" to Category.SHOPPING,
        "com.amazon.mShop.android.shopping" to Category.SHOPPING,
        "com.ebay.mobile" to Category.SHOPPING,
        "com.paypal.android.p2pmobile" to Category.SHOPPING,
        "com.revolut.revolut" to Category.SHOPPING,
        "co.uk.getmondo" to Category.SHOPPING,
        "com.vinted" to Category.SHOPPING,
        // Travel
        "com.google.android.apps.maps" to Category.TRAVEL,
        "com.waze" to Category.TRAVEL,
        "com.ubercab" to Category.TRAVEL,
        "com.airbnb.android" to Category.TRAVEL,
        "com.booking" to Category.TRAVEL,
        "com.citymapper.app.release" to Category.TRAVEL,
        "com.trainline.trainline" to Category.TRAVEL,
        // News
        "com.google.android.apps.magazines" to Category.NEWS,
        "com.google.android.apps.books" to Category.NEWS,
        "bbc.mobile.news.uk" to Category.NEWS,
        "bbc.mobile.news.ww" to Category.NEWS,
        "com.amazon.kindle" to Category.NEWS,
        "com.medium.reader" to Category.NEWS,
        // Health
        "com.google.android.apps.fitness" to Category.HEALTH,
        "com.fitbit.FitbitMobile" to Category.HEALTH,
        "com.strava" to Category.HEALTH,
        "com.myfitnesspal.android" to Category.HEALTH,
        "com.calm.android" to Category.HEALTH,
        "com.getsomeheadspace.android" to Category.HEALTH,
        // Tools
        "com.google.android.apps.nbu.files" to Category.UTILITIES,
        "com.google.android.documentsui" to Category.UTILITIES,
        "com.android.chrome" to Category.UTILITIES,
        "com.google.android.apps.translate" to Category.UTILITIES,
        "com.google.ar.lens" to Category.UTILITIES,
        "com.google.android.apps.authenticator2" to Category.UTILITIES,
        "com.google.android.apps.chromecast.app" to Category.UTILITIES,
        "com.google.android.apps.weather" to Category.UTILITIES,
        "com.google.android.apps.pixel.weather" to Category.UTILITIES,
        "com.google.android.apps.security.securityhub" to Category.SYSTEM,
    )

    private val keywordRules: List<Pair<Category, List<String>>> = listOf(
        Category.GAMES to listOf("game", "games", "puzzle", "chess", "craft", "clash", "candy", "solitaire", "racing", "minecraft", "roblox", "pokemon", "sudoku", "wordle"),
        Category.COMMUNICATION to listOf("messag", "chat", "mail", "dialer", "phone", "contacts", "sms", "call", "meet", "voice", "telegram", "signal"),
        Category.SOCIAL to listOf("social", "facebook", "instagram", "tiktok", "twitter", "threads", "snap", "reddit", "linkedin", "mastodon", "bluesky", "dating", "tinder", "bumble"),
        Category.ENTERTAINMENT to listOf("music", "video", "tv", "movie", "film", "stream", "podcast", "radio", "player", "spotify", "netflix", "youtube", "audible", "sound"),
        Category.PHOTOS to listOf("camera", "photo", "gallery", "picture", "snapseed", "editor", "lens", "scanner"),
        Category.SHOPPING to listOf("shop", "store", "bank", "pay", "wallet", "money", "finance", "card", "invest", "crypto", "market", "deal", "coupon", "food", "eats", "deliver"),
        Category.TRAVEL to listOf("map", "travel", "flight", "train", "rail", "bus", "taxi", "ride", "hotel", "trip", "navig", "transit", "parking", "airline"),
        Category.NEWS to listOf("news", "read", "book", "magazine", "kindle", "rss", "journal", "weather"),
        Category.HEALTH to listOf("health", "fit", "workout", "run", "step", "sleep", "meditat", "yoga", "diet", "calorie", "period", "medic"),
        Category.PRODUCTIVITY to listOf("calendar", "note", "task", "todo", "docs", "sheet", "slide", "office", "drive", "work", "clock", "alarm", "calculator", "recorder", "scan", "pdf", "assistant"),
        Category.UTILITIES to listOf("file", "browser", "chrome", "firefox", "vpn", "translate", "tool", "util", "clean", "backup", "authenticator", "password", "remote", "home", "keyboard"),
        Category.SYSTEM to listOf("settings", "google", "android", "pixel", "system", "launcher", "play store"),
    )

    fun categorize(packageName: String, label: String, appInfo: ApplicationInfo?): Category {
        knownPackages[packageName]?.let { return it }

        appInfo?.let { info -> fromPlatformCategory(info.category)?.let { return it } }

        val haystack = "${label.lowercase()} ${packageName.lowercase().replace('.', ' ')}"
        for ((category, words) in keywordRules) {
            if (words.any { word -> haystack.containsWord(word) }) return category
        }

        if (appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
            return Category.SYSTEM
        }
        return Category.OTHER
    }

    private fun fromPlatformCategory(value: Int): Category? = when (value) {
        ApplicationInfo.CATEGORY_GAME -> Category.GAMES
        ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO -> Category.ENTERTAINMENT
        ApplicationInfo.CATEGORY_IMAGE -> Category.PHOTOS
        ApplicationInfo.CATEGORY_SOCIAL -> Category.SOCIAL
        ApplicationInfo.CATEGORY_NEWS -> Category.NEWS
        ApplicationInfo.CATEGORY_MAPS -> Category.TRAVEL
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> Category.PRODUCTIVITY
        ApplicationInfo.CATEGORY_ACCESSIBILITY -> Category.UTILITIES
        else -> null // CATEGORY_UNDEFINED and anything newer
    }

    /** Matches whole words, or word prefixes for short stems like "messag" or "navig". */
    private fun String.containsWord(word: String): Boolean {
        var index = indexOf(word)
        while (index >= 0) {
            val startOk = index == 0 || !this[index - 1].isLetter()
            if (startOk) return true
            index = indexOf(word, index + 1)
        }
        return false
    }
}
