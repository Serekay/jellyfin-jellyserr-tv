package com.tailscale.ipn.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tailscale.ipn.ui.viewModel.PingViewModel

@Composable
fun PingView(model: PingViewModel? = null) {
	Column(
		verticalArrangement = Arrangement.spacedBy(12.dp),
		modifier = Modifier.fillMaxWidth()
	) {
		Text(
			text = "Ping-Statistiken sind in dieser eingebetteten Version deaktiviert.",
			style = MaterialTheme.typography.bodyMedium,
			fontWeight = FontWeight.SemiBold,
			textAlign = TextAlign.Start
		)
		Text(
			text = "Die VPN-Funktion bleibt davon unberührt.",
			style = MaterialTheme.typography.bodySmall,
			textAlign = TextAlign.Start
		)
	}
}

fun Double.roundedString(decimals: Int): String = "%.${decimals}f".format(this)
