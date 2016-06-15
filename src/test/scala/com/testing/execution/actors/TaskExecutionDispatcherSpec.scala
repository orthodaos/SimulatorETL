package com.testing.execution.actors

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import akka.util.Timeout
import com.testing.BaseSpec
import com.testing.execution.actors.TaskExecutionDispatcher.{StartExecutingTaskGraph, TaskCompleted}
import com.testing.execution.actors.TaskExecutionWorker.StartTask
import com.testing.model.Task
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._
import scala.util.Success

/**
  * @author Roman Maksyutov
  */
class TaskExecutionDispatcherSpec(_system: ActorSystem) extends TestKit(_system)
		with ImplicitSender
		with Matchers
		with FlatSpecLike
		with BeforeAndAfterAll
		with BaseSpec {

	def this() = this(ActorSystem("SimulatorETL", ConfigFactory.load("test-application.conf")))

	override def afterAll: Unit = {
		system.terminate()
	}

	"worker" should "send back message after execution task" in {
		val worker = system.actorOf(Props[TaskExecutionWorker], "worker")
		worker ! StartTask(F)
		expectMsg(TaskCompleted(F))
	}


	"TaskExecutionDispatcher" should "execute correctly all tasks" in {
		val p = TestProbe()
		val dispatcher = TestActorRef(new TaskExecutionDispatcher() {
			var orderedExecutedTasks: List[Task] = List()

			override def onTaskCompleted(task: Task) {
				super.onTaskCompleted(task)
				orderedExecutedTasks = task :: orderedExecutedTasks
			}

			override def terminateSystem {
				p.ref ! Success("Ok")
			}
		})

		implicit val timeout = Timeout(1000.millis)
		dispatcher ? StartExecutingTaskGraph

		p.expectMsg(Success("Ok"))
		dispatcher.underlyingActor.executedTasks should be === taskSet
	}
}
