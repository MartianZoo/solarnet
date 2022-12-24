package dev.martianzoo.tfm.types

import com.google.common.graph.ElementOrder.insertion
import com.google.common.graph.Graph
import com.google.common.graph.GraphBuilder
import com.google.common.graph.Graphs.transitiveClosure
import com.google.common.graph.MutableGraph
import com.google.common.graph.Traverser
import dev.martianzoo.tfm.pets.ComponentDef
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.util.associateByStrict

// TODO restrict viz?
class PetClassLoader(val definitions: Map<String, ComponentDef>) : PetClassTable {
  constructor(definitions: Collection<ComponentDef>) :
      this(definitions.associateByStrict { it.name })

  internal val directSubclasses: MutableGraph<PetClass> =
      GraphBuilder.directed()
          .allowsSelfLoops(false)
          .nodeOrder(insertion<PetClass>())
          .build()

  internal val allSubclasses: Graph<PetClass> by lazy {
    require(frozen)
    transitiveClosure(directSubclasses)
  }

  private fun putSuperToSub(supe: PetClass, sub: PetClass) {
    require(!hasPath(sub, supe)) { "cycle between $supe and $sub"}
    directSubclasses.putEdge(supe, sub)
  }

  private fun hasPath(from: PetClass, to: PetClass) =
      Traverser.forGraph(directSubclasses).breadthFirst(from).contains(to)

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

  override fun all() = table.values.toSet().also { require(frozen) }

  override fun resolveWithDefaults(expression: TypeExpression): PetType {
    return resolve(applyDefaultsIn(expression, this))
  }

  override fun resolve(expression: TypeExpression): PetType {
    val specs: List<PetType> = expression.specializations.map { resolve(it) }
    val petClass = get(expression.className)
    try {
      return petClass.baseType.specialize(specs)
    } catch (e: Exception) {
      throw RuntimeException("1. trying to resolve $expression\npetClass is $petClass\nbaseType is ${petClass.baseType}\n", e)
    }
  }

  override fun isValid(expression: TypeExpression): Boolean {
    return try {
      resolve(expression)
      true
    } catch (e: RuntimeException) {
      false
    }
  }
}
