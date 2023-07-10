# ModelInstancedRendering

This repo provides a [libGDX](https://libgdx.com/) v1.12 demonstration of instanced 3D rendering.  The desktop version will render
1 million 3D cubes.  GWT and android version will render 25 thousand cubes.

![Capture](https://github.com/antzGames/ModelInstancedRendering/assets/10563814/4114394c-ed84-4e3f-977f-047b536462c0)

Since libGDX v1.12 now has WebGL 2 support
for GWT, you can now use instancing in GWT projects.

Tested on desktop, android, and GWT.  iOS project is included but not tested.

You can run the demonstration, or download the desktop and android binaries here: [https://antzgames.itch.io/modelinstancedrendering](https://antzgames.itch.io/modelinstancedrendering)

## Voxel Terrain

I have a method to create a very simple minecraft terrain included in the code.  You just need to
comment out original + uncomment voxel method in 
[ModelInstancedRenderingScreen.java](https://github.com/antzGames/ModelInstancedRendering/blob/master/core/src/main/java/com/antz/instanced/ModelInstancedRenderingScreen.java) as shown below: 

```java
        //createBoxField(); // regular box field
        createVoxelTerrain(); // simple minecraft terrain
```

## Videos

https://github.com/antzGames/ModelInstancedRendering/assets/10563814/3cfea22b-a343-4b9f-ac30-b8980115080b

https://github.com/antzGames/ModelInstancedRendering/assets/10563814/f1324e7e-9421-4973-8ad6-f547baec6ab7

## Learn More about OpenGL Instancing

Based on: [ModelInstancedRenderingTest.java](https://github.com/libgdx/libgdx/blob/master/tests/gdx-tests/src/com/badlogic/gdx/tests/gles3/ModelInstancedRenderingTest.java)

Learn OpenGL Instancing: [https://learnopengl.com/Advanced-OpenGL/Instancing](https://learnopengl.com/Advanced-OpenGL/Instancing)

## Project Structure

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/tommyettinger/gdx-liftoff).

This project was generated with a template including simple application launchers and a main class extending `Game` that sets the first screen.

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3.
- `android`: Android mobile platform. Needs Android SDK.
- `ios`: iOS mobile platform using RoboVM.
- `html`: Web platform using GWT and WebGL. Supports only Java projects.

## Gradle

This project uses [Gradle](http://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `android:lint`: performs Android project validation.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `html:dist`: compiles GWT sources. The compiled application can be found at `html/build/dist`: you can use any HTTP server to deploy it.
- `html:superDev`: compiles GWT sources and runs the application in SuperDev mode. It will be available at [localhost:8080/html](http://localhost:8080/html). Use only during development.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
