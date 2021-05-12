package a98apps.lyricsedge.helper.dao.artist

import a98apps.lyricsedge.model.Artist

interface IArtistDAO
{
    fun save(artist: Artist) : Long
    fun delete(id: Long) : Boolean
    fun delete(ids: Array<String>) : Boolean
    fun update(artist: Artist) : Boolean
    fun list() : List<Artist>
    fun has(hash: String) : Long
    fun drop()
}