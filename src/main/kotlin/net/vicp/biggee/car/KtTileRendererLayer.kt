package net.vicp.biggee.car

import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Point
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.hills.HillsRenderConfig
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.model.IMapViewPosition

class KtTileRendererLayer(tileCache: TileCache, mapDataStore: MapDataStore, mapViewPosition: IMapViewPosition, hillsRenderConfig: HillsRenderConfig?): TileRendererLayer(tileCache,mapDataStore,mapViewPosition,false,true,true,Core.GRAPHIC_FACTORY,hillsRenderConfig) {
    override fun onTap(tapLatLong: LatLong?, layerXY: Point?, tapXY: Point?): Boolean {
        return super.onTap(tapLatLong, layerXY, tapXY)
    }
}
