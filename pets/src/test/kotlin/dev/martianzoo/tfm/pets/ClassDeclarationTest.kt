package dev.martianzoo.tfm.pets

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.ClassDeclarationParser.parseClassDeclarations
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import org.junit.jupiter.api.Test

private class ClassDeclarationTest {

  @Test
  fun body() {
    assertThat(parseClassDeclarations("""
          CLASS Bar : Qux { DEFAULT +This?
            Foo -> Bar


            Foo: Bar
            CLASS Foo

          }
        """.trim())).hasSize(2)
  }

  @Test
  fun series() {
    assertThat(parseClassDeclarations("""
        CLASS Die {
        }
        CLASS DieHard {
          // whatever
        }

        CLASS Atomized

        CLASS Generation

        """.trim()))
  }

  @Test
  fun nesting() {
    val cs = parseClassDeclarations("""
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
        setOf(gte("Component")),
        setOf(gte("One")),
        setOf(gte("Component")),
        setOf(gte("Three")),
        setOf(gte("One"), gte("Three")),
        setOf(gte("Three")),
        setOf(gte("Six")),
        setOf(gte("One"), gte("Six")),
    )
  }

  @Test
  fun oneLiner() {
    parseClassDeclarations("""
      CLASS One { This: That }
    """)
  }

  @Test
  fun nestedOneLiner() {
    parseClassDeclarations("""
      CLASS One {
        CLASS Two { This: That }
        CLASS Three { This: That }
      }
    """)
  }

  @Test
  fun withDefaults() {
    parseClassDeclarations("""
        ABSTRACT CLASS Component {
           DEFAULT +This!
           DEFAULT This<Foo>

           CLASS What   // comment


           ABSTRACT CLASS Phase { // comment
               // comment

               CLASS End
           }
        }
    """)
  }
}
