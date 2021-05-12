package a98apps.lyricsedge.helper.dao.music

import a98apps.lyricsedge.model.Music

interface IMusicDAO
{
    fun save(music: Music) : Long
    fun delete(id: Long) : Boolean
    fun delete(ids: Array<String>) : Boolean
    fun update(music: Music) : Boolean
    fun list() : List<Music>
    fun drop()
}