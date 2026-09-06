package com.cyclemonitor.core.ride

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RideStateMachineTest {

    @Test
    fun `idle can start`() {
        assertThat(RideStateMachine.canTransition(RideState.Idle, RideState.Starting)).isTrue()
    }

    @Test
    fun `starting can become riding`() {
        assertThat(RideStateMachine.canTransition(RideState.Starting, RideState.Riding)).isTrue()
    }

    @Test
    fun `riding can pause`() {
        assertThat(RideStateMachine.canTransition(RideState.Riding, RideState.Paused)).isTrue()
    }

    @Test
    fun `paused can resume to riding`() {
        assertThat(RideStateMachine.canTransition(RideState.Paused, RideState.Riding)).isTrue()
    }

    @Test
    fun `riding can finish`() {
        assertThat(RideStateMachine.canTransition(RideState.Riding, RideState.Finishing)).isTrue()
    }

    @Test
    fun `paused can finish directly`() {
        assertThat(RideStateMachine.canTransition(RideState.Paused, RideState.Finishing)).isTrue()
    }

    @Test
    fun `finishing becomes completed`() {
        assertThat(RideStateMachine.canTransition(RideState.Finishing, RideState.Completed)).isTrue()
    }

    @Test
    fun `completed can only go back to idle`() {
        assertThat(RideStateMachine.canTransition(RideState.Completed, RideState.Idle)).isTrue()
        assertThat(RideStateMachine.canTransition(RideState.Completed, RideState.Riding)).isFalse()
    }

    @Test
    fun `idle cannot jump straight to riding`() {
        assertThat(RideStateMachine.canTransition(RideState.Idle, RideState.Riding)).isFalse()
    }

    @Test
    fun `idle cannot pause`() {
        assertThat(RideStateMachine.canTransition(RideState.Idle, RideState.Paused)).isFalse()
    }

    @Test
    fun `completed cannot pause`() {
        assertThat(RideStateMachine.canTransition(RideState.Completed, RideState.Paused)).isFalse()
    }

    @Test
    fun `any active state can transition to error`() {
        assertThat(RideStateMachine.canTransition(RideState.Riding, RideState.Error("gps lost"))).isTrue()
        assertThat(RideStateMachine.canTransition(RideState.Starting, RideState.Error("permission denied"))).isTrue()
        assertThat(RideStateMachine.canTransition(RideState.Paused, RideState.Error("storage failure"))).isTrue()
    }

    @Test
    fun `error can recover to idle`() {
        assertThat(RideStateMachine.canTransition(RideState.Error("gps lost"), RideState.Idle)).isTrue()
    }

    @Test
    fun `transition throws on illegal move`() {
        try {
            RideStateMachine.transition(RideState.Idle, RideState.Riding)
            throw AssertionError("expected IllegalStateException")
        } catch (expected: IllegalStateException) {
            // expected
        }
    }

    @Test
    fun `transition returns target state on legal move`() {
        val result = RideStateMachine.transition(RideState.Idle, RideState.Starting)
        assertThat(result).isEqualTo(RideState.Starting)
    }
}
