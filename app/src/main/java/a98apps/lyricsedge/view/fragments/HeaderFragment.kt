package a98apps.lyricsedge.view.fragments

import a98apps.lyricsedge.BuildConfig
import a98apps.lyricsedge.R
import a98apps.lyricsedge.constants.Actions
import a98apps.lyricsedge.constants.Constants
import a98apps.lyricsedge.constants.Constants.APP_VERSION
import a98apps.lyricsedge.constants.Constants.CLEAR_CACHE
import a98apps.lyricsedge.constants.Constants.COFFEE
import a98apps.lyricsedge.edge.Cocktail
import a98apps.lyricsedge.helper.DbHelper
import a98apps.lyricsedge.helper.dao.artist.ArtistDAO
import a98apps.lyricsedge.helper.dao.cache.CacheDAO
import a98apps.lyricsedge.helper.dao.music.MusicDAO
import a98apps.lyricsedge.notification.NotificationListener
import a98apps.lyricsedge.preferences.SecurityPreferences
import a98apps.lyricsedge.util.FormatterUtil
import a98apps.lyricsedge.util.ThemeUtil
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager

class HeaderFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener
{
    private lateinit var mSharedPreferences: SharedPreferences
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)
    {
        setPreferencesFromResource(R.xml.header_preferences, rootKey)

        this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val resetCache: Preference? = findPreference(CLEAR_CACHE)
        resetCache?.let {
            resetCache.setOnPreferenceClickListener {
                val snackBar = Snackbar.make(requireView(), getString(R.string.text_are_you_sure), Snackbar.LENGTH_LONG)
                snackBar.setAction(getString(android.R.string.ok)) {
                    resetCache()
                }
                snackBar.show()
                true
            }
        }

        val version: Preference? = findPreference(APP_VERSION)
        version?.let {
            val labelVersion: String = FormatterUtil.formatSpaceText(getString(R.string.text_version), BuildConfig.VERSION_NAME)
            version.summary = labelVersion
        }

        val coffee: Preference? = findPreference(COFFEE)
        coffee?.let {
            it.setOnPreferenceClickListener {

                val alertDialog: AlertDialog = requireActivity().let {
                    //configure builder
                    val builder = AlertDialog.Builder(requireActivity())
                    builder.apply {
                        setPositiveButton(android.R.string.ok) { _, _ ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/donate?hosted_button_id=GMFA26U2YCTJ4"))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }
                        setTitle(R.string.title_donate)
                        setMessage(R.string.message_coffee)
                        setIcon(R.drawable.ic_baseline_coffee_24)
                    }
                    builder.create()
                }

                if(!isRemoving) alertDialog.show()

                true
            }
        }
    }

    override fun onResume()
    {
        super.onResume()
        this.mSharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause()
    {
        super.onPause()
        this.mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?)
    {
        when(key)
        {
            Constants.APP_THEME -> ThemeUtil.setTheme(requireContext())
            Constants.PANEL_COLOR ->
            {
                val cocktailManager = SlookCocktailManager.getInstance(context)
                val cocktailIds = cocktailManager.getCocktailIds(ComponentName(requireContext(), Cocktail::class.java))

                val cocktail = Cocktail()
                cocktail.onUpdate(requireContext(), cocktailManager, cocktailIds)
            }
            Constants.DIVIDER_COLOR, Constants.TEXT_COLOR, Constants.TITLE_COLOR, Constants.TEXT_SIZE ->
            {
                val cocktailManager = SlookCocktailManager.getInstance(context)
                val cocktailIds = cocktailManager.getCocktailIds(ComponentName(requireContext(), Cocktail::class.java))
                for(id in cocktailIds)
                {
                    cocktailManager.notifyCocktailViewDataChanged(id, R.id.remote_list)
                }
            }
            Constants.ENABLE_TRANSLATION ->
            {
                val prefs = SecurityPreferences(requireContext())
                prefs.setDefault(Constants.TRANSLATE_TOGGLE)
                Cocktail.updateList(requireContext())
                Cocktail.updatePanel(requireContext(), false)
            }
            Constants.IGNORE_LONG_TRACK ->
            {
                val prefs = SecurityPreferences(requireContext())
                if(prefs.get(Constants.IGNORE_LONG_TRACK) as Boolean)
                {
                    requireContext().startService(Intent(requireContext(), NotificationListener::class.java).setAction(Actions.ACTION_CHANGED_SETTINGS))
                }
            }
            Constants.LYRICS_APPROX ->
            {
                val prefs = SecurityPreferences(requireContext())
                if(prefs.get(Constants.LYRICS_APPROX) as Boolean)
                {
                    prefs.setDefault(Constants.TEMP_HASH)
                    prefs.setDefault(Constants.BLOCKED_HASH)

                    requireContext().startService(Intent(requireContext(), NotificationListener::class.java).setAction(Actions.ACTION_CHANGED_SETTINGS))
                }
            }
        }
    }

    private fun resetCache()
    {
        Toast.makeText(requireContext(), getString(R.string.text_cache_cleared), Toast.LENGTH_SHORT).show()

        requireContext().stopService(Intent(requireContext(), NotificationListener::class.java))

        val prefs = SecurityPreferences(requireContext())

        //set temporary disabled flag to notification receiver not make changes in DB
        prefs.set(Constants.ENABLED_PANEL, false)

        val artistDAO = ArtistDAO(requireContext())
        artistDAO.drop()

        val lyricDAO = MusicDAO(requireContext())
        lyricDAO.drop()

        val cacheDAO = CacheDAO(requireContext())
        cacheDAO.drop()

        //recreate db
        val dbHelper = DbHelper(requireContext())
        dbHelper.onCreate(dbHelper.writableDatabase)

        //reset indexes controllers
        prefs.setDefault(Constants.CACHE_INDEX)
        prefs.setDefault(Constants.TEMP_HASH)
        prefs.setDefault(Constants.BLOCKED_HASH)

        //reset translation
        prefs.setDefault(Constants.TRANSLATE_TOGGLE)
        prefs.setDefault(Constants.AVAILABLE_TRANSLATE)

        //Alright, can be changes in DB
        prefs.set(Constants.ENABLED_PANEL, true)

        Cocktail.updateList(requireContext())
        Cocktail.updatePanel(requireContext(), false)

        requireContext().startService(Intent(requireContext(), NotificationListener::class.java).setAction(Actions.ACTION_RESET_LISTENER))
    }
}