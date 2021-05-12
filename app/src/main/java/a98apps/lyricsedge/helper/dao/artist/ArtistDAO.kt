package a98apps.lyricsedge.helper.dao.artist

import a98apps.lyricsedge.constants.Db
import a98apps.lyricsedge.helper.DbHelper
import a98apps.lyricsedge.model.Artist
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.lang.Exception

class ArtistDAO(context: Context) : IArtistDAO
{
    private val dbHelper = DbHelper(context)
    private val read: SQLiteDatabase = dbHelper.readableDatabase
    private val write: SQLiteDatabase = dbHelper.writableDatabase

    //if success return the ID, if error return -1L
    override fun save(artist: Artist): Long
    {
        return try
        {
            val cv = ContentValues()
            cv.put("hash", artist.hash)
            cv.put("source_hash", artist.sourceHash)
            cv.put("name", artist.name)
            cv.put("url", artist.url)
            val result = write.insert(Db.TABLE_ARTIST, null, cv)
            write.close()
            result
        }
        catch (e: Exception)
        {
            Log.e(Db.DB_LOG, "Error on save on table ${Db.TABLE_ARTIST}: ", e)
            -1L
        }
    }

    override fun delete(id: Long): Boolean
    {
        return try
        {
            val args = arrayOf("$id")
            write.delete(Db.TABLE_ARTIST, "id=?", args)
            write.close()
            true
        }
        catch (e: Exception)
        {
            Log.e(Db.DB_LOG, "Error on delete on table ${Db.TABLE_ARTIST}: ", e)
            false
        }
    }

    override fun delete(ids: Array<String>): Boolean
    {
        return try
        {
            write.delete(Db.TABLE_ARTIST, "id=?", ids)
            write.close()
            true
        }
        catch (e: Exception)
        {
            Log.e(Db.DB_LOG, "Error on delete on table ${Db.TABLE_ARTIST}: ", e)
            false
        }
    }

    override fun update(artist: Artist): Boolean
    {
        return try
        {
            val cv = ContentValues()
            cv.put("id", artist.id)
            cv.put("hash", artist.hash)
            cv.put("source_hash", artist.sourceHash)
            cv.put("name", artist.name)
            cv.put("url", artist.url)
            val args = arrayOf("$artist.id")
            write.update(Db.TABLE_ARTIST, cv, "id=?", args)
            write.close()
            true
        }
        catch (e: Exception)
        {
            Log.e(Db.DB_LOG, "Error on update on table ${Db.TABLE_ARTIST}: ", e)
            false
        }
    }

    override fun list(): List<Artist>
    {
        val listArtists = ArrayList<Artist>()
        val sql = "SELECT * FROM " + Db.TABLE_ARTIST + ";"

        val c = read.rawQuery(sql, null)

        while (c.moveToNext())
        {
            val artists = Artist()
            artists.id = c.getLong(c.getColumnIndex("id"))
            artists.hash = c.getString(c.getColumnIndex("hash"))
            artists.sourceHash = c.getString(c.getColumnIndex("source_hash"))
            artists.name = c.getString(c.getColumnIndex("name"))
            artists.url = c.getString(c.getColumnIndex("url"))
            listArtists.add(artists)
        }

        c.close()
        read.close()

        return listArtists
    }

    //if has, return the ID, if not return -1L
    override fun has(hash: String): Long
    {
        val sql = "SELECT id FROM " + Db.TABLE_ARTIST + " WHERE hash = '$hash' LIMIT 1;"

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
        val sql = "DROP TABLE IF EXISTS " + Db.TABLE_ARTIST + ";"

        try
        {
            write.execSQL(sql)
            write.close()
        }
        catch (e: Exception)
        {
            Log.e(Db.DB_LOG, "Error on drop table ${Db.TABLE_ARTIST}: ", e)
        }
    }

}