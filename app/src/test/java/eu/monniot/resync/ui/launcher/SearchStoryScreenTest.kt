package eu.monniot.resync.ui.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchStoryScreenTest {

    @Test
    fun isValidNumericId_digitsOnly_isValid() {
        assertTrue(isValidNumericId("39200706"))
    }

    @Test
    fun isValidNumericId_empty_isInvalid() {
        assertFalse(isValidNumericId(""))
    }

    @Test
    fun isValidNumericId_nonDigits_isInvalid() {
        assertFalse(isValidNumericId("abc"))
    }

    @Test
    fun isValidNumericId_mixedDigitsAndLetters_isInvalid() {
        assertFalse(isValidNumericId("123abc"))
    }

    @Test
    fun isValidNumericId_whitespace_isInvalid() {
        assertFalse(isValidNumericId(" 123"))
    }

    @Test
    fun isValidOptionalNumericId_blank_isValid() {
        assertTrue(isValidOptionalNumericId(""))
    }

    @Test
    fun isValidOptionalNumericId_digitsOnly_isValid() {
        assertTrue(isValidOptionalNumericId("42"))
    }

    @Test
    fun isValidOptionalNumericId_nonDigits_isInvalid() {
        assertFalse(isValidOptionalNumericId("abc"))
    }

    @Test
    fun canSyncStory_validStoryIdAndBlankChapterId_isTrue() {
        assertTrue(canSyncStory("39200706", ""))
    }

    @Test
    fun canSyncStory_validStoryIdAndValidChapterId_isTrue() {
        assertTrue(canSyncStory("39200706", "1"))
    }

    @Test
    fun canSyncStory_blankStoryId_isFalse() {
        assertFalse(canSyncStory("", ""))
    }

    @Test
    fun canSyncStory_nonNumericStoryId_isFalse() {
        assertFalse(canSyncStory("abc", ""))
    }

    @Test
    fun canSyncStory_validStoryIdAndNonNumericChapterId_isFalse() {
        assertFalse(canSyncStory("39200706", "abc"))
    }
}
