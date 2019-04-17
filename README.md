# star-system

Modelling of motion relay control with graphical view

## [See on youtube](https://www.youtube.com/watch?v=M8uZXrG1V4E)

## Overview

This demo application shows an ability of rocket motion relay control, with lots of simplifications, but only working algorithm.

![alt text](https://user-images.githubusercontent.com/10473034/52907315-6c86a480-3270-11e9-90c6-4ff782d7950a.png "Screenshot")

## Setup

To get an interactive development environment run:

    lein figwheel

This will auto compile, open your default browser with projects url
and send all changes to the browser without the need to reload.
After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

To create a production build run:

    lein do clean, cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL. 

## License

Copyright © 2019

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.

