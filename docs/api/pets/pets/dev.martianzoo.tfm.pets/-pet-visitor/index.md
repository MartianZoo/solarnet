//[pets](../../../index.md)/[dev.martianzoo.tfm.pets](../index.md)/[PetVisitor](index.md)

# PetVisitor

[jvm]\
class [PetVisitor](index.md)(val shouldContinue: ([PetNode](../../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)) -&gt; [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html))

See [PetNode.visitChildren](../../dev.martianzoo.tfm.pets.ast/-pet-node/visit-children.md).

## Constructors

| | |
|---|---|
| [PetVisitor](-pet-visitor.md) | [jvm]<br>fun [PetVisitor](-pet-visitor.md)(shouldContinue: ([PetNode](../../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)) -&gt; [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) |

## Functions

| Name | Summary |
|---|---|
| [visit](visit.md) | [jvm]<br>fun [visit](visit.md)(node: [PetNode](../../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)?)<br>fun [visit](visit.md)(vararg nodes: [PetNode](../../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)?)<br>fun [visit](visit.md)(nodes: [Iterable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterable/index.html)&lt;[PetNode](../../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)?&gt;) |

## Properties

| Name | Summary |
|---|---|
| [shouldContinue](should-continue.md) | [jvm]<br>val [shouldContinue](should-continue.md): ([PetNode](../../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)) -&gt; [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
