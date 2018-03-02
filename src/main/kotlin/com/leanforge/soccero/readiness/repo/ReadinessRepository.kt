package com.leanforge.soccero.readiness.repo

import com.leanforge.soccero.readiness.domain.Readiness
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ReadinessRepository : MongoRepository<Readiness, String> {

    fun findAllByState(state: Readiness.State): Iterable<Readiness>
}