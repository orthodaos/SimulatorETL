package com.testing.execution.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.testing.execution.TaskExecutor
import com.testing.execution.actors.TaskExecutionDispatcher.{StartExecutingTaskGraph, TaskCompleted, Terminate}
import com.testing.execution.actors.TaskExecutionWorker.StartTask
import com.testing.model.{Task, TaskExecutionGraph}


/**
  * @author Roman Maksyutov
  */

object TaskExecutionDispatcher {

	/**
	  * Worker send to dipatcher when finish executing task
	  * @param task
	  */
	case class TaskCompleted(task: Task)

	/**
	  * Main object send to dispatcher for starting executing tasks
	  */
	case object StartExecutingTaskGraph

	/**
	  * dispatcher send itself when all tasks are completed to terminate application
	  * @param msg
	  */
	case class Terminate(msg: String)

}

/**
  * Dispatcher which create and start workers for task execution
  */
class TaskExecutionDispatcher extends Actor with ActorLogging {

	import akka.actor.OneForOneStrategy
	import akka.actor.SupervisorStrategy._

	override val supervisorStrategy = {
		def stoppingDecider: Decider = {
			case _: Exception => Escalate
		}
		OneForOneStrategy()(stoppingDecider)
	}

	@scala.throws[Exception](classOf[Exception])
	override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
		super.preRestart(reason, message)
		val cause: Any = message.getOrElse(reason)
		log.error(s"Error occurred while execution the task and system will terminated: $cause")
		terminateSystem
	}

	private[actors] var workerNumber: Int = 0
	private[actors] var executedTasks: Set[Task] = Set()
	private[actors] var availableWorkers: List[ActorRef] = Nil

	private[actors] val graph: TaskExecutionGraph = new TaskExecutionGraph(context.system, context.system.settings.config)

	def receive = {
		case StartExecutingTaskGraph => {
			log.debug(s"Received [StartExecutingTaskGraph] message and going to execute task's graph: '${graph}'")
			graph.firstTasks().map(task =>
				createTaskWorker ! StartTask(task)
			)
		}
		case TaskCompleted(task) => {
			log.debug(s"Received [TaskCompleted] message for task: '${task.name}'")
			onTaskCompleted(task)
		}
		case Terminate(msg) =>
			log.debug("Received [Terminate] and going to terminate system")
			log.info(msg)
			terminateSystem
	}

	def onTaskCompleted(task: Task): Unit = {
		executedTasks += task
		availableWorkers = sender() :: availableWorkers
		graph.nextTasks(task, executedTasks) match {
			case Nil => if (graph.isAllTasksCompleted(executedTasks))
				self ! Terminate("All tasks completed and system will be terminated")
			case tasks => tasks.map(task =>
				nextTaskWorker ! StartTask(task))
		}
	}

	def terminateSystem {
		context.system.terminate()
	}

	def createTaskWorker = {
		val name: String = nextWorkerName
		log.debug(s"Created worker: $name")
		context.actorOf(Props[TaskExecutionWorker], name)
	}

	/**
	  * create new or take available worker
	  * @return
	  */
	def nextTaskWorker = availableWorkers match {
		case Nil =>
			createTaskWorker
		case worker :: workers => {
			availableWorkers = workers
			worker
		}
	}

	def nextWorkerName = {
		workerNumber += 1
		s"worker-$workerNumber"
	}
}

object TaskExecutionWorker {

	case class StartTask(task: Task)

}

/**
  * Worker which create according type executor for task and start it
  */
class TaskExecutionWorker extends Actor with ActorLogging {

	override def unhandled(message: Any): Unit = super.unhandled(message)

	@scala.throws[Exception](classOf[Exception])
	override def postStop(): Unit = {
		log.info(s"Stop worker: ${self.path}")
	}

	def receive = {
		case StartTask(task) => {
			log.debug(s"Received [StartTask] message and going to execute task: '${task.name}'")
			TaskExecutor(context.system, task).execute
			sender ! TaskCompleted(task)
		}
	}

}

