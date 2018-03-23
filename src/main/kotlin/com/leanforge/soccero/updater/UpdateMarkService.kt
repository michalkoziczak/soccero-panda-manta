package com.leanforge.soccero.updater

import com.leanforge.soccero.updater.domain.UpdateMark
import com.leanforge.soccero.updater.repo.UpdateMarkRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

interface UpdateMarkService {
    fun markForUpdate(type: UpdateMark.Type)
    fun markUpdated(type: UpdateMark.Type)
    fun needsUpdate(type: UpdateMark.Type): Boolean
}

@Service
class DefaultUpdateMarkService @Autowired constructor(val updateMarkRepository: UpdateMarkRepository) : UpdateMarkService {

    override fun markForUpdate(type: UpdateMark.Type) {
        updateMarkRepository.save(UpdateMark(type))
    }

    override fun markUpdated(type: UpdateMark.Type) {
        updateMarkRepository.delete(type)
    }

    override fun needsUpdate(type: UpdateMark.Type): Boolean {
        return updateMarkRepository.exists(type)
    }
}