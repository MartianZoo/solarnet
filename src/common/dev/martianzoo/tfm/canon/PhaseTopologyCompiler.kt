package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.data.ClassDeclaration

/** Compiles authored phase-order facts into the concrete scopes that advance each segment. */
internal object PhaseTopologyCompiler {
  private val MODULE = cn("Module")
  private val PHASE_AFTER = PropertyName("phaseAfter")
  private val PHASE_SCOPE = cn("PhaseScope")
  private val PHASE_SEGMENT = PropertyName("phaseSegment")

  fun lower(source: Collection<ClassDeclaration>): List<ClassDeclaration> {
    val byName = source.associateBy(ClassDeclaration::className)
    val modules = source.filter { isSubtypeOf(it.className, MODULE, byName) }
    val segments = source.mapNotNull { declaration ->
      metadata(declaration, PHASE_SEGMENT)?.let { phases ->
        if (phases.size != 2) invalid("phaseSegment must name its start and endpoint: $phases")
        if (phases.first() != declaration.className) {
          invalid("phaseSegment must be declared by its start phase: ${declaration.className}")
        }
        Segment(phases[0], phases[1])
      }
    }
    val edges = modules.flatMap { module ->
      metadata(module, PHASE_AFTER)?.let { phases ->
        if (phases.size < 2) invalid("phaseAfter must name a phase and its predecessors: $phases")
        val later = phases.first()
        phases.drop(1).map { earlier -> Edge(earlier, later, module.className) }
      } ?: emptyList()
    }
    if (segments.isEmpty()) {
      if (edges.isNotEmpty()) invalid("PhaseAfter constraints have no PhaseSegment")
      return source.toList()
    }
    if (segments.distinct().size != segments.size) invalid("duplicate PhaseSegment declarations")

    val compiled = segments.map { segment -> compile(segment, edges, byName) }
    val usedEdges = compiled.flatMapTo(linkedSetOf()) { it.edges }
    val unusedEdges = edges.toSet() - usedEdges
    if (unusedEdges.isNotEmpty()) {
      invalid("PhaseAfter constraints do not belong to one PhaseSegment: $unusedEdges")
    }

    val phaseContributions = compiled.flatMap { it.phaseEffects }
    val scopeContributions = compiled.flatMap { it.scopes }
    val additions = phaseContributions + scopeContributions
    val duplicateAdditions =
        additions.groupBy(ClassDeclaration::className).filterValues {
          it.size > 1
        }
    if (duplicateAdditions.isNotEmpty()) {
      invalid(
          "compiled topology contributes to a declaration more than once: ${duplicateAdditions.keys}"
      )
    }
    val additionsByName = additions.associateBy(ClassDeclaration::className)
    scopeContributions.forEach { contribution ->
      val existing = byName[contribution.className] ?: return@forEach
      val phaseScope = contribution.supertypes.single { it.className == PHASE_SCOPE }
      if (phaseScope !in existing.supertypes) {
        invalid("authored ${existing.className} must directly extend $phaseScope")
      }
    }

    return source.map { declaration ->
      val addition = additionsByName[declaration.className] ?: return@map declaration
      declaration.copy(
          authoredEffects = declaration.authoredEffects + addition.authoredEffects,
      )
    } + scopeContributions.filter { it.className !in byName }
  }

  private fun compile(
      segment: Segment,
      allEdges: List<Edge>,
      declarations: Map<ClassName, ClassDeclaration>,
  ): CompiledSegment {
    val reachable = linkedSetOf(segment.start)
    var changed: Boolean
    do {
      changed = false
      allEdges
          .filter { it.earlier in reachable }
          .forEach { edge ->
            if (reachable.add(edge.later)) changed = true
          }
    } while (changed)
    val edges = allEdges.filter { it.earlier in reachable && it.later in reachable }
    val phases = reachable.toList()
    if (segment.end in reachable) invalid("segment endpoint ${segment.end} is also a member")
    (phases + segment.end).forEach { phase ->
      if (phase !in declarations) invalid("unknown phase in topology: $phase")
    }

    val owners = mutableMapOf<ClassName, ClassName?>().apply { put(segment.start, null) }
    edges.forEach { edge ->
      val owner = owners.getOrPut(edge.later) { edge.module }
      if (owner != edge.module) {
        invalid("phase ${edge.later} has constraints owned by $owner and ${edge.module}")
      }
    }
    if (owners.keys != reachable) invalid("every non-start segment phase must have an owner")

    val order = uniqueOrder(phases, edges)
    order.indices.forEach { laterIndex ->
      for (earlierIndex in 0 until laterIndex) {
        val required =
            Edge(
                order[earlierIndex],
                order[laterIndex],
                owners.getValue(order[laterIndex])!!,
            )
        if (required !in edges) {
          invalid("phase ${required.later} must declare that it follows ${required.earlier}")
        }
      }
    }

    val phaseEffects = order.map { phase -> parsePhaseEffect(phase) }
    val scopes = order.mapIndexed { index, phase ->
      parseScope(phase, index, order, owners, segment.end)
    }
    return CompiledSegment(edges.toSet(), phaseEffects, scopes)
  }

  private fun uniqueOrder(phases: List<ClassName>, edges: List<Edge>): List<ClassName> {
    val remaining = phases.toMutableSet()
    val result = mutableListOf<ClassName>()
    while (remaining.isNotEmpty()) {
      val next = remaining.filter { candidate ->
        edges.none { edge -> edge.later == candidate && edge.earlier in remaining }
      }
      if (next.size != 1) invalid("phase constraints do not produce one order: $next in $remaining")
      result += next.single()
      remaining -= next.single()
    }
    return result
  }

  private fun parsePhaseEffect(phase: ClassName): ClassDeclaration =
      parseClasses(
              """
              CLASS $phase {
                This IF WorkflowStarted:: ${phase}Scope
              }
              """
                  .trimIndent()
          )
          .single()

  private fun parseScope(
      phase: ClassName,
      index: Int,
      order: List<ClassName>,
      owners: Map<ClassName, ClassName?>,
      endpoint: ClassName,
  ): ClassDeclaration {
    val transitions = mutableListOf<String>()
    for (targetIndex in index + 1 until order.size) {
      val target = order[targetIndex]
      val requirements = mutableListOf("WorkflowStarted")
      owners.getValue(target)?.let { module -> requirements.add(module.toString()) }
      order.subList(index + 1, targetIndex).mapNotNull(owners::getValue).distinct().mapTo(
          requirements
      ) { module ->
        "MAX 0 $module"
      }
      transitions += "-This IF ${requirements.distinct().joinToString()}:: $target FROM $phase"
    }
    val endpointRequirements = mutableListOf("WorkflowStarted")
    order.subList(index + 1, order.size).mapNotNull(owners::getValue).distinct().mapTo(
        endpointRequirements
    ) { module ->
      "MAX 0 $module"
    }
    transitions += "-This IF ${endpointRequirements.joinToString()}:: $endpoint FROM $phase"
    return parseClasses(
            """
            "The compiled lifetime anchor and continuation for $phase"
            CLASS ${phase}Scope : PhaseScope<$phase>, Temporary {
              ${transitions.joinToString("\n")}
            }
            """
                .trimIndent()
        )
        .single()
  }

  private fun metadata(
      declaration: ClassDeclaration,
      propertyName: PropertyName,
  ): List<ClassName>? {
    val value = declaration.properties[propertyName] as? RequirementValue ?: return null
    val expressions = value.value.descendantsOfType<Expression>()
    if (expressions.any { !it.simple }) {
      invalid("phase topology properties must name only simple phases: ${value.value}")
    }
    return expressions.map(Expression::className)
  }

  private fun isSubtypeOf(
      candidate: ClassName,
      superclass: ClassName,
      declarations: Map<ClassName, ClassDeclaration>,
      visited: Set<ClassName> = emptySet(),
  ): Boolean {
    if (candidate == superclass) return true
    if (candidate in visited) return false
    return declarations[candidate]?.supertypes.orEmpty().any { supertype ->
      isSubtypeOf(supertype.className, superclass, declarations, visited + candidate)
    }
  }

  private fun invalid(message: String): Nothing = throw InvalidPetDefinitionException(message)

  private data class Segment(val start: ClassName, val end: ClassName)

  private data class Edge(
      val earlier: ClassName,
      val later: ClassName,
      val module: ClassName,
  )

  private data class CompiledSegment(
      val edges: Set<Edge>,
      val phaseEffects: List<ClassDeclaration>,
      val scopes: List<ClassDeclaration>,
  )
}
