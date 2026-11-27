package com.hazia.chatelaine.ui.nav

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** The places the stage 2 app can show. */
sealed interface Route {
    data object Search : Route
    data object QuickAdd : Route
    data class Item(val id: String) : Route
    data object Tags : Route
    data object Settings : Route
}

/** A back stack of [Route]s; the last one is on screen. R84: until the homepage exists, it starts on Search. */
class Navigator(start: Route = Route.Search) {
    private val _stack = MutableStateFlow(listOf(start))
    val stack: StateFlow<List<Route>> = _stack.asStateFlow()

    // Each change goes through update {}, so two changes at once cannot lose one of them.

    /** Shows [route] on top; opening the route already on top does nothing. */
    fun open(route: Route) {
        _stack.update { if (it.last() == route) it else it + route }
    }

    /** Goes back one step; false at the root, where the stack stays as it is. */
    fun back(): Boolean {
        var popped = false
        _stack.update {
            // update {} may run this more than once; the last run decides.
            popped = it.size > 1
            if (popped) it.dropLast(1) else it
        }
        return popped
    }

    /** Swaps the route on top for [route]. */
    fun replaceTop(route: Route) {
        _stack.update { it.dropLast(1) + route }
    }
}
