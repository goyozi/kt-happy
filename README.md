# Happy Lang

This is a proof-of-concept implementation of the Happy programming language, written in Kotlin.

## Installation & Usage

The easiest way to start using the language now is to use the provided Docker image:

```shell
alias happy="docker run -it -v `pwd`/:/src -t ghcr.io/goyozi/happy"
alias happy-upgrade="docker pull ghcr.io/goyozi/happy"
```

To run a program, go to your project's directory and run:

```shell
happy file.happy
```

To get the newest version of happy, run:

```shell
happy-upgrade
```
