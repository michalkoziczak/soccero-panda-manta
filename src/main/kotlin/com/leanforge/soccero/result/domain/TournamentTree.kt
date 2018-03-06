package com.leanforge.soccero.result.domain

data class TournamentTree(
        var leagueName: String?,
        var competition: String?,
        var nodes: List<TournamentTreeNode>?
)