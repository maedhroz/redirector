package com.steelhouse.honeycomb.redirector
import scala.io.Source
import scala.collection.mutable.HashSet

class RobotIdentifier(botsFile: String) {
  
  var bots = new HashSet[String]
  
  Source.fromFile(botsFile).getLines() foreach { line =>
    bots += line.trim().stripLineEnd
  }

  def isRobot(agent: String): Boolean = {
    bots.contains(agent)
  }
}