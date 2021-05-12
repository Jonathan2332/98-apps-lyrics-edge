package a98apps.lyricsedge.model

import a98apps.lyricsedge.constants.Constants
import a98apps.lyricsedge.preferences.SecurityPreferences
import android.content.Context

class Design(context : Context)
{
    private val prefs = SecurityPreferences(context)
    val titleColor : Int = prefs.get(Constants.TITLE_COLOR) as Int
    val textColor : Int = prefs.get(Constants.TEXT_COLOR) as Int
    val dividerColor : Int = prefs.get(Constants.DIVIDER_COLOR) as Int
    val panelColor : Int = prefs.get(Constants.PANEL_COLOR) as Int
    val textSize : Float = (prefs.get(Constants.TEXT_SIZE) as Int).toFloat()
}