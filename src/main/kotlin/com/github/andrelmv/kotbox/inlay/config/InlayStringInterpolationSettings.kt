package com.github.andrelmv.kotbox.inlay.config

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service
@State(name = "InlayStringInterpolationState", storages = [Storage("inlay-string-hint.xml")])
class InlayStringInterpolationSettings :
    SimplePersistentStateComponent<InlayStringInterpolationState>(InlayStringInterpolationState())

class InlayStringInterpolationState : BaseState() {
    var withStringInterpolationHint: Boolean by property(true)
    var withStringConstantHint: Boolean by property(true)
}
