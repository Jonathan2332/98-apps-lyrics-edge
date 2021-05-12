package a98apps.lyricsedge.network

import a98apps.lyricsedge.constants.Constants
import a98apps.lyricsedge.edge.Cocktail
import a98apps.lyricsedge.helper.dao.artist.ArtistDAO
import a98apps.lyricsedge.helper.dao.cache.CacheDAO
import a98apps.lyricsedge.helper.dao.music.MusicDAO
import a98apps.lyricsedge.model.Artist
import a98apps.lyricsedge.model.Cache
import a98apps.lyricsedge.model.Music
import a98apps.lyricsedge.preferences.SecurityPreferences
import a98apps.lyricsedge.util.NetworkUtil
import android.content.Context
import android.util.Base64
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class LyricsRequest(private val context: Context, private val listener: ILyricListener)
{
    companion object
    {
        var isRunning: Boolean = false

        //manage queue
        private val hashMap = HashMap<String, Future<LyricsResult>>()
        private val jobMap = HashMap<String, Job>()
    }

    fun fetchLyrics(oldHash: String, hash: String, art: String, sourceArtistHash: String, title: String, sourceTitleHash: String)
    {
        if(jobMap.containsKey(oldHash))
        {
            jobMap[oldHash]?.cancel(null)
            jobMap.remove(oldHash)
        }

        val prefs = SecurityPreferences(context)
        val approxLyric = prefs.get(Constants.LYRICS_APPROX) as Boolean

        //checks whether the old hash is in the queue, if true, cancel and remove and add the new hash
        if(hashMap.containsKey(oldHash))
        {
            hashMap[oldHash]?.cancel(true)
            hashMap.remove(oldHash)

            val executor = Executors.newSingleThreadExecutor()
            hashMap[hash] = executor.submit(LyricTask(Constants.DEFAULT_VALUE_CAPTCHA, Constants.DEFAULT_VALUE_CAPTCHA, art, title, approxLyric))
            executor.shutdown()
        }
        else
        {
            val executor = Executors.newSingleThreadExecutor()
            hashMap[hash] = executor.submit(LyricTask(Constants.DEFAULT_VALUE_CAPTCHA, Constants.DEFAULT_VALUE_CAPTCHA, art, title, approxLyric))
            executor.shutdown()
        }

        jobMap[hash] = GlobalScope.launch(Dispatchers.IO) {
            delay(400)

            isRunning = true
            listener.onFetchStarted(context)

            try
            {
                val lyricsResult = hashMap[hash]?.get(1, TimeUnit.MINUTES)

                var success = false
                if(lyricsResult != null && lyricsResult.validSave)
                {
                    //insert artist on DB
                    var artistDAO = ArtistDAO(context)
                    val hashArtist = Base64.encodeToString(lyricsResult.artistName.encodeToByteArray(), Base64.DEFAULT)
                    var artistId = artistDAO.has(hashArtist)

                    if(artistId == -1L)//not in db
                    {
                        val artist = Artist()
                        artist.name = lyricsResult.artistName
                        artist.hash = Base64.encodeToString(lyricsResult.artistName.encodeToByteArray(), Base64.DEFAULT)
                        artist.sourceHash = sourceArtistHash
                        artist.url = lyricsResult.artistUrl

                        artistDAO = ArtistDAO(context)
                        artist.id = artistDAO.save(artist)
                        if(artist.id != -1L)//no errors on save
                        {
                            artistId = artist.id ?: -1L
                        }
                    }

                    if(artistId != -1L)//artist on DB
                    {
                        //insert lyric music on DB
                        val music = Music()
                        music.hash = Base64.encodeToString(lyricsResult.musicName.encodeToByteArray(), Base64.DEFAULT)
                        music.sourceHash = sourceTitleHash
                        music.name = lyricsResult.musicName
                        music.lyric = lyricsResult.lyric
                        music.url = lyricsResult.lyricUrl
                        music.translate = lyricsResult.lyricTranslate

                        var musicDAO = MusicDAO(context)
                        music.id = musicDAO.save(music)
                        if(music.id != -1L)//no errors on save
                        {
                            //insert on cache
                            val cacheDAO = CacheDAO(context)
                            val cache = Cache()
                            cache.musicId = music.id
                            cache.artistId = artistId
                            cache.hash = hash
                            cache.type = lyricsResult.type

                            cache.id = cacheDAO.save(cache)
                            if(cache.id != -1L)//no errors on save
                            {
                                success = true

                                //set lyric index to current founded lyric
                                prefs.set(Constants.CACHE_INDEX, cache.id)

                                //reset button
                                prefs.setDefault(Constants.TRANSLATE_TOGGLE)

                                //update panel list
                                Cocktail.updateList(context)
                            }
                            else
                            {
                                prefs.set(Constants.BLOCKED_HASH, hash)

                                //error on insert cache, remove music
                                musicDAO = MusicDAO(context)//instantiate again to reopen db
                                musicDAO.delete(artistId)

                                //error on insert cache, remove artist
                                artistDAO = ArtistDAO(context)//instantiate again to reopen db
                                artistDAO.delete(artistId)
                            }
                        }
                        else
                        {
                            prefs.set(Constants.BLOCKED_HASH, hash)

                            //error on insert music, remove artist
                            artistDAO = ArtistDAO(context)//instantiate again to reopen db
                            artistDAO.delete(artistId)
                        }
                    }
                    else//error on artist on DB
                    {
                        prefs.set(Constants.BLOCKED_HASH, hash)
                    }
                }
                else if(lyricsResult != null && lyricsResult.hasCaptcha)
                {
                    prefs.set(Constants.CAPTCHA_PENDING, true)
                    prefs.set(Constants.CAPTCHA_VERIFICATION, lyricsResult.captchaUrl)
                    prefs.set(Constants.CAPTCHA_SERIAL, lyricsResult.captchaSerial)

                    prefs.set(Constants.CAPTCHA_TITLE_TARGET, art)
                    prefs.set(Constants.CAPTCHA_NAME_TARGET, title)

                    //update panel list
                    Cocktail.updateList(context)
                }
                else
                {
                    /*
                    blocks the hash only if you have a network connection,
                    it means that the request was made but the letter was not found,
                    if the connection goes down during the request, the hash should not be blocked
                    TEMP_HASH is reset so that it can be searched again
                    */
                    if(NetworkUtil.isNetworkAvailable(context))
                        prefs.set(Constants.BLOCKED_HASH, hash)
                    else
                        prefs.setDefault(Constants.TEMP_HASH)
                }

                isRunning = false
                listener.onFetchFinished(context, success)
            }
            catch(t: TimeoutException)
            {
                //reset to search again
                prefs.setDefault(Constants.TEMP_HASH)
                isRunning = false
                listener.onFetchFinished(context, false)//update panel to hide loader
            }
            catch(e: Exception)//interrupted or cancelled
            {
                isRunning = false
                listener.onFetchFinished(context, false)//update panel to hide loader
            }
        }
    }

    fun fetchExistingLyrics(oldHash: String, hash: String, art: String, sourceArtistHash: String, title: String, sourceTitleHash: String, artistId: Long, musicId: Long)
    {
        if(jobMap.containsKey(oldHash))
        {
            jobMap[oldHash]?.cancel(null)
            jobMap.remove(oldHash)
        }

        val prefs = SecurityPreferences(context)
        val approxLyric = prefs.get(Constants.LYRICS_APPROX) as Boolean

        //checks whether the old hash is in the queue, if true, cancel and remove and add the new hash
        if(hashMap.containsKey(oldHash))
        {
            hashMap[oldHash]?.cancel(true)
            hashMap.remove(oldHash)

            val executor = Executors.newSingleThreadExecutor()
            hashMap[hash] = executor.submit(LyricTask(Constants.DEFAULT_VALUE_CAPTCHA, Constants.DEFAULT_VALUE_CAPTCHA, art, title, approxLyric))
            executor.shutdown()
        }
        else
        {
            val executor = Executors.newSingleThreadExecutor()
            hashMap[hash] = executor.submit(LyricTask(Constants.DEFAULT_VALUE_CAPTCHA, Constants.DEFAULT_VALUE_CAPTCHA, art, title, approxLyric))
            executor.shutdown()
        }

        jobMap[hash] = GlobalScope.launch(Dispatchers.IO) {
            delay(400)

            isRunning = true
            listener.onFetchStarted(context)

            try
            {
                val lyricsResult = hashMap[hash]?.get(1, TimeUnit.MINUTES)

                var success = false
                if(lyricsResult != null && lyricsResult.validSave)
                {
                    //update artist on DB
                    val artist = Artist()
                    artist.id = artistId
                    artist.hash = Base64.encodeToString(lyricsResult.artistName.encodeToByteArray(), Base64.DEFAULT)
                    artist.sourceHash = sourceArtistHash
                    artist.name = lyricsResult.artistName
                    artist.url = lyricsResult.artistUrl

                    val artistDAO = ArtistDAO(context)
                    if(artistDAO.update(artist))//no errors on update
                    {
                        //update lyric on DB
                        val music = Music()
                        music.id = musicId
                        music.hash = Base64.encodeToString(lyricsResult.musicName.encodeToByteArray(), Base64.DEFAULT)
                        music.sourceHash = sourceTitleHash
                        music.name = lyricsResult.musicName
                        music.lyric = lyricsResult.lyric
                        music.url = lyricsResult.lyricUrl
                        music.translate = lyricsResult.lyricTranslate

                        val musicDAO = MusicDAO(context)
                        if(musicDAO.update(music))//no errors on update
                        {
                            success = true

                            prefs.setDefault(Constants.TRANSLATE_TOGGLE)

                            //update panel list
                            Cocktail.updateList(context)
                        }
                    }
                }
                else if(lyricsResult != null && lyricsResult.hasCaptcha)
                {
                    prefs.set(Constants.CAPTCHA_PENDING, true)
                    prefs.set(Constants.CAPTCHA_VERIFICATION, lyricsResult.captchaUrl)
                    prefs.set(Constants.CAPTCHA_SERIAL, lyricsResult.captchaSerial)

                    prefs.set(Constants.CAPTCHA_TITLE_TARGET, art)
                    prefs.set(Constants.CAPTCHA_NAME_TARGET, title)

                    //update panel list
                    Cocktail.updateList(context)
                }
                else
                {
                    /*
                    blocks the hash only if you have a network connection,
                    it means that the request was made but the letter was not found,
                    if the connection goes down during the request, the hash should not be blocked
                    TEMP_HASH is reset so that it can be searched again
                    */
                    if(NetworkUtil.isNetworkAvailable(context))
                        prefs.set(Constants.BLOCKED_HASH, hash)
                    else
                        prefs.setDefault(Constants.TEMP_HASH)
                }

                isRunning = false
                listener.onFetchFinished(context, success)
            }
            catch(t: TimeoutException)
            {
                //reset to search again
                prefs.setDefault(Constants.TEMP_HASH)
                isRunning = false
                listener.onFetchFinished(context, false)//update panel to hide loader
            }
            catch(e: Exception)//interrupted or cancelled
            {
                isRunning = false
                listener.onFetchFinished(context, false)//update panel to hide loader
            }
        }
    }

    fun fetchLyricsCaptcha(serial: String?, udig: String?, oldHash: String, hash: String, art: String, mus: String)
    {
        if(jobMap.containsKey(oldHash))
        {
            jobMap[oldHash]?.cancel(null)
            jobMap.remove(oldHash)
        }

        val prefs = SecurityPreferences(context)
        val approxLyric = prefs.get(Constants.LYRICS_APPROX) as Boolean

        //checks whether the old hash is in the queue, if true, cancel and remove and add the new hash
        if(hashMap.containsKey(oldHash))
        {
            hashMap[oldHash]?.cancel(true)
            hashMap.remove(oldHash)

            val executor = Executors.newSingleThreadExecutor()
            hashMap[hash] = executor.submit(LyricTask(serial, udig, art, mus, approxLyric))
            executor.shutdown()
        }
        else
        {
            val executor = Executors.newSingleThreadExecutor()
            hashMap[hash] = executor.submit(LyricTask(serial, udig, art, mus, approxLyric))
            executor.shutdown()
        }

        jobMap[hash] = GlobalScope.launch(Dispatchers.IO) {

            isRunning = true
            listener.onFetchStarted(context)

            try
            {
                val lyricsResult = hashMap[hash]?.get(40, TimeUnit.SECONDS)

                var success = false
                if(lyricsResult != null && lyricsResult.validSave)
                {
                    success = true
                    prefs.setDefault(Constants.TRANSLATE_TOGGLE)
                }
                else if(lyricsResult != null && lyricsResult.hasCaptcha)
                {
                    prefs.set(Constants.CAPTCHA_PENDING, true)
                    prefs.set(Constants.CAPTCHA_VERIFICATION, lyricsResult.captchaUrl)
                    prefs.set(Constants.CAPTCHA_SERIAL, lyricsResult.captchaSerial)

                    prefs.set(Constants.CAPTCHA_TITLE_TARGET, art)
                    prefs.set(Constants.CAPTCHA_NAME_TARGET, mus)

                    //update panel list
                    Cocktail.updateList(context)
                }

                isRunning = false
                listener.onFetchFinished(context, success)
            }
            catch(e: Exception)//interrupted or cancelled or timeout
            {
                isRunning = false
                listener.onFetchFinished(context, false)//update panel to hide loader
            }
        }
    }
}