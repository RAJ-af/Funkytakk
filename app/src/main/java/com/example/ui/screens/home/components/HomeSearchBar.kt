package com.example.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FunkySearchIcon
import com.example.ui.components.SoftFilterIcon
import com.example.ui.theme.LocalFunkyColors

@Composable
fun HomeSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenFilter: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search bar capsule
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = "Search language or room...",
                    color = funkyColors.mute,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                FunkySearchIcon(
                    color = funkyColors.mute,
                    modifier = Modifier.size(18.dp)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(100.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = funkyColors.surface,
                unfocusedContainerColor = funkyColors.surface,
                focusedBorderColor = funkyColors.mango,
                unfocusedBorderColor = funkyColors.line,
                focusedTextColor = funkyColors.ink,
                unfocusedTextColor = funkyColors.ink,
                cursorColor = funkyColors.mango
            ),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("home_search_input"),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { /* Filter live */ })
        )

        // Filter icon button
        Box(
            modifier = Modifier
                .size(48.dp)
                .border(1.dp, funkyColors.line, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(funkyColors.surface)
                .clickable { onOpenFilter() }
                .testTag("home_filter_button"),
            contentAlignment = Alignment.Center
        ) {
            SoftFilterIcon(
                color = funkyColors.ink,
                size = 18.dp
            )
        }
    }
}
