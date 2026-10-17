package dev.martianzoo.tfm.web.almanac

import dev.martianzoo.search.CanonQuery

internal data class QueryExample(
    val section: String,
    val description: String,
    val xpath: String,
    val form: CanonQuery.Form = CanonQuery.Form.AUTHORED,
)
