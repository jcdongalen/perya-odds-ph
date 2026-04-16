package com.jcdongalen.peryaodds.shared.domain.engines

import com.jcdongalen.peryaodds.shared.domain.models.*
import kotlin.test.*

class BiasDetectionEngineTest {
    
    @Test
    fun `detectBias classifies Hot when observed exceeds expected`() {
        val probResult = ProbabilityResult(
            perOutcome = mapOf(
                "Ace" to OutcomeProbability(observed = 0.25, expected = 0.1667)
            ),
            confidenceLevel = ConfidenceLevel.Medium,
            totalRounds = 100
        )
        
        val biasResult = BiasDetectionEngine.detectBias(probResult)
        
        val aceBias = biasResult.perOutcome["Ace"]
        assertNotNull(aceBias)
        assertTrue(aceBias.deviation > 0.0)
        assertEquals(BiasClassification.Hot, aceBias.classification)
    }
    
    @Test
    fun `detectBias classifies Cold when observed is below expected`() {
        val probResult = ProbabilityResult(
            perOutcome = mapOf(
                "King" to OutcomeProbability(observed = 0.10, expected = 0.1667)
            ),
            confidenceLevel = ConfidenceLevel.Medium,
            totalRounds = 100
        )
        
        val biasResult = BiasDetectionEngine.detectBias(probResult)
        
        val kingBias = biasResult.perOutcome["King"]
        assertNotNull(kingBias)
        assertTrue(kingBias.deviation < 0.0)
        assertEquals(BiasClassification.Cold, kingBias.classification)
    }
    
    @Test
    fun `detectBias classifies Neutral when observed equals expected`() {
        val probResult = ProbabilityResult(
            perOutcome = mapOf(
                "Queen" to OutcomeProbability(observed = 0.1667, expected = 0.1667)
            ),
            confidenceLevel = ConfidenceLevel.Medium,
            totalRounds = 100
        )
        
        val biasResult = BiasDetectionEngine.detectBias(probResult)
        
        val queenBias = biasResult.perOutcome["Queen"]
        assertNotNull(queenBias)
        assertEquals(0.0, queenBias.deviation, 0.0001)
        assertEquals(BiasClassification.Neutral, queenBias.classification)
    }
    
    @Test
    fun `detectBias computes correct deviation formula`() {
        val observed = 0.20
        val expected = 0.1667
        val expectedDeviation = (observed - expected) / expected
        
        val probResult = ProbabilityResult(
            perOutcome = mapOf(
                "Jack" to OutcomeProbability(observed = observed, expected = expected)
            ),
            confidenceLevel = ConfidenceLevel.High,
            totalRounds = 200
        )
        
        val biasResult = BiasDetectionEngine.detectBias(probResult)
        
        val jackBias = biasResult.perOutcome["Jack"]
        assertNotNull(jackBias)
        assertEquals(expectedDeviation, jackBias.deviation, 0.0001)
    }
    
    @Test
    fun `detectBias handles multiple outcomes correctly`() {
        val probResult = ProbabilityResult(
            perOutcome = mapOf(
                "Ace" to OutcomeProbability(observed = 0.25, expected = 0.1667),   // Hot
                "King" to OutcomeProbability(observed = 0.10, expected = 0.1667),  // Cold
                "Queen" to OutcomeProbability(observed = 0.1667, expected = 0.1667), // Neutral
                "Jack" to OutcomeProbability(observed = 0.20, expected = 0.1667),  // Hot
                "10" to OutcomeProbability(observed = 0.15, expected = 0.1667),    // Cold
                "9" to OutcomeProbability(observed = 0.1333, expected = 0.1667)    // Cold
            ),
            confidenceLevel = ConfidenceLevel.VeryHigh,
            totalRounds = 500
        )
        
        val biasResult = BiasDetectionEngine.detectBias(probResult)
        
        assertEquals(6, biasResult.perOutcome.size)
        assertEquals(BiasClassification.Hot, biasResult.perOutcome["Ace"]?.classification)
        assertEquals(BiasClassification.Cold, biasResult.perOutcome["King"]?.classification)
        assertEquals(BiasClassification.Neutral, biasResult.perOutcome["Queen"]?.classification)
        assertEquals(BiasClassification.Hot, biasResult.perOutcome["Jack"]?.classification)
        assertEquals(BiasClassification.Cold, biasResult.perOutcome["10"]?.classification)
        assertEquals(BiasClassification.Cold, biasResult.perOutcome["9"]?.classification)
    }
    
    @Test
    fun `detectBias handles zero expected probability safely`() {
        val probResult = ProbabilityResult(
            perOutcome = mapOf(
                "Ace" to OutcomeProbability(observed = 0.0, expected = 0.0)
            ),
            confidenceLevel = ConfidenceLevel.Low,
            totalRounds = 0
        )
        
        val biasResult = BiasDetectionEngine.detectBias(probResult)
        
        val aceBias = biasResult.perOutcome["Ace"]
        assertNotNull(aceBias)
        assertEquals(0.0, aceBias.deviation)
        assertEquals(BiasClassification.Neutral, aceBias.classification)
    }
    
    @Test
    fun `detectBias with extreme positive deviation`() {
        val probResult = ProbabilityResult(
            perOutcome = mapOf(
                "Ace" to OutcomeProbability(observed = 0.50, expected = 0.1667)
            ),
            confidenceLevel = ConfidenceLevel.VeryHigh,
            totalRounds = 1000
        )
        
        val biasResult = BiasDetectionEngine.detectBias(probResult)
        
        val aceBias = biasResult.perOutcome["Ace"]
        assertNotNull(aceBias)
        assertTrue(aceBias.deviation > 1.0) // Deviation should be > 100%
        assertEquals(BiasClassification.Hot, aceBias.classification)
    }
    
    @Test
    fun `detectBias with extreme negative deviation`() {
        val probResult = ProbabilityResult(
            perOutcome = mapOf(
                "King" to OutcomeProbability(observed = 0.01, expected = 0.1667)
            ),
            confidenceLevel = ConfidenceLevel.VeryHigh,
            totalRounds = 1000
        )
        
        val biasResult = BiasDetectionEngine.detectBias(probResult)
        
        val kingBias = biasResult.perOutcome["King"]
        assertNotNull(kingBias)
        assertTrue(kingBias.deviation < -0.9) // Deviation should be very negative
        assertEquals(BiasClassification.Cold, kingBias.classification)
    }
}
