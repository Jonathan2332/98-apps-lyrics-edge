package a98apps.lyricsedge.model

class Lyric
{
    var id: Long? = null
    var type: String? = null
    var hash: String? = null

    var artistId: Long? = null
    var artistHash: String? = null
    var artistSourceHash: String? = null
    var artistName: String? = null
    var artistUrl: String? = null

    var musicId: Long? = null
    var musicHash: String? = null
    var musicSourceHash: String? = null
    var musicName: String? = null
    var musicLyric: String? = null
    var musicUrl: String? = null
    var musicTranslate: String? = null
}