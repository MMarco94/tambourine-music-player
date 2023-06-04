package ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Inspired by https://github.com/gtk-flutter/adwaita/blob/main/lib/src/theme.dart
 */
object MusicPlayerTheme {
    val shapes: Shapes = Shapes(
        small = RoundedCornerShape(size = 4.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(12.dp)
    )
    val colors: Colors = darkColors()
    val typography
        get() = Typography(
            defaultFontFamily = FontFamily.Default,
            h1 = TextStyle(
                fontWeight = FontWeight.Bold,//FontWeight.Light,
                fontSize = 26.sp,//96.sp,
                letterSpacing = (-1.5).sp
            ),
            h2 = TextStyle(
                fontWeight = FontWeight.Bold,//FontWeight.Light,
                fontSize = 21.sp,// 60.sp,
                letterSpacing = (-0.5).sp
            ),
            h3 = TextStyle(
                fontWeight = FontWeight.Bold,//FontWeight.Light,
                fontSize = 20.sp,//48.sp,
                letterSpacing = 0.sp
            ),
            h4 = TextStyle(
                fontWeight = FontWeight.Bold,//FontWeight.Light,
                fontSize = 17.sp,//34.sp,
                letterSpacing = 0.25.sp
            ),
            h5 = TextStyle(
                fontWeight = FontWeight.Bold,//FontWeight.Light,
                fontSize = 15.sp,//24.sp,
                letterSpacing = 0.sp
            ),
            h6 = TextStyle(
                fontWeight = FontWeight.SemiBold,// FontWeight.Medium,
                fontSize = 13.sp,// 20.sp,
                letterSpacing = 0.15.sp
            ),
            subtitle1 = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp, //16.sp,
                letterSpacing = 0.15.sp
            ),
            subtitle2 = TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,//14.sp,
                letterSpacing = 0.1.sp
            ),
            body1 = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp, //16.sp,
                letterSpacing = 0.25.sp
            ),
            body2 = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,//14.sp,
                letterSpacing = 0.2.sp
            ),
            button = TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,//14.sp,
                letterSpacing = 1.25.sp
            ),
            caption = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                letterSpacing = 0.4.sp
            ),
            overline = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp
            )

        )
}