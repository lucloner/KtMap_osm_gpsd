package net.vicp.biggee.car

import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.MapPosition
import org.mapsforge.core.util.LatLongUtils
import org.mapsforge.core.util.Parameters
import org.mapsforge.map.awt.graphics.AwtGraphicFactory
import org.mapsforge.map.awt.util.AwtUtil
import org.mapsforge.map.awt.util.JavaPreferences
import org.mapsforge.map.awt.view.MapView
import org.mapsforge.map.datastore.MultiMapDataStore
import org.mapsforge.map.layer.debug.TileCoordinatesLayer
import org.mapsforge.map.layer.debug.TileGridLayer
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik
import org.mapsforge.map.model.common.PreferencesFacade
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.InternalRenderTheme
import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.util.*
import java.util.prefs.Preferences
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.WindowConstants

object Core : WindowAdapter() {
    const val MAP_FILE = "china.map"
    const val SHOW_DEBUG_LAYERS = true
    const val SHOW_RASTER_MAP = false
    const val MESSAGE = "Are you sure you want to exit the application?"
    const val TITLE = "Confirm close"
    val GRAPHIC_FACTORY by lazy { AwtGraphicFactory.INSTANCE }
    val mapFile by lazy { File(MAP_FILE) }
    val XUJIAHUI by lazy { LatLong(31.191340, 121.445790) }
    val latLongHistory by lazy { LinkedHashMap<LatLong, Long>() }
    var now = System.currentTimeMillis()

    lateinit var mapView: MapView
    lateinit var preferencesFacade: PreferencesFacade
    lateinit var boundingBox: BoundingBox
    lateinit var frame: JFrame

    fun init() {
        // Square frame buffer
        Parameters.SQUARE_FRAME_BUFFER = false

        val mapFiles = ArrayList<File>().apply {
            if (!SHOW_RASTER_MAP) {
                add(mapFile)
            }
        }
        mapView = MapView()
        mapView.mapScaleBar.isVisible = true

        val layers = mapView.layerManager.layers
        val tileSize = if (SHOW_RASTER_MAP) 256 else 512
        // Tile cache
        val tileCache = AwtUtil.createTileCache(
            tileSize,
            mapView.model.frameBufferModel.overdrawFactor,
            1024,
            File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString())
        )

        if (SHOW_RASTER_MAP) {
            // Raster
            mapView.model.displayModel.setFixedTileSize(tileSize)
            val tileSource = OpenStreetMapMapnik.INSTANCE
            tileSource.userAgent = "mapsforge-samples-awt"
            val tileDownloadLayer = KtTileDownloadLayer(tileCache, mapView.model.mapViewPosition, tileSource)
            layers.add(tileDownloadLayer)
            tileDownloadLayer.start()
            mapView.setZoomLevelMin(tileSource.zoomLevelMin)
            mapView.setZoomLevelMax(tileSource.zoomLevelMax)
            boundingBox = BoundingBox(
                LatLongUtils.LATITUDE_MIN,
                LatLongUtils.LONGITUDE_MIN,
                LatLongUtils.LATITUDE_MAX,
                LatLongUtils.LONGITUDE_MAX
            )
        } else {
            // Vector
            mapView.model.displayModel.setFixedTileSize(tileSize)
            val mapDataStore = MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL)
            mapFiles.forEach { file ->
                mapDataStore.addMapDataStore(MapFile(file), false, false)
            }
            val tileRendererLayer =
                KtTileRendererLayer(tileCache, mapDataStore, mapView.model.mapViewPosition, null).apply {
                    setXmlRenderTheme(InternalRenderTheme.DEFAULT)
                }
            layers.add(tileRendererLayer)
            boundingBox = mapDataStore.boundingBox()
        }

        // Debug
        if (SHOW_DEBUG_LAYERS) {
            mapView.fpsCounter.isVisible = true
            layers.add(TileGridLayer(GRAPHIC_FACTORY, mapView.model.displayModel))
            layers.add(
                TileCoordinatesLayer(
                    GRAPHIC_FACTORY,
                    mapView.model.displayModel
                )
            )
        }

        preferencesFacade = JavaPreferences(Preferences.userNodeForPackage(this::class.java))
        frame = JFrame().apply {
            title = "Mapsforge Car Map"
            layout = BorderLayout()
            add(BorderLayout.CENTER, mapView)
//            add(BorderLayout.NORTH, Button("test").apply {
//                addActionListener {
//
//                }
//            })
            pack()
            setSize(1024, 768)
            setLocationRelativeTo(null)
            defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            addWindowListener(this@Core)
            isVisible = true
        }
    }

    override fun windowOpened(e: WindowEvent?) {
        val model = mapView.model
        model.init(preferencesFacade)
        if (model.mapViewPosition.zoomLevel.toInt() == 0 || !boundingBox.contains(model.mapViewPosition.center)) {
            addLatLong(XUJIAHUI)
        }
    }

    override fun windowClosed(e: WindowEvent?) {
        val result = JOptionPane.showConfirmDialog(frame, MESSAGE, TITLE, JOptionPane.YES_NO_OPTION)
        if (result == JOptionPane.YES_OPTION) {
            mapView.model.save(preferencesFacade)
            mapView.destroyAll()
            AwtGraphicFactory.clearResourceMemoryCache()
            frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        }
    }

    fun addLatLong(latLong: LatLong): Long {
        now = System.currentTimeMillis()
        val result = latLongHistory.put(latLong, now) ?: -1
        forcus()
        return result
    }

    fun forcus() {
        val points = latLongHistory.keys.toTypedArray().apply {
            reverse()
        }
        val p0 = points.first()
        val t0 = latLongHistory[p0] ?: -1

        val p1 = if (points.size < 2) LatLong(p0.latitude + 0.1, p0.longitude + 0.1) else points[1]
        val t1 = latLongHistory[p1] ?: -1

        val model = mapView.model
        model.init(preferencesFacade)
        val zoomLevel =
            LatLongUtils.zoomForBounds(
                model.mapViewDimension.dimension,
                BoundingBox(
                    p0.latitude,
                    p0.longitude,
                    p1.latitude,
                    p1.longitude
                ),
                model.displayModel.tileSize
            )
        model.mapViewPosition.setMapPosition(MapPosition(XUJIAHUI, zoomLevel), true)

        latLongHistory.apply {
            clear()
            put(p1, t1)
            put(p0, t0)
        }
    }
}
