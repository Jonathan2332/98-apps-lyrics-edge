package a98apps.lyricsedge.view

import a98apps.lyricsedge.R
import a98apps.lyricsedge.constants.Actions
import a98apps.lyricsedge.constants.Constants
import a98apps.lyricsedge.notification.NotificationListener
import a98apps.lyricsedge.preferences.SecurityPreferences
import a98apps.lyricsedge.util.ThemeUtil
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.Button
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat

class SetupActivity : AppCompatActivity()
{
    private val mHandler = Handler(Looper.getMainLooper())
    private val mRunnable: Runnable by lazy {
        Runnable {
            if(NotificationManagerCompat.getEnabledListenerPackages(applicationContext).contains(packageName))
            {
                mHandler.removeCallbacks(mRunnable)
                val prefs = SecurityPreferences(applicationContext)
                prefs.setDefault(Constants.TEMP_HASH)
                prefs.setDefault(Constants.BLOCKED_HASH)
                startService(Intent(this, NotificationListener::class.java).setAction(Actions.ACTION_LISTENER_PERMISSION_GRANTED))
            }
            else
            {
                mHandler.postDelayed(mRunnable, 500)
            }
        }
    }
    private var flagClicked = false
    private lateinit var buttonPermission: Button
    private lateinit var titleCompleteSetup: AppCompatTextView

    override fun onCreate(savedInstanceState: Bundle?)
    {
        ThemeUtil.setTheme(applicationContext)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        val d: Drawable = InsetDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_baseline_settings_24), 10, 0, 10, 0)
        val actionBar = supportActionBar
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(false)
            actionBar.setLogo(d)
            actionBar.setDisplayShowHomeEnabled(true)
            actionBar.setDisplayUseLogoEnabled(true)
        }


        val lyricsProvider = findViewById<TextView>(R.id.lyricsProvider)
        //set link clickable
        lyricsProvider.movementMethod = LinkMovementMethod.getInstance()

        buttonPermission = findViewById(R.id.buttonPermission)
        titleCompleteSetup = findViewById(R.id.title_complete_setup)
        buttonPermission.setOnClickListener {
            flagClicked = true
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)

            mHandler.removeCallbacks(mRunnable)
            mHandler.post(mRunnable)
        }
    }

    override fun onResume()
    {
        super.onResume()
        if(!flagClicked)
        {
            mHandler.removeCallbacks(mRunnable)
        }

        checkButton()
        checkSetup()
    }

    override fun onStop()
    {
        super.onStop()
        if(flagClicked)
        {
            flagClicked = false
        }
    }

    override fun onDestroy()
    {
        super.onDestroy()
        mHandler.removeCallbacks(mRunnable)
    }

    private fun checkButton()
    {
        if(NotificationManagerCompat.getEnabledListenerPackages(applicationContext).contains(packageName))
        {
            buttonPermission.text = getString(R.string.text_permission_granted)
            buttonPermission.isEnabled = false
        }
        else
        {
            buttonPermission.text = getString(R.string.text_request_permission)
            buttonPermission.isEnabled = true
        }
    }

    private fun checkSetup()
    {
        if(NotificationManagerCompat.getEnabledListenerPackages(applicationContext).contains(packageName))
        {
            titleCompleteSetup.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_check_circle_24, 0, 0, 0)
            TextViewCompat.setCompoundDrawableTintList(titleCompleteSetup, ColorStateList.valueOf(0xFF1DD42E.toInt()))
        }
        else
        {
            titleCompleteSetup.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_settings_24, 0, 0, 0)
            TextViewCompat.setCompoundDrawableTintList(titleCompleteSetup, ColorStateList.valueOf(getColorFromAttr(R.attr.colorOnSurface)))
        }
    }

    @ColorInt
    fun Context.getColorFromAttr(@AttrRes attrColor: Int, typedValue: TypedValue = TypedValue(), resolveRefs: Boolean = true): Int
    {
        theme.resolveAttribute(attrColor, typedValue, resolveRefs)
        return typedValue.data
    }
}