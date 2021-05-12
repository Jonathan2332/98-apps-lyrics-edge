package a98apps.lyricsedge.network

import android.content.Context

interface ILyricListener
{
    fun onFetchStarted(context: Context)
    fun onFetchFinished(context: Context, success: Boolean)
}