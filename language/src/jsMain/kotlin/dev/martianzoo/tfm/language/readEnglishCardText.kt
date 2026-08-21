package dev.martianzoo.tfm.language

import org.w3c.xhr.XMLHttpRequest

internal actual fun readEnglishFilteredDraws(): String {
  return readEnglishResource("english-filtered-draws.tsv", "filtered-draw")
}

internal fun readEnglishResource(fileName: String, description: String): String {
  val request = XMLHttpRequest()
  request.open("GET", "language/$fileName", false)
  request.send()
  if (request.status.toInt() in listOf(0, 200)) return request.responseText
  error("Missing English $description data")
}
