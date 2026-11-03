package com.hawatraders.manager.util

object PriceMath {
    /**
     * Applies a bulk change to [old]: percent or fixed amount, up or down. The result is rounded to
     * whole taka and never drops below 1 so a product can't end up free or negative.
     */
    fun adjust(old: Double, byPercent: Boolean, increase: Boolean, amount: Double): Double {
        val delta = if (byPercent) old * amount / 100.0 else amount
        val raw = if (increase) old + delta else old - delta
        val rounded = Math.round(raw).toDouble()
        return if (rounded < 1.0) 1.0 else rounded
    }
}
