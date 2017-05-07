package de.mkammerer.aleksa

import com.amazon.speech.Sdk
import com.amazon.speech.speechlet.SpeechletV2
import com.amazon.speech.speechlet.servlet.SpeechletServlet
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.server.*
import org.eclipse.jetty.servlet.ServletHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.slf4j.LoggerFactory

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
        options.addOption("i", "interface", true, "Interface to bind to")
        options.addOption("p", "port", true, "Port to bind to")
        options.addOption("d", "dev", false, "Enable development mode")
        options.addOption("ks", "keystore", true, "Location to the keystore")
        options.addOption("kspw", "keystore-password", true, "Keystore password")
        options.addOption("kpw", "key-password", true, "Key password. If not set, the keystore password will be used")
        options.addOption("ka", "key-alias", true, "Key alias. If not set, a key will be automatically selected")
        options.addOption("h", "help", false, "Prints help")
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

        val keystore = cli.getOptionValue("keystore")
        val tlsConfig = if (keystore == null) null else {
            val keystorePassword = cli.getOptionValue("keystore-password") ?: ""
            val keyPassword = cli.getOptionValue("key-password") ?: keystorePassword
            val keyAlias = cli.getOptionValue("key-alias")
            TlsConfig(keystore, keystorePassword, keyPassword, keyAlias)
        }

        if (help) {
            printHelp()
        } else {
            start(theInterface, port, dev, tlsConfig)
        }
    }

    /**
     * Starts Aleksa.
     *
     * [theInterface] sets the network interface to bind to (use 0.0.0.0 for all interfaces), [port] sets the
     * port to bind to. Enabling [dev] mode will disable request signature checking, timestamp checking and
     * application id verification. [tlsConfig] configures TLS. If set to null, no TLS will be used.
     */
    fun start(theInterface: String = DEFAULT_INTERFACE, port: Int = DEFAULT_PORT, dev: Boolean = DEFAULT_DEV, tlsConfig: TlsConfig? = null) {
        run(theInterface, port, dev, tlsConfig)
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
        speechletRegistrations.clear()
        this.server = null
        logger.info("Stopped")
    }

    /**
     * Prints the commandline help to stdout.
     */
    fun printHelp() {
        val formatter = HelpFormatter()
        formatter.printHelp("aleksa", options)
    }

    private fun run(theInterface: String, port: Int, dev: Boolean, tlsConfig: TlsConfig? = null) {
        if (speechletRegistrations.isEmpty()) throw IllegalStateException("No speechlets registered. Use the addSpeechlet method to register at least one speechlet")
        if (server != null) throw IllegalStateException("Already running")

        setProperties(dev)

        val server = Server()

        val connector = if (tlsConfig == null) {
            ServerConnector(server)
        } else {
            configureTls(tlsConfig, server)
        }
        connector.host = theInterface
        connector.port = port
        server.connectors = arrayOf(connector)

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

    private fun configureTls(tlsConfig: TlsConfig, server: Server): ServerConnector {
        val httpConfiguration = HttpConfiguration()
        httpConfiguration.addCustomizer(SecureRequestCustomizer())
        val sslContextFactory = SslContextFactory()
        sslContextFactory.keyStoreResource = Resource.newResource(tlsConfig.keystore)
        sslContextFactory.setKeyStorePassword(tlsConfig.keystorePassword)
        sslContextFactory.setKeyManagerPassword(tlsConfig.keyPassword)
        sslContextFactory.certAlias = tlsConfig.alias

        return ServerConnector(server, SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()), HttpConnectionFactory(httpConfiguration))
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