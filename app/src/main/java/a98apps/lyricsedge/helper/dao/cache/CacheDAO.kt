package a98apps.lyricsedge.helper.dao.cache

import a98apps.lyricsedge.constants.Db
import a98apps.lyricsedge.helper.DbHelper
import a98apps.lyricsedge.model.Cache
import a98apps.lyricsedge.model.CacheManager
import a98apps.lyricsedge.model.Lyric
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.text.TextUtils
import android.util.Log

class CacheDAO(context: Context) : ICacheDAO
{
    private val dbHelper = DbHelper(context)
    private val read: SQLiteDatabase = dbHelper.readableDatabase
    private val write: SQLiteDatabase = dbHelper.writableDatabase

    //if success return the ID, if error return -1L
    override fun save(cache: Cache): Long
    {
        return try
        {
            val cv = ContentValues()
            cv.put("music_id", cache.musicId)
            cv.put("artist_id", cache.artistId)
            cv.put("hash", cache.hash)
            cv.put("type", cache.type)
            val result = write.insert(Db.TABLE_CACHE, null, cv)
            write.close()
            result
        }
        catch (e: Exception)
        {
            Log.e(Db.DB_LOG, "Error on save on table ${Db.TABLE_CACHE}: ", e)
            -1L
        }
    }

    override fun delete(id: Long): Boolean
    {
        return try
        {
            val args = arrayOf("$id")
            write.delete(Db.TABLE_CACHE, "id=?", args)
            write.close()
            true
        }
        catch (e: Exception)
        {
            Log.e(Db.DB_LOG, "Error on delete on table ${Db.TABLE_CACHE}: ", e)
            false
        }
    }

    override fun delete(ids: Array<String>): Boolean
    {
        return try
        {
            val idsInString: String = TextUtils.join(",", ids)
            write.delete(Db.TABLE_CACHE, "id IN ($idsInString)", null)
            write.close()
            true
        }
        catch (e: Exception)
        {
            Log.e(Db.DB_LOG, "Error on delete on table ${Db.TABLE_CACHE}: ", e)
            false
        }
    }

    override fun update(cache: Cache): Boolean
    {
        return try
        {
            val cv = ContentValues()
            cv.put("id", cache.id)
            cv.put("music_id", cache.musicId)
            cv.put("artist_id", cache.artistId)
            cv.put("hash", cache.hash)
            cv.put("type", cache.type)
            val args = arrayOf("$cache.id")
            write.update(Db.TABLE_CACHE, cv, "id=?", args)
            write.close()
            true
        }
        catch (e: Exception)
        {
            Log.e(Db.DB_LOG, "Error on update on table ${Db.TABLE_CACHE}: ", e)
            false
        }
    }

    override fun list(): MutableList<CacheManager>
    {
        val list = ArrayList<CacheManager>()
        val sql = "SELECT c.id, c.type, c.music_id,\n" +
                "m.name as music_name, m.lyric as music_lyric,\n" +
                "a.name as artist_name\n" +
                "FROM cache AS c\n" +
                "INNER JOIN artist AS a ON c.artist_id = a.id\n" +
                "INNER JOIN music AS m ON c.music_id = m.id;"

        val c = read.rawQuery(sql, null)

        while (c.moveToNext())
        {
            val lyricCache = CacheManager()
            lyricCache.id = c.getLong(c.getColumnIndex("id"))
            lyricCache.musicId = c.getLong(c.getColumnIndex("music_id"))
            lyricCache.type = c.getString(c.getColumnIndex("type"))
            lyricCache.musicName = c.getString(c.getColumnIndex("music_name"))
            lyricCache.artistName = c.getString(c.getColumnIndex("artist_name"))
            lyricCache.musicLyric = c.getString(c.getColumnIndex("music_lyric"))
            list.add(lyricCache)
        }

        c.close()
        read.close()

        return list
    }

    override fun get(id: Long): Lyric
    {
        val lyric = Lyric()
        val sql = "SELECT c.id, c.hash, c.type, c.artist_id, c.music_id, \n" +
                "m.hash as music_hash, m.source_hash as music_source_hash, m.name as music_name, m.lyric as music_lyric, m.url as music_url, m.translate as music_translate,\n" +
                "a.hash as artist_hash, a.source_hash as artist_source_hash, a.name as artist_name, a.url as artist_url\n" +
                "FROM cache AS c \n" +
                "INNER JOIN artist AS a ON c.artist_id = a.id\n" +
                "INNER JOIN music AS m ON c.music_id = m.id\n" +
                "WHERE c.id = $id;"

        val c = read.rawQuery(sql, null)

        if(c.count > 0)
        {
            c.moveToFirst()
            lyric.id = c.getLong(c.getColumnIndex("id"))
            lyric.type = c.getString(c.getColumnIndex("type"))
            lyric.hash = c.getString(c.getColumnIndex("hash"))

            lyric.musicId = c.getLong(c.getColumnIndex("music_id"))
            lyric.musicHash = c.getString(c.getColumnIndex("music_hash"))
            lyric.musicSourceHash = c.getString(c.getColumnIndex("music_source_hash"))
            lyric.musicName = c.getString(c.getColumnIndex("music_name"))
            lyric.musicLyric = c.getString(c.getColumnIndex("music_lyric"))
            lyric.musicUrl = c.getString(c.getColumnIndex("music_url"))
            lyric.musicTranslate = c.getString(c.getColumnIndex("music_translate"))

            lyric.artistId = c.getLong(c.getColumnIndex("artist_id"))
            lyric.artistHash = c.getString(c.getColumnIndex("artist_hash"))
            lyric.artistSourceHash = c.getString(c.getColumnIndex("artist_source_hash"))
            lyric.artistName = c.getString(c.getColumnIndex("artist_name"))
            lyric.artistUrl = c.getString(c.getColumnIndex("artist_url"))
        }
        c.close()
        read.close()

        return lyric
    }

    //if has, return the ID, if not return -1L
    override fun has(hash: String): Long
    {
        val sql = "SELECT id FROM " + Db.TABLE_CACHE + " WHERE hash = '$hash' LIMIT 1;"

        val c = read.rawQuery(sql, null)

        val id: Long = if(c.count > 0)
        {
            c.moveToFirst()
            c.getLong(c.getColumnIndex("id"))
        }
        else -1L

        c.close()
        read.close()

        return id
    }

    override fun drop()
    {
        val sql = "DROP TABLE IF EXISTS " + Db.TABLE_CACHE + ";"

        try
        {
            write.execSQL(sql)
            write.close()
        }
        catch (e: Exception)
        {
            Log.e(Db.DB_LOG, "Error on drop table ${Db.TABLE_CACHE}: ", e)
        }
    }

}