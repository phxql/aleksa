# Aleksa
Aleksa is a small framework for writing [Alexa Skills](https://developer.amazon.com/alexa-skills-kit) in Kotlin.

## Warning

**This framework uses an old version of the Alexa SDK - skills built with it won't work with Alexa anymore.**

## Usage

### Maven

```xml
<dependency>
    <groupId>de.mkammerer.aleksa</groupId>
    <artifactId>aleksa</artifactId>
    <version>1.2</version>
</dependency>
```

### Gradle

```
compile group: 'de.mkammerer.aleksa', name: 'aleksa', version: '1.2'
```

## Features

* Embedded Jetty server
* Configurable via code or commandline flags
* Supports hosting multiple skills in one application
* Convenience functions for plaintext responses, SSML, repromts, slots, sessions and more
* Dev mode which simplifies skill testing while development
* TLS
* Metrics

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

Run it with `--interface 127.0.0.1 --port 8080 --dev`. Now you can test the
skill with curl or some other tool at the url `http://127.0.0.1:8080/helloworld`.

If you don't specify any commandline arguments, it binds to all interfaces on port 8080 and without dev mode. 
The dev mode disables request signature checking, timestamp checking and application id verification. It also shows some
information on `/` to ease debugging infrastructure problems (reverse proxies, etc.).

If you want metrics (statistics on how often your skills are executed), add `--metrics` and check the `/metrics` endpoint. 

For more examples see the [examples](examples) directory.

## Commandline parameters

```
 -d,--dev                          Enable development mode
 -h,--help                         Prints help
 -i,--interface <arg>              Interface to bind to
 -ka,--key-alias <arg>             Key alias. If not set, a key will be
                                   automatically selected
 -kpw,--key-password <arg>         Key password. If not set, the keystore
                                   password will be used
 -ks,--keystore <arg>              Location to the keystore
 -kspw,--keystore-password <arg>   Keystore password
 -m,--metrics                      Enable metrics
 -p,--port <arg>                   Port to bind to
```

## Documentation

* [Functions](docs/functions.md)
* [Constants](docs/constants.md)

## License

[LGPLv3](LICENSE)

## Contributing

See [contributing guidelines](docs/contributing.md).

## Maintainer

Moritz Kammerer ([phXql](https://github.com/phxql))