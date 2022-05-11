package net.vicp.biggee.car

import org.mapsforge.core.model.BoundingBox
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
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.util.*
import java.util.prefs.Preferences
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.WindowConstants

object Core : WindowAdapter() {
    const val MAP_FILE = "/home/lucloner/china.map"
    const val SHOW_DEBUG_LAYERS = false
    const val SHOW_RASTER_MAP = false
    const val MESSAGE = "Are you sure you want to exit the application?"
    const val TITLE = "Confirm close"
    val GRAPHIC_FACTORY by lazy { AwtGraphicFactory.INSTANCE }
    val mapFile by lazy { File(MAP_FILE) }

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
            val tileDownloadLayer = KtTileDownloadLayer(
                tileCache,
                mapView.model.mapViewPosition,
                tileSource
            )
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
            val tileRendererLayer = KtTileRendererLayer(
                tileCache,
                mapDataStore,
                mapView.model.mapViewPosition,
                null
            ).apply {
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
        frame = JFrame()
        frame.title = "Mapsforge Samples"
        frame.add(mapView)
        frame.pack()
        frame.setSize(1024, 768)
        frame.setLocationRelativeTo(null)
        frame.defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        frame.addWindowListener(this)
        frame.isVisible = true
    }

    override fun windowOpened(e: WindowEvent?) {
        val model = mapView.model
        model.init(preferencesFacade)
        if (model.mapViewPosition.zoomLevel.toInt() == 0 || !boundingBox.contains(model.mapViewPosition.center)) {
            val zoomLevel =
                LatLongUtils.zoomForBounds(model.mapViewDimension.dimension, boundingBox, model.displayModel.tileSize)
            model.mapViewPosition.mapPosition = MapPosition(boundingBox.getCenterPoint(), zoomLevel)
        }
    }

    override fun windowClosed(e: WindowEvent?) {
        val result = JOptionPane.showConfirmDialog(
            frame,
            MESSAGE,
            TITLE,
            JOptionPane.YES_NO_OPTION
        )
        if (result == JOptionPane.YES_OPTION) {
            mapView.model.save(preferencesFacade)
            mapView.destroyAll()
            AwtGraphicFactory.clearResourceMemoryCache()
            frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        }
    }
}
