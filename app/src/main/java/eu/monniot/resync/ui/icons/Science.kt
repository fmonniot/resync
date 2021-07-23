package eu.monniot.resync.ui.icons


import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

val Science: ImageVector
    get() {
        if (science != null) {
            return science!!
        }
        science = materialIcon(name = "Rounded.Science") {
            materialPath {
                moveTo(20.54f, 17.73f)
                lineTo(15f, 11f)
                verticalLineTo(5f)
                horizontalLineToRelative(1f)
                curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
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
            }
        }
        return science!!
    }

private var science: ImageVector? = null
