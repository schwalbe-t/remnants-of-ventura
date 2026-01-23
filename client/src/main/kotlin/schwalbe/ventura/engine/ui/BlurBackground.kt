
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Shader
import schwalbe.ventura.engine.gfx.Texture
import org.joml.*
import schwalbe.ventura.engine.gfx.BufferWriteFreq
import schwalbe.ventura.engine.gfx.Framebuffer
import schwalbe.ventura.engine.gfx.UniformBuffer
import kotlin.math.exp

class BlurBackground : GpuUiElement() {
    
    companion object {
        const val MAX_KERNEL_RADIUS: Int = 1024
        const val SIGMA: Float = 1.75f
    }
    
    
    private var kernelWeights = Framebuffer()
        .attachColor(Texture(1, 1, Texture.Filter.NEAREST, Texture.Format.R16F))
    
    private fun computeKernelWeights() {
        val size: Int = 1 + (this.radius * 2)
        this.kernelWeights.resize(size, size)
        val shader: Shader<FullBuffer, GaussianDistrib> = gaussianDistribShader()
        shader[GaussianDistrib.sigma] = this.radius.toFloat() / BlurBackground.SIGMA
        shader[GaussianDistrib.kernelRadius] = this.radius
        quad().render(shader, this.kernelWeights)
    }
    
    private var radius: Int = 1
        set(value) {
            val maxValue: Int = BlurBackground.MAX_KERNEL_RADIUS
            require(value in 0..maxValue) {
                "Blur radius $value is not between 0 and $maxValue"
            }
            field = value
            this.computeKernelWeights()
            this.invalidate()
        }
    
    override val children: List<UiElement> = listOf()

    override fun captureInput(context: UiElementContext) {
        super.captureInput(context)
        this.invalidate()
    }

    override fun render(context: UiElementContext) {
        this.prepareTarget()
        val background: Texture = context.global.output.color ?: return
        val kernelWeights: Texture = this.kernelWeights.color ?: return
        val shader: Shader<FullBuffer, BlurBg> = blurBgShader()
        shader[BlurBg.background] = background
        shader[BlurBg.backgroundSizePx] = Vector2f(
            background.width.toFloat(), background.height.toFloat()
        )
        shader[BlurBg.bufferSizePx] = Vector2f(
            this.target.width.toFloat(), this.target.height.toFloat()
        )
        shader[BlurBg.absOffsetPx] = Vector2f(
            context.absPxX.toFloat(), context.absPxY.toFloat()
        )
        shader[BlurBg.kernelRadius] = this.radius
        shader[BlurBg.kernelWeights] = kernelWeights
        quad().render(shader, this.target)
    }
    
    override fun dispose() {
        super.dispose()
        this.kernelWeights.color?.dispose()
        this.kernelWeights.dispose()
    }
    
    fun withRadius(radius: Int): BlurBackground {
        this.radius = radius
        return this
    }
    
}