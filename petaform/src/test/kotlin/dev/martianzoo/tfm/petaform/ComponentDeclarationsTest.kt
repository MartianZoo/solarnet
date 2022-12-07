package dev.martianzoo.tfm.petaform

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.petaform.Action.Cost.Spend
import dev.martianzoo.tfm.petaform.Instruction.Gain
import dev.martianzoo.tfm.petaform.Instruction.Intensity
import dev.martianzoo.tfm.petaform.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.petaform.PetaformParser.ComponentClasses
import dev.martianzoo.tfm.petaform.PetaformParser.ComponentClasses.Count
import dev.martianzoo.tfm.petaform.PetaformParser.QEs
import dev.martianzoo.tfm.petaform.PetaformParser.parse
import dev.martianzoo.tfm.petaform.PetaformParser.parseComponentClasses
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
        .isEqualTo(listOf(ComponentDeclaration(Expression("Foo"), complete=false)))
  }

  @Test
  fun body() {
    assertThat(parseComponentClasses("""
          class Bar : Qux { default Foo?
            Foo -> Bar


            Foo: Bar
            class Foo

          }
        """.trim())).hasSize(2)
  }

  @Test
  fun series() {
    assertThat(parseComponentClasses("""
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
    val cs = parseComponentClasses("""
        abstract class One
    """)
    assertThat(cs).containsExactly(ComponentDeclaration(Expression("One"), abstract=true))
  }

  @Test
  fun threeSimple() {
    val cs = parseComponentClasses("""
        abstract class One
        class Two

        abstract class Three
    """)
    assertThat(cs).containsExactly(
        ComponentDeclaration(Expression("One"), abstract=true),
        ComponentDeclaration(Expression("Two"), abstract=false),
        ComponentDeclaration(Expression("Three"), abstract=true),
    )
  }

  @Test fun withSupers() {
    val cs = parseComponentClasses("""
        class One : Two, Three
    """)
    assertThat(cs).containsExactly(
        ComponentDeclaration(
            Expression("One"),
            supertypes = setOf(
                Expression("Two"), Expression("Three")
            )
        )
    )
  }

  @Test fun complexExprs() {
    val cs = parseComponentClasses("""
        class One<Two<Three>(HAS Four)> : Five(HAS 6 Seven), Eight<Nine>
    """)
    assertThat(cs).containsExactly(
        ComponentDeclaration(
            parse("One<Two<Three>(HAS Four)>"),
            supertypes = setOf(
                parse("Five(HAS 6 Seven)"), parse("Eight<Nine>"))
        )
    )
  }

  @Test fun nested() {
    val cs = parseComponentClasses("""
        class One { // comment
          abstract class Two
          // comment
          class Three : Four
        }
    """)
    assertThat(cs).containsExactly(
        ComponentDeclaration(parse("One")),
        ComponentDeclaration(parse("Two"), abstract = true, supertypes = setOf(parse("One"))),
        ComponentDeclaration(parse("Three"), supertypes = setOf(parse("One"), parse("Four"))),
    )
  }

  @Test fun oneLiner() {
    val cs = parseComponentClasses("""
      class One { This: That }
    """)
  }

  @Test fun nestedOneLiner() {
    val cs = parseComponentClasses("""
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
    val cs = parseComponentClasses("""
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
    val cs = parseComponentClasses("""
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
  }
}
