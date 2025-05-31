---
id: walkthrough
layout: docs
title: Airframe Walkthrough
---

# Building Applications with Airframe: A Complete Walkthrough

This walkthrough demonstrates how to build a complete application using Airframe's modules, starting from basic logging and dependency injection, progressing to web services, RPC, and even frontend development with Scala.js.

## Overview

We'll build a **Task Management System** that showcases:

1. **Logging** with airframe-log
2. **Dependency Injection** with airframe-di  
3. **Configuration** with airframe-config
4. **REST API** with airframe-http
5. **RPC Services** with airframe-rpc
6. **Data Serialization** with airframe-codec
7. **Testing** with airspec
8. **Frontend UI** with Scala.js and airframe-rx

By the end, you'll have a complete understanding of how Airframe modules work together to create powerful, maintainable applications.

## Chapter 1: Project Setup and Logging

Let's start by setting up a new project with basic logging support.

### Project Structure

```
task-manager/
├── build.sbt
├── project/
│   ├── build.properties  
│   └── plugins.sbt
└── src/main/scala/
    └── taskmanager/
```

### build.sbt

```scala
val AIRFRAME_VERSION = "24.12.0" // Use the latest version

ThisBuild / scalaVersion := "3.3.1"
ThisBuild / organization := "com.example"

lazy val root = project
  .in(file("."))
  .settings(
    name := "task-manager",
    libraryDependencies ++= Seq(
      "org.wvlet.airframe" %% "airframe"         % AIRFRAME_VERSION, // DI
      "org.wvlet.airframe" %% "airframe-log"     % AIRFRAME_VERSION, // Logging
      "org.wvlet.airframe" %% "airframe-config"  % AIRFRAME_VERSION, // Configuration
      "org.wvlet.airframe" %% "airframe-http"    % AIRFRAME_VERSION, // HTTP API
      "org.wvlet.airframe" %% "airframe-http-netty" % AIRFRAME_VERSION, // Netty backend
      "org.wvlet.airframe" %% "airframe-codec"   % AIRFRAME_VERSION, // Serialization
      "org.wvlet.airframe" %% "airframe-control" % AIRFRAME_VERSION, // Retry/control flow
      "org.wvlet.airframe" %% "airspec"          % AIRFRAME_VERSION % Test // Testing
    )
  )
```

### Basic Application with Logging

```scala
package taskmanager

import wvlet.log.LogSupport

// Domain model
case class Task(
  id: String,
  title: String, 
  description: String,
  completed: Boolean = false,
  createdAt: Long = System.currentTimeMillis()
)

// Basic task service with logging
class TaskService extends LogSupport {
  private var tasks: Map[String, Task] = Map.empty
  
  def createTask(title: String, description: String): Task = {
    val task = Task(
      id = java.util.UUID.randomUUID().toString,
      title = title,
      description = description
    )
    tasks = tasks + (task.id -> task)
    info(s"Created task: ${task.title} (${task.id})")
    task
  }
  
  def findTask(id: String): Option[Task] = {
    debug(s"Looking up task: $id")
    tasks.get(id)
  }
  
  def listTasks(): List[Task] = {
    debug(s"Listing ${tasks.size} tasks")
    tasks.values.toList
  }
  
  def completeTask(id: String): Option[Task] = {
    tasks.get(id) match {
      case Some(task) =>
        val updated = task.copy(completed = true)
        tasks = tasks + (id -> updated)
        info(s"Completed task: ${task.title}")
        Some(updated)
      case None =>
        warn(s"Task not found: $id")
        None
    }
  }
}
```

### Running with Proper Log Configuration

```scala
package taskmanager

import wvlet.log.{LogLevel, Logger}

object TaskManagerApp extends App {
  // Configure logging
  Logger.setDefaultLogLevel(LogLevel.INFO)
  
  val service = new TaskService()
  
  // Create some tasks
  val task1 = service.createTask("Learn Airframe", "Complete the walkthrough tutorial")
  val task2 = service.createTask("Build API", "Create REST endpoints")
  
  // Use the service
  service.listTasks().foreach(task => println(s"- ${task.title}"))
  service.completeTask(task1.id)
}
```

## Chapter 2: Dependency Injection and Configuration

Now let's introduce proper dependency injection and configuration management.

### Configuration

First, create a configuration file `src/main/resources/application.yml`:

```yaml
server:
  port: 8080
  host: "localhost"

database:
  maxConnections: 10
  timeout: 30s

app:
  name: "Task Manager"
  version: "1.0.0"
```

### Configuration Case Classes

```scala
package taskmanager.config

case class ServerConfig(
  port: Int = 8080,
  host: String = "localhost"
)

case class DatabaseConfig(
  maxConnections: Int = 10,
  timeout: String = "30s"
)

case class AppConfig(
  name: String = "Task Manager",
  version: String = "1.0.0"
)

case class Config(
  server: ServerConfig = ServerConfig(),
  database: DatabaseConfig = DatabaseConfig(),
  app: AppConfig = AppConfig()
)
```

### Refactoring with Dependency Injection

```scala
package taskmanager

import wvlet.airframe.*
import wvlet.log.LogSupport
import taskmanager.config.*

// Abstract repository interface
trait TaskRepository {
  def save(task: Task): Task
  def findById(id: String): Option[Task]
  def findAll(): List[Task]
  def update(task: Task): Option[Task]
}

// In-memory implementation
class InMemoryTaskRepository extends TaskRepository with LogSupport {
  private var tasks: Map[String, Task] = Map.empty
  
  def save(task: Task): Task = {
    tasks = tasks + (task.id -> task)
    debug(s"Saved task: ${task.id}")
    task
  }
  
  def findById(id: String): Option[Task] = {
    debug(s"Finding task: $id")
    tasks.get(id)
  }
  
  def findAll(): List[Task] = {
    debug(s"Finding all tasks (${tasks.size} total)")
    tasks.values.toList.sortBy(_.createdAt)
  }
  
  def update(task: Task): Option[Task] = {
    tasks.get(task.id) match {
      case Some(_) =>
        tasks = tasks + (task.id -> task)
        debug(s"Updated task: ${task.id}")
        Some(task)
      case None =>
        warn(s"Task not found for update: ${task.id}")
        None
    }
  }
}

// Updated service with DI
class TaskService(
  repository: TaskRepository,
  config: AppConfig
) extends LogSupport {
  
  info(s"Starting ${config.name} v${config.version}")
  
  def createTask(title: String, description: String): Task = {
    val task = Task(
      id = java.util.UUID.randomUUID().toString,
      title = title,
      description = description
    )
    repository.save(task)
    info(s"Created task: ${task.title}")
    task
  }
  
  def getTask(id: String): Option[Task] = {
    repository.findById(id)
  }
  
  def listTasks(): List[Task] = {
    repository.findAll()
  }
  
  def completeTask(id: String): Option[Task] = {
    repository.findById(id) match {
      case Some(task) =>
        val updated = task.copy(completed = true)
        repository.update(updated)
        info(s"Completed task: ${task.title}")
        Some(updated)
      case None =>
        warn(s"Task not found: $id")
        None
    }
  }
}
```

### Application with DI Design

```scala
package taskmanager

import wvlet.airframe.*
import wvlet.airframe.config.Config
import wvlet.log.{LogLevel, Logger}
import taskmanager.config.*

object TaskManagerApp extends App {
  Logger.setDefaultLogLevel(LogLevel.INFO)
  
  // Load configuration from YAML
  val config = Config.of[taskmanager.config.Config]
  
  // Design with dependency injection
  val design = newDesign
    .bind[taskmanager.config.Config].toInstance(config)
    .bind[TaskRepository].to[InMemoryTaskRepository]
    .bind[TaskService].toSingleton // Ensure single instance
  
  design.build[TaskService] { service =>
    // Use the service
    val task1 = service.createTask("Setup DI", "Configure dependency injection")
    val task2 = service.createTask("Add config", "Add YAML configuration")
    
    println("=== All Tasks ===")
    service.listTasks().foreach { task =>
      val status = if (task.completed) "✓" else "○"
      println(s"$status ${task.title} - ${task.description}")
    }
    
    service.completeTask(task1.id)
    println("\n=== After completing first task ===")
    service.listTasks().foreach { task =>
      val status = if (task.completed) "✓" else "○"
      println(s"$status ${task.title}")
    }
  }
}
```

## Chapter 3: Building a REST API

Now let's expose our task service as a REST API using airframe-http.

### REST API Controller

```scala
package taskmanager.api

import wvlet.airframe.http.*
import wvlet.log.LogSupport
import taskmanager.{Task, TaskService}

// Request/Response models
case class CreateTaskRequest(title: String, description: String)
case class TaskResponse(task: Task)
case class TaskListResponse(tasks: List[Task])
case class ErrorResponse(error: String, message: String)

// REST API endpoints
class TaskController(taskService: TaskService) extends LogSupport {
  
  @Endpoint(method = HttpMethod.GET, path = "/api/tasks")
  def listTasks(): TaskListResponse = {
    info("GET /api/tasks")
    TaskListResponse(taskService.listTasks())
  }
  
  @Endpoint(method = HttpMethod.POST, path = "/api/tasks")
  def createTask(request: CreateTaskRequest): TaskResponse = {
    info(s"POST /api/tasks: ${request.title}")
    val task = taskService.createTask(request.title, request.description)
    TaskResponse(task)
  }
  
  @Endpoint(method = HttpMethod.GET, path = "/api/tasks/:id")
  def getTask(id: String): TaskResponse | ErrorResponse = {
    info(s"GET /api/tasks/$id")
    taskService.getTask(id) match {
      case Some(task) => TaskResponse(task)
      case None => ErrorResponse("NOT_FOUND", s"Task not found: $id")
    }
  }
  
  @Endpoint(method = HttpMethod.PUT, path = "/api/tasks/:id/complete")
  def completeTask(id: String): TaskResponse | ErrorResponse = {
    info(s"PUT /api/tasks/$id/complete")
    taskService.completeTask(id) match {
      case Some(task) => TaskResponse(task)
      case None => ErrorResponse("NOT_FOUND", s"Task not found: $id")
    }
  }
}
```

### HTTP Server Setup

```scala
package taskmanager

import wvlet.airframe.*
import wvlet.airframe.config.Config
import wvlet.airframe.http.{HttpServer, RxRouter}
import wvlet.airframe.http.netty.Netty
import wvlet.log.{LogLevel, Logger}
import taskmanager.config.*
import taskmanager.api.TaskController

object TaskManagerServer extends App {
  Logger.setDefaultLogLevel(LogLevel.INFO)
  
  val config = Config.of[taskmanager.config.Config]
  
  // Create router for our API
  val router = RxRouter.of[TaskController]
  
  // Design with HTTP server
  val design = newDesign
    .bind[taskmanager.config.Config].toInstance(config)
    .bind[TaskRepository].to[InMemoryTaskRepository]
    .bind[TaskService].toSingleton
    .bind[TaskController].toSingleton
    .add(
      Netty.server
        .withHost(config.server.host)
        .withPort(config.server.port)
        .withRouter(router)
        .design
    )
  
  design.build[HttpServer] { server =>
    info(s"Task Manager server started at http://${config.server.host}:${config.server.port}")
    info("API endpoints:")
    info("  GET    /api/tasks")
    info("  POST   /api/tasks")
    info("  GET    /api/tasks/:id")
    info("  PUT    /api/tasks/:id/complete")
    
    // Test the API with some initial data
    design.build[TaskService] { service =>
      service.createTask("Sample Task 1", "This is a sample task")
      service.createTask("Sample Task 2", "Another sample task")
    }
    
    server.waitServerTermination
  }
}
```

### Testing the API

You can test the API with curl:

```bash
# List tasks
curl http://localhost:8080/api/tasks

# Create a task
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title": "New Task", "description": "Task created via API"}'

# Get a specific task  
curl http://localhost:8080/api/tasks/{task-id}

# Complete a task
curl -X PUT http://localhost:8080/api/tasks/{task-id}/complete
```

## Chapter 4: Building RPC Services

Let's add RPC capabilities for service-to-service communication.

### RPC Interface Definition

```scala
package taskmanager.rpc

import wvlet.airframe.http.*
import taskmanager.Task

// RPC API interface
trait TaskRPC {
  
  @RPC
  def createTask(title: String, description: String): Task
  
  @RPC  
  def getTask(id: String): Option[Task]
  
  @RPC
  def listTasks(): List[Task]
  
  @RPC
  def completeTask(id: String): Boolean
  
  @RPC
  def deleteTask(id: String): Boolean
}
```

### RPC Implementation

```scala
package taskmanager.rpc

import wvlet.log.LogSupport
import taskmanager.{Task, TaskService}

class TaskRPCImpl(taskService: TaskService) extends TaskRPC with LogSupport {
  
  def createTask(title: String, description: String): Task = {
    info(s"RPC createTask: $title")
    taskService.createTask(title, description)
  }
  
  def getTask(id: String): Option[Task] = {
    info(s"RPC getTask: $id")
    taskService.getTask(id)
  }
  
  def listTasks(): List[Task] = {
    info("RPC listTasks")
    taskService.listTasks()
  }
  
  def completeTask(id: String): Boolean = {
    info(s"RPC completeTask: $id")
    taskService.completeTask(id).isDefined
  }
  
  def deleteTask(id: String): Boolean = {
    info(s"RPC deleteTask: $id")
    // Add deletion logic to TaskService first
    false // Not implemented yet
  }
}
```

### RPC Server

```scala
package taskmanager

import wvlet.airframe.*
import wvlet.airframe.config.Config
import wvlet.airframe.http.HttpServer
import wvlet.airframe.http.netty.Netty
import wvlet.airframe.http.RxRouter
import wvlet.log.{LogLevel, Logger}
import taskmanager.config.*
import taskmanager.api.TaskController
import taskmanager.rpc.{TaskRPC, TaskRPCImpl}

object TaskManagerRPCServer extends App {
  Logger.setDefaultLogLevel(LogLevel.INFO)
  
  val config = Config.of[taskmanager.config.Config]
  
  // Router that includes both REST API and RPC
  val router = RxRouter
    .add[TaskController] // REST endpoints
    .andThen(RxRouter.of[TaskRPC]) // RPC endpoints
  
  val design = newDesign
    .bind[taskmanager.config.Config].toInstance(config)
    .bind[TaskRepository].to[InMemoryTaskRepository]
    .bind[TaskService].toSingleton
    .bind[TaskController].toSingleton
    .bind[TaskRPC].to[TaskRPCImpl]
    .add(
      Netty.server
        .withHost(config.server.host)
        .withPort(config.server.port)
        .withRouter(router)
        .design
    )
  
  design.build[HttpServer] { server =>
    info(s"Task Manager RPC server started at http://${config.server.host}:${config.server.port}")
    
    // Add some test data
    design.build[TaskService] { service =>
      service.createTask("RPC Task 1", "Task created for RPC testing")
      service.createTask("RPC Task 2", "Another RPC task")
    }
    
    server.waitServerTermination
  }
}
```

### RPC Client

```scala
package taskmanager.client

import wvlet.airframe.*
import wvlet.airframe.http.Http
import wvlet.airframe.http.rpc.RPCClient
import wvlet.log.{LogLevel, Logger, LogSupport}
import taskmanager.rpc.TaskRPC

object TaskRPCClient extends App with LogSupport {
  Logger.setDefaultLogLevel(LogLevel.INFO)
  
  val client = Http.client.newRPCClient("http://localhost:8080")
  
  val taskRPC = client.rpc[TaskRPC]
  
  try {
    // Test RPC calls
    info("Testing RPC client...")
    
    // Create a task via RPC
    val newTask = taskRPC.createTask("RPC Client Task", "Created from RPC client")
    info(s"Created task: ${newTask.title} (${newTask.id})")
    
    // List all tasks
    val tasks = taskRPC.listTasks()
    info(s"Found ${tasks.length} tasks:")
    tasks.foreach(task => info(s"  - ${task.title} (completed: ${task.completed})"))
    
    // Complete the task
    val completed = taskRPC.completeTask(newTask.id)
    info(s"Task completion result: $completed")
    
    // Get the updated task
    taskRPC.getTask(newTask.id) match {
      case Some(task) => info(s"Updated task status: completed = ${task.completed}")
      case None => warn("Task not found")
    }
    
  } finally {
    client.close()
  }
}
```

## Chapter 5: Advanced Features - Testing, Error Handling, and Frontend

### Testing with AirSpec

Create a test file `src/test/scala/taskmanager/TaskServiceSpec.scala`:

```scala
package taskmanager

import wvlet.airframe.*
import wvlet.airspec.*
import taskmanager.config.*

class TaskServiceSpec extends AirSpec {
  
  // Design for testing with mock dependencies
  override def design: Design = newDesign
    .bind[AppConfig].toInstance(AppConfig("Test App", "1.0.0-test"))
    .bind[TaskRepository].to[InMemoryTaskRepository]
    .bind[TaskService].toSingleton
  
  test("create and retrieve tasks") { (service: TaskService) =>
    val task = service.createTask("Test Task", "Test Description")
    
    task.title shouldBe "Test Task"
    task.description shouldBe "Test Description"
    task.completed shouldBe false
    
    service.getTask(task.id) shouldBe Some(task)
  }
  
  test("complete tasks") { (service: TaskService) =>
    val task = service.createTask("Completable Task", "To be completed")
    
    service.completeTask(task.id) match {
      case Some(completed) =>
        completed.completed shouldBe true
        completed.id shouldBe task.id
      case None =>
        fail("Task completion should succeed")
    }
  }
  
  test("list tasks in order") { (service: TaskService) =>
    val task1 = service.createTask("First", "First task")
    Thread.sleep(1) // Ensure different timestamps
    val task2 = service.createTask("Second", "Second task")
    
    val tasks = service.listTasks()
    tasks.length shouldBe 2
    tasks.head.title shouldBe "First"
    tasks.last.title shouldBe "Second"
  }
}
```

### Error Handling and Validation

```scala
package taskmanager

import wvlet.log.LogSupport

// Custom exceptions
case class TaskValidationException(message: String) extends Exception(message)
case class TaskNotFoundException(id: String) extends Exception(s"Task not found: $id")

// Enhanced service with validation
class EnhancedTaskService(
  repository: TaskRepository,
  config: AppConfig
) extends LogSupport {
  
  private def validateTask(title: String, description: String): Unit = {
    if (title.trim.isEmpty) {
      throw TaskValidationException("Task title cannot be empty")
    }
    if (title.length > 100) {
      throw TaskValidationException("Task title cannot exceed 100 characters")
    }
    if (description.length > 500) {
      throw TaskValidationException("Task description cannot exceed 500 characters")
    }
  }
  
  def createTask(title: String, description: String): Either[String, Task] = {
    try {
      validateTask(title, description)
      val task = Task(
        id = java.util.UUID.randomUUID().toString,
        title = title.trim,
        description = description.trim
      )
      repository.save(task)
      info(s"Created task: ${task.title}")
      Right(task)
    } catch {
      case e: TaskValidationException =>
        error(s"Validation failed: ${e.message}")
        Left(e.message)
    }
  }
  
  def getTask(id: String): Either[String, Task] = {
    repository.findById(id) match {
      case Some(task) => Right(task)
      case None => Left(s"Task not found: $id")
    }
  }
}
```

### Frontend with Scala.js (Basic Example)

For a complete frontend, you would set up a separate Scala.js module. Here's a basic structure:

**build.sbt** (add Scala.js support):

```scala
lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(
    libraryDependencies ++= Seq(
      "org.wvlet.airframe" %%% "airframe-codec" % AIRFRAME_VERSION
    )
  )
  .jsConfigure(_.enablePlugins(ScalaJSPlugin))

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

lazy val server = project
  .in(file("server"))
  .dependsOn(sharedJvm)
  .settings(
    // Server dependencies
    libraryDependencies ++= Seq(
      "org.wvlet.airframe" %% "airframe-http-netty" % AIRFRAME_VERSION
    )
  )

lazy val client = project
  .in(file("client"))
  .dependsOn(sharedJs)
  .enablePlugins(ScalaJSPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "org.wvlet.airframe" %%% "airframe-rx-html" % AIRFRAME_VERSION
    )
  )
```

**Frontend component** (client/src/main/scala/TaskApp.scala):

```scala
package taskmanager.client

import wvlet.airframe.rx.html.*
import wvlet.airframe.rx.html.all.*
import org.scalajs.dom

case class TaskViewModel(
  id: String,
  title: String,
  description: String,
  completed: Boolean
)

object TaskApp {
  def main(args: Array[String]): Unit = {
    val tasks = Rx.variable(List.empty[TaskViewModel])
    
    val app = div(
      h1("Task Manager"),
      
      // Task input form
      div(
        cls := "task-form",
        input(tpe := "text", placeholder := "Task title", id := "title-input"),
        input(tpe := "text", placeholder := "Description", id := "desc-input"),
        button(
          "Add Task",
          onclick := { () =>
            val title = dom.document.getElementById("title-input").asInstanceOf[dom.HTMLInputElement].value
            val desc = dom.document.getElementById("desc-input").asInstanceOf[dom.HTMLInputElement].value
            // Call backend API to create task
            // Update tasks list
          }
        )
      ),
      
      // Tasks list
      div(
        tasks.map { taskList =>
          ul(
            taskList.map { task =>
              li(
                span(cls := if (task.completed) "completed" else "", task.title),
                " - ",
                span(task.description),
                button("Complete", onclick := { () =>
                  // Call API to complete task
                })
              )
            }
          )
        }
      )
    )
    
    RxElement.mount(dom.document.getElementById("app"), app)
  }
}
```

## Chapter 6: Putting It All Together

### Complete Application Design

Here's how to structure a production-ready application:

```scala
package taskmanager

import wvlet.airframe.*
import wvlet.airframe.config.Config
import wvlet.airframe.control.Retry
import wvlet.airframe.http.HttpServer
import wvlet.airframe.http.netty.Netty
import wvlet.airframe.http.RxRouter
import wvlet.log.{LogLevel, Logger}
import taskmanager.config.*
import taskmanager.api.TaskController
import taskmanager.rpc.{TaskRPC, TaskRPCImpl}

object ProductionTaskManager extends App {
  // Production logging configuration
  Logger.setDefaultLogLevel(LogLevel.INFO)
  
  val config = Config.of[taskmanager.config.Config]
  
  // Production design with error handling and resilience
  val design = newDesign
    .bind[taskmanager.config.Config].toInstance(config)
    .bind[TaskRepository].to[InMemoryTaskRepository].onStart { repo =>
      info("Initializing task repository")
      // Could initialize database connections here
    }.onShutdown { repo =>
      info("Closing task repository")
      // Cleanup resources
    }
    .bind[TaskService].to[EnhancedTaskService].toSingleton
    .bind[TaskController].toSingleton
    .bind[TaskRPC].to[TaskRPCImpl]
    .add(
      Netty.server
        .withHost(config.server.host)
        .withPort(config.server.port)
        .withRouter(
          RxRouter
            .add[TaskController]
            .andThen(RxRouter.of[TaskRPC])
        )
        .design
    )
  
  // Graceful shutdown handling
  sys.addShutdownHook {
    info("Shutting down Task Manager...")
  }
  
  design.build[HttpServer] { server =>
    info(s"🚀 Task Manager started at http://${config.server.host}:${config.server.port}")
    info("Available endpoints:")
    info("  REST API: /api/tasks")
    info("  RPC API:  /rpc/taskmanager.rpc.TaskRPC")
    
    // Wait for termination
    server.waitServerTermination
  }
}
```

## Summary

This walkthrough demonstrates how Airframe's modules work together to build complete applications:

1. **airframe-log** provides structured, efficient logging with source location tracking
2. **airframe-di** manages dependencies and application lifecycle
3. **airframe-config** handles YAML-based configuration
4. **airframe-http** creates REST APIs with minimal boilerplate
5. **airframe-rpc** enables type-safe service-to-service communication
6. **airframe-codec** handles JSON/MessagePack serialization automatically
7. **airspec** provides powerful testing with dependency injection
8. **airframe-rx** (with Scala.js) enables reactive frontend development

### Key Benefits

- **Minimal Boilerplate**: Airframe handles serialization, routing, and DI automatically
- **Type Safety**: Strong typing across API boundaries with RPC
- **Testability**: Easy testing with dependency injection
- **Configuration**: YAML-based configuration with type safety
- **Observability**: Built-in logging with source location tracking
- **Scalability**: Modular design allows incremental adoption

### Next Steps

- Explore [specific module documentation](index.md#list-of-airframe-modules) for advanced features
- Check out [real-world examples](https://github.com/wvlet/airframe/tree/main/examples) in the repository
- Build your own application following this pattern!

The power of Airframe lies not just in individual modules, but in how they integrate seamlessly to reduce complexity while maintaining flexibility and type safety.