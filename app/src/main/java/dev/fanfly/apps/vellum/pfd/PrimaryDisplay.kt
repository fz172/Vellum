package dev.fanfly.apps.vellum.pfd

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.fanfly.apps.vellum.adhars.AdharsViewModel


@Composable
fun PrimaryDisplay(adharsViewModel: AdharsViewModel = hiltViewModel()) {

  val adharsData = adharsViewModel.adharsData.collectAsState()

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    ArtificialHorizon(
      pitch = adharsData.value.pitch,
      roll = adharsData.value.roll,
      modifier = Modifier.size(500.dp)
    )
  }
}


