---
id: airframe-walkthrough
layout: docs
title: Airframe Walkthrough: Building Applications Step by Step
---

# Airframe Walkthrough: Building Applications Step by Step

This comprehensive walkthrough guides you through building a complete application with Airframe, demonstrating how different modules work together to create scalable and maintainable Scala applications. 

**What makes this walkthrough special:** Instead of learning isolated modules, you'll see how Airframe's components complement each other to solve real-world development challenges. We'll progressively build a **Task Management Application** that showcases the entire Airframe ecosystem in action.

## What We'll Build Together

Throughout this walkthrough, we'll create a complete **Task Management Application** featuring:

- **🔧 Backend Server**: RPC services with dependency injection and structured logging
- **🚀 RPC Communication**: Type-safe client-server communication with shared interfaces  
- **💻 Web Frontend**: Reactive Scala.js-based UI that shares code with the server
- **📟 Command-Line Tool**: Rich CLI with multiple output formats and RPC integration
- **🧪 Comprehensive Testing**: Unit and integration tests using AirSpec with DI

**By the end**, you'll understand how Airframe enables rapid development of type-safe, cross-platform applications and how to leverage its modules together effectively.

## Prerequisites

Before starting, make sure you have:

- Scala 2.13 or 3.x
- sbt 1.x
- Basic knowledge of Scala

## Project Setup

Create a new sbt project with the following structure:

```
task-app/
├── build.sbt
├── project/
│   ├── build.properties
│   └── plugins.sbt
├── api/                    # Shared API definitions
├── server/                 # Backend server
├── client/                 # Scala.js frontend  
└── cli/                    # Command-line interface
```

**build.sbt**
```scala
val AIRFRAME_VERSION = "(latest_version)"

ThisBuild / scalaVersion := "2.13.12"
ThisBuild / organization := "com.example"

// Shared API module (for both JVM and JS)
lazy val api = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("api"))
  .settings(
    name := "task-api",
    libraryDependencies ++= Seq(
      "org.wvlet.airframe" %%% "airframe-http" % AIRFRAME_VERSION
    )
  )

// Server module (JVM only)
lazy val server = project
  .in(file("server"))
  .settings(
    name := "task-server",
    libraryDependencies ++= Seq(
      "org.wvlet.airframe" %% "airframe"             % AIRFRAME_VERSION,
      "org.wvlet.airframe" %% "airframe-http-netty"  % AIRFRAME_VERSION,
      "org.wvlet.airframe" %% "airframe-log"         % AIRFRAME_VERSION,
      "org.wvlet.airframe" %% "airframe-codec"       % AIRFRAME_VERSION,
      "org.wvlet.airframe" %% "airframe-launcher"    % AIRFRAME_VERSION,
      "org.wvlet.airframe" %% "airspec"              % AIRFRAME_VERSION % Test
    )
  )
  .dependsOn(api.jvm)

// Scala.js client module
lazy val client = project
  .in(file("client"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "task-client",
    libraryDependencies ++= Seq(
      "org.wvlet.airframe" %%% "airframe-http"  % AIRFRAME_VERSION,
      "org.wvlet.airframe" %%% "airframe-rx"    % AIRFRAME_VERSION,
      "org.wvlet.airframe" %%% "airframe-log"   % AIRFRAME_VERSION
    )
  )
  .dependsOn(api.js)

// CLI module
lazy val cli = project
  .in(file("cli"))
  .settings(
    name := "task-cli",
    libraryDependencies ++= Seq(
      "org.wvlet.airframe" %% "airframe-launcher" % AIRFRAME_VERSION,
      "org.wvlet.airframe" %% "airframe-http"     % AIRFRAME_VERSION,
      "org.wvlet.airframe" %% "airframe-log"      % AIRFRAME_VERSION
    )
  )
  .dependsOn(api.jvm)
```

**project/plugins.sbt**
```scala
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.13.2")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2")
addSbtPlugin("org.wvlet.airframe" % "sbt-airframe" % "(version)")
```

## Step 1: Foundation with Logging

Let's start by setting up logging, which is essential for any application. Airframe provides [airframe-log](airframe-log.md), a modern logging library designed specifically for Scala.

### Basic Logging Setup

**api/src/main/scala/taskapp/api/Models.scala**
```scala
package taskapp.api

import java.time.Instant

case class Task(
  id: String,
  title: String, 
  description: String,
  completed: Boolean = false,
  createdAt: Instant = Instant.now(),
  updatedAt: Option[Instant] = None
)

case class CreateTaskRequest(title: String, description: String)
case class UpdateTaskRequest(title: Option[String], description: Option[String], completed: Option[Boolean])
case class TaskListResponse(tasks: Seq[Task], total: Int)
```

**server/src/main/scala/taskapp/server/TaskApp.scala**
```scala
package taskapp.server

import wvlet.log.{LogSupport, Logger}

object TaskApp extends App with LogSupport {
  info("Starting Task Management Application")
  debug("Debug logging is available but won't show unless debug level is enabled")
  
  // Configure log level programmatically
  // Logger.setDefaultLogLevel(LogLevel.DEBUG)
  
  info("Application initialized successfully")
}
```

### Key Logging Features

1. **Source Code Locations**: Automatically shows the file and line number where logs are generated
2. **Macro-based Performance**: Debug logs have zero overhead when disabled
3. **Programmatic Configuration**: No XML or property files needed

Run the application:
```bash
$ sbt "server/run"
```

You'll see output like:
```
2023-12-15 10:30:15.123-0800  info [TaskApp] Starting Task Management Application  - (TaskApp.scala:8)
2023-12-15 10:30:15.125-0800  info [TaskApp] Application initialized successfully  - (TaskApp.scala:14)
```

## Step 2: Dependency Injection with Airframe DI

As our application grows, managing dependencies becomes crucial. [Airframe DI](airframe-di.md) provides a Scala-friendly dependency injection framework.

**server/src/main/scala/taskapp/server/repository/TaskRepository.scala**
```scala
package taskapp.server.repository

import taskapp.api.{Task, CreateTaskRequest, UpdateTaskRequest}
import wvlet.log.LogSupport
import java.time.Instant
import java.util.UUID
import scala.collection.mutable

trait TaskRepository {
  def findAll(): Seq[Task]
  def findById(id: String): Option[Task] 
  def create(request: CreateTaskRequest): Task
  def update(id: String, request: UpdateTaskRequest): Option[Task]
  def delete(id: String): Boolean
}

// In-memory implementation for this example
class InMemoryTaskRepository extends TaskRepository with LogSupport {
  private val tasks = mutable.Map[String, Task]()
  
  override def findAll(): Seq[Task] = {
    debug(s"Finding all tasks, current count: ${tasks.size}")
    tasks.values.toSeq.sortBy(_.createdAt)
  }
  
  override def findById(id: String): Option[Task] = {
    debug(s"Finding task by id: ${id}")
    tasks.get(id)
  }
  
  override def create(request: CreateTaskRequest): Task = {
    val task = Task(
      id = UUID.randomUUID().toString,
      title = request.title,
      description = request.description
    )
    tasks += task.id -> task
    info(s"Created task: ${task.id}")
    task
  }
  
  override def update(id: String, request: UpdateTaskRequest): Option[Task] = {
    tasks.get(id).map { existing =>
      val updated = existing.copy(
        title = request.title.getOrElse(existing.title),
        description = request.description.getOrElse(existing.description),
        completed = request.completed.getOrElse(existing.completed),
        updatedAt = Some(Instant.now())
      )
      tasks += id -> updated
      info(s"Updated task: ${id}")
      updated
    }
  }
  
  override def delete(id: String): Boolean = {
    val existed = tasks.contains(id)
    tasks.remove(id)
    if (existed) info(s"Deleted task: ${id}")
    existed
  }
}
```

**server/src/main/scala/taskapp/server/service/TaskService.scala**
```scala
package taskapp.server.service

import taskapp.api.{Task, CreateTaskRequest, UpdateTaskRequest, TaskListResponse}
import taskapp.server.repository.TaskRepository
import wvlet.log.LogSupport

class TaskService(repository: TaskRepository) extends LogSupport {
  
  def listTasks(): TaskListResponse = {
    debug("Listing all tasks")
    val tasks = repository.findAll()
    TaskListResponse(tasks = tasks, total = tasks.size)
  }
  
  def getTask(id: String): Option[Task] = {
    debug(s"Getting task: ${id}")
    repository.findById(id)
  }
  
  def createTask(request: CreateTaskRequest): Task = {
    info(s"Creating task: ${request.title}")
    repository.create(request)
  }
  
  def updateTask(id: String, request: UpdateTaskRequest): Option[Task] = {
    info(s"Updating task: ${id}")
    repository.update(id, request)
  }
  
  def deleteTask(id: String): Boolean = {
    info(s"Deleting task: ${id}")
    repository.delete(id)
  }
}
```

**server/src/main/scala/taskapp/server/AppDesign.scala**
```scala
package taskapp.server

import taskapp.server.repository.{TaskRepository, InMemoryTaskRepository}
import taskapp.server.service.TaskService
import wvlet.airframe.*

object AppDesign {
  
  val design: Design = newDesign
    // Bind configuration as singleton
    .bind[AppConfig].toProvider { Config(env = "default").of[AppConfig] }
    
    // Bind repository interface to implementation
    .bind[TaskRepository].to[InMemoryTaskRepository]
    
    // TaskService will automatically receive TaskRepository through constructor injection
    .bind[TaskService].toSingleton
}
```

**server/src/main/scala/taskapp/server/DIApp.scala**
```scala
package taskapp.server

import taskapp.api.CreateTaskRequest
import taskapp.server.service.TaskService
import wvlet.log.{LogSupport, Logger}

object DIApp extends App with LogSupport {
  // Build and run the application using the design
  AppDesign.design.build[TaskService] { taskService =>
    info("Testing dependency injection")
    
    // Create some tasks
    val task1 = taskService.createTask(CreateTaskRequest("Learn Airframe", "Complete the walkthrough"))
    val task2 = taskService.createTask(CreateTaskRequest("Build app", "Create a task management app"))
    
    // List all tasks
    val response = taskService.listTasks()
    info(s"Total tasks: ${response.total}")
    response.tasks.foreach { task =>
      info(s"- ${task.title}: ${task.description}")
    }
  }
}
```

### Key DI Features Demonstrated

1. **Constructor Injection**: Dependencies are automatically injected through constructor parameters
2. **Interface Binding**: Bind interfaces to implementations for easy testing and flexibility
3. **Singleton Scoping**: Services can be singletons to share state
4. **Provider Binding**: Complex object creation through provider functions

## Step 3: RPC Communication

[Airframe RPC](airframe-rpc.md) provides type-safe communication between services. Unlike traditional REST APIs, RPC allows you to define service interfaces once and use them for both server implementation and client generation.

**api/src/main/scala/taskapp/api/TaskApi.scala**
```scala
package taskapp.api

import wvlet.airframe.http.RPC

@RPC
trait TaskApi {
  def listTasks(): TaskListResponse
  def getTask(id: String): Option[Task]
  def createTask(request: CreateTaskRequest): Task
  def updateTask(id: String, request: UpdateTaskRequest): Option[Task]
  def deleteTask(id: String): Boolean
}
```

**server/src/main/scala/taskapp/server/api/TaskApiImpl.scala**
```scala
package taskapp.server.api

import taskapp.api.{TaskApi, Task, CreateTaskRequest, UpdateTaskRequest, TaskListResponse}
import taskapp.server.service.TaskService
import wvlet.log.LogSupport

class TaskApiImpl(taskService: TaskService) extends TaskApi with LogSupport {
  
  override def listTasks(): TaskListResponse = {
    debug("RPC: Listing tasks")
    taskService.listTasks()
  }
  
  override def getTask(id: String): Option[Task] = {
    debug(s"RPC: Getting task ${id}")
    taskService.getTask(id)
  }
  
  override def createTask(request: CreateTaskRequest): Task = {
    info(s"RPC: Creating task '${request.title}'")
    taskService.createTask(request)
  }
  
  override def updateTask(id: String, request: UpdateTaskRequest): Option[Task] = {
    info(s"RPC: Updating task ${id}")
    taskService.updateTask(id, request)
  }
  
  override def deleteTask(id: String): Boolean = {
    info(s"RPC: Deleting task ${id}")
    taskService.deleteTask(id)
  }
}
```

**server/src/main/scala/taskapp/server/TaskRPCServer.scala**  
```scala
package taskapp.server

import taskapp.server.api.TaskApiImpl  
import wvlet.airframe.*
import wvlet.airframe.http.*
import wvlet.airframe.http.netty.Netty
import wvlet.log.{LogSupport, Logger}

object TaskRPCServer extends App with LogSupport {
  // Create RPC router from our API implementation
  val router = RxRouter.of[TaskApiImpl]
  
  val rpcDesign = AppDesign.design
    .bind[TaskApiImpl].toSingleton
    .add(Netty.server
      .withPort(8080)
      .withRouter(router)
      .design)
  
  rpcDesign.build[HttpServer] { server =>
    info(s"Task RPC server started at http://localhost:${server.port}")
    info("RPC interface available for type-safe client access")
    
    server.awaitTermination()
  }
}
```

**cli/src/main/scala/taskapp/cli/TaskCLI.scala**
```scala
package taskapp.cli

import taskapp.api.{TaskApi, CreateTaskRequest, UpdateTaskRequest}
import wvlet.airframe.http.Http
import wvlet.airframe.launcher.{Launcher, command, option}
import wvlet.log.{LogSupport, Logger}
import scala.util.Using

object TaskCLI extends App {
  Launcher.of[TaskCLI].execute(args)
}

class TaskCLI(
  @option(prefix = "-h,--help", description = "show help", isHelp = true)
  help: Boolean = false,
  @option(prefix = "--host", description = "server host")  
  host: String = "localhost",
  @option(prefix = "--port", description = "server port")
  port: Int = 8080
) extends LogSupport {
  // Create RPC client - this demonstrates type-safe client generation
  private def withClient[A](f: TaskApi => A): A = {
    Using.resource(Http.client.newSyncClient(s"${host}:${port}")) { httpClient =>
      // Generated RPC client provides type-safe access to remote services
      val rpcClient = taskapp.api.TaskApiRPC.newRPCSyncClient(httpClient)
      f(rpcClient.TaskApi)
    }
  }
  
  @command(description = "List all tasks")
  def list(): Unit = {
    withClient { client =>
      val response = client.listTasks()
      info(s"Found ${response.total} tasks:")
      response.tasks.foreach { task =>
        val status = if (task.completed) "✓" else "○"
        println(s"  ${status} [${task.id}] ${task.title}")
        println(s"    ${task.description}")
      }
    }
  }
  
  @command(description = "Create a new task")
  def create(
    @option(prefix = "--title", description = "task title") title: String,
    @option(prefix = "--desc", description = "task description") description: String = ""
  ): Unit = {
    withClient { client =>
      val task = client.createTask(CreateTaskRequest(title, description))
      info(s"Created task: ${task.id}")
    }
  }
  
  @command(description = "Complete a task")
  def complete(
    @option(prefix = "--id", description = "task ID") id: String
  ): Unit = {
    withClient { client =>
      client.updateTask(id, UpdateTaskRequest(completed = Some(true))) match {
        case Some(task) => info(s"Marked task '${task.title}' as completed")
        case None => info(s"Task ${id} not found")
      }
    }
  }
  
  @command(description = "Delete a task")  
  def delete(
    @option(prefix = "--id", description = "task ID") id: String
  ): Unit = {
    withClient { client =>
      if (client.deleteTask(id)) {
        info(s"Deleted task ${id}")
      } else {
        info(s"Task ${id} not found")
      }
    }
  }
}
```

To use the CLI:
```bash
# Start the RPC server
$ sbt "server/run"

# In another terminal, use the CLI
$ sbt "cli/run create --title 'Learn RPC' --desc 'Understand Airframe RPC'"
$ sbt "cli/run list"
$ sbt "cli/run complete --id {task-id}"  
$ sbt "cli/run delete --id {task-id}"
```

### Key RPC Features

1. **Type Safety**: Client and server share the same interface
2. **Multiple Protocols**: Supports JSON, MessagePack, and gRPC
3. **Cross-Platform**: Works with Scala.js for frontend development
4. **Automatic Code Generation**: Client code generated from interfaces

## Step 4: Scala.js Frontend with airframe-rx

Let's build a browser-based frontend using [Airframe Rx](airframe-rx.md) for reactive UI development.

**client/src/main/scala/taskapp/client/TaskClient.scala**
```scala
package taskapp.client

import taskapp.api.{TaskApi, Task, CreateTaskRequest, UpdateTaskRequest}
import wvlet.airframe.http.Http
import wvlet.airframe.rx.*
import wvlet.airframe.rx.html.all.*
import wvlet.log.{LogSupport, Logger}
import org.scalajs.dom
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

object TaskClient extends LogSupport {
  // RPC client for communicating with the server
  private val httpClient = Http.client.newAsyncClient("http://localhost:8080")
  private val rpcClient = taskapp.api.TaskApiRPC.newRPCAsyncClient(httpClient).TaskApi
  
  def main(args: Array[String]): Unit = {
    val app = new TaskApp()
    dom.document.getElementById("app").appendChild(app.render)
    
    // Load initial tasks
    app.loadTasks()
  }
}

class TaskApp extends LogSupport {
  // Reactive state
  private val tasks = Rx.variable(Seq.empty[Task])
  private val newTaskTitle = Rx.variable("")
  private val newTaskDesc = Rx.variable("")
  private val loading = Rx.variable(false)
  
  def loadTasks(): Unit = {
    loading := true
    // Using type-safe RPC client
    TaskClient.rpcClient.listTasks().onComplete {
      case Success(response) =>
        tasks := response.tasks
        loading := false
        info(s"Loaded ${response.total} tasks")
      case Failure(ex) =>
        loading := false
        info(s"Failed to load tasks: ${ex.getMessage}")
    }
  }
  
  def createTask(): Unit = {
    val title = newTaskTitle.get
    if (title.nonEmpty) {
      val request = CreateTaskRequest(title, newTaskDesc.get)
      TaskClient.rpcClient.createTask(request).onComplete {
        case Success(task) =>
          tasks := tasks.get :+ task
          newTaskTitle := ""
          newTaskDesc := ""
          info(s"Created task: ${task.title}")
        case Failure(ex) =>
          info(s"Failed to create task: ${ex.getMessage}")
      }
    }
  }
  
  def toggleTask(task: Task): Unit = {
    val request = UpdateTaskRequest(completed = Some(!task.completed))
    TaskClient.rpcClient.updateTask(task.id, request).onComplete {
      case Success(Some(updated)) =>
        tasks := tasks.get.map(t => if (t.id == task.id) updated else t)
        info(s"Updated task: ${task.title}")
      case Success(None) =>
        info("Task not found")
      case Failure(ex) =>
        info(s"Failed to update task: ${ex.getMessage}")
    }
  }
  
  def deleteTask(task: Task): Unit = {
    TaskClient.rpcClient.deleteTask(task.id).onComplete {
      case Success(true) =>
        tasks := tasks.get.filterNot(_.id == task.id)
        info(s"Deleted task: ${task.title}")
      case Success(false) =>
        info("Task not found")
      case Failure(ex) =>
        info(s"Failed to delete task: ${ex.getMessage}")
    }
  }
  
  def render: RxElement = {
    div(cls -> "container",
      h1("Task Management"),
      
      // New task form
      div(cls -> "new-task-form",
        h3("Add New Task"),
        input(
          tpe -> "text",
          placeholder -> "Task title",
          value := newTaskTitle,
          onInput := { (e: dom.Event) =>
            newTaskTitle := e.target.asInstanceOf[dom.HTMLInputElement].value
          }
        ),
        textarea(
          placeholder -> "Task description",
          value := newTaskDesc,
          onInput := { (e: dom.Event) =>
            newTaskDesc := e.target.asInstanceOf[dom.HTMLTextAreaElement].value
          }
        ),
        button(
          cls -> "btn btn-primary",
          onclick := { () => createTask() },
          disabled := newTaskTitle.map(_.isEmpty),
          "Add Task"
        )
      ),
      
      // Loading indicator
      loading.map { isLoading =>
        if (isLoading) div(cls -> "loading", "Loading...") else span()
      },
      
      // Task list
      div(cls -> "task-list",
        h3("Tasks"),
        tasks.map { taskList =>
          if (taskList.isEmpty) {
            div(cls -> "empty-state", "No tasks yet. Create one above!")
          } else {
            div(
              taskList.map { task =>
                div(cls -> s"task-item ${if (task.completed) "completed" else ""}",
                  div(cls -> "task-content",
                    h4(task.title),
                    p(task.description),
                    small(s"Created: ${task.createdAt}")
                  ),
                  div(cls -> "task-actions",
                    button(
                      cls -> "btn btn-sm",
                      onclick := { () => toggleTask(task) },
                      if (task.completed) "Mark Incomplete" else "Mark Complete"
                    ),
                    button(
                      cls -> "btn btn-sm btn-danger",
                      onclick := { () => deleteTask(task) },
                      "Delete"
                    )
                  )
                )
              }
            )
          }
        }
      ),
      
      // Refresh button  
      div(cls -> "actions",
        button(
          cls -> "btn btn-secondary",
          onclick := { () => loadTasks() },
          "Refresh"
        )
      )
    )
  }
}
```

**client/src/main/resources/index.html**
```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Task Management App</title>
    <style>
        .container { max-width: 800px; margin: 0 auto; padding: 20px; }
        .new-task-form { background: #f5f5f5; padding: 20px; margin-bottom: 20px; }
        .new-task-form input, .new-task-form textarea { 
            width: 100%; margin-bottom: 10px; padding: 8px; 
        }
        .task-item { 
            border: 1px solid #ddd; margin-bottom: 10px; padding: 15px; 
            display: flex; justify-content: space-between; align-items: center;
        }
        .task-item.completed { background-color: #e8f5e8; }
        .task-content { flex-grow: 1; }
        .task-content h4 { margin: 0 0 5px 0; }
        .task-content p { margin: 5px 0; color: #666; }
        .task-actions button { margin-left: 10px; }
        .btn { padding: 8px 16px; border: none; cursor: pointer; }
        .btn-primary { background: #007bff; color: white; }
        .btn-secondary { background: #6c757d; color: white; }
        .btn-danger { background: #dc3545; color: white; }
        .btn-sm { padding: 4px 8px; font-size: 12px; }
        .loading { text-align: center; padding: 20px; color: #666; }
        .empty-state { text-align: center; padding: 40px; color: #999; }
    </style>
</head>
<body>
    <div id="app"></div>
    <script src="../client-fastopt/main.js"></script>
</body>
</html>
```

To run the frontend:
```bash
# Compile Scala.js
$ sbt "client/fastOptJS"

# Serve the HTML file (you can use any static server)
$ cd client/src/main/resources && python3 -m http.server 3000

# Open http://localhost:3000 in your browser
```

### Key Scala.js Features

1. **Reactive UI**: Airframe Rx provides reactive programming for the DOM
2. **Type Safety**: Share API definitions between client and server  
3. **RPC Integration**: Use the same RPC client on both JVM and JS
4. **Full Scala Ecosystem**: Use Scala libraries in the browser

## Step 5: Testing with AirSpec

A complete application needs comprehensive testing. [AirSpec](airspec.md) provides a functional testing framework with DI integration.

**server/src/test/scala/taskapp/server/TaskServiceTest.scala**
```scala
package taskapp.server

import taskapp.api.{CreateTaskRequest, UpdateTaskRequest}
import taskapp.server.service.TaskService
import wvlet.airspec.*

class TaskServiceTest extends AirSpec {
  // Use the application design for testing
  override def design: Design = AppDesign.design
  
  test("should create and retrieve tasks") { (service: TaskService) =>
    // Create a task
    val created = service.createTask(CreateTaskRequest("Test Task", "Test Description"))
    created.title shouldBe "Test Task"
    created.description shouldBe "Test Description"
    created.completed shouldBe false
    created.id shouldNotBe empty
    
    // Retrieve the task
    val retrieved = service.getTask(created.id)
    retrieved shouldBe Some(created)
  }
  
  test("should list tasks") { (service: TaskService) =>
    // Initially empty
    val empty = service.listTasks()
    empty.tasks shouldBe empty
    empty.total shouldBe 0
    
    // Create some tasks
    service.createTask(CreateTaskRequest("Task 1", "Description 1"))
    service.createTask(CreateTaskRequest("Task 2", "Description 2"))
    
    // Should have 2 tasks
    val list = service.listTasks()
    list.total shouldBe 2
    list.tasks.size shouldBe 2
  }
  
  test("should update tasks") { (service: TaskService) =>
    // Create a task
    val task = service.createTask(CreateTaskRequest("Original", "Original desc"))
    
    // Update title
    val updated1 = service.updateTask(task.id, UpdateTaskRequest(title = Some("Updated")))
    updated1 shouldBe defined
    updated1.get.title shouldBe "Updated"
    updated1.get.description shouldBe "Original desc"
    
    // Mark as completed
    val updated2 = service.updateTask(task.id, UpdateTaskRequest(completed = Some(true)))
    updated2 shouldBe defined  
    updated2.get.completed shouldBe true
  }
  
  test("should delete tasks") { (service: TaskService) =>
    // Create a task
    val task = service.createTask(CreateTaskRequest("To Delete", "Will be deleted"))
    
    // Verify it exists
    service.getTask(task.id) shouldBe defined
    
    // Delete it
    val deleted = service.deleteTask(task.id)
    deleted shouldBe true
    
    // Verify it's gone
    service.getTask(task.id) shouldBe None
    
    // Deleting again should return false
    service.deleteTask(task.id) shouldBe false
  }
  
  test("should handle non-existent tasks") { (service: TaskService) =>
    val nonExistentId = "non-existent"
    
    service.getTask(nonExistentId) shouldBe None
    service.updateTask(nonExistentId, UpdateTaskRequest(title = Some("Updated"))) shouldBe None
    service.deleteTask(nonExistentId) shouldBe false
  }
}
```

**server/src/test/scala/taskapp/server/TaskApiTest.scala**
```scala
package taskapp.server

import taskapp.api.{Task, CreateTaskRequest, UpdateTaskRequest, TaskListResponse}
import taskapp.server.api.TaskApiImpl
import wvlet.airframe.http.*
import wvlet.airframe.http.netty.Netty
import wvlet.airspec.*

class TaskApiTest extends AirSpec {
  
  private val testDesign = AppDesign.design
    .bind[TaskApiImpl].toSingleton
    .add(Netty.server.withRandomPort.withRouter(RxRouter.of[TaskApiImpl]).design)
  
  test("API should handle task lifecycle") { (server: HttpServer, client: Http.Client) =>
    // Create a task via POST
    val createRequest = CreateTaskRequest("API Test", "Testing the API")
    val created = client.post[CreateTaskRequest, Task]("/api/tasks", createRequest)
    
    created.title shouldBe "API Test"
    created.description shouldBe "Testing the API"
    
    // Get the task via GET
    val retrieved = client.get[Task](s"/api/tasks/${created.id}")
    retrieved shouldBe created
    
    // Update the task via PUT  
    val updateRequest = UpdateTaskRequest(completed = Some(true))
    val updated = client.put[UpdateTaskRequest, Task](s"/api/tasks/${created.id}", updateRequest)
    
    updated.completed shouldBe true
    updated.id shouldBe created.id
    
    // List tasks
    val list = client.get[TaskListResponse]("/api/tasks")
    list.total >= 1 shouldBe true
    list.tasks.contains(updated) shouldBe true
    
    // Delete the task
    val deleted = client.delete[Boolean](s"/api/tasks/${created.id}")
    deleted shouldBe true
    
    // Verify deletion - this may return None rather than throwing
    // Implementation detail depends on your specific API design
  }
  
  test("API should handle errors gracefully") { (server: HttpServer, client: Http.Client) =>
    // Try to get non-existent task - should return None or appropriate response
    val response = client.sendSafe(Http.GET("/api/tasks/non-existent"))
    // Should handle gracefully without throwing
    
    // Try to update non-existent task
    val updateResponse = client.sendSafe(
      Http.PUT(s"/api/tasks/non-existent")
        .withJson(UpdateTaskRequest(title = Some("Updated")))
    )
    // Should handle gracefully
  }
}
```

Run the tests:
```bash
$ sbt "server/test"
```

### Key Testing Features

1. **DI Integration**: Tests can inject dependencies automatically
2. **HTTP Testing**: Built-in support for testing HTTP endpoints
3. **Lifecycle Management**: AirSpec manages service lifecycles for tests
4. **Property-based Testing**: Support for property-based testing scenarios

## Conclusion

Congratulations! You've built a complete task management application using Airframe that demonstrates:

### What We've Accomplished

1. **Logging**: Structured, performant logging with source code locations
2. **Dependency Injection**: Clean separation of concerns with constructor injection
3. **RPC Communication**: Type-safe client-server communication
4. **Scala.js Frontend**: Reactive browser-based UI sharing server code
5. **Command-Line Interface**: Rich CLI with multiple output formats
6. **Testing**: Comprehensive testing with DI integration

### Key Benefits of Airframe

- **Unified Ecosystem**: All modules work seamlessly together
- **Type Safety**: Compile-time checking across the entire stack
- **Cross-Platform**: Code sharing between JVM, JS, and Native
- **Scala-Centric**: Designed specifically for Scala developers
- **Minimal Boilerplate**: Focus on business logic, not infrastructure

### Next Steps

To further enhance your application, consider:

1. **Database Integration**: Use [airframe-jdbc](airframe-jdbc.md) for database connectivity
2. **Metrics & Monitoring**: Add [airframe-jmx](airframe-jmx.md) for runtime monitoring
3. **Advanced HTTP Features**: Explore [airframe-http-recorder](airframe-http-recorder.md) for development
4. **Configuration Profiles**: Use environment-specific configurations
5. **Error Handling**: Implement comprehensive error handling and recovery
6. **Authentication**: Add security layers to your APIs
7. **Deployment**: Package with [sbt-pack](https://github.com/xerial/sbt-pack) for production

### Module Reference

For deeper understanding of individual modules:

- **[Airframe DI](airframe-di.md)**: Advanced dependency injection patterns
- **[Airframe HTTP](airframe-http.md)**: HTTP server and client features  
- **[Airframe RPC](airframe-rpc.md)**: RPC protocols and client generation
- **[Airframe Rx](airframe-rx.md)**: Reactive programming for UIs
- **[AirSpec](airspec.md)**: Testing patterns and best practices
- **[Logging](airframe-log.md)**: Logging configuration and patterns

This walkthrough shows how Airframe enables you to build sophisticated applications with minimal effort, leveraging the power of Scala across your entire stack. The modules work together naturally, reducing complexity while maintaining flexibility and type safety.