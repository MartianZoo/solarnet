package dev.martianzoo.tfm.pets

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.Parsing.parseClassDeclarations
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import org.junit.jupiter.api.Test

private class ClassDeclarationTest {
  @Test
  fun simpleOneLiners() {
    parseClassDeclarations("CLASS Foo") // minimal
    parseClassDeclarations("ABSTRACT CLASS Foo") // abstract
    parseClassDeclarations("CLASS Foo<Bar>") // with spec
    parseClassDeclarations("CLASS Foo(HAS Bar)") // with ref
    // parseClassDeclarations("CLASS Foo[FOO]") // with shortname
    parseClassDeclarations("CLASS Foo : Bar") // with supertype
    parseClassDeclarations("CLASS Foo { HAS 1 }") // with same-line body
    parseClassDeclarations(" CLASS Foo") // with space first
    parseClassDeclarations("\nCLASS Foo") // with newline first
    parseClassDeclarations("CLASS Foo ") // with space after
    parseClassDeclarations("CLASS Foo\n") // with newline after
  }

  @Test
  fun slightlyMoreComplex() {
    parseClassDeclarations("""
      CLASS Foo
      CLASS Bar
    """) // two separate

    parseClassDeclarations("""
      CLASS Foo {
      }
    """) // empty body

    parseClassDeclarations("""
      CLASS Foo {
        HAS Bar
      }
    """) // invariant
    parseClassDeclarations("""
      CLASS Foo {
        DEFAULT +This!
      }
    """) // default
    parseClassDeclarations("""
      CLASS Foo {
        DEFAULT +This!
      }
      CLASS Bar {
        DEFAULT +This!
      }
    """) // two blocks
    parseClassDeclarations("""
      CLASS Foo {
        DEFAULT +This!
      }
      CLASS Bar, Qux
    """)
  }

  @Test
  fun body() {
    assertThat(parseClassDeclarations("""
          CLASS Bar : Qux { DEFAULT +This?
            Foo -> Bar


            Foo: Bar
            CLASS Foo

          }
        """)).hasSize(2)
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

        """))
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
    """)

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
