package com.testing.execution

import akka.actor.ActorSystem
import akka.event.Logging
import com.testing.model.{Task, TaskType}

/**
  * @author Roman Maksyutov
  */

private[execution] object TaskExecutor {
	/**
	  * Create executor according task's type
	  * @param system
	  * @param task
	  * @return
	  */
	def apply(system: ActorSystem, task: Task): TaskExecutor = (task.taskType match {
		case TaskType.Extracting => Extractor(system, task)
		case TaskType.Transforming => Transformer(system, task)
		case TaskType.Loading => Loader(system, task)
	})
}

/**
  * Base trait for execution task
  */
trait TaskExecutor {

	import scala.concurrent.duration._

	val system: ActorSystem
	val log = Logging(system, s"$logSource(${task.name})")
	val task: Task

	def action: String

	protected def logSource: String

	def execute {
		var elapsedTime = System.currentTimeMillis
		log.debug(s"Start task '${task.name}'.")

		log.debug(s"$action ${task.data.executionMessage} should take ${task.data.executionTime}.")
		Thread.sleep(task.data.executionTime)
		task.data.errorMessage.foreach { errorMsg =>
			throw new Exception(errorMsg)
		}

		elapsedTime = System.currentTimeMillis - elapsedTime
		log.info(s"Task '${task.name}' completed in ${elapsedTime.millis}.")
	}
}

case class Extractor(system: ActorSystem,
                     task: Task,
                     action: String = "Extracting",
                     logSource: String = "Extractor") extends TaskExecutor

case class Transformer(system: ActorSystem,
                       task: Task,
                       action: String = "Transforming",
                       logSource: String = "Transformer") extends TaskExecutor

case class Loader(system: ActorSystem,
                  task: Task,
                  action: String = "Loading",
                  logSource: String = "Loader") extends TaskExecutor

