package com.dream.wowiptv.presentation.navigation

object Routes {
    const val MAIN = "main"
    const val ALL_ITEMS = "all_items/{tab}"
    const val ALL_FAVORITES = "all_favorites"
    const val SETTINGS = "settings"

    fun allItemsRoute(tab: Int) = "all_items/$tab"
    const val PLAYER = "player/{streamType}/{streamId}?name={name}&position={position}"
    const val EPG = "epg/{streamId}"
    const val VOD = "vod/{vodId}"
    const val SERIES = "series/{seriesId}"
    const val SOURCE_ADD = "source_add"
    const val SOURCE_EDIT = "source_edit/{sourceId}"

    fun playerRoute(streamType: String, streamId: String, name: String = "", position: Long = 0L) =
        "player/$streamType/$streamId?name=${java.net.URLEncoder.encode(name, "UTF-8")}&position=$position"
    fun epgRoute(streamId: Int) = "epg/$streamId"
    fun vodRoute(vodId: Int) = "vod/$vodId"
    fun seriesRoute(seriesId: Int) = "series/$seriesId"
    fun sourceEditRoute(sourceId: Int) = "source_edit/$sourceId"
}
