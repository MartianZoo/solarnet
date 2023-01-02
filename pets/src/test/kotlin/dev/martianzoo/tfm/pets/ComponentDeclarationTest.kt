package dev.martianzoo.tfm.pets

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.PetsParser.parseComponents
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.te
import org.junit.jupiter.api.Test

class ComponentDeclarationTest {

  @Test
  fun body() {
    assertThat(parseComponents("""
          CLASS Bar : Qux { DEFAULT +This?
            Foo -> Bar


            Foo: Bar
            CLASS Foo

          }
        """.trim())).hasSize(2)
  }

  @Test
  fun series() {
    assertThat(parseComponents("""
        CLASS Die {
        }
        CLASS DieHard {
          // whatever
        }

        CLASS Atomized

        CLASS Generation

        """.trim()
        )
    )
  }

  @Test fun nesting() {
    val cs = parseComponents("""
      CLASS Component

      CLASS One
      CLASS Two: One
      CLASS Three {
          CLASS Four
          CLASS Five: One
          CLASS Six {
              CLASS Seven
              CLASS Eight: One
          }
      }
    """.trimIndent())

    assertThat(cs.map { it.supertypes }).containsExactly(
        setOf<TypeExpression>(),
        setOf(te("Component")),
        setOf(te("One")),
        setOf(te("Component")),
        setOf(te("Three")),
        setOf(te("One"), te("Three")),
        setOf(te("Three")),
        setOf(te("Six")),
        setOf(te("One"), te("Six")),
    )
  }

  @Test fun oneLiner() {
    val cs = parseComponents("""
      CLASS One { This: That }
    """)
  }

  @Test fun nestedOneLiner() {
    val cs = parseComponents("""
      CLASS One {
        CLASS Two { This: That }
        CLASS Three { This: That }
      }
    """)
  }

  @Test fun withDefaults() {
    val cs = parseComponents("""
        ABSTRACT CLASS Component {
           DEFAULT +This!
           DEFAULT This<Foo>

           CLASS This   // comment


           ABSTRACT CLASS Phase { // comment
               // comment

               CLASS End
           }
        }
    """)
  }
}
