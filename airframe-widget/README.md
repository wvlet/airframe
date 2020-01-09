# airframe-widget


## Development


### Developing Scala.js Widgets


```
$ ./sbt 
> ~widgetJS/fastOptJS
```

Open another terminal and run:
```
$ browser-sync start --server airframe-widget/src/main/public --serveStatic airframe-widget/.js/target/scala-2.12 --files airframe-widget/.js/target/scala-2.12/airframe-widget-fastopt.js
```
