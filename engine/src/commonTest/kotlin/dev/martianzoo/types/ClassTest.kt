package dev.martianzoo.types

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.Exceptions.PetException
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.data.ClassSelection
import dev.martianzoo.data.GameConfig
import dev.martianzoo.data.GamePremise
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.AbsentRequirementValue
import dev.martianzoo.pets.ast.PropertyValue.MetricType
import dev.martianzoo.pets.ast.PropertyValue.MetricValue
import dev.martianzoo.pets.ast.PropertyValue.NumberType
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import dev.martianzoo.pets.ast.PropertyValue.OptionalRequirementType
import dev.martianzoo.pets.ast.PropertyValue.RequirementType
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.api.Bundle
import dev.martianzoo.tfm.api.BundleContentSelection
import dev.martianzoo.tfm.api.BundleContentSelection.Kind.MILESTONES
import dev.martianzoo.tfm.api.TfmAuthority
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.util.toSetStrict
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ClassTest {
  @Test
  internal fun `metric properties narrow through number bounds literals and metric expressions`() {
    val table =
        loader(
            """
            ABSTRACT CLASS Area {
              score = Metric
            }
            ABSTRACT CLASS FixedArea : Area {
              score = Number
            }
            CLASS EightPointArea : FixedArea {
              score = 8
            }
            CLASS TemperatureArea : Area {
              score = COUNT "TemperatureStep"
            }
            CLASS SevenPointArea : Area {
              score = 7
            }
            """
                .trimIndent()
        )

    table.getClass(cn("Area")).properties[PropertyName("score")] shouldBe MetricType
    table.getClass(cn("FixedArea")).properties[PropertyName("score")] shouldBe NumberType
    table.getClass(cn("EightPointArea")).properties[PropertyName("score")] shouldBe NumberValue(8)
    table.getClass(cn("TemperatureArea")).properties[PropertyName("score")] shouldBe
        MetricValue(parse<Metric>("TemperatureStep"))
    table.getClass(cn("SevenPointArea")).properties[PropertyName("score")] shouldBe NumberValue(7)
  }

  @Test
  internal fun `requirement properties narrow to requirement expressions`() {
    val table =
        loader(
            """
            ABSTRACT CLASS Goal {
              requirement = Requirement
            }
            CLASS Gardener : Goal {
              requirement = HAS "3 Plant, MAX 2 Steel"
            }
            """
                .trimIndent()
        )

    table.getClass(cn("Goal")).properties[PropertyName("requirement")] shouldBe RequirementType
    table.getClass(cn("Gardener")).properties[PropertyName("requirement")] shouldBe
        RequirementValue(parse<Requirement>("3 Plant, MAX 2 Steel"))
  }

  @Test
  internal fun `optional requirement properties may be absent present or narrowed to required`() {
    val table =
        loader(
            """
            ABSTRACT CLASS Goal {
              requirement = Requirement?
            }
            CLASS OptionalGoal : Goal
            CLASS SpecificGoal : Goal {
              requirement = HAS "3 Plant"
            }
            ABSTRACT CLASS RequiredGoal : Goal {
              requirement = Requirement
            }
            CLASS SpecificRequiredGoal : RequiredGoal {
              requirement = HAS "2 Steel"
            }
            """
                .trimIndent()
        )

    table.getClass(cn("Goal")).properties[PropertyName("requirement")] shouldBe
        OptionalRequirementType
    table.getClass(cn("OptionalGoal")).properties[PropertyName("requirement")] shouldBe
        AbsentRequirementValue
    table.getClass(cn("SpecificGoal")).properties[PropertyName("requirement")] shouldBe
        RequirementValue(parse<Requirement>("3 Plant"))
    table.getClass(cn("RequiredGoal")).properties[PropertyName("requirement")] shouldBe
        RequirementType
    table.getClass(cn("SpecificRequiredGoal")).properties[PropertyName("requirement")] shouldBe
        RequirementValue(parse<Requirement>("2 Steel"))
  }

  @Test
  internal fun `property inheritance rejects incomplete concrete classes overrides and conflicts`() {
    shouldThrow<PetException> {
      loader("ABSTRACT CLASS Area { row = Number }\nCLASS ConcreteArea : Area")
    }
    shouldThrow<PetException> {
      loader("ABSTRACT CLASS Area { score = Metric }\nCLASS ConcreteArea : Area")
    }
    shouldThrow<PetException> {
      loader("ABSTRACT CLASS Goal { requirement = Requirement }\nCLASS ConcreteGoal : Goal")
    }
    shouldThrow<PetException> {
      loader(
          "ABSTRACT CLASS Area { row = Number }\n" +
              "ABSTRACT CLASS FixedArea : Area { row = 8 }\n" +
              "CLASS ConcreteArea : FixedArea { row = 9 }"
      )
    }
    shouldThrow<PetException> {
      loader(
          "ABSTRACT CLASS FirstArea { row = 8 }\n" +
              "ABSTRACT CLASS SecondArea { row = 8 }\n" +
              "CLASS ConcreteArea : FirstArea, SecondArea"
      )
    }
    shouldThrow<PetException> {
      loader(
          "ABSTRACT CLASS Area { row = Number }\n" +
              "ABSTRACT CLASS FirstArea : Area { row = 8 }\n" +
              "ABSTRACT CLASS SecondArea : Area { row = 8 }\n" +
              "CLASS ConcreteArea : FirstArea, SecondArea"
      )
    }
    shouldThrow<PetException> {
      loader(
          "ABSTRACT CLASS Area { row = Number }\n" +
              "CLASS ConcreteArea : Area { row = TemperatureStep }"
      )
    }
    shouldThrow<PetException> {
      loader(
          "ABSTRACT CLASS Area { score = Metric }\n" +
              "ABSTRACT CLASS FirstArea : Area { score = Number }\n" +
              "ABSTRACT CLASS SecondArea : Area { score = COUNT \"TemperatureStep\" }\n" +
              "CLASS ConcreteArea : FirstArea, SecondArea"
      )
    }
    shouldThrow<PetException> {
      loader(
          "ABSTRACT CLASS Goal { requirement = Requirement }\n" +
              "CLASS ConcreteGoal : Goal { requirement = TemperatureStep }"
      )
    }
  }

  @Test
  internal fun `diamond inheritance coalesces the selfsame property fact`() {
    val table =
        loader(
            """
            ABSTRACT CLASS Area { row = 8 }
            ABSTRACT CLASS FirstArea : Area
            ABSTRACT CLASS SecondArea : Area
            CLASS ConcreteArea : FirstArea, SecondArea
            """
                .trimIndent()
        )

    table.getClass(cn("ConcreteArea")).properties[PropertyName("row")] shouldBe NumberValue(8)
  }

  @Test
  internal fun `a narrower property fact wins when a broader inheritance path rejoins it`() {
    val table =
        loader(
            """
            ABSTRACT CLASS Area { score = Metric }
            ABSTRACT CLASS FixedArea : Area { score = Number }
            ABSTRACT CLASS OtherArea : Area
            ABSTRACT CLASS RejoinedArea : FixedArea, OtherArea
            CLASS ConcreteArea : RejoinedArea { score = 8 }
            """
                .trimIndent()
        )

    table.getClass(cn("RejoinedArea")).properties[PropertyName("score")] shouldBe NumberType
    table.getClass(cn("ConcreteArea")).properties[PropertyName("score")] shouldBe NumberValue(8)
  }

  @Test
  internal fun classLoadingUsesCanonicalNamesOnly() {
    val classes = parseClasses("CLASS Foo").toSetStrict()
    val authority =
        object : TfmAuthority() {
          override val explicitClassDeclarations = classes
        }

    shouldThrow<PetException> { ClassLoader(authority).load(cn("F")) }
  }

  @Test
  internal fun `effects cannot create class representatives`() {
    shouldThrow<PetException> {
      loader("CLASS Source { This:: Class<Target> }\nCLASS Target")
    }
  }

  @Test
  internal fun `root classes reject unexpected custom implementations`() {
    val authority =
        object : TfmAuthority() {
          override val customClasses = setOf(object : CustomClass(COMPONENT) {})
        }

    shouldThrow<PetException> { ClassLoader(authority) }
  }

  @Test
  internal fun nothingness() {
    val loader = loadTypes()
    val cpt = loader.componentClass
    cpt.abstract shouldBe true
    cpt.directSuperclasses.shouldBeEmpty()
    cpt.allSuperclasses().classNames().shouldContainExactlyInAnyOrder(COMPONENT)
    cpt.dependencies.keys.shouldBeEmpty()
  }

  @Test
  internal fun onethingness() {
    val loader = loadTypes("CLASS Foo")
    val foo = loader.getClass(cn("Foo"))
    foo.abstract shouldBe false
    foo.directSuperclasses.classNames().shouldContainExactlyInAnyOrder(COMPONENT)
    foo.allSuperclasses().classNames().shouldContainExactlyInAnyOrder(COMPONENT, cn("Foo"))
    foo.dependencies.keys.shouldBeEmpty()
  }

  @Test
  internal fun classTableEnumerationRequiresFreezeWithoutCapturingAnEarlySnapshot() {
    val classes = parseClasses("CLASS Foo").toSetStrict()
    val authority =
        object : TfmAuthority() {
          override val explicitClassDeclarations = classes
        }
    val loader = ClassLoader(authority)

    loader.findClass(cn("Foo")) shouldBe null
    shouldThrow<IllegalArgumentException> { loader.allClasses() }
    val foo = loader.load(cn("Foo"))
    loader.findClass(cn("Foo")) shouldBe foo

    val table = loader.freeze()
    table.getClass(cn("Foo")) shouldBe foo
    table.allClassNames shouldBe authority.allClassNames
  }

  @Test
  internal fun `custom class requirements load with the custom class only`() {
    val declarations =
        parseClasses(
                """
                CLASS DependencySource : Custom
                CLASS RuntimeDependency
                """
                    .trimIndent()
            )
            .toSetStrict()
    val implementation =
        object : CustomClass(cn("DependencySource")) {
          override val requiredClassNames = setOf(cn("RuntimeDependency"))
        }
    val authority =
        object : TfmAuthority() {
          override val explicitClassDeclarations = declarations
          override val customClasses = setOf(implementation)
        }

    val inactive = project(authority)
    inactive.isActive(cn("RuntimeDependency")) shouldBe false

    val loaded = project(authority, "DependencySource")
    loaded.isActive(cn("RuntimeDependency")) shouldBe true
  }

  @Test
  internal fun `custom classes cannot inherit Pets behavior and failed validation is not cached`() {
    listOf(
            "ABSTRACT CLASS BehavioralParent { Trigger: Result }\nCLASS Trigger, Result",
            "ABSTRACT CLASS BehavioralParent { HAS MAX 1 This }",
            "ABSTRACT CLASS BehavioralParent { DEFAULT +BehavioralParent. }",
        )
        .forEach { parentDeclaration ->
          val declarations =
              parseClasses("$parentDeclaration\nCLASS CustomChild : BehavioralParent, Custom")
                  .toSetStrict()
          val authority =
              object : TfmAuthority() {
                override val explicitClassDeclarations = declarations
                override val customClasses = setOf(object : CustomClass(cn("CustomChild")) {})
              }

          val loader = ClassLoader(authority)
          repeat(2) {
            shouldThrow<PetException> { loader.load(cn("CustomChild")) }
            loader.findClass(cn("CustomChild")) shouldBe null
          }
        }
  }

  @Test
  internal fun `authority-known inactive classes resolve structurally but are not enumerated`() {
    val activeBundle = bundle("ActiveBundle", "CLASS Active")
    val inactiveBundle =
        bundle(
            "InactiveBundle",
            """
            ABSTRACT CLASS InactiveBase
            CLASS Inactive : InactiveBase
            """,
        )
    val authority = TfmAuthority.compose(activeBundle, inactiveBundle)
    val table = project(authority, "Active")

    val inactive = table.getClass(cn("Inactive"))
    val inactiveBase = table.getClass(cn("InactiveBase"))
    table.isActive(inactive) shouldBe false
    table.isActive(inactiveBase) shouldBe false
    inactive.isSubtypeOf(inactiveBase) shouldBe true
    inactiveBase.isSubtypeOf(inactive) shouldBe false
    table.allSubclasses(inactive) shouldBe emptySet()
    table.allSubclasses(inactiveBase) shouldBe emptySet()
    table.isActive(table.resolve(te("Inactive"))) shouldBe false
    table.isActive(table.resolve(te("Class<Inactive>"))) shouldBe false
    table.allConcreteSubtypes(table.resolve(te("Inactive"))).toList() shouldBe emptyList()
    (inactive in table.allClasses()) shouldBe false
    (inactiveBase in table.allClasses()) shouldBe false
  }

  @Test
  internal fun `dependency signatures activate available vocabulary`() {
    val activeBundle =
        bundle(
            "ActiveBundle",
            """
            CLASS SelectedContent<AvailableVocabulary>
            CLASS AvailableVocabulary
            """,
        )
    val authority = TfmAuthority.compose(activeBundle)
    val table = project(authority, "SelectedContent")

    table.isActive(cn("AvailableVocabulary")) shouldBe true
  }

  @Test
  internal fun `excluding an inactive type does not make a complement dependency inactive`() {
    val activeBundle =
        bundle(
            "ActiveBundle",
            """
            ABSTRACT CLASS Domain
            ABSTRACT CLASS Holder<Domain>
            """,
        )
    val inactiveBundle = bundle("InactiveBundle", "CLASS Inactive : Domain")
    val authority = TfmAuthority.compose(activeBundle, inactiveBundle)
    val table = project(authority, "Holder")

    table.isActive(table.resolve(te("Holder<!Inactive>"))) shouldBe true
  }

  @Test
  internal fun `structural dependencies activate authority-known classes`() {
    val activeBundle = bundle("ActiveBundle", "CLASS Active<Inactive>")
    val inactiveBundle = bundle("InactiveBundle", "CLASS Inactive")
    val authority = TfmAuthority.compose(activeBundle, inactiveBundle)

    val table = project(authority, "Active")

    table.isActive(cn("Inactive")) shouldBe true
  }

  @Test
  internal fun `premise rejects a structurally activated unrequested Module`() {
    val authority =
        object : TfmAuthority() {
          override val explicitClassDeclarations =
              parseClasses(
                      """
                      ABSTRACT CLASS Module
                      CLASS Requested<Other> : Module
                      CLASS Other : Module
                      """
                          .trimIndent()
                  )
                  .toSetStrict()
        }
    val premise = GamePremise(authority, setOf(cn("Requested")), emptySet(), emptySet())

    shouldThrow<IllegalArgumentException> { ClassTable.forPremise(premise) }
  }

  @Test
  internal fun `premise rejects structural reactivation of an excluded class`() {
    val authority =
        object : TfmAuthority() {
          override val explicitClassDeclarations =
              parseClasses("CLASS Active<Excluded>\nCLASS Excluded").toSetStrict()
        }
    val premise =
        GamePremise(
            authority,
            emptySet(),
            setOf(ClassSelection(cn("Active")), ClassSelection(cn("Excluded"), included = false)),
            emptySet(),
        )

    shouldThrow<IllegalArgumentException> { ClassTable.forPremise(premise) }
  }

  @Test
  internal fun `premise rejects structural reactivation of a conditionally excluded class`() {
    val authority =
        object : Bundle(cn("ConditionalBundle")) {
          override val explicitClassDeclarations =
              parseClasses(
                      """
                      ABSTRACT CLASS Module
                      ABSTRACT CLASS Milestone { requirement = Requirement }
                      CLASS Requested : Module { This:: Active }
                      CLASS ConditionalMilestone : Milestone {
                        HAS Flag
                        requirement = HAS "MAX 0 Flag"
                      }
                      CLASS Active<ConditionalMilestone>
                      CLASS Flag
                      """
                          .trimIndent()
                  )
                  .toSetStrict()
          override val moduleContentSelections =
              mapOf(
                  cn("Requested") to
                      setOf(BundleContentSelection(cn("ConditionalBundle"), setOf(MILESTONES)))
              )
        }
    val premise = authority.gamePremise(GameConfig("Requested"))

    shouldThrow<IllegalArgumentException> { ClassTable.forPremise(premise) }
  }

  @Test
  internal fun `class metrics do not activate the represented class`() {
    val activeBundle = bundle("ActiveBundle", "CLASS Querying { HAS MAX 0 Class<Inactive> }")
    val inactiveBundle = bundle("InactiveBundle", "CLASS Inactive")
    val authority = TfmAuthority.compose(activeBundle, inactiveBundle)
    val table = project(authority, "Querying")

    table.isActive(cn("Inactive")) shouldBe false
  }

  @Test
  internal fun `reachable constructive instructions activate their destination`() {
    val authority =
        bundle(
            "Bundle",
            """
            CLASS Active { This:: Constructed }
            CLASS Constructed
            """,
        )

    val table = project(authority, "Active")

    table.isActive(cn("Constructed")) shouldBe true
  }

  @Test
  internal fun `configuration counting does not treat a mixed hierarchy as Module-only`() {
    val authority =
        bundle(
            "MixedBundle",
            """
            ABSTRACT CLASS Module
            ABSTRACT CLASS Mixed
            CLASS SelectedModule : Module, Mixed
            CLASS Ordinary : Mixed
            CLASS Source { This IF 2 Mixed: Constructed }
            CLASS Constructed
            """,
        )
    val premise =
        GamePremise(
            authority,
            setOf(cn("SelectedModule")),
            setOf(ClassSelection(cn("Ordinary")), ClassSelection(cn("Source"))),
            emptySet(),
        )

    val table = ClassTable.forPremise(premise)

    table.isActive(cn("Constructed")) shouldBe true
  }

  @Test
  internal fun `bare trigger does not activate its externally issued protocol`() {
    val authority =
        bundle(
            "Bundle",
            """
            CLASS Active { Protocol: Constructed }
            CLASS Protocol
            CLASS Constructed
            """,
        )

    val table = project(authority, "Active")

    table.isActive(cn("Protocol")) shouldBe false
    table.isActive(cn("Constructed")) shouldBe false
  }

  @Test
  internal fun `positive invariants activate their required inhabitants`() {
    val authority = bundle("Bundle", "CLASS Active { HAS =1 Required }\nCLASS Required")

    val table = project(authority, "Active")

    table.isActive(cn("Required")) shouldBe true
  }

  @Test
  internal fun `constructive instructions activate only when their trigger and gate can be reached`() {
    val authority =
        bundle(
            "Bundle",
            """
            CLASS Active {
              InactiveTrigger<InactiveTriggerArgument>: Triggered
              This:: (Class<InactiveGate>: Gated)
            }
            CLASS InactiveTrigger<InactiveTriggerArgument>
            CLASS InactiveTriggerArgument
            CLASS Triggered
            CLASS InactiveGate
            CLASS Gated
            """,
        )

    val dormant = project(authority, "Active")
    val reachable =
        project(authority, "Active", "InactiveTrigger", "InactiveTriggerArgument", "InactiveGate")

    dormant.isActive(cn("Triggered")) shouldBe false
    dormant.isActive(cn("Gated")) shouldBe false
    dormant.isActive(cn("InactiveTrigger")) shouldBe false
    reachable.isActive(cn("Triggered")) shouldBe true
    reachable.isActive(cn("Gated")) shouldBe true
    reachable.isActive(cn("InactiveTrigger")) shouldBe true
  }

  @Test
  internal fun `structural supertypes become active`() {
    val activeBundle = bundle("ActiveBundle", "CLASS Active : Inactive")
    val inactiveBundle = bundle("InactiveBundle", "ABSTRACT CLASS Inactive")
    val authority = TfmAuthority.compose(activeBundle, inactiveBundle)

    val table = project(authority, "Active")

    table.isActive(cn("Inactive")) shouldBe true
  }

  @Test
  internal fun subclass() {
    val loader = loadTypes("ABSTRACT CLASS Foo", "CLASS Bar : Foo")
    val component = loader.componentClass
    val foo = loader.getClass(cn("Foo"))
    val bar = loader.getClass(cn("Bar"))
    bar.directSuperclasses.classNames().shouldContainExactlyInAnyOrder(cn("Foo"))
    bar.allSuperclasses()
        .classNames()
        .shouldContainExactlyInAnyOrder(COMPONENT, cn("Foo"), cn("Bar"))
    component.directSubclasses().contains(foo) shouldBe true
    component.allSubclasses().containsAll(setOf(component, foo, bar)) shouldBe true
    foo.directSubclasses() shouldBe setOf(bar)
    foo.allSubclasses() shouldBe setOf(foo, bar)
    bar.directSubclasses() shouldBe emptySet()
    bar.allSubclasses() shouldBe setOf(bar)
    bar.dependencies.keys.shouldBeEmpty()
  }

  @Test
  internal fun `frozen subclass masks cross machine-word boundaries`() {
    val levels =
        (0 until 70).map { index ->
          if (index == 0) "ABSTRACT CLASS Level0"
          else "ABSTRACT CLASS Level$index : Level${index - 1}"
        }
    val declarations =
        levels +
            listOf("CLASS Leaf : Level69", "CLASS Unrelated", "ABSTRACT CLASS ChildlessAbstract")
    val loader = loadTypes(*declarations.toTypedArray())
    val leaf = loader.getClass(cn("Leaf"))
    val childlessAbstract = loader.getClass(cn("ChildlessAbstract"))

    leaf.isSubtypeOf(leaf) shouldBe true
    listOf(0, 63, 64, 69).forEach { index ->
      leaf.isSubtypeOf(loader.getClass(cn("Level$index"))) shouldBe true
    }
    leaf.isSubtypeOf(loader.getClass(cn("Unrelated"))) shouldBe false
    childlessAbstract.isSubtypeOf(childlessAbstract) shouldBe true
    leaf.isSubtypeOf(childlessAbstract) shouldBe false
  }

  @Test
  internal fun forwardReference() {
    val loader = loadTypes("CLASS Bar : Foo", "ABSTRACT CLASS Foo")
    val bar = loader.getClass(cn("Bar"))
    bar.directSuperclasses.classNames().shouldContainExactlyInAnyOrder(cn("Foo"))
    bar.allSuperclasses()
        .classNames()
        .shouldContainExactlyInAnyOrder(COMPONENT, cn("Foo"), cn("Bar"))
    bar.dependencies.keys.shouldBeEmpty()
  }

  @Test
  internal fun concreteSuperclassRejected() {
    shouldThrow<PetException> { loadTypes("CLASS Foo", "CLASS Bar : Foo") }
    shouldThrow<PetException> {
      loadTypes("CLASS Foo", "ABSTRACT CLASS Bar : Foo")
    }
  }

  @Test
  internal fun cycle() {
    val s =
        """
      CLASS Foo : Bar
      CLASS Bar : Foo
    """
    shouldThrow<PetException> { loader(s) }
  }

  @Test
  internal fun trivialCycle() {
    val s =
        """
      CLASS Foo : Foo
    """
    shouldThrow<PetException> { loader(s) }
  }

  @Test
  internal fun dependency() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar<Foo>")
    val bar = loader.getClass(cn("Bar"))
    bar.directSuperclasses.classNames().shouldContainExactlyInAnyOrder(COMPONENT)
    bar.dependencies.keys.shouldContainExactlyInAnyOrder(Key(cn("Bar"), 0))
  }

  @Test
  internal fun inheritedDependency() {
    val loader = loadTypes("CLASS Foo", "ABSTRACT CLASS Bar<Foo>", "CLASS Qux : Bar")
    val bar = loader.getClass(cn("Bar"))
    val qux = loader.getClass(cn("Qux"))
    qux.directSuperclasses.classNames().shouldContainExactlyInAnyOrder(cn("Bar"))

    val key = Key(cn("Bar"), 0)
    bar.dependencies.keys.shouldContainExactlyInAnyOrder(key)
    qux.dependencies.keys.shouldContainExactlyInAnyOrder(key)
  }

  @Test
  internal fun restatedDependency() {
    val loader = loadTypes("CLASS Foo", "ABSTRACT CLASS Bar<Foo>", "CLASS Qux : Bar<Foo>")
    val bar = loader.getClass(cn("Bar"))
    val qux = loader.getClass(cn("Qux"))
    qux.directSuperclasses.classNames().shouldContainExactlyInAnyOrder(cn("Bar"))

    val key = Key(cn("Bar"), 0)
    bar.dependencies.keys.shouldContainExactlyInAnyOrder(key)
    qux.dependencies.keys.shouldContainExactlyInAnyOrder(key)
  }

  @Test
  internal fun addedDependency() {
    val loader =
        loadTypes("CLASS Foo", "ABSTRACT CLASS Bar<Foo>", "CLASS Baz", "CLASS Qux<Baz> : Bar<Foo>")
    val bar = loader.getClass(cn("Bar"))
    val qux = loader.getClass(cn("Qux"))

    bar.dependencies.keys.shouldContainExactlyInAnyOrder(Key(cn("Bar"), 0))
    qux.dependencies.keys.shouldContainExactlyInAnyOrder(
        Key(cn("Bar"), 0),
        Key(cn("Qux"), 0),
    )
  }

  @Test
  internal fun refinedDependency() {
    val loader =
        loadTypes(
            "ABSTRACT CLASS Foo",
            "ABSTRACT CLASS Bar<Foo>",
            "CLASS Baz : Foo",
            "CLASS Qux : Bar<Baz>",
        )
    val bar = loader.getClass(cn("Bar"))
    val qux = loader.getClass(cn("Qux"))
    qux.directSuperclasses.classNames().shouldContainExactlyInAnyOrder(cn("Bar"))

    val key = Key(cn("Bar"), 0)
    bar.dependencies.keys.shouldContainExactlyInAnyOrder(key)
    qux.dependencies.keys.shouldContainExactlyInAnyOrder(key)
  }

  @Test
  internal fun cycleDependency() {
    loadTypes("CLASS Foo<Bar>", "CLASS Bar<Foo>")
  }

  @Test
  internal fun depsAndSpecs() {
    val table =
        loadTypes(
            "ABSTRACT CLASS SuperFoo",
            "ABSTRACT CLASS Foo : SuperFoo",
            "CLASS SubFoo : Foo",
            "ABSTRACT CLASS SuperBar<SuperFoo>",
            "ABSTRACT CLASS Bar : SuperBar<Foo>",
            "CLASS SubBar : Bar<SubFoo>",
            "CLASS Qux",
        )

    // abstract: SuperFoo, SuperBar, Foo
    val supSup = table.resolve(te("SuperBar<SuperFoo>"))
    val supFoo = table.resolve(te("SuperBar<Foo>"))
    val supSub = table.resolve(te("SuperBar<SubFoo>"))
    val barFoo = table.resolve(te("Bar<Foo>"))
    val barSub = table.resolve(te("Bar<SubFoo>"))
    val subSub = table.resolve(te("SubBar<SubFoo>"))

    supSup.abstract shouldBe true
    supSup.isSubtypeOf(supSup) shouldBe true

    supFoo.abstract shouldBe true
    supFoo.isSubtypeOf(supSup) shouldBe true
    supFoo.isSubtypeOf(supFoo) shouldBe true

    supSub.abstract shouldBe true
    supSub.isSubtypeOf(supSup) shouldBe true
    supSub.isSubtypeOf(supFoo) shouldBe true
    supSub.isSubtypeOf(supSub) shouldBe true

    barFoo.abstract shouldBe true
    barFoo.isSubtypeOf(supSup) shouldBe true
    barFoo.isSubtypeOf(supFoo) shouldBe true
    barFoo.isSubtypeOf(barFoo) shouldBe true

    barSub.abstract shouldBe true
    barSub.isSubtypeOf(supSup) shouldBe true
    barSub.isSubtypeOf(supFoo) shouldBe true
    barSub.isSubtypeOf(supSub) shouldBe true
    barSub.isSubtypeOf(barFoo) shouldBe true
    barSub.isSubtypeOf(barSub) shouldBe true

    subSub.abstract shouldBe false
    subSub.isSubtypeOf(supSup) shouldBe true
    subSub.isSubtypeOf(supFoo) shouldBe true
    subSub.isSubtypeOf(supSub) shouldBe true
    subSub.isSubtypeOf(barFoo) shouldBe true
    subSub.isSubtypeOf(barSub) shouldBe true
    subSub.isSubtypeOf(subSub) shouldBe true

    fun checkAutoAdjust(`in`: String, out: String, classTable: ClassTable) =
        classTable.resolve(te(`in`)).expressionFull.toString() shouldBe out

    checkAutoAdjust("Bar<SuperFoo>", "Bar<Foo>", table)
    checkAutoAdjust("SubBar<SuperFoo>", "SubBar<SubFoo>", table)
    checkAutoAdjust("SubBar<Foo>", "SubBar<SubFoo>", table)

    assertFails("outta bounds") { table.resolve(te("Foo<Qux>")) }
    assertFails("no deps") { table.resolve(te("Foo<Bar>")) }
  }

  @Test
  internal fun testLubOne() {
    val (cpt, foo) = loadAndGetClasses("Foo")
    cpt.lub(cpt) shouldBe cpt
    cpt.lub(foo) shouldBe cpt
    foo.lub(cpt) shouldBe cpt
    foo.lub(foo) shouldBe foo
  }

  @Test
  internal fun testLubSibling() {
    val (cpt, foo, bar) = loadAndGetClasses("Foo", "Bar")
    foo.lub(bar) shouldBe cpt
  }

  @Test
  internal fun testLubParent() {
    val table = loadTypes("ABSTRACT CLASS Foo", "CLASS Bar : Foo")
    val cpt = table.componentClass
    val foo = table.getClass(cn("Foo"))
    val bar = table.getClass(cn("Bar"))
    cpt.lub(cpt) shouldBe cpt
    cpt.lub(foo) shouldBe cpt
    cpt.lub(bar) shouldBe cpt
    foo.lub(cpt) shouldBe cpt
    foo.lub(foo) shouldBe foo
    foo.lub(bar) shouldBe foo
    bar.lub(cpt) shouldBe cpt
    bar.lub(foo) shouldBe foo
    bar.lub(bar) shouldBe bar
  }

  @Test
  internal fun testLubNibling() {
    val table = loadTypes("CLASS Foo", "ABSTRACT CLASS Bar", "CLASS Qux : Bar")
    val cpt = table.componentClass
    val foo = table.getClass(cn("Foo"))
    val bar = table.getClass(cn("Bar"))
    val qux = table.getClass(cn("Qux"))
    qux.lub(qux) shouldBe qux

    cpt.lub(qux) shouldBe cpt
    foo.lub(qux) shouldBe cpt
    bar.lub(qux) shouldBe bar

    qux.lub(cpt) shouldBe cpt
    qux.lub(foo) shouldBe cpt
    qux.lub(bar) shouldBe bar
  }

  @Test
  internal fun classTypes() {
    val loader = loadTypes("CLASS Foo", "CLASS Bar", "CLASS Qux")

    assertFails { loader.resolve(te("Class<Class<Class>>")) }
    assertFails { loader.resolve(te("Class<Class<Foo>>")) }
    assertFails { loader.resolve(te("Class<Foo<Bar>>")) }
    assertFails { loader.resolve(te("Class<Foo, Bar>")) }
    assertFails { loader.resolve(te("Qux<Class<Foo<Bar>>>")) }
    assertFails { loader.resolve(te("Qux<Class<Foo, Bar>>")) }
    assertFails { loader.resolve(te("Class<Class<Component>>")) }
  }

  @Test
  internal fun unknownClassTypes() {
    val declarations =
        arrayOf(
            "CLASS Foo<Class<Component>>",
            "CLASS Querying { HAS MAX 0 Class<AnyWordHere> }",
        )
    val loaderWithOptionalClass =
        loadTypes(
            *declarations,
            "CLASS AnyWordHere",
        )
    val known = loaderWithOptionalClass.resolve(te("Class<AnyWordHere>"))

    known.abstract shouldBe false
    known.allConcreteSubtypes().toList().single() shouldBe known
    shouldThrow<PetException> { loadTypes(*declarations) }
    loaderWithOptionalClass.resolve(te("Foo<Class<AnyWordHere>>")).abstract shouldBe false
  }
}

private fun project(authority: TfmAuthority, vararg activeClassNames: String): ClassTable =
    ClassTable.forPremise(
        GamePremise(
            authority,
            emptySet(),
            activeClassNames.mapTo(linkedSetOf()) { ClassSelection(cn(it)) },
            emptySet(),
        )
    )

private fun bundle(name: String, declarations: String): Bundle =
    object : Bundle(cn(name)) {
      override val explicitClassDeclarations = parseClasses(declarations).toSetStrict()
    }

internal fun loader(petsText: String): ClassTable {
  val classes = parseClasses(petsText).toSetStrict()
  val authority =
      object : TfmAuthority() {
        override val explicitClassDeclarations = classes
      }
  return ClassLoader(authority).loadEverything()
}

private val regex = Regex("^(\\w+).*")

private fun loadAndGetClasses(vararg decl: String): List<Class> {
  val all =
      """
        ${decl.joinToString("") { "CLASS $it\n" }}
      """
  val loader = loader(all)
  val strings = listOf("Component") + decl.map { regex.matchEntire(it)!!.groupValues[1] }
  return strings.map { loader.getClass(cn(it)) }
}
