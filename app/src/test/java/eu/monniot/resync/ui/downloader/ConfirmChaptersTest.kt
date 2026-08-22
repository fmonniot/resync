package eu.monniot.resync.ui.downloader

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
 * 2. The two-thumb [androidx.compose.material3.RangeSlider] doesn't expose a stable per-thumb
 *    test tag - only the "Range start"/"Range end" a11y content descriptions Material3 attaches
 *    to each thumb by default - so these tests grab a thumb directly with
 *    [onNodeWithContentDescription] and drag from *that thumb's own on-screen position*, instead
 *    of computing an edge-to-edge swipe against the slider's bounds. That distinction matters
 *    because of how RangeSlider decides which thumb a touch captures
 *    (`rangeSliderPressDragModifier` in Material3's `Slider.kt`): it picks whichever thumb is
 *    nearest the touch-down point, *unless* both thumbs are within touch-slop of that point, in
 *    which case it instead picks based on the initial drag direction. Touching down exactly on a
 *    named thumb's own position makes the nearest-thumb rule trivially pick that thumb (distance
 *    to itself is zero, which is never more than the other thumb's distance) - and even once the
 *    two thumbs are dragged on top of each other, where the slop-based direction rule also kicks
 *    in, dragging further in the same direction still resolves to the same thumb. So both rules
 *    agree regardless of which one actually applies at runtime, and which content-description
 *    node the test happened to target keeps controlling the outcome. [dragFarLeft] and
 *    [dragFarRight] then overshoot far past the slider's opposite edge so each drag reliably
 *    clamps at `valueRange`'s floor/ceiling instead of landing on some layout-dependent pixel:
 *      - totalChapters = 10, initial range is chapterStart=1 (left edge) .. chapterEnd=10 (right
 *        edge). Dragging the "Range end" thumb far left ([dragFarLeft]) clamps it at
 *        chapterStart's value (1) -> both thumbs meet -> `ChapterSelection.One(1)`.
 *      - Dragging that same "Range end" thumb back out ([dragFarRight]) from the merged-at-1
 *        state clamps it at totalChapters (10) -> `ChapterSelection.Range(1, 10)`.
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

    // See class doc point 2: dragging starts from the targeted thumb's own center rather than
    // the slider's edges, and overshoots by a distance no reasonably-sized test screen's
    // RangeSlider could span, so the drag reliably clamps at the value range's floor/ceiling
    // regardless of the slider's actual pixel layout.
    private fun TouchInjectionScope.dragFarLeft() {
        swipe(center, Offset(centerX - FAR_DRAG_DISTANCE_PX, centerY), durationMillis = 200)
    }

    private fun TouchInjectionScope.dragFarRight() {
        swipe(center, Offset(centerX + FAR_DRAG_DISTANCE_PX, centerY), durationMillis = 200)
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

        composeRule.onNodeWithContentDescription("Range end")
            .performTouchInput { dragFarLeft() }

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

        val endThumb = composeRule.onNodeWithContentDescription("Range end")

        // Converge both thumbs at chapter 1 first (see class doc), then drag them back apart -
        // this exercises both the One and Range branches of the confirm button, not just Range.
        endThumb.performTouchInput { dragFarLeft() }
        composeRule.onNodeWithText("Download chapter 1").assertExists()

        endThumb.performTouchInput { dragFarRight() }

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

// Overshoot distance for [ConfirmChaptersTest.dragFarLeft]/[ConfirmChaptersTest.dragFarRight]:
// comfortably larger than any RangeSlider width this test could plausibly render at, so the
// drag's destination always lies past the slider's opposite edge and clamps there.
private const val FAR_DRAG_DISTANCE_PX = 10_000f
