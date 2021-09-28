# Python Parameter Inlay Putter

This plugin will add inlay hints to the function calls in Python.
Compatible with PyCharm IDE.

## Installation
- download jar file from releases
- open Settings -> Plugins
- first gear icon on the top and then click "Install Plugin from Disk..."

It is possible to disable inlay hints in Settings -> Editor -> Inlay Hints -> Python

Example of what does it look like before/after:
![](./img/before-after.jpg)

## TODO

- add unit tests
- do not generate anything inside external libs code
- add blacklist for builtins (partially done)
- ignore "self" param in case of class (partially done)
- do not generate anything for named params such as: func(name="value") - done