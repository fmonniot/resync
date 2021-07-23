package eu.monniot.resync.ui.icons


import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder

val LibraryBooks: ImageVector
    get() {
        if (libraryBooks != null) {
            return libraryBooks!!
        }
        libraryBooks = materialIcon(name = "Rounded.LibraryBooks") {

            materialPath {
                // back document outline
                moveTo(3f, 6f)
                curveToRelative(-.55f, 0f, -1f, .45f, -1f, 1f)
                verticalLineToRelative(13f)
                curveToRelative(0f, 1.1f, .9f, 2f, 2f, 2f)
                horizontalLineToRelative(13f)
                curveToRelative(.55f, 0f, 1f, -.45f, 1f, -1f)
                reflectiveCurveToRelative(-.45f, -1f, -1f, -1f)
                horizontalLineTo(5f)
                curveToRelative(-.55f, 0f, -1f, -.45f, -1f, -1f)
                verticalLineTo(7f)
                curveToRelative(0f, -.55f, -.45f, -1f, -1f, -1f)
                close()

                // main document
                moveToRelative(17f, -4f)
                horizontalLineTo(8f)
                curveToRelative(-1.1f, 0f, -2f, .9f, -2f, 2f)
                verticalLineToRelative(12f)
                curveToRelative(0f, 1.1f, .9f, 2f, 2f, 2f)
                horizontalLineToRelative(12f)
                curveToRelative(1.1f, 0f, 2f, -.9f, 2f, -2f)
                verticalLineTo(4f)
                curveToRelative(0f, -1.1f, -.9f, -2f, -2f, -2f)
                close()

                // bottom line
                documentLine(-2f, 9f, 8f)

                // middle
                documentLine(-4f, 4f, 4f)

                // top
                documentLine(4f, -8f, 8f)
            }
        }
        return libraryBooks!!
    }

private fun PathBuilder.documentLine(dx: Float, dy: Float, height: Float) {
    moveToRelative(dx, dy)
    horizontalLineToRelative(-height)
    curveToRelative(-.55f, 0f, -1f, -.45f, -1f, -1f)
    reflectiveCurveToRelative(.45f, -1f, 1f, -1f)
    horizontalLineToRelative(height)
    curveToRelative(.55f, 0f, 1f, .45f, 1f, 1f)
    reflectiveCurveToRelative(-.45f, 1f, -1f, 1f)
    close()
}

private var libraryBooks: ImageVector? = null
