package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.config.AiEndpointConfig

@Composable
fun OpenAiSettingsDialog(
    currentRuntimeKey: String,
    onSaveKey: (String) -> Unit,
    onClearKey: () -> Unit,
    onDismiss: () -> Unit
) {
    var keyInput by remember(currentRuntimeKey) { mutableStateOf(currentRuntimeKey) }
    var isKeyVisible by remember { mutableStateOf(false) }

    val hasRuntimeKey = currentRuntimeKey.isNotBlank()
    val hasEnvKey = AiEndpointConfig.hasEnvKey()
    val isConnected = hasRuntimeKey || hasEnvKey

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("openai_settings_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "تنظیمات اتصال به OpenAI",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Connection status banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isConnected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isConnected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isConnected) Icons.Filled.CheckCircle else Icons.Outlined.Info,
                                contentDescription = null,
                                tint = if (isConnected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    hasRuntimeKey -> "فعال با کلید موقت درون‌برنامه‌ای"
                                    hasEnvKey -> "فعال با متغیر محیطی VITE_OPENAI_API_KEY"
                                    else -> "کلید OpenAI تنظیم نشده است (حالت دانشنامه آفلاین)"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isConnected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                        }
                        Text(
                            text = "مدل: ${AiEndpointConfig.modelName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "آدرس سرویس: ${AiEndpointConfig.chatCompletionsUrl()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "برای تست یا ارائه زنده می‌توانید OpenAI API Key خود را موقتاً در این کادر وارد کنید. در صورت نبود کلید یا خطای شبکه، اپلیکیشن به‌طور خودکار از پایگاه داده محلی آثار سلطانیه استفاده می‌کند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("openai_api_key_input"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        textDirection = TextDirection.Ltr,
                        textAlign = TextAlign.Left
                    ),
                    label = { Text("کلید OpenAI API (اختیاری)") },
                    placeholder = {
                        Text(
                            text = "sk-...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDirection = TextDirection.Ltr,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    },
                    visualTransformation = if (isKeyVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { isKeyVisible = !isKeyVisible },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("toggle_api_key_visibility")
                        ) {
                            Icon(
                                imageVector = if (isKeyVisible) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },
                                contentDescription = if (isKeyVisible) "مخفی کردن کلید" else "نمایش کلید"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isKeyVisible) KeyboardType.Ascii else KeyboardType.Password,
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            keyInput = ""
                            onClearKey()
                            onDismiss()
                        },
                        enabled = hasRuntimeKey || keyInput.isNotBlank(),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("clear_openai_key_button")
                    ) {
                        Text("حذف کلید موقت")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("cancel_openai_settings_button")
                        ) {
                            Text("بستن")
                        }
                        Button(
                            onClick = {
                                onSaveKey(keyInput)
                                onDismiss()
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("save_openai_key_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("ذخیره موقت")
                        }
                    }
                }
            }
        }
    }
}
