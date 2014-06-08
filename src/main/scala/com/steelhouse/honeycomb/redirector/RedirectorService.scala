package com.steelhouse.honeycomb.redirector

import scala.reflect.BeanProperty
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import org.jboss.netty.handler.codec.http.HttpMethod._
import org.jboss.netty.util.CharsetUtil.UTF_8
import com.codahale.jerkson.Json.parse
import com.twitter.cassie.codecs.LexicalUUIDCodec
import com.twitter.cassie.codecs.Utf8Codec
import com.twitter.cassie.types.String2LexicalUUID
import com.twitter.cassie.Cluster
import com.twitter.cassie.ReadConsistency
import com.twitter.finagle.http.Status._
import com.twitter.finagle.http.Version._
import com.twitter.finagle.http.path._
import com.twitter.finagle.http._
import com.twitter.finagle.Service
import com.twitter.util.Future
import com.twitter.cassie.Column
import com.twitter.finagle.builder.ClientBuilder
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.DefaultHttpRequest
import org.jboss.netty.util.CharsetUtil
import com.twitter.util.Duration
import java.util.concurrent.TimeUnit
import java.net.URLEncoder
import java.net.URLEncoder.encode
import com.twitter.logging.Logger

class RedirectorService(cassandraHosts: String, failURL: String, reporter: ReportingProxy, botIdentifier: RobotIdentifier, siteURL: String) extends Service[Request, Response] {

  val logger = Logger.get(this.getClass)

  val cluster = new Cluster(cassandraHosts)
  val keyspace = cluster.keyspace(Environment.getProperty("keyspace.name")).connect()
  Runtime.getRuntime.addShutdownHook(new Thread { override def run = keyspace.close() })

  val tagsCF = keyspace.columnFamily("Tags", LexicalUUIDCodec, Utf8Codec, Utf8Codec).consistency(ReadConsistency.One)
  val linksCF = keyspace.columnFamily("Links", Utf8Codec, Utf8Codec, Utf8Codec).consistency(ReadConsistency.One)

  def apply(request: Request) = {
    try {
      request.method -> Path(request.path) match {
      	case GET -> Root => {
          Future.value(seeOtherResponse(siteURL))
        }  
      	case GET -> Root / "ping" => {
          val response = Response()
          response.content = copiedBuffer("pong", UTF_8)
          Future.value(response)
        }
        case GET -> Root / "tags" / id => {
          tagsCF.getColumn(id, "json") flatMap { column => tagRedirectResponse(request, column) }
        }
        case GET -> Root / id => {
          linksCF.getRow(id) flatMap { row => shortRedirectResponse(request, row) }
        }
        case _ => {
          logger.warning("There is no resource at " + request.path)
          Future.value(seeOtherResponse(failURL))
        }
      }
    } catch {
      case e: Exception => {
        logger.error(e.toString())
        Future.value(seeOtherResponse(failURL))
      }
    }
  }

  private[this] def shortRedirectResponse(request: Request, row: java.util.Map[String, Column[String, String]]): Future[Response] = {
    if (row.isEmpty) {
      logger.info("Trying to access a non-existent tag at " + request.path)
      Future.value(seeOtherResponse(failURL))
    } else { 
      reportRedirect(request, row.get("source").value)
      val url = row.get("url").value
      Future.value(seeOtherResponse(url))
    }
  }

  private[this] def tagRedirectResponse(request: Request, column: Option[Column[String, String]]): Future[Response] = {
    if (column.isDefined) {
      reportRedirect(request, request.getParam("source"))
      val tag = parse[Map[String, Any]](column.get.value)
      val pageURL = tag("page_url").asInstanceOf[String]
      Future.value(seeOtherResponse(pageURL))
    } else {
      logger.warning("Trying to access a non-existent tag at " + request.path)
      Future.value(seeOtherResponse(failURL))
    }
  }

  private[this] def seeOtherResponse(url: String): Response = {
    val response = Response(Http11, SeeOther)
    response.addHeader(HttpHeaders.Names.LOCATION, url)
    response
  }
  
  private[this] def reportRedirect(request: Request, source: String) {
    val agent = request.userAgent

    if (!agent.isEmpty) {
      logger.info("User-Agent: " + agent.get)
      
      if (source == null) {
        logger.warning("Redirect from agent '" + agent.get + "' has no source!")
      } else if (!botIdentifier.isRobot(agent.get)) {
        reporter.countRedirect(source)
      } else {
        // TODO: Remove this when we're satisfied that this works and traffic picks up.
        logger.info(agent.get + " is a bot!")
      }
    }
  }
}