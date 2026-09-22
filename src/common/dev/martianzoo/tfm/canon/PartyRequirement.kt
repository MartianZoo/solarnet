package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.CustomMetric
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.Type
import dev.martianzoo.tfm.canon.ApiUtils.getPlayerOwner

/** Counts one when the player meets a party's printed project-card requirement. */
internal object PartyRequirement : CustomMetric() {
  private val PARTY = cn("Party")
  private val PARTY_DELEGATE = cn("PartyDelegate")
  private val RULING = cn("Ruling")

  override val requiredClassNames: Set<ClassName> = setOf(PARTY_DELEGATE, RULING)

  override fun count(game: GameReader, type: Type): Int {
    val partyType = game.resolve(PARTY.expression)
    val party = type.typeDependencies.map { it.boundType }.single { it.narrows(partyType, game) }
    val player = getPlayerOwner(game, type)
    val ruling = game.count(game.resolve(RULING.of(party.expression))) == 1
    val delegates = game.count(game.resolve(PARTY_DELEGATE.of(party.expression, player.expression)))
    return if (ruling || delegates >= 2) 1 else 0
  }
}
