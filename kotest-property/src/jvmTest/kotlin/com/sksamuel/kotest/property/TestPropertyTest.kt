package com.sksamuel.kotest.property

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropertyTesting
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.testProperty

// Tests for `testProperty()` without any configuration
class TestPropertyTest : FunSpec({
   @Suppress("UNUSED_PARAMETER")
   fun readVariable(variable: Any?) = Unit

   context("test iterations") {
      val default = PropertyTesting.defaultIterationCount

      suspend fun countIterations(
         arbitrary: Boolean = false,
      ): Int {
         var iterations = 0

         testProperty {
            iterations++

            if (arbitrary) {
               val arbVariable by Arb.int()
               readVariable(arbVariable)
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
})
