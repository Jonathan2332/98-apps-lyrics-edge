package a98apps.lyricsedge.view.fragments

import a98apps.lyricsedge.R
import a98apps.lyricsedge.constants.Actions
import a98apps.lyricsedge.constants.Constants
import a98apps.lyricsedge.edge.Cocktail
import a98apps.lyricsedge.helper.dao.cache.CacheDAO
import a98apps.lyricsedge.helper.dao.music.MusicDAO
import a98apps.lyricsedge.notification.NotificationListener
import a98apps.lyricsedge.preferences.SecurityPreferences
import a98apps.lyricsedge.util.FormatterUtil
import a98apps.lyricsedge.view.adapter.LyricCacheRecyclerViewAdapter
import a98apps.lyricsedge.view.listener.RecyclerClickListener
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*


class LyricCacheFragment : Fragment(), RecyclerClickListener
{

    private lateinit var mSwipe: SwipeRefreshLayout
    private lateinit var mLoader: LinearProgressIndicator
    private lateinit var mAdapter: LyricCacheRecyclerViewAdapter
    private lateinit var mSearchView: SearchView
    private lateinit var mSearchItem: MenuItem

    private var mActionMode: ActionMode? = null

    private lateinit var mJob: Job

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        val view = inflater.inflate(R.layout.fragment_cache_list, container, false)

        mSwipe = view.findViewById(R.id.swipe_list)
        mLoader = view.findViewById(R.id.list_loader)

        val recyclerView = view.findViewById<RecyclerView>(R.id.list)

        //initialize the adapter
        showLoader()
        mJob = GlobalScope.launch(Dispatchers.IO) {
            val cacheDao = CacheDAO(requireContext())
            val list = cacheDao.list()
            launch(Dispatchers.Main) {
                mAdapter = LyricCacheRecyclerViewAdapter(list, this@LyricCacheFragment)

                with(recyclerView) {
                    layoutManager = LinearLayoutManager(context)
                    adapter = mAdapter
                    addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
                }
                hideLoader()
            }
        }

        mSwipe.setOnRefreshListener {
            if(!mJob.isActive)
            {
                refreshContent()
            }
        }

        setHasOptionsMenu(true)

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.search_menu, menu)

        mSearchItem = menu.findItem(R.id.search)
        mSearchView = mSearchItem.actionView as SearchView
        mSearchView.imeOptions = EditorInfo.IME_ACTION_DONE
        mSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener
        {
            override fun onQueryTextSubmit(query: String): Boolean
            {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean
            {
                if(::mAdapter.isInitialized && !mJob.isActive && mActionMode == null) mAdapter.filter.filter(newText)
                return false
            }
        })
    }

    override fun onDestroy()
    {
        super.onDestroy()
        if(::mJob.isInitialized && mJob.isActive) mJob.cancel(null)
    }

    //called only on swipe
    private fun refreshContent()
    {
        setMenuVisibility(false)
        mJob = GlobalScope.launch(Dispatchers.IO) {
            val cacheDao = CacheDAO(requireContext())
            val list = cacheDao.list()

            launch(Dispatchers.Main) {
                mAdapter.updateItems(list)
                mSwipe.isRefreshing = false
                setMenuVisibility(true)
            }
        }
    }

    private fun updateContent()
    {
        showLoader()
        mJob = GlobalScope.launch(Dispatchers.IO) {
            val cacheDao = CacheDAO(requireContext())
            val list = cacheDao.list()

            launch(Dispatchers.Main) {
                mAdapter.updateItems(list)
                hideLoader()
            }
        }
    }

    private val mActionModeCallback: ActionMode.Callback = object : ActionMode.Callback
    {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean
        {
            mode.menuInflater.inflate(R.menu.actions_menu, menu)
            mode.title = "0"

            mSearchView.clearFocus()
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean
        {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean
        {
            return when(item.itemId)
            {
                R.id.delete ->
                {
                    val snackBar = Snackbar.make(requireView(), getString(R.string.text_are_you_sure), Snackbar.LENGTH_LONG)
                    snackBar.setAction(getString(android.R.string.ok)) {
                        if(!mJob.isActive)
                        {
                            showLoader()

                            val cacheIds = mAdapter.getSelectedCacheIds().toTypedArray()
                            val musicIds = mAdapter.getSelectedMusicIds().toTypedArray()

                            mode.finish()

                            mJob = GlobalScope.launch(Dispatchers.IO) {
                                val prefs = SecurityPreferences(requireContext())
                                val cacheIndex = prefs.get(Constants.CACHE_INDEX) as Long

                                val cacheDAO = CacheDAO(requireContext())
                                if(cacheDAO.delete(cacheIds))
                                {
                                    if(cacheIds.contains(cacheIndex.toString()))//Update panel if the current index is the deleted
                                    {
                                        prefs.setDefault(Constants.TEMP_HASH)
                                        prefs.setDefault(Constants.TRANSLATE_TOGGLE)
                                        prefs.setDefault(Constants.AVAILABLE_TRANSLATE)
                                        Cocktail.updateList(requireContext())
                                        Cocktail.updatePanel(requireContext(), false)
                                    }

                                    val musicDAO = MusicDAO(requireContext())
                                    if(musicDAO.delete(musicIds))
                                    {
                                        launch(Dispatchers.Main) {
                                            requireContext().startService(Intent(requireContext(), NotificationListener::class.java).setAction(Actions.ACTION_CHANGED_SETTINGS))
                                            Toast.makeText(requireContext(), getString(R.string.text_successful_deleted), Toast.LENGTH_SHORT).show()
                                            hideLoader()
                                            updateContent()
                                        }
                                    }
                                    else
                                    {
                                        launch(Dispatchers.Main) {
                                            requireContext().startService(Intent(requireContext(), NotificationListener::class.java).setAction(Actions.ACTION_CHANGED_SETTINGS))
                                            Toast.makeText(requireContext(), getString(R.string.text_partially_deleted), Toast.LENGTH_SHORT).show()
                                            hideLoader()
                                            updateContent()
                                        }
                                    }
                                }
                                else
                                {
                                    launch(Dispatchers.Main) {
                                        Toast.makeText(requireContext(), getString(R.string.text_error_on_delete), Toast.LENGTH_SHORT).show()
                                        hideLoader()
                                    }
                                }
                            }
                        }
                    }
                    if(!mJob.isActive)
                    {
                        snackBar.show()
                    }
                    true
                }
                R.id.share ->
                {
                    if(mAdapter.selectedItemsCount() == 1)
                    {
                        val selectedItem = mAdapter.getSelectedItem()
                        val title = FormatterUtil.formatNewLine(selectedItem.musicName ?: "", selectedItem.artistName ?: "")
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.type = "text/plain"
                        intent.putExtra(Intent.EXTRA_SUBJECT, title)
                        intent.putExtra(Intent.EXTRA_TEXT, mAdapter.getSelectedItem().musicLyric)
                        if(intent.resolveActivity(requireActivity().packageManager) != null) //Android 11 or up require queries tag on manifest
                        {
                            startActivity(intent)
                        }
                        else
                        {
                            Toast.makeText(activity, getString(R.string.text_not_found_activity_share), Toast.LENGTH_LONG).show()
                        }
                        mode.finish()
                    }
                    true
                }
                R.id.select_all ->
                {
                    if(mAdapter.itemCount != 0)
                    {
                        if(!mJob.isActive)
                        {
                            showLoader()
                            mJob = GlobalScope.launch(Dispatchers.IO) {

                                mAdapter.selectAllItems()
                                launch(Dispatchers.Main) {

                                    mAdapter.notifyDataSetChanged()
                                    hideLoader()

                                    //update selected items count and items visibility
                                    handleActionItems()
                                }
                            }
                        }
                    }
                    true
                }
                R.id.view ->
                {
                    if(mAdapter.selectedItemsCount() == 1)
                    {
                        Toast.makeText(requireContext(), getString(R.string.text_preview_panel), Toast.LENGTH_SHORT).show()
                        val prefs = SecurityPreferences(requireContext())
                        prefs.set(Constants.CACHE_INDEX, mAdapter.getSelectedCacheIds()[0].toLong())
                        prefs.setDefault(Constants.TRANSLATE_TOGGLE)
                        Cocktail.updateList(requireContext())
                        Cocktail.updatePanel(requireContext(), true)
                        mode.finish()
                    }
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode)
        {
            mAdapter.clearSelectedItems()
            mActionMode = null
            if(::mSearchItem.isInitialized)
            {
                mSearchItem.collapseActionView()
            }
        }
    }

    override fun onItemClick(position: Int, cacheId: String, musicId: String)
    {
        if(mActionMode != null)//Only able to select if action mode started
        {
            mAdapter.handleSelectedItem(position, cacheId, musicId)

            //update selected items count and items visibility
            handleActionItems()
        }
    }

    override fun onItemLongClick(position: Int, cacheId: String, musicId: String)
    {
        if(mActionMode == null && !mJob.isActive)//Not started
        {
            mActionMode = requireActivity().startActionMode(mActionModeCallback)
            mAdapter.handleSelectedItem(position, cacheId, musicId)

            //update selected items count and items visibility
            handleActionItems()
        }
    }

    private fun handleActionItems()
    {
        if(mAdapter.selectedItemsCount() != 0)
        {
            //update selected items count
            mActionMode?.title = mAdapter.selectedItemsCount().toString()

            val am: AudioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val view = mActionMode?.menu?.findItem(R.id.view)

            if(!am.isMusicActive)
            {
                view?.isVisible = mAdapter.selectedItemsCount() == 1
            }
            else
            {
                view?.isVisible = false
            }

            val share = mActionMode?.menu?.findItem(R.id.share)
            share?.isVisible = mAdapter.selectedItemsCount() == 1

            val selectAll = mActionMode?.menu?.findItem(R.id.select_all)
            selectAll?.isVisible = mAdapter.itemCount != 0 && mAdapter.itemCount > 1
        }
        else mActionMode?.finish()
    }

    private fun showLoader()
    {
        mLoader.visibility = View.VISIBLE
        mSwipe.isEnabled = false

        setMenuVisibility(false)
    }

    private fun hideLoader()
    {
        mLoader.visibility = View.GONE
        mSwipe.isEnabled = true
        mSwipe.isRefreshing = false

        setMenuVisibility(true)
    }
}