# Contributing Guide

## Setup

### Install git

### Install Java, Scala and sbt

1. Download Coursier:

```
$ curl -fLo cs https://git.io/coursier-cli-"$(uname | tr LD ld)"
$ chmod +x cs
```

2. Run:

```
$ ./cs setup --yes
```

### Install Visual Studio Code and Metals

1. Download Visual Studio Code from https://code.visualstudio.com/Download.
2. Install and launch Visual Studio Code.
3. In the extension panel, find `Scala (Metals)` and install it.


## Compile and Run

1. Clone the repository with git

```
git clone git@github.com:mau-game/mau.git
```

2. In the repository, start the sbt shell

```
sbtn
```

3. Start the server

```
sbt:mau> server / run
```

4. Open http://localhost:8080/
