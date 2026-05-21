package com.mawaai.love.app.design.presentation.flow

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.mawaai.love.app.design.data.repository.DesignSessionStore
import com.mawaai.love.app.design.domain.model.DesignSession
import com.mawaai.love.app.design.domain.model.InputMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class InputMethodViewModel @Inject constructor(
    private val sessionStore: DesignSessionStore
) : ViewModel() {

    fun createSession(
        categoryId: String?,
        subTypeId: String?,
        method: InputMethod,
        isConverterFlow: Boolean
    ): String {
        val session = sessionStore.create(
            DesignSession(
                id = UUID.randomUUID().toString(),
                categoryId = categoryId,
                subTypeId = subTypeId,
                inputMethod = method,
                isConverterFlow = isConverterFlow
            )
        )
        return session.id
    }

    fun setInputUri(sessionId: String, uri: Uri) {
        sessionStore.setInputImage(sessionId, uri)
    }
}
