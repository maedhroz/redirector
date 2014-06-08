package com.steelhouse.honeycomb.redirector

import java.net.InetSocketAddress

import org.clapper.argot.ArgotConverters.convertInt
import org.clapper.argot.ArgotConverters.convertString
import org.clapper.argot.ArgotParser

import com.twitter.finagle.builder.ServerBuilder
import com.twitter.finagle.http.Http
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.RichHttp

object RedirectorLauncher {
  
  val DEFAULT_PORT = 10003

  def main(args: Array[String]) {
    val parser = new ArgotParser("redirector", preUsage=Some("Version 0.1.5.0"))
    val port = parser.option[Int](List("p", "port"), "port", "The port to which the server should bind. Default: " + DEFAULT_PORT)
    val evironment = parser.option[String](List("e", "environment"), "environment", "The name of the environment from which to pull configuration.  Default: dev")
    parser.parse(args)
    
    Environment.load("env/" + evironment.value.getOrElse("dev") + "/env.properties")

    val reporter = new ReportingProxy(Environment.getProperty("reporting.host"),
                                      Environment.getProperty("reporting.connection.limit").toInt,
                                      Environment.getProperty("reporting.connection.timeout").toInt)
    
    val identifier = new RobotIdentifier(Environment.getProperty("file.bots"))
    
    val redirect = new RedirectorService(Environment.getProperty("cassandra.cluster.hosts"),
                                         Environment.getProperty("fail.url"),
                                         reporter, identifier,
                                         Environment.getProperty("site.url"))

    val server = ServerBuilder().codec(RichHttp[Request](Http()))
    							.bindTo(new InetSocketAddress(port.value.getOrElse(DEFAULT_PORT)))
    							.name("redirector")
    							.build(redirect)
    							
    println("Redirector listening on port " + port.value.getOrElse(DEFAULT_PORT) + "...")
  }
}