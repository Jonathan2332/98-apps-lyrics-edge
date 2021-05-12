package a98apps.lyricsedge.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class SecurityPreferences(context: Context) : DefaultPreferences(context)
{
    private val mSharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun get(key: String): Any?
    {
        val defaultValue: Any?
        if(mSharedPreferences.contains(key))
        {
            return mSharedPreferences.all[key]
        }
        else
        {
            defaultValue = getDefaultByKey(key)
            set(key, defaultValue)
        }
        return defaultValue
    }

    fun set(key: String, value: Any?)
    {
        when(value)
        {
            is Boolean ->
            {
                mSharedPreferences.edit().putBoolean(key, value).apply()
            }
            is Int ->
            {
                mSharedPreferences.edit().putInt(key, value).apply()
            }
            is Long ->
            {
                mSharedPreferences.edit().putLong(key, value).apply()
            }
            is Float ->
            {
                mSharedPreferences.edit().putFloat(key, value).apply()
            }
            is String? ->
            {
                mSharedPreferences.edit().putString(key, value).apply()
            }
        }
    }

    fun setDefault(key: String)
    {
        val defaultValue = getDefaultByKey(key)
        set(key, defaultValue)
    }
}