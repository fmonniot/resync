package eu.monniot.resync.ui

import androidx.compose.animation.core.CubicBezierEasing

/**
 * The design's "emphasized decelerate" easing curve (see
 * docs/tickets/redesign-12-motion-and-animation.md). The M3 library's own
 * `androidx.compose.material3.tokens.MotionTokens.EmphasizedDecelerateCubicBezier` isn't usable -
 * that package is internal - so this is a hand-transcribed equivalent, declared once here and
 * reused by every screen-level transition instead of each screen rolling its own.
 */
val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
