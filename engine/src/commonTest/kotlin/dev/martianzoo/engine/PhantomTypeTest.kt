package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.api.TfmAuthority
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.canonicalPremise
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PhantomTypeTest {
  private fun gameplay() = Engine.newGame(canonicalPremise()).gameplay(ENGINE).godMode()

  @Test
  fun `phantom types and their class literals count zero`() {
    val game = Engine.newGame(canonicalPremise())
    val gameplay = game.gameplay(ENGINE).godMode()
    val venusTag = gameplay.resolve("VenusTag")

    gameplay.count("VenusTag") shouldBe 0
    gameplay.count("Class<VenusTag>") shouldBe 0
    venusTag.phantom shouldBe true
    gameplay.resolve("Class<VenusTag>").phantom shouldBe true
    game.reader.count(venusTag) shouldBe 0
    game.reader.containsAny(venusTag) shouldBe false
    game.reader.countComponent(venusTag) shouldBe 0
    game.reader.getComponents(venusTag).isEmpty() shouldBe true
  }

  @Test
  fun `unknown names remain errors`() {
    val gameplay = gameplay()

    shouldThrow<ExpressionException> { gameplay.count("Typo") }
    shouldThrow<ExpressionException> { gameplay.count("Class<Typo>") }
  }

  @Test
  fun `optional and amap phantom changes do nothing while mandatory changes die`() {
    val gameplay = gameplay()

    gameplay.manual("VenusTag?")
    gameplay.manual("VenusTag.")
    gameplay.manual("-VenusTag?")
    gameplay.manual("-VenusTag.")
    shouldThrow<DeadEndException> { gameplay.manual("VenusTag!") }
    shouldThrow<DeadEndException> { gameplay.manual("-VenusTag!") }
    gameplay.count("VenusTag") shouldBe 0
  }

  @Test
  fun `choices discard mandatory phantom branches`() {
    val gameplay = gameplay()

    gameplay.manual("VenusTag! OR Plant<Player1>!")

    gameplay.count("Plant<Player1>") shouldBe 1
  }

  @Test
  fun `optional phantom changes in component effects normalize to no-op`() {
    val probeAuthority =
        object : TfmAuthority() {
          override val explicitClassDeclarations =
              parseClasses(
                      """
                      CLASS PhantomEffectProbe {
                        HAS =1 This
                        This: VenusTag?
                        This: VenusTag.
                        VenusTag: Plant<Player1>!
                      }
                      """
                          .trimIndent()
                  )
                  .toSet()
        }
    val premise = canonicalPremise(authority = TfmAuthority.Composite(Canon, probeAuthority))

    val gameplay = Engine.newGame(premise).gameplay(ENGINE).godMode()

    gameplay.count("PhantomEffectProbe") shouldBe 1
    gameplay.count("VenusTag") shouldBe 0
    gameplay.count("Plant<Player1>") shouldBe 0
  }
}
