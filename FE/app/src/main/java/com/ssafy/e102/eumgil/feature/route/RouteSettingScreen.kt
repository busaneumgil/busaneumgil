package com.ssafy.e102.eumgil.feature.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.common.model.PlaceholderAction
import com.ssafy.e102.eumgil.core.designsystem.component.layout.EumPlaceholderScaffold
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.model.PlaceDestination

@Composable
fun RouteSettingScreen(
    selectedDestination: PlaceDestination?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EumPlaceholderScaffold(
        title = stringResource(id = R.string.route_setting_screen_title),
        description =
            if (selectedDestination == null) {
                stringResource(id = R.string.route_setting_screen_description_empty)
            } else {
                stringResource(
                    id = R.string.route_setting_screen_description_with_destination,
                    selectedDestination.name,
                )
            },
        featurePath = stringResource(id = R.string.feature_path_route_setting),
        actions =
            listOf(
                PlaceholderAction(
                    label = stringResource(id = R.string.action_go_map),
                    onClick = onNavigateBack,
                    isPrimary = true,
                ),
            ),
        modifier = modifier,
        content = {
            RouteSettingDestinationCard(selectedDestination = selectedDestination)
        },
    )
}

@Composable
private fun RouteSettingDestinationCard(selectedDestination: PlaceDestination?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            Text(
                text = stringResource(id = R.string.route_setting_destination_section_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            if (selectedDestination == null) {
                Text(
                    text = stringResource(id = R.string.route_setting_destination_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            RouteSettingDestinationField(
                label = stringResource(id = R.string.route_setting_destination_name_label),
                value = selectedDestination.name,
            )
            RouteSettingDestinationField(
                label = stringResource(id = R.string.route_setting_destination_address_label),
                value =
                    selectedDestination.address
                        ?: stringResource(id = R.string.route_setting_destination_address_empty),
            )
            RouteSettingDestinationField(
                label = stringResource(id = R.string.route_setting_destination_coordinate_label),
                value =
                    stringResource(
                        id = R.string.route_setting_destination_coordinate_value,
                        selectedDestination.latitude,
                        selectedDestination.longitude,
                    ),
            )
        }
    }
}

@Composable
private fun RouteSettingDestinationField(
    label: String,
    value: String,
) {
    Text(
        text = stringResource(id = R.string.route_setting_destination_field_value, label, value),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
