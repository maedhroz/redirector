package com.steelhouse.honeycomb.redirector

import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.http.Http
import com.twitter.util.Duration
import java.util.concurrent.TimeUnit
import com.codahale.jerkson.Json.parse
import org.jboss.netty.util.CharsetUtil.UTF_8
import com.twitter.logging.Logger

class ReportingProxy(reportingHost: String, limit: Int, timeout: Int) {
  
  val logger = Logger.get(this.getClass)

  val ERROR_KEY = "error"
    
  val client = ClientBuilder().codec(Http()).hosts(reportingHost).hostConnectionLimit(limit).connectionTimeout(Duration(timeout, TimeUnit.SECONDS)).build()
  Runtime.getRuntime.addShutdownHook(new Thread { override def run = client.release() })  
    
  def countRedirect(source: String) {
    val builder = new HttpPostRequestBuilder(reportingHost, "/redirect")
    
    builder.param("source", source)

    // Note: This is the current time, not the time the tag was made.
    builder.param("epoch", System.currentTimeMillis().toString())

    client(builder.build()) onSuccess { response =>
      val json = parse[Map[String, String]](response.getContent().toString(UTF_8))
      if (json.contains(ERROR_KEY)) logger.error(json(ERROR_KEY))
    } onFailure { error =>
      logger.error(error, error.getMessage())
    }
  }
}