package dev.martianzoo.repl

import java.io.File

internal object ReplPathCompletions {
  fun arguments(pathText: String): List<ReplCompletion> {
    val path = File(pathText.ifEmpty { "." })
    val dir =
        if (pathText.endsWith(File.separator) || path.isDirectory) {
          path
        } else {
          path.parentFile ?: File(".")
        }
    val prefix = if (pathText.endsWith(File.separator) || path.isDirectory) "" else path.name
    val base =
        if (pathText.endsWith(File.separator)) {
          pathText
        } else {
          pathText.substringBeforeLast(File.separator, "")
        }
    val prefixWithSlash = if (base.isEmpty()) "" else "$base${File.separator}"

    return dir
        .listFiles()
        ?.filter { it.name.startsWith(prefix, ignoreCase = true) }
        ?.map {
          val value = prefixWithSlash + it.name + if (it.isDirectory) File.separator else ""
          ReplCompletion(
              value,
              if (it.isDirectory) "directories" else "files",
              replaceFragment = false,
              complete = !it.isDirectory,
          )
        }
        ?: emptyList()
  }
}
