package dev.martianzoo.state

import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.GamePremise
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Opaque JSON encoding for the data needed to open a passive recording. */
public object GameRecordingJson {
  private val json = Json

  /** Encodes the premise recipe, viewer positions, and exact events in one JSON value. */
  public fun encode(recording: GameRecording): String {
    val premise = recording.premise
    val selectedClasses = buildList {
      premise.modules.mapTo(this) { it.toString() }
      (premise.catalog.modules.keys - premise.modules).mapTo(this) { "-$it" }
      premise.classSelections.mapTo(this) { selection ->
        if (selection.included) selection.className.toString() else "-${selection.className}"
      }
    }
        .sorted()
    return encodeJson(
        buildJsonObject {
          put("classes", strings(selectedClasses))
          put("players", strings(recording.premise.playerNames.map { it.toString() }))
          put(
              "positions",
              buildJsonArray {
                recording.positions.forEach { add(JsonPrimitive(it.ordinal)) }
              },
          )
          put("events", EventLogJson.encodeEvents(recording.events))
        }
    )
  }

  /** Parses one recording document for premise reconstruction and typed event decoding. */
  public fun parse(text: String): Document = Document(json.parseToJsonElement(text).jsonObject)

  /** One parsed recording document retained across premise reconstruction and event decoding. */
  public class Document internal constructor(private val source: JsonObject) {
    /** The premise recipe that must be reconstructed before typed events can be decoded. */
    public val config: GameConfig = GameRecordingJson.config(source)

    /** Decodes the exact event stream against its reconstructed [premise]. */
    public fun decode(premise: GamePremise): GameRecording {
      val events =
          EventLogJson.decodeEvents(
              GameRecordingJson.requiredArray(source, "events"),
              premise,
          )
      val positions =
          GameRecordingJson.requiredArray(source, "positions").map {
            Checkpoint(it.jsonPrimitive.int)
          }
      return GameRecording(premise, events, positions)
    }
  }

  private fun config(source: JsonObject): GameConfig =
      GameConfig(
          requiredStrings(source, "classes").joinToString(),
          *requiredStrings(source, "players").toTypedArray(),
      )

  private fun strings(values: Iterable<String>) = buildJsonArray {
    values.forEach { add(JsonPrimitive(it)) }
  }

  private fun requiredStrings(source: JsonObject, name: String): List<String> =
      requiredArray(source, name).map { element -> element.jsonPrimitive.content }

  private fun requiredArray(source: JsonObject, name: String): JsonArray =
      requireNotNull(source[name]) { "missing field: $name" }.jsonArray

  private fun encodeJson(source: JsonObject): String =
      json.encodeToString(JsonObject.serializer(), source)
}
