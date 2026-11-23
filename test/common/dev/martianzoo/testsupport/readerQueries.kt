package dev.martianzoo.testsupport

import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.GameReader

/** String query notation for scenarios reading the World without a Player context. */
internal fun GameReader.count(text: String): Int =
    count(
        PetElaborator(classTable).elaborateMetricInput(Parsing.parse(text), ADMIN.expression, null)
    )

internal fun GameReader.has(text: String): Boolean =
    has(
        PetElaborator(classTable).elaborateInput(Parsing.parse<Requirement>(text), null)
            as Requirement
    )

internal fun GameReader.resolve(text: String) =
    resolve(
        PetElaborator(classTable).elaborateInput(Parsing.parse<Expression>(text), null)
            as Expression
    )

internal fun GameReader.list(text: String) = getComponents(resolve(text)).map { it.expression }
