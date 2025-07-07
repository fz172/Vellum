package dev.fanfly.apps.vellum.logs.repository

import dev.fanfly.apps.vellum.proto.LogEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogsRepository @Inject internal constructor() {

  private val _logs = mutableListOf<LogEntry>()
  val logs: List<LogEntry>
    get() = _logs

  fun addLog(log: LogEntry) {
    _logs.add(log)
  }
}