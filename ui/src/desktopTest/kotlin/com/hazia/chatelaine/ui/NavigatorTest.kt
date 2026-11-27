package com.hazia.chatelaine.ui

import com.hazia.chatelaine.ui.nav.Navigator
import com.hazia.chatelaine.ui.nav.Route
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigatorTest {

    /** R84: until the homepage exists, the app opens on Search and its results. */
    @Test
    fun startsOnSearch() {
        assertEquals(listOf<Route>(Route.Search), Navigator().stack.value)
    }

    @Test
    fun openPushesAndBackPops() {
        val nav = Navigator()
        nav.open(Route.Item("a"))
        assertEquals(listOf(Route.Search, Route.Item("a")), nav.stack.value)
        assertTrue(nav.back())
        assertEquals(listOf<Route>(Route.Search), nav.stack.value)
    }

    @Test
    fun backAtTheRootReturnsFalse() {
        val nav = Navigator()
        assertFalse(nav.back())
        assertEquals(listOf<Route>(Route.Search), nav.stack.value)
    }

    @Test
    fun openingTheTopRouteAgainDoesNotStack() {
        val nav = Navigator()
        nav.open(Route.Tags)
        nav.open(Route.Tags)
        assertEquals(listOf(Route.Search, Route.Tags), nav.stack.value)
    }

    @Test
    fun replaceTopSwapsTheTopAndKeepsTheSize() {
        val nav = Navigator()
        nav.open(Route.QuickAdd)
        nav.replaceTop(Route.Item("a"))
        assertEquals(listOf(Route.Search, Route.Item("a")), nav.stack.value)
    }
}
