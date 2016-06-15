package com.testing

import com.testing.model.{Task, TaskData, TaskDepends, TaskType}

/**
  * @author Roman Maksyutov
  */
trait BaseSpec {
	val planOfDepends: String =
		"""
		  | A--B---C------D--E
		  |    |   |      |
		  | F--G   H--J   R--J
		  |    |   |  |   |
		  |    E   F  G   M--G
		""".stripMargin

	implicit val defaultExecutionTime: Long = 0

	val A = Task("A", TaskType.Transforming, TaskData("some data", defaultExecutionTime, None))
	val B = Task("B", TaskType.Transforming, TaskData("some data", defaultExecutionTime, None))
	val C = Task("C", TaskType.Transforming, TaskData("some data", defaultExecutionTime, None))
	val D = Task("D", TaskType.Transforming, TaskData("some data", defaultExecutionTime, None))
	val E = Task("E", TaskType.Transforming, TaskData("some data", defaultExecutionTime, None))
	val F = Task("F", TaskType.Transforming, TaskData("some data", defaultExecutionTime, None))
	val G = Task("G", TaskType.Transforming, TaskData("some data", defaultExecutionTime, None))
	val H = Task("H", TaskType.Transforming, TaskData("some data", defaultExecutionTime, None))
	val J = Task("J", TaskType.Transforming, TaskData("some data", defaultExecutionTime, None))
	val M = Task("M", TaskType.Transforming, TaskData("some data", defaultExecutionTime, None))
	val R = Task("R", TaskType.Transforming, TaskData("some data", defaultExecutionTime, None))
	val taskSet = Set(A, B, C, D, E, F, G, H, J, M, R)

	val correctDependsCache: Map[Task, TaskDepends] = Map(
		(A -> TaskDepends(A, Nil, List(B))),
		(B -> TaskDepends(B, List(A), List(C, G))),
		(C -> TaskDepends(C, List(B), List(D, H))),
		(D -> TaskDepends(D, List(C), List(E, R))),
		(E -> TaskDepends(E, List(G, D), Nil)),
		(F -> TaskDepends(F, List(G, H), Nil)),
		(G -> TaskDepends(G, List(J, M, B), List(F, E))),
		(H -> TaskDepends(H, List(C), List(J, F))),
		(J -> TaskDepends(J, List(H, R), List(G))),
		(M -> TaskDepends(M, List(R), List(G))),
		(R -> TaskDepends(R, List(D), List(J, M)))
	)
}
