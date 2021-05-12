package a98apps.lyricsedge.helper.dao.cache

import a98apps.lyricsedge.model.Cache
import a98apps.lyricsedge.model.CacheManager
import a98apps.lyricsedge.model.Lyric

interface ICacheDAO
{
    fun save(cache: Cache) : Long
    fun delete(id: Long) : Boolean
    fun delete(ids: Array<String>) : Boolean
    fun update(cache: Cache) : Boolean
    fun list() : MutableList<CacheManager>
    fun get(id: Long) : Lyric
    fun has(hash: String) : Long
    fun drop()
}