package com.maxsykes.pixelorganiser.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class AppEntry(
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val icon: ImageBitmap,
    val autoCategory: Category,
)

private val Context.dataStore by preferencesDataStore(name = "organiser")

class AppRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    /** Every launchable app on the phone, except this one. */
    suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val iconSizePx = (60 * context.resources.displayMetrics.density).toInt()

        pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map { info ->
                val activity = info.activityInfo
                val label = info.loadLabel(pm).toString()
                AppEntry(
                    label = label,
                    packageName = activity.packageName,
                    component = ComponentName(activity.packageName, activity.name),
                    icon = renderIcon(info.loadIcon(pm), iconSizePx).asImageBitmap(),
                    autoCategory = Categorizer.categorize(activity.packageName, label, activity.applicationInfo),
                )
            }
            .distinctBy { it.component }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * Draws the icon full-bleed as a square, so the UI can give every app the same
     * rounded-square shape (a Pixel and iPhone blend) instead of the system's circle.
     */
    private fun renderIcon(drawable: Drawable, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (drawable is AdaptiveIconDrawable) {
            // Adaptive layers are 108dp with a 72dp visible area: draw them 1.5x and centred.
            val pad = size / 4
            listOfNotNull(drawable.background, drawable.foreground).forEach { layer ->
                layer.setBounds(-pad, -pad, size + pad, size + pad)
                layer.draw(canvas)
            }
        } else {
            // Older square or odd-shaped icons sit on a soft white tile.
            canvas.drawColor(android.graphics.Color.WHITE)
            val inset = size / 7
            drawable.toBitmap(size - inset * 2, size - inset * 2).let {
                canvas.drawBitmap(it, inset.toFloat(), inset.toFloat(), null)
            }
        }
        return bitmap
    }

    /** Categories the user picked by hand: packageName -> Category. */
    val overrides: Flow<Map<String, Category>> = context.dataStore.data.map { prefs ->
        prefs.asMap().mapNotNull { (key, value) ->
            val category = runCatching { Category.valueOf(value as String) }.getOrNull()
            category?.let { key.name.removePrefix(PREFIX) to it }
        }.toMap()
    }

    suspend fun setOverride(packageName: String, category: Category?) {
        context.dataStore.edit { prefs ->
            val key = stringPreferencesKey(PREFIX + packageName)
            if (category == null) prefs.remove(key) else prefs[key] = category.name
        }
    }

    fun launch(app: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
    }

    fun openAppInfo(app: AppEntry) {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.fromParts("package", app.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private companion object {
        const val PREFIX = "cat:"
    }
}
