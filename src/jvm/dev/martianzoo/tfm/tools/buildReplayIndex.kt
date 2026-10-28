package dev.martianzoo.tfm.tools

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.GameFileJson
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.fake.FakeCanon
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Extracts chooser metadata once at build time; browsing the list need not open any game. */
internal suspend fun replayIndex(directory: Path): String {
  val files =
      Files.list(directory).use { paths ->
        paths
            .filter { it.toString().endsWith(".json") && !it.toString().endsWith(".short.json") }
            .sorted()
            .toList()
      }
  val entries = mutableListOf<JsonObject>()
  for (file in files) {
    val name = file.fileName.toString().removeSuffix(".json")
    val document = GameFileJson.parse(Files.readString(file))
    val config = document.config
    val catalog =
        if (
            FakeCanon.bundles.any { it.bundleName in config.includedCardBundles } ||
                config.includedClassNames.any { it in FakeCanon.allClassNames }
        ) {
          TfmCatalog(Canon, FakeCanon)
        } else Canon
    val world = document.open(catalog).world
    val reader = world.reader
    val finished = world.tasks.isEmpty()
    val scores =
        if (finished) {
          reader.actors
              .filterIsInstance<Player>()
              .map {
                it.className.toString() to
                    reader.count(reader.resolve(cn("VictoryPoint").of(it.expression)))
              }
              .sortedByDescending { it.second }
              .joinToString { (player, score) -> "$player $score" }
        } else null
    entries += buildJsonObject {
      put("name", name)
      put("config", Files.readString(directory.resolve("$name.config.txt")))
      put("scores", scores)
    }
  }
  return JsonArray(entries).toString()
}

public fun main(args: Array<String>): Unit = runBlocking {
  require(args.size == 2) { "Usage: buildReplayIndex <recordings-directory> <index.json>" }
  val output = Path.of(args[1])
  val index = replayIndex(Path.of(args[0]))
  Files.createDirectories(output.toAbsolutePath().parent)
  Files.writeString(output, index)
  println("Wrote replay chooser index to $output")
}
