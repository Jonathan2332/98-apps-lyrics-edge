package a98apps.lyricsedge.edge

import a98apps.lyricsedge.R
import a98apps.lyricsedge.constants.Actions
import a98apps.lyricsedge.constants.Constants
import a98apps.lyricsedge.helper.dao.cache.CacheDAO
import a98apps.lyricsedge.model.Design
import a98apps.lyricsedge.network.LyricsRequest
import a98apps.lyricsedge.notification.NotificationListener
import a98apps.lyricsedge.preferences.SecurityPreferences
import a98apps.lyricsedge.view.CaptchaActivity
import a98apps.lyricsedge.view.SetupActivity
import android.app.ActivityManager
import android.app.PendingIntent
import android.content.*
import android.net.Uri
import android.text.TextUtils
import android.util.Base64
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider

class Cocktail : SlookCocktailProvider()
{
    override fun onUpdate(context: Context?, cocktailManager: SlookCocktailManager?, cocktailIds: IntArray?)
    {
        super.onUpdate(context, cocktailManager, cocktailIds)
        if(context != null)
        {
            val cm = cocktailManager ?: SlookCocktailManager.getInstance(context)
            val ids = cocktailIds ?: cm.getCocktailIds(ComponentName(context, Cocktail::class.java))

            updatePanel(context, cm, ids, false)
        }
    }

    override fun onVisibilityChanged(context: Context?, cocktailId: Int, visibility: Int)
    {
        super.onVisibilityChanged(context, cocktailId, visibility)

        if(context != null)
        {
            if(visibility == SlookCocktailManager.COCKTAIL_VISIBILITY_SHOW)
            {
                if(!isServiceRunning(context, NotificationListener::class.java.name))
                {
                    context.startService(Intent(context, NotificationListener::class.java))
                }
            }
        }
    }

    override fun onEnabled(context: Context?)
    {
        super.onEnabled(context)
        if(context != null)
        {
            val prefs = SecurityPreferences(context)
            prefs.set(Constants.ENABLED_PANEL, true)

            if(!isServiceRunning(context, NotificationListener::class.java.name))
            {
                context.startService(Intent(context, NotificationListener::class.java).setAction(Actions.ACTION_RESET_LISTENER))
            }
        }
    }

    override fun onDisabled(context: Context?)
    {
        super.onDisabled(context)
        if(context != null)
        {
            val prefs = SecurityPreferences(context)
            prefs.set(Constants.ENABLED_PANEL, false)

            context.stopService(Intent(context, NotificationListener::class.java))
        }
    }

    override fun onReceive(context: Context?, intent: Intent?)
    {
        super.onReceive(context, intent)
        if(context != null && intent != null)
        {
            val action = intent.action
            if(action != null)
            {
                when(action)
                {
                    Actions.ACTION_PULL_TO_REFRESH -> performPullToRefresh(context)
                    Actions.ACTION_REMOTE_LIST_CLICK -> performRemoteListClick(context, intent)
                    Actions.ACTION_REMOTE_LONG_CLICK -> performRemoteLongClick(context, intent)
                    else ->
                    {
                    }
                }
            }
        }
    }

    private fun performRemoteListClick(context: Context, intent: Intent)
    {
        val id = intent.getIntExtra("id", -1)
        if(id == R.id.remote_list)
        {
            val artistUrl = intent.getStringExtra("art_url")
            val lyricUrl = intent.getStringExtra("lyric_url")
            val setup = intent.getBooleanExtra("setup", false)
            val captcha = intent.getBooleanExtra("captcha", false)

            when
            {
                captcha ->
                {
                    val intentCaptcha = Intent(context, CaptchaActivity::class.java)
                    intentCaptcha.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    intentCaptcha.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intentCaptcha.putExtra("captcha_success", Constants.CAPTCHA_UNKNOWN)
                    intentCaptcha.putExtra("captcha_url", SecurityPreferences(context).get(Constants.CAPTCHA_VERIFICATION) as String)
                    context.startActivity(intentCaptcha)
                }
                setup ->
                {
                    val intentSetup = Intent(context, SetupActivity::class.java)
                    intentSetup.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    intentSetup.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intentSetup)
                }
                lyricUrl != null && !TextUtils.isEmpty(lyricUrl) ->
                {
                    val intentLyricUrl = Intent(Intent.ACTION_VIEW, Uri.parse(lyricUrl))
                    intentLyricUrl.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intentLyricUrl)
                }
                artistUrl != null && !TextUtils.isEmpty(artistUrl) ->
                {
                    val intentArtistUrl = Intent(Intent.ACTION_VIEW, Uri.parse(artistUrl))
                    intentArtistUrl.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intentArtistUrl)
                }
            }
        }
        else if(id == R.id.translate)
        {
            val prefs = SecurityPreferences(context)
            val current = prefs.get(Constants.TRANSLATE_TOGGLE) as Boolean
            prefs.set(Constants.TRANSLATE_TOGGLE, !current)

            updateList(context)
            updatePanel(context, false)
        }
    }

    private fun performRemoteLongClick(context: Context, intent: Intent)
    {
        val id = intent.getIntExtra("id", -1)
        if(id == R.id.remote_list)
        {
            copyToClipboard(context, intent)
        }
    }

    private fun performPullToRefresh(context: Context)
    {
        val prefs = SecurityPreferences(context)
        prefs.setDefault(Constants.BLOCKED_HASH)

        val lyricIndex = prefs.get(Constants.CACHE_INDEX) as Long
        if(lyricIndex != -1L)
        {
            val cacheDAO = CacheDAO(context)
            val lyric = cacheDAO.get(lyricIndex)
            if(lyric.musicId != null)
            {
                if(!LyricsRequest.isRunning)
                {
                    val pullIntent = Intent(Actions.ACTION_PULL_TO_REFRESH)
                    pullIntent.putExtra("music_id", lyric.musicId)
                    pullIntent.putExtra("artist_id", lyric.artistId)
                    pullIntent.putExtra("hash", lyric.hash)
                    pullIntent.putExtra("source_music_hash", lyric.musicSourceHash)
                    pullIntent.putExtra("source_artist_hash", lyric.artistSourceHash)
                    pullIntent.putExtra("artist_name", String(Base64.decode(lyric.artistSourceHash, Base64.DEFAULT), Charsets.UTF_8))
                    pullIntent.putExtra("music_name", String(Base64.decode(lyric.musicSourceHash, Base64.DEFAULT), Charsets.UTF_8))
                    context.sendBroadcast(pullIntent)
                    return
                }
            }
        }

        val cocktailManager = SlookCocktailManager.getInstance(context)
        val cocktailIds = cocktailManager.getCocktailIds(ComponentName(context, Cocktail::class.java))
        for(id in cocktailIds)
        {
            cocktailManager.notifyCocktailViewDataChanged(id, R.id.remote_list)
        }
    }

    companion object
    {
        fun isEnabled(context: Context): Boolean
        {
            val prefs = SecurityPreferences(context)
            return prefs.get(Constants.ENABLED_PANEL) as Boolean
        }

        private fun getLongClickIntent(context: Context): PendingIntent?
        {
            val longClickIntent = Intent(context, Cocktail::class.java)
            longClickIntent.action = Actions.ACTION_REMOTE_LONG_CLICK
            longClickIntent.putExtra("id", R.id.remote_list)
            return PendingIntent.getBroadcast(
                context, R.id.remote_list, longClickIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        private fun getClickIntent(context: Context, id: Int): PendingIntent?
        {
            val clickIntent = Intent(context, Cocktail::class.java)
            clickIntent.action = Actions.ACTION_REMOTE_LIST_CLICK
            clickIntent.putExtra("id", id)
            return PendingIntent.getBroadcast(
                context, id, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        fun updateList(context: Context)
        {
            val cocktailManager = SlookCocktailManager.getInstance(context)
            val cocktailIds = cocktailManager.getCocktailIds(ComponentName(context, Cocktail::class.java))
            for(id in cocktailIds)
            {
                cocktailManager.notifyCocktailViewDataChanged(id, R.id.remote_list)
            }
        }

        fun updatePanel(context: Context, scroll: Boolean)
        {
            val cocktailManager = SlookCocktailManager.getInstance(context)
            val cocktailIds = cocktailManager.getCocktailIds(ComponentName(context, Cocktail::class.java))
            updatePanel(context, cocktailManager, cocktailIds, scroll)
        }

        fun updatePanel(context: Context, cocktailManager: SlookCocktailManager, cocktailIds: IntArray, scroll: Boolean)
        {
            for(id in cocktailIds)
            {
                //create view
                val remoteViews = RemoteViews(context.packageName, R.layout.cocktail_layout)

                //create subarea view
                val remoteSubViews = RemoteViews(context.packageName, R.layout.cocktail_subarea_layout)

                //set adapter
                val remoteIntent = Intent(context, CocktailService::class.java)
                remoteViews.setRemoteAdapter(R.id.remote_list, remoteIntent)

                //set intent click
                cocktailManager.setOnLongClickPendingIntentTemplate(remoteViews, R.id.remote_list, getLongClickIntent(context))
                remoteViews.setPendingIntentTemplate(R.id.remote_list, getClickIntent(context, R.id.remote_list))
                remoteViews.setOnClickPendingIntent(R.id.translate, getClickIntent(context, R.id.translate))

                //set panel color
                val design = Design(context)
                remoteViews.setInt(R.id.remote_list, "setBackgroundColor", design.panelColor)

                //check loader
                remoteSubViews.setViewVisibility(R.id.loader, if(LyricsRequest.isRunning) View.VISIBLE else View.GONE)

                //check translate
                val prefs = SecurityPreferences(context)
                val toggleTranslation = prefs.get(Constants.TRANSLATE_TOGGLE) as Boolean
                val enableTranslation = prefs.get(Constants.ENABLE_TRANSLATION) as Boolean
                val availableTranslate = prefs.get(Constants.AVAILABLE_TRANSLATE) as Boolean
                remoteViews.setViewVisibility(R.id.translate, if(enableTranslation && availableTranslate) View.VISIBLE else View.GONE)
                remoteViews.setTextViewText(R.id.translate, if(toggleTranslation) context.getString(R.string.text_original) else context.getString(R.string.title_translation))

                //lyric changed, scroll to top
                if(scroll)
                {
                    remoteViews.setScrollPosition(R.id.remote_list, 0)
                }

                cocktailManager.updateCocktail(id, remoteViews, remoteSubViews)

                if(NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName))
                {
                    val captchaPending = prefs.get(Constants.CAPTCHA_PENDING) as Boolean
                    if(!captchaPending)
                    {
                        val lyricIndex = prefs.get(Constants.CACHE_INDEX) as Long
                        if(lyricIndex != -1L)
                        {
                            if(!LyricsRequest.isRunning)
                            {
                                // set pull to refresh
                                val refreshIntent = Intent(context, Cocktail::class.java)
                                refreshIntent.action = Actions.ACTION_PULL_TO_REFRESH
                                val pendingIntent = PendingIntent.getBroadcast(context, 0xff, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                                cocktailManager.setOnPullPendingIntent(id, R.id.remote_list, pendingIntent)
                            }
                            else cocktailManager.setOnPullPendingIntent(id, R.id.remote_list, null)
                        }
                        else cocktailManager.setOnPullPendingIntent(id, R.id.remote_list, null)
                    }
                    else cocktailManager.setOnPullPendingIntent(id, R.id.remote_list, null)
                }
                else cocktailManager.setOnPullPendingIntent(id, R.id.remote_list, null)
            }
        }
    }

    private fun copyToClipboard(context: Context, intent: Intent)
    {
        var label: String? = null
        var item: String? = null

        if(intent.hasExtra("title") && intent.getStringExtra("title") != null && !TextUtils.isEmpty(intent.getStringExtra("title")))
        {
            label = "title"
            item = intent.getStringExtra("title")
        }
        else if(intent.hasExtra("lyric") && intent.getStringExtra("lyric") != null && !TextUtils.isEmpty(intent.getStringExtra("lyric")))
        {
            label = "lyric"
            item = intent.getStringExtra("lyric")
        }

        if(label != null && item != null)
        {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, item)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, context.getString(R.string.text_copied_to_clipboard), Toast.LENGTH_SHORT).show()
        }
    }

    //Used to check if internal services if running as NotificationListener
    @Suppress("DEPRECATION")
    private fun isServiceRunning(context: Context, serviceClassName: String): Boolean
    {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val services = activityManager.getRunningServices(Int.MAX_VALUE)
        for(runningServiceInfo in services)
        {
            if(runningServiceInfo.service.className == serviceClassName)
            {
                return true
            }
        }
        return false
    }
}