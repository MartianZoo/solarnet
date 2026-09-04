package dev.martianzoo.pets.types

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Requirement
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TypeVariableTest {
  @Test
  internal fun `action lowering preserves variables shared by the cost and result`() {
    val table = loadTypes("ABSTRACT CLASS Resource", "CLASS Plant : Resource")
    val action = table.inferTypeVariables().transformAction(parse<Action>("Resource -> Resource"))
    val lowered = action.toInstruction() as Then
    val variable = lowered.typeVariables.variables.single()

    lowered.typeVariables
        .bind(mapOf(variable to table.resolve(parse("Plant"))))
        .transformInstruction(lowered)
        .toString() shouldBe "-Plant! THEN Plant"
  }

  @Test
  internal fun `a class body sees the variables declared by its own header`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person",
            "CLASS Alice : Person",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Token<Box<Person>>",
            "ABSTRACT CLASS Parent<Box<Person>>",
            "CLASS Child : Parent { This: Token<Box> }",
            "CLASS NamedChild : Parent<Box> { This: Token<Box> }",
        )
    val child = table.getClass(parse<Expression>("Child").className)
    val childEffect = child.interpretTypeVariablesIn(child.declaration.effects.single())
    child.typeVariables.isEmpty() shouldBe true
    childEffect.typeVariables.isEmpty shouldBe true

    val namedChild = table.getClass(parse<Expression>("NamedChild").className)
    val namedEffect = namedChild.interpretTypeVariablesIn(namedChild.declaration.effects.single())
    val variable = namedChild.typeVariables.single()
    namedEffect.typeVariables.variables.single() shouldBe variable
    val specific = table.resolve(parse("NamedChild<Box<Alice>>"))
    val bindings = specific.variableBindingsFrom(namedChild.defaultType, listOf(variable))

    namedEffect.typeVariables.bind(bindings).transformEffect(namedEffect).toString() shouldBe
        "This: Token<Box<Alice>>"
  }

  @Test
  internal fun `a first-stage dependency choice takes precedence over a Class variable`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person",
            "CLASS Alice : Person",
            "ABSTRACT CLASS Coin<Person>",
            "ABSTRACT CLASS Receipt<Person>",
            "CLASS Offer<Person> { This: Coin<Person> THEN Receipt<Person> }",
        )
    val klass = table.getClass(parse<Expression>("Offer").className)
    val classInterpreted = klass.interpretTypeVariablesIn(klass.declaration.effects.single())
    val effect = table.inferTypeVariables().transformEffect(classInterpreted)
    val then = effect.instruction as Then
    val choice = then.typeVariables.variables.single()

    classInterpreted.typeVariables.isEmpty shouldBe true
    choice.declaration.expression.toString() shouldBe "Person"
    then.typeVariables
        .bind(mapOf(choice to table.resolve(parse("Alice"))))
        .transformEffect(effect)
        .toString() shouldBe "This: Coin<Alice> THEN Receipt<Alice>"
  }

  @Test
  internal fun `binding ignores a candidate missing a nested dependency path`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS CardBack",
            "ABSTRACT CLASS Envelope<CardBack>",
            "CLASS Hand",
            "ABSTRACT CLASS Container<Component>",
        )
    val authored = parse<Expression>("Container<Envelope<CardBack>>")
    val cardBack = authored.arguments.single().arguments.single()
    val scope =
        TypeVariableScope.infer(
            listOf(authored),
            table,
            explicitDeclarations = listOf(cardBack),
        )

    scope.bindingsFrom(
        authored,
        table.resolve(authored),
        table.resolve(parse("Container<Hand>")),
    ) shouldBe emptyMap()
  }

  @Test
  internal fun `a complemented occurrence captures its candidate from the dependency domain`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Owner, Actor",
            "CLASS Player1 : Player",
            "CLASS Passive : Owner",
            "ABSTRACT CLASS Resource<Owner>",
            "ABSTRACT CLASS Notice<Owner>",
        )
    val effect =
        table
            .inferTypeVariables()
            .transformEffect(parse("Resource<!Player> BY Player: Notice<!Player>"))
    val actor =
        effect.typeVariables.variables.single {
          it.declaration.expression.toString() == "Player"
        }
    val actorBound =
        effect.typeVariables
            .bind(mapOf(actor to table.resolve(parse("Player1"))))
            .transformEffect(effect)
    val complemented = actorBound.typeVariables.variables.single()

    val bindings =
        actorBound.typeVariables.bindingsFrom(
            parse("Resource<!Player1>"),
            table.resolve(parse("Resource<!Player1>")),
            table.resolve(parse("Resource<Passive>")),
        )

    bindings[complemented].toString() shouldBe "Passive"
    complemented.bound.toString() shouldBe "Owner"
    actorBound.typeVariables.bind(bindings).transformEffect(actorBound).toString() shouldBe
        "Resource<Passive> BY Player1: Notice<Passive>"
    complemented.declaration.expression.toString() shouldBe "!Player"
  }

  @Test
  internal fun `binding consumes a refined declaration after its candidate is captured`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Resource",
            "CLASS Plant : Resource",
            "ABSTRACT CLASS Marker<Resource>",
            "ABSTRACT CLASS Token<Resource>",
        )
    val authored = parse<Effect>("Resource(HAS Marker): Token<Resource(HAS Marker)>")
    val effect = table.inferTypeVariables().transformEffect(authored)
    val variable = effect.typeVariables.variables.single()
    val binding = table.resolve(parse<Expression>("Plant"))
    var evaluations = 0
    val info =
        object : TypeInfo {
          override fun isAbstract(e: Expression): Boolean = error("unused")

          override fun ensureNarrows(wide: Expression, narrow: Expression): Unit = error("unused")

          override fun has(requirement: Requirement): Boolean {
            evaluations++
            return true
          }
        }

    binding.narrows(variable.bound, info) shouldBe true

    effect.typeVariables
        .bind(mapOf(variable to binding))
        .transformEffect(effect)
        .toString() shouldBe "Plant: Token<Plant>"
    evaluations shouldBe 1
  }

  @Test
  internal fun `actor declaration and its derived uses remain ordinary Type views`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Owner, Actor",
            "CLASS Player1 : Player",
            "ABSTRACT CLASS Notice<Actor>",
            "ABSTRACT CLASS Pair<Actor, Actor>",
        )
    val effect =
        table
            .inferTypeVariables()
            .transformEffect(parse("Notice<!Player> BY Player: Pair<Player, !Player>"))
    val variable =
        effect.typeVariables.variables.single {
          it.declaration.expression.toString() == "Player"
        }
    val eventVariable =
        effect.typeVariables.variables.single {
          it.declaration.expression.toString() == "!Player"
        }

    variable.declaration.expression.toString() shouldBe "Player"
    variable.usages.map { it.expression.toString() } shouldContainExactly
        listOf("!Player", "Player", "!Player")
    listOf<Type>(
            variable.bound,
            variable,
            variable.declaration,
            variable.usages.first(),
        )
        .map(Type::groundType)
        .distinct() shouldContainExactly listOf(table.resolve(parse("Player")))
    variable.bound.typeVariable shouldBe null
    variable.declaration.typeVariable shouldBe variable
    eventVariable.usages.map { it.expression.toString() } shouldContainExactly listOf("!Player")
    effect.typeVariables
        .bind(mapOf(variable to table.resolve(parse("Player1"))))
        .transformEffect(effect)
        .toString() shouldBe "Notice<!Player1> BY Player1: Pair<Player1, !Player1>"
  }

  @Test
  internal fun `authored argument order does not identify one enclosing variable`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Place",
            "CLASS Mars : Place",
            "ABSTRACT CLASS Person",
            "CLASS Alice : Person",
            "ABSTRACT CLASS Duo<Place, Person>",
            "ABSTRACT CLASS Notice<Component>",
            "ABSTRACT CLASS Token<Component>",
        )
    val effect =
        table
            .inferTypeVariables()
            .transformEffect(parse("Notice<Duo<Place, Person>>: Token<Duo<Person, Place>>"))
    effect.typeVariables.variables.map { it.declaration.expression.toString() }.toSet() shouldBe
        setOf("Place", "Person")
  }

  @Test
  internal fun `class specialization binds only inspected header variables`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person",
            "CLASS Alice : Person",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Holder<Box<Person>> { This: Box<Person> }",
        )
    val klass = table.getClass(parse<Expression>("Holder").className)
    val effect = klass.interpretTypeVariablesIn(klass.declaration.effects.single())
    val box = klass.typeVariables.single { it.declaration.expression.toString() == "Box<Person>" }
    val person = klass.typeVariables.single { it.declaration.expression.toString() == "Person" }

    box.usages.any { it.expression.toString() == "Box<Person>" } shouldBe true
    person.usages.any { it.expression.toString() == "Person" } shouldBe true

    val specific = table.resolve(parse("Holder<Box<Alice>>"))
    val bindings = specific.variableBindingsFrom(klass.defaultType, effect.typeVariables.variables)
    bindings[box].toString() shouldBe "Box<Alice>"
    bindings[person].toString() shouldBe "Alice"
    effect.typeVariables.bind(bindings).transformEffect(effect).toString() shouldBe
        "This: Box<Alice>"
  }

  @Test
  internal fun `identical nested bounds in sibling header branches remain independent`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person",
            "CLASS Alice : Person",
            "CLASS Bob : Person",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Pair<Box<Person>, Box<Person>>",
            "ABSTRACT CLASS Holder<Pair<Box<Person>, Box<Person>>>",
        )

    table.resolve(parse("Holder<Pair<Box<Alice>, Box<Bob>>>")).expressionFull.toString() shouldBe
        "Holder<Pair<Box<Alice>, Box<Bob>>>"
  }

  @Test
  internal fun `an effect use cannot choose between independent header declarations`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person",
            "ABSTRACT CLASS Ambiguous<Person, Person> { This: Person }",
        )
    val klass = table.getClass(parse<Expression>("Ambiguous").className)

    shouldThrow<PetException> { klass.typeVariables }
  }
}
