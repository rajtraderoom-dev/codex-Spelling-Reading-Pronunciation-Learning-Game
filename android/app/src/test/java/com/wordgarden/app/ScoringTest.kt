package com.wordgarden.app

import org.junit.Assert.*
import org.junit.Test

class ScoringTest {
    @Test fun ignoresCaseAndPunctuation() { assertTrue(Scoring.matches("Garden", "garden!")); assertFalse(Scoring.matches("cat", "")) }
    @Test fun acceptsSmallTranscriptErrorsForLongerWords() { assertTrue(Scoring.matches("garden","gardin")); assertTrue(Scoring.matches("butterfly","buterfly")) }
    @Test fun protectsMinimalPairs() { for((a,b) in listOf("ship" to "sheep","bit" to "beat","vet" to "wet","pat" to "bat","ten" to "den","sip" to "ship","fan" to "van")) assertFalse("$a must not match $b",Scoring.matches(a,b)) }
    @Test fun acceptsRecognizerHomophonesWithoutCollapsingDifferentVowels() { assertTrue(Scoring.matches("see","sea")); assertTrue(Scoring.matches("right","write")); assertFalse(Scoring.matches("bed","bead")); assertFalse(Scoring.matches("full","fool")) }
    @Test fun alignsMissingWordsWithoutShiftingTheRest() { assertEquals(listOf(true,false,true,true,true),Scoring.align("The cat is on grass","the is on grass")) }
    @Test fun alignsRepeatedAndExtraWords() { assertEquals(listOf(true,true,true,true),Scoring.align("I see the tree","I really see the tree")); assertEquals(listOf(true,true,true),Scoring.align("the little cat","the little cat")) }
    @Test fun emptyTranscriptDoesNotMatchSentence() { assertEquals(listOf(false,false,false),Scoring.align("I can hop", "")) }
    @Test fun voiceSpellingUnderstandsLetterNames() { assertEquals("cat",Scoring.letters("see ay tee")); assertEquals("wow",Scoring.letters("double you oh double u")); assertEquals("bee",Scoring.letters("bee ee ee")) }
    @Test fun editDistanceIsSymmetric() { assertEquals(1,Scoring.distance("garden","gardin")); assertEquals(3,Scoring.distance("cat","")); assertEquals(Scoring.distance("pig","big"),Scoring.distance("big","pig")) }
}
