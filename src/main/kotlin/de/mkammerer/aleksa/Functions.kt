package de.mkammerer.aleksa

import com.amazon.speech.json.SpeechletRequestEnvelope
import com.amazon.speech.slu.Intent
import com.amazon.speech.speechlet.SpeechletResponse
import com.amazon.speech.ui.OutputSpeech
import com.amazon.speech.ui.PlainTextOutputSpeech
import com.amazon.speech.ui.Reprompt
import com.amazon.speech.ui.SsmlOutputSpeech

/**
 * Creates an output speech from the given [text].
 *
 * Automatically detects SSML.
 */
fun outputSpeech(text: String): OutputSpeech {
    return if (isSsml(text)) ssml(text) else plaintext(text)
}

/**
 * Creates plaintext speech from the given [text].
 */
fun plaintext(text: String): PlainTextOutputSpeech {
    val result = PlainTextOutputSpeech()
    result.text = text
    return result
}

/**
 * Creates SSML speech from the given [ssml].
 */
fun ssml(ssml: String): SsmlOutputSpeech {
    val result = SsmlOutputSpeech()
    result.ssml = ssml
    return result
}

/**
 * Creates a reprompt from the given [speech].
 */
fun reprompt(speech: OutputSpeech): Reprompt {
    val reprompt = Reprompt()
    reprompt.outputSpeech = speech
    return reprompt
}

/**
 * Extracts the value of the slot with the given [name][slotName] from the [intent].
 */
fun slotValue(intent: Intent, slotName: String): String? {
    val slot = intent.getSlot(slotName) ?: return null
    return slot.value
}

/**
 * Creates a tell response with the given [text].
 *
 * Automatically detects SSML.
 */
fun tell(text: String): SpeechletResponse {
    return SpeechletResponse.newTellResponse(outputSpeech(text))
}

/**
 * Creates a ask response with the given [question] and a given [reprompt]. If the [reprompt] isn't set, it uses [question] as reprompt.
 *
 * Automatically detects SSML.
 */
fun ask(question: String, reprompt: String = question): SpeechletResponse {
    return SpeechletResponse.newAskResponse(outputSpeech(question), reprompt(outputSpeech(reprompt)))
}

/**
 * Determines if the given [text] is SSML.
 */
private fun isSsml(text: String): Boolean {
    return text.startsWith("<speak>")
}

/**
 * Reads the session value with the given [key] from the request.
 *
 * Returns null if the key doesn't exist or if the value assigned to key isn't a string.
 */
fun getSessionString(request: SpeechletRequestEnvelope<*>, key: String): String? {
    val value = request.session.getAttribute(key) ?: return null
    return value as String
}

/**
 * Puts the given [value] in the session under the given [key].
 *
 * if there is already a value with this [key], the existing value will be overwritten.
 */
fun putSessionString(request: SpeechletRequestEnvelope<*>, key: String, value: String) {
    request.session.setAttribute(key, value)
}

/**
 * Removes the given [key] from the session.
 *
 * If the [key] doesn't exist, nothing will happen.
 */
fun removeSessionAttribute(request: SpeechletRequestEnvelope<*>, key: String) {
    request.session.removeAttribute(key)
}

/**
 * Returns the SSML for a digit-spoken [telephone number][number] with reasonable pauses between them.
 *
 * Filters all non-digit characters from the given string.
 */
fun telephoneNumber(number: String): String {
    return number
            .filter(Char::isDigit)
            .map { digit -> "<say-as interpret-as=\"digits\">$digit</say-as> ${SSML.Breaks.WEAK}" }
            .joinToString(separator = "")
}