package a98apps.lyricsedge.network

import a98apps.lyricsedge.BuildConfig
import a98apps.lyricsedge.constants.Constants
import android.net.Uri
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Callable


class LyricTask(private val serial: String?, private val udig: String?, private val art: String, private val mus: String, private val approxLyric: Boolean) : Callable<LyricsResult>
{
    override fun call(): LyricsResult?
    {
        var bufferedReader: BufferedReader? = null
        var conn: HttpURLConnection? = null
        try
        {
            val url: URL = if(serial != Constants.DEFAULT_VALUE_CAPTCHA && udig != Constants.DEFAULT_VALUE_CAPTCHA)
            {
                URL(BuildConfig.API_BASE_URL +
                        BuildConfig.API_ART_PARAMTER + Uri.encode(art) +
                        BuildConfig.API_MUS_PARAMTER + Uri.encode(mus) +
                        BuildConfig.API_SERIAL_PARAMTER + Uri.encode(serial) +
                        BuildConfig.API_UDIG_PARAMTER + Uri.encode(udig) +
                        BuildConfig.API_KEY_PARAMTER + BuildConfig.API_KEY)
            }
            else
            {
                Thread.sleep(400)//Delay to prevent flood and multiple unnecessary calls
                URL(BuildConfig.API_BASE_URL +
                        BuildConfig.API_ART_PARAMTER + Uri.encode(art) +
                        BuildConfig.API_MUS_PARAMTER + Uri.encode(mus) +
                        BuildConfig.API_KEY_PARAMTER + BuildConfig.API_KEY)
            }

            if(!Thread.currentThread().isInterrupted)
            {
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 60000
                conn.readTimeout = 60000
            }

            if(!Thread.currentThread().isInterrupted && conn != null)
            {
                bufferedReader = BufferedReader(InputStreamReader(conn.inputStream))

                var output: String?
                val response = StringBuilder()
                while(bufferedReader.readLine().also { output = it } != null)
                {
                    if(!Thread.currentThread().isInterrupted) response.append(output)
                }

                val jsonObject = JSONObject(response.toString())
                if(jsonObject.length() != 0)
                {
                    return LyricsResult(jsonObject, art, mus, approxLyric)
                }
            }

        }
        catch(e: Exception)
        {
            conn?.inputStream?.close()
            conn?.disconnect()
        }
        finally
        {
            conn?.inputStream?.close()
            conn?.disconnect()
            if(bufferedReader != null)
            {
                try
                {
                    bufferedReader.close()
                }
                catch(ignore: IOException)
                {

                }
            }
        }
        return null
    }
}