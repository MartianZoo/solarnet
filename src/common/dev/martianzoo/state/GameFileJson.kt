package dev.martianzoo.state

import dev.martianzoo.catalog.Catalog
import dev.martianzoo.catalog.ClassSelection
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Requirement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * A version-locked game file. Every file has the resolved premise recipe and an ordered input
 * sequence beginning with the game's start. Full files also contain exact events with transaction
 * metadata, from which [Document.open] derives playback positions without an engine. Short files
 * omit events and require an Agent-based importer to execute the inputs. No recorded event is used
 * to repair an Agent replay. Encoding and hash-checked imports suspend while the browser computes
 * SHA-256 with Web Crypto, which requires a secure context.
 */
public object GameFileJson {
  private const val FORMAT = "solarnet-game-3"
  private val json = Json

  /**
   * Encodes [recording] and [inputs]. With [short] true, omits events while retaining the same
   * premise and input fields. [inputs] must start the game exactly once, first.
   */
  public suspend fun encode(
      recording: GameRecording,
      inputs: List<RecordedInput>,
      short: Boolean = false,
  ): String {
    require(inputs.firstOrNull() == RecordedInput.StartGame) { "a game file must start its game" }
    require(inputs.drop(1).none { it == RecordedInput.StartGame }) {
      "a game file cannot start its game twice"
    }
    val premise = recording.premise
    val catalogHash = GameFingerprint.catalog(premise.catalog)
    val premiseHash = GameFingerprint.premise(premise)
    val source = buildJsonObject {
      put("format", FORMAT)
      put("catalogHash", catalogHash)
      put("premiseHash", premiseHash)
      put(
          "selections",
          buildJsonArray {
            premise.classSelections
                .sortedBy { it.className.toString() }
                .forEach { selection ->
                  add(
                      buildJsonObject {
                        put("class", selection.className.toString())
                        put("included", selection.included)
                        put(
                            "requirement",
                            selection.requirement?.let { JsonPrimitive(it.toString()) } ?: JsonNull,
                        )
                      }
                  )
                }
          },
      )
      put(
          "adjustments",
          buildJsonObject {
            premise.componentAdjustments.entries
                .sortedBy { it.key.toString() }
                .forEach { (name, count) ->
                  put(name.toString(), count)
                }
          },
      )
      put("players", strings(premise.playerNames.map { it.toString() }))
      put("bootstrap", premise.bootstrapClassName?.let { JsonPrimitive(it.toString()) } ?: JsonNull)
      put(
          "premiseClass",
          premise.premiseClassName?.let { JsonPrimitive(it.toString()) } ?: JsonNull,
      )
      put(
          "localDeclarations",
          strings(premise.premiseClassDeclarations.map { it.toString() }.sorted()),
      )
      put("inputs", buildJsonArray { inputs.forEach { add(encodeInput(it)) } })
      if (!short) {
        put("events", EventLogJson.encodeEvents(recording.events))
      }
    }
    return json.encodeToString(JsonObject.serializer(), source)
  }

  /** Parses a file's envelope; the caller supplies its expected Catalog for import. */
  public fun parse(text: String): Document = Document(json.parseToJsonElement(text).jsonObject)

  /** A parsed game file whose fingerprints are checked before its events or inputs are decoded. */
  public class Document internal constructor(private val source: JsonObject) {
    init {
      require(requiredString(source, "format") == FORMAT) { "unsupported game-file format" }

      val inputs = requiredArray(source, "inputs")
      require(inputs.firstOrNull()?.jsonObject?.get("kind")?.jsonPrimitive?.content == "start") {
        "a game file must start its game"
      }
      require(inputs.drop(1).none { it.jsonObject["kind"]?.jsonPrimitive?.content == "start" }) {
        "a game file cannot start its game twice"
      }
    }

    /**
     * The resolved selections rendered as a [GameConfig] for inspection. This is not necessarily
     * the caller's original setup request; [premise] reconstructs the exact resolved premise.
     */
    public val config: GameConfig =
        GameConfig(
            (selections().map { if (it.included) it.className.toString() else "-${it.className}" } +
                    requiredObject(source, "adjustments").map { (name, count) ->
                      "${count.jsonPrimitive.int} $name"
                    })
                .joinToString(),
            *requiredStrings(source, "players").toTypedArray(),
        )

    /** Whether this file contains an exact recording that [open] can passively materialize. */
    public val hasEvents: Boolean
      get() = source.containsKey("events")

    /** Reconstructs the resolved premise from [catalog], refusing either data-hash mismatch. */
    public suspend fun premise(catalog: Catalog): GamePremise {
      require(GameFingerprint.catalog(catalog) == requiredString(source, "catalogHash")) {
        "game-file Catalog hash differs from the supplied Catalog"
      }
      val local = requiredStrings(source, "localDeclarations").flatMap(::parseClasses).toSet()
      return GamePremise(
              catalog = catalog,
              classSelections = selections(),
              componentAdjustments = config.componentAdjustments,
              playerNames = config.playerNames,
              bootstrapClassName = optionalClassName(source, "bootstrap"),
              premiseClassName = optionalClassName(source, "premiseClass"),
              premiseClassDeclarations = local,
          )
          .also { verifyPremise(it) }
    }

    /** Decodes the exact recording; a short file cannot be imported by state alone. */
    public suspend fun decode(premise: GamePremise): GameRecording {
      verifyPremise(premise)
      require(hasEvents) { "short game file has no exact events for state import" }
      val events = EventLogJson.decodeEvents(requiredArray(source, "events"), premise)
      return GameRecording(premise, events)
    }

    /** Opens a full recording without interpreting its instructions or running effects. */
    public suspend fun open(catalog: Catalog): GameRecording.Playback =
        decode(premise(catalog)).open()

    /** Decodes the chronological inputs against their checked Actor namespace. */
    public suspend fun inputs(premise: GamePremise): List<RecordedInput> {
      verifyPremise(premise)
      val actors = premise.actors.associateBy { it.className.toString() }
      return requiredArray(source, "inputs").map { value ->
        val input = value.jsonObject
        when (requiredString(input, "kind")) {
          "start" -> RecordedInput.StartGame
          "command" ->
              RecordedInput.Command(
                  requireNotNull(actors[requiredString(input, "actor")]) {
                    "unknown Actor in game file"
                  },
                  RecordedInput.Kind.valueOf(requiredString(input, "command")),
                  requiredString(input, "instruction"),
                  requiredArray(input, "cardInputs").map { cardInput ->
                    val card = cardInput.jsonObject
                    RecordedInput.CardInput(
                        requireNotNull(actors[requiredString(card, "player")] as? Player) {
                          "unknown card-input Player in game file"
                        },
                        requiredString(card, "back"),
                        RecordedInput.CardMovement.valueOf(requiredString(card, "movement")),
                        requiredStrings(card, "names"),
                    )
                  },
                  input["taskSelector"]?.jsonObject?.let { selector ->
                    RecordedInput.TaskSelector(
                        requiredString(selector, "instruction"),
                        requiredArray(selector, "cause").map { step ->
                          val value = step.jsonObject
                          RecordedInput.CauseStep(
                              requiredString(value, "context"),
                              requiredString(value, "actor"),
                              requiredString(value, "change"),
                          )
                        },
                        requireNotNull(selector["occurrence"]).jsonPrimitive.int,
                    )
                  },
              )
          else -> error("unknown game input")
        }
      }
    }

    private suspend fun verifyPremise(premise: GamePremise) {
      require(GameFingerprint.catalog(premise.catalog) == requiredString(source, "catalogHash")) {
        "game-file Catalog hash differs from the supplied Catalog"
      }
      require(GameFingerprint.premise(premise) == requiredString(source, "premiseHash")) {
        "game-file premise hash differs from the supplied premise"
      }
    }

    private fun selections(): Set<ClassSelection> =
        requiredArray(source, "selections").mapTo(linkedSetOf()) { value ->
          val entry = value.jsonObject
          ClassSelection(
              parse<ClassName>(requiredString(entry, "class")),
              requireNotNull(entry["included"]).jsonPrimitive.boolean,
              when (val requirement = requireNotNull(entry["requirement"])) {
                JsonNull -> null
                else -> parse<Requirement>(requirement.jsonPrimitive.content)
              },
          )
        }
  }

  private fun encodeInput(input: RecordedInput): JsonObject = buildJsonObject {
    when (input) {
      is RecordedInput.StartGame -> put("kind", "start")
      is RecordedInput.Command -> {
        put("kind", "command")
        put("actor", input.actor.className.toString())
        put("command", input.kind.name)
        put("instruction", input.instruction)
        put(
            "cardInputs",
            buildJsonArray {
              input.cardInputs.forEach { card ->
                add(
                    buildJsonObject {
                      put("player", card.player.className.toString())
                      put("back", card.back)
                      put("movement", card.movement.name)
                      put("names", strings(card.names))
                    }
                )
              }
            },
        )
        input.taskSelector?.let { selector ->
          put(
              "taskSelector",
              buildJsonObject {
                put("instruction", selector.instruction)
                put("occurrence", selector.occurrence)
                put(
                    "cause",
                    buildJsonArray {
                      selector.cause.forEach { step ->
                        add(
                            buildJsonObject {
                              put("context", step.context)
                              put("actor", step.actor)
                              put("change", step.change)
                            }
                        )
                      }
                    },
                )
              },
          )
        }
      }
    }
  }

  private fun strings(values: Iterable<String>): JsonArray = buildJsonArray {
    values.forEach { add(JsonPrimitive(it)) }
  }

  private fun requiredString(source: JsonObject, name: String): String =
      requireNotNull(source[name]) { "missing field: $name" }.jsonPrimitive.content

  private fun requiredArray(source: JsonObject, name: String): JsonArray =
      requireNotNull(source[name]) { "missing field: $name" }.jsonArray

  private fun requiredObject(source: JsonObject, name: String): JsonObject =
      requireNotNull(source[name]) { "missing field: $name" }.jsonObject

  private fun requiredStrings(source: JsonObject, name: String): List<String> =
      requiredArray(source, name).map { it.jsonPrimitive.content }

  private fun optionalClassName(source: JsonObject, name: String): ClassName? =
      when (val value = requireNotNull(source[name]) { "missing field: $name" }) {
        JsonNull -> null
        else -> parse<ClassName>(value.jsonPrimitive.content)
      }
}
