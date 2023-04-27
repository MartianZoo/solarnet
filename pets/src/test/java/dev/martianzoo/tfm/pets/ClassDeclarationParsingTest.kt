package dev.martianzoo.tfm.pets

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.Parsing.parseClasses
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Expression
import org.junit.jupiter.api.Test

private class ClassDeclarationParsingTest {
  @Test
  fun simpleOneLiners() {
    parseClasses("CLASS Foo") // minimal
    parseClasses("ABSTRACT CLASS Foo") // abstract
    parseClasses("CLASS Foo<Bar>") // with spec
    parseClasses("CLASS Foo[FOO]") // with shortname
    parseClasses("CLASS Foo : Bar") // with supertype
    parseClasses("CLASS Foo { HAS 1 }") // with same-line body
    parseClasses(" CLASS Foo") // with space first
    parseClasses("\nCLASS Foo") // with newline first
    parseClasses("CLASS Foo ") // with space after
    parseClasses("CLASS Foo\n") // with newline after
  }

  @Test
  fun slightlyMoreComplex() {
    parseClasses("""
      CLASS Foo
      CLASS Bar
    """) // two separate

    parseClasses("""
      CLASS Foo {
      }
    """) // empty body

    parseClasses("""
      CLASS Foo {
        HAS Bar
      }
    """) // invariant
    parseClasses("""
      CLASS Foo {
        DEFAULT +Foo!
      }
    """) // default
    parseClasses(
        """
      CLASS Foo {
        DEFAULT +Foo!
      }
      CLASS Bar {
        DEFAULT +Bar!
      }
    """) // two blocks
    parseClasses(
        """
      CLASS Foo {
        DEFAULT +Foo!
      }
      CLASS Bar, Qux
    """)
  }

  @Test
  fun body() {
    assertThat(
            parseClasses(
                """
                  CLASS Bar : Qux { DEFAULT +Bar?
                    Foo -> Bar


                    Foo: Bar
                    CLASS Foo

                  }
                """))
        .hasSize(2)
  }

  @Test
  fun series() {
    parseClasses(
        """
          CLASS Die {
          }
          CLASS DieHard {
            // whatever
          }

          CLASS Atomized

          CLASS Generation

        """)
  }

  @Test
  fun nesting() {
    val cs =
        parseClasses(
            """
              ABSTRACT CLASS Component

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

    assertThat(cs.map { it.supertypes })
        .containsExactly(
            setOf<Expression>(),
            setOf<Expression>(),
            setOf(cn("One").expression),
            setOf<Expression>(),
            setOf(cn("Three").expression),
            setOf(cn("One").expression, cn("Three").expression),
            setOf(cn("Three").expression),
            setOf(cn("Six").expression),
            setOf(cn("One").expression, cn("Six").expression),
        )
  }

  @Test
  fun nestedOneLiner() {
    parseClasses(
        """
      CLASS One {
        CLASS Two { This: That }
        CLASS Three { This: That }
      }
    """)
  }

  @Test
  fun withDefaults() {
    parseClasses(
        """
        ABSTRACT CLASS Component {
           DEFAULT +Component!
           DEFAULT Component<Foo>

           CLASS What   // comment


           ABSTRACT CLASS Phase { // comment
               // comment

               CLASS End
           }
        }
    """)
  }
}
