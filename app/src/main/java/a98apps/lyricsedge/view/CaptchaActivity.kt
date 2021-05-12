package a98apps.lyricsedge.view

import a98apps.lyricsedge.R
import a98apps.lyricsedge.constants.Actions
import a98apps.lyricsedge.constants.Constants
import a98apps.lyricsedge.edge.Cocktail
import a98apps.lyricsedge.network.ILyricListener
import a98apps.lyricsedge.network.LyricsRequest
import a98apps.lyricsedge.notification.NotificationListener
import a98apps.lyricsedge.preferences.SecurityPreferences
import a98apps.lyricsedge.util.ThemeUtil
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso


class CaptchaActivity : AppCompatActivity(), ILyricListener
{
    private lateinit var groupCaptchaTextImage: Group
    private lateinit var groupCaptchaInputSubmit: Group
    private lateinit var buttonReloadCaptcha: Button
    private lateinit var buttonSubmitCaptcha: Button
    private lateinit var textCaptcha: TextView
    private lateinit var captchaError: TextView
    private lateinit var captchaLoader: ProgressBar

    private lateinit var lyricsRequest: LyricsRequest

    override fun onCreate(savedInstanceState: Bundle?)
    {
        ThemeUtil.setTheme(applicationContext)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_captcha)

        lyricsRequest = LyricsRequest(applicationContext, this)

        //to support RTL, add inset on Left and Right
        val d: Drawable = InsetDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_baseline_security_24), 10, 0, 10, 0)
        val actionBar = supportActionBar
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(false)
            actionBar.setLogo(d)
            actionBar.setDisplayShowHomeEnabled(true)
            actionBar.setDisplayUseLogoEnabled(true)
        }

        groupCaptchaTextImage = findViewById(R.id.groupCaptchaTextImage)
        groupCaptchaInputSubmit = findViewById(R.id.groupInputSubmit)
        captchaLoader = findViewById(R.id.captchaLoader)
        buttonReloadCaptcha = findViewById(R.id.reloadCaptcha)
        buttonSubmitCaptcha = findViewById(R.id.submitCaptcha)
        textCaptcha = findViewById(R.id.textCaptcha)
        captchaError = findViewById(R.id.captchaError)

        val captchaImage = findViewById<ImageView>(R.id.captchaImage)
        val inputCaptcha = findViewById<EditText>(R.id.inputCaptcha)

        buttonSubmitCaptcha.isEnabled = inputCaptcha.text.length >= 4

        val captchaUrl: String? = intent?.extras?.getString("captcha_url")

        val captchaSuccess: Int? = intent?.extras?.getInt("captcha_success")

        if(captchaSuccess != null)
        {
            captchaError.visibility = if(captchaSuccess == Constants.CAPTCHA_ERROR) View.VISIBLE else View.GONE
        }

        if(captchaUrl != null && captchaUrl != Constants.DEFAULT_VALUE_CAPTCHA)
        {
            inputCaptcha.addTextChangedListener(object : TextWatcher
            {
                override fun afterTextChanged(arg0: Editable)
                {
                    val input = inputCaptcha.text
                    buttonSubmitCaptcha.isEnabled = input.length >= 4
                }

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int)
                {

                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int)
                {
                }
            })

            buttonReloadCaptcha.setOnClickListener {
                loadURLImage(captchaUrl, captchaImage)
            }

            buttonSubmitCaptcha.setOnClickListener {
                buttonSubmitCaptcha.isEnabled = false

                val prefs = SecurityPreferences(applicationContext)

                val captchaSerial = prefs.get(Constants.CAPTCHA_SERIAL) as String

                val captchaTitleTarget = prefs.get(Constants.CAPTCHA_TITLE_TARGET) as String
                val captchaNameTarget = prefs.get(Constants.CAPTCHA_NAME_TARGET) as String

                val hashTarget: String = captchaTitleTarget + captchaNameTarget
                val hash = Base64.encodeToString(hashTarget.encodeToByteArray(), Base64.DEFAULT)

                lyricsRequest.fetchLyricsCaptcha(captchaSerial, inputCaptcha.text.toString(), hash, hash, captchaTitleTarget, captchaNameTarget)
            }

            loadURLImage(captchaUrl, captchaImage)
        }
        else
        {
            loadURLImage("", captchaImage)
        }
    }

    private fun loadURLImage(url: String, captchaImage: ImageView)
    {
        buttonReloadCaptcha.visibility = View.GONE
        groupCaptchaInputSubmit.visibility = View.GONE
        groupCaptchaTextImage.visibility = View.GONE
        captchaLoader.visibility = View.VISIBLE

        if(!TextUtils.isEmpty(url))
        {
            Picasso.get().load(url).error(R.drawable.ic_baseline_error_outline_128).into(captchaImage, object : Callback
            {
                override fun onSuccess()
                {
                    startPostponedEnterTransition()
                    buttonReloadCaptcha.visibility = View.GONE
                    groupCaptchaInputSubmit.visibility = View.VISIBLE
                    groupCaptchaTextImage.visibility = View.VISIBLE
                    textCaptcha.text = getString(R.string.text_success_captcha)
                    captchaLoader.visibility = View.GONE
                }

                override fun onError(e: Exception?)
                {
                    startPostponedEnterTransition()
                    buttonReloadCaptcha.visibility = View.VISIBLE
                    groupCaptchaInputSubmit.visibility = View.GONE
                    groupCaptchaTextImage.visibility = View.VISIBLE
                    textCaptcha.text = getString(R.string.text_error_captcha)
                    captchaLoader.visibility = View.GONE
                }
            })
        }
        else
        {
            buttonReloadCaptcha.visibility = View.GONE
            groupCaptchaInputSubmit.visibility = View.GONE
            groupCaptchaTextImage.visibility = View.VISIBLE
            textCaptcha.text = getString(R.string.text_invalid_captcha)
            captchaLoader.visibility = View.GONE
        }
    }

    override fun onFetchStarted(context: Context)
    {
        Cocktail.updatePanel(context, false)
    }

    override fun onFetchFinished(context: Context, success: Boolean)
    {
        val prefs = SecurityPreferences(applicationContext)
        if(success)
        {
            prefs.setDefault(Constants.TRANSLATE_TOGGLE)
            prefs.setDefault(Constants.CAPTCHA_PENDING)
            prefs.setDefault(Constants.CAPTCHA_SERIAL)
            prefs.setDefault(Constants.CAPTCHA_VERIFICATION)
            prefs.setDefault(Constants.CAPTCHA_TITLE_TARGET)
            prefs.setDefault(Constants.CAPTCHA_NAME_TARGET)
            Cocktail.updateList(context)
            Cocktail.updatePanel(context, true)
            startService(Intent(context, NotificationListener::class.java).setAction(Actions.ACTION_CAPTCHA_VALIDATED))
            finish()
        }
        else
        {
            Cocktail.updateList(context)
            Cocktail.updatePanel(context, false)
            val captchaUrl = prefs.get(Constants.CAPTCHA_VERIFICATION) as String
            val intentCaptcha = Intent(context, CaptchaActivity::class.java)
            intentCaptcha.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intentCaptcha.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intentCaptcha.putExtra("captcha_success", Constants.CAPTCHA_ERROR)
            intentCaptcha.putExtra("captcha_url", captchaUrl)
            startActivity(intentCaptcha)
        }
    }
}