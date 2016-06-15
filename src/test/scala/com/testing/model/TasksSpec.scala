package com.testing.model

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.testing.BaseSpec
import com.typesafe.config.ConfigFactory
import org.scalatest._

/**
  * @author Roman Maksyutov
  */
class TasksSpec(_system: ActorSystem) extends TestKit(_system)
		with ImplicitSender
		with Matchers
		with FlatSpecLike
		with BeforeAndAfterAll
		with BaseSpec {

	def this() = this(ActorSystem("SimulatorETL", ConfigFactory.load("test-application.conf")))

	override def afterAll: Unit = {
		system.terminate()
	}

	val graph = new TaskExecutionGraph(system, system.settings.config)

	"TaskExecutionGraph" should "should correctly building tasks" in {
		val actual = graph.taskDependsCache.keySet
		val expected = correctDependsCache.keySet
		actual should be === expected
		actual should be === taskSet
	}

	it should "should correctly building dependencies for the tasks" in {
		val actual = graph.taskDependsCache.values.toSet
		val expected = correctDependsCache.values.toSet
		actual should be === expected
		graph.taskDependsCache should be === correctDependsCache
	}

	it should "correctly define first tasks to execution" in {
		graph.firstTasks().toSet should be === Set(E, F)
	}

	it should "correctly define next tasks to execution" in {
		graph.nextTasks(E, Set(E)) shouldBe empty
		graph.nextTasks(F, Set(E, F)).toSet should be === Set(G)
		graph.nextTasks(G, Set(G, E, F)).toSet should be === Set(J, M)
		graph.nextTasks(J, Set(J, G, E, F)).toSet should be === Set(H)
		graph.nextTasks(M, Set(M, J, G, E, F)).toSet should be === Set(R)
		graph.nextTasks(R, Set(R, M, J, G, E, F)).toSet should be === Set(D)
		graph.nextTasks(H, Set(H, R, M, J, G, E, F)).toSet shouldBe empty
		graph.nextTasks(D, Set(D, H, R, M, J, G, E, F)).toSet should be === Set(C)
		graph.nextTasks(C, Set(C, D, H, R, M, J, G, E, F)).toSet should be === Set(B)
		graph.nextTasks(B, Set(B, C, D, H, R, M, J, G, E, F)).toSet should be === Set(A)
	}

	it should "correctly define when completed all task" in {
		graph.isAllTasksCompleted(taskSet) shouldBe true
		graph.isAllTasksCompleted(taskSet - A) shouldBe false
		graph.isAllTasksCompleted(taskSet - D) shouldBe false
		graph.isAllTasksCompleted(Set.empty) shouldBe false
	}
}


