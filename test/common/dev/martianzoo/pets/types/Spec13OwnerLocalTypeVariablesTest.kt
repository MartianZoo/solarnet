package dev.martianzoo.pets.types

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Each
import dev.martianzoo.pets.ast.Instruction.Transmute
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Class-header and selector scopes when L12 owner-local declarations are extracted. */
internal class Spec13OwnerLocalTypeVariablesTest {
  @Test
  internal fun headerVariableDoesNotDiscardLocalBody() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Widget { CLASS Item }",
            "ABSTRACT CLASS Base<Widget>",
            "CLASS Owner1<A@Widget> { This: Base<A@Widget> {} }",
        )
    table.getClass(cn("Owner1_Base")).declaration.supertypes.map { "$it" } shouldBe
        listOf("Base<Widget>")
    table
        .getClass(cn("Owner1"))
        .typeVariables
        .single()
        .usages
        .single()
        .expression
        .className shouldBe cn("Widget")
  }

  @Test
  internal fun selectorVariableDoesNotDiscardLocalBody() {
    val declarations = parseClasses("CLASS Owner1 { This: EACH A@Widget { Base<A@Widget> {} } }")
    declarations.map { it.className } shouldContainExactly listOf(cn("Owner1"), cn("Owner1_Base"))
    val each = declarations.first().authoredEffects.single().instruction as Each
    val gaining = each.body as dev.martianzoo.pets.ast.Instruction.Gain
    gaining.gaining.expression.className shouldBe cn("Owner1_Base")
    declarations.last().supertypes.map { "$it" } shouldBe listOf("Base<Widget>")
  }

  @Test
  internal fun settlementArgumentVariableDoesNotDeclareAGeneratedHeaderMarker() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Widget { CLASS Item }",
            "ABSTRACT CLASS Base<Widget>",
            "CLASS Owner1 { This: Base<A@Widget> {} FROM A@Widget }",
        )
    val effect =
        table
            .recordTypeVariableScopes()
            .transformEffect(table.getClass(cn("Owner1")).declaration.authoredEffects.single())
    val transmute = effect.instruction as Transmute
    transmute.typeVariables.variables.single().occurrences.size shouldBe 2
    transmute.gaining.expression.className shouldBe cn("Owner1_Base")
    table.getClass(cn("Owner1_Base")).declaration.supertypes.map { "$it" } shouldBe
        listOf("Base<Widget>")
  }

  @Test
  internal fun generatedSupertypeRetainsSharedArgumentConstraint() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Person {
            CLASS Alice
            CLASS Bob
            }
            """
                .trimIndent(),
            "ABSTRACT CLASS Pair<Person, Person>",
            "CLASS Owner1 { This: EACH P@Person { Pair<P@Person, P@Person> {} } }",
        )
    val derived = table.getClass(cn("Owner1_Pair"))
    derived.isEqualityConstrainedDependency(Dependency.Key(cn("Pair"), 0)) shouldBe true
    derived.isEqualityConstrainedDependency(Dependency.Key(cn("Pair"), 1)) shouldBe true
    derived.typeVariables.size shouldBe 1
  }

  @Test
  internal fun referenceExpansionCopiesArgumentsWithoutDeclaringTheirBodiesAgain() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Base",
            "ABSTRACT CLASS Wrapper<Base>",
            "CLASS Owner1 { This: A@Wrapper<Base {}> FROM A@Wrapper }",
        )
    val transmute =
        table.getClass(cn("Owner1")).declaration.authoredEffects.single().instruction as Transmute
    transmute.gaining.expression.arguments.single().className shouldBe cn("Owner1_Base")
    transmute.removing.expression.arguments.single().className shouldBe cn("Owner1_Base")
  }

  @Test
  internal fun referenceExpansionCopiesRefinementsWithoutDeclaringTheirBodiesAgain() {
    val declarations = parseClasses("CLASS Owner1 { This: A@Wrapper(HAS Base {}) FROM A@Wrapper }")
    declarations.map { it.className } shouldContainExactly listOf(cn("Owner1"), cn("Owner1_Base"))
    val transmute = declarations.first().authoredEffects.single().instruction as Transmute
    transmute.gaining.expression.refinement shouldBe transmute.removing.expression.refinement
  }

  @Test
  internal fun generatedSharedArgumentConstraintDoesNotDependOnWhichOccurrenceIsLoweredFirst() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Person {
            CLASS Alice
            CLASS Bob
            }
            """
                .trimIndent(),
            "ABSTRACT CLASS Pair<Person, Person>",
            "ABSTRACT CLASS Wrapper<Pair>",
            "CLASS Observer",
            """
            CLASS Owner1<P@Person> {
              This: Observer(HAS A@Wrapper) FROM A@Wrapper<Pair<P@Person, P@Person> {}>
            }
            """
                .trimIndent(),
        )
    val derived = table.getClass(cn("Owner1_Pair"))
    derived.isEqualityConstrainedDependency(Dependency.Key(cn("Pair"), 0)) shouldBe true
    derived.typeVariables.size shouldBe 1
  }

  @Test
  internal fun selectorBindingReachesBothCopiesOfLocalClassArguments() {
    val declarations =
        parseClasses(
            """
            CLASS Owner1 {
              This: EACH P@Person { A@Wrapper<Base<P@Person> {}> FROM A@Wrapper }
            }
            """
                .trimIndent()
        )
    val each = declarations.first().authoredEffects.single().instruction as Each
    val bound = each.bodyFor(cn("Alice").expression) as Transmute
    bound.gaining.expression.arguments.single().arguments.single().className shouldBe cn("Alice")
    bound.removing.expression.arguments.single().arguments.single().className shouldBe cn("Alice")
  }

  @Test
  internal fun repeatedBodiesAreNotCollapsedByRequirementNormalization() {
    shouldThrow<PetSyntaxException> {
          parseClasses("CLASS Owner1 { This: Widget(HAS Base {}, HAS Base {}) }")
        }
        .detail shouldBe "owner `Owner1` declares more than one unnamed derived `Base` class"
  }

  @Test
  internal fun generatedSharedArgumentConstraintSurvivesSourceRendering() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Person {
            CLASS Alice
            CLASS Bob
            }
            """
                .trimIndent(),
            "ABSTRACT CLASS Pair<Person, Person>",
            "CLASS Owner1 { This: EACH P@Person { Pair<P@Person, P@Person> {} } }",
        )
    val source = table.getClass(cn("Owner1_Pair")).declaration.toString()
    val reparsed =
        loadTypes(
                """
                ABSTRACT CLASS Person {
                CLASS Alice
                CLASS Bob
                }
                """
                    .trimIndent(),
                "ABSTRACT CLASS Pair<Person, Person>",
                source,
            )
            .getClass(cn("Owner1_Pair"))
    reparsed.isEqualityConstrainedDependency(Dependency.Key(cn("Pair"), 0)) shouldBe true
    reparsed.typeVariables.size shouldBe 1
  }

  @Test
  internal fun extractedClassResolvesItsOwnSignatureAndBodyTogether() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Base<Person>",
            """
            CLASS Owner1 {
              This: EACH P@Person { Base<P@Person> { This: P@Person } }
            }
            """
                .trimIndent(),
        )
    val derived = table.getClass(cn("Owner1_Base"))
    derived.typeVariables.size shouldBe 1
    derived.typeVariables.single().usages.size shouldBe 1
    val effect = derived.interpretTypeVariablesIn(derived.declaration.authoredEffects.single())
    effect.typeVariables
        .bind(mapOf(derived.typeVariables.single() to table.resolve(cn("Alice").expression)))
        .transformEffect(effect)
        .toString() shouldBe "This: Alice"
    parseClasses(derived.declaration.toString()).single() shouldBe derived.declaration
  }

  @Test
  internal fun extractedClassCannotCaptureAnEnclosingSelectorWithoutAnArgument() {
    shouldThrow<InvalidPetDefinitionException> {
          loadTypes(
              "ABSTRACT CLASS Person",
              "ABSTRACT CLASS Base",
              """
              CLASS Owner1 {
                This: EACH P@Person { P@Person, Base { This: P@Person } }
              }
              """
                  .trimIndent(),
          )
        }
        .detail shouldBe "`Owner1_Base` has no inherited type variable `P@Person`"
  }

  @Test
  internal fun extractedClassBodyCanDeclareItsOwnScopeWithTheSameSpelling() {
    val declarations =
        parseClasses(
            """
            CLASS Owner1 {
              This: EACH P@Person { Base<P@Person> { This: EACH P@Person { P@Person } } }
            }
            """
                .trimIndent()
        )
    parseClasses(declarations.joinToString("\n")) shouldBe declarations
  }

  @Test
  internal fun aGeneratedHeaderParameterTakesPrecedenceOverSettlementScope() {
    listOf(
            "This: P@Person FROM P@Person",
            "This: Coin<P@Person> THEN Receipt<P@Person>",
            "P@Person: P@Person",
            "P@Person -> P@Person",
        )
        .forEach { body ->
          val inline =
              parseClasses("CLASS Owner1 { This: EACH P@Person { Base<P@Person> { $body } } }")
          val explicit =
              parseClasses(
                  """
        CLASS Owner1 {
          This: EACH P@Person { Owner1_Base<P@Person> }
        }
        CLASS Owner1_Base : Base<P@Person> {
          $body
        }
        """
                      .trimIndent()
              )
          inline shouldBe explicit
        }
  }
}
