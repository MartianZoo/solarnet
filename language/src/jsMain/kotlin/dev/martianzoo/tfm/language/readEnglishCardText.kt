package dev.martianzoo.tfm.language

import org.w3c.xhr.XMLHttpRequest

internal actual fun readEnglishCardText(): String {
  val request = XMLHttpRequest()
  request.open("GET", "language/english-card-text.tsv", false)
  request.send()
  if (request.status.toInt() in listOf(0, 200)) return request.responseText
  error("Missing English card-text data")
}
