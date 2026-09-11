package dev.martianzoo.agent

/** Instructions issued within one coordinated Agent operation. */
public typealias OperationBlock = Agent.OperationScope.() -> Unit
