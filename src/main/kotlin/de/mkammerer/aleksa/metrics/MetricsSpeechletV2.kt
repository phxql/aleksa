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
        sessionsStarted.time().use {
            delegate.onSessionStarted(requestEnvelope)
        }
    }

    override fun onSessionEnded(requestEnvelope: SpeechletRequestEnvelope<SessionEndedRequest>) {
        sessionsEnded.time().use {
            delegate.onSessionEnded(requestEnvelope)
        }
    }

    override fun onIntent(requestEnvelope: SpeechletRequestEnvelope<IntentRequest>): SpeechletResponse {
        totalIntentsHandled.time().use {
            val intent = requestEnvelope.request.intent.name
            metricRegistry.timer("intent-handled:$intent").time().use {
                return delegate.onIntent(requestEnvelope)
            }
        }
    }

    override fun onLaunch(requestEnvelope: SpeechletRequestEnvelope<LaunchRequest>): SpeechletResponse {
        launches.time().use {
            return delegate.onLaunch(requestEnvelope)
        }
    }
}