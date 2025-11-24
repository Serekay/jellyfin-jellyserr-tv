package org.jellyfin.androidtv.ui.jellyseerr

import android.view.SoundEffectConstants
import android.widget.ImageView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.JellyseerrCast
import org.jellyfin.androidtv.data.repository.JellyseerrGenreSlider
import org.jellyfin.androidtv.data.repository.JellyseerrRequest
import org.jellyfin.androidtv.data.repository.JellyseerrSearchItem
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.AsyncImage

@Composable
fun JellyseerrGenreCard(
	genre: JellyseerrGenreSlider,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	focusRequester: FocusRequester? = null,
	onFocus: (() -> Unit)? = null,
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isFocused by interactionSource.collectIsFocusedAsState()
	val view = LocalView.current
	val scale = if (isFocused) 1.05f else 1f

	LaunchedEffect(isFocused) {
		if (isFocused) {
			view.playSoundEffect(SoundEffectConstants.NAVIGATION_DOWN)
			onFocus?.invoke()
		}
	}

	Box(
		modifier = modifier
			.width(175.dp)
			.fillMaxHeight()
			.clickable(onClick = onClick, interactionSource = interactionSource, indication = null)
			.then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
			.focusable(interactionSource = interactionSource)
			.graphicsLayer(
				scaleX = scale,
				scaleY = scale,
			)
			.padding(vertical = 4.dp),
		contentAlignment = Alignment.Center,
	) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.clip(RoundedCornerShape(8.dp))
				.border(
					width = if (isFocused) 3.dp else 1.dp,
					color = if (isFocused) Color.White else Color(0xFF444444),
					shape = RoundedCornerShape(8.dp),
				),
			contentAlignment = Alignment.Center,
		) {
			if (!genre.backdropUrl.isNullOrBlank()) {
				AsyncImage(
					modifier = Modifier
						.fillMaxSize()
						.graphicsLayer(alpha = 0.7f),
					url = genre.backdropUrl,
					aspectRatio = 16f / 9f,
					scaleType = ImageView.ScaleType.CENTER_CROP,
				)
			} else {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.background(Color(0xFF2A2A2A)),
				)
			}

			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(
						Brush.verticalGradient(
							colors = listOf(
								Color.Black.copy(alpha = 0.5f),
								Color.Black.copy(alpha = 0.8f),
							)
						)
					),
			)

			Text(
				text = genre.name,
				color = Color.White,
				fontSize = 14.sp,
				fontWeight = FontWeight.Bold,
				textAlign = TextAlign.Center,
				modifier = Modifier.padding(8.dp),
			)
		}
	}
}

@Composable
fun JellyseerrViewAllCard(
	onClick: () -> Unit,
	posterUrls: List<String?> = emptyList(),
	focusRequester: FocusRequester? = null,
	onFocus: (() -> Unit)? = null,
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isFocused by interactionSource.collectIsFocusedAsState()
	val scale = if (isFocused) 1.1f else 1f
	val view = LocalView.current

	LaunchedEffect(isFocused) {
		if (isFocused) {
			view.playSoundEffect(SoundEffectConstants.NAVIGATION_DOWN)
			onFocus?.invoke()
		}
	}

	Column(
		modifier = Modifier
			.width(150.dp)
			.fillMaxHeight()
			.clickable(onClick = onClick, interactionSource = interactionSource, indication = null)
			.then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
			.focusable(interactionSource = interactionSource)
			.graphicsLayer(
				scaleX = scale,
				scaleY = scale,
			)
			.padding(vertical = 4.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(200.dp)
				.clip(RoundedCornerShape(12.dp))
				.border(
					width = if (isFocused) 4.dp else 1.dp,
					color = if (isFocused) Color.White else Color(0xFF888888),
					shape = RoundedCornerShape(12.dp),
				),
		) {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(4.dp),
				verticalArrangement = Arrangement.spacedBy(4.dp),
			) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.weight(1f),
					horizontalArrangement = Arrangement.spacedBy(4.dp),
				) {
					posterUrls.getOrNull(0)?.let { posterUrl ->
						AsyncImage(
							url = posterUrl,
							scaleType = ImageView.ScaleType.CENTER_CROP,
							modifier = Modifier
								.weight(1f)
								.fillMaxHeight()
								.clip(RoundedCornerShape(4.dp)),
						)
					} ?: Box(
						modifier = Modifier
							.weight(1f)
							.fillMaxHeight()
							.clip(RoundedCornerShape(4.dp))
							.background(Color(0xFF2A2A2A)),
					)
					posterUrls.getOrNull(1)?.let { posterUrl ->
						AsyncImage(
							url = posterUrl,
							scaleType = ImageView.ScaleType.CENTER_CROP,
							modifier = Modifier
								.weight(1f)
								.fillMaxHeight()
								.clip(RoundedCornerShape(4.dp)),
						)
					} ?: Box(
						modifier = Modifier
							.weight(1f)
							.fillMaxHeight()
							.clip(RoundedCornerShape(4.dp))
							.background(Color(0xFF2A2A2A)),
					)
				}
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.weight(1f),
					horizontalArrangement = Arrangement.spacedBy(4.dp),
				) {
					posterUrls.getOrNull(2)?.let { posterUrl ->
						AsyncImage(
							url = posterUrl,
							scaleType = ImageView.ScaleType.CENTER_CROP,
							modifier = Modifier
								.weight(1f)
								.fillMaxHeight()
								.clip(RoundedCornerShape(4.dp)),
						)
					} ?: Box(
						modifier = Modifier
							.weight(1f)
							.fillMaxHeight()
							.clip(RoundedCornerShape(4.dp))
							.background(Color(0xFF2A2A2A)),
					)
					posterUrls.getOrNull(3)?.let { posterUrl ->
						AsyncImage(
							url = posterUrl,
							scaleType = ImageView.ScaleType.CENTER_CROP,
							modifier = Modifier
								.weight(1f)
								.fillMaxHeight()
								.clip(RoundedCornerShape(4.dp)),
						)
					} ?: Box(
						modifier = Modifier
							.weight(1f)
							.fillMaxHeight()
							.clip(RoundedCornerShape(4.dp))
							.background(Color(0xFF2A2A2A)),
					)
				}
			}

			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(Color.Black.copy(alpha = 0.5f)),
			)

			Column(
				modifier = Modifier.fillMaxSize(),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.Center,
			) {
				Box(
					modifier = Modifier
						.size(32.dp)
						.clip(CircleShape)
						.background(Color.White),
					contentAlignment = Alignment.Center,
				) {
					androidx.compose.material.Icon(
						imageVector = Icons.Filled.ArrowForward,
						contentDescription = null,
						tint = Color.Black,
						modifier = Modifier.size(20.dp),
					)
				}

				Spacer(modifier = Modifier.height(8.dp))

				Text(
					text = stringResource(R.string.jellyseerr_view_more),
					color = Color.White,
					fontSize = 12.sp,
					fontWeight = FontWeight.Medium,
				)
			}
		}
	}
}

@Composable
fun JellyseerrSearchCard(
	item: JellyseerrSearchItem,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	focusRequester: FocusRequester? = null,
	onFocus: (() -> Unit)? = null,
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isFocused by interactionSource.collectIsFocusedAsState()
	val scale = if (isFocused) 1.1f else 1f
	val view = LocalView.current

	LaunchedEffect(isFocused) {
		if (isFocused) {
			view.playSoundEffect(SoundEffectConstants.NAVIGATION_DOWN)
			onFocus?.invoke()
		}
	}

	Column(
		modifier = modifier
			.width(150.dp)
			.fillMaxSize()
			.clickable(onClick = onClick, interactionSource = interactionSource, indication = null)
			.then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
			.focusable(interactionSource = interactionSource)
			.graphicsLayer(
				scaleX = scale,
				scaleY = scale,
			)
			.padding(vertical = 4.dp),
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(200.dp)
				.clip(RoundedCornerShape(12.dp))
				.border(
					width = if (isFocused) 4.dp else 1.dp,
					color = if (isFocused) Color.White else Color(0xFF888888),
					shape = RoundedCornerShape(12.dp),
				),
		) {
			if (item.posterPath.isNullOrBlank()) {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.background(Color(0xFF333333)),
					contentAlignment = Alignment.Center,
				) {
					Image(
						imageVector = ImageVector.vectorResource(id = R.drawable.ic_clapperboard),
						contentDescription = null,
						modifier = Modifier.size(48.dp),
						colorFilter = ColorFilter.tint(Color(0xFF888888)),
					)
				}
			} else {
				AsyncImage(
					modifier = Modifier.fillMaxSize(),
					url = item.posterPath,
					aspectRatio = 2f / 3f,
					scaleType = ImageView.ScaleType.CENTER_CROP,
				)
			}

			val hasPendingRequest = item.requestStatus != null && item.requestStatus != 5

			when {
				hasPendingRequest -> {
					Box(
						modifier = Modifier
							.align(Alignment.TopEnd)
							.padding(6.dp)
							.clip(RoundedCornerShape(999.dp))
							.background(Color(0xFFAA5CC3)),
					) {
						Image(
							imageVector = ImageVector.vectorResource(id = R.drawable.ic_time),
							contentDescription = null,
							modifier = Modifier
								.padding(4.dp)
								.size(16.dp),
						)
					}
				}
				item.isPartiallyAvailable -> {
					Box(
						modifier = Modifier
							.align(Alignment.TopEnd)
							.padding(6.dp)
							.clip(RoundedCornerShape(999.dp))
							.background(Color(0xFF2E7D32)),
					) {
						Image(
							imageVector = ImageVector.vectorResource(id = R.drawable.ic_decrease),
							contentDescription = null,
							modifier = Modifier
								.padding(4.dp)
								.size(16.dp),
						)
					}
				}
				item.isAvailable -> {
					Box(
						modifier = Modifier
							.align(Alignment.TopEnd)
							.padding(6.dp)
							.clip(RoundedCornerShape(999.dp))
							.background(Color(0xFF2E7D32)),
					) {
						Image(
							imageVector = ImageVector.vectorResource(id = R.drawable.ic_check),
							contentDescription = null,
							modifier = Modifier
								.padding(4.dp)
								.size(16.dp),
						)
					}
				}
			}
		}

		Spacer(modifier = Modifier.size(4.dp))

		Text(
			text = item.title,
			color = JellyfinTheme.colorScheme.onBackground,
			maxLines = 2,
			overflow = TextOverflow.Ellipsis,
			modifier = Modifier.padding(horizontal = 4.dp),
		)
	}
}

@Composable
fun JellyseerrRequestRow(
	request: JellyseerrRequest,
	onClick: () -> Unit,
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isFocused by interactionSource.collectIsFocusedAsState()
	val scale = if (isFocused) 1.1f else 1f

	Column(
		modifier = Modifier
			.width(150.dp)
			.fillMaxHeight()
			.padding(vertical = 4.dp)
			.clickable(onClick = onClick, interactionSource = interactionSource, indication = null)
			.focusable(interactionSource = interactionSource)
			.graphicsLayer(
				scaleX = scale,
				scaleY = scale,
			),
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(200.dp)
				.clip(RoundedCornerShape(12.dp))
				.border(
					width = if (isFocused) 4.dp else 1.dp,
					color = if (isFocused) Color.White else Color(0xFF888888),
					shape = RoundedCornerShape(12.dp),
				),
		) {
			AsyncImage(
				modifier = Modifier.fillMaxSize(),
				url = request.posterPath,
				aspectRatio = 2f / 3f,
				scaleType = ImageView.ScaleType.CENTER_CROP,
			)
		}

		Spacer(modifier = Modifier.size(4.dp))

		Text(
			text = request.title,
			color = JellyfinTheme.colorScheme.onBackground,
			maxLines = 2,
			overflow = TextOverflow.Ellipsis,
			modifier = Modifier.padding(horizontal = 4.dp),
		)
	}
}

@Composable
fun JellyseerrCastCard(
	person: JellyseerrCast,
	onClick: () -> Unit,
	focusRequester: FocusRequester? = null,
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isFocused by interactionSource.collectIsFocusedAsState()
	val scale = if (isFocused) 1.05f else 1f
	val view = LocalView.current

	LaunchedEffect(isFocused) {
		if (isFocused) {
			view.playSoundEffect(SoundEffectConstants.NAVIGATION_DOWN)
		}
	}

	val baseModifier = Modifier
		.width(120.dp)
		.padding(vertical = 4.dp)
		.clickable(onClick = onClick, interactionSource = interactionSource, indication = null)
		.focusable(interactionSource = interactionSource)
		.graphicsLayer(
			scaleX = scale,
			scaleY = scale,
		)

	val modifier = if (focusRequester != null) baseModifier.focusRequester(focusRequester) else baseModifier

	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = modifier,
	) {
		Box(
			modifier = Modifier
				.size(80.dp)
				.clip(CircleShape)
				.border(
					width = if (isFocused) 4.dp else 1.dp,
					color = if (isFocused) Color.White else Color(0xFF888888),
					shape = CircleShape,
				),
		) {
			AsyncImage(
				modifier = Modifier.fillMaxSize(),
				url = person.profilePath,
				aspectRatio = 1f,
				scaleType = ImageView.ScaleType.CENTER_CROP,
			)
		}

		Spacer(modifier = Modifier.size(4.dp))

		Text(
			text = person.name,
			color = JellyfinTheme.colorScheme.onBackground,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
		)

		person.character?.takeIf { it.isNotBlank() }?.let { role ->
			Text(
				text = role,
				color = JellyfinTheme.colorScheme.onBackground,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
			)
		}
	}
}

@Composable
fun JellyseerrCompanyCard(
	name: String,
	logoUrl: String?,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	focusRequester: FocusRequester? = null,
	onFocus: (() -> Unit)? = null,
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isFocused by interactionSource.collectIsFocusedAsState()
	val scale = if (isFocused) 1.05f else 1f
	val view = LocalView.current

	LaunchedEffect(isFocused) {
		if (isFocused) {
			view.playSoundEffect(SoundEffectConstants.NAVIGATION_DOWN)
			onFocus?.invoke()
		}
	}

	Box(
		modifier = modifier
			.width(175.dp)
			.fillMaxHeight()
			.clickable(onClick = onClick, interactionSource = interactionSource, indication = null)
			.then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
			.focusable(interactionSource = interactionSource)
			.graphicsLayer(
				scaleX = scale,
				scaleY = scale,
			)
			.padding(vertical = 4.dp),
		contentAlignment = Alignment.Center,
	) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.clip(RoundedCornerShape(8.dp))
				.border(
					width = if (isFocused) 3.dp else 1.dp,
					color = if (isFocused) Color.White else Color(0xFF444444),
					shape = RoundedCornerShape(8.dp),
				),
			contentAlignment = Alignment.Center,
		) {
			if (!logoUrl.isNullOrBlank()) {
				AsyncImage(
					modifier = Modifier
						.fillMaxSize()
						.padding(15.dp),
					url = logoUrl,
					aspectRatio = 16f / 9f,
					scaleType = ImageView.ScaleType.CENTER_INSIDE,
				)
			} else {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.background(Color(0xFF1A1A1A)),
				)
			}

			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(
						Brush.verticalGradient(
							colors = listOf(
								Color.Black.copy(alpha = 0.2f),
								Color.Black.copy(alpha = 0.8f),
							),
						)
					),
			)
		}
	}
}
