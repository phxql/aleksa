package de.mkammerer.aleksa

import com.amazon.speech.Sdk
import com.amazon.speech.speechlet.SpeechletV2
import com.amazon.speech.speechlet.servlet.SpeechletServlet
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

/**
 * Aleksa.
 */
object Aleksa {
    private val logger = LoggerFactory.getLogger(Aleksa::class.java)

    private var server: Server? = null
    private val speechletRegistrations = mutableListOf<SpeechletRegistration>()

    /**
     * Default interface to bind to.
     */
    const val DEFAULT_INTERFACE = "0.0.0.0"
    /**
     * Default port to bind to.
     */
    const val DEFAULT_PORT = 8080
    /**
     * Default value of dev mode.
     */
    const val DEFAULT_DEV = false

    private val options = Options()

    init {
        val options = Options()
        options.addOption("i", "interface", true, "Interface to bind to")
        options.addOption("p", "port", true, "Port to bind to")
        options.addOption("d", "dev", false, "Enable development mode")
        options.addOption("help", "help", false, "Prints help")
    }

    /**
     * Starts Aleksa with the given [commandline arguments][args].
     *
     * Run with --help to get a list of available commandline options.
     */
    fun start(args: Array<String>) {
        val cli = DefaultParser().parse(options, args)
        val theInterface = cli.getOptionValue("interface", DEFAULT_INTERFACE)
        val port = cli.getOptionValue("port", DEFAULT_PORT.toString()).toInt()
        val dev = cli.hasOption("dev")
        val help = cli.hasOption("help")

        if (help) {
            printHelp()
        } else {
            start(theInterface, port, dev)
        }
    }

    /**
     * Starts Aleksa.
     *
     * [theInterface] sets the network interface to bind to (use 0.0.0.0 for all interfaces), [port] sets the
     * port to bind to. Enabling [dev] mode will disable request signature checking, timestamp checking and
     * application id verification.
     */
    fun start(theInterface: String = DEFAULT_INTERFACE, port: Int = DEFAULT_PORT, dev: Boolean = DEFAULT_DEV) {
        run(theInterface, port, dev)
    }

    /**
     * Adds a new [speechlet] which serves the skill with the given [applicationId] under the given [path].
     */
    fun addSpeechlet(path: String, applicationId: String, speechlet: SpeechletV2) {
        speechletRegistrations.add(SpeechletRegistration(path, applicationId, speechlet))
    }

    /**
     * Blocks the current thread until [stop] is called.
     */
    fun join() {
        server?.join()
    }

    /**
     * Stops Aleksa.
     */
    fun stop() {
        val server = this.server ?: return
        server.stop()
        logger.info("Stopped")
    }

    /**
     * Prints the commandline help to stdout.
     */
    fun printHelp() {
        val formatter = HelpFormatter()
        formatter.printHelp("aleksa", options)
    }

    private fun run(theInterface: String, port: Int, dev: Boolean) {
        if (speechletRegistrations.isEmpty()) throw IllegalStateException("No speechlets registered. Use the addSpeechlet method to register at least one speechlet")
        if (server != null) throw IllegalStateException("Already running")

        setProperties(dev)

        val address = InetSocketAddress(theInterface, port)
        val server = Server(address)

        val servletHandler = ServletHandler()
        for (speechletRegistration in speechletRegistrations) {
            logger.info("Registering {} on {}", speechletRegistration.speechlet, speechletRegistration.path)
            val speechletServlet = SpeechletServlet()
            speechletServlet.setSpeechlet(speechletRegistration.speechlet)
            servletHandler.addServletWithMapping(ServletHolder(speechletServlet), speechletRegistration.path)
        }

        if (dev) {
            val hasRoot = speechletRegistrations.any { it.path == "/" }
            if (!hasRoot) {
                logger.debug("Installing root servlet")
                servletHandler.addServletWithMapping(ServletHolder(RootServlet), "/")
            }
        }

        server.handler = servletHandler

        if (dev) logger.info("DEV mode active")
        server.start()
        this.server = server
        logger.info("Running on {}:{}", theInterface, port)
    }

    /**
     * Sets the System properties for the [SpeechletServlet].
     *
     * If running in [dev] mode, timestamp tolerance, application id and request signature checking is disabled.
     */
    private fun setProperties(dev: Boolean) {
        if (dev) {
            System.setProperty(Sdk.DISABLE_REQUEST_SIGNATURE_CHECK_SYSTEM_PROPERTY, "true")
            System.setProperty(Sdk.TIMESTAMP_TOLERANCE_SYSTEM_PROPERTY, "")
            System.setProperty(Sdk.SUPPORTED_APPLICATION_IDS_SYSTEM_PROPERTY, "")
        } else {
            val applicationIds = speechletRegistrations.map { it.applicationId }.joinToString(",")

            System.setProperty(Sdk.DISABLE_REQUEST_SIGNATURE_CHECK_SYSTEM_PROPERTY, "false")
            System.setProperty(Sdk.TIMESTAMP_TOLERANCE_SYSTEM_PROPERTY, "150")
            System.setProperty(Sdk.SUPPORTED_APPLICATION_IDS_SYSTEM_PROPERTY, applicationIds)
        }
    }

    private data class SpeechletRegistration(val path: String, val applicationId: String, val speechlet: SpeechletV2)
}