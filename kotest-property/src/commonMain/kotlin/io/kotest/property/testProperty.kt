package io.kotest.property

import io.kotest.property.arbitrary.lazy
import io.kotest.property.exhaustive.lazy
import io.kotest.property.seed.createRandom
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
   val scope = TestingPropertyTestScope(createRandom(config), config.edgeConfig)

   fun getIterations(): Int {
      val hasOnlyExhaustive = scope.hasExhaustiveVariables && !scope.hasArbVariables
      val defaultIterations = if (hasOnlyExhaustive) 0 else PropertyTesting.defaultIterationCount

      return maxOf(scope.minIterations, defaultIterations)
   }

   var iteration = 0
   while (iteration++ < getIterations()) {
      scope.startTestIteration()
      scope.property()
   }
}

sealed interface PropertyTestScope {
   operator fun <A> Gen<A>.getValue(thisRef: Nothing?, variable: KProperty<*>): A
}

private class TestingPropertyTestScope(
   private val randomSource: RandomSource,
   private val edgeConfig: EdgeConfig
) : PropertyTestScope {
   private var iteration = 0

   private val variableGenerators = hashMapOf<KProperty<*>, Gen<*>>()
   private val arbVariables = ArbVariableContainer()
   private val exhaustiveVariables = ExhaustiveVariableContainer()

   val minIterations: Int
      get() = exhaustiveVariables.permutations

   val hasArbVariables: Boolean
      get() = arbVariables.count > 0

   val hasExhaustiveVariables: Boolean
      get() = exhaustiveVariables.count > 0

   override operator fun <A> Gen<A>.getValue(thisRef: Nothing?, variable: KProperty<*>): A {
      @Suppress("UNCHECKED_CAST")
      val generator = variableGenerators
         .getOrPut(variable) { this } as Gen<A>

      return when (generator) {
         is Arb -> arbVariables.getValue(variable, generator)
         is Exhaustive -> exhaustiveVariables.getValue(variable, generator, iteration)
      }
   }

   fun startTestIteration() {
      arbVariables.clearValues()
      iteration++
   }

   private inner class ArbVariableContainer {
      private val samples = hashMapOf<KProperty<*>, Iterator<Sample<*>>>()
      private val values = hashMapOf<KProperty<*>, Sample<*>>()

      val count: Int
         get() = samples.size

      fun clearValues() {
         values.clear()
      }

      fun <A> getValue(variable: KProperty<*>, arbitrary: Arb<A>): A {
         val samples = samples.getOrPut(variable) {
            arbitrary.generate(randomSource, edgeConfig).iterator()
         }

         @Suppress("UNCHECKED_CAST")
         return values
            .getOrPut(variable) { samples.next() }
            .value as A
      }
   }

   private class ExhaustiveVariableContainer {
      var permutations: Int = 1
         private set

      class State<A>(val values: List<A>, val samplePeriod: Int)

      private val variableStates = hashMapOf<KProperty<*>, State<*>>()

      val count: Int
         get() = variableStates.size

      fun <A> getValue(variable: KProperty<*>, exhaustive: Exhaustive<A>, iteration: Int): A {
         val state = variableStates.getOrPut(variable) {
            val values = exhaustive.values.toList()
            val samplePeriod = permutations
            permutations *= values.size

            State(values, samplePeriod)
         }

         val valueIndex = iteration / state.samplePeriod % state.values.size

         @Suppress("UNCHECKED_CAST")
         return state.values[valueIndex] as A
      }
   }
}
