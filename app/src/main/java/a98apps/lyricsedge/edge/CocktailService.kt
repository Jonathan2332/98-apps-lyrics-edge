package a98apps.lyricsedge.edge

import a98apps.lyricsedge.R
import a98apps.lyricsedge.constants.Api
import a98apps.lyricsedge.constants.Constants
import a98apps.lyricsedge.helper.dao.cache.CacheDAO
import a98apps.lyricsedge.model.Design
import a98apps.lyricsedge.model.Lyric
import a98apps.lyricsedge.preferences.SecurityPreferences
import a98apps.lyricsedge.util.FormatterUtil
import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.ColorUtils

class CocktailService : RemoteViewsService()
{
    override fun onGetViewFactory(intent: Intent?): RemoteViewsFactory
    {
        return LyricsViewFactor()
    }

    private inner class LyricsViewFactor : RemoteViewsFactory
    {
        private lateinit var mDesign: Design
        private lateinit var mLyric: Lyric

        private var hasCaptcha: Boolean = false
        private var hasPermission: Boolean = false
        private var translate: Boolean = false

        override fun onCreate()
        {

        }

        override fun onDataSetChanged()
        {
            this.mDesign = Design(applicationContext)

            hasPermission = NotificationManagerCompat.getEnabledListenerPackages(applicationContext).contains(packageName)

            val prefs = SecurityPreferences(applicationContext)
            hasCaptcha = prefs.get(Constants.CAPTCHA_PENDING) as Boolean
            translate = prefs.get(Constants.ENABLE_TRANSLATION) as Boolean && prefs.get(Constants.TRANSLATE_TOGGLE) as Boolean

            val lyricIndex = prefs.get(Constants.CACHE_INDEX) as Long
            if(lyricIndex != -1L)
            {
                val cacheDAO = CacheDAO(applicationContext)

                val lyric = cacheDAO.get(lyricIndex)
                if(lyric.musicId != null)
                {
                    this.mLyric = lyric
                }
                else
                {
                    initializeEmptyLyric(getString(R.string.app_name), getString(R.string.text_lyric_not_found_cache))
                }
            }
            else
            {
                initializeEmptyLyric(getString(R.string.app_name), getString(R.string.text_panel_default))
            }

            prefs.set(Constants.AVAILABLE_TRANSLATE, !mLyric.musicTranslate.isNullOrEmpty())
            Cocktail.updatePanel(applicationContext, false)
        }

        override fun onDestroy()
        {

        }

        override fun getCount(): Int
        {
            return 2
        }

        override fun getViewAt(position: Int): RemoteViews
        {
            // create list item
            val itemView: RemoteViews

            // set fill in intent
            val intent = Intent()

            if(position == 0)
            {
                itemView = RemoteViews(packageName, R.layout.cocktail_item_title_layout)

                //Set design
                try
                {
                    itemView.setTextColor(R.id.musicName, mDesign.titleColor)
                    itemView.setTextColor(R.id.artistName, mDesign.titleColor)
                    itemView.setTextColor(R.id.lyricType, mDesign.titleColor)
                }
                catch(ignore: ArrayIndexOutOfBoundsException)
                {

                }
                itemView.setTextViewTextSize(R.id.musicName, TypedValue.COMPLEX_UNIT_SP, mDesign.textSize)
                itemView.setTextViewTextSize(R.id.artistName, TypedValue.COMPLEX_UNIT_SP, mDesign.textSize)
                itemView.setTextViewTextSize(R.id.lyricType, TypedValue.COMPLEX_UNIT_SP, mDesign.textSize)
                itemView.setInt(R.id.divider, "setBackgroundColor", mDesign.dividerColor)

                //Set text
                if(!hasPermission)
                {
                    itemView.setTextViewText(R.id.musicName, getString(R.string.app_name))
                    itemView.setViewVisibility(R.id.artistName, View.GONE)
                    itemView.setViewVisibility(R.id.lyricType, View.GONE)
                }
                else if(hasCaptcha)
                {
                    itemView.setTextViewText(R.id.musicName, getString(R.string.title_activity_captcha))
                    itemView.setViewVisibility(R.id.artistName, View.GONE)
                    itemView.setViewVisibility(R.id.lyricType, View.GONE)
                }
                else
                {
                    itemView.setTextViewText(R.id.musicName, mLyric.musicName)
                    if(mLyric.artistName != null)
                    {
                        itemView.setViewVisibility(R.id.artistName, View.VISIBLE)
                        itemView.setTextViewText(R.id.artistName, mLyric.artistName)
                    }
                    else
                    {
                        itemView.setViewVisibility(R.id.artistName, View.GONE)
                    }

                    if(mLyric.type != null)
                    {
                        if(mLyric.type == Api.RESULT_TYPE_APPROX)
                        {
                            itemView.setViewVisibility(R.id.lyricType, View.VISIBLE)
                            itemView.setTextViewText(R.id.lyricType, getString(R.string.text_approximate_lowercase_parentheses))
                        }
                        else itemView.setViewVisibility(R.id.lyricType, View.GONE)
                    }
                    else
                    {
                        itemView.setViewVisibility(R.id.lyricType, View.GONE)
                    }
                }


                //Set intents
                setCaptchaIntent(intent, false)
                setURLIntent(intent, mLyric.artistUrl, null)
                val title = FormatterUtil.formatNewLine(mLyric.musicName ?: "", mLyric.artistName ?: "")
                setCopyIntent(intent, title, null)

                setSetupIntent(intent, false)
            }
            else
            {
                if(!hasPermission)
                {
                    itemView = RemoteViews(packageName, R.layout.cocktail_item_setup_layout)

                    //Set design
                    try
                    {
                        itemView.setTextColor(R.id.setupText, mDesign.textColor)
                        itemView.setTextColor(R.id.setupButton, mDesign.textColor)
                    }
                    catch(ignore: ArrayIndexOutOfBoundsException)
                    {

                    }
                    itemView.setTextViewText(R.id.setupText, getString(R.string.text_setup_panel))
                    itemView.setTextViewTextSize(R.id.setupText, TypedValue.COMPLEX_UNIT_SP, mDesign.textSize)
                    itemView.setTextViewTextSize(R.id.setupButton, TypedValue.COMPLEX_UNIT_SP, mDesign.textSize)
                    itemView.setInt(R.id.setupButtonIcon, "setColorFilter", mDesign.textColor)

                    //Set intents
                    setCaptchaIntent(intent, false)
                    setURLIntent(intent, null, null)
                    setCopyIntent(intent, null, null)

                    setSetupIntent(intent, true)
                }
                else if(hasCaptcha)
                {
                    itemView = RemoteViews(packageName, R.layout.cocktail_item_setup_layout)

                    //Set design
                    try
                    {
                        itemView.setTextColor(R.id.setupText, mDesign.textColor)
                        itemView.setTextColor(R.id.setupButton, mDesign.textColor)
                    }
                    catch(ignore: ArrayIndexOutOfBoundsException)
                    {

                    }
                    itemView.setTextViewText(R.id.setupText, getString(R.string.text_captcha_security))
                    itemView.setTextViewTextSize(R.id.setupText, TypedValue.COMPLEX_UNIT_SP, mDesign.textSize)
                    itemView.setTextViewTextSize(R.id.setupButton, TypedValue.COMPLEX_UNIT_SP, mDesign.textSize)
                    itemView.setInt(R.id.setupButtonIcon, "setColorFilter", mDesign.textColor)

                    //Set intents
                    setCaptchaIntent(intent, true)
                    setURLIntent(intent, null, null)
                    setCopyIntent(intent, null, null)

                    setSetupIntent(intent, true)
                }
                else
                {
                    itemView = RemoteViews(packageName, R.layout.cocktail_item_lyric_layout)

                    //Set design
                    try
                    {
                        itemView.setTextColor(R.id.lyricText, mDesign.textColor)
                    }
                    catch(ignore: ArrayIndexOutOfBoundsException)
                    {

                    }
                    itemView.setTextViewTextSize(R.id.lyricText, TypedValue.COMPLEX_UNIT_SP, mDesign.textSize)

                    //Set logo
                    itemView.setTextViewCompoundDrawablesRelative(
                        R.id.lyricText, 0, 0, 0,
                        if(mLyric.musicId != null)
                        {
                            if(isDark(mDesign.panelColor)) R.drawable.ic_logo_vagalume_light else R.drawable.ic_logo_vagalume_dark
                        }
                        else
                        {
                            0
                        }
                    )

                    //Set text
                    itemView.setTextViewText(R.id.lyricText,
                    if(translate && !mLyric.musicTranslate.isNullOrEmpty())
                    {
                        mLyric.musicTranslate
                    }
                    else
                    {
                        mLyric.musicLyric
                    })


                    //Set intents
                    setCaptchaIntent(intent, false)
                    setURLIntent(intent, null, mLyric.musicUrl)
                    setCopyIntent(intent, null, mLyric.musicLyric)

                    setSetupIntent(intent, false)
                }
            }

            // should be set fillInIntent to root of item layout
            itemView.setOnClickFillInIntent(R.id.item_root, intent)

            return itemView
        }

        override fun getLoadingView(): RemoteViews?
        {
            return null
        }

        override fun getViewTypeCount(): Int
        {
            return 3
        }

        override fun getItemId(position: Int): Long
        {
            return position.toLong()
        }

        override fun hasStableIds(): Boolean
        {
            return true
        }

        private fun isDark(color: Int): Boolean
        {
            return ColorUtils.calculateLuminance(color) < 0.5
        }

        private fun initializeEmptyLyric(title: String, text: String)
        {
            val lyric = Lyric()
            lyric.id = null
            lyric.type = null
            lyric.hash = null

            lyric.artistId = null
            lyric.artistHash = null
            lyric.artistSourceHash = null
            lyric.artistName = null
            lyric.artistUrl = null

            lyric.musicId = null
            lyric.musicHash = null
            lyric.musicSourceHash = null
            lyric.musicName = title
            lyric.musicLyric = text
            lyric.musicUrl = null
            lyric.musicTranslate = null
            this.mLyric = lyric
        }

        private fun setCaptchaIntent(intent: Intent, captcha: Boolean)
        {
            intent.putExtra("captcha", captcha)
        }

        private fun setSetupIntent(intent: Intent, permission: Boolean)
        {
            intent.putExtra("setup", permission)
        }

        private fun setURLIntent(intent: Intent, artistUrl: String?, lyricUrl: String?)
        {
            if(mLyric.musicId != null)
            {
                intent.putExtra("art_url", artistUrl ?: "")
                intent.putExtra("lyric_url", lyricUrl ?: "")
            }
            else
            {
                intent.putExtra("art_url", "")
                intent.putExtra("lyric_url", "")
            }
        }

        private fun setCopyIntent(intent: Intent, title: String?, lyric: String?)
        {
            if(mLyric.musicId != null)
            {
                intent.putExtra("title", title ?: "")
                intent.putExtra("lyric", lyric ?: "")
            }
            else
            {
                intent.putExtra("title", "")
                intent.putExtra("lyric", "")
            }
        }
    }
}