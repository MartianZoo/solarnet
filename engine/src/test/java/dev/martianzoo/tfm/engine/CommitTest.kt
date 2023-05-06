package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.UserException.LimitsException
import dev.martianzoo.tfm.api.UserException.RequirementException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Game.PlayerAgentImpl
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.Transforming.replaceOwnerWith
import dev.martianzoo.tfm.pets.ast.Instruction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private class CommitTest {

  val game = Engine.newGame(Canon.SIMPLE_GAME)
  val p1 = game.asPlayer(PLAYER1)
  val ex = InstructionExecutor(game, p1 as PlayerAgentImpl, null)
  val c = Preparer(p1.reader, game.components)

  init {
    p1.sneakyChange(gaining = game.toComponent(parse("Plant<P1>")))
    p1.sneakyChange(10, gaining = game.toComponent(parse("ProjectCard<Player1>")))
    p1.sneakyChange(removing = game.toComponent(parse("Production<Player1, Class<Megacredit>>")))
  }

  fun prep(instr: Instruction): Instruction {
    return PetTransformer.chain(
            game.transformers.deprodify(),
            game.transformers.insertDefaults(),
            replaceOwnerWith(PLAYER1),
        )
        .transform(instr)
  }

  fun prepAndCommit(uncommitted: String) = c.toPreparedForm(prep(parse<Instruction>(uncommitted)))

  fun checkCommit(uncommitted: String, expected: String, abstract: Boolean = false) {
    val commit = prepAndCommit(uncommitted)
    assertThat(commit.toString()).isEqualTo(expected)
    assertThat(commit.isAbstract(p1.reader)).isEqualTo(abstract)
  }

  @Test
  fun testCommitChange() {
    checkCommit("Ok", "Ok")
    checkCommit("2 Plant", "2 Plant<Player1>!")
    checkCommit("2 Plant.", "2 Plant<Player1>!")
    checkCommit("2 Plant?", "2 Plant<Player1>?", true)
    checkCommit("-Plant", "-Plant<Player1>!")
    checkCommit("-9 Plant.", "-Plant<Player1>!")
    checkCommit("55 OxygenStep.", "14 OxygenStep!")
    checkCommit("-4 Heat.", "Ok")
    checkCommit("-4 Heat?", "Ok")
    checkCommit("Heat FROM Plant.", "Heat<Player1> FROM Plant<Player1>!")
    checkCommit("9 Heat FROM Plant?", "Heat<Player1> FROM Plant<Player1>?", true)
    checkCommit("Plant FROM Heat.", "Ok")
    checkCommit("Plant FROM Heat?", "Ok")
    assertThrows<LimitsException>("1") { prepAndCommit("15 OxygenStep!") }
    assertThrows<LimitsException>("2") { prepAndCommit("-2 Plant") }
    assertThrows<LimitsException>("3") { prepAndCommit("Plant FROM Heat") }
    assertThrows<LimitsException>("4") { prepAndCommit("2 Heat FROM Plant") }
    assertThrows<LimitsException>("5") { prepAndCommit("2 Plant<P2 FROM P1>") }
  }

  @Test
  fun testCommitPer() {
    checkCommit("Plant / TerraformRating", "20 Plant<Player1>!")
    checkCommit("Plant / 3 TerraformRating", "6 Plant<Player1>!")
    checkCommit("Plant / 3 TerraformRating MAX 2", "2 Plant<Player1>!")
    checkCommit("Plant / Steel", "Ok")
    checkCommit("Plant / 21 TerraformRating", "Ok")
    // checkCommit("Plant / 6 TR / 7 TR", "6 Plant<Player1>!") // TODO should work
  }

  @Test
  fun testCommitGated() {
    checkCommit("10 TR: Plant", "Plant<Player1>!")
    checkCommit("10 TR: Plant / TerraformRating", "20 Plant<Player1>!")
    // TODO I'm nervous about the <Anyone> disappearing
    checkCommit("10 TR: Plant<Anyone> / TerraformRating", "20 Plant!", true)
    checkCommit(
        "10 TR: (Titanium OR TerraformRating)",
        "Titanium<Player1>! OR TerraformRating<Player1>!",
        true,
    )
    // checkCommit("(10 TR: Plant) / TerraformRating", "20 Plant<Player1>!") // TODO as well
    assertThrows<RequirementException>("1") { prepAndCommit("30 TR: Plant") }
  }

  @Test
  fun testCommitOr() {
    checkCommit(
        "15 OxygenStep! OR -2 Plant OR Plant FROM Heat " +
            "OR Ok OR 2 Heat FROM Plant OR 2 Plant<P2 FROM P1> OR 30 TR: Plant",
        "Ok",
    )
    checkCommit(
        "15 OxygenStep! OR -2 Plant OR Plant FROM Heat " +
            "OR (TR: 8 Steel) OR 2 Heat FROM Plant OR 2 Plant<P2 FROM P1> OR 30 TR: Plant",
        "8 Steel<Player1>!",
    )
    checkCommit(
        "15 OxygenStep! OR -2 Plant OR Plant FROM Heat OR -Plant. / TR OR 8 Steel OR " +
            "2 Heat FROM Plant OR 2 Plant<P2 FROM P1> OR 30 TR: Plant",
        "-Plant<Player1>! OR 8 Steel<Player1>!",
        true,
    )
    checkCommit("PROD[Plant OR 3 PlantTag: 4 Plant]", "Production<Player1, Class<Plant>>!")
    assertThrows<Exception>("1") { // TODO what exception type is appropriate?
      prepAndCommit(
          "15 OxygenStep! OR -2 Plant OR Plant FROM Heat OR 2 Heat FROM Plant " +
              "OR 2 Plant<P2 FROM P1> OR 30 TR: Plant",
      )
    }
  }

  @Test
  fun testMulti() {
    assertThrows<IllegalStateException>("1") { prepAndCommit("Plant, Heat") }
    assertThrows<IllegalStateException>("2") { prepAndCommit("(TR: Plant), Heat") }
    assertThrows<IllegalStateException>("3") { prepAndCommit("TR: (Plant, Heat)") }
  }

  @Test
  fun testCustom() {
    checkCommit("@gainLowestProduction(Player1)", "@gainLowestProduction(Player1)")
    checkCommit("@gainLowestProduction(Anyone)", "@gainLowestProduction(Anyone)", true)
    // checkCommit("@gainLowestProduction(Player1)",
    //     "Production<Player1, Class<Megacredit>>!") // TODO
  }
}
