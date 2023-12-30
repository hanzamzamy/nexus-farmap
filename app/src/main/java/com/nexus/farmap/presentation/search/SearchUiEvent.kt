package com.nexus.farmap.presentation.search

sealed class SearchUiEvent{
    object SearchSuccess: SearchUiEvent()
    object SearchInvalid: SearchUiEvent()
}
