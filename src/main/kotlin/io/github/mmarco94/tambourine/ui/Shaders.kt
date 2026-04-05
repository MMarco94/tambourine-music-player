package io.github.mmarco94.tambourine.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ShaderBrush
import org.intellij.lang.annotations.Language
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder


@Language("AGSL")
private const val paperLikeShader = """
uniform float2 resolution;
half4 main(float2 fragCoord) {
    float2 uv = fragCoord / resolution;
    float noise1 = fract(sin(dot(uv, float2(12.987, 56.123))) * 12345.6789);
    float noise2 = fract(sin(dot(uv, float2(23.654, 67.456))) * 23456.7891);
    float noise3 = fract(sin(dot(uv, float2(34.321, 78.789))) * 34567.8912);
    return 0.05 * half4(noise1, noise2, noise3, 0.0);
}"""

@Composable
fun Modifier.paperNoise(): Modifier {
    val shaderBuilder = remember(paperLikeShader) {
        RuntimeShaderBuilder(RuntimeEffect.makeForShader(paperLikeShader))
    }
    return drawWithContent {
        drawContent()
        shaderBuilder.uniform("resolution", this.size.width, this.size.height)
        val brush = ShaderBrush(shaderBuilder.makeShader())
        drawRect(brush)
    }
}
