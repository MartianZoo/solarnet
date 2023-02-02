//[pets](../../index.md)/[dev.martianzoo.tfm.pets.ast](index.md)

# Package dev.martianzoo.tfm.pets.ast

## Types

| Name | Summary |
|---|---|
| [Action](-action/index.md) | [jvm]<br>data class [Action](-action/index.md)(val cost: [Action.Cost](-action/-cost/index.md)?, val instruction: [Instruction](-instruction/index.md)) : [PetNode](-pet-node/index.md) |
| [ClassName](-class-name/index.md) | [jvm]<br>data class [ClassName](-class-name/index.md)(asString: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [PetNode](-pet-node/index.md), [Comparable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-comparable/index.html)&lt;[ClassName](-class-name/index.md)&gt; |
| [Effect](-effect/index.md) | [jvm]<br>data class [Effect](-effect/index.md)(val trigger: [Effect.Trigger](-effect/-trigger/index.md), val instruction: [Instruction](-instruction/index.md), val automatic: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false) : [PetNode](-pet-node/index.md) |
| [From](-from/index.md) | [jvm]<br>sealed class [From](-from/index.md) : [PetNode](-pet-node/index.md) |
| [Instruction](-instruction/index.md) | [jvm]<br>sealed class [Instruction](-instruction/index.md) : [PetNode](-pet-node/index.md) |
| [PetNode](-pet-node/index.md) | [jvm]<br>sealed class [PetNode](-pet-node/index.md)<br>An API object that can be represented as PETS source code. |
| [Requirement](-requirement/index.md) | [jvm]<br>sealed class [Requirement](-requirement/index.md) : [PetNode](-pet-node/index.md) |
| [ScalarAndType](-scalar-and-type/index.md) | [jvm]<br>data class [ScalarAndType](-scalar-and-type/index.md)(val scalar: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 1, val typeExpr: [TypeExpr](-type-expr/index.md) = MEGACREDIT.type) : [PetNode](-pet-node/index.md) |
| [TypeExpr](-type-expr/index.md) | [jvm]<br>data class [TypeExpr](-type-expr/index.md)(val className: [ClassName](-class-name/index.md), val arguments: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeExpr](-type-expr/index.md)&gt; = listOf(), val refinement: [Requirement](-requirement/index.md)? = null, val link: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)? = null) : [PetNode](-pet-node/index.md)<br>A noun expression. May be a simple type (`ClassName`), a parameterized type (`Foo<Bar, Qux>`) or a *refined* type (`Foo<Bar(HAS 3 Qux)>(HAS Wau)`). A refined type is the combination of a real type with various predicates. |
