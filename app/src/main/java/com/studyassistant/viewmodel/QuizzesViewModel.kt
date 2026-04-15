package com.studyassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyassistant.domain.model.Quiz
import com.studyassistant.repository.LocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuizzesViewModel @Inject constructor(
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _quizzes = MutableStateFlow<List<Quiz>>(emptyList())
    val quizzes: StateFlow<List<Quiz>> = _quizzes.asStateFlow()

    init {
        viewModelScope.launch {
            localRepository.getCachedQuizzes().collect { list ->
                _quizzes.value = list
            }
        }
    }
}

