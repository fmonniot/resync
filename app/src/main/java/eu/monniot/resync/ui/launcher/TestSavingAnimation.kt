package eu.monniot.resync.ui.launcher

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.progressSemantics
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.monniot.resync.ui.ReSyncTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max


// CircularProgressIndicator Material specs
// Diameter of the indicator circle
private val CircularIndicatorDiameter = 40.dp

// Indeterminate circular indicator transition specs

// The animation comprises of 5 rotations around the circle forming a 5 pointed star.
// After the 5th rotation, we are back at the beginning of the circle.
private const val RotationsPerCycle = 5

// Each rotation is 1 and 1/3 seconds, but 1332ms divides more evenly
private const val RotationDuration = 1332

// When the rotation is at its beginning (0 or 360 degrees) we want it to be drawn at 12 o clock,
// which means 270 degrees when drawing.
private const val StartAngleOffset = -90f

// How far the base point moves around the circle
private const val BaseRotationAngle = 286f

// How far the head and tail should jump forward during one rotation past the base point
private const val JumpRotationAngle = 290f

// Each rotation we want to offset the start position by this much, so we continue where
// the previous rotation ended. This is the maximum angle covered during one rotation.
private const val RotationAngleOffset = (BaseRotationAngle + JumpRotationAngle) % 360f

// The head animates for the first half of a rotation, then is static for the second half
// The tail is static for the first half and then animates for the second half
private const val HeadAndTailAnimationDuration = (RotationDuration * 0.5).toInt()
private const val HeadAndTailDelayDuration = HeadAndTailAnimationDuration

// The easing for the head and tail jump
private val CircularEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)


// Inspiration: https://lottiefiles.com/782-check-mark-success
@Composable
fun TestingSavingAnimation() {

    val (state, setState) = remember { mutableStateOf("loading") }
    val (angles, setAngles) = remember { mutableStateOf(Angles(0f, 0f)) }


    Column {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.LightGray)
                .height(128.dp)
        ) {

            // Left side, the progress indicator
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                val progressModifier = Modifier
                    .aspectRatio(1f)
                    .background(Color.DarkGray)

                when (state) {
                    "done" -> {
                        DeterminateProgress(
                            modifier = progressModifier,
                            outcome = ProgressOutcome.Success, // Depends on the upload result
                            angles = angles
                        )
                    }
                    "error" -> {

                    }
                    else -> {
                        IndeterminateProgress(
                            modifier = progressModifier,
                            setAngles = setAngles
                        )
                    }
                }
            }

            // Right side, state controls
            Column(
                modifier = Modifier
                    .requiredWidth(100.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = { setState("loading") }) {
                    Text(text = "loading")
                }
                Button(onClick = { setState("done") }) {
                    Text(text = "done")
                }
                Button(onClick = { setState("error") }) {
                    Text(text = "error")
                }
            }

        }

        Text("state = $state")
        Text("angles = $angles")

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 24.dp)
        ) {
            Text("Sweep", modifier = Modifier.width(64.dp))
            Slider(
                value = angles.sweep,
                onValueChange = { setAngles(angles.copy(sweep = it)) },
                valueRange = 0f..360f,
            )
        }
    }
}

data class Angles(
    val start: Float,
    val sweep: Float
)

enum class ProgressOutcome { Success, Error }

/**
 *
 * The circle is completely drawn, we should now
 * 1. Draw the background color, growing it from the center
 * 2. Half-way through the bg, start drawing checkmark.
 *    At that time, start drawing a brighter bg, same anim
 * 3. 3/4 in the check animation, reduce the bg brightness to
 *    its final value.
 */
enum class ProgressState {
    Initialized, // start finishing circle
    CircleCompleted, // start drawing background
    BackgroundHalfDrawn, // start drawing icon
    IconHalfDrawn, // start adjusting bg color
    Finished,
}

@Composable
fun DeterminateProgress(
    angles: Angles,
    outcome: ProgressOutcome,
    modifier: Modifier = Modifier,
) {
    val color: Color = MaterialTheme.colors.primary
    val strokeWidth: Dp = ProgressIndicatorDefaults.StrokeWidth

    val stroke = with(LocalDensity.current) {
        Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Square)
    }

    var arcDimen by remember { mutableStateOf(0f) }
    var currentState by remember { mutableStateOf(ProgressState.Initialized) }
    val transition = updateTransition(currentState, "progress-transition")

    // TODO Speed (°/ms) should be constant and animation time variable
    val sweepAngle by transition.animateFloat(label = "circle sweep angle") { state ->
        when (state) {
            ProgressState.Initialized -> angles.sweep
            else -> 360f
        }
    }

    val backgroundRadius by transition.animateFloat(label = "bg radius") { state ->
        // arcDimen gives us the diameter, and we need the radius
        when (state) {
            ProgressState.Initialized, ProgressState.CircleCompleted -> 0f
            ProgressState.BackgroundHalfDrawn -> arcDimen / 2 / 2
            else -> arcDimen / 2
        }
    }

    // TODO Tweak colors, not convinced by the colors we currently have
    val backgroundColor by transition.animateColor(label = "bg color") { state ->
        when (state) {
            ProgressState.IconHalfDrawn, ProgressState.Finished -> {
                Color(0xFF66BB6A) // darker green
            }
            else -> Color(0xFF76FF03) // lighter green
        }
    }

    // When the transition reached the point when current and target are the same,
    // it means the current animation has finished. We use that time to schedule
    // the next animation.
    if (transition.currentState == transition.targetState) {
        when (transition.currentState) {
            ProgressState.Initialized -> {
                // Start the animation by completing the circle
                currentState = ProgressState.CircleCompleted
            }
            ProgressState.CircleCompleted -> {
                // 1. Draw the background color, growing it from the center
                currentState = ProgressState.BackgroundHalfDrawn
            }
            ProgressState.BackgroundHalfDrawn -> {
                // 2. Half-way through the bg, start drawing checkmark.
                //    At that time, start drawing a brighter bg, same anim
                currentState = ProgressState.IconHalfDrawn
            }
            ProgressState.IconHalfDrawn -> {
                // 3. 3/4 in the check animation, reduce the bg brightness to
                //    its final value.
                currentState = ProgressState.Finished
            }
            ProgressState.Finished -> {
                // We are done with the animation :)
                // Maybe a callback to the caller to let them know ?
            }
        }
    }



    Canvas(
        modifier.focusable()
    ) {

        // To draw this circle we need a rect with edges that line up with the midpoint of the stroke.
        // To do this we need to remove half the stroke width from the total diameter for both sides.
        val diameterOffset = stroke.width / 2

        if (arcDimen == 0f) {
            println("Update arc dimension")
            arcDimen = size.width - 2 * diameterOffset
        }

        drawArc(
            color = color,
            startAngle = angles.start,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(diameterOffset, diameterOffset),
            size = Size(arcDimen, arcDimen),
            style = stroke
        )

        drawCircle(
            color = backgroundColor,
            radius = backgroundRadius,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CheckmarkAnimationPreview() {
    ReSyncTheme {
        Box(Modifier.size(128.dp)) {
            DeterminateProgress(
                Angles(147f, 10f),
                ProgressOutcome.Success,
                modifier = Modifier
                    .aspectRatio(1f)
                    .background(Color.DarkGray)
            )
        }
    }
}

@Composable
fun IndeterminateProgress(
    modifier: Modifier = Modifier,
    setAngles: (Angles) -> Unit,
) {
// The code below has been extracted from compose-material, because we need to have access
    // to the angles of the head and tail of the indicator. Without this constraint, we could
    // have reused the original implementation.
    // TODO Try to extract this code into its own object,
    // to separate it from the rest of the code base. That should simplify maintenance if there
    // are bug fixes to merge from upstream.

    //// fun CircularProgressIndicator

    val color: Color = MaterialTheme.colors.primary
    val strokeWidth: Dp = ProgressIndicatorDefaults.StrokeWidth

    val stroke = with(LocalDensity.current) {
        Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Square)
    }

    val transition = rememberInfiniteTransition()
    // The current rotation around the circle, so we know where to start the rotation from
    val currentRotation by transition.animateValue(
        0,
        RotationsPerCycle,
        Int.VectorConverter,
        infiniteRepeatable(
            animation = tween(
                durationMillis = RotationDuration * RotationsPerCycle,
                easing = LinearEasing
            )
        )
    )
    // How far forward (degrees) the base point should be from the start point
    val baseRotation by transition.animateFloat(
        0f,
        BaseRotationAngle,
        infiniteRepeatable(
            animation = tween(
                durationMillis = RotationDuration,
                easing = LinearEasing
            )
        )
    )
    // How far forward (degrees) both the head and tail should be from the base point
    val endAngle by transition.animateFloat(
        0f,
        JumpRotationAngle,
        infiniteRepeatable(
            animation = keyframes {
                durationMillis = HeadAndTailAnimationDuration + HeadAndTailDelayDuration
                0f at 0 with CircularEasing
                JumpRotationAngle at HeadAndTailAnimationDuration
            }
        )
    )

    val startAngle by transition.animateFloat(
        0f,
        JumpRotationAngle,
        infiniteRepeatable(
            animation = keyframes {
                durationMillis = HeadAndTailAnimationDuration + HeadAndTailDelayDuration
                0f at HeadAndTailDelayDuration with CircularEasing
                JumpRotationAngle at durationMillis
            }
        )
    )

    Canvas(
        modifier
            .progressSemantics()
            .size(CircularIndicatorDiameter)
            .focusable()
    ) {

        val currentRotationAngleOffset = (currentRotation * RotationAngleOffset) % 360f

        // How long a line to draw using the start angle as a reference point
        val sweep = abs(endAngle - startAngle)

        // Offset by the constant offset and the per rotation offset
        val startAngle = startAngle + StartAngleOffset + currentRotationAngleOffset + baseRotation


        //// fun drawIndeterminateCircularIndicator

        // Length of arc is angle * radius
        // Angle (radians) is length / radius
        // The length should be the same as the stroke width for calculating the min angle
        val squareStrokeCapOffset =
            (180.0 / PI).toFloat() * (strokeWidth / (CircularIndicatorDiameter / 2)) / 2f

        // Adding a square stroke cap draws half the stroke width behind the start point, so we want to
        // move it forward by that amount so the arc visually appears in the correct place
        val adjustedStartAngle = startAngle + squareStrokeCapOffset

        // When the start and end angles are in the same place, we still want to draw a small sweep, so
        // the stroke caps get added on both ends and we draw the correct minimum length arc
        val adjustedSweep = max(sweep, 0.1f)

        //// fun drawCircularIndicator


        // Update the current angles to let us switch to check animation
        // at any time. That animation will need to complete the circle
        // and need a starting point equals to what is currently on screen.
        setAngles(Angles(adjustedStartAngle, adjustedSweep))

        // To draw this circle we need a rect with edges that line up with the midpoint of the stroke.
        // To do this we need to remove half the stroke width from the total diameter for both sides.
        val diameterOffset = stroke.width / 2
        val arcDimen = size.width - 2 * diameterOffset
        drawArc(
            color = color,
            startAngle = adjustedStartAngle,
            sweepAngle = adjustedSweep,
            useCenter = false,
            topLeft = Offset(diameterOffset, diameterOffset),
            size = Size(arcDimen, arcDimen),
            style = stroke
        )
    }
}