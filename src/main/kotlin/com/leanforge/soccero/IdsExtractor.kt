package com.leanforge.soccero

import java.util.regex.Pattern

object IdsExtractor {

    private val idsPattern: Pattern = Pattern.compile("<@([^>]+)>")


    fun extractIds(ids: String) : List<String> {
        val ret = mutableListOf<String>()
        val matcher = idsPattern.matcher(ids)

        while (matcher.find()) {
            ret.add(matcher.group(1))
        }

        return ret
    }
}