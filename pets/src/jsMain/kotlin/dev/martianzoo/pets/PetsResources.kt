package dev.martianzoo.pets

import org.w3c.xhr.XMLHttpRequest

internal actual fun readPetsResource(filename: String): String {
  val resourceName = "pets/$filename"
  configuredResourceReader()?.let {
    return it(resourceName)
  }

  val request = XMLHttpRequest()
  request.open("GET", resourceName, false)
  request.send()
  if (request.status.toInt() in listOf(0, 200)) {
    return request.responseText
  }
  error("Unknown Pets resource: $filename")
}

private fun configuredResourceReader(): ((String) -> String)? {
  val reader = js("globalThis.solarnetResourceReader")
  return if (jsTypeOf(reader) == "function") reader.unsafeCast<(String) -> String>() else null
}
