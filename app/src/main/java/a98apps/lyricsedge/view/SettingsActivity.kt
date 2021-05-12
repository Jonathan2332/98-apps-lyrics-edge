package a98apps.lyricsedge.view

import a98apps.lyricsedge.R
import a98apps.lyricsedge.util.ThemeUtil
import a98apps.lyricsedge.view.fragments.HeaderFragment
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

private const val TITLE_TAG = "settingsActivityTitle"

class SettingsActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        ThemeUtil.setTheme(applicationContext)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if(savedInstanceState == null)
        {
            supportFragmentManager.beginTransaction().replace(R.id.settings, HeaderFragment()).commit()
        }
        else
        {
            title = savedInstanceState.getCharSequence(TITLE_TAG)
        }
        supportFragmentManager.addOnBackStackChangedListener {
            checkActionBarIcon()
        }

        checkActionBarIcon()
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, title)
    }

    override fun onSupportNavigateUp(): Boolean
    {
        if(supportFragmentManager.popBackStackImmediate())
        {
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean
    {
        // Instantiate the new Fragment
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(classLoader, pref.fragment).apply {
            arguments = args
            setTargetFragment(caller, 0)
        }
        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_up, R.anim.slide_in_up, R.anim.slide_out_up)
            .replace(R.id.settings, fragment)
            .addToBackStack(null)
            .commit()
        title = pref.title
        return true
    }

    private fun checkActionBarIcon()
    {
        if(supportFragmentManager.backStackEntryCount == 0)
        {
            setTitle(R.string.title_activity_settings)
            val d: Drawable = InsetDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_baseline_settings_24), 10, 0, 10, 0)
            val actionBar = supportActionBar
            if(actionBar != null)
            {
                actionBar.setDisplayHomeAsUpEnabled(false)
                actionBar.setLogo(d)
                actionBar.setDisplayShowHomeEnabled(true)
                actionBar.setDisplayUseLogoEnabled(true)
            }
        }
        else
        {
            val actionBar = supportActionBar
            if(actionBar != null)
            {
                actionBar.setDisplayHomeAsUpEnabled(true)
                actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_keyboard_arrow_down_24)
                actionBar.setDisplayShowHomeEnabled(true)
                actionBar.setDisplayUseLogoEnabled(false)
            }
        }
    }
}