# airframe-rx-widget

Reactive Widget Library for Scala.js

## Development


### Developing Scala.js Widgets

```
$ ./sbt 
> ~widgetJS/fastOptJS
```

Open another terminal and run:
```
$ npm install -g browser-sync

$ browser-sync start --server airframe-rx-widget/src/main/public --serveStatic airframe-rx-widget/.js/target/scala-2.12 --files airframe-rx-widget/.js/target/scala-2.12/airframe-rx-widget-fastopt.js
```

It will refresh the browser automatically.


