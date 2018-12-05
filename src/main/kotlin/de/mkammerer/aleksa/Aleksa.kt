package de.mkammerer.aleksa

import com.amazon.speech.Sdk
import com.amazon.speech.speechlet.SpeechletV2
import com.amazon.speech.speechlet.servlet.SpeechletServlet
import de.mkammerer.aleksa.metrics.MetricsSpeechletV2
import de.mkammerer.aleksa.metrics.MicrometerPrometheusServlet
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
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

    private val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    private var server: Server? = null
    private val speechletRegistrations = mutableListOf<SpeechletRegistration>()

    /**
     * Default interface to bind to.
     */
    private const val DEFAULT_INTERFACE = "0.0.0.0"
    /**
     * Default port to bind to.
     */
    private const val DEFAULT_PORT = 8080
    /**
     * Default value of dev mode.
     */
    private const val DEFAULT_DEV = false
    /**
     * Root path.
     */
    private const val ROOT_PATH = "/"
    /**
     * Metrics path.
     */
    private const val METRICS_PATH = "/metrics/prometheus"

    private val options = Options()

    init {
        options.addOption("i", "interface", true, "Interface to bind to")
        options.addOption("p", "port", true, "Port to bind to")
        options.addOption("d", "dev", false, "Enable development mode")
        options.addOption("ks", "keystore", true, "Location to the TLS keystore")
        options.addOption("kspw", "keystore-password", true, "TLS Keystore password")
        options.addOption("kpw", "key-password", true, "TLS Key password. If not set, the keystore password will be used")
        options.addOption("ka", "key-alias", true, "TLS Key alias. If not set, a key will be automatically selected")
        options.addOption("m", "metrics", false, "Enable metrics")

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
        val featureConfig = FeatureConfig(
                metrics = cli.hasOption("metrics")
        )

        if (help) {
            printHelp()
        } else {
            start(theInterface, port, dev, tlsConfig, featureConfig)
        }
    }

    /**
     * Starts Aleksa.
     *
     * [theInterface] sets the network interface to bind to (use 0.0.0.0 for all interfaces), [port] sets the
     * port to bind to. Enabling [dev] mode will disable request signature checking, timestamp checking and
     * application id verification. [tlsConfig] configures TLS. If set to null, no TLS will be used.
     * [featureConfig] enables additional features.
     */
    fun start(theInterface: String = DEFAULT_INTERFACE, port: Int = DEFAULT_PORT, dev: Boolean = DEFAULT_DEV, tlsConfig: TlsConfig? = null, featureConfig: FeatureConfig? = null) {
        run(theInterface, port, dev, tlsConfig, featureConfig)
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

    private fun run(theInterface: String, port: Int, dev: Boolean, tlsConfig: TlsConfig? = null, featureConfig: FeatureConfig?) {
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

        installSpeechlets(featureConfig, servletHandler)
        enableDevMode(dev, servletHandler)
        enableMetrics(featureConfig, servletHandler)

        server.handler = servletHandler
        server.start()
        this.server = server

        logger.info("Running on {}:{}", theInterface, port)

    }

    private fun installSpeechlets(featureConfig: FeatureConfig?, servletHandler: ServletHandler) {
        for (speechletRegistration in speechletRegistrations) {
            logger.info("Registering {} on {}", speechletRegistration.speechlet, speechletRegistration.path)
            val speechletServlet = SpeechletServlet()

            val speechlet = addMetricsToSpeechlet(speechletRegistration, featureConfig)
            speechletServlet.setSpeechlet(speechlet)
            servletHandler.addServletWithMapping(ServletHolder(speechletServlet), speechletRegistration.path)
        }
    }

    private fun enableMetrics(featureConfig: FeatureConfig?, servletHandler: ServletHandler) {
        if (areMetricsEnabled(featureConfig)) {
            // Install common metrics
            ClassLoaderMetrics().bindTo(meterRegistry)
            JvmMemoryMetrics().bindTo(meterRegistry)
            JvmGcMetrics().bindTo(meterRegistry)
            ProcessorMetrics().bindTo(meterRegistry)
            JvmThreadMetrics().bindTo(meterRegistry)

            val hasOverriddenMetrics = speechletRegistrations.any { it.path == METRICS_PATH }
            if (hasOverriddenMetrics) {
                logger.warn("Can't add metrics, because a speechlet is running on $METRICS_PATH")
            } else {
                servletHandler.addServletWithMapping(ServletHolder(MicrometerPrometheusServlet(meterRegistry)), METRICS_PATH)
                logger.info("Metrics available on $METRICS_PATH")
            }
        }
    }

    private fun enableDevMode(dev: Boolean, servletHandler: ServletHandler) {
        if (dev) {
            val hasRoot = speechletRegistrations.any { it.path == ROOT_PATH }
            if (!hasRoot) {
                logger.debug("Installing root servlet on $ROOT_PATH")
                servletHandler.addServletWithMapping(ServletHolder(RootServlet), ROOT_PATH)
            }

            logger.info("DEV mode active")
        }
    }

    private fun areMetricsEnabled(featureConfig: FeatureConfig?) = featureConfig?.metrics == true

    /**
     * Add metrics to the speechlet in the given [speechletRegistration], if enabled in the [featureConfig].
     */
    private fun addMetricsToSpeechlet(speechletRegistration: SpeechletRegistration, featureConfig: FeatureConfig?): SpeechletV2 {
        return if (areMetricsEnabled(featureConfig)) {
            MetricsSpeechletV2(speechletRegistration.path, speechletRegistration.speechlet, meterRegistry)
        } else {
            speechletRegistration.speechlet
        }
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