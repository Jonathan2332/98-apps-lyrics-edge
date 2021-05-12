package a98apps.lyricsedge.util

import a98apps.lyricsedge.constants.Constants
import a98apps.lyricsedge.preferences.SecurityPreferences
import android.content.Context
import android.os.PowerManager
import androidx.appcompat.app.AppCompatDelegate

class ThemeUtil
{
    companion object
    {
        fun setTheme(context: Context)
        {
            when(getThemeMode(context))
            {
                Constants.THEME_SYSTEM ->
                {
                    val service = context.getSystemService(Context.POWER_SERVICE)
                    if(service != null)
                    {
                        val powerManager = service as PowerManager
                        if(powerManager.isPowerSaveMode)
                        {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        }
                        else
                        {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        }
                    }
                }
                Constants.THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                Constants.THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else ->
                {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
        }

        private fun getThemeMode(context: Context): Int?
        {
            val prefs = SecurityPreferences(context)
            val mode = prefs.get(Constants.APP_THEME) as String?
            return mode?.toInt()
        }
    }
}