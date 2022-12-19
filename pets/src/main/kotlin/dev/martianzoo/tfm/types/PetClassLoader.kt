package dev.martianzoo.tfm.types

import com.google.common.graph.Graph
import com.google.common.graph.GraphBuilder
import com.google.common.graph.Graphs.reachableNodes
import com.google.common.graph.Graphs.transitiveClosure
import com.google.common.graph.MutableGraph
import dev.martianzoo.tfm.pets.ComponentDef
import dev.martianzoo.util.associateByCareful
import dev.martianzoo.util.toSetCareful

// it has access to all the data when it needs it
class PetClassLoader(val definitions: Map<String, ComponentDef>) : PetClassTable {
  constructor(definitions: Collection<ComponentDef>) :
      this(definitions.associateByCareful { it.name })

  internal val directSubclasses: MutableGraph<PetClass> = GraphBuilder.directed()
      .allowsSelfLoops(false)
      .expectedNodeCount(500)
      .build()

  internal val allSubclasses: Graph<PetClass> by lazy {
    require(frozen)
    transitiveClosure(directSubclasses)
  }

  private fun putSuperToSub(supe: PetClass, sub: PetClass) {
    require(supe !in reachableNodes(directSubclasses, sub)) { "cycle between $supe and $sub"}
    directSubclasses.putEdge(supe, sub)
  }

  private val table = mutableMapOf<String, PetClass>()

  private var frozen: Boolean = false

  override fun get(name: String) = table[name] ?: error(name)

  /** Returns the petclass named `name`, loading it first if necessary. */
  internal fun load(name: String) = table[name] ?: construct(definitions[name]!!)

  private fun construct(def: ComponentDef): PetClass {
    require(!frozen)

    // We can insist that the superclasses are defined first, since there can't be cycles
    val petClass = PetClass(def, this)
    require(table.put(petClass.name, petClass) == null)
    directSubclasses.addNode(petClass)
    def.superclassNames.map(::load).forEach { putSuperToSub(it, petClass) }
    return petClass
  }

  fun freeze(): PetClassTable {
    frozen = true
    return this
  }

  fun loadAll(): PetClassTable {
    definitions.keys.forEach(::load)
    return freeze()
  }

  override fun all() = table.values.toSetCareful().also { require(frozen) }
}
