package eu.monniot.resync

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.monniot.resync.rmcloud.readTokens
import eu.monniot.resync.ui.ReSyncTheme
import eu.monniot.resync.ui.SetupRemarkableScreen
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max


class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokens = mutableStateOf(readTokens(applicationContext))

        setContent(null) {
            ReSyncTheme {
                Surface(color = MaterialTheme.colors.background) {

                    if (tokens.value == null) {
                        SetupRemarkableScreen {
                            tokens.value = it
                        }
                    } else {
                        Greeting()
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting() {
    Column {

        val storyId = mutableStateOf(TextFieldValue())
        val chapter = mutableStateOf(TextFieldValue())

        Text("Story Id")
        TextField(
            value = storyId.value,
            onValueChange = { storyId.value = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Text("Specific chapter (optional)")
        TextField(
            value = chapter.value,
            onValueChange = { chapter.value = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Button(onClick = { /*TODO*/ }) {
            Text("Sync (TODO)")
        }

        Spacer(modifier = Modifier.height(30.dp))

        TestingLoading()
    }
}

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
fun TestingLoading() {

    val (state, setState) = remember { mutableStateOf("loading") }
    val (angles, setAngles) = remember { mutableStateOf(Pair(0f, 0f)) }


    Row(modifier = Modifier
        .fillMaxWidth()
        .height(128.dp)) {

        // Left side, the progress indicator
        when(state) {
            "done" -> {

            }
            "error" -> {

            }
            else -> {
                IndeterminateProgress(
                    modifier = Modifier
                        .fillMaxSize() // Or do I want to fix it? It's going to only be used with DownloadScreen and that would simplify the impl a lot
                        .background(Color.DarkGray),
                    setAngles = setAngles
                )
            }
        }

        // Right side, state controls
        Column(modifier = Modifier.width(128.dp)) {
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
}

@Composable
fun IndeterminateProgress(
    modifier: Modifier = Modifier,
    setAngles: (Pair<Float, Float>) -> Unit,
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

    // Update the current angles to let us switch to check animation
    // at any time. That animation will need to complete the circle
    // and need a starting point equals to what is currently on screen.
    setAngles(Pair(startAngle, endAngle))

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

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
    name = "Launcher - Pixel 3"
)
@Composable
fun DefaultPreview() {
    ReSyncTheme {
        Greeting()
    }
}