package com.steelhouse.honeycomb.redirector

import java.net.URLEncoder.encode

import scala.collection.mutable.ListBuffer

import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import org.jboss.netty.handler.codec.http.HttpMethod.POST
import org.jboss.netty.handler.codec.http.DefaultHttpRequest
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.util.CharsetUtil.UTF_8

import com.twitter.finagle.http.Version.Http11

final class HttpPostRequestBuilder(host: String, path: String) {
  val DEFAULT_ENCODING = UTF_8.toString()

  val request = new DefaultHttpRequest(Http11, POST, path)
  request.setHeader(HttpHeaders.Names.HOST, host)

  var params = new ListBuffer[String]

  def build(): HttpRequest = {
    val buffer = copiedBuffer(params.mkString("&"), UTF_8)
    request.setContent(buffer)
    request.addHeader(HttpHeaders.Names.CONTENT_LENGTH, buffer.readableBytes())
    request
  }

  def param(name: String, value: String) {
    params += name + "=" + encode(value, DEFAULT_ENCODING)
  }
}
