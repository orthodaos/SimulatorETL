package com.testing

import akka.actor.{ActorSystem, Props}
import akka.event.Logging
import com.testing.execution.actors.TaskExecutionDispatcher
import com.testing.execution.actors.TaskExecutionDispatcher.StartExecutingTaskGraph
import com.typesafe.config.ConfigFactory

/**
  * @author Roman Maksyutov
  */


/**
  * Main object which load config and startup application
  */
object Main extends App {

	// Create the 'EtlEmulator' actor system
	val system = ActorSystem("SimulatorETL", ConfigFactory.load())

	val log = Logging(system, toString)
	log.info("Config loaded")

	//	private val graph: TaskExecutionGraph = new TaskExecutionGraph(system, config)
	val dispatcher = system.actorOf(Props[TaskExecutionDispatcher], "dispatcher")
	log.debug("create [TaskExecutionDispatcher] and going to start graph")

	dispatcher ! StartExecutingTaskGraph
}
