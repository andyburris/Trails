package com.andb.apps.trails

import android.app.ProgressDialog
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.andb.apps.trails.database.areasDao
import com.andb.apps.trails.database.db
import com.andb.apps.trails.database.regionAreaDao
import com.andb.apps.trails.download.setupRegions
import com.andb.apps.trails.download.updateAreas
import com.andb.apps.trails.lists.AreaList
import com.andb.apps.trails.lists.FavoritesList
import com.andb.apps.trails.lists.RegionList
import com.andb.apps.trails.pages.ExploreFragment
import com.andb.apps.trails.pages.FavoritesFragment
import com.andb.apps.trails.pages.SearchFragment
import com.google.android.material.tabs.TabLayout
import jonathanfinerty.once.Once
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.explore_layout.*
import kotlinx.coroutines.*
import kotlinx.coroutines.android.Main

class MainActivity : AppCompatActivity() {

    val favoritesFragment by lazy { FavoritesFragment() }
    val exploreFragment  by lazy { ExploreFragment() }
    val searchFragment  by lazy { SearchFragment() }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter internal constructor(fm: FragmentManager) :
        FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment? {
            return when (position) {
                0 -> favoritesFragment

                1 -> exploreFragment

                2 -> searchFragment

                else -> null
            }
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).

        }

        override fun getCount(): Int {
            // Show 3 total pages.
            return 3
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)


        CoroutineScope(Dispatchers.IO).launch {
            setupData(pager)
            AreaList.setup()

        }
        toolbar.subtitle = subtitleFromPage(0)
        pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                toolbar.subtitle = subtitleFromPage(position)
            }
        })
        navigation.setOnNavigationItemSelectedListener {
            when (it.itemId){
                R.id.navigation_fav -> pager.currentItem = 0

                R.id.navigation_explore -> pager.currentItem = 1

                R.id.navigation_search -> pager.currentItem = 2
            }

            true
        }


    }

    lateinit var dialog: ProgressDialog
    suspend fun setupData(pager: ViewPager){


        if(!Once.beenDone("regionSetup")){
            withContext(Dispatchers.Main) {
                dialog = ProgressDialog.show(this@MainActivity, getString(R.string.download_progress_title), getString(R.string.download_progress_desc), true, false)
            }

            Log.d("regionSetup", "Setting up regions")
            val regionsSet = setupRegions()
            if(regionsSet){
                Once.markDone("regionSetup")
            }
            val areasSet = updateAreas()

            dialog.cancel()

            if(!areasSet || !regionsSet){
                AlertDialog.Builder(this).setTitle(R.string.download_progress_failed).setPositiveButton(R.string.download_progress_try_again){ dlg, _ ->
                    dlg.cancel()
                    CoroutineScope(Dispatchers.IO).launch {
                        setupData(pager)
                    }
                }
            }


        }

        withContext(Dispatchers.Main){
            pager.adapter = SectionsPagerAdapter(supportFragmentManager)
        }
    }

    fun subtitleFromPage(page: Int): String {
        return when (page) {
            1 -> {
                if (!RegionList.backStack.isEmpty()) {
                    RegionList.currentRegion().name
                } else {
                    ""
                }
            }
            else -> ""
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.overflow, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.test_menu_item -> {
                AsyncTask.execute {
                    Log.d("dbCount", "Join Count: ${regionAreaDao().getSize()}")
                    Log.d("dbCount", "Area Count: ${areasDao().getSize()}")

                }
                return true
            }
        }
        return false
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            super.onBackPressed()
            if (supportFragmentManager.backStackEntryCount <= 0) {
                toolbar.title = getString(R.string.app_name)
                toolbar.subtitle = subtitleFromPage(pager.currentItem)
            }
        } else if (RegionList.backStack.size > 1 && pager.currentItem == 1) {
            RegionList.drop()
            exploreFragment.exploreAdapter.notifyDataSetChanged()
            toolbar.subtitle = RegionList.currentRegion().name
            if (RegionList.backStack.size == 1) {
                switchRegionButton.visibility = View.VISIBLE
            }
        } else {
            super.onBackPressed()
        }

    }


}
