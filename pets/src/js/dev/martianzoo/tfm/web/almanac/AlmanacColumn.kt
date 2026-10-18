package dev.martianzoo.tfm.web.almanac

import dev.martianzoo.pets.types.Class
import dev.martianzoo.search.CanonQuery

internal sealed interface AlmanacColumn {
  data class Details(val klass: Class) : AlmanacColumn

  data class Results(val klass: Class, val group: String) : AlmanacColumn

  data object Examples : AlmanacColumn

  data class Query(val xpath: String, val form: CanonQuery.Form) : AlmanacColumn
}
