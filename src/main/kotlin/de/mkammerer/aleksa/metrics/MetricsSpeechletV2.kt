package de.mkammerer.aleksa.metrics

import com.amazon.speech.json.SpeechletRequestEnvelope
import com.amazon.speech.speechlet.*
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag

/**
 * Decorator for a [SpeechletV2] which collects metrics.
 */
class MetricsSpeechletV2(
        path: String,
        private val delegate: SpeechletV2,
        private val meterRegistry: MeterRegistry
) : SpeechletV2 {
    private val pathTag = Tag.of("path", path)
    private val tags = listOf(pathTag)

    private val sessionsStarted = meterRegistry.timer("aleksa.sessions.started", tags)
    private val sessionsEnded = meterRegistry.timer("aleksa.sessions.ended", tags)
    private val launches = meterRegistry.timer("aleksa.launches", tags)

    override fun onSessionStarted(requestEnvelope: SpeechletRequestEnvelope<SessionStartedRequest>) {
        sessionsStarted.record {
            delegate.onSessionStarted(requestEnvelope)
        }
    }

    override fun onSessionEnded(requestEnvelope: SpeechletRequestEnvelope<SessionEndedRequest>) {
        sessionsEnded.record {
            delegate.onSessionEnded(requestEnvelope)
        }
    }

    override fun onIntent(requestEnvelope: SpeechletRequestEnvelope<IntentRequest>): SpeechletResponse {
        val intent = requestEnvelope.request.intent.name ?: "null"
        return meterRegistry.timer("aleksa.intents.handled", listOf(pathTag, Tag.of("intent", intent))).recordCallable {
            delegate.onIntent(requestEnvelope)
        }
    }

    override fun onLaunch(requestEnvelope: SpeechletRequestEnvelope<LaunchRequest>): SpeechletResponse {
        return launches.recordCallable {
            delegate.onLaunch(requestEnvelope)
        }
    }
}