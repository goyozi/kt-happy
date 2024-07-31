package happy

val String.loc
    get(): Loc {
        val startEnd = this.split("-")
        val startLinePos = startEnd[0].split(":")
        val endLinePos = startEnd[1].split(":")
        return Loc(startLinePos[0].toInt(), startLinePos[1].toInt(), endLinePos[0].toInt(), endLinePos[1].toInt())
    }