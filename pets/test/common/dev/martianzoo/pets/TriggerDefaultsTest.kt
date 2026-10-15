package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.types.loadTypes
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

/** Trigger-specific defaults: T10-1, L9-5, and L11-8. */
internal class TriggerDefaultsTest {
  private val declarations =
      """
      ABSTRACT CLASS Holder {
        CLASS Printed
        CLASS Wild
      }
      ABSTRACT CLASS Tag<Holder> {
        DEFAULT Tag<Printed>:
        CLASS EarthTag
      }
      CLASS Reward
      """

  @Test
  internal fun `inherited trigger defaults leave metrics and gains unrestricted`() {
    val table = loadTypes(declarations)
    val elaborator = PetElaborator(table)
    table.getClass(cn("EarthTag")).defaults.triggerOnly.dependencies.toString() shouldContain
        "Printed"
    elaborator.elaborateMetricInput(parse<Metric>("EarthTag"), parse("Reward")) shouldBe
        parse<Metric>("EarthTag")
    elaborator.elaborateInput(parse("EarthTag<Wild>"), cn("Reward").expression).toString() shouldBe
        "EarthTag<Wild>!"
  }

  @Test
  internal fun `gain and removal triggers accept defaults or explicit alternative holders`() {
    val table =
        loadTypes(
            declarations,
            """
            CLASS Listener {
              EarthTag<>: Reward
              -EarthTag<>: Reward
              EarthTag<Wild>: Reward
              EarthTag<Holder>: Reward
            }
            """,
        )
    PetElaborator(table).classEffects(table.getClass(cn("Listener"))).map { it.toString() } shouldBe
        listOf(
            "EarthTag<Printed>: Reward!",
            "-EarthTag<Printed>: Reward!",
            "EarthTag<Wild>: Reward!",
            "EarthTag<Holder>: Reward!",
        )
  }

  @Test
  internal fun `trigger defaults require explicit acceptance`() {
    val table = loadTypes(declarations, "CLASS Listener { EarthTag: Reward }")
    shouldThrow<InvalidPetDefinitionException> {
          PetElaborator(table).classEffects(table.getClass(cn("Listener")))
        }
        .message shouldContain "write `EarthTag<>`"
  }

  @Test
  internal fun `trigger defaults round trip independently of gain and removal defaults`() {
    val source =
        """
        CLASS Tag<Holder> {
          DEFAULT Tag<Printed>:
          DEFAULT +Tag<Wild>
          DEFAULT -Tag<Holder>*
        }
        """
    val declaration = parseClasses(source).single()
    parseClasses(declaration.toString()).single() shouldBe declaration
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Holder {
              CLASS Printed
              CLASS Wild
            }
            """,
            source,
        )
    val defaults = table.getClass(cn("Tag")).defaults
    defaults.triggerOnly.dependencies.toString() shouldContain "Printed"
    defaults.gainOnly.dependencies.toString() shouldContain "Wild"
  }
}
