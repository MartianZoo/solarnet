package dev.martianzoo.tfm.canon

import org.w3c.xhr.XMLHttpRequest

internal actual fun readCanonResource(filename: String): String {
  val resourceName = "dev/martianzoo/tfm/canon/$filename"
  val request = XMLHttpRequest()
  request.open("GET", resourceName, false)
  request.send()
  if (request.status.toInt() in listOf(0, 200)) {
    return request.responseText
  }
  error("Unknown canon resource: $filename")
}
