//[pets](../../index.md)/[dev.martianzoo.tfm.pets](index.md)/[childNodesOfType](child-nodes-of-type.md)

# childNodesOfType

[jvm]\
inline fun &lt;[P](child-nodes-of-type.md) : [PetNode](../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)&gt; [childNodesOfType](child-nodes-of-type.md)(root: [PetNode](../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[P](child-nodes-of-type.md)&gt;

fun &lt;[P](child-nodes-of-type.md) : [PetNode](../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)&gt; [childNodesOfType](child-nodes-of-type.md)(type: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;[P](child-nodes-of-type.md)&gt;, root: [PetNode](../dev.martianzoo.tfm.pets.ast/-pet-node/index.md)): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[P](child-nodes-of-type.md)&gt;

Returns every child node of [root](child-nodes-of-type.md) (including [root](child-nodes-of-type.md) itself) that is of type [P](child-nodes-of-type.md).
