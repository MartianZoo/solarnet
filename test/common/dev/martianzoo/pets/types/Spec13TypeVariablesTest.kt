package dev.martianzoo.pets.types

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.typeVariablesFor
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Catalog
import dev.martianzoo.pets.data.GamePremise
import dev.martianzoo.pets.util.Multiset
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Section 13 of `docs/type-system-spec.md`: type variables. */
internal class Spec13TypeVariablesTest {

  private val resources =
      loadTypes(
          "CLASS Player1 : Owner",
          "ABSTRACT CLASS StandardResource : Owned<Owner> { CLASS Plant, Steel }",
          "ABSTRACT CLASS Production<Class<StandardResource>> : Owned<Owner>",
          "ABSTRACT CLASS Receipt<Class<StandardResource>>",
      )

  private fun effect(source: String): Effect =
      resources.inferTypeVariables().transformEffect(parse(source))

  private fun names(scope: TypeVariableScope) =
      scope.variables.map { it.name?.toString() ?: "${it.declaration.expression}" }

  // T13-1 A variable is a kind of type

  @Test
  internal fun `T13-1 a variable is a Type whose structural meaning is its bound`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<Person AS P> { This: Token<P> }",
        )
    val variable = table.getClass(cn("Badge")).typeVariables.single()

    variable.typeVariable shouldBe variable
    variable.bound shouldBe table.resolve(te("Person"))
    variable.groundType shouldBe table.resolve(te("Person"))
    variable.bound.typeVariable shouldBe null
    variable.rootClass shouldBe table.getClass(cn("Person"))
    variable.abstract shouldBe true
    variable.isSubtypeOf(table.resolve(te("Person"))) shouldBe true
    table.resolve(te("Alice")).isSubtypeOf(variable) shouldBe true
    "${variable.expression}" shouldBe "Person AS P"
  }

  @Test
  internal fun `T13-1 every occurrence is a Type view of the same variable`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<Person AS P> { This: Token<P> }",
        )
    val variable = table.getClass(cn("Badge")).typeVariables.single()

    variable.occurrences shouldBe listOf(variable.declaration) + variable.usages
    variable.declaration.typeVariable shouldBe variable
    variable.usages.single().typeVariable shouldBe variable
    variable.occurrences.map { "${it.expression}" } shouldContainExactly listOf("Person AS P", "P")
    variable.occurrences.map { it.ordinal } shouldBe listOf(0, 1)
  }

  // T13-2 Class-header variables

  @Test
  internal fun `T13-2 each eligible abstract header expression declares one variable`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Holder<Box<Person>>",
        )

    table.getClass(cn("Holder")).typeVariables.map { "$it" } shouldContainExactly
        listOf("Box<Person>", "Person")
  }

  @Test
  internal fun `T13-2 a concrete or This header expression declares nothing`() {
    val table =
        loadTypes(
            "CLASS Alice",
            "ABSTRACT CLASS Box<Alice>",
            "ABSTRACT CLASS Link<Class<Component>>",
            "ABSTRACT CLASS SelfBound : Link<Class<This>>",
        )

    table.getClass(cn("Box")).typeVariables.shouldContainExactly(listOf())
    table.getClass(cn("SelfBound")).typeVariables.map { "$it" } shouldContainExactly listOf()
  }

  @Test
  internal fun `T13-2 occurrences that reach one dependency path are one variable`() {
    // `Cardbound<CardFront<Player>> : Owned<Player>`: the card's owner is the component's owner.
    val cards =
        loadTypes(
            "CLASS Player1 : Owner",
            "ABSTRACT CLASS CardFront : Owned<Owner>",
            "ABSTRACT CLASS Cardbound<CardFront<Owner>> : Owned<Owner>",
        )

    cards.getClass(cn("Cardbound")).typeVariables.map { "$it" } shouldContainExactly
        listOf("CardFront<Owner>", "Owner")
    cards
        .getClass(cn("Cardbound"))
        .isEqualityConstrainedDependency(Dependency.Key(cn("Owned"), 0)) shouldBe true
  }

  @Test
  internal fun `T13-2 two header roots spelled alike stay independent`() {
    val table =
        loadTypes("ABSTRACT CLASS Person { CLASS Alice }", "ABSTRACT CLASS Duo<Person, Person>")

    table.getClass(cn("Duo")).typeVariables.map { "$it" } shouldContainExactly
        listOf("Person", "Person")
    table.getClass(cn("Duo")).typeVariables.distinct().size shouldBe 2
  }

  @Test
  internal fun `T13-2 identical nested bounds in sibling branches stay independent`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice, Bob }",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Pair<Box<Person>, Box<Person>>",
            "ABSTRACT CLASS Holder<Pair<Box<Person>, Box<Person>>>",
        )

    val pair = table.getClass(cn("Pair"))
    pair.typeVariables.map { "$it" } shouldContainExactly
        listOf("Box<Person>", "Person", "Box<Person>", "Person")
    pair.typeVariables.distinct().size shouldBe 4

    // The two people are free to differ, so no equality propagation (T3-8) forces them together,
    // at whatever depth the sibling branches sit.
    table.resolve(te("Pair<Box<Alice>, Box<Bob>>")).expressionFull shouldBe
        te("Pair<Box<Alice>, Box<Bob>>")
    table.resolve(te("Holder<Pair<Box<Alice>, Box<Bob>>>")).expressionFull shouldBe
        te("Holder<Pair<Box<Alice>, Box<Bob>>>")
  }

  // T13-3 Uses in the class body

  @Test
  internal fun `T13-3 interpreting shared source in another universe preserves earlier scope`() {
    val firstCatalog =
        testCatalog(
            """
            ABSTRACT CLASS Person { CLASS Alice }
            ABSTRACT CLASS Token<Person>
            ABSTRACT CLASS Holder<Person AS P> { This: Token<P> }
            """
        )
    val secondCatalog =
        object : Catalog by firstCatalog {
          override val classTable: ClassTable by lazy { ClassLoader(this).loadEverything() }
        }
    val firstTable = firstCatalog.classTable
    val secondTable = secondCatalog.classTable
    val firstHolder = firstTable.getClass(cn("Holder"))
    val secondHolder = secondTable.getClass(cn("Holder"))
    val source = firstHolder.declaration.effects.single()

    val first = firstHolder.interpretTypeVariablesIn(source)
    val second = secondHolder.interpretTypeVariablesIn(source)

    first.typeVariables.variables.single().bound shouldBe firstTable.resolve(te("Person"))
    second.typeVariables.variables.single().bound shouldBe secondTable.resolve(te("Person"))
    source.typeVariables.isEmpty shouldBe true
  }

  @Test
  internal fun `T13-3 choice inference uses the game universe for premise Classes`() {
    val sourceCatalog = testCatalog("ABSTRACT CLASS Master")
    val premise =
        GamePremise(
            catalog = sourceCatalog,
            modules = emptySet(),
            classSelections = emptySet(),
            initialComponentTypes = emptySet(),
            premiseClassDeclarations = parseClasses("ABSTRACT CLASS Local").toSet(),
        )
    val reader =
        object : GameReader {
          override val actors: List<Actor> = emptyList()
          override val catalog: Catalog = sourceCatalog
          override val classTable: ClassTable = premise.classTable

          override fun resolve(expression: Expression): Type = classTable.resolve(expression)

          override fun isAbstract(e: Expression): Boolean = error("unused")

          override fun ensureNarrows(wide: Expression, narrow: Expression): Unit = error("unused")

          override fun has(requirement: Requirement): Boolean = error("unused")

          override fun count(metric: Metric): Int = error("unused")

          override fun count(type: Type): Int = error("unused")

          override fun countComponent(concreteType: Type): Int = error("unused")

          override fun getComponents(type: Type): Multiset<Type> = error("unused")

          override fun getDependents(component: Type): Set<Type> = error("unused")
        }
    val instruction = parse<Instruction>("Local AS L THEN L")

    names(instruction.typeVariablesFor(reader)) shouldContainExactly listOf("L")
  }

  @Test
  internal fun `T13-3 a named header variable is visible in the class's own effects`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Token<Box<Person>>",
            "ABSTRACT CLASS Holder<Box<Person> AS HeldBox> { This: Token<HeldBox> }",
        )
    val holder = table.getClass(cn("Holder"))
    val effect = holder.interpretTypeVariablesIn(holder.declaration.effects.single())

    names(effect.typeVariables) shouldContainExactly listOf("HeldBox")
  }

  @Test
  internal fun `T13-3 a header variable reference may appear inside another expression`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Holder<Person AS P> { This: Box<P> }",
        )
    val variable = table.getClass(cn("Holder")).typeVariables.single()

    variable.occurrences.map { "${it.expression}" } shouldContainExactly listOf("Person AS P", "P")
  }

  @Test
  internal fun `T13-3 repeated header spelling in a body is not linked`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person",
            "ABSTRACT CLASS Independent<Person> { This: Person }",
        )

    table.getClass(cn("Independent")).typeVariables.single().usages shouldBe emptyList()
  }

  @Test
  internal fun `T13-3 a simple named header variable can receive use-site arguments`() {
    val table =
        loadTypes(
            "CLASS Player1 : Owner",
            "ABSTRACT CLASS Person : Owned<Owner> { CLASS Alice }",
            "ABSTRACT CLASS Holder<Person AS P> { This: P<Player1> }",
        )
    val variable = table.getClass(cn("Holder")).typeVariables.single()

    variable.usages.map { "${it.expression}" } shouldContainExactly listOf("P<Player1>")
  }

  @Test
  internal fun `T13-3 a Class-header variable name cannot be a Type name`() {
    shouldThrow<PetException> {
      loadTypes(
          "ABSTRACT CLASS Person",
          "ABSTRACT CLASS Holder<Person AS Holder> { This: Holder }",
      )
    }
  }

  @Test
  internal fun `T13-3 a Class-header variable name must be used`() {
    shouldThrow<PetSyntaxException> {
      parseClasses("ABSTRACT CLASS Person\nABSTRACT CLASS Holder<Person AS P>")
    }
  }

  // T13-4 Inheritance

  @Test
  internal fun `T13-4 a subclass does not redeclare an inherited variable`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<Person AS P> { This: Token<P> }",
            "ABSTRACT CLASS Middle : Badge",
            "CLASS Leaf : Badge<Alice>",
        )

    table.getClass(cn("Badge")).typeVariables.map { "$it" } shouldContainExactly listOf("P")
    table.getClass(cn("Middle")).typeVariables.map { "$it" } shouldContainExactly listOf()
    table.getClass(cn("Leaf")).typeVariables.map { "$it" } shouldContainExactly listOf()
  }

  @Test
  internal fun `T13-4 a supplied supertype argument is visible structurally in the subclass body`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<Person>",
            "ABSTRACT CLASS PersonBadge : Badge<Person> { This: Token<Person> }",
        )
    val personBadge = table.getClass(cn("PersonBadge"))
    val effect = personBadge.interpretTypeVariablesIn(personBadge.declaration.effects.single())
    val variable = effect.typeVariables.variables.single()
    val specialized = table.resolve(te("PersonBadge<Alice>"))

    effect.typeVariables
        .bind(specialized.variableBindingsFrom(personBadge.defaultType, listOf(variable)))
        .transformEffect(effect)
        .toString() shouldBe "This: Token<Alice>"
  }

  // T13-5 Capturing values

  @Test
  internal fun `T13-5 specializing a component type supplies its header variables`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Token<Box<Person>>",
            "ABSTRACT CLASS Holder<Box<Person> AS HeldBox> { This: HeldBox }",
        )
    val holder = table.getClass(cn("Holder"))
    val effect = holder.interpretTypeVariablesIn(holder.declaration.effects.single())
    val bindings =
        table
            .resolve(te("Holder<Box<Alice>>"))
            .variableBindingsFrom(holder.defaultType, effect.typeVariables.variables)

    bindings.map { (variable, value) -> "$variable=$value" } shouldContainExactly
        listOf("HeldBox=Box<Alice>")
    effect.typeVariables.bind(bindings).transformEffect(effect).toString() shouldBe
        "This: Box<Alice>"
  }

  @Test
  internal fun `T13-5 nested named variables both specialize a class body`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Resource<Holder>",
            "ABSTRACT CLASS Holder<Class<Resource>>",
            "CLASS Rock<Holder> : Resource<Holder>",
            "CLASS RockHolder : Holder<Class<Rock>>",
            "CLASS Offer<Holder<Class<Resource AS R>> AS H> { This: R<H> }",
        )
    val offer = table.getClass(cn("Offer"))
    val effect = offer.interpretTypeVariablesIn(offer.declaration.effects.single())
    val specialized = table.resolve(te("Offer<RockHolder>"))

    effect.typeVariables
        .bind(specialized.variableBindingsFrom(offer.defaultType, effect.typeVariables.variables))
        .transformEffect(effect)
        .toString() shouldBe "This: Rock<RockHolder>"
  }

  @Test
  internal fun `T13-5 an unchanged abstract value supplies nothing`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<Person AS P> { This: Token<P> }",
        )
    val badge = table.getClass(cn("Badge"))
    val variable = badge.typeVariables.single()

    badge.defaultType.variableBindingsFrom(badge.defaultType, listOf(variable)) shouldBe emptyMap()
  }

  @Test
  internal fun `T13-5 a subclass that fixes the dependency does supply a value`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<Person AS P> { This: Token<P> }",
            "CLASS Leaf : Badge<Alice>",
        )
    val variable = table.getClass(cn("Badge")).typeVariables.single()
    val leaf = table.getClass(cn("Leaf")).defaultType

    "${leaf.variableBindingsFrom(leaf, listOf(variable))[variable]}" shouldBe "Alice"
  }

  @Test
  internal fun `T13-5 both types must share a root class`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Badge<Person>",
            "ABSTRACT CLASS Middle : Badge",
        )
    val badge = table.getClass(cn("Badge"))
    val variable = badge.typeVariables.single()

    shouldThrowIae {
      table
          .getClass(cn("Middle"))
          .defaultType
          .variableBindingsFrom(badge.defaultType, listOf(variable))
    }
  }

  // T13-6 Explicit Effect variables

  @Test
  internal fun `T13-6 AS declares an Effect variable and its name uses it`() {
    val trade = effect("StandardResource AS R: R")
    val variable = trade.typeVariables.variables.single()

    variable.name shouldBe cn("R")
    variable.occurrences.map { "${it.expression}" } shouldContainExactly
        listOf("StandardResource AS R", "R")
  }

  @Test
  internal fun `T13-6 repeated Effect spelling alone declares nothing`() {
    effect("StandardResource: StandardResource").typeVariables.variables shouldBe listOf()
    effect("StandardResource: Plant").typeVariables.variables shouldBe listOf()
  }

  @Test
  internal fun `T13-6 a variable name cannot be a Type name`() {
    shouldThrow<ExpressionException> { effect("StandardResource AS Plant: Plant") }
    shouldThrow<ExpressionException> {
      loadTypes(
          "ABSTRACT CLASS StandardResource { CLASS Plant }",
          "CLASS Observer { StandardResource AS Plant: Plant }",
      )
    }
  }

  @Test
  internal fun `T13-6 all occurrences in one region join the same variable`() {
    val many = effect("StandardResource AS R: R, R")

    many.typeVariables.variables.single().occurrences.size shouldBe 3
  }

  @Test
  internal fun `T13-6 the value chosen for a variable reaches every occurrence`() {
    val trade = effect("Production<Class<StandardResource AS R>>: R")
    val variable = trade.typeVariables.variables.single()

    trade.typeVariables
        .bind(mapOf(variable to resources.resolve(te("Plant"))))
        .transformEffect(trade)
        .toString() shouldBe "Production<Class<Plant>>: Plant"
  }

  // T13-7 Regions

  @Test
  internal fun `T13-7 an effect's regions are its trigger and its instruction`() {
    names(effect("StandardResource AS R: R").typeVariables) shouldContainExactly listOf("R")
  }

  @Test
  internal fun `T13-7 an action names a choice shared by its cost and result`() {
    val action: Action =
        resources.inferTypeVariables().transformAction(parse("StandardResource AS R -> R"))
    val lowered = action.toInstruction() as Then
    val variable = lowered.typeVariables.variables.single()

    names(action.typeVariables) shouldContainExactly listOf("R")
    lowered.typeVariables
        .bind(mapOf(variable to resources.resolve(te("Plant"))))
        .transformInstruction(lowered)
        .toString() shouldBe "-Plant! THEN Plant"

    val unlinked =
        resources
            .inferTypeVariables()
            .transformAction(parse("StandardResource -> StandardResource"))
    unlinked.typeVariables.variables shouldBe listOf()
  }

  @Test
  internal fun `T13-7 repeated inference does not duplicate a named Action variable`() {
    val infer = resources.inferTypeVariables()
    val once = infer.transformAction(parse("StandardResource AS R -> R"))
    val twice = infer.transformAction(once)

    names(twice.typeVariables) shouldContainExactly listOf("R")
  }

  @Test
  internal fun `T13-7 an Action variable name cannot be a Type name`() {
    shouldThrow<ExpressionException> {
      resources.inferTypeVariables().transformAction(parse("StandardResource AS Plant -> Plant"))
    }
  }

  @Test
  internal fun `T13-7 a THEN sequence names a choice shared by its stages`() {
    val instruction: Instruction =
        resources.inferTypeVariables().transformInstruction(parse("StandardResource AS R THEN R"))
    val then = instruction as Then
    val variable = then.typeVariables.variables.single()

    names(then.typeVariables) shouldContainExactly listOf("R")
    then.typeVariables
        .bind(mapOf(variable to resources.resolve(te("Plant"))))
        .transformInstruction(then)
        .toString() shouldBe "Plant THEN Plant"

    val unlinked =
        resources
            .inferTypeVariables()
            .transformInstruction(parse("StandardResource THEN StandardResource")) as Then
    unlinked.typeVariables.variables shouldBe listOf()
  }

  @Test
  internal fun `T13-7 repeated inference does not duplicate a named THEN variable`() {
    val infer = resources.inferTypeVariables()
    val once = infer.transformInstruction(parse("StandardResource AS R THEN R"))
    val twice = infer.transformInstruction(once) as Then

    names(twice.typeVariables) shouldContainExactly listOf("R")
  }

  @Test
  internal fun `T13-7 a THEN name follows declaration order and must cross stages`() {
    shouldThrow<ExpressionException> {
      resources.inferTypeVariables().transformInstruction(parse("R THEN StandardResource AS R"))
    }

    val table =
        loadTypes(
            "ABSTRACT CLASS StandardResource { CLASS Plant }",
            "ABSTRACT CLASS Duo<StandardResource, StandardResource>",
            "CLASS Coin",
        )
    shouldThrow<ExpressionException> {
      table
          .inferTypeVariables()
          .transformInstruction(parse("Duo<StandardResource AS R, R> THEN Coin"))
    }
  }

  @Test
  internal fun `T13-7 a THEN variable name cannot be a Type name`() {
    shouldThrow<ExpressionException> {
      resources
          .inferTypeVariables()
          .transformInstruction(parse("StandardResource AS Plant THEN Plant"))
    }
  }

  @Test
  internal fun `T13-7 a transmutation names a destination choice used by its source`() {
    val transmute =
        resources
            .inferTypeVariables()
            .transformInstruction(
                parse("Receipt<Class<StandardResource AS R>> FROM Production<Class<R>>")
            ) as Instruction.Transmute
    val variable = transmute.typeVariables.variables.single()

    variable.name shouldBe cn("R")
    variable.occurrences.map { "${it.expression}" } shouldContainExactly
        listOf("StandardResource AS R", "R")
    transmute.typeVariables
        .bind(mapOf(variable to resources.resolve(te("Plant"))))
        .transformInstruction(transmute)
        .toString() shouldBe "Receipt<Class<Plant>> FROM Production<Class<Plant>>"
  }

  @Test
  internal fun `T13-7 repeated transmutation spelling alone declares nothing`() {
    val transmute =
        resources
            .inferTypeVariables()
            .transformInstruction(
                parse(
                    "Production<Class<StandardResource>> FROM Production<Class<StandardResource>>"
                )
            ) as Instruction.Transmute

    transmute.typeVariables.variables shouldBe listOf()
  }

  @Test
  internal fun `T13-7 a compact transmutation structurally shares each unchanged argument`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice, Bob }",
            "ABSTRACT CLASS Side { CLASS Left, Right }",
            "ABSTRACT CLASS Pair<Person, Side>",
        )
    val transmute =
        table.inferTypeVariables().transformInstruction(parse("Pair<Person, Left FROM Right>"))
            as Instruction.Transmute
    val variable = transmute.typeVariables.variables.single()

    variable.occurrences.map { "${it.expression}" } shouldContainExactly listOf("Person", "Person")
    transmute.typeVariables
        .bind(mapOf(variable to table.resolve(te("Alice"))))
        .transformInstruction(transmute)
        .toString() shouldBe "Pair<Alice, Left FROM Right>"
  }

  @Test
  internal fun `T13-3 a transmutation uses a class variable rather than hiding it`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Resource { CLASS A }",
            "ABSTRACT CLASS Source<Resource>",
            "ABSTRACT CLASS Destination<Resource>",
            "CLASS Converter<Resource AS R> { This: Destination<R> FROM Source<R> }",
        )
    val converter = table.getClass(cn("Converter"))
    val effect = converter.interpretTypeVariablesIn(converter.declaration.effects.single())
    val transmute = effect.instruction as Instruction.Transmute
    val variable = converter.typeVariables.single()

    effect.typeVariables.variables shouldBe converter.typeVariables
    transmute.typeVariables.variables shouldBe listOf()
    effect.typeVariables
        .bind(mapOf(variable to table.resolve(te("A"))))
        .transformEffect(effect)
        .toString() shouldBe "This: Destination<A> FROM Source<A>"
  }

  @Test
  internal fun `T13-7 a transmutation variable name cannot be a Type name`() {
    shouldThrow<ExpressionException> {
      resources
          .inferTypeVariables()
          .transformInstruction(
              parse("Receipt<Class<StandardResource AS Plant>> FROM Production<Class<Plant>>")
          )
    }
  }

  @Test
  internal fun `T13-7 a second inference does not duplicate a named transmutation variable`() {
    val infer = resources.inferTypeVariables()
    val once =
        infer.transformInstruction(
            parse("Receipt<Class<StandardResource AS R>> FROM Production<Class<R>>")
        )
    val twice = infer.transformInstruction(once) as Instruction.Transmute

    names(twice.typeVariables) shouldContainExactly listOf("R")
  }

  // T13-8 What does not declare a variable

  @Test
  internal fun `T13-8 an EACH selector declares its own variable, never a header one`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS StandardResource { CLASS Plant }",
            "ABSTRACT CLASS Holder<StandardResource> { This: EACH StandardResource { StandardResource } }",
        )
    val variable = table.getClass(cn("Holder")).typeVariables.single()

    variable.occurrences.size shouldBe 1
  }

  @Test
  internal fun `T13-3 a named class variable remains visible inside a THEN`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Coin<Person>",
            "ABSTRACT CLASS Receipt<Person>",
            "CLASS Offer<Person AS P> { This: Coin<P> THEN Receipt<P> }",
        )
    val offer = table.getClass(cn("Offer"))
    val classScoped = offer.interpretTypeVariablesIn(offer.declaration.effects.single())
    val classVariable = classScoped.typeVariables.variables.single()

    "${classVariable.declaration.expression}" shouldBe "Person AS P"
    classVariable.occurrences.size shouldBe 3
    classScoped.typeVariables
        .bind(mapOf(classVariable to table.resolve(te("Alice"))))
        .transformEffect(classScoped)
        .toString() shouldBe "This: Coin<Alice> THEN Receipt<Alice>"
  }

  @Test
  internal fun `T13-8 an earlier gate occurrence belongs to that same first-stage choice`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Eligible<Person>",
            "ABSTRACT CLASS Coin<Person>",
            "ABSTRACT CLASS Receipt<Person>",
        )
    val inferred =
        table
            .inferTypeVariables()
            .transformEffect(
                parse("This: (Eligible<Choice>: Coin<Person AS Choice>) THEN Receipt<Choice>")
            )
    val then = inferred.instruction as Then
    val choice = then.typeVariables.variables.single()

    choice.name.toString() shouldBe "Choice"
    choice.occurrences.map { "${it.expression}" } shouldContainExactly
        listOf("Person AS Choice", "Choice", "Choice")
    (choice.usages.first().ordinal < choice.declaration.ordinal) shouldBe true
    then.typeVariables
        .bind(mapOf(choice to table.resolve(te("Alice"))))
        .transformEffect(inferred)
        .toString() shouldBe "This: Eligible<Alice>: Coin<Alice> THEN Receipt<Alice>"
  }

  // T13-9 Actor selectors

  private val actors =
      loadTypes(
          "ABSTRACT CLASS Player : Owner, Actor { CLASS Player1, Player2 }",
          "ABSTRACT CLASS Heat : Owned<Owner>",
          "ABSTRACT CLASS Notice<Owner>",
      )

  private fun actorEffect(source: String) =
      actors.inferTypeVariables().transformEffect(parse<Effect>(source))

  @Test
  internal fun `T13-9 a simple abstract actor selector binds even with no repetition`() {
    val bound = actorEffect("Heat BY Player: Ok")

    names(bound.typeVariables) shouldContainExactly listOf("Player")
    bound.typeVariables.variables.single().occurrences.size shouldBe 1
  }

  @Test
  internal fun `T13-9 repeating an unnamed actor Type does not reuse its value`() {
    val bound = actorEffect("Heat BY Player: Notice<Player>")

    bound.typeVariables.variables.single().occurrences.size shouldBe 1
  }

  @Test
  internal fun `T13-9 Anyone and a refined selector are filters, not binders`() {
    names(actorEffect("Heat BY Anyone: Ok").typeVariables) shouldContainExactly listOf()
    names(actorEffect("Heat BY Player(NOT Owner): Ok").typeVariables) shouldContainExactly listOf()
  }

  @Test
  internal fun `T13-9 an exclusion may use the actor variable, and is tested after binding`() {
    val bound =
        actorEffect(
            "Notice<Owner(NOT ActingPlayer) AS Other> BY Player AS ActingPlayer: Heat<Other>"
        )
    val actor = bound.typeVariables.variables.single { it.name == cn("ActingPlayer") }
    val event = bound.typeVariables.variables.single { it.name == cn("Other") }

    actor.occurrences.size shouldBe 3
    event.occurrences.size shouldBe 2
    bound.typeVariables
        .bind(mapOf(actor to actors.resolve(te("Player1"))))
        .transformEffect(bound)
        .toString() shouldBe "Notice<Owner(NOT Player1) AS Other> BY Player1: Heat<Other>"
  }

  @Test
  internal fun `T13-9 a difference occurrence captures its candidate from its own domain`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Owner, Actor { CLASS Player1 }",
            "CLASS Passive : Owner",
            "ABSTRACT CLASS Resource<Owner>",
            "ABSTRACT CLASS Notice<Owner>",
        )
    val bound =
        table
            .inferTypeVariables()
            .transformEffect(
                parse<Effect>(
                    "Resource<Owner(NOT ActingPlayer) AS Other> BY Player AS ActingPlayer: Notice<Other>"
                )
            )
    val actor = bound.typeVariables.variables.single { it.name == cn("ActingPlayer") }
    val afterActor =
        bound.typeVariables
            .bind(mapOf(actor to table.resolve(te("Player1"))))
            .transformEffect(bound)
    val event = afterActor.typeVariables.variables.single()

    event.bound shouldBe table.resolve(te("Owner"))
    "${afterActor.typeVariables.expressionOf(event.declaration)}" shouldBe
        "Owner(NOT Player1) AS Other"

    val captured =
        afterActor.typeVariables.bindingsFrom(
            afterActor.trigger.descendantsOfType<Expression>().first(),
            table.resolve(parse("Resource<Owner(NOT Player1)>")),
            table.resolve(parse("Resource<Passive>")),
        )

    "${captured[event]}" shouldBe "Passive"
    afterActor.typeVariables.bind(captured).transformEffect(afterActor).toString() shouldBe
        "Resource<Passive> BY Player1: Notice<Passive>"
  }

  // T13-10 Binding

  @Test
  internal fun `T13-10 binding replaces only the recorded occurrences`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS StandardResource { CLASS Plant, Steel }",
            "ABSTRACT CLASS Notice<StandardResource>",
        )
    val bound =
        table
            .inferTypeVariables()
            .transformEffect(parse<Effect>("StandardResource AS R: Notice<R>, Steel"))
    val variable = bound.typeVariables.variables.single()

    bound.typeVariables
        .bind(mapOf(variable to table.resolve(te("Plant"))))
        .transformEffect(bound)
        .toString() shouldBe "Plant: Notice<Plant>, Steel"
  }

  @Test
  internal fun `T13-10 a binding must satisfy every recorded occurrence`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS StandardResource { CLASS Plant }",
            "CLASS Hand",
            "ABSTRACT CLASS Notice<StandardResource>",
        )
    val bound =
        table
            .inferTypeVariables()
            .transformEffect(parse<Effect>("StandardResource AS R: Notice<R>"))
    val variable = bound.typeVariables.variables.single()

    shouldThrow<NarrowingException> {
      bound.typeVariables.bind(mapOf(variable to table.resolve(te("Hand"))))
    }
  }

  @Test
  internal fun `T13-10 a refined declaration is evaluated once, while its value is captured`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS StandardResource { CLASS Plant }",
            "ABSTRACT CLASS Marker<StandardResource>",
            "ABSTRACT CLASS Token<StandardResource>",
        )
    val bound =
        table
            .inferTypeVariables()
            .transformEffect(parse<Effect>("StandardResource(HAS Marker) AS R: Token<R>"))
    val variable = bound.typeVariables.variables.single()
    val world = RecordingWorld(answer = true)

    table.resolve(te("Plant")).narrows(variable.bound, world) shouldBe true
    world.questions.size shouldBe 1

    bound.typeVariables
        .bind(mapOf(variable to table.resolve(te("Plant"))))
        .transformEffect(bound)
        .toString() shouldBe "Plant: Token<Plant>"
    world.questions.size shouldBe 1
  }

  // T13-11 Scope queries

  @Test
  internal fun `T13-11 a scope reports the variables and spellings visible in it`() {
    val trade = effect("StandardResource AS R: R")
    val scope = trade.typeVariables
    val variable = scope.variables.single()

    scope.isEmpty shouldBe false
    TypeVariableScope.EMPTY.isEmpty shouldBe true
    scope.expressionsOf(variable).map { "$it" }.toSet() shouldBe setOf("StandardResource AS R", "R")
    "${scope.expressionOf(variable.declaration)}" shouldBe "StandardResource AS R"
    scope.variableAt(parse<Expression>("R")) shouldBe variable
    scope.variableDeclaredAt(trade.trigger.descendantsOfType<Expression>().first()) shouldBe
        variable
    scope.variableAt(parse<Expression>("Plant")) shouldBe null
  }

  @Test
  internal fun `T13-11 a scope can capture values from a specialized expression`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Container<Component>",
        )
    val authored = parse<Expression>("Container<Box<Person>>")
    val person = authored.arguments.single().arguments.single()
    val scope =
        TypeVariableScope.fromDeclarations(
            listOf(authored),
            table,
            unnamedDeclarations = listOf(person),
        )

    scope
        .bindingsFrom(
            authored,
            table.resolve(authored),
            table.resolve(parse("Container<Box<Alice>>")),
        )
        .map { (variable, value) -> "$variable=$value" } shouldContainExactly listOf("Person=Alice")
  }

  @Test
  internal fun `T13-11 capture follows dependency paths, so a mismatched candidate captures nothing`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Box<Person>",
            "CLASS Hand",
            "ABSTRACT CLASS Container<Component>",
        )
    val authored = parse<Expression>("Container<Box<Person>>")
    val person = authored.arguments.single().arguments.single()
    val scope =
        TypeVariableScope.fromDeclarations(
            listOf(authored),
            table,
            unnamedDeclarations = listOf(person),
        )

    scope.bindingsFrom(
        authored,
        table.resolve(authored),
        table.resolve(parse("Container<Hand>")),
    ) shouldBe emptyMap()
  }

  @Test
  internal fun `T13-11 one variable cannot capture conflicting structural values`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice, Bob }",
            "ABSTRACT CLASS Pair<Person, Person>",
        )
    val authored = parse<Expression>("Pair<Person, Person>")
    val person = authored.arguments.first()
    val scope =
        TypeVariableScope.fromDeclarations(
            listOf(authored),
            table,
            unnamedDeclarations = listOf(person),
        )

    shouldThrow<IllegalStateException> {
      scope.bindingsFrom(
          authored,
          table.resolve(authored),
          table.resolve(parse("Pair<Alice, Bob>")),
      )
    }
  }
}
