package com.hazia.chatelaine.data

/**
 * What the search screen asks for. [text] is trimmed; blank means no text condition. Every id in [tagIds] must be
 * matched, by the tag itself or by one of its sub-tags. [unplacedOnly] keeps the items that have no place and are not away.
 */
data class SearchQuery(
    val text: String = "",
    val tagIds: Set<String> = emptySet(),
    val unplacedOnly: Boolean = false,
)

/** A tag with how many of some items carry it. */
data class TagCount(val tag: Tag, val items: Int)
