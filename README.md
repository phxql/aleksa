# Aleksa
Aleksa is a small framework for writing Alexa Skills in Kotlin.

## Usage

### Maven

```xml
<dependency>
    <groupId>de.mkammerer.aleksa</groupId>
    <artifactId>aleksa</artifactId>
    <version>1.0</version>
</dependency>
```

### Gradle

```
compile group: 'de.mkammerer.aleksa', name: 'aleksa', version: '1.0'
```

## Features

* Embedded Jetty server
* Configurable via code or commandline flags
* Supports hosting multiple skills in one application
* Convenience functions for plaintext responses, SSML, repromts, slots, sessions and more

## Example

Speechlet implementation:

```kotlin
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
```

Application:

```kotlin
// Start with --help to see available commandline options
fun main(args: Array<String>) {
    // Create your speechlet
    val speechlet = HelloWorldSpeechlet()
    // Add the speechlet to Aleksa
    Aleksa.addSpeechlet(path = "/helloworld", applicationId = "[Your skill id]", speechlet = speechlet)
    // Start Aleksa with the commandline parameters of your application
    Aleksa.start(args)
}
```

For more examples see the [examples](examples) directory.

## License

[LGPLv3](LICENSE)

## Maintainer

Moritz Kammerer ([phXql](https://github.com/phxql))