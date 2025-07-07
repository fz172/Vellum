package dev.fanfly.apps.vellum.nav

sealed class NavScreen(val routing: String) {

  object Login : NavScreen("login")
  object LoginError : NavScreen("login_error")

  object LogList : NavScreen("log_list")
  object AddLog : NavScreen("add_log")
}