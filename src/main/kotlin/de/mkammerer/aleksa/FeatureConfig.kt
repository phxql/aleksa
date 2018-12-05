package de.mkammerer.aleksa

/**
 * Enables additional features of Aleksa.
 *
 * - [metrics]: True if metrics should be collected and made available under /metrics/prometheus
 */
data class FeatureConfig(
        val metrics: Boolean
)