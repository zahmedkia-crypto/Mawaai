package com.mawaai.love.app.design.data.repository

import android.net.Uri
import com.mawaai.love.app.design.domain.model.DesignSession
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DesignSessionStore @Inject constructor() {

    private val sessions = ConcurrentHashMap<String, DesignSession>()

    fun create(session: DesignSession = DesignSession(id = UUID.randomUUID().toString())): DesignSession {
        sessions[session.id] = session
        return session
    }

    fun get(id: String): DesignSession? = sessions[id]

    fun update(id: String, transform: (DesignSession) -> DesignSession): DesignSession? {
        val current = sessions[id] ?: return null
        val updated = transform(current)
        sessions[id] = updated
        return updated
    }

    fun setInputImage(id: String, uri: Uri): DesignSession? =
        update(id) { it.copy(inputImageUri = uri) }

    fun setProcessedImage(id: String, uri: Uri): DesignSession? =
        update(id) { it.copy(processedImageUri = uri) }

    fun setSelectedTemplate(id: String, templateId: String): DesignSession? =
        update(id) { it.copy(selectedTemplateId = templateId) }

    fun clear(id: String) {
        sessions.remove(id)
    }
}
