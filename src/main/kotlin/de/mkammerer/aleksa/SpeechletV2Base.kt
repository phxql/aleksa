package de.mkammerer.aleksa

import com.amazon.speech.json.SpeechletRequestEnvelope
import com.amazon.speech.speechlet.*

/**
 * Abstract class which a speechlet can use.
 *
 * Implenents [onSessionEnded] and [onSessionStarted] with empty methods.
 */
abstract class SpeechletV2Base : SpeechletV2 {
    override fun onSessionEnded(requestEnvelope: SpeechletRequestEnvelope<SessionEndedRequest>) {
    }

    override fun onSessionStarted(requestEnvelope: SpeechletRequestEnvelope<SessionStartedRequest>) {
    }

    abstract override fun onIntent(requestEnvelope: SpeechletRequestEnvelope<IntentRequest>): SpeechletResponse

    abstract override fun onLaunch(requestEnvelope: SpeechletRequestEnvelope<LaunchRequest>): SpeechletResponse
}