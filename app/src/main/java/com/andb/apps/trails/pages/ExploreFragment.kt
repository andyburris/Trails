package com.andb.apps.trails.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.andb.apps.trails.AreaViewFragment
import com.andb.apps.trails.R
import com.andb.apps.trails.lists.FavoritesList
import com.andb.apps.trails.lists.RegionList
import com.andb.apps.trails.objects.BaseSkiArea
import com.andb.apps.trails.objects.SkiRegion
import com.andb.apps.trails.utils.Utils
import com.github.rongi.klaster.Klaster
import com.google.android.material.chip.Chip
import com.like.LikeButton
import com.like.OnLikeListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.explore_item_area.*
import kotlinx.android.synthetic.main.explore_item_region.*
import kotlinx.android.synthetic.main.explore_layout.*
import kotlinx.coroutines.*
import kotlinx.coroutines.android.Main

const val EXPLORE_AREA_ITEM_TYPE = 32940
const val EXPLORE_REGION_ITEM_TYPE = 34987

class ExploreFragment : Fragment() {

    lateinit var exploreAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.explore_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoroutineScope(Dispatchers.IO).launch {
            RegionList.setup()
            withContext(Dispatchers.Main) {
                setParentRegion(1, true)
            }
        }

        switchRegionButton.setOnClickListener {
            val popup = PopupMenu(context, switchRegionButton)
            popup.menuInflater.inflate(R.menu.region_switcher, popup.menu)
            popup.show()
            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.region_americas -> setParentRegion(1)
                    R.id.region_europe -> setParentRegion(2)
                    R.id.region_asia -> setParentRegion(3)
                    R.id.region_oceania -> setParentRegion(4)
                    else -> return@setOnMenuItemClickListener false
                }

                return@setOnMenuItemClickListener true
            }
        }

    }

    private fun setParentRegion(id: Int, start: Boolean = false) {
        activity!!.loadingIndicator.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            RegionList.backStack.apply {
                clear()
                add(RegionList.parentRegions[id - 1])
            }
            withContext(Dispatchers.Main) {
                activity!!.loadingIndicator.visibility = View.GONE
                if (start) {
                    exploreAdapter = exploreAdapter()
                    exploreRegionRecycler.layoutManager = LinearLayoutManager(context)
                    exploreRegionRecycler.adapter = exploreAdapter
                    activity!!.apply {
                        if (pager.currentItem == 1) {
                            toolbar.subtitle = RegionList.currentRegion().name
                        }
                    }
                } else {
                    activity!!.toolbar.subtitle = RegionList.currentRegion().name
                    exploreAdapter.notifyDataSetChanged()
                }
            }
        }
    }


    private fun exploreAdapter() = Klaster.get()
        .itemCount {
            RegionList.currentRegion().let {
                if (it.children.isEmpty()) it.areas.size else it.children.size
            }
        }
        .view { viewType, parent ->
            when (viewType) {
                EXPLORE_REGION_ITEM_TYPE -> layoutInflater.inflate(
                    R.layout.explore_item_region,
                    parent,
                    false
                )
                else -> layoutInflater.inflate(R.layout.explore_item_area, parent, false)
            }
        }
        .bind { position ->
            RegionList.currentRegion().apply {
                if (itemViewType == EXPLORE_REGION_ITEM_TYPE) {
                    val region = children[position]
                    exploreRegionName.text = region.name
                    Utils.showIfAvailible(region.mapCount, exploreRegionMaps, R.string.explore_item_region_maps)
                    chipChild(0, region, exploreRegionChildrenChip1)
                    chipChild(1, region, exploreRegionChildrenChip2)
                    itemView.setOnClickListener {
                        activity!!.loadingIndicator.visibility = View.VISIBLE
                        nextRegion(region)
                        activity!!.switchRegionButton.visibility = View.GONE
                    }
                } else {
                    exploreItemAreaName.text = areas[position].name
                    areaLikeButton.apply {
                        isLiked = FavoritesList.contains(areas[position])
                        setOnLikeListener(object : OnLikeListener {
                            override fun liked(p0: LikeButton?) {
                                FavoritesList.add(areas[position])
                            }

                            override fun unLiked(p0: LikeButton?) {

                            }
                        })
                    }
                    itemView.setOnClickListener {
                        openAreaView(areas[position])
                    }
                }

            }

        }.getItemViewType {
            if (RegionList.currentRegion().children.isEmpty()) EXPLORE_AREA_ITEM_TYPE else EXPLORE_REGION_ITEM_TYPE
        }

        .build()

    fun openAreaView(area: BaseSkiArea) {
        val fragmentActivity = context as FragmentActivity
        val ft = fragmentActivity.supportFragmentManager.beginTransaction()

        val intent = AreaViewFragment()
        intent.arguments =
            Bundle().also { it.putInt("areaKey", area.id) }
        ft.add(R.id.exploreAreaReplacement, intent)
        ft.addToBackStack("areaView")
        ft.commit()
    }

    fun nextRegion(region: SkiRegion) {
        CoroutineScope(Dispatchers.IO).launch {
            RegionList.backStack.add(region)
            withContext(Dispatchers.Main) {
                activity!!.toolbar.subtitle = RegionList.currentRegion().name
                activity!!.loadingIndicator.visibility = View.GONE
                exploreAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun chipChild(position: Int, region: SkiRegion, chip: Chip) {
        chip.apply {
            val chipText = chipText(position, region)
            if (chipText == "") {
                visibility = View.GONE
            } else {
                visibility = View.VISIBLE
                text = chipText
                setOnClickListener {
                    if (region.children.isEmpty()) {
                        openAreaView(region.areas[position])
                    } else {
                        nextRegion(region.children.sortedWith(Comparator { o1, o2 ->
                            o2.mapCount.compareTo(o1.mapCount)
                        })[position])
                    }
                }
            }
        }
    }

    private fun chipText(position: Int, region: SkiRegion): String {
        region.apply {
            return if (children.isEmpty()) {
                if (areas.size > position) areas[position].name else ""
            } else {
                if (children.size > position)
                    children.sortedWith(Comparator { o1, o2 ->
                        o2.mapCount.compareTo(o1.mapCount)
                    })[position].name
                else
                    ""
            }
        }
    }
}

