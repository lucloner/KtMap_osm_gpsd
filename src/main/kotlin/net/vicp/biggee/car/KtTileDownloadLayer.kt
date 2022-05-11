package net.vicp.biggee.car

import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Point
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.download.TileDownloadLayer
import org.mapsforge.map.layer.download.tilesource.TileSource
import org.mapsforge.map.model.IMapViewPosition

class KtTileDownloadLayer(tileCache: TileCache, mapViewPosition: IMapViewPosition, tileSource: TileSource): TileDownloadLayer(tileCache,mapViewPosition,tileSource,Core.GRAPHIC_FACTORY) {

    override fun onTap(tapLatLong: LatLong?, layerXY: Point?, tapXY: Point?): Boolean {
        return super.onTap(tapLatLong, layerXY, tapXY)
    }
}
