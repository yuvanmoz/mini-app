package com.mini.app.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mini.app.domain.model.Tag
import com.mini.app.ui.theme.*

@Composable
fun TagManagementDialog(
    postTags: List<Tag>,
    allTags: List<Tag>,
    onSaveTags: (List<String>) -> Unit,
    onCreateTag: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedTagNames = remember {
        mutableStateListOf<String>().apply {
            addAll(postTags.map { it.name })
        }
    }

    var newTagName by remember { mutableStateOf("") }
    var isAddingTag by remember { mutableStateOf(false) }
    val isDark = LocalIsDarkTheme.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Manage Collections & Tags",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = AppFontFamily
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                Text(
                    text = "Select tags for this post:",
                    color = MiniSecondaryText,
                    fontSize = 13.sp,
                    fontFamily = AppFontFamily,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (allTags.isEmpty() && !isAddingTag) {
                    Text(
                        text = "No collections created yet.",
                        color = MiniSecondaryText,
                        fontSize = 14.sp,
                        fontFamily = AppFontFamily,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                    ) {
                        items(allTags, key = { it.name }) { tag ->
                            val isChecked = selectedTagNames.contains(tag.name)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (!selectedTagNames.contains(tag.name)) {
                                                selectedTagNames.add(tag.name)
                                            }
                                        } else {
                                            selectedTagNames.remove(tag.name)
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MiniPrimary,
                                        checkmarkColor = Color.White
                                    )
                                )
                                Text(
                                    text = tag.name,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 15.sp,
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isAddingTag) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newTagName,
                            onValueChange = { newTagName = it },
                            placeholder = { Text("Tag name (e.g. Trading)", fontSize = 13.sp, fontFamily = AppFontFamily) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MiniPrimary,
                                unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newTagName.isNotBlank()) {
                                    val trimmed = newTagName.trim()
                                    onCreateTag(trimmed)
                                    if (!selectedTagNames.contains(trimmed)) {
                                        selectedTagNames.add(trimmed)
                                    }
                                    newTagName = ""
                                    isAddingTag = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MiniPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Add", color = Color.White, fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    TextButton(
                        onClick = { isAddingTag = true },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MiniPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create new tag", color = MiniPrimary, fontSize = 14.sp, fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveTags(selectedTagNames.toList())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MiniPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done", color = Color.White, fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onBackground, fontFamily = AppFontFamily)
            }
        },
        containerColor = if (isDark) GlassSurfaceHighDark else GlassSurfaceHighLight,
        shape = RoundedCornerShape(20.dp)
    )
}

