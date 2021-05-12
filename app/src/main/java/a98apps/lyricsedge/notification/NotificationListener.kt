package a98apps.lyricsedge.notification

import a98apps.lyricsedge.constants.Actions
import a98apps.lyricsedge.edge.Cocktail
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils


class NotificationListener : NotificationListenerService()
{
    private lateinit var mNotificationReceiver: NotificationReceiver

    override fun onCreate()
    {
        super.onCreate()

        mNotificationReceiver = NotificationReceiver(applicationContext)

        val intentFilter = IntentFilter()
        intentFilter.addAction(Actions.ACTION_PULL_TO_REFRESH)

        registerReceiver(mNotificationReceiver, intentFilter)
    }

    override fun onDestroy()
    {
        super.onDestroy()
        unregisterReceiver(mNotificationReceiver)
    }

    override fun onListenerConnected()
    {
        super.onListenerConnected()
        Cocktail.updateList(applicationContext)
        Cocktail.updatePanel(applicationContext, false)

        //on permission granted, check current notifications
        activeNotifications.forEach {
            onNotificationPosted(it)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        if(intent != null)
        {
            val action = intent.action
            if(action != null)
            {
                when(action)
                {
                    //Actions.ACTION_RESET_LISTENER = on reset listener, check notifications again
                    //Actions.ACTION_CHANGED_SETTINGS = on lyric approximate or long track setting changed, check notifications again
                    //Actions.ACTION_CAPTCHA_VALIDATED = on captcha validated, check notifications again
                    Actions.ACTION_RESET_LISTENER, Actions.ACTION_CHANGED_SETTINGS, Actions.ACTION_CAPTCHA_VALIDATED ->
                    {
                        activeNotifications.forEach {
                            onNotificationPosted(it)
                        }
                    }
                    Actions.ACTION_LISTENER_PERMISSION_GRANTED ->
                    {
                        Cocktail.updateList(applicationContext)
                        Cocktail.updatePanel(applicationContext, false)//update panel to enable pull to refresh

                        //on permission granted, check current notifications
                        activeNotifications.forEach {
                            onNotificationPosted(it)
                        }
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder?
    {
        return super.onBind(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?)
    {
        super.onNotificationPosted(sbn)
        if(sbn != null && Cocktail.isEnabled(applicationContext))
        {
            val packageName = sbn.packageName
            if(packageName != null && !TextUtils.isEmpty(packageName))
            {
                val service = getSystemService(Context.MEDIA_SESSION_SERVICE)
                if(service != null)
                {
                    val mediaSessionManager = service as MediaSessionManager
                    val controllers: List<MediaController> = mediaSessionManager.getActiveSessions(ComponentName(this, NotificationListener::class.java))
                    if(controllers.isNotEmpty())
                    {
                        for(controller in controllers)
                        {
                            mNotificationReceiver.handleMediaController(applicationContext, packageName, controller, sbn)
                        }
                    }
                    else
                    {
                        mNotificationReceiver.handleMediaSession(applicationContext, packageName, sbn.notification.extras.get("android.mediaSession"), sbn)
                    }
                }
            }
        }
    }
}