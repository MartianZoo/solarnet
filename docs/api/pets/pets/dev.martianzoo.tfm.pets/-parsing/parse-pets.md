//[pets](../../../index.md)/[dev.martianzoo.tfm.pets](../index.md)/[Parsing](index.md)/[parsePets](parse-pets.md)

# parsePets

[jvm]\
inline fun &lt;[P](parse-pets.md) : [PetNode](../../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)&gt; [parsePets](parse-pets.md)(elementSource: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [P](parse-pets.md)

Parses the PETS element in [elementSource](parse-pets.md), expecting a construct of type [P](parse-pets.md), and returning the parsed [P](parse-pets.md). [P](parse-pets.md) can only be one of the major elemental types like [Effect](../../dev.martianzoo.tfm.pets.ast/-effect/index.md), [Action](../../dev.martianzoo.tfm.pets.ast/-action/index.md), [Instruction](../../dev.martianzoo.tfm.pets.ast/-instruction/index.md), [TypeExpr](../../dev.martianzoo.tfm.pets.ast/-type-expr/index.md), etc.

[jvm]\
fun &lt;[P](parse-pets.md) : [PetNode](../../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)&gt; [parsePets](parse-pets.md)(expectedType: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;[P](parse-pets.md)&gt;, source: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [P](parse-pets.md)

Non-reified version of `parse(source)`.

[jvm]\
fun &lt;[P](parse-pets.md) : [PetNode](../../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)&gt; [parsePets](parse-pets.md)(expectedType: [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;[P](parse-pets.md)&gt;, source: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [P](parse-pets.md)
