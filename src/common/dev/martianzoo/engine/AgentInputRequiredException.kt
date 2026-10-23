package dev.martianzoo.engine

/**
 * Signals that an Agent call needs information from its client before it can finish. For example, a
 * card movement may need externally supplied card identities. This is not a game-rule rejection or
 * a promise that pending Admin work may be left for a Player to resolve.
 *
 * Existing Agent handling may retain the task for more input. How that interacts with mandatory
 * Admin handling remains subject to the proposed client contract and human review.
 *
 * @constructor Creates a missing-input signal with the caller-facing [message]. Subclasses may
 *   carry the specific information the client must supply; constructing it changes no game state.
 *
 * TODO(ENGINE_CONTRACT): Investigate removing this client-input exception from the engine API along
 *   with its application guard. The engine should enforce game rules without requesting application
 *   data.
 */
public open class AgentInputRequiredException(message: String) : RuntimeException(message)
