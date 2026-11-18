package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.GameRoom
import com.example.model.PublicProfile
import com.example.model.UiState
import com.example.model.UserSettings
import com.example.model.VisualizerProfile
import com.example.ui.components.VicdaruBrandHeader
import com.example.viewmodel.VicdaruMainViewModel

@Composable
fun MinigamesHubScreen(
    viewModel: VicdaruMainViewModel,
    profile: PublicProfile?,
    settings: UserSettings?,
    visualizerProfilesState: UiState<List<VisualizerProfile>>,
    friendProfiles: List<PublicProfile>,
    invitedRooms: List<GameRoom>,
    activeRoom: GameRoom?,
    currentUserId: String,
    initialTab: Int = 0,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }

    LaunchedEffect(activeRoom?.gameType) {
        if (activeRoom != null) {
            selectedTab = if (activeRoom.gameType == "MUSIC_FOCUS") 1 else 0
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VicdaruBrandHeader(
                title = "Arcade & Enfoque",
                subtitle = "Minijuegos anime y estudio musical adaptativo"
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = {
                        if (selectedTab != 0) viewModel.haptics.navTabSwitch()
                        selectedTab = 0
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { Icon(Icons.Filled.Terrain, contentDescription = null) },
                    modifier = Modifier.testTag("tab_climber_game")
                ) {
                    Text("Escalada")
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = {
                        if (selectedTab != 1) viewModel.haptics.navTabSwitch()
                        selectedTab = 1
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { Icon(Icons.Filled.GraphicEq, contentDescription = null) },
                    modifier = Modifier.testTag("tab_music_focus")
                ) {
                    Text("Música y Foco")
                }
            }

            if (invitedRooms.isNotEmpty() && activeRoom == null) {
                val invite = invitedRooms.first()
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Invitación de ${invite.hostName}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = if (invite.gameType == "CLIMBER") "Duelo de Escalada • Sala ${invite.roomCode}"
                                else "Sesión Enfoque Musical • Sala ${invite.roomCode}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { viewModel.joinMultiplayerRoom(invite.roomCode) }) {
                            Text("Aceptar")
                        }
                    }
                }
            }
        }

        AnimatedContent(
            targetState = selectedTab,
            label = "minigameTabSwitch",
            modifier = Modifier.weight(1f)
        ) { tab ->
            if (tab == 0) {
                ClimberGameScreen(
                    viewModel = viewModel,
                    profile = profile,
                    friendProfiles = friendProfiles,
                    activeRoom = activeRoom,
                    currentUserId = currentUserId
                )
            } else {
                MusicFocusScreen(
                    viewModel = viewModel,
                    profile = profile,
                    settings = settings,
                    visualizerProfilesState = visualizerProfilesState,
                    friendProfiles = friendProfiles,
                    activeRoom = activeRoom,
                    currentUserId = currentUserId
                )
            }
        }
    }
}
