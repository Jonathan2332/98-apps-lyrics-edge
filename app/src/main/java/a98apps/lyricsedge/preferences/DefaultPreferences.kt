package a98apps.lyricsedge.preferences

import a98apps.lyricsedge.R
import a98apps.lyricsedge.constants.Constants
import android.content.Context

open class DefaultPreferences(open val context: Context)
{
    protected fun getDefaultByKey(key: String): Any?
    {
        return when(key)
        {
            Constants.PANEL_COLOR -> context.getColor(android.R.color.white)
            Constants.TEXT_COLOR -> context.getColor(android.R.color.black)
            Constants.DIVIDER_COLOR -> context.getColor(R.color.grey)
            Constants.TITLE_COLOR -> context.getColor(R.color.blue)
            Constants.TEXT_SIZE -> 14
            Constants.AVAILABLE_TRANSLATE -> false
            Constants.CAPTCHA_PENDING -> false
            Constants.TRANSLATE_TOGGLE -> false
            Constants.ENABLE_TRANSLATION -> true
            Constants.ENABLED_PANEL -> true
            Constants.LYRICS_APPROX -> true
            Constants.IGNORE_LONG_TRACK -> true
            Constants.CACHE_INDEX -> -1L
            Constants.APP_THEME -> "0"
            Constants.CAPTCHA_VERIFICATION -> Constants.DEFAULT_VALUE_CAPTCHA
            Constants.CAPTCHA_SERIAL -> Constants.DEFAULT_VALUE_CAPTCHA
            Constants.CAPTCHA_TITLE_TARGET -> Constants.DEFAULT_VALUE_CAPTCHA
            Constants.CAPTCHA_NAME_TARGET -> Constants.DEFAULT_VALUE_CAPTCHA
            Constants.BLOCKED_HASH -> Constants.DEFAULT_VALUE_HASH
            Constants.TEMP_HASH -> Constants.DEFAULT_VALUE_HASH
            else -> null
        }
    }
}