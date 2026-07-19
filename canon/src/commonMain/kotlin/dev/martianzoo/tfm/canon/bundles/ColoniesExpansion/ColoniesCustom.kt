@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon.bundles.ColoniesExpansion

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.Type
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.tfm.api.TfmRuleset

internal object AddColonyTile : CustomClass("AddColonyTile") {
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
