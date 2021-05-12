package a98apps.lyricsedge.notification

import android.app.Notification
import android.content.Context
import android.media.MediaMetadata
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import java.util.concurrent.TimeUnit

class NotificationInfo(val context: Context, private val mediaMetadata: MediaMetadata?, private val sbn: StatusBarNotification?)
{
    fun geTitle(): String?
    {
        return getByMetadata(MediaMetadata.METADATA_KEY_TITLE) ?: getBySbn(Notification.EXTRA_TITLE)
    }

    fun getArtist(): String?
    {
        return getByMetadata(MediaMetadata.METADATA_KEY_ARTIST) ?: getBySbn(Notification.EXTRA_TEXT)
    }

    private fun getByMetadata(metadata: String): String?
    {
        if(mediaMetadata != null)
        {
            val result = mediaMetadata.getString(metadata)
            if(result != null && !TextUtils.isEmpty(result))
            {
                return result
            }
        }
        return null
    }

    private fun getBySbn(extra: String): String?
    {
        if(sbn != null)
        {
            val result = sbn.notification.extras.getCharSequence(extra, "")
            if(!TextUtils.isEmpty(result))
            {
                return result.toString()
            }
        }
        return null
    }

    fun getDuration(): Int
    {
        return if(mediaMetadata != null) TimeUnit.MILLISECONDS.toMinutes(mediaMetadata.getLong(MediaMetadata.METADATA_KEY_DURATION)).toInt() else -1
    }
}