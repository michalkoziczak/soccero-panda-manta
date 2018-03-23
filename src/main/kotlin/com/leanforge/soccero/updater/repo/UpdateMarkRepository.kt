package com.leanforge.soccero.updater.repo

import com.leanforge.soccero.updater.domain.UpdateMark
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UpdateMarkRepository : MongoRepository<UpdateMark, UpdateMark.Type>