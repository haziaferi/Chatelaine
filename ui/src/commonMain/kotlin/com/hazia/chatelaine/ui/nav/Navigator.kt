package com.hazia.chatelaine.ui.nav

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    /** Shows [route] on top; opening the route already on top does nothing. */
    fun open(route: Route) {
        _stack.value.let { if (it.last() != route) _stack.value = it + route }
    }

    /** Goes back one step; false at the root, where the stack stays as it is. */
    fun back(): Boolean {
        val current = _stack.value
        if (current.size <= 1) return false
        _stack.value = current.dropLast(1)
        return true
    }

    /** Swaps the route on top for [route]. */
    fun replaceTop(route: Route) {
        _stack.value = _stack.value.dropLast(1) + route
    }
}
