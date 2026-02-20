
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Shader
import schwalbe.ventura.engine.gfx.Texture
import schwalbe.ventura.engine.gfx.Framebuffer
import org.joml.*

class BlurBackground : GpuUiElement() {
    
    companion object {
        const val MAX_KERNEL_RADIUS: Int = 1024
        const val SIGMA: Float = 1.75f
    }
    
    
    private var kernelWeights = Framebuffer()
        .attachColor(Texture(1, 1, Texture.Filter.NEAREST, Texture.Format.R8))
    
    private fun computeKernelWeights() {
        val size: Int = 1 + (this.radius * 2)
        if (!this.kernelWeights.resize(size, size)) { return }
        val shader: Shader<FullBuffer, GaussianDistrib>
            = gaussianDistribShader()
        shader[GaussianDistrib.sigma] = this.radius.toFloat() / SIGMA
        shader[GaussianDistrib.kernelRadius] = this.radius
        quad().render(shader, this.kernelWeights)
    }
    
    var radius: Int = 0
        set(value) {
            require(value in 0..MAX_KERNEL_RADIUS) {
                "Blur radius $value is not between 0 and $MAX_KERNEL_RADIUS"
            }
            if (field != value) {
                this.invalidate()
            }
            field = value
        }
    var spread: Int = 1
        set(value) {
            require(value >= 1) {
                "Blur spread distance must be greater or equal to 1"
            }
            if (field != value) {
                this.invalidate()
            }
            field = value
        }
    
    override val children: List<UiElement> = listOf()

    override fun render(context: UiElementContext) {
        this.prepareTarget()
        this.computeKernelWeights()
        val background: Texture = context.global.nav.output.color ?: return
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
        shader[BlurBg.kernelSpread] = this.spread
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

    fun withSpread(spread: Int): BlurBackground {
        this.spread = spread
        return this
    }
    
}