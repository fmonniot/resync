package eu.monniot.resync.ui.downloader

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import eu.monniot.resync.downloader.DriverType
import eu.monniot.resync.ui.ReSyncTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Renders [ConfirmChapters] with Robolectric + [createComposeRule] and drives it through real
 * Compose semantics (clicks, drags) rather than calling its private lambdas directly - see
 * CLAUDE.md's "Testing notes" for the Robolectric graphicsMode/dependency setup this needs.
 *
 * IMPORTANT - this test passes and genuinely exercises [ConfirmChapters] (every assertion here
 * depends on a real callback firing from inside the composable), but it will *not* move the
 * needle in `jacocoTestReport`: JaCoCo instruments non-constructor methods via
 * `invokedynamic`/`ConstantDynamic` probes, and Robolectric's own bytecode rewriting (applied to
 * any class it decides needs Android-API shadowing, which includes this whole file) silently
 * drops those probes for anything but constructors. This is a pre-existing gap, not something
 * this test introduced or something Compose-specific - see CLAUDE.md's "Testing notes" for the
 * full writeup and for two other already-merged Robolectric tests
 * (`DeepLinkActivityTest`/`DriverTest`) that are equally invisible to JaCoCo today.
 *
 * Two non-obvious things this suite works around, both specific to driving Compose animations
 * and gestures from a test rather than a real device (a third - [performScrollTo] before the
 * confirm-button clicks - is needed because the expanded card can push the button below the
 * fold; without it the click silently lands on a clipped, zero-size node):
 *
 * 1. The expanded card is inside `AnimatedVisibility(enter = expandVertically(tween(300)), ...)`.
 *    A single big [ComposeContentTestRule.mainClock] jump (e.g. `advanceTimeBy(500)`) does *not*
 *    carry the animation to completion under Robolectric - the child stays laid out at its
 *    animation-start size (0x0), so anything inside it is untappable. Stepping the clock one
 *    frame at a time via [settleAnimations] does.
 * 2. The two-thumb [androidx.compose.material3.RangeSlider] doesn't expose a stable per-thumb id
 *    (only "Range start"/"Range end" a11y content descriptions), so [ConfirmChapters] tags the
 *    slider itself ([CHAPTER_RANGE_SLIDER_TEST_TAG]) and these tests drag against its bounds.
 *    RangeSlider always captures whichever thumb is nearest the touch-down point (ties go to
 *    whichever thumb still has room to move away from the other), so a drag starting at one edge
 *    and overshooting past the opposite edge deterministically slams that edge's thumb all the
 *    way to the far end, landing exactly on a known chapter number regardless of the slider's
 *    exact pixel layout:
 *      - totalChapters = 10, initial range is chapterStart=1 (left edge) .. chapterEnd=10 (right
 *        edge). [dragFromRightEdgeToLeftEdge] grabs the end thumb (nearest the touch-down point
 *        at the right) and drags it past the left edge -> clamps at chapterStart's value (1) ->
 *        both thumbs meet -> `ChapterSelection.One(1)`.
 *      - A further [dragFromLeftEdgeToRightEdge] from that merged-at-1 state (touch-down at the
 *        left edge, tied between both co-located thumbs, resolved to the thumb that still has
 *        room to move - the start thumb is pinned at the valueRange floor, so it's the end thumb
 *        again) drags the end thumb past the right edge -> clamps at totalChapters (10) ->
 *        `ChapterSelection.Range(1, 10)`.
 *
 * Rendering the whole composable also exercises nearly all of its non-interactive lines (every
 * Text/Modifier call runs on composition), so these four interaction tests genuinely cover
 * essentially all of [ConfirmChapters] at runtime - JaCoCo just can't see it (see above).
 */
@RunWith(RobolectricTestRunner::class)
class ConfirmChaptersTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        totalChapters: Int = 10,
        initialChapterNumber: Int = 1,
        onUserConfirmation: (ChapterSelection) -> Unit = {},
        onCancel: () -> Unit = {},
    ) {
        composeRule.setContent {
            ReSyncTheme {
                ConfirmChapters(
                    storyName = "A Very Long Story",
                    authorName = "Some Author",
                    initialChapterNumber = initialChapterNumber,
                    totalChapters = totalChapters,
                    driverType = DriverType.ArchiveOfOurOwn,
                    onUserConfirmation = onUserConfirmation,
                    onCancel = onCancel,
                )
            }
        }
    }

    // See class doc point 1: steps the test clock forward one frame at a time so the expand
    // AnimatedVisibility's tween actually reaches its end size, instead of a single big jump
    // that leaves the child laid out at 0x0.
    private fun ComposeContentTestRule.settleAnimations() {
        mainClock.autoAdvance = false
        repeat(40) {
            mainClock.advanceTimeByFrame()
            waitForIdle()
        }
        mainClock.autoAdvance = true
    }

    // See class doc point 2: a plain swipeLeft()/swipeRight() (edge to edge, exactly) lands one
    // discrete step short of the boundary in practice - RangeSlider insets its draggable track by
    // half a thumb-width, so the node's literal edge coordinate doesn't quite reach the last step.
    // Overshooting well past the opposite edge sidesteps that and reliably clamps at the boundary.
    private fun TouchInjectionScope.dragFromRightEdgeToLeftEdge() {
        swipe(Offset(right, centerY), Offset(left - width, centerY), durationMillis = 200)
    }

    private fun TouchInjectionScope.dragFromLeftEdgeToRightEdge() {
        swipe(Offset(left, centerY), Offset(right + width, centerY), durationMillis = 200)
    }

    @Test
    fun downloadEntireStory_click_confirmsAll() {
        var selection: ChapterSelection? = null
        setContent(onUserConfirmation = { selection = it })

        composeRule.onNodeWithText("Download entire story").performClick()

        assertEquals(ChapterSelection.All, selection)
    }

    @Test
    fun expandAndDragThumbsTogether_confirmsOne() {
        var selection: ChapterSelection? = null
        setContent(
            totalChapters = 10,
            initialChapterNumber = 1,
            onUserConfirmation = { selection = it },
        )

        composeRule.onNodeWithText("Choose specific chapters").performClick()
        composeRule.settleAnimations()

        composeRule.onNodeWithTag(CHAPTER_RANGE_SLIDER_TEST_TAG)
            .performTouchInput { dragFromRightEdgeToLeftEdge() }

        composeRule.onNodeWithText("Download chapter 1")
            .performScrollTo()
            .performClick()

        assertEquals(ChapterSelection.One(1), selection)
    }

    @Test
    fun expandAndDragThumbsApart_confirmsRange() {
        var selection: ChapterSelection? = null
        setContent(
            totalChapters = 10,
            initialChapterNumber = 1,
            onUserConfirmation = { selection = it },
        )

        composeRule.onNodeWithText("Choose specific chapters").performClick()
        composeRule.settleAnimations()

        val rangeSlider = composeRule.onNodeWithTag(CHAPTER_RANGE_SLIDER_TEST_TAG)

        // Converge both thumbs at chapter 1 first (see class doc), then drag them back apart -
        // this exercises both the One and Range branches of the confirm button, not just Range.
        rangeSlider.performTouchInput { dragFromRightEdgeToLeftEdge() }
        composeRule.onNodeWithText("Download chapter 1").assertExists()

        rangeSlider.performTouchInput { dragFromLeftEdgeToRightEdge() }

        // En dash (U+2013), not a hyphen - see the composable's own comment on this string.
        composeRule.onNodeWithText("Download chapters 1–10")
            .performScrollTo()
            .performClick()

        assertEquals(ChapterSelection.Range(1, 10), selection)
    }

    @Test
    fun backArrow_firesOnCancel() {
        var cancelled = false
        setContent(onCancel = { cancelled = true })

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(cancelled)
    }
}
