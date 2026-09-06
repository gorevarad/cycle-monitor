// Intentionally empty: each module applies exactly the plugins it needs (see core/build.gradle.kts
// and app/build.gradle.kts). Declaring Android/Kotlin plugins here with `apply false` would force
// Gradle to resolve the Android Gradle Plugin while configuring the root project even when only
// running a `:core` task -- keeping this file empty lets `:core:test` run on a plain JVM/Kotlin
// toolchain with no Android SDK present (see gradle.properties' configureondemand note).
