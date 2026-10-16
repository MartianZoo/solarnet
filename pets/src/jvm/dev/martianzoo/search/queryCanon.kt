package dev.martianzoo.search

import kotlin.system.exitProcess

internal fun main(args: Array<String>) {
  try {
    require(args.size in 3..4 && args[0] == "--form") {
      "Usage: query --form authored|elaborated [--xml] 'XPath expression'"
    }
    val form = CanonQuery.Form.entries.singleOrNull { it.name.lowercase() == args[1] }
    require(form != null) { "Choose --form authored or --form elaborated" }
    val asXml = args.size == 4
    require(!asXml || args[2] == "--xml") { "Expected --xml before the XPath expression" }
    val query = CanonQuery(form)
    query.evaluate(args.last(), includeXml = asXml).forEach {
      println(if (asXml) it.xml ?: it.value else it.describe())
    }
  } catch (e: Exception) {
    val cause = generateSequence<Throwable>(e) { it.cause }.last()
    System.err.println("query: ${cause.message ?: cause::class.simpleName}")
    exitProcess(1)
  }
}
