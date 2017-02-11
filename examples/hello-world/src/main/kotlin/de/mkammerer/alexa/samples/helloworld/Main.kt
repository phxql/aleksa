package de.mkammerer.alexa.samples.helloworld

import de.mkammerer.aleksa.Aleksa

// Start with --help to see available commandline options
fun main(args: Array<String>) {
    // Create your speechlet
    val speechlet = HelloWorldSpeechlet()
    // Add the speechlet to Aleksa
    Aleksa.addSpeechlet(path = "/helloworld", applicationId = "[Your skill id]", speechlet = speechlet)
    // Start Aleksa with the commandline parameters of your application
    Aleksa.start(args)
}