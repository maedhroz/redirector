package com.steelhouse.honeycomb.redirector

import java.util.Properties
import java.io.FileReader
import com.twitter.logging.config.LoggerConfig
import com.twitter.logging.config.FileHandlerConfig
import com.twitter.logging.Logger
import com.twitter.logging.Policy
import com.twitter.logging.LoggerFactory

object Environment {
  
  val logLevelMappings = Map("DEBUG" -> Logger.DEBUG, "INFO" -> Logger.INFO, "WARNING" -> Logger.WARNING, "ERROR" -> Logger.ERROR)

  val properties = new Properties

  def load(filename: String) {
    val reader = new FileReader(filename)
    properties.load(reader)
    
    val loggingLevel = properties.getProperty("logging.level")
    
    val cassieLoggerHandler =
      new FileHandlerConfig { 
        filename = "/var/log/honeycomb/redirector/cassie.log"
    	roll = Policy.SigHup
      }
      
    val cassieLoggerFactory = new LoggerFactory ("com.twitter.cassie", logLevelMappings.get(loggingLevel), List(cassieLoggerHandler))()
    
    val redirectorLoggerHandler =
      new FileHandlerConfig { 
        filename = "/var/log/honeycomb/redirector/redirector.log"
    	roll = Policy.SigHup
      }
      
    val redirectorLoggerFactory = new LoggerFactory ("com.steelhouse.honeycomb.redirector", logLevelMappings.get(loggingLevel), List(redirectorLoggerHandler))()
  }

  def getProperty(name: String) = properties.getProperty(name)
}