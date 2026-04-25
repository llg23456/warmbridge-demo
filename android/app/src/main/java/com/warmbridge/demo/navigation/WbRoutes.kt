package com.warmbridge.demo.navigation

/** 与《家长端底部导航-产品方案》§4.2 一致，避免手写字符串散落。 */
object WbRoutes {
    const val Role = "role"
    const val Parent = "parent"
    const val Child = "child"

    const val ParentHome = "parent_home"
    const val ParentHot = "parent_hot"
    const val ParentMine = "parent_mine"

    const val ChildHome = "child_home"
    const val ChildHot = "child_hot"
    const val ChildMine = "child_mine"

    const val Reminder = "reminder"
    const val Share = "share"
    const val ImageExplain = "image_explain"
    const val VideoQuick = "video_quick"

    fun detail(id: String) = "detail/$id"
}
