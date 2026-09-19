package dev.martianzoo.pets.types

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Declaration
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
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
      resources.recordTypeVariableScopes().transformEffect(parse(source))

  private fun names(scope: TypeVariableScope) =
      scope.variables.map { it.name ?: "${it.declaration.expression}" }

  // T13-1 A variable is a kind of type

  @Test
  internal fun `T13-1 a variable is a Type whose structural meaning is its bound`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<Person^P> { This: Token<Person^P> }",
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
    "${variable.expression}" shouldBe "Person^P"
  }

  @Test
  internal fun `T13-1 every occurrence is a Type view of the same variable`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<Person^P> { This: Token<Person^P> }",
        )
    val variable = table.getClass(cn("Badge")).typeVariables.single()

    variable.occurrences shouldBe listOf(variable.declaration) + variable.usages
    variable.declaration.typeVariable shouldBe variable
    variable.usages.single().typeVariable shouldBe variable
    variable.occurrences.map { "${it.expression}" } shouldContainExactly
        listOf("Person^P", "Person^P")
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
  internal fun `T13-2 a class literal declares either its represented class or itself`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Carries<Class<Person>>",
            "ABSTRACT CLASS Represented<Class<Person>>",
            "ABSTRACT CLASS Literal<Class^1<Person>> : Carries<Class^1>",
        )

    table.getClass(cn("Represented")).typeVariables.map { "$it" } shouldContainExactly
        listOf("Person")
    table.getClass(cn("Literal")).typeVariables.map { "$it" } shouldContainExactly listOf("1")
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
  internal fun `T13-2 an explicit name links nested and inherited dependency positions`() {
    val cards =
        loadTypes(
            "CLASS Player1 : Owner",
            "ABSTRACT CLASS CardFront : Owned<Owner>",
            "ABSTRACT CLASS Cardbound<CardFront<Owner^CardOwner>> : Owned<Owner^CardOwner>",
        )

    cards.getClass(cn("Cardbound")).typeVariables.map { "$it" } shouldContainExactly
        listOf("CardFront<Owner^CardOwner>", "CardOwner")
    cards
        .getClass(cn("Cardbound"))
        .isEqualityConstrainedDependency(Dependency.Key(cn("Owned"), 0)) shouldBe true
  }

  @Test
  internal fun `T13-2 nested and inherited positions declare distinct variables`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person : Owner { CLASS Alice, Bob }",
            "ABSTRACT CLASS City : Owned<Person>",
            "ABSTRACT CLASS Cathedral<City<Person>, Person^CathedralOwner> : " +
                "Owned<Person^CathedralOwner>",
        )

    val cathedral = table.getClass(cn("Cathedral"))
    cathedral.typeVariables.map { "$it" } shouldContainExactly
        listOf("City<Person>", "Person", "CathedralOwner")
    cathedral.typeVariables.distinct().size shouldBe 3
  }

  @Test
  internal fun `T13-2 each equal header root declares its own variable`() {
    val table =
        loadTypes("ABSTRACT CLASS Person { CLASS Alice }", "ABSTRACT CLASS Duo<Person, Person>")

    table.getClass(cn("Duo")).typeVariables.map { "$it" } shouldContainExactly
        listOf("Person", "Person")
    table.getClass(cn("Duo")).typeVariables.distinct().size shouldBe 2
  }

  @Test
  internal fun `T13-2 an explicit name can link two header roots`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Duo<Person^P, Person^P>",
        )

    table.getClass(cn("Duo")).typeVariables.map { "$it" } shouldContainExactly listOf("P")
    table.getClass(cn("Duo")).isEqualityConstrainedDependency(Dependency.Key(cn("Duo"), 0)) shouldBe
        true
  }

  @Test
  internal fun `T13-2 each sibling branch declares its own nested variables`() {
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
            ABSTRACT CLASS Holder<Person^P> { This: Token<Person^P> }
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
  internal fun `T13-3 scope recording uses the game universe for premise Classes`() {
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
    val instruction = parse<Instruction>("Local^L THEN Local^L")

    names(instruction.typeVariablesFor(reader)) shouldContainExactly listOf("L")
  }

  @Test
  internal fun `T13-3 a named header variable is visible in the class's own effects`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Token<Box<Person>>",
            "ABSTRACT CLASS Holder<Box^HeldBox<Person>> { This: Token<Box^HeldBox> }",
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
            "ABSTRACT CLASS Holder<Person^P> { This: Box<Person^P> }",
        )
    val variable = table.getClass(cn("Holder")).typeVariables.single()

    variable.occurrences.map { "${it.expression}" } shouldContainExactly
        listOf("Person^P", "Person^P")
  }

  @Test
  internal fun `T13-3 a class body uses a header variable only through its name`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person",
            "ABSTRACT CLASS Holder<Person> { This: Person }",
        )

    table.getClass(cn("Holder")).typeVariables.single().usages shouldBe emptyList()
  }

  @Test
  internal fun `T13-3 an ordinary header variable reference cannot receive arguments`() {
    shouldThrow<PetSyntaxException> {
      parseClasses(
          "CLASS Player1 : Owner\n" +
              "ABSTRACT CLASS Person : Owned<Owner>\n" +
              "ABSTRACT CLASS Holder<Person^P> { This: Person^P<Player1> }"
      )
    }
  }

  @Test
  internal fun `T13-3 a represented-Class header variable can receive dependency arguments`() {
    val table =
        loadTypes(
            "CLASS Player1 : Owner",
            "ABSTRACT CLASS Person : Owned<Owner> { CLASS Alice }",
            "ABSTRACT CLASS Holder<Class<Person^P>> { This: Person^P<Player1> }",
        )
    val holder = table.getClass(cn("Holder"))
    val variable = holder.typeVariables.single { it.name == "P" }
    val effect = holder.interpretTypeVariablesIn(holder.declaration.effects.single())
    val specialized = table.resolve(te("Holder<Class<Alice>>"))

    variable.usages.map { "${it.expression}" } shouldContainExactly listOf("Person^P<Player1>")
    effect.typeVariables
        .bind(specialized.variableBindingsFrom(holder.defaultType, listOf(variable)))
        .transformEffect(effect)
        .toString() shouldBe "This: Alice<Player1>"
  }

  @Test
  internal fun `T13-3 a Class-header variable name may also be a Type name`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person",
            "ABSTRACT CLASS Holder<Person^Holder> { This: Person^Holder }",
        )

    table.getClass(cn("Holder")).typeVariables.single().name shouldBe "Holder"
  }

  @Test
  internal fun `T13-3 a Class-header variable name must be used`() {
    shouldThrow<PetSyntaxException> {
      parseClasses("ABSTRACT CLASS Person\nABSTRACT CLASS Holder<Person^P>")
    }
  }

  @Test
  internal fun `T13-3 only an eligible abstract header occurrence can declare a variable`() {
    shouldThrow<PetException> {
      loadTypes(
          "CLASS Alice",
          "ABSTRACT CLASS Holder<Alice^A>",
      )
    }
  }

  // T13-4 Inheritance

  @Test
  internal fun `T13-4 a subclass does not redeclare an inherited variable`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<Person^P> { This: Token<Person^P> }",
            "ABSTRACT CLASS Middle : Badge",
            "CLASS Leaf : Badge<Alice>",
        )

    table.getClass(cn("Badge")).typeVariables.map { "$it" } shouldContainExactly listOf("P")
    table.getClass(cn("Middle")).typeVariables.map { "$it" } shouldContainExactly listOf()
    table.getClass(cn("Leaf")).typeVariables.map { "$it" } shouldContainExactly listOf()
  }

  @Test
  internal fun `T13-4 a supplied supertype argument needs a name in the subclass body`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<Person>",
            "ABSTRACT CLASS PersonBadge : Badge<Person^P> { This: Token<Person^P> }",
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
            "ABSTRACT CLASS Holder<Box^HeldBox<Person>> { This: Box^HeldBox }",
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
            "CLASS Offer<Holder^H<Class<Resource^R>>> { " + "This: Resource^R<Holder^H> }",
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
  internal fun `T13-5 represented-Class arguments must agree with the selected Class`() {
    val table =
        loadTypes(
            "CLASS Player1 : Owner",
            "CLASS Player2 : Owner",
            "ABSTRACT CLASS Token : Owned<Owner>",
            "CLASS Player1Token : Token, Owned<Player1>",
            "CLASS Compatible<Class<Token^T>> { This: Token^T<Player1> }",
            "CLASS Conflicting<Class<Token^T>> { This: Token^T<Player2> }",
        )

    fun boundEffect(className: String): Effect {
      val klass = table.getClass(cn(className))
      val effect = klass.interpretTypeVariablesIn(klass.declaration.effects.single())
      val specialized = table.resolve(te("$className<Class<Player1Token>>"))
      val variable = effect.typeVariables.variables.single()
      return effect.typeVariables
          .bind(specialized.variableBindingsFrom(klass.defaultType, listOf(variable)))
          .transformEffect(effect)
    }

    boundEffect("Compatible").toString() shouldBe "This: Player1Token<Player1>"
    shouldThrow<NarrowingException> { boundEffect("Conflicting") }
  }

  @Test
  internal fun `T13-5 specializing an outer variable preserves a nested local declaration`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Owner",
            "ABSTRACT CLASS Resource<Owner>",
            "CLASS Plant<Owner> : Resource<Owner>",
            "ABSTRACT CLASS Watcher<Class<Resource^ThatResource>> { " +
                "-X Resource^ThatResource<Owner^Victim> BY Player^Attacker: " +
                "Resource<Owner^Victim>, Resource<Player^Attacker> }",
        )
    val watcher = table.getClass(cn("Watcher"))
    val effect =
        table
            .recordTypeVariableScopes()
            .transformEffect(watcher.interpretTypeVariablesIn(watcher.declaration.effects.single()))
    val specialized = table.resolve(te("Watcher<Class<Plant>>"))

    effect.typeVariables
        .bind(specialized.variableBindingsFrom(watcher.defaultType, effect.typeVariables.variables))
        .transformEffect(effect)
        .toString() shouldBe
        "-X Plant<Owner^Victim> BY Player^Attacker: " +
            "Resource<Owner^Victim>, Resource<Player^Attacker>"
  }

  @Test
  internal fun `T13-5 an unchanged abstract value supplies nothing`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<Person^P> { This: Token<Person^P> }",
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
            "ABSTRACT CLASS Badge<Person^P> { This: Token<Person^P> }",
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
  internal fun `T13-6 matching markers identify one Effect variable`() {
    val trade = effect("StandardResource^R: StandardResource^R")
    val variable = trade.typeVariables.variables.single()

    variable.name shouldBe "R"
    variable.occurrences.map { "${it.expression}" } shouldContainExactly
        listOf("StandardResource^R", "StandardResource^R")
  }

  @Test
  internal fun `T13-6 a decimal handle identifies a variable`() {
    val trade = effect("StandardResource^1: StandardResource^1")

    trade.typeVariables.variables.single().name shouldBe "1"
    trade.toString() shouldBe "StandardResource^1: StandardResource^1"
  }

  @Test
  internal fun `T13-6 one handle may identify variables with different bound Classes`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Owner, Actor",
            "ABSTRACT CLASS Notice<Owner>",
            "ABSTRACT CLASS Pair<Owner, Player>",
        )
    val scoped =
        table
            .recordTypeVariableScopes()
            .transformEffect(parse("Notice<Owner^1> BY Player^1: Pair<Owner^1, Player^1>"))

    names(scoped.typeVariables) shouldContainExactly listOf("1", "1")
    scoped.typeVariables.variables.map { it.rootClass.className } shouldContainExactly
        listOf(cn("Owner"), cn("Player"))
  }

  @Test
  internal fun `T13-6 a reference's bound Class must match its declaration`() {
    shouldThrow<PetSyntaxException> {
      parse<Effect>("StandardResource^1: Owner^1")
    }
  }

  @Test
  internal fun `T13-6 unnamed Effect expressions do not declare a variable`() {
    effect("StandardResource: StandardResource").typeVariables.variables shouldBe listOf()
    effect("StandardResource: Plant").typeVariables.variables shouldBe listOf()
  }

  @Test
  internal fun `T13-6 a variable name may also be a Type name`() {
    names(effect("StandardResource^Plant: StandardResource^Plant").typeVariables) shouldBe
        listOf("Plant")
  }

  @Test
  internal fun `T13-6 all occurrences in one region join the same variable`() {
    val many = effect("StandardResource^R: StandardResource^R, StandardResource^R")

    many.typeVariables.variables.single().occurrences.size shouldBe 3
  }

  @Test
  internal fun `T13-6 the value chosen for a variable reaches every occurrence`() {
    val trade = effect("Production<Class<StandardResource^R>>: StandardResource^R")
    val variable = trade.typeVariables.variables.single()

    trade.typeVariables
        .bind(mapOf(variable to resources.resolve(te("Plant"))))
        .transformEffect(trade)
        .toString() shouldBe "Production<Class<Plant>>: Plant"
  }

  @Test
  internal fun `T13-6 a represented-Class Effect variable accepts dependency arguments`() {
    val trade = effect("Production<Class<StandardResource^R>>: StandardResource^R<Player1>")
    val variable = trade.typeVariables.variables.single()

    trade.typeVariables
        .bind(mapOf(variable to resources.resolve(te("Plant"))))
        .transformEffect(trade)
        .toString() shouldBe "Production<Class<Plant>>: Plant<Player1>"

    val acceptingDefaults = effect("Production<Class<StandardResource^R>>: StandardResource^R<>")
    val defaultedVariable = acceptingDefaults.typeVariables.variables.single()
    acceptingDefaults.typeVariables
        .bind(mapOf(defaultedVariable to resources.resolve(te("Plant"))))
        .transformEffect(acceptingDefaults)
        .toString() shouldBe "Production<Class<Plant>>: Plant<>"
  }

  // T13-7 Regions

  @Test
  internal fun `T13-7 an effect's regions are its trigger and its instruction`() {
    names(effect("StandardResource^R: StandardResource^R").typeVariables) shouldContainExactly
        listOf("R")
    shouldThrow<PetSyntaxException> {
      effect("StandardResource^R OR StandardResource^R: Ok")
    }
  }

  @Test
  internal fun `T13-7 an action names a choice shared by its cost and result`() {
    val action: Action =
        resources
            .recordTypeVariableScopes()
            .transformAction(parse("StandardResource^R -> StandardResource^R"))
    val lowered = action.toInstruction() as Then
    val variable = lowered.typeVariables.variables.single()

    names(action.typeVariables) shouldContainExactly listOf("R")
    lowered.typeVariables
        .bind(mapOf(variable to resources.resolve(te("Plant"))))
        .transformInstruction(lowered)
        .toString() shouldBe "-Plant! THEN Plant"

    val ordinary =
        resources
            .recordTypeVariableScopes()
            .transformAction(parse("StandardResource -> StandardResource"))
    ordinary.typeVariables.variables shouldBe listOf()
  }

  @Test
  internal fun `T13-7 recording twice does not duplicate a named Action variable`() {
    val recorder = resources.recordTypeVariableScopes()
    val once = recorder.transformAction(parse("StandardResource^R -> StandardResource^R"))
    val twice = recorder.transformAction(once)

    names(twice.typeVariables) shouldContainExactly listOf("R")
  }

  @Test
  internal fun `T13-7 an Action variable name may be a Type name`() {
    val action =
        resources
            .recordTypeVariableScopes()
            .transformAction(parse("StandardResource^Plant -> StandardResource^Plant"))

    names(action.typeVariables) shouldContainExactly listOf("Plant")
  }

  @Test
  internal fun `T13-7 a THEN sequence names a choice shared by its stages`() {
    val instruction: Instruction =
        resources
            .recordTypeVariableScopes()
            .transformInstruction(parse("StandardResource^R THEN StandardResource^R"))
    val then = instruction as Then
    val variable = then.typeVariables.variables.single()

    names(then.typeVariables) shouldContainExactly listOf("R")
    then.typeVariables
        .bind(mapOf(variable to resources.resolve(te("Plant"))))
        .transformInstruction(then)
        .toString() shouldBe "Plant THEN Plant"

    val ordinary =
        resources
            .recordTypeVariableScopes()
            .transformInstruction(parse("StandardResource THEN StandardResource")) as Then
    ordinary.typeVariables.variables shouldBe listOf()
  }

  @Test
  internal fun `T13-7 recording twice does not duplicate a named THEN variable`() {
    val recorder = resources.recordTypeVariableScopes()
    val once = recorder.transformInstruction(parse("StandardResource^R THEN StandardResource^R"))
    val twice = recorder.transformInstruction(once) as Then

    names(twice.typeVariables) shouldContainExactly listOf("R")
  }

  @Test
  internal fun `T13-7 a THEN marker has no authored declaration order and must cross stages`() {
    val shared =
        resources
            .recordTypeVariableScopes()
            .transformInstruction(parse("StandardResource^R THEN StandardResource^R")) as Then
    names(shared.typeVariables) shouldContainExactly listOf("R")

    val table =
        loadTypes(
            "ABSTRACT CLASS StandardResource { CLASS Plant }",
            "ABSTRACT CLASS Duo<StandardResource, StandardResource>",
            "CLASS Coin",
        )
    shouldThrow<PetSyntaxException> {
      table
          .recordTypeVariableScopes()
          .transformInstruction(parse("Duo<StandardResource^R, StandardResource^R> THEN Coin"))
    }
  }

  @Test
  internal fun `T13-7 a THEN variable name may be a Type name`() {
    val then =
        resources
            .recordTypeVariableScopes()
            .transformInstruction(parse("StandardResource^Plant THEN StandardResource^Plant"))
            as Then

    names(then.typeVariables) shouldContainExactly listOf("Plant")
  }

  @Test
  internal fun `T13-7 a transmutation names a destination choice used by its source`() {
    val transmute =
        resources
            .recordTypeVariableScopes()
            .transformInstruction(
                parse(
                    "Receipt<Class<StandardResource^R>> FROM " +
                        "Production<Class<StandardResource^R>>"
                )
            ) as Instruction.Transmute
    val variable = transmute.typeVariables.variables.single()

    variable.name shouldBe "R"
    variable.occurrences.map { "${it.expression}" } shouldContainExactly
        listOf("StandardResource^R", "StandardResource^R")
    transmute.typeVariables
        .bind(mapOf(variable to resources.resolve(te("Plant"))))
        .transformInstruction(transmute)
        .toString() shouldBe "Receipt<Class<Plant>> FROM Production<Class<Plant>>"
  }

  @Test
  internal fun `T13-7 full transmutation sides share only named variables`() {
    val transmute =
        resources
            .recordTypeVariableScopes()
            .transformInstruction(
                parse(
                    "Production<Class<StandardResource>> FROM Production<Class<StandardResource>>"
                )
            ) as Instruction.Transmute

    transmute.typeVariables.variables shouldBe listOf()
  }

  @Test
  internal fun `T13-3 a transmutation uses a class variable rather than hiding it`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Resource { CLASS A }",
            "ABSTRACT CLASS Source<Resource>",
            "ABSTRACT CLASS Destination<Resource>",
            "CLASS Converter<Resource^R> { " +
                "This: Destination<Resource^R> FROM Source<Resource^R> }",
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
  internal fun `T13-7 a transmutation variable name may be a Type name`() {
    val transmute =
        resources
            .recordTypeVariableScopes()
            .transformInstruction(
                parse(
                    "Receipt<Class<StandardResource^Plant>> FROM " +
                        "Production<Class<StandardResource^Plant>>"
                )
            ) as Instruction.Transmute

    names(transmute.typeVariables) shouldContainExactly listOf("Plant")
  }

  @Test
  internal fun `T13-7 recording twice does not duplicate a named transmutation variable`() {
    val recorder = resources.recordTypeVariableScopes()
    val once =
        recorder.transformInstruction(
            parse(
                "Receipt<Class<StandardResource^R>> FROM " + "Production<Class<StandardResource^R>>"
            )
        )
    val twice = recorder.transformInstruction(once) as Instruction.Transmute

    names(twice.typeVariables) shouldContainExactly listOf("R")
  }

  // T13-8 Variable declaration sites

  @Test
  internal fun `T13-8 an unnamed EACH selector does not use a header variable`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS StandardResource { CLASS Plant }",
            "ABSTRACT CLASS Holder<StandardResource> { This: EACH StandardResource { StandardResource } }",
        )
    val variable = table.getClass(cn("Holder")).typeVariables.single()

    variable.occurrences.size shouldBe 1
  }

  @Test
  internal fun `T13-8 a selector variable name may be a Type name`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person",
            "CLASS Holder { This: EACH Person^Holder { Person^Holder } }",
        )

    table.getClass(cn("Holder")).declaration.effects.single().toString() shouldBe
        "This: EACH Person^Holder { Person^Holder }"
  }

  @Test
  internal fun `T13-8 a selector variable is distinct from an equal enclosing marker`() {
    val scoped =
        effect(
            "StandardResource^1: " +
                "EACH StandardResource^1 { StandardResource^1 }, StandardResource^1"
        )
    val outer = scoped.typeVariables.variables.single()
    val each = scoped.instruction.descendantsOfType<Instruction.Each>().single()
    val nestedReference = each.body.descendantsOfType<Expression>().single()

    outer.occurrences.map { "${it.expression}" } shouldContainExactly
        listOf("StandardResource^1", "StandardResource^1")
    scoped.typeVariables.variableAt(nestedReference) shouldBe null
    scoped.typeVariables
        .bind(mapOf(outer to resources.resolve(te("Steel"))))
        .transformEffect(scoped)
        .toString() shouldBe "Steel: EACH StandardResource^1 { StandardResource^1 }, Steel"
    scoped.typeVariables.expandNames().transformEffect(scoped).toString() shouldBe
        "StandardResource: EACH StandardResource^1 { StandardResource^1 }, StandardResource"
  }

  @Test
  internal fun `T13-8 a selector variable shadows an equal Class-header marker`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Resource { CLASS Plant, Steel }",
            "ABSTRACT CLASS Holder<Resource^1> { " +
                "This: EACH Resource^1 { Resource^1 }, Resource^1 }",
        )
    val holder = table.getClass(cn("Holder"))
    val variable = holder.typeVariables.single()
    val effect = holder.interpretTypeVariablesIn(holder.declaration.effects.single())

    variable.occurrences.map { "${it.expression}" } shouldContainExactly
        listOf("Resource^1", "Resource^1")
    effect.typeVariables
        .bind(mapOf(variable to table.resolve(te("Steel"))))
        .transformEffect(effect)
        .toString() shouldBe "This: EACH Resource^1 { Resource^1 }, Steel"
  }

  @Test
  internal fun `T13-3 a named class variable remains visible inside a THEN`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Coin<Person>",
            "ABSTRACT CLASS Receipt<Person>",
            "CLASS Offer<Person^P> { This: Coin<Person^P> THEN Receipt<Person^P> }",
        )
    val offer = table.getClass(cn("Offer"))
    val classScoped = offer.interpretTypeVariablesIn(offer.declaration.effects.single())
    val classVariable = classScoped.typeVariables.variables.single()

    "${classVariable.declaration.expression}" shouldBe "Person^P"
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
    val scoped =
        table
            .recordTypeVariableScopes()
            .transformEffect(
                parse(
                    "This: (Eligible<Person^Choice>: Coin<Person^Choice>) THEN " +
                        "Receipt<Person^Choice>"
                )
            )
    val then = scoped.instruction as Then
    val choice = then.typeVariables.variables.single()

    choice.name.toString() shouldBe "Choice"
    choice.occurrences.map { "${it.expression}" } shouldContainExactly
        listOf("Person^Choice", "Person^Choice", "Person^Choice")
    (choice.usages.first().ordinal < choice.declaration.ordinal) shouldBe true
    then.typeVariables
        .bind(mapOf(choice to table.resolve(te("Alice"))))
        .transformEffect(scoped)
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
      actors.recordTypeVariableScopes().transformEffect(parse<Effect>(source))

  @Test
  internal fun `T13-9 an unnamed actor selector is only a filter`() {
    names(actorEffect("Heat BY Player: Ok").typeVariables) shouldContainExactly listOf()
  }

  @Test
  internal fun `T13-9 an unnamed actor filter does not bind the instruction`() {
    val bound = actorEffect("Heat BY Player: Notice<Player>")

    names(bound.typeVariables) shouldContainExactly listOf()
    bound.toString() shouldBe "Heat BY Player: Notice<Player>"
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
            "Notice<Owner^Other(NOT Player^ActingPlayer)> BY Player^ActingPlayer: " +
                "Heat<Owner^Other>"
        )
    val actor = bound.typeVariables.variables.single { it.name == "ActingPlayer" }
    val event = bound.typeVariables.variables.single { it.name == "Other" }

    actor.occurrences.size shouldBe 3
    event.occurrences.size shouldBe 2
    bound.typeVariables
        .bind(mapOf(actor to actors.resolve(te("Player1"))))
        .transformEffect(bound)
        .toString() shouldBe "Notice<Owner^Other(NOT Player1)> BY Player1: Heat<Owner^Other>"
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
            .recordTypeVariableScopes()
            .transformEffect(
                parse<Effect>(
                    "Resource<Owner^Other(NOT Player^ActingPlayer)> " +
                        "BY Player^ActingPlayer: Notice<Owner^Other>"
                )
            )
    val actor = bound.typeVariables.variables.single { it.name == "ActingPlayer" }
    val afterActor =
        bound.typeVariables
            .bind(mapOf(actor to table.resolve(te("Player1"))))
            .transformEffect(bound)
    val event = afterActor.typeVariables.variables.single()

    event.bound shouldBe table.resolve(te("Owner"))
    "${afterActor.typeVariables.expressionOf(event.declaration)}" shouldBe
        "Owner^Other(NOT Player1)"

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
            .recordTypeVariableScopes()
            .transformEffect(
                parse<Effect>(
                    "StandardResource^R: Notice<StandardResource^R>, StandardResource, Steel"
                )
            )
    val variable = bound.typeVariables.variables.single()

    bound.typeVariables
        .bind(mapOf(variable to table.resolve(te("Plant"))))
        .transformEffect(bound)
        .toString() shouldBe "Plant: Notice<Plant>, StandardResource, Steel"
  }

  @Test
  internal fun `T13-10 binding follows marked occurrences through copied syntax`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS StandardResource { CLASS Plant, Steel }",
            "ABSTRACT CLASS Notice<StandardResource>",
        )
    val scoped =
        table
            .recordTypeVariableScopes()
            .transformEffect(
                parse<Effect>("StandardResource^R: Notice<StandardResource^R>, StandardResource")
            )
    val copier =
        object : PetTransformer() {
          override fun transformNode(node: PetNode): PetNode {
            val transformed = transformChildren(node)
            return if (transformed is Expression) transformed.copy() else transformed
          }
        }
    val copied = copier.transformEffect(scoped)
    val variable = copied.typeVariables.variables.single()

    copied.typeVariables
        .bind(mapOf(variable to table.resolve(te("Plant"))))
        .transformEffect(copied)
        .toString() shouldBe "Plant: Notice<Plant>, StandardResource"
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
            .recordTypeVariableScopes()
            .transformEffect(parse<Effect>("StandardResource^R: Notice<StandardResource^R>"))
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
            .recordTypeVariableScopes()
            .transformEffect(
                parse<Effect>("StandardResource^R(HAS Marker): Token<StandardResource^R>")
            )
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
    val trade = effect("StandardResource^R: StandardResource^R")
    val scope = trade.typeVariables
    val variable = scope.variables.single()

    scope.isEmpty shouldBe false
    TypeVariableScope.EMPTY.isEmpty shouldBe true
    scope.expressionsOf(variable).map { "$it" }.toSet() shouldBe
        setOf("StandardResource^R", "StandardResource^R")
    "${scope.expressionOf(variable.declaration)}" shouldBe "StandardResource^R"
    scope.variableAt(scope.expressionOf(variable.usages.single())) shouldBe variable
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
    val container = parse<Expression>("Container<Box<Person>>")
    val person =
        container.arguments
            .single()
            .arguments
            .single()
            .copy(typeVariableName = Declaration("P", cn("Person")))
    val authored =
        container.copy(
            arguments = listOf(container.arguments.single().copy(arguments = listOf(person)))
        )
    val scope =
        TypeVariableScope.fromDeclarations(
            listOf(authored),
            table,
            namedDeclarations = listOf(person),
        )

    scope
        .bindingsFrom(
            authored,
            table.resolve(authored),
            table.resolve(parse("Container<Box<Alice>>")),
        )
        .map { (variable, value) -> "$variable=$value" } shouldContainExactly listOf("P=Alice")
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
    val container = parse<Expression>("Container<Box<Person>>")
    val person =
        container.arguments
            .single()
            .arguments
            .single()
            .copy(typeVariableName = Declaration("P", cn("Person")))
    val authored =
        container.copy(
            arguments = listOf(container.arguments.single().copy(arguments = listOf(person)))
        )
    val scope =
        TypeVariableScope.fromDeclarations(
            listOf(authored),
            table,
            namedDeclarations = listOf(person),
        )

    scope.bindingsFrom(
        authored,
        table.resolve(authored),
        table.resolve(parse("Container<Hand>")),
    ) shouldBe emptyMap()
  }

  @Test
  internal fun `T13-11 capture follows only the selected dependency occurrence`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice, Bob }",
            "ABSTRACT CLASS Pair<Person, Person>",
        )
    val pair = parse<Expression>("Pair<Person, Person>")
    val person = pair.arguments.first().copy(typeVariableName = Declaration("P", cn("Person")))
    val authored = pair.copy(arguments = listOf(person, pair.arguments.last()))
    val scope =
        TypeVariableScope.fromDeclarations(
            listOf(authored),
            table,
            namedDeclarations = listOf(person),
        )

    scope
        .bindingsFrom(
            authored,
            table.resolve(authored),
            table.resolve(parse("Pair<Alice, Bob>")),
        )
        .map { (variable, value) -> "$variable=$value" } shouldContainExactly listOf("P=Alice")
  }
}
