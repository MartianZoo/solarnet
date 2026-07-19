@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.Type
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.tfm.data.ColonyTileDefinition
import dev.martianzoo.tfm.data.JsonReader
import dev.martianzoo.util.toSetStrict

/** The Colonies expansion rules currently supported by Canon. */
internal object ColoniesExpansion :
    CanonicalBundle(
        name = "ColoniesExpansion",
        legacyCode = "C",
        cards = true,
        actions = true,
    ) {
  override val explicitClassDeclarations: Set<ClassDeclaration> by lazy {
    parseClasses(readResource("colonies.pets")).toSetStrict()
  }

  override val colonyTileDefinitions: Set<ColonyTileDefinition> by lazy {
    JsonReader.readColonyTiles(readResource("colonies.json5"), bundleName.toString())
        .toSetStrict(::ColonyTileDefinition)
  }

  override val customClasses: Set<CustomClass> = setOf(AddColonyTile, ColoniesSetup)

  private object AddColonyTile : CustomClass("AddColonyTile") {
    override fun translate(reader: GameReader, tileClassType: Type): Instruction {
      val name = tileClassType.expression.arguments.single().className
      val tile = (reader.ruleset as TfmRuleset).colonyTile(name)
      return if (tile.resourceType == null) {
        parse("$name")
      } else {
        parse("DelayedColonyTile<Class<$name>, Class<${tile.resourceType}>>")
      }
    }
  }

  private object ColoniesSetup : CustomClass("ColoniesSetup") {
    override fun translate(reader: GameReader): Instruction {
      val tileInstructions =
          reader.setup.colonyTiles.map {
            parse<Instruction>("AddColonyTile<Class<${it.className}>>")
          }
      val fleetInstructions =
          reader.setup.players().mapIndexed { index, player ->
            val letter = 'A' + index
            parse<Instruction>("TradeFleet$letter<$player>")
          }
      return Then.create(tileInstructions + fleetInstructions)
    }
  }

  private fun readResource(filename: String): String =
      CanonResources.read("bundles/ColoniesExpansion/$filename")
}
