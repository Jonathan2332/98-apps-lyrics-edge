package a98apps.lyricsedge.notification

import a98apps.lyricsedge.constants.Actions
import a98apps.lyricsedge.constants.Constants
import a98apps.lyricsedge.edge.Cocktail
import a98apps.lyricsedge.helper.dao.cache.CacheDAO
import a98apps.lyricsedge.network.ILyricListener
import a98apps.lyricsedge.network.LyricsRequest
import a98apps.lyricsedge.preferences.SecurityPreferences
import a98apps.lyricsedge.util.NetworkUtil
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.util.Base64

class NotificationReceiver(context: Context) : BroadcastReceiver(), ILyricListener
{
    private val mLyricsRequest: LyricsRequest = LyricsRequest(context, this)
    private val mCallbacks = HashMap<String, ControllerCallback>()

    override fun onReceive(context: Context?, intent: Intent?)
    {
        if(intent != null && context != null)
        {
            val extras = intent.extras
            if(extras != null)
            {
                //handle actions
                val action = intent.action
                if(action != null)
                {
                    if(action == Actions.ACTION_PULL_TO_REFRESH)
                    {
                        val musicId = extras.getLong("music_id")
                        val artistId = extras.getLong("artist_id")
                        val hash = extras.getString("hash") ?: ""
                        val artist = extras.getString("artist_name") ?: ""
                        val title = extras.getString("music_name") ?: ""
                        val sourceArtistHash = extras.getString("source_artist_hash") ?: ""
                        val sourceMusicHash = extras.getString("source_music_hash") ?: ""

                        if(!TextUtils.isEmpty(hash) && !TextUtils.isEmpty(artist) && !TextUtils.isEmpty(title))
                        {
                            mLyricsRequest.fetchExistingLyrics(hash, hash, artist, sourceArtistHash, title, sourceMusicHash, artistId, musicId)
                        }
                    }
                }
            }
        }
    }

    fun handleMediaSession(context: Context, packageName: String, mediaSession: Any?, sbn: StatusBarNotification?)
    {
        if(mediaSession != null)
        {
            val token = mediaSession as MediaSession.Token
            val mediaController = MediaController(context, token)

            if(!mCallbacks.containsKey(packageName))
            {
                val callback = ControllerCallback(context, packageName, mediaController, sbn)
                mCallbacks[packageName] = callback
                mediaController.registerCallback(callback)
                callback.onPlaybackStateChanged(mediaController.playbackState)
            }
            else
            {
                val callback = mCallbacks[packageName]
                if(callback != null)//update mediaController
                {
                    callback.mediaController = mediaController
                    callback.onPlaybackStateChanged(mediaController.playbackState)
                }
            }
        }
    }

    fun handleMediaController(context: Context, packageName: String, mediaController: MediaController, sbn: StatusBarNotification?)
    {
        if(!mCallbacks.containsKey(packageName))
        {
            val callback = ControllerCallback(context, packageName, mediaController, sbn)
            mCallbacks[packageName] = callback
            mediaController.registerCallback(callback)
            callback.onPlaybackStateChanged(mediaController.playbackState)
        }
        else
        {
            val callback = mCallbacks[packageName]
            if(callback != null)//update mediaController
            {
                callback.mediaController = mediaController
                callback.onPlaybackStateChanged(mediaController.playbackState)
            }
        }
    }

    override fun onFetchStarted(context: Context)
    {
        Cocktail.updatePanel(context, false)
    }

    override fun onFetchFinished(context: Context, success: Boolean)
    {
        Cocktail.updatePanel(context, success)
    }

    private inner class ControllerCallback(val context: Context, val packageName: String, var mediaController: MediaController, val sbn: StatusBarNotification?) : MediaController.Callback()
    {
        override fun onSessionDestroyed()
        {
            super.onSessionDestroyed()
            if(mCallbacks.containsKey(packageName))
            {
                mediaController.unregisterCallback(this)
                mCallbacks.remove(packageName)
            }
        }

        override fun onPlaybackStateChanged(playbackState: PlaybackState?)
        {
            super.onPlaybackStateChanged(playbackState)
            if(playbackState != null && playbackState.state == PlaybackState.STATE_PLAYING)
            {
                //metadata can be null, if null the NotificationInfo uses the StatusBarNotification to get artist and music title
                val metadata = mediaController.metadata
                checkLyric(context, NotificationInfo(context, metadata, sbn))
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadata?)
        {
            super.onMetadataChanged(metadata)

            val playbackState = mediaController.playbackState
            if(playbackState != null && playbackState.state == PlaybackState.STATE_PLAYING)
            {
                //metadata can be null, if null the NotificationInfo uses the StatusBarNotification to get artist and music title
                checkLyric(context, NotificationInfo(context, metadata, sbn))
            }
        }
    }

    private fun checkLyric(context: Context, notificationInfo: NotificationInfo)
    {
        val artist: String? = notificationInfo.getArtist()
        val title: String? = notificationInfo.geTitle()

        if(artist != null && title != null)
        {
            val prefs = SecurityPreferences(context)

            val captchaPending = prefs.get(Constants.CAPTCHA_PENDING) as Boolean
            if(captchaPending) return

            val ignoreLongTracks = prefs.get(Constants.IGNORE_LONG_TRACK) as Boolean
            if(ignoreLongTracks && notificationInfo.getDuration() > 20) return

            val sourceArtistHash: String = Base64.encodeToString(artist.encodeToByteArray(), Base64.DEFAULT)
            val sourceTitleHash: String = Base64.encodeToString(title.encodeToByteArray(), Base64.DEFAULT)
            val hash = Base64.encodeToString((artist + title).encodeToByteArray(), Base64.DEFAULT)

            //If a hash has been blocked, it means that the lyric was not found and prevent the lyric from being searched again
            val blockedHash = prefs.get(Constants.BLOCKED_HASH)
            if(blockedHash != null && blockedHash == hash) return
            else if(blockedHash != null && blockedHash != hash) prefs.setDefault(Constants.BLOCKED_HASH)

            val cacheDAO = CacheDAO(context)

            val cacheId = cacheDAO.has(hash)

            if(cacheId == -1L)//not in cache
            {
                if(NetworkUtil.isNetworkAvailable(context))
                {
                    val tempHash = prefs.get(Constants.TEMP_HASH) as String
                    if(hash != tempHash)
                    {
                        prefs.set(Constants.TEMP_HASH, hash)

                        mLyricsRequest.fetchLyrics(tempHash, hash, artist, sourceArtistHash, title, sourceTitleHash)
                    }
                }
            }
            else
            {
                val cacheIndex = prefs.get(Constants.CACHE_INDEX)

                if(cacheId != cacheIndex)
                {
                    prefs.set(Constants.CACHE_INDEX, cacheId)
                    prefs.setDefault(Constants.TRANSLATE_TOGGLE)

                    //update panel list
                    Cocktail.updateList(context)

                    onFetchFinished(context, true)
                }
            }
        }
    }
}