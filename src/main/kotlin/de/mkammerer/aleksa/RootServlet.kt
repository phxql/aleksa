package de.mkammerer.aleksa

import java.time.Instant
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Is registered in DEV mode on "/".
 */
object RootServlet : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.writer.write("Aleksa running: ")
        resp.writer.write(Instant.now().toString())
        resp.writer.write("\n")
    }
}