# skybar
Skybar: Live code coverage engine

Byte code transformation:
* Learn byte code (HelloWorldByteCode)
* Learn ASM code (HelloWorldAsm)
* Complete the LineCountingMethodVisitor

Registry:
* Complete the SkybarRegistry

HTML application
* Create the application!
* List source files, coverage % in a table?
* Select source file, show source coverage
* Collect snapshots of coverage
* Compare before/after snapshots

Live updates
* Subclass Jetty's WebSocketServlet and add it to WebServer
* Detect changed classes and send push them as JSON to clients
* Update client to listen for Websocket push, then merge class counts and the update view

Features:
* Configure package prefix
* Record and show time of last line visit
* Create a Maven plugin for configuring -javaAgent for Maven ${argLine} for Surefire / unit tests / jetty:run-forked (Usability)

Advanced:
* Use InvokeDynamic to reduce lookup time for LongAdder (Performance)
* Attach to a live process and retransform already loaded classes (Attach API)
* Support multiple class loaders