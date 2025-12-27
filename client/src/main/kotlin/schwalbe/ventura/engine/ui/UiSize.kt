
package schwalbe.ventura.engine.ui

typealias UiSize = (ctx: UiElementContext) -> Float

/** Size of a pixel */
val px: UiSize = { 1f }
/** Relative to 1% of the width of the viewport */
val vw: UiSize = { ctx -> ctx.global.output.width / 100f }
/** Relative to 1% of the height of the viewport */
val vh: UiSize = { ctx -> ctx.global.output.height / 100f }
/** Relative to 1% of the width of the parent element */
val pw: UiSize = { ctx -> ctx.parent.pxWidth / 100f }
/** Relative to 1% of the height of the parent element */
val ph: UiSize = { ctx -> ctx.parent.pxHeight / 100f }

/** The full width of the viewport */
val fvw: UiSize = { ctx -> ctx.global.output.width.toFloat() }
/** The full height of the viewport */
val fvh: UiSize = { ctx -> ctx.global.output.height.toFloat() }
/** The full width of the parent element */
val fpw: UiSize = { ctx -> ctx.parent.pxWidth }
/** The full height of the parent element */
val fph: UiSize = { ctx -> ctx.parent.pxHeight }

/** Relative to 1% of viewport's smaller dimension */
val vmin: UiSize = minOf(vw, vh)
/** Relative to 1% of viewport's larger dimension */
val vmax: UiSize = maxOf(vw, vh)
/** Relative to 1% of parent element's smaller dimension */
val pmin: UiSize = minOf(pw, ph)
/** Relative to 1% of parent element's larger dimension */
val pmax: UiSize = maxOf(pw, ph)

inline operator fun Int.times(crossinline f: UiSize): UiSize
    = { ctx -> this * f(ctx) }
inline operator fun Float.times(crossinline f: UiSize): UiSize
    = { ctx -> this * f(ctx) }
inline operator fun Double.times(crossinline f: UiSize): UiSize
    = { ctx -> this.toFloat() * f(ctx) }

operator fun UiSize.times(scalar: Int): UiSize
    = { ctx -> this(ctx) * scalar }
operator fun UiSize.times(scalar: Float): UiSize
    = { ctx -> this(ctx) * scalar }
operator fun UiSize.times(scalar: Double): UiSize
    = { ctx -> this(ctx) * scalar.toFloat() }

inline operator fun UiSize.plus(crossinline f: UiSize): UiSize
    = { ctx -> this(ctx) + f(ctx) }
inline operator fun UiSize.minus(crossinline f: UiSize): UiSize
    = { ctx -> this(ctx) - f(ctx) }

inline fun minOf(crossinline a: UiSize, crossinline b: UiSize): UiSize
    = { ctx -> minOf(a(ctx), b(ctx)) }
inline fun minOf(
    crossinline a: UiSize, crossinline b: UiSize, crossinline c: UiSize
): UiSize
    = { ctx -> minOf(a(ctx), b(ctx), c(ctx)) }
fun minOf(vararg sizes: UiSize): UiSize
    = { ctx -> sizes.minOf { it(ctx) } }

inline fun maxOf(crossinline a: UiSize, crossinline b: UiSize): UiSize
    = { ctx -> maxOf(a(ctx), b(ctx)) }
inline fun maxOf(
    crossinline a: UiSize, crossinline b: UiSize, crossinline c: UiSize
): UiSize
    = { ctx -> maxOf(a(ctx), b(ctx), c(ctx)) }
fun maxOf(vararg sizes: UiSize): UiSize
    = { ctx -> sizes.maxOf { it(ctx) } }

inline fun UiSize.clamp(
    crossinline minVal: UiSize, crossinline maxVal: UiSize
): UiSize
    = { ctx -> this(ctx).coerceIn(minVal(ctx), maxVal(ctx)) }

val Int.px: UiSize      get() = this * schwalbe.ventura.engine.ui.px
val Int.vw: UiSize      get() = this * schwalbe.ventura.engine.ui.vw
val Int.vh: UiSize      get() = this * schwalbe.ventura.engine.ui.vh
val Int.pw: UiSize      get() = this * schwalbe.ventura.engine.ui.pw
val Int.ph: UiSize      get() = this * schwalbe.ventura.engine.ui.ph
val Int.vmin: UiSize    get() = this * schwalbe.ventura.engine.ui.vmin
val Int.vmax: UiSize    get() = this * schwalbe.ventura.engine.ui.vmax
val Int.pmin: UiSize    get() = this * schwalbe.ventura.engine.ui.pmin
val Int.pmax: UiSize    get() = this * schwalbe.ventura.engine.ui.pmax

val Float.px: UiSize    get() = this * schwalbe.ventura.engine.ui.px
val Float.vw: UiSize    get() = this * schwalbe.ventura.engine.ui.vw
val Float.vh: UiSize    get() = this * schwalbe.ventura.engine.ui.vh
val Float.pw: UiSize    get() = this * schwalbe.ventura.engine.ui.pw
val Float.ph: UiSize    get() = this * schwalbe.ventura.engine.ui.ph
val Float.vmin: UiSize  get() = this * schwalbe.ventura.engine.ui.vmin
val Float.vmax: UiSize  get() = this * schwalbe.ventura.engine.ui.vmax
val Float.pmin: UiSize  get() = this * schwalbe.ventura.engine.ui.pmin
val Float.pmax: UiSize  get() = this * schwalbe.ventura.engine.ui.pmax

val Double.px: UiSize   get() = this * schwalbe.ventura.engine.ui.px
val Double.vw: UiSize   get() = this * schwalbe.ventura.engine.ui.vw
val Double.vh: UiSize   get() = this * schwalbe.ventura.engine.ui.vh
val Double.pw: UiSize   get() = this * schwalbe.ventura.engine.ui.pw
val Double.ph: UiSize   get() = this * schwalbe.ventura.engine.ui.ph
val Double.vmin: UiSize get() = this * schwalbe.ventura.engine.ui.vmin
val Double.vmax: UiSize get() = this * schwalbe.ventura.engine.ui.vmax
val Double.pmin: UiSize get() = this * schwalbe.ventura.engine.ui.pmin
val Double.pmax: UiSize get() = this * schwalbe.ventura.engine.ui.pmax