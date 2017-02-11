package de.mkammerer.alexa.samples.helloworld

import com.amazon.speech.json.SpeechletRequestEnvelope
import com.amazon.speech.speechlet.IntentRequest
import com.amazon.speech.speechlet.LaunchRequest
import com.amazon.speech.speechlet.SpeechletResponse
import de.mkammerer.aleksa.BuiltInIntents
import de.mkammerer.aleksa.SpeechletV2Base
import de.mkammerer.aleksa.ask
import de.mkammerer.aleksa.tell

// Inherit from SpeechletV2Base, it implements SpeechletV2 and implements optional methods with empty bodies
class HelloWorldSpeechlet : SpeechletV2Base() {
    override fun onIntent(requestEnvelope: SpeechletRequestEnvelope<IntentRequest>): SpeechletResponse {
        val intent = requestEnvelope.request.intent

        return when (intent.name) {
        // use the tell function to create a tell response
            "HelloWorldIntent" -> tell("Hello world")
        // The BuiltInIntents object contains the Alexa built-in intents
            BuiltInIntents.CANCEL, BuiltInIntents.STOP -> tell("Good bye")
        // use the ask function to create an ask response
            else -> ask("What do you want to do?")
        }
    }

    override fun onLaunch(requestEnvelope: SpeechletRequestEnvelope<LaunchRequest>): SpeechletResponse {
        return ask("Hello world. What do you want to do?")
    }
}