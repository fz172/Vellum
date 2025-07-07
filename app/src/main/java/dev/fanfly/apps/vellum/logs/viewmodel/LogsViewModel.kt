package dev.fanfly.apps.vellum.logs.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fanfly.apps.vellum.logs.repository.LogsRepository
import dev.fanfly.apps.vellum.proto.LogEntry
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


@HiltViewModel
class LogsViewModel @Inject internal constructor(private val logsRepository: LogsRepository) :
  ViewModel() {

  private val _logs = MutableStateFlow(listOf<LogEntry>())
  val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

  fun addLog(logEntry: LogEntry) {
    logsRepository.addLog(logEntry)
    _logs.update { logs -> logsRepository.logs }
  }

}