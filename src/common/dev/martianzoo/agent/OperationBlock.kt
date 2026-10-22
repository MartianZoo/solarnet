package dev.martianzoo.agent

/**
 * Client commands grouped inside an Agent call.
 *
 * TODO(ENGINE_CONTRACT): investigate removing the operation API that consumes this block. The
 *   operation may continue across calls and must not be treated as an engine transaction.
 */
public typealias OperationBlock = Agent.OperationScope.() -> Unit
