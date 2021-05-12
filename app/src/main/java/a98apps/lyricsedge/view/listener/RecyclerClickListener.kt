package a98apps.lyricsedge.view.listener

interface RecyclerClickListener
{
    fun onItemClick(position: Int, cacheId: String, musicId: String)
    fun onItemLongClick(position: Int , cacheId: String, musicId: String)
}