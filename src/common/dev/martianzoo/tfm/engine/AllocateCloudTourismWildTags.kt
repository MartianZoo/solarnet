@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.engine

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.CustomInstruction
import dev.martianzoo.state.GameReader

internal object AllocateCloudTourismWildTags : CustomInstruction() {
  override fun translate(game: GameReader, player: Type): InstructionTree {
    val unused =
        game
            .getComponents(cn("WildTag").of(player.expression))
            .elements
            .filter { game.count(game.resolve(cn("Tag").of(it.expression))) == 0 }
            .sortedBy { it.expression.toString() }
    val earth = game.count(game.resolve(cn("EarthTag").of(player.expression)))
    val venus = game.count(game.resolve(cn("VenusTag").of(player.expression)))
    val target = minOf(earth + unused.size, venus + unused.size, (earth + venus + unused.size) / 2)
    val tags =
        List((target - earth).coerceAtLeast(0)) { cn("EarthTag") } +
            List((target - venus).coerceAtLeast(0)) { cn("VenusTag") }
    return InstructionGroup.of(tags.zip(unused) { tag, wild -> Gain.gain(tag.of(wild.expression)) })
  }
}
