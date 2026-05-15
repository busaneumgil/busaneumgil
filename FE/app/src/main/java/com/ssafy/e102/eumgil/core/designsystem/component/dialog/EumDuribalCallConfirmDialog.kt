package com.ssafy.e102.eumgil.core.designsystem.component.dialog

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius

enum class EumDuribalCallConfirmDismissStyle {
    TextButton,
    SecondaryButton,
}

@Composable
fun EumDuribalCallConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    dismissStyle: EumDuribalCallConfirmDismissStyle,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.my_page_duribal_call_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.my_page_duribal_call_dialog_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(EumRadius.scaleM),
            ) {
                Text(text = stringResource(id = R.string.my_page_duribal_call_dialog_confirm))
            }
        },
        dismissButton = {
            when (dismissStyle) {
                EumDuribalCallConfirmDismissStyle.TextButton ->
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.my_page_duribal_call_dialog_dismiss))
                    }

                EumDuribalCallConfirmDismissStyle.SecondaryButton ->
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(EumRadius.scaleM),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                    ) {
                        Text(text = stringResource(id = R.string.my_page_duribal_call_dialog_dismiss))
                    }
            }
        },
    )
}
