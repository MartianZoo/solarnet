//[pets](../../../index.md)/[dev.martianzoo.tfm.pets](../index.md)/[Parsing](index.md)

# Parsing

[jvm]\
object [Parsing](index.md)

## Functions

| Name | Summary |
|---|---|
| [parse](parse.md) | [jvm]<br>fun &lt;[T](parse.md)&gt; [parse](parse.md)(parser: Parser&lt;[T](parse.md)&gt;, source: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [T](parse.md) |
| [parseClassDeclarations](parse-class-declarations.md) | [jvm]<br>fun [parseClassDeclarations](parse-class-declarations.md)(declarationsSource: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ClassDeclaration](../../dev.martianzoo.tfm.data/-class-declaration/index.md)&gt;<br>Parses an entire PETS class declarations source file. |
| [parseOneLineClassDeclaration](parse-one-line-class-declaration.md) | [jvm]<br>fun [parseOneLineClassDeclaration](parse-one-line-class-declaration.md)(declarationSource: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ClassDeclaration](../../dev.martianzoo.tfm.data/-class-declaration/index.md) |
| [parsePets](parse-pets.md) | [jvm]<br>inline fun &lt;[P](parse-pets.md) : [PetNode](../../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)&gt; [parsePets](parse-pets.md)(elementSource: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [P](parse-pets.md)<br>Parses the PETS element in [elementSource](parse-pets.md), expecting a construct of type [P](parse-pets.md), and returning the parsed [P](parse-pets.md). [P](parse-pets.md) can only be one of the major elemental types like [Effect](../../dev.martianzoo.tfm.pets.ast/-effect/index.md), [Action](../../dev.martianzoo.tfm.pets.ast/-action/index.md), [Instruction](../../dev.martianzoo.tfm.pets.ast/-instruction/index.md), [TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md), etc.<br>[jvm]<br>fun &lt;[P](parse-pets.md) : [PetNode](../../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)&gt; [parsePets](parse-pets.md)(expectedType: [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;[P](parse-pets.md)&gt;, source: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [P](parse-pets.md)<br>[jvm]<br>fun &lt;[P](parse-pets.md) : [PetNode](../../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)&gt; [parsePets](parse-pets.md)(expectedType: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;[P](parse-pets.md)&gt;, source: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [P](parse-pets.md)<br>Non-reified version of `parse(source)`. |
