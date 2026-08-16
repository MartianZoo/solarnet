package dev.martianzoo.tools

import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.data.GameConfig
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.World
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.types.Class as PetsClass
import dev.martianzoo.types.ClassTable
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.DependencySet.DependencyPath
import dev.martianzoo.types.Type
import java.math.BigInteger
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.system.measureTimeMillis

@Suppress("LargeClass") // Keeping the report calculations and formatting together aids comparison.
internal object TypeStructureReport {
  private val selectedOptions: Set<ClassName> =
      setOf(
          cn("TerraformingMars"),
          cn("CorporateEraExpansion"),
          cn("TharsisMapOption"),
          cn("VenusNextExpansion"),
          cn("PreludeExpansion"),
          cn("ColoniesExpansion"),
          cn("TurmoilCardPack"),
          cn("PromoCardPack"),
      )

  fun createGame(): World {
    val colonyCount = if (PLAYERS == 1) 3 else if (PLAYERS == 2) 5 else PLAYERS + 2
    val colonies =
        Canon.colonyTileDefinitions.map { it.className }.sorted().take(colonyCount).toSet()
    return Engine.newGame(
        Canon.gamePremise(
            GameConfig.create(
                included = selectedOptions + colonies,
                playerClassNames = (1..PLAYERS).map { cn("Player$it") },
            )
        )
    )
  }

  @Suppress("CyclomaticComplexMethod") // The branches compute independent report statistics.
  fun render(game: World): String {
    val table = game.classTable
    val authority = game.reader.authority
    val active = table.allClasses().sortedBy { it.className }
    val known =
        (authority.allClassDeclarations.keys.mapNotNull(table::findClass) + active)
            .distinct()
            .sortedBy { it.className }
    val phantom = known.filter(PetsClass::phantom)
    val concrete = active.filterNot(PetsClass::abstract)
    val abstract = active.filter(PetsClass::abstract)
    val bitBearingSuperclassesUnsorted = known.flatMap(PetsClass::directSuperclasses).distinct()
    val properSubclassCounts = bitBearingSuperclassesUnsorted.associateWith { superclass ->
      known.count { it !== superclass && superclass in it.allSuperclasses() }
    }
    val bitBearingSuperclasses =
        bitBearingSuperclassesUnsorted.sortedWith(
            compareBy<PetsClass>(PetsClass::phantom)
                .thenByDescending { properSubclassCounts.getValue(it) }
                .thenBy(PetsClass::className)
        )
    check(bitBearingSuperclasses.all(PetsClass::abstract))
    val superclassBits =
        bitBearingSuperclasses.withIndex().associate { (index, klass) -> klass to index }

    fun compiledMaskWords(klass: PetsClass): Int =
        klass.allSuperclasses().mapNotNull(superclassBits::get).maxOrNull()?.let {
          wordsFor(it + 1)
        } ?: 0

    val activeCompiledMaskWords = active.map(::compiledMaskWords)
    val knownCompiledMaskWords = known.map(::compiledMaskWords)
    val knownCompiledSetBits =
        known.sumOf { klass -> klass.allSuperclasses().count(superclassBits::containsKey) }.toLong()

    val depthCache = mutableMapOf<PetsClass, Int>()
    fun depth(klass: PetsClass): Int =
        depthCache.getOrPut(klass) {
          klass.directSuperclasses.maxOfOrNull(::depth)?.plus(1) ?: 0
        }

    val directEdges = active.sumOf { it.directSuperclasses.size }
    val ancestorCounts = active.map { it.allSuperclasses().size }
    val descendantCounts = active.map { it.allSubclasses().size }
    val subtypePairs = ancestorCounts.sumOf(Int::toLong)
    val dependencyCounts = active.map { it.dependencies.keys.size }
    val declaredDependencyCounts = active.map { it.declaration.dependencies.size }
    val dependencyPathCounts = active.map { it.dependencies.flatten().size }
    val dependencyDepths = active.map { typeDepth(it.baseType) }
    val concreteDescendantCounts = active.map { klass ->
      klass.allSubclasses().count { !it.abstract }
    }

    val intersectionTypes = active.filter(PetsClass::isIntersectionType)
    val multipleInheritance = active.filter { it.directSuperclasses.size > 1 }
    val emptyAbstract = abstract.filter { klass -> klass.allSubclasses().none { !it.abstract } }
    val equalConcreteExtensions =
        active
            .groupBy { klass ->
              klass.allSubclasses().filterNot(PetsClass::abstract).map { it.className }.toSet()
            }
            .values
            .filter { it.size > 1 }

    val flattenedChoiceProducts = concrete.associateWith(::flattenedChoiceProduct)
    val dependencyLinks = dependencyLinks(active, table)
    val concreteTypeCounter = ConcreteTypeCounter(table, dependencyLinks)
    val rootCounts = linkedMapOf<PetsClass, RootCount>()
    val groundMillis = measureTimeMillis {
      concrete.forEach { klass -> rootCounts[klass] = countRootTypes(klass, concreteTypeCounter) }
    }
    val flattenedGroundProduct =
        flattenedChoiceProducts.values.fold(BigInteger.ZERO, BigInteger::add)
    val exactGroundCounts = rootCounts.mapValues { it.value.count }
    val exactGroundAtomCount = exactGroundCounts.values.fold(BigInteger.ZERO, BigInteger::add)
    val groundWitnesses = rootCounts.values.flatMap(RootCount::samples).distinct()
    val rootDenotationStart = System.nanoTime()
    val rootDenotationCounts = active.associateWith {
      concreteTypeCounter.countAllConcrete(it.baseType)
    }
    val rootDenotationMillis = (System.nanoTime() - rootDenotationStart) / 1_000_000
    check(rootDenotationCounts.getValue(table.componentClass) == exactGroundAtomCount)
    val productMismatches = rootCounts.filter { (klass, result) ->
      flattenedChoiceProducts.getValue(klass) != result.count
    }

    val expressionStats = collectPlainObservedTypes(active, table)
    val candidateTypes = buildSet {
      active.forEach { klass ->
        add(klass.baseType)
        add(klass.defaultType)
      }
      addAll(expressionStats.resolvedTypeOccurrences.keys)
      addAll(game.reader.getComponents("Component").elements)
    }
        .filter(::isPlain)
        .distinct()
        .sortedBy { it.expressionFull.toString() }
    val candidateDenotationCounts = candidateTypes.map(concreteTypeCounter::countAllConcrete)

    val witnessMasks = mutableListOf<LongArray>()
    val maskMillis = measureTimeMillis {
      candidateTypes.mapTo(witnessMasks) { candidate ->
        LongArray(wordsFor(groundWitnesses.size)).also { mask ->
          groundWitnesses.forEachIndexed { index, witness ->
            if (witness.isSubtypeOf(candidate)) {
              mask[index / Long.SIZE_BITS] =
                  mask[index / Long.SIZE_BITS] or (1L shl (index % Long.SIZE_BITS))
            }
          }
        }
      }
    }

    var structuralPairs = 0L
    var witnessInclusionPairs = 0L
    var witnessViolationPairs = 0L
    val witnessViolationExamples = mutableListOf<String>()
    val relationMillis = measureTimeMillis {
      candidateTypes.indices.forEach { narrowIndex ->
        candidateTypes.indices.forEach { wideIndex ->
          val structurallyNarrows =
              candidateTypes[narrowIndex].isSubtypeOf(candidateTypes[wideIndex])
          val witnessInclusion = witnessMasks[narrowIndex].subsetOf(witnessMasks[wideIndex])
          if (structurallyNarrows) structuralPairs++
          if (witnessInclusion) witnessInclusionPairs++
          if (structurallyNarrows && !witnessInclusion) {
            witnessViolationPairs++
            if (witnessViolationExamples.size < EXAMPLE_LIMIT) {
              witnessViolationExamples +=
                  "${candidateTypes[narrowIndex].expressionFull} <: ${candidateTypes[wideIndex].expressionFull}"
            }
          }
        }
      }
    }

    val distinctWitnessMasks = witnessMasks.map { it.toList() }.distinct().size
    val equivalentWitnessSignatures =
        candidateTypes.indices
            .groupBy { witnessMasks[it].toList() }
            .values
            .filter { it.size > 1 }
            .sortedByDescending(List<Int>::size)
    val witnessMaskCardinalities = witnessMasks.map { it.cardinality() }
    val totalWitnessBits = candidateTypes.size.toLong() * groundWitnesses.size
    val setWitnessBits = witnessMaskCardinalities.sumOf(Int::toLong)
    val currentComponents = game.reader.getComponents("Component")
    val currentComponentTypes = currentComponents.elements.toSet()

    return buildString {
      section("Premise")
      line("players", PLAYERS)
      line("requested options", selectedOptions.sorted().joinToString())
      line(
          "enabled options",
          currentComponentTypes
              .map { it.className }
              .filter { it in reportOptionClassNames }
              .sorted()
              .joinToString(),
      )
      line(
          "map policy",
          "one legal map: Tharsis; all gameplay expansion bundles/card packs enabled",
      )
      line(
          "selected colonies",
          game.reader.getComponents("ColonyTile").map { it.className }.sorted().joinToString(),
      )
      line("actors", game.reader.getComponents("Actor").elements.size)
      line("current component instances", currentComponents.size)
      line("current distinct component types", currentComponentTypes.size)

      section("Class universe")
      line("active classes", active.size)
      line("active abstract / concrete", "${abstract.size} / ${concrete.size}")
      line("authority-known classes", known.size)
      line("phantom classes", phantom.size)
      line("direct inheritance edges", directEdges)
      line("multiple-inheritance classes", multipleInheritance.size)
      line("maximum direct supertypes", active.maxOf { it.directSuperclasses.size })
      line("nominal intersection classes", intersectionTypes.size)
      line("empty abstract classes", emptyAbstract.size)
      line("inheritance depth", distribution(active.map(::depth)))
      line("ancestor count (including self)", distribution(ancestorCounts))
      line("descendant count (including self)", distribution(descendantCounts))
      line("concrete-descendant count", distribution(concreteDescendantCounts))
      line("reflexive subtype pairs", subtypePairs)
      line("subtype relation density", percent(subtypePairs, active.size.toLong() * active.size))
      line("equal concrete-extension class groups", equalConcreteExtensions.size)
      appendTop("largest class families", active, { it.allSubclasses().size })
      appendTop("most ancestors", active, { it.allSuperclasses().size })

      section("Class bitmap costs")
      val activeWords = wordsFor(active.size)
      val knownWords = wordsFor(known.size)
      line("classes requiring a superclass bit", bitBearingSuperclasses.size)
      line(
          "active / phantom superclass bits",
          "${bitBearingSuperclasses.count { !it.phantom }} / ${bitBearingSuperclasses.count(PetsClass::phantom)}",
      )
      line("compiled active mask words", distribution(activeCompiledMaskWords))
      line("compiled known mask words", distribution(knownCompiledMaskWords))
      line(
          "compiled known mask word payload",
          bytes(knownCompiledMaskWords.sumOf(Int::toLong) * Long.SIZE_BYTES),
      )
      line("compiled known set bits", knownCompiledSetBits)
      line(
          "compiled known bit density",
          percent(knownCompiledSetBits, known.size.toLong() * bitBearingSuperclasses.size),
      )
      line("active-class words per fixed bitmap", activeWords)
      line("active ancestor matrix", bytes(active.size.toLong() * activeWords * Long.SIZE_BYTES))
      line(
          "active full relation lower bound",
          bytes(bitsToBytes(active.size.toLong() * active.size)),
      )
      line("known-class words per fixed bitmap", knownWords)
      line("known full relation lower bound", bytes(bitsToBytes(known.size.toLong() * known.size)))

      section("Dependency structure")
      line("inherited dependency slots per class", distribution(dependencyCounts))
      line("newly declared dependency slots", distribution(declaredDependencyCounts))
      line("flattened dependency paths", distribution(dependencyPathCounts))
      line("maximum nested type depth", distribution(dependencyDepths))
      line("classes without dependencies", dependencyCounts.count { it == 0 })
      line("classes with inherited dependencies", dependencyCounts.count { it > 0 })
      line("classes with linked dependency groups", dependencyLinks.count { it.value.isNotEmpty() })
      line(
          "linked dependency groups",
          dependencyLinks.values.sumOf(List<Set<DependencyPath>>::size),
      )
      appendTop("widest dependency schemas", active, { it.dependencies.keys.size })
      appendTop("deepest dependency schemas", active, { typeDepth(it.baseType) })

      section("Structurally concrete type atoms")
      line("recursively counted concrete roots", rootCounts.size)
      line("recursive counts memoized", concreteTypeCounter.memoizedCount)
      line("exceptional streamed subproblems", concreteTypeCounter.streamedSubproblems)
      line("small roots stream-cross-checked", rootCounts.count { it.value.streamCrossChecked })
      line("flattened-product mismatches", productMismatches.size)
      line("exact calculated ground atoms", integer(exactGroundAtomCount))
      line("sum of flattened choice products", integer(flattenedGroundProduct))
      line("exact counting time", "${groundMillis} ms")
      line("all nominal-root counting time", "${rootDenotationMillis} ms")
      line("sample concrete types retained", groundWitnesses.size)
      line("witness nested depth", distribution(groundWitnesses.map(::typeDepth)))
      line(
          "witness expression length",
          distribution(groundWitnesses.map { it.expressionFull.toString().length }),
      )
      appendTopBig(
          "largest flattened choice products",
          concrete,
          flattenedChoiceProducts::getValue,
      )
      productMismatches.entries.take(EXAMPLE_LIMIT).forEach { (klass, result) ->
        appendLine(
            "  product mismatch: $klass exact=${result.count}, " +
                "product=${integer(flattenedChoiceProducts.getValue(klass))}"
        )
      }
      appendTopBig("largest exact root-type counts", concrete, exactGroundCounts::getValue)
      appendTopBig("largest nominal-root denotations", active, rootDenotationCounts::getValue)
      val exactGroundWords = wordsFor(exactGroundAtomCount)
      line("dense-mask words at exact count", integer(exactGroundWords))
      line(
          "dense-mask bytes at exact count",
          bytes(exactGroundWords * BigInteger.valueOf(Long.SIZE_BYTES.toLong())),
      )
      line("dense-mask bytes at flattened product", bytes(bitsToBytes(flattenedGroundProduct)))

      section("Observed plain structural types")
      line("type-expression occurrences", expressionStats.expressionOccurrences)
      line("distinct contextual expressions", expressionStats.distinctExpressions)
      line("expressions involving complements", expressionStats.complementExpressions)
      line("expressions involving refinements", expressionStats.refinedExpressions)
      line("distinct plain expressions resolved", expressionStats.resolvedTypeOccurrences.size)
      line("plain expression resolution failures", expressionStats.resolutionFailures.size)
      expressionStats.resolutionFailures.take(EXAMPLE_LIMIT).forEach {
        appendLine("  unresolved: $it")
      }
      line("candidate structural types", candidateTypes.size)
      line(
          "candidate abstract / concrete",
          "${candidateTypes.count(Type::abstract)} / ${candidateTypes.count { !it.abstract }}",
      )
      line("exact candidate denotation size", bigDistribution(candidateDenotationCounts))
      line(
          "exact empty candidate denotations",
          candidateDenotationCounts.count { it == BigInteger.ZERO },
      )
      line("candidate nested depth", distribution(candidateTypes.map(::typeDepth)))
      line(
          "candidate dependency slots",
          distribution(candidateTypes.map { it.dependencies.keys.size }),
      )
      line(
          "candidate expression length",
          distribution(candidateTypes.map { it.expressionFull.toString().length }),
      )
      line("distinct witness signatures", distinctWitnessMasks)
      line("duplicate witness-signature groups", equivalentWitnessSignatures.size)
      line("empty witness signatures", witnessMaskCardinalities.count { it == 0 })
      line("witness-mask cardinality", distribution(witnessMaskCardinalities))
      line("witness-mask bit density", percent(setWitnessBits, totalWitnessBits))
      line(
          "all candidate witness masks",
          bytes(candidateTypes.size.toLong() * wordsFor(groundWitnesses.size) * Long.SIZE_BYTES),
      )
      line("witness-mask construction time", "${maskMillis} ms")
      appendTop(
          "most frequent declaration types",
          expressionStats.resolvedTypeOccurrences.keys,
          expressionStats.resolvedTypeOccurrences::getValue,
      )
      appendEquivalentExamples(equivalentWitnessSignatures, candidateTypes)

      section("Subtype semantics over observed types")
      val candidatePairCount = candidateTypes.size.toLong() * candidateTypes.size
      line("ordered candidate pairs", candidatePairCount)
      line("structural subtype pairs", structuralPairs)
      line("witness-inclusion pairs", witnessInclusionPairs)
      line("structural pairs violating witnesses", witnessViolationPairs)
      line("structural relation density", percent(structuralPairs, candidatePairCount))
      line("candidate subtype matrix", bytes(bitsToBytes(candidatePairCount)))
      line("comparison time", "${relationMillis} ms")
      witnessViolationExamples.forEach { appendLine("  witness violation: $it") }
      line(
          "note",
          "witness inclusion is necessary but not sufficient for full denotational inclusion",
      )

      section("Combined encoding scale")
      val combinedLowerBound =
          exactGroundAtomCount + BigInteger.valueOf(candidateTypes.size.toLong())
      val combinedProduct =
          flattenedGroundProduct + BigInteger.valueOf(candidateTypes.size.toLong())
      line("ground plus observed types exact", integer(combinedLowerBound))
      line("ground plus observed flattened product", integer(combinedProduct))
      line(
          "combined subtype matrix at exact count",
          bytes(bitsToBytes(combinedLowerBound * combinedLowerBound)),
      )
      line(
          "combined matrix at flattened product",
          bytes(bitsToBytes(combinedProduct * combinedProduct)),
      )
      line(
          "candidate ground masks at exact count",
          bytes(
              bitsToBytes(exactGroundAtomCount) * BigInteger.valueOf(candidateTypes.size.toLong())
          ),
      )
      line(
          "candidate masks at flattened product",
          bytes(
              bitsToBytes(flattenedGroundProduct) * BigInteger.valueOf(candidateTypes.size.toLong())
          ),
      )
      line(
          "note",
          "candidate types are declaration-observed plus class base/default types, not every " +
              "combinatorially expressible abstract type",
      )
      line(
          "product caveat",
          "flattening dependency paths can over- or under-count linkage, subtype-specific " +
              "dependencies, covariance, defaults, and complements",
      )

      section("Exact concrete-type counts by nominal root")
      active.forEach { klass ->
        appendLine(
            "${klass.className.toString().padEnd(42)} ${integer(rootDenotationCounts.getValue(klass))}"
        )
      }
    }
        .trimEnd()
  }

  private val reportOptionClassNames =
      selectedOptions + cn("MultiplayerMode") + cn("WorldGovernmentOption")

  private data class ExpressionStats(
      val expressionOccurrences: Int,
      val distinctExpressions: Int,
      val complementExpressions: Int,
      val refinedExpressions: Int,
      val resolvedTypeOccurrences: Map<Type, Int>,
      val resolutionFailures: List<String>,
  )

  private data class RootCount(
      val count: BigInteger,
      val streamCrossChecked: Boolean,
      val samples: List<Type>,
  )

  private fun flattenedChoiceProduct(klass: PetsClass): BigInteger =
      klass.dependencies.flatten().values.fold(BigInteger.ONE) { product, boundClass ->
        val choices = boundClass.allSubclasses().count { !it.abstract }
        product * BigInteger.valueOf(choices.toLong())
      }

  private fun countRootTypes(
      klass: PetsClass,
      counter: ConcreteTypeCounter,
  ): RootCount {
    val count = counter.countSameClass(klass.baseType)
    val streamCrossChecked = count <= BigInteger.valueOf(STREAM_CHECK_CAP.toLong())
    val iterator = klass.concreteTypes().iterator()
    val samples = mutableListOf<Type>()
    var streamedCount = BigInteger.ZERO
    while (iterator.hasNext() && (streamCrossChecked || samples.size < GROUND_SAMPLES_PER_ROOT)) {
      val next = iterator.next()
      if (samples.size < GROUND_SAMPLES_PER_ROOT) samples += next
      streamedCount++
    }
    if (streamCrossChecked) {
      check(streamedCount == count) {
        "$klass calculated $count concrete types but streamed $streamedCount"
      }
    }
    return RootCount(count, streamCrossChecked, samples)
  }

  private class ConcreteTypeCounter(
      private val table: ClassTable,
      private val dependencyLinks: Map<PetsClass, List<Set<DependencyPath>>>,
  ) {
    private val allConcreteMemo = mutableMapOf<Type, BigInteger>()
    private val sameClassMemo = mutableMapOf<Type, BigInteger>()

    var streamedSubproblems: Int = 0
      private set

    val memoizedCount: Int
      get() = allConcreteMemo.size + sameClassMemo.size

    fun countSameClass(type: Type): BigInteger =
        sameClassMemo.getOrPut(type) {
          when {
            type.rootClass.abstract -> BigInteger.ZERO
            type.rootClass == table.classClass -> {
              val represented = table.getClass(type.expressionFull.arguments.single().className)
              BigInteger.valueOf(represented.allSubclasses().count { !it.abstract }.toLong())
            }
            !isPlain(type) || dependencyLinks.getValue(type.rootClass).isNotEmpty() -> {
              streamedSubproblems++
              type.concreteSubtypesSameClass().fold(BigInteger.ZERO) { count, _ ->
                count + BigInteger.ONE
              }
            }
            else ->
                type.typeDependencies.fold(BigInteger.ONE) { product, dependency ->
                  product * countAllConcrete(dependency.boundType)
                }
          }
        }

    fun countAllConcrete(type: Type): BigInteger =
        allConcreteMemo.getOrPut(type) {
          type.rootClass
              .allSubclasses()
              .asSequence()
              .filterNot(PetsClass::abstract)
              .mapNotNull { concreteClass -> type glb concreteClass.baseType }
              .fold(BigInteger.ZERO) { total, concreteType ->
                total + countSameClass(concreteType)
              }
        }
  }

  private fun dependencyLinks(
      classes: List<PetsClass>,
      table: ClassTable,
  ): Map<PetsClass, List<Set<DependencyPath>>> {
    val memo = mutableMapOf<PetsClass, List<Set<DependencyPath>>>()

    fun declaredLinks(klass: PetsClass): List<Set<DependencyPath>> {
      val occurrences = mutableMapOf<Pair<Expression, Key>, MutableSet<DependencyPath>>()
      val contextualizer = replaceThisExpressionsWith(klass.className.expression)

      fun collect(expression: Expression, prefix: List<Key>) {
        if (expression.arguments.isEmpty()) return
        val arguments = expression.arguments.map(contextualizer::transformExpression)
        val dependencies = table.getClass(expression.className).dependencies
        val matchedKeys = dependencies.matchPartial(arguments).keys.toList()
        arguments.zip(matchedKeys).forEach { (argument, key) ->
          val path = DependencyPath(prefix + key)
          if (argument.simple && argument.className != THIS) {
            occurrences.getOrPut(argument to key, ::mutableSetOf) += path
          }
          collect(argument, path.keyList)
        }
      }

      klass.declaration.dependencies.forEachIndexed { index, expression ->
        collect(expression, listOf(Key(klass.className, index)))
      }
      klass.declaration.supertypes.forEach { collect(it, emptyList()) }
      return occurrences.values.filter { it.size > 1 }.map(Set<DependencyPath>::toSet)
    }

    fun linksFor(klass: PetsClass): List<Set<DependencyPath>> =
        memo.getOrPut(klass) {
          val merged = mutableListOf<Set<DependencyPath>>()
          val incoming = klass.directSuperclasses.flatMap(::linksFor) + declaredLinks(klass)
          incoming.forEach { link ->
            val overlapping = merged.filter { prior -> prior.any(link::contains) }
            merged.removeAll(overlapping)
            merged += link + overlapping.flatten()
          }
          merged
        }

    classes.forEach(::linksFor)
    return memo
  }

  @Suppress("TooGenericExceptionCaught") // The report records every expression-resolution failure.
  private fun collectPlainObservedTypes(
      classes: List<PetsClass>,
      table: ClassTable,
  ): ExpressionStats {
    var occurrences = 0
    var complements = 0
    var refinements = 0
    val contextualExpressions = linkedSetOf<Expression>()
    val resolved = linkedMapOf<Type, Int>()
    val failures = mutableListOf<String>()

    classes.forEach { klass ->
      val contextualizer = replaceThisExpressionsWith(klass.className.expression)
      klass.declaration.allNodes.forEach { node ->
        node.descendantsOfType<Expression>().forEach { source ->
          occurrences++
          val expression = contextualizer.transformExpression(source)
          contextualExpressions += expression
          val nestedExpressions = expression.descendantsOfType<Expression>()
          val hasComplement = nestedExpressions.any(Expression::complement)
          val hasRefinement = nestedExpressions.any { it.refinement != null }
          if (hasComplement) complements++
          if (hasRefinement) refinements++
          if (!hasComplement && !hasRefinement) {
            try {
              val type = table.resolve(expression)
              resolved[type] = resolved.getOrDefault(type, 0) + 1
            } catch (e: Exception) {
              failures += "${klass.className}: $expression (${e.message})"
            }
          }
        }
      }
    }
    return ExpressionStats(
        occurrences,
        contextualExpressions.size,
        complements,
        refinements,
        resolved,
        failures,
    )
  }

  private fun isPlain(type: Type): Boolean =
      type.expressionFull.descendantsOfType<Expression>().none {
        it.complement || it.refinement != null
      }

  private fun typeDepth(type: Type, visiting: Set<Type> = emptySet()): Int {
    if (type in visiting) return 0
    val next = visiting + type
    return 1 + (type.typeDependencies.maxOfOrNull { typeDepth(it.boundType, next) } ?: 0)
  }

  private fun LongArray.cardinality(): Int = sumOf(java.lang.Long::bitCount)

  private fun LongArray.subsetOf(that: LongArray): Boolean {
    check(size == that.size)
    return indices.all { index -> this[index] and that[index].inv() == 0L }
  }

  private fun StringBuilder.section(title: String) {
    if (isNotEmpty()) appendLine()
    appendLine("== $title ==")
  }

  private fun StringBuilder.line(label: String, value: Any) {
    appendLine("${label.padEnd(42)} $value")
  }

  private fun <T> StringBuilder.appendTop(
      title: String,
      values: Collection<T>,
      metric: (T) -> Int,
  ) {
    appendLine("$title:")
    values
        .sortedWith(compareByDescending(metric).thenBy { it.toString() })
        .take(TOP_LIMIT)
        .forEach {
          appendLine("  ${metric(it).toString().padStart(8)}  $it")
        }
  }

  private fun <T> StringBuilder.appendTopBig(
      title: String,
      values: Collection<T>,
      metric: (T) -> BigInteger,
  ) {
    appendLine("$title:")
    values
        .sortedWith(compareByDescending(metric).thenBy { it.toString() })
        .take(TOP_LIMIT)
        .forEach {
          appendLine("  ${integer(metric(it)).padStart(20)}  $it")
        }
  }

  private fun StringBuilder.appendEquivalentExamples(
      groups: List<List<Int>>,
      candidates: List<Type>,
  ) {
    groups.take(EXAMPLE_LIMIT).forEach { group ->
      appendLine(
          "  same denotation (${group.size}): " +
              group.take(EXAMPLE_MEMBER_LIMIT).joinToString { "${candidates[it].expressionFull}" }
      )
    }
  }

  private fun distribution(values: Collection<Int>): String {
    if (values.isEmpty()) return "n=0"
    val sorted = values.sorted()
    return "n=${values.size}, min=${sorted.first()}, p50=${percentile(sorted, 0.50)}, " +
        "p90=${percentile(sorted, 0.90)}, p99=${percentile(sorted, 0.99)}, " +
        "max=${sorted.last()}, mean=${format(values.average())}"
  }

  private fun bigDistribution(values: Collection<BigInteger>): String {
    if (values.isEmpty()) return "n=0"
    val sorted = values.sorted()
    return "n=${values.size}, min=${integer(sorted.first())}, " +
        "p50=${integer(bigPercentile(sorted, 0.50))}, " +
        "p90=${integer(bigPercentile(sorted, 0.90))}, " +
        "p99=${integer(bigPercentile(sorted, 0.99))}, max=${integer(sorted.last())}"
  }

  private fun percentile(sorted: List<Int>, proportion: Double): Int =
      sorted[max(0, ceil(sorted.size * proportion).toInt() - 1)]

  private fun bigPercentile(sorted: List<BigInteger>, proportion: Double): BigInteger =
      sorted[max(0, ceil(sorted.size * proportion).toInt() - 1)]

  private fun percent(numerator: Long, denominator: Long): String =
      if (denominator == 0L) "n/a" else "${format(numerator * 100.0 / denominator)}%"

  private fun format(value: Double): String = String.format(Locale.ROOT, "%.3f", value)

  private fun wordsFor(bits: Int): Int = (bits + Long.SIZE_BITS - 1) / Long.SIZE_BITS

  private fun wordsFor(bits: BigInteger): BigInteger =
      (bits + BigInteger.valueOf(Long.SIZE_BITS - 1L)) / BigInteger.valueOf(Long.SIZE_BITS.toLong())

  private fun bitsToBytes(bits: Long): Long = (bits + Byte.SIZE_BITS - 1) / Byte.SIZE_BITS

  private fun bitsToBytes(bits: BigInteger): BigInteger =
      (bits + BigInteger.valueOf(Byte.SIZE_BITS - 1L)) / BigInteger.valueOf(Byte.SIZE_BITS.toLong())

  private fun integer(value: BigInteger): String = String.format(Locale.ROOT, "%,d", value)

  private fun bytes(value: Long): String {
    if (value < 1024) return "$value B"
    val kib = value / 1024.0
    if (kib < 1024) return "${format(kib)} KiB ($value B)"
    val mib = kib / 1024.0
    if (mib < 1024) return "${format(mib)} MiB ($value B)"
    return "${format(mib / 1024.0)} GiB ($value B)"
  }

  private fun bytes(value: BigInteger): String =
      if (value.bitLength() < Long.SIZE_BITS - 1) {
        bytes(value.toLong())
      } else {
        "${integer(value)} B (~2^${value.bitLength() - 1})"
      }

  private const val PLAYERS = 5
  private const val STREAM_CHECK_CAP = 4_096
  private const val GROUND_SAMPLES_PER_ROOT = 4
  private const val TOP_LIMIT = 12
  private const val EXAMPLE_LIMIT = 8
  private const val EXAMPLE_MEMBER_LIMIT = 8
}

public fun main(): Unit = println(TypeStructureReport.render(TypeStructureReport.createGame()))
