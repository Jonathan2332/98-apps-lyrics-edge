package a98apps.lyricsedge.helper

import a98apps.lyricsedge.BuildConfig
import a98apps.lyricsedge.constants.Db
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.lang.Exception

class DbHelper(context: Context) : SQLiteOpenHelper(context, Db.DB_NAME, null, BuildConfig.VERSION_CODE)
{
    override fun onCreate(db: SQLiteDatabase?)
    {
        if(db != null)
        {
            val sqlTableMusic = "CREATE TABLE IF NOT EXISTS `music` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL ,`hash` TEXT NOT NULL,`source_hash` TEXT NOT NULL,`name` TEXT NOT NULL,`lyric` TEXT NOT NULL,`url` TEXT NOT NULL, `translate` TEXT NOT NULL);"
            val sqlTableArtist = "CREATE TABLE IF NOT EXISTS `artist` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL ,`hash` TEXT NOT NULL,`source_hash` TEXT NOT NULL,`name` TEXT NOT NULL,`url` TEXT NOT NULL);"
            val sqlTableCache = "CREATE TABLE IF NOT EXISTS `cache` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `artist_id` INT NOT NULL, `music_id` INT NOT NULL, `hash` TEXT NOT NULL, `type` VARCHAR(50) NOT NULL, FOREIGN KEY (`artist_id`) REFERENCES `artist` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION, FOREIGN KEY (`music_id`) REFERENCES `music` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION);"
            val sqlIndexFkArtist = "CREATE " + "INDEX `fk_cache_artist_idx` on cache (`artist_id` ASC);"
            val sqlIndexFkMusic = "CREATE " + "INDEX `fk_cache_music1_idx` on cache (`music_id` ASC);"

            try
            {
                db.execSQL(sqlTableArtist)
                db.execSQL(sqlTableMusic)
                db.execSQL(sqlTableCache)
                db.execSQL(sqlIndexFkArtist)
                db.execSQL(sqlIndexFkMusic)
            }
            catch (e: Exception)
            {
                e.printStackTrace()
            }
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int)
    {

    }

}