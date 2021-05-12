package a98apps.lyricsedge.helper.dao.music

import a98apps.lyricsedge.constants.Db
import a98apps.lyricsedge.helper.DbHelper
import a98apps.lyricsedge.model.Music
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.text.TextUtils
import android.util.Log
import java.lang.Exception

class MusicDAO(context: Context) : IMusicDAO
{
    private val dbHelper = DbHelper(context)
    private val read: SQLiteDatabase = dbHelper.readableDatabase
    private val write: SQLiteDatabase = dbHelper.writableDatabase

    override fun save(music: Music): Long
    {
        try
        {
            val cv = ContentValues()
            cv.put("hash", music.hash)
            cv.put("source_hash", music.sourceHash)
            cv.put("name", music.name)
            cv.put("lyric", music.lyric)
            cv.put("url", music.url)
            cv.put("translate", music.translate)
            val result = write.insert(Db.TABLE_MUSIC, null, cv)
            write.close()
            return result
        }
        catch (e: Exception)
        {
            Log.e(Db.DB_LOG, "Error on save on table ${Db.TABLE_MUSIC}: ", e)
            return -1L
        }
    }

    override fun delete(id: Long): Boolean
    {
        return try
        {
            val args = arrayOf("$id")
            write.delete(Db.TABLE_MUSIC, "id=?", args)
            write.close()
            true
        }
        catch (e: Exception)
        {
            Log.e(Db.DB_LOG, "Error on delete on table ${Db.TABLE_MUSIC}: ", e)
            false
        }
    }

    override fun delete(ids: Array<String>): Boolean
    {
        return try
        {
            val idsInString: String = TextUtils.join(",", ids)
            write.delete(Db.TABLE_MUSIC, "id IN ($idsInString)", null)
            write.close()
            true
        }
        catch (e: Exception)
        {
            Log.e(Db.DB_LOG, "Error on delete on table ${Db.TABLE_MUSIC}: ", e)
            false
        }
    }

    override fun update(music: Music): Boolean
    {
        return try
        {
            val cv = ContentValues()
            cv.put("id", music.id)
            cv.put("hash", music.hash)
            cv.put("source_hash", music.sourceHash)
            cv.put("name", music.name)
            cv.put("lyric", music.lyric)
            cv.put("url", music.url)
            cv.put("translate", music.translate)

            val args = arrayOf("$music.id")
            write.update(Db.TABLE_MUSIC, cv, "id=?", args)
            write.close()
            true
        }
        catch (e: Exception)
        {
            Log.e(Db.DB_LOG, "Error on update on table ${Db.TABLE_MUSIC}: ", e)
            false
        }
    }

    override fun list(): List<Music>
    {
        val list = ArrayList<Music>()
        val sql = "SELECT * FROM " + Db.TABLE_MUSIC + ";"

        val c = read.rawQuery(sql, null)
        
        while (c.moveToNext())
        {
            val music = Music()
            music.id = c.getLong(c.getColumnIndex("id"))
            music.hash = c.getString(c.getColumnIndex("hash"))
            music.sourceHash = c.getString(c.getColumnIndex("source_hash"))
            music.name = c.getString(c.getColumnIndex("name"))
            music.lyric = c.getString(c.getColumnIndex("lyric"))
            music.url = c.getString(c.getColumnIndex("url"))
            music.translate = c.getString(c.getColumnIndex("translate"))
            list.add(music)
        }

        c.close()
        read.close()

        return list
    }

    override fun drop()
    {
        val sql = "DROP TABLE IF EXISTS " + Db.TABLE_MUSIC + ";"

        try
        {
            write.execSQL(sql)
            write.close()
        }
        catch (e: Exception)
        {
            Log.e(Db.DB_LOG, "Error on drop table ${Db.TABLE_MUSIC}: ", e)
        }
    }
}