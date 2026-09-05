package dev.martianzoo.tools

import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Path
import java.security.MessageDigest

private const val DETEKT_CLASSPATH_PROPERTY = "solarnet.kotlinFileComplexity.detektClasspath"

public fun main(args: Array<String>) {
  require(args.size >= 3) { "Expected: ROOT REPORT STATE [KOTLIN_SOURCE ...]" }
  val root = File(args[0]).toPath().toAbsolutePath().normalize()
  val reportFile = File(args[1])
  val stateFile = File(args[2])
  val sourceFiles = args.drop(3).map(::File)
  val classpath =
      requireNotNull(System.getProperty(DETEKT_CLASSPATH_PROPERTY)) {
            "$DETEKT_CLASSPATH_PROPERTY is not set"
          }
          .split(File.pathSeparator)
          .map(::File)

  writeComplexityReport(root, reportFile, stateFile, sourceFiles, classpath)
}

private fun writeComplexityReport(
    root: Path,
    reportFile: File,
    stateFile: File,
    sourceFiles: List<File>,
    analyzerClasspath: List<File>,
) {
  KotlinFileComplexityAnalyzer(analyzerClasspath).use { analyzer ->
    val previous = readState(stateFile, analyzer.implementationFingerprint)
    val hashes = sourceFiles.associate { file ->
      root.relativePath(file) to sha256(file.readBytes())
    }
    val current = mutableMapOf<String, Pair<String, Int?>>()
    var analyzedFiles = 0

    hashes.toSortedMap().forEach { (path, hash) ->
      val previousEntry = previous[path]
      if (previousEntry?.first == hash) {
        current[path] = previousEntry
      } else {
        val complexity = analyzer.analyze(path, root.resolve(path).toFile().readText(UTF_8))
        current[path] = hash to complexity
        analyzedFiles++
      }
    }

    writeState(stateFile, analyzer.implementationFingerprint, current)
    writeReport(reportFile, current)
    val reportedFiles = current.count { it.value.second != null }
    println(
        "Wrote $reportedFiles Kotlin file complexities to ${reportFile.absolutePath} " +
            "($analyzedFiles files analyzed)"
    )
  }
}

private fun readState(file: File, expectedFingerprint: String): Map<String, Pair<String, Int?>> {
  if (!file.isFile) return emptyMap()
  return file.useLines { lines ->
    val iterator = lines.iterator()
    if (!iterator.hasNext() || iterator.next() != "fingerprint\t$expectedFingerprint") {
      return@useLines emptyMap()
    }
    iterator.asSequence().associate { line ->
      val (path, hash, complexity) = line.split('\t', limit = 3)
      path to (hash to complexity.ifEmpty { null }?.toInt())
    }
  }
}

private fun writeState(
    file: File,
    fingerprint: String,
    entries: Map<String, Pair<String, Int?>>,
) {
  file.parentFile.mkdirs()
  file.bufferedWriter().use { writer ->
    writer.append("fingerprint\t").appendLine(fingerprint)
    entries.toSortedMap().forEach { (path, value) ->
      writer
          .append(path)
          .append('\t')
          .append(value.first)
          .append('\t')
          .appendLine(value.second?.toString().orEmpty())
    }
  }
}

private fun writeReport(file: File, entries: Map<String, Pair<String, Int?>>) {
  file.parentFile.mkdirs()
  file.bufferedWriter().use { writer ->
    writer.appendLine("path\tcyclomatic_complexity")
    entries.toSortedMap().forEach { (path, value) ->
      value.second?.let { complexity ->
        writer.append(path).append('\t').appendLine(complexity.toString())
      }
    }
  }
}

private fun Path.relativePath(file: File): String {
  val path = file.toPath().toAbsolutePath().normalize()
  require(path.startsWith(this)) { "Kotlin source is outside the project: $file" }
  return relativize(path).joinToString("/")
}

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
