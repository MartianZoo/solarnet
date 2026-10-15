package dev.martianzoo.state

import dev.martianzoo.catalog.Catalog
import dev.martianzoo.catalog.ClassSelection
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.data.ClassDeclaration
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Stable data fingerprints for the lockstep game-file format. */
internal object GameFingerprint {
  /** The complete static Catalog data visible to games, including unselected declarations. */
  suspend fun catalog(catalog: Catalog): String =
      digest(
          buildJsonObject {
            put("declarations", declarations(catalog.allClassDeclarations.values))
            put("authoredDeclarations", declarations(catalog.explicitClassDeclarations))
            put(
                "modules",
                buildJsonObject {
                  catalog.modules.entries
                      .sortedBy { it.key.toString() }
                      .forEach { (name, values) ->
                        put(name.toString(), selections(values))
                      }
                },
            )
            put(
                "availability",
                buildJsonObject {
                  catalog.classAvailabilityModules.entries
                      .sortedBy { it.key.toString() }
                      .forEach { (name, modules) ->
                        put(name.toString(), strings(modules.map { it.toString() }.sorted()))
                      }
                },
            )
            put("transforms", strings(catalog.transformHandlerFactories.keys.sorted()))
            put(
                "metadata",
                buildJsonObject {
                  catalog.fingerprintMetadata.entries
                      .sortedBy { it.key }
                      .forEach { (name, value) ->
                        put(name, value)
                      }
                },
            )
            put(
                "displayNames",
                buildJsonObject {
                  catalog.displayNamesByLanguage.entries
                      .sortedBy { it.key }
                      .forEach { (language, names) ->
                        put(
                            language,
                            buildJsonObject {
                              names.entries
                                  .sortedBy { it.key.toString() }
                                  .forEach { (name, display) ->
                                    put(name.toString(), display)
                                  }
                            },
                        )
                      }
                },
            )
          }
      )

  /** Exact resolved configuration, including declarations local to this game. */
  suspend fun premise(premise: GamePremise): String =
      digest(
          buildJsonObject {
            put("selections", selections(premise.classSelections))
            put(
                "adjustments",
                buildJsonObject {
                  premise.componentAdjustments.entries
                      .sortedBy { it.key.toString() }
                      .forEach { (name, amount) ->
                        put(name.toString(), amount)
                      }
                },
            )
            put("players", strings(premise.playerNames.map { it.toString() }))
            put("bootstrap", nullable(premise.bootstrapClassName?.toString()))
            put("premiseClass", nullable(premise.premiseClassName?.toString()))
            put("localDeclarations", declarations(premise.premiseClassDeclarations))
          }
      )

  private fun declarations(values: Collection<ClassDeclaration>): JsonArray = buildJsonArray {
    values
        .sortedBy { it.className.toString() }
        .forEach { declaration ->
          add(
              buildJsonObject {
                // ClassDeclaration renders parseable Pets source. Include the executable lowering
                // and
                // extra nodes separately because its source rendering does not contain those
                // values.
                put("source", declaration.toString())
                put("effects", strings(declaration.effects.map { it.toString() }))
                put("extraNodes", strings(declaration.extraNodes.map { it.toString() }.sorted()))
              }
          )
        }
  }

  private fun selections(values: Collection<ClassSelection>): JsonArray = buildJsonArray {
    values
        .sortedWith(
            compareBy({ it.className.toString() }, { it.included }, { it.requirement.toString() })
        )
        .forEach { selection ->
          add(
              buildJsonObject {
                put("class", selection.className.toString())
                put("included", selection.included)
                put("requirement", nullable(selection.requirement?.toString()))
              }
          )
        }
  }

  private fun strings(values: Iterable<String>): JsonArray = buildJsonArray {
    values.forEach { add(JsonPrimitive(it)) }
  }

  private fun nullable(value: String?): JsonElement = value?.let(::JsonPrimitive) ?: JsonNull

  private suspend fun digest(value: JsonObject): String = sha256(value.toString())
}
