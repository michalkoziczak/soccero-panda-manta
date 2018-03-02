package com.leanforge.soccero.help.domain

data class CommandManual(val name: String,
                         val description: String,
                         val args: List<CommandArg> = emptyList(),
                         val examples: List<CommandExample> = emptyList(),
                         val isReaction: Boolean = false
) {

    fun slackLabel() : String {
        if (isReaction) {
            return ":pushpin: ${name} - ${description}"
        }
        return ":pushpin: `${name}${argList()}` - ${description}${explainArgs()}${exampleList()}"
    }

    fun argList() : String {
        if (args.isEmpty()) {
            return ""
        }
        return " " + args.joinToString(" ") { "[${it.name}]" }
    }

    fun explainArgs() : String {
        if (args.isEmpty()) {
            return ""
        }
        return "\n\tArgs: \n" + args.joinToString("\n") { "\t:small_orange_diamond: ${it.name} `/${it.pattern}/g` -> _${it.description}_" }
    }

    fun exampleList() : String {
        if (examples.isEmpty()) {
            return ""
        }
        return "\n\tExamples: \n" + examples.joinToString("\n") { "\t:small_orange_diamond: ${it.value} -> _${it.description}_" }
    }
}