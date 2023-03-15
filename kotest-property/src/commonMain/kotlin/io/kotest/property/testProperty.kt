package io.kotest.property

import io.kotest.property.arbitrary.lazy
import io.kotest.property.exhaustive.lazy
import kotlin.reflect.KProperty

/**
 * Run a property test with generated values conveniently provided inline by delegating variables.
 *
 * ```
 * // Test String concatenation
 * testProperty {
 *     // Generator variables
 *     val first by Arb.string()
 *     val second by Arb.string()
 *
 *     val concatenated = concat(first, second)
 *
 *     concatenated shouldStartWith first
 *     concatenated shouldEndWith second
 *     concatenated shouldHaveLength (first.length + second.length)
 * }
 * ```
 *
 * Each generator variable will behave like a hardcoded value, having the same value throughout the entire test
 * iteration. If declared within a loop, for example, the variable will have the same value every loop.
 *
 * With multiple [Exhaustive] variables, every combination of possible values will be enumerated throughout the
 * test iterations.
 *
 * The very first [Gen] provided for a variable will be the only one used throughout the rest of the test, with all
 * others discarded. Use [`Arb.lazy {...}`][Arb.Companion.lazy] or [`Exhaustive.lazy {...}`][Exhaustive.Companion.lazy]
 * for a generator if redundantly instantiating it each test iteration becomes a performance concern.
 */
suspend fun testProperty(
   config: PropTestConfig = PropTestConfig(),
   property: suspend PropertyTestScope.() -> Unit
) {
   TODO()
}

class PropertyTestScope {
   operator fun <A> Gen<A>.getValue(thisRef: Nothing?, variable: KProperty<*>): A {
      TODO()
   }
}
