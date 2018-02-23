package com.leanforge.soccero.team.repo

import com.leanforge.soccero.team.domain.TeamExclusion
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TeamExclusionRepository : MongoRepository<TeamExclusion, UUID> {

    fun findAllByLeagueName(leagueName: String) : List<TeamExclusion>
}