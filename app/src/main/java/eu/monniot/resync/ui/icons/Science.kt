package eu.monniot.resync.ui.icons


import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

val KeyboardArrowRight: ImageVector
    get() {
        if (Cache.keyboardArrowRight != null) {
            return Cache.keyboardArrowRight!!
        }
        Cache.keyboardArrowRight = materialIcon(name = "Rounded.KeyboardArrowRight") {
            materialPath {
                moveTo(9.29f, 15.46f)
                lineToRelative(3.88f, -3.88f)
                lineTo(9.29f, 7.7f)
                curveToRelative(-0.39f, -0.39f, -0.39f, -1.02f, 0.0f, -1.41f)
                lineToRelative(0.0f, 0.0f)
                curveToRelative(0.39f, -0.39f, 1.02f, -0.39f, 1.41f, 0.0f)
                lineToRelative(4.59f, 4.59f)
                curveToRelative(0.39f, 0.39f, 0.39f, 1.02f, 0.0f, 1.41f)
                lineToRelative(-4.59f, 4.59f)
                curveToRelative(-0.39f, 0.39f, -1.02f, 0.39f, -1.41f, 0.0f)
                lineToRelative(0.0f, 0.0f)
                curveTo(8.91f, 16.49f, 8.9f, 15.85f, 9.29f, 15.46f)
                close()
            }
        }
        return Cache.keyboardArrowRight!!
    }

val Science: ImageVector
    get() {
        if (Cache.science != null) {
            return Cache.science!!
        }
        Cache.science = materialIcon(name = "Rounded.Science") {
            materialPath {
                /*
                Help: SVG defines 6 types of path commands, for a total of 20 commands:
                    MoveTo: M, m
                    LineTo: L, l, H, h, V, v
                    Cubic Bézier Curve: C, c, S, s
                    Quadratic Bézier Curve: Q, q, T, t
                    Elliptical Arc Curve: A, a
                    ClosePath: Z, z

                Path below is for a 24x24 image
                    M 20.54,17.73
                    L 15,11
                    V 5
                    h 1
                    c 0.55,0,1-0.45,1-1
                    s -0.45-1-1-1
                    H 8
                    C 7.45,3,7,3.45,7,4
                    s 0.45,1,1,1
                    h 1
                    v 6
                    l -5.54,6.73
                    C 3.14,18.12,3,18.56,3,19
                    c 0.01,1.03,0.82,2,2,2
                    H 19

                    c 1.19,0,2-0.97,2-2
                    C 21,18.56,20.86,18.12,20.54,17.73
                    z

                This has to be converted in something similar to what's below
         */
                moveTo(20.54f, 17.73f)
                lineTo(15f, 11f)
                verticalLineTo(5f)
                horizontalLineToRelative(1f)
                curveToRelative(0.55f,0f,1f,-0.45f,1f,-1f)
                reflectiveCurveToRelative(-0.45f, -1f, -1f, -1f)
                horizontalLineTo(8f)
                curveTo(7.45f, 3f, 7f, 3.45f, 7f, 4f)
                reflectiveCurveToRelative(0.45f, 1f, 1f, 1f)
                horizontalLineToRelative(1f)
                verticalLineToRelative(6f)
                lineToRelative(-5.54f, 6.73f)
                curveTo(3.14f, 18.12f, 3f, 18.56f, 3f, 19f)
                curveToRelative(0.01f, 1.03f, 0.82f, 2f, 2f, 2f)
                horizontalLineTo(19f)
                curveToRelative(1.19f, 0f, 2f, -0.97f, 2f, -2f)
                curveTo(21f, 18.56f, 20.86f, 18.12f, 20.54f, 17.73f)
                close()

                // previous one, will be removed
                /*
                moveTo(9.29f, 15.46f)
                lineToRelative(3.88f, -3.88f)
                lineTo(9.29f, 7.7f)
                curveToRelative(-0.39f, -0.39f, -0.39f, -1.02f, 0.0f, -1.41f)
                lineToRelative(0.0f, 0.0f)
                curveToRelative(0.39f, -0.39f, 1.02f, -0.39f, 1.41f, 0.0f)
                lineToRelative(4.59f, 4.59f)
                curveToRelative(0.39f, 0.39f, 0.39f, 1.02f, 0.0f, 1.41f)
                lineToRelative(-4.59f, 4.59f)
                curveToRelative(-0.39f, 0.39f, -1.02f, 0.39f, -1.41f, 0.0f)
                lineToRelative(0.0f, 0.0f)
                curveTo(8.91f, 16.49f, 8.9f, 15.85f, 9.29f, 15.46f)
                close()
                */
            }
        }
        return Cache.science!!



        TODO()
    }

private object Cache {
    var keyboardArrowRight: ImageVector? = null
    var science: ImageVector? = null
}
