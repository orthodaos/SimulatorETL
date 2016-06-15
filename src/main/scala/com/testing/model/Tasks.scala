package com.testing.model

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.event.Logging
import com.testing.model.TaskType.TaskType
import com.testing.utils.ImplicitUtils
import com.typesafe.config.{Config, ConfigObject}


/**
  * @author Roman Maksyutov
  */

/**
  * Model for task
  * @param name
  * @param taskType
  * @param data
  */
case class Task(name: String, taskType: TaskType, data: TaskData) {

	override def toString: String = s"Task('$name')"
}

object Task extends ImplicitUtils {

	def apply(confValue: Any)(implicit defaultExecutionTime: Long): Task = confValue match {
		case obj: ConfigObject =>
			Task((obj("name"), obj("type"), obj("data")))
		case (name: String, taskType: String, data: ConfigObject) =>
			Task(name, TaskType.valueOf(taskType), TaskData(data))
		case x => throw new IllegalArgumentException(s"Wrong arguments for creating Task $x")
	}
}

/**
  * Data of the task which needed for execution
  * @param executionMessage
  * @param executionTime time which task will be executing
  * @param errorMessage if not None will throw Exception with message from option
  */
case class TaskData(executionMessage: String, executionTime: Long, errorMessage: Option[String])

object TaskData extends ImplicitUtils {

	def apply(confValue: Any)(implicit defaultExecutionTime: Long): TaskData = confValue match {
		case obj: ConfigObject =>
			TaskData((obj("execution-message"),
					obj.getTime("execution-time"),
					obj.option[String]("error-message")))
		case ((executionMessage: String, executionTime: Long, errorMessage: Option[String@unchecked])) =>
			TaskData(executionMessage, executionTime, errorMessage)
	}
}

/**
  * Type of the task
  */
object TaskType extends Enumeration {
	type TaskType = Value

	val Extracting = Value("extracting")
	val Transforming = Value("transforming")
	val Loading = Value("loading")

	def valueOf(name: String): TaskType = {
		val lowCaseName = name.toLowerCase
		values.find(_.toString == lowCaseName).getOrElse(
			throw new IllegalArgumentException(s"Unknown type of task: $name")
		)
	}
}

/**
  * Wrapper for the task with all dependencies from other the tasks
  * @param task
  * @param parents
  * @param children
  */
case class TaskDepends(val task: Task, val parents: List[Task], val children: List[Task]) {

	def addParent(parent: Task) = TaskDepends(task, parents :+ parent, children)

	def addParents: List[Task] => TaskDepends = {
		case Nil => this
		case parents: List[Task] => TaskDepends(task, parents ::: this.parents, children)
	}

	def addChild(child: Task) = TaskDepends(task, parents, children :+ child)

	override def toString: String =
		s"""
		   |TaskDepends('${task.name}'): {
		   |             parents: [${parents.map(task => s"'${task.name}'").mkString(", ")}]
		   |             children: [${children.map(task => s"'${task.name}'").mkString(", ")}]
		   |}
			""".stripMargin
}

/**
  * The main class which building and storing all of the tasks with their dependencies.
  * With the help of which the dispatcher determines the sequence of tasks.
  *
  * Information for building will taken from loaded configuration ('config/application.conf')
  *
  * @param system
  * @param config
  */
class TaskExecutionGraph(system: ActorSystem, config: Config) extends ImplicitUtils {

	type TaskCache = Map[Task, TaskDepends]

	import scala.collection.JavaConversions._

	val log = Logging(system, toString)

	private[model] val taskDependsCache = createTaskDependsCache()
	log.debug(s"built taskDependsCache: $taskDependsCache")


	private def createTaskDependsCache(): TaskCache = {

		implicit val defaultExecutionTime: Long = config.getDuration("app.default-execution-time", TimeUnit.MILLISECONDS)

		val tasksObject = config.getObject("app.task-map")

		val taskMap: Map[String, Task] = tasksObject.entrySet().map { e =>
			(e.getKey, Task(e.getValue))
		}.toMap

		val dependsList = taskMap.map { case (key, task) =>
			TaskDepends(task, Nil, tasksObject.stringList(s"$key.depends").map(childKey => {
				taskMap.get(childKey).get
			}).toList)
		}

		dependsList.foldLeft(dependsList.map(depends => (depends.task, depends)).toMap) { case (cache, depends) =>
			depends.children.foldLeft(cache) { case (cache, child) =>
				cache + (child -> cache.get(child).get.addParent(depends.task))
			}
		}
	}

	/**
	  * @param executedTasks
	  * @return true if all tasks were completed
	  */
	def isAllTasksCompleted(executedTasks: Set[Task]): Boolean =
		taskDependsCache.keySet.forall(executedTasks.contains(_))

	/**
	  * @param task
	  * @param executedTasks
	  * @return list of task which can be executing after that task that can be executed simultaneously
	  */
	def nextTasks(task: Task, executedTasks: Set[Task]): List[Task] =
		task.parents.filter(parent =>
			parent.children.forall(executedTasks.contains(_))
		)

	/**
	  * @return list of the tasks which can that can be executed simultaneously on the start
	  */
	def firstTasks(): List[Task] = taskDependsCache.foldLeft(List[Task]()) { case (list, (task, depends)) =>
		if (depends.children.isEmpty) task :: list else list
	}

	implicit class TaskExt(task: Task) {

		def parents = taskDependsCache.get(task).get.parents

		def children = taskDependsCache.get(task).get.children
	}

}





