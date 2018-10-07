package de.mkammerer.aleksa.metrics

import com.amazon.speech.json.SpeechletRequestEnvelope
import com.amazon.speech.speechlet.*
import com.codahale.metrics.MetricRegistry

/**
 * Decorator for a [SpeechletV2] which collects metrics.
 */
class MetricsSpeechletV2(
        private val delegate: SpeechletV2,
        private val metricRegistry: MetricRegistry
) : SpeechletV2 {
    private val sessionsStarted = metricRegistry.timer("sessions-started")
    private val sessionsEnded = metricRegistry.timer("sessions-ended")
    private val totalIntentsHandled = metricRegistry.timer("total-intents-handled")
    private val launches = metricRegistry.timer("launches")

    override fun onSessionStarted(requestEnvelope: SpeechletRequestEnvelope<SessionStartedRequest>) {
        sessionsStarted.time {
            delegate.onSessionStarted(requestEnvelope)
        }
    }

    override fun onSessionEnded(requestEnvelope: SpeechletRequestEnvelope<SessionEndedRequest>) {
        sessionsEnded.time {
            delegate.onSessionEnded(requestEnvelope)
        }
    }

    override fun onIntent(requestEnvelope: SpeechletRequestEnvelope<IntentRequest>): SpeechletResponse {
        return totalIntentsHandled.timeSupplier {
            val intent = requestEnvelope.request.intent.name
            metricRegistry.timer("intent-handled:$intent").timeSupplier {
                delegate.onIntent(requestEnvelope)
            }
        }
    }

    override fun onLaunch(requestEnvelope: SpeechletRequestEnvelope<LaunchRequest>): SpeechletResponse {
        return launches.timeSupplier {
            delegate.onLaunch(requestEnvelope)
        }
    }
}