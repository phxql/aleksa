package de.mkammerer.aleksa

import com.amazon.speech.json.SpeechletRequestEnvelope
import com.amazon.speech.speechlet.IntentRequest
import com.amazon.speech.speechlet.LaunchRequest
import com.amazon.speech.speechlet.SpeechletResponse
import org.eclipse.jetty.util.resource.Resource
import org.junit.Test

class AleksaTest {
    @Test
    fun test() {
        val tlsConfig = TlsConfig(
                Resource.newClassPathResource("/tls.jks").toString(),
                "keystore-pw", "key-pw",
                "alias-2"
        )

        Aleksa.addSpeechlet("/dummy", "12345", DummySpeechlet)
        Aleksa.start("localhost", 9999, true, tlsConfig)
        Aleksa.stop()
    }

    @Test
    fun testCommandline() {
        Aleksa.addSpeechlet("/dummy", "12345", DummySpeechlet)
        Aleksa.start(arrayOf(
                "--interface", "localhost",
                "--port", "9999",
                "--dev",
                "--keystore", Resource.newClassPathResource("/tls.jks").toString(),
                "--keystore-password", "keystore-pw",
                "--key-password", "key-pw",
                "--key-alias", "alias-2"
        ))
        Aleksa.stop()
    }

    private object DummySpeechlet : SpeechletV2Base() {
        override fun onIntent(requestEnvelope: SpeechletRequestEnvelope<IntentRequest>): SpeechletResponse {
            return tell("Dummy")
        }

        override fun onLaunch(requestEnvelope: SpeechletRequestEnvelope<LaunchRequest>): SpeechletResponse {
            return ask("Dummy")
        }
    }
}