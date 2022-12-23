package dev.martianzoo.tfm.pets

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.Instruction.Intensity
import dev.martianzoo.tfm.pets.PetsParser.Components
import dev.martianzoo.tfm.pets.PetsParser.QEs
import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.PetsParser.parseComponents
import org.junit.jupiter.api.Test

class ComponentDefTest {

  @Test
  fun body() {
    assertThat(parseComponents("""
          class Bar : Qux { default +This?
            Foo -> Bar


            Foo: Bar
            class Foo

          }
        """.trim())).hasSize(2)
  }

  @Test
  fun series() {
    assertThat(parseComponents("""
        class Die {
        }
        class DieHard {
          // whatever
        }

        class Atomized

        class Generation

        """.trim()
        )
    )
  }

  @Test fun nesting() {
    val cs = parseComponents("""
      class Component

      class One
      class Two: One
      class Three {
          class Four
          class Five: One
          class Six {
              class Seven
              class Eight: One
          }
      }
    """.trimIndent())

    assertThat(cs).containsExactly(
        ComponentDef("Component", supertypes=setOf()),
        ComponentDef("One", supertypes=setOf(rootEx)),
        ComponentDef("Two", supertypes=setOf(te("One"))),
        ComponentDef("Three", supertypes=setOf(rootEx)),
        ComponentDef("Four", supertypes=setOf(te("Three"))),
        ComponentDef("Five", supertypes=setOf(te("One"), te("Three"))),
        ComponentDef("Six", supertypes=setOf(te("Three"))),
        ComponentDef("Seven", supertypes=setOf(te("Six"))),
        ComponentDef("Eight", supertypes=setOf(te("One"), te("Six"))))
  }

  @Test fun oneLiner() {
    val cs = parseComponents("""
      class One { This: That }
    """)
  }

  @Test fun nestedOneLiner() {
    val cs = parseComponents("""
      class One {
        class Two { This: That }
        class Three { This: That }
      }
    """)
  }

  @Test fun withCount1() {
    assertThat(parse(QEs.scalar, "0")).isEqualTo(0)
    assertThat(parse(QEs.scalar, "2")).isEqualTo(2)
    assertThat(parse(Components.upper, "2")).isEqualTo(2)
    assertThat(parse(Components.upper, "*")).isNull()
    assertThat(parse(Components.twoDots, "..")).isNotNull()
    assertThat(parse(Components.twoDots, " .. ")).isNotNull()
  }

  @Test fun withDefaults() {
    val cs = parseComponents("""
        abstract class Component {
           default +This!
           default -This!
           default This<Foo>

           class This   // comment


           abstract class Phase { // comment
               // comment

               class End
           }
        }
    """)
  }
}
