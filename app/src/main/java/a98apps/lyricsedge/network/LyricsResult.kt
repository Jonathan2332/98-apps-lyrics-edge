package a98apps.lyricsedge.network

import a98apps.lyricsedge.constants.Api
import org.json.JSONArray
import org.json.JSONObject

class LyricsResult(jsonObject: JSONObject, artist: String, music: String, approx: Boolean)
{
    val type: String = jsonObject.getString(Api.TYPE) ?: Api.RESULT_TYPE_UNKNOWN

    var validSave: Boolean = false

    var hasCaptcha: Boolean = false
    var captchaUrl: String? = null
    var captchaSerial: String? = null

    lateinit var artistName: String
    lateinit var artistUrl: String

    lateinit var musicName: String
    lateinit var lyric: String
    lateinit var lyricUrl: String
    lateinit var lyricTranslate: String

    init
    {
        if(jsonObject.has(Api.CAPTCHA) && jsonObject.getBoolean(Api.CAPTCHA))
        {
            validSave = false
            hasCaptcha = true
            captchaSerial = jsonObject.getString(Api.CAPTCHA_SERIAL)
            captchaUrl = jsonObject.getString(Api.CAPTCHA_IMG)
        }
        else if(type != Api.RESULT_TYPE_SONG_NOT_FOUND && type != Api.RESULT_TYPE_NOT_FOUND && type != Api.RESULT_TYPE_UNKNOWN && approx)
        {
            if(jsonObject.has(Api.RESULT_ARTIST) && jsonObject.has(Api.RESULT_MUSIC))
            {
                validSave = true

                val objectArt: JSONObject = jsonObject.getJSONObject(Api.RESULT_ARTIST)

                artistName = objectArt.getString(Api.RESULT_NAME) ?: artist
                artistUrl = objectArt.getString(Api.RESULT_URL) ?: ""

                val arrayMus: JSONArray = jsonObject.getJSONArray(Api.RESULT_MUSIC)
                val objectMus = arrayMus.getJSONObject(0)

                musicName = objectMus.getString(Api.RESULT_NAME) ?: music
                lyric = objectMus.getString(Api.RESULT_LYRIC) ?: ""
                lyricUrl = objectMus.getString(Api.RESULT_URL) ?: ""

                if(objectMus.has(Api.RESULT_TRANSLATE))
                {
                    val arrayTranslate: JSONArray = objectMus.getJSONArray(Api.RESULT_TRANSLATE)
                    for(i in 0 until arrayTranslate.length())
                    {
                        val jsonTranslate = arrayTranslate.getJSONObject(i)
                        if(jsonTranslate.get(Api.RESULT_TRANSLATE_LANG) == Api.TRANSLATE_LANG_PT_BR)
                        {
                            lyricTranslate = jsonTranslate.getString(Api.RESULT_LYRIC) ?: ""
                            break
                        }
                    }
                }

                //not found translate
                if(!::lyricTranslate.isInitialized)
                {
                    lyricTranslate = ""
                }
            }
        }
        else
        {
            validSave = false
        }
    }
}