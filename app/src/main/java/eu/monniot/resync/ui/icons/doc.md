We follow how the `androidx.compose.material.icons.*.*` extensions are written but for other icons.
The idea is to translate the SVG path attribute into a series of method calls on `PathBuilder`. This
is pretty straightforward :)

Here is a table/list of all the SVG path commands and their alternative on PathBuilder:

    MoveTo:
        M -> moveTo, m -> moveToRelative
    LineTo:
        L -> lineTo, l -> lineToRelative,
        H -> horizontalTo, h -> horizontalToRelative,
        V -> verticalTo, v -> verticalToRelative
    Cubic Bézier Curve:
        C -> curveTo, c -> curveToRelative,
        S -> reflectiveCurveTo, s -> reflectiveCurveToRelative
    Quadratic Bézier Curve:
        Q -> quadTo , q -> quadToRelative,
        T -> reflectiveQuadTo, t -> reflectiveQuadToRelative
    Elliptical Arc Curve:
        A -> arcTo, a -> arcToRelative
    ClosePath:
        Z, z -> close
