package de.mkammerer.aleksa.metrics

import io.micrometer.prometheus.PrometheusMeterRegistry
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class MicrometerPrometheusServlet(
        private val meterRegistry: PrometheusMeterRegistry
) : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.writer.write(meterRegistry.scrape())
        resp.writer.flush()
    }
}