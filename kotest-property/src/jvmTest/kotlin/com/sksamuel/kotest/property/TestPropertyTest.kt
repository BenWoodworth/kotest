package com.sksamuel.kotest.property

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.PropertyTesting
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.exhaustive.exhaustive
import io.kotest.property.exhaustive.ints
import io.kotest.property.testProperty

// Tests for `testProperty()` without any configuration
class TestPropertyTest : FunSpec({
   @Suppress("UNUSED_PARAMETER")
   fun readVariable(variable: Any?) = Unit

   context("test iterations") {
      val default = PropertyTesting.defaultIterationCount

      suspend fun countIterations(
         arbitrary: Boolean = false,
         exhaustive: Int? = null,
      ): Int {
         var iterations = 0

         testProperty {
            iterations++

            if (arbitrary) {
               val arbVariable by Arb.int()
               readVariable(arbVariable)
            }

            if (exhaustive != null) {
               val exhaustiveVariable by Exhaustive.ints(1..exhaustive)
               readVariable(exhaustiveVariable)
            }
         }

         return iterations
      }

      test("with no generator variables, should use the default") {
         countIterations() shouldBe default
      }

      test("with only arbitrary variables, should use the default") {
         countIterations(arbitrary = true) shouldBe default
      }

      test("with only exhaustive variables, should use the number of permutations") {
         val lessPermutations = default / 2
         val morePermutations = default * 2

         // permutations < default
         countIterations(exhaustive = lessPermutations) shouldBe lessPermutations

         // default < permutations
         countIterations(exhaustive = morePermutations) shouldBe morePermutations
      }

      context("with both arbitrary and exhaustive variables") {
         test("and exhaustive permutations < default, should use the default") {
            val permutations = default / 2

            // permutations < default
            countIterations(arbitrary = true, exhaustive = permutations) shouldBe default
         }

         test("and exhaustive permutations > default, should use the number of permutations") {
            val permutations = default * 2

            // default < permutations
            countIterations(arbitrary = true, exhaustive = permutations) shouldBe permutations
         }
      }
   }

   context("arbitrary variables") {
      test("should return the value sampled from the generator") {
         val failures = mutableListOf<String>()

         var sampleValue: Int? = null

         testProperty {
            val variable by Arb.int()
               .map { sample ->
                  sampleValue = sample
                  sample
               }

            if (variable != sampleValue) {
               failures += "Expected variable to return the generator sample, $sampleValue, but got $variable"
            }
         }

         failures.shouldBeEmpty()
      }

      test("should only sample from the first generator") {
         var iterations = 0
         val failures = mutableListOf<String>()

         var firstSample: String? = null

         testProperty {
            iterations++

            val iterationStringSample = "Iteration #$iterations"
            val variable by arbitrary { iterationStringSample }

            val sample = variable

            if (firstSample == null) {
               firstSample = sample
            }

            if (sample != firstSample) {
               failures += "Iteration $iterations: expected sample from $firstSample's generator, but was from $sample"
            }
         }

         failures.shouldBeEmpty()
      }

      test("when accessed once, should sample once") {
         var iterations = 0
         val failures = mutableListOf<String>()

         var samples: Int

         testProperty {
            iterations++
            samples = 0

            val variable by arbitrary { samples++ }

            readVariable(variable)

            if (samples != 1) {
               failures += "Iteration $iterations: expected 1 sample, but had $samples"
            }
         }

         failures.shouldBeEmpty()
      }

      test("when accessed 0 times, should not be sampled") {
         var iterations = 0
         val failures = mutableListOf<String>()

         var samples: Int

         testProperty {
            iterations++
            samples = 0

            val variable by arbitrary { samples++ }

            var accessed = false
            if (iterations % 6 == 5) {
               readVariable(variable)
               accessed = true
            }

            if (!accessed && samples != 0) {
               failures += "Iteration $iterations: expected 0 samples, but had $samples"
            }
         }

         failures.shouldBeEmpty()
      }

      test("when accessed multiple times, should sample once") {
         var iterations = 0
         val failures = mutableListOf<String>()

         var samples: Int

         testProperty {
            iterations++
            samples = 0

            val variable by arbitrary { samples++ }

            repeat(4) {
               readVariable(variable)
            }

            if (samples != 1) {
               failures += "Iteration $iterations: expected 1 sample, but had $samples"
            }
         }

         failures.shouldBeEmpty()
      }

      test("when declared and accessed within a loop, should sample once") {
         var iterations = 0
         val failures = mutableListOf<String>()

         var samples: Int

         testProperty {
            iterations++
            samples = 0

            repeat(4) {
               val variable by arbitrary { samples++ }
               readVariable(variable)
            }

            if (samples != 1) {
               failures += "Iteration $iterations: expected 1 sample, but had $samples"
            }
         }

         failures.shouldBeEmpty()
      }

      test("sampling with multiple variables") {
         var iterations = 0
         val failures = mutableListOf<String>()

         var samplesA: Int
         var samplesB: Int

         testProperty {
            iterations++
            samplesA = 0
            samplesB = 0

            val variableA by arbitrary { samplesA++ }
            val variableB by arbitrary { samplesB++ }

            var accessedA = false
            if (iterations % 7 == 5) {
               readVariable(variableA)
               accessedA = true
            }

            var accessedB = false
            if (iterations % 11 == 9) {
               readVariable(variableB)
               accessedB = true
            }

            val expectedSamplesA = if (accessedA) 1 else 0
            if (samplesA != expectedSamplesA) {
               failures += "Iteration $iterations, variableA: expected $expectedSamplesA samples, but had $samplesA"
            }

            val expectedSamplesB = if (accessedB) 1 else 0
            if (samplesB != expectedSamplesB) {
               failures += "Iteration $iterations, variableB: expected $expectedSamplesB samples, but had $samplesB"
            }
         }

         failures.shouldBeEmpty()
      }
   }

   context("exhaustive variables") {
      test("should sample every value") {
         val integersLessThan100 = 0..99

         val sampledValues = mutableSetOf<Int>()

         testProperty {
            val variable by exhaustive(integersLessThan100.toList())

            sampledValues += variable
         }

         sampledValues shouldContainOnly integersLessThan100
      }

      test("should sample every permutation of multiple values") {
         val numbers = "0123456789".toList()
         val letters = "ABCDEF".toList()
         val symbols = "!@#$%".toList()

         val sampledValues = mutableSetOf<String>()

         testProperty {
            val number by exhaustive(numbers)
            val letter by exhaustive(letters)
            val symbol by exhaustive(symbols)

            sampledValues += "$number$letter$symbol"
         }

         val everyPermutation = buildSet {
            for (number in numbers) {
               for (letter in letters) {
                  for (symbol in symbols) {
                     add("$number$letter$symbol")
                  }
               }
            }
         }

         sampledValues shouldContainOnly everyPermutation
      }

      test("should only sample from the originally provided values") {
         class ChangingExhaustive<A>(
            override val values: MutableList<A>
         ) : Exhaustive<A>()

         val originalValues = listOf("a", "b", "c")

         val changingExhaustive = ChangingExhaustive(originalValues.toMutableList())
         var valuesChanged = false

         val sampledValues = mutableSetOf<String>()

         testProperty {
            val value by changingExhaustive

            sampledValues += value

            if (!valuesChanged) {
               changingExhaustive.values.removeLast()
               changingExhaustive.values.add("<Swapped Value>")
               valuesChanged = true
            }
         }

         sampledValues shouldContainOnly originalValues
      }
   }
})
