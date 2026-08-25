package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.*
import kotlin.test.Test

internal class DeterminedZeroCostActionTest : TfmTest() {
  @Test
  internal fun `an action remains usable when its determined cost is zero`() {
    val extension =
        object : TfmCatalog() {
          override val explicitClassDeclarations =
              parseClasses(
                      """
                      CLASS DeterminedZeroCostAction : ActionCard<Class<ProjectCard>> {
                        HAS MAX 1 This
                        cost = 0
                        UseAction<Owner, This, First>: -Megacredit<Owner> / CityTile<Anyone> THEN Plant<Owner>
                      }
                      """
                          .trimIndent()
                  )
                  .toSet()
        }
    val catalog = TfmCatalog.compose(Canon, extension)
    game = setUpGame(canonicalPremise(catalog = catalog))
    val p1 = game.tfm(PLAYER1)
    p1.godMode().manual("DeterminedZeroCostAction")

    p1.godMode().manual("UseAction<DeterminedZeroCostAction, First>").expect("Plant")
  }
}
