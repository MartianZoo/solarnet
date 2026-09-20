package dev.martianzoo.tfm.text

import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EnglishSyntaxRewriteTest {
  @Test
  internal fun factorsAHeadSharedByStructuredNouns() {
    val productions = listOf(production("steel"), production("heat"))

    factorSharedNounHead(productions, Conjunction.AND)?.linearize() shouldBe
        "steel and heat production"
  }

  @Test
  internal fun doesNotCollapseARepeatedStepOperation() {
    val steel = stepClause("increase", "steel", Determiner.YOUR)
    val heat = stepClause("increase", "heat", Determiner.YOUR)

    rewriteAdjacentClauses(listOf(steel, steel, heat)).single().linearize() shouldBe
        "increase your steel production 1 step and increase your steel production and " +
            "your heat production 1 step each"
  }

  @Test
  internal fun doesNotDistributeStepsAcrossDifferentPossessors() {
    val yours = stepClause("decrease", "M€", Determiner.YOUR)
    val anyPlayers = stepClause("decrease", "heat", Determiner.ANY_PLAYER_POSSESSIVE)

    rewriteAdjacentClauses(listOf(yours, anyPlayers)).single().linearize() shouldBe
        "decrease your M€ production 1 step and any player's heat production 1 step"
  }

  @Test
  internal fun attachesOnePurposeToCoordinatedCosts() {
    val costs =
        listOf(
            simpleClause("pay", "3 M€"),
            simpleClause("remove", "1 card"),
        )
    val result = simpleClause("gain", "1 plant")

    attachPurpose(costs, result).linearize() shouldBe "pay 3 M€ and remove 1 card to gain 1 plant"
  }

  private fun production(resource: String): NounPhrase =
      NounPhrase("production").withAttributiveModifier(NounPhrase.text(resource))

  private fun simpleClause(verb: String, objectPhrase: String): Clause.Simple =
      Clause.Simple(
          Predicate(
              Verb(verb),
              Coordination.one(NounPhrase.text(objectPhrase)),
          )
      )

  private fun stepClause(
      verb: String,
      resource: String,
      determiner: Determiner,
  ): Clause.Simple =
      Clause.Simple(
          Predicate(
              Verb(verb),
              Coordination.one(
                  production(resource).withDeterminer(determiner).withModifier(Modifier.Steps(1))
              ),
          )
      )
}
