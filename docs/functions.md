# Aleksa helper functions

## Speech

```kotlin
val speech: OutputSpeech = outputSpeech("Hello world")
```

Creates an output speech from a given string. Automatically detects
SSML and creates a `SsmlOutputSpeech` if necessary. SSML must start with
`<speak>`

```kotlin
val speech: PlainTextOutputSpeech = plaintext("Hello world")
```

Creates a plaintext output speech from a given string.

```kotlin
val speech: SsmlOutputSpeech = ssml("<speak>Hello world</speak>")
```

Creates a SSML output speech from a given string.

## Intents

```kotlin
// intent is read from the request
val value: String? = slotValue(intent, "Name")
```

Reads the slot `Name` from the intent. Returns `null` if either the slot or the slot value is missing.

## Responses

```kotlin
val speech: OutputSpeech = outputSpeech("What's it gonna be?")
val reprompt: Reprompt = reprompt(speech)
```

Creates a reprompt from the given speech.

```kotlin
val response: SpeechletResponse  = tell("Hello world")
```

Creates a tell response from the given string. You can use SSML in the string,
just start it with `<speak>`.

```kotlin
val response: SpeechletResponse = ask("What's your name?")
```

Creates an ask response from the given string. You can use SSML in the string,
just start it with `<speak>`. You can optionally provide a reprompt:

```kotlin
val response: SpeechletResponse = ask("What's your name?", "Don't be shy, what's your name?")
```

By default the reprompt is the same as the question.

## Session

```kotlin
// Request is the request from the speechlet
val value: String? = getSessionString(request, "name")
```

Reads the value with the key `name` from the session. Returns null if the key
 doesn't exist or the value isn't a string.

```kotlin
// Request is the request from the speechlet
putSessionString(request, "name", "value")
```

Stores the attribute with key `name` and the value `value` in the session.

```kotlin
// Request is the request from the speechlet
removeSessionAttribute(request, "name")
```

Removes the attribute with the key `name` from the session.

## Miscellaneous

```kotlin
val telephoneSSML: String = telephoneNumber("030 22732152")
```

Returns the SSML for a digit-spoken telephone number with reasonable pauses 
between them. Filters all non-digit characters from the given string. The
SSML doesn't include the `<speak>` tags.