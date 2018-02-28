package com.leanforge.soccero.queue.repo

import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.queue.domain.QueueConfig
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface QueueConfigRepository : MongoRepository<QueueConfig, Competition> {
}