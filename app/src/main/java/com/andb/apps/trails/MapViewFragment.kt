package com.andb.apps.trails

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import com.andb.apps.trails.download.FileDownloader
import com.andb.apps.trails.objects.SkiMap
import com.andb.apps.trails.views.GlideApp
import com.andb.apps.trails.xml.MapXMLParser
import com.andb.apps.trails.xml.filenameFromURL
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import de.number42.subsampling_pdf_decoder.PDFDecoder
import de.number42.subsampling_pdf_decoder.PDFRegionDecoder
import kotlinx.android.synthetic.main.map_view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.android.Main

class MapViewFragment : Fragment() {

    var map: SkiMap? = null
    private var mapKey = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapKey = arguments!!.getInt("mapKey")
        Log.d("initialized fragment", "mapKey: $mapKey")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.map_view, container!!.parent as ViewGroup, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("MapViewFragment", "before thread")
        setStatusBarColors(activity!!, false)
        mapLoadingIndicator.visibility = View.VISIBLE
        val handler = Handler()
        Thread(Runnable {
            Log.d("MapViewFragment", "before parsing")
            map = MapXMLParser.parseFull(mapKey)
            Log.d("MapViewFragment", "after parsing")
            handler.post {
                map?.apply {
                    skiMapAreaName?.text = skiArea.name
                    skiMapYear?.text = year.toString()
                    if (!isPdf()) {
                        GlideApp.with(this@MapViewFragment)
                            .asBitmap()
                            .load(imageUrl)
                            .into(object : CustomViewTarget<View, Bitmap>(mapImageView){
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    mapImageView.setImage(ImageSource.cachedBitmap(resource))
                                    mapLoadingIndicator.visibility = View.GONE
                                }

                                override fun onLoadFailed(errorDrawable: Drawable?) {
                                    mapImageView.recycle()
                                }
                                override fun onResourceCleared(placeholder: Drawable?) {
                                }
                            })

                    } else {
                        CoroutineScope(Dispatchers.IO).launch {
                            val file = FileDownloader.downloadFile(imageUrl, filenameFromURL(imageUrl))
                            withContext(Dispatchers.Main) {
                                mapImageView?.apply {
                                    setMinimumTileDpi(120)
                                    setBitmapDecoderFactory { PDFDecoder(0, file, 10f) }
                                    setRegionDecoderFactory { PDFRegionDecoder(0, file, 10f) }
                                    setImage(ImageSource.uri(file.absolutePath))
                                }
                                mapLoadingIndicator?.visibility = View.GONE
                            }
                        }

                    }
                }
            }
        }).start()

    }
}

fun setStatusBarColors(activity: Activity, light: Boolean = true){
    val color = if(light) Color.WHITE else Color.BLACK
    activity.window.statusBarColor = color
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        activity.window.decorView.systemUiVisibility = if(light) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0
    }
}

fun openMapView(id: Int, context: Context){
    val activity = context as FragmentActivity
    val ft = activity.supportFragmentManager.beginTransaction()
    ft.addToBackStack("mapView")
    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)

    val fragment = MapViewFragment()
    val bundle = Bundle()
    bundle.putInt("mapKey", id)
    fragment.arguments = bundle

    ft.add(R.id.mapViewHolder, fragment)
    ft.commit()
}