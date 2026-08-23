package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.api.TfmAuthority
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

internal class DeterminedZeroCostActionTest : TfmTest() {
  @Test
  internal fun `an action remains usable when its determined cost is zero`() {
    val extension =
        object : TfmAuthority() {
          override val explicitClassDeclarations =
              parseClasses(
                      """
                      CLASS DeterminedZeroCostAction : ActionCard {
                        HAS MAX 1 This
                        cost = 0
                        UseAction<Owner, This, First>: -Megacredit<Owner> / CityTile<Anyone> THEN Plant<Owner>
                      }
                      """
                          .trimIndent()
                  )
                  .toSet()
        }
    val authority = TfmAuthority.compose(Canon, extension)
    game = setUpGame(canonicalPremise(authority = authority))
    val p1 = game.tfm(PLAYER1)
    p1.godMode().manual("DeterminedZeroCostAction")

    p1.godMode().manual("UseAction<DeterminedZeroCostAction, First>").expect("Plant")
  }
}
