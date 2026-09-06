package com.cyclemonitor.core.ride

/** The single source of truth for what an in-progress (or just-finished) ride is doing. */
sealed class RideState {
    data object Idle : RideState()
    data object Starting : RideState()
    data object Riding : RideState()
    data object Paused : RideState()
    data object Finishing : RideState()
    data object Completed : RideState()
    data class Error(val reason: String) : RideState()

    /** Stable label independent of the sealed subtype's data, useful for logging/UI. */
    val label: String
        get() = when (this) {
            Idle -> "IDLE"
            Starting -> "STARTING"
            Riding -> "RIDING"
            Paused -> "PAUSED"
            Finishing -> "FINISHING"
            Completed -> "COMPLETED"
            is Error -> "ERROR"
        }
}

/**
 * Centralizes every legal ride-state transition so it is never scattered across UI components.
 * Any transition not explicitly allowed here is rejected.
 */
object RideStateMachine {

    private val allowedTransitions: Map<String, Set<String>> = mapOf(
        "IDLE" to setOf("STARTING", "ERROR"),
        "STARTING" to setOf("RIDING", "ERROR", "IDLE"),
        "RIDING" to setOf("PAUSED", "FINISHING", "ERROR"),
        "PAUSED" to setOf("RIDING", "FINISHING", "ERROR"),
        "FINISHING" to setOf("COMPLETED", "ERROR"),
        "COMPLETED" to setOf("IDLE"),
        "ERROR" to setOf("IDLE"),
    )

    fun canTransition(from: RideState, to: RideState): Boolean {
        val allowedTargets = allowedTransitions[from.label] ?: return false
        return to.label in allowedTargets
    }

    /** Returns the new state if the transition is legal, otherwise throws. Callers own the state field. */
    fun transition(from: RideState, to: RideState): RideState {
        check(canTransition(from, to)) { "Illegal ride state transition: ${from.label} -> ${to.label}" }
        return to
    }
}
