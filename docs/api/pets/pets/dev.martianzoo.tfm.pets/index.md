//[pets](../../index.md)/[dev.martianzoo.tfm.pets](index.md)

# Package dev.martianzoo.tfm.pets

## Types

| Name | Summary |
|---|---|
| [ClassDeclarationParsers](-class-declaration-parsers/index.md) | [jvm]<br>object [ClassDeclarationParsers](-class-declaration-parsers/index.md) : [PetParser](-pet-parser/index.md)<br>Parses the PETS language. |
| [Parsing](-parsing/index.md) | [jvm]<br>object [Parsing](-parsing/index.md) |
| [PetException](-pet-exception/index.md) | [jvm]<br>class [PetException](-pet-exception/index.md)(val message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;(no message)&quot;) : [Exception](https://docs.oracle.com/javase/8/docs/api/java/lang/Exception.html) |
| [PetParser](-pet-parser/index.md) | [jvm]<br>open class [PetParser](-pet-parser/index.md)<br>Parses the Petaform language. |
| [PetTransformer](-pet-transformer/index.md) | [jvm]<br>abstract class [PetTransformer](-pet-transformer/index.md) |
| [PetVisitor](-pet-visitor/index.md) | [jvm]<br>class [PetVisitor](-pet-visitor/index.md)(val shouldContinue: ([PetNode](../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)) -&gt; [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html))<br>See [PetNode.visitChildren](../dev.martianzoo.tfm.pets.ast/-pet-node/visit-children.md). |
| [SpecialClassNames](-special-class-names/index.md) | [jvm]<br>object [SpecialClassNames](-special-class-names/index.md) |

## Functions

| Name | Summary |
|---|---|
| [childNodesOfType](child-nodes-of-type.md) | [jvm]<br>inline fun &lt;[P](child-nodes-of-type.md) : [PetNode](../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)&gt; [childNodesOfType](child-nodes-of-type.md)(root: [PetNode](../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[P](child-nodes-of-type.md)&gt;<br>fun &lt;[P](child-nodes-of-type.md) : [PetNode](../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)&gt; [childNodesOfType](child-nodes-of-type.md)(type: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;[P](child-nodes-of-type.md)&gt;, root: [PetNode](../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[P](child-nodes-of-type.md)&gt;<br>Returns every child node of [root](child-nodes-of-type.md) (including [root](child-nodes-of-type.md) itself) that is of type [P](child-nodes-of-type.md). |
| [countNodesInTree](count-nodes-in-tree.md) | [jvm]<br>fun [countNodesInTree](count-nodes-in-tree.md)(root: [PetNode](../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [deprodify](deprodify.md) | [jvm]<br>fun &lt;[P](deprodify.md) : [PetNode](../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)&gt; [deprodify](deprodify.md)(node: [P](deprodify.md), producible: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[ClassName](../dev.martianzoo.tfm.pets.ast/-class-name/index.md)&gt;): [P](deprodify.md) |
| [myThrow](my-throw.md) | [jvm]<br>fun [myThrow](my-throw.md)(result: ErrorResult) |
| [parseRepeated](parse-repeated.md) | [jvm]<br>fun &lt;[T](parse-repeated.md)&gt; [parseRepeated](parse-repeated.md)(listParser: Parser&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[T](parse-repeated.md)&gt;&gt;, tokens: TokenMatchesSequence): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[T](parse-repeated.md)&gt; |
| [replaceThis](replace-this.md) | [jvm]<br>fun &lt;[P](replace-this.md) : [PetNode](../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)&gt; [replaceThis](replace-this.md)(node: [P](replace-this.md), resolveTo: [TypeExpr](../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)): [P](replace-this.md) |
| [replaceTypes](replace-types.md) | [jvm]<br>fun &lt;[P](replace-types.md) : [PetNode](../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)&gt; [P](replace-types.md).[replaceTypes](replace-types.md)(from: [TypeExpr](../dev.martianzoo.tfm.pets.ast/-type-expr/index.md), to: [TypeExpr](../dev.martianzoo.tfm.pets.ast/-type-expr/index.md)): [P](replace-types.md) |
| [visit](visit.md) | [jvm]<br>fun [visit](visit.md)(node: [PetNode](../dev.martianzoo.tfm.pets.ast/-pet-node/index.md), visitor: ([PetNode](../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)) -&gt; [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) |
| [visitAll](visit-all.md) | [jvm]<br>fun [visitAll](visit-all.md)(node: [PetNode](../dev.martianzoo.tfm.pets.ast/-pet-node/index.md), visitor: ([PetNode](../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
