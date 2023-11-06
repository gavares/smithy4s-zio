package smithy4s.zio.examples.impl

import example.todo._
import zio.{Task, ZIO}
class ToDoImpl(database: Database[Todo], host: String)
    extends TodoService[Task] {

  override def healthCheck(): Task[Status] =
    ZIO.attempt(println("All good")) *> ZIO.succeed(Status("OK"))

  override def createTodo(
      title: Title,
      order: Option[Order],
      description: Option[TodoDescription]
  ): Task[Todo] =
    for {
      id <- ZIO.attempt(java.util.UUID.randomUUID().toString)
      todo = Todo(
        id = id,
        title = title,
        order = order,
        description = description,
        url = s"$host/todos/$id",
        completed = false
      )
      _ <- database.insert(todo)
    } yield todo

  override def getTodo(id: Id): Task[Todo] = {
    for {
      todo <- database.get(id.value)
      _ <- ZIO.attempt(println(todo))
    } yield todo.get
  }

  override def updateTodo(
      id: Id,
      title: Option[Title],
      order: Option[Order],
      description: Option[TodoDescription],
      completed: Option[Boolean]
  ): Task[Todo] = {
    for {
      todo <- database.get(id.value)
      _ <- ZIO.attempt(println(todo))
      updatedTodo = todo.get.copy(
        title = title.getOrElse(todo.get.title),
        order = order.orElse(todo.get.order),
        description = description.orElse(todo.get.description),
        completed = completed.getOrElse(todo.get.completed)
      )
      _ <- database.update(id.value, updatedTodo)
    } yield updatedTodo
  }

  override def deleteTodo(id: Id): Task[Unit] = {
    for {
      _ <- database.delete(id.value)
    } yield ()
  }

  override def deleteAll(): Task[Unit] = {
    for {
      _ <- database.deleteAll()
    } yield ()
  }

  override def listTodos(): Task[ListTodosOutput] = {
    for {
      todos <- database.list()
    } yield ListTodosOutput(todos)
  }

  override def apiVersion(): Task[ApiVersionOutput] = {
    ZIO.succeed(ApiVersionOutput("1.0"))
  }
}

object ToDoImpl {
  def apply(database: Database[Todo], host: String): ToDoImpl =
    new ToDoImpl(database, host)
}
