package com.example.cipod_final.indoor

import androidx.lifecycle.ViewModel

/** Retains authoring drafts across configuration changes without any saved-state serialization. */
class FloorPlanEditorViewModel(developerEnabled: Boolean) : ViewModel() {
    val editor = FloorPlanEditorState(developerEnabled)
}
