package io.github.mmarco94.tambourine.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ShaderBrush
import org.intellij.lang.annotations.Language
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder


@Language("AGSL")
private const val paperLikeShader = """
half4 main(float2 uv) {
    float noise1 = fract(sin(dot(uv, float2(0.012987, 0.056123))) * 12345.6789);
    float noise2 = fract(sin(dot(uv, float2(0.023654, 0.067456))) * 23456.7891);
    float noise3 = fract(sin(dot(uv, float2(0.034321, 0.078789))) * 34567.8912);
    return 0.05 * half4(noise1, noise2, noise3, 0.0);
}"""

private val paperLikeShaderBrush = ShaderBrush(
    RuntimeShaderBuilder(RuntimeEffect.makeForShader(paperLikeShader))
        .makeShader()
)

@Composable
fun Modifier.paperNoise(): Modifier {
    return drawWithContent {
        drawContent()
        drawRect(paperLikeShaderBrush)
    }
}
