package dev.martianzoo.tfm.petaform.parser

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.petaform.api.Action
import dev.martianzoo.tfm.petaform.api.Action.Cost.Spend
import dev.martianzoo.tfm.petaform.api.ComponentClassDeclaration
import dev.martianzoo.tfm.petaform.api.ComponentDecls
import dev.martianzoo.tfm.petaform.api.Effect
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.Instruction
import dev.martianzoo.tfm.petaform.api.Instruction.Gain
import dev.martianzoo.tfm.petaform.api.Instruction.Intensity
import dev.martianzoo.tfm.petaform.api.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.petaform.api.PetaformNode
import dev.martianzoo.tfm.petaform.parser.PetaformParser.ComponentClasses
import dev.martianzoo.tfm.petaform.parser.PetaformParser.ComponentClasses.Count
import dev.martianzoo.tfm.petaform.parser.PetaformParser.QEs
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse
import org.junit.jupiter.api.Test

class ComponentDeclarationsTest {
  @Test
  fun bodyElements() {
    assertThat(parse(ComponentClasses.repeatableElement, "default Foo?"))
        .isEqualTo(Gain(Expression("Foo"), 1, OPTIONAL))
    assertThat(parse(ComponentClasses.repeatableElement, "Foo -> Bar"))
        .isEqualTo(Action(Spend(Expression("Foo")), Gain(Expression("Bar"))))
    assertThat(parse(ComponentClasses.repeatableElement, "Foo: Bar")).isInstanceOf(Effect::class.java)
    assertThat(parse(ComponentClasses.repeatableElement, "class Foo"))
        .isEqualTo(listOf(ComponentClassDeclaration(Expression("Foo"), complete=false)))
  }

  @Test
  fun body() {
    assertThat(parse(ComponentClasses.componentClump, """
          class Bar : Qux { default Foo?
            Foo -> Bar


            Foo: Bar
            class Foo

          }
        """.trim())).hasSize(2)
  }

  @Test
  fun series() {
    assertThat(parse(ComponentClasses.componentsFile, """
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


  @Test
  fun oneSimple() {
    val cs = parse<ComponentDecls>("""
        abstract class One
    """)
    assertThat(cs.decls).containsExactly(ComponentClassDeclaration(Expression("One"), abstract=true))
  }

  @Test
  fun threeSimple() {
    val cs = parse<ComponentDecls>("""
        abstract class One
        class Two

        abstract class Three
    """)
    assertThat(cs.decls).containsExactly(
        ComponentClassDeclaration(Expression("One"), abstract=true),
        ComponentClassDeclaration(Expression("Two"), abstract=false),
        ComponentClassDeclaration(Expression("Three"), abstract=true),
    )
  }

  @Test fun withSupers() {
    val cs = parse<ComponentDecls>("""
        class One : Two, Three
    """)
    assertThat(cs.decls).containsExactly(
        ComponentClassDeclaration(
            Expression("One"),
            supertypes = setOf(
                Expression("Two"), Expression("Three"))
        )
    )
  }

  @Test fun complexExprs() {
    val cs = parse<ComponentDecls>("""
        class One<Two<Three>(HAS Four)> : Five(HAS 6 Seven), Eight<Nine>
    """)
    assertThat(cs.decls).containsExactly(
        ComponentClassDeclaration(
            parse("One<Two<Three>(HAS Four)>"),
            supertypes = setOf(
                parse("Five(HAS 6 Seven)"), parse("Eight<Nine>"))
        )
    )
  }

  @Test fun nested() {
    val cs = parse<ComponentDecls>("""
        class One { // comment
          abstract class Two
          // comment
          class Three : Four
        }
    """)
    assertThat(cs.decls).containsExactly(
        ComponentClassDeclaration(parse("One")),
        ComponentClassDeclaration(parse("Two"), abstract = true, supertypes = setOf(parse("One"))),
        ComponentClassDeclaration(parse("Three"), supertypes = setOf(parse("One"), parse("Four"))),
    )
  }

  @Test fun oneLiner() {
    val cs = parse<ComponentDecls>("""
      class One { This: That }
    """)
  }

  @Test fun nestedOneLiner() {
    val cs = parse<ComponentDecls>("""
      class One {
        class Two { This: That }
        class Three { This: That }
      }
    """)
  }

  @Test fun withCount1() {
    assertThat(parse(QEs.scalar, "0")).isEqualTo(0)
    assertThat(parse(QEs.scalar, "2")).isEqualTo(2)
    assertThat(parse(ComponentClasses.upper, "2")).isEqualTo(2)
    assertThat(parse(ComponentClasses.upper, "*")).isNull()
    assertThat(parse(ComponentClasses.twoDots, "..")).isNotNull()
    assertThat(parse(ComponentClasses.twoDots, " .. ")).isNotNull()
    assertThat(parse(ComponentClasses.count, "count 2..3")).isEqualTo(Count(2, 3))
    val cs = parse<ComponentDecls>("""
      class One {
        count 2..3
      }
    """)
  }

  @Test fun default() {
    val instr: PetaformNode = parse(PetaformParser.ComponentClasses.default, "default -Component!")
    assertThat(instr).isEqualTo(Instruction.Remove(Expression("Component"), 1,  Intensity.MANDATORY))
  }

  @Test fun withDefaults() {
    val cs = parse<ComponentDecls>("""
        abstract class Component {
           default Component!
           default -Component!

           class This   // comment


           abstract class Phase { // comment
               // comment

               class End
           }
        }
    """)
    //  assertThat(cs.decls).containsExactly(
  }
}
