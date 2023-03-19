package dev.martianzoo.tfm.pets

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.Parsing.parseClassDeclarations
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Expression
import org.junit.jupiter.api.Test

private class ClassDeclarationParsingTest {
  @Test
  fun simpleOneLiners() {
    parseClassDeclarations("CLASS Foo") // minimal
    parseClassDeclarations("ABSTRACT CLASS Foo") // abstract
    parseClassDeclarations("CLASS Foo<Bar>") // with spec
    parseClassDeclarations("CLASS Foo[FOO]") // with shortname
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
        DEFAULT +Foo!
      }
    """) // default
    parseClassDeclarations(
        """
      CLASS Foo {
        DEFAULT +Foo!
      }
      CLASS Bar {
        DEFAULT +Bar!
      }
    """) // two blocks
    parseClassDeclarations(
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
            parseClassDeclarations(
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
    parseClassDeclarations(
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
        parseClassDeclarations(
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
            setOf(cn("One").expr),
            setOf<Expression>(),
            setOf(cn("Three").expr),
            setOf(cn("One").expr, cn("Three").expr),
            setOf(cn("Three").expr),
            setOf(cn("Six").expr),
            setOf(cn("One").expr, cn("Six").expr),
        )
  }

  @Test
  fun nestedOneLiner() {
    parseClassDeclarations(
        """
      CLASS One {
        CLASS Two { This: That }
        CLASS Three { This: That }
      }
    """)
  }

  @Test
  fun withDefaults() {
    parseClassDeclarations(
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
