package com.example.cipod_final.indoor

import kotlin.math.max
import kotlin.math.min

/** One image-space transform shared by drawing, hit testing, placement, and marker dragging. */
data class PlanTransform(
    val viewportWidth: Float = 1f,
    val viewportHeight: Float = 1f,
    val aspectRatio: Float,
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f
) {
    val fitWidth: Float get() = min(viewportWidth, viewportHeight * aspectRatio)
    val width: Float get() = fitWidth * zoom
    val height: Float get() = width / aspectRatio
    val left: Float get() = (viewportWidth - width) / 2f + panX
    val top: Float get() = (viewportHeight - height) / 2f + panY

    fun screen(point: PlanPoint): Pair<Float, Float> =
        Pair(left + point.x * width, top + point.y * height)

    fun normalized(x: Float, y: Float): PlanPoint? {
        val nx = (x - left) / width
        val ny = (y - top) / height
        return if (nx.isFinite() && ny.isFinite() && nx in 0f..1f && ny in 0f..1f) PlanPoint(nx, ny) else null
    }

    fun drag(point: PlanPoint, dx: Float, dy: Float): PlanPoint = PlanPoint(
        (point.x + dx / width).coerceIn(0f, 1f), (point.y + dy / height).coerceIn(0f, 1f)
    )

    fun gesture(factor: Float, dx: Float, dy: Float, anchorX: Float, anchorY: Float): PlanTransform {
        if (!factor.isFinite() || factor <= 0 || !dx.isFinite() || !dy.isFinite()) return this
        val nextZoom = (zoom * factor).coerceIn(1f, 6f)
        val nx = (anchorX - left) / width
        val ny = (anchorY - top) / height
        val next = copy(zoom = nextZoom)
        return next.copy(
            panX = anchorX + dx - nx * next.width - (viewportWidth - next.width) / 2,
            panY = anchorY + dy - ny * next.height - (viewportHeight - next.height) / 2
        ).bounded()
    }

    fun resized(w: Float, h: Float): PlanTransform {
        val next = copy(viewportWidth = max(1f, w), viewportHeight = max(1f, h))
        val ratio = next.fitWidth / fitWidth
        return next.copy(panX = panX * ratio, panY = panY * ratio).bounded()
    }

    private fun bounded(): PlanTransform = copy(
        panX = panX.coerceIn(-max(0f, (width - viewportWidth) / 2), max(0f, (width - viewportWidth) / 2)),
        panY = panY.coerceIn(-max(0f, (height - viewportHeight) / 2), max(0f, (height - viewportHeight) / 2))
    )
}
