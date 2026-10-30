package incident.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import incident.android.R
import incident.android.ui.icons.FilterListFilledIcon
import incident.android.ui.icons.FilterListIcon
import incident.android.ui.icons.SearchIcon
import incident.shared.incidents.Category
import incident.shared.incidents.Priority
import incident.shared.incidents.Status

@Composable
fun SearchAndFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    hasActiveFilters: Boolean,
    onFilterClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                SearchTextField(
                    query = query,
                    onQueryChange = onQueryChange
                )
            }

            FilterIconButton(
                hasActiveFilters = hasActiveFilters,
                onFilterClick = onFilterClick
            )
        }
    }
}

@Composable
private fun SearchTextField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search_incidents)) },
        leadingIcon = {
            Icon(
                imageVector = SearchIcon,
                contentDescription = stringResource(R.string.search)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun FilterIconButton(
    hasActiveFilters: Boolean,
    onFilterClick: () -> Unit
) {
    IconButton(
        onClick = onFilterClick,
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (hasActiveFilters) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
    ) {
        Icon(
            imageVector = if (hasActiveFilters) FilterListFilledIcon else FilterListIcon,
            contentDescription = stringResource(R.string.filter),
            tint = if (hasActiveFilters) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FilterDialog(
    selectedPriority: Set<Priority>,
    selectedStatus: Set<Status>,
    selectedCategory: Set<Category>,
    onUpdatePriority: (Set<Priority>) -> Unit,
    onUpdateStatus: (Set<Status>) -> Unit,
    onUpdateCategory: (Set<Category>) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter_incidents)) },
        text = {
            FilterDialogContent(
                selectedPriority = selectedPriority,
                selectedStatus = selectedStatus,
                selectedCategory = selectedCategory,
                onUpdatePriority = onUpdatePriority,
                onUpdateStatus = onUpdateStatus,
                onUpdateCategory = onUpdateCategory
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.apply))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onClearAll()
                onDismiss()
            }) {
                Text(stringResource(R.string.clear_all))
            }
        }
    )
}

@Composable
private fun FilterDialogContent(
    selectedPriority: Set<Priority>,
    selectedStatus: Set<Status>,
    selectedCategory: Set<Category>,
    onUpdatePriority: (Set<Priority>) -> Unit,
    onUpdateStatus: (Set<Status>) -> Unit,
    onUpdateCategory: (Set<Category>) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FilterSection(
            title = stringResource(R.string.priority_lowercase),
            options = Priority.entries,
            selectedOptions = selectedPriority,
            onOptionsSelected = { onUpdatePriority(it.toSet()) }
        )

        HorizontalDivider()

        FilterSection(
            title = stringResource(R.string.status_lowercase),
            options = Status.entries,
            selectedOptions = selectedStatus,
            onOptionsSelected = { onUpdateStatus(it.toSet()) }
        )

        HorizontalDivider()

        FilterSection(
            title = stringResource(R.string.category_lowercase),
            options = Category.entries,
            selectedOptions = selectedCategory,
            onOptionsSelected = { onUpdateCategory(it.toSet()) }
        )
    }
}

@Composable
private fun <T : Enum<T>> FilterSection(
    title: String,
    options: List<T>,
    selectedOptions: Set<T>,
    onOptionsSelected: (List<T>) -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )

    FilterChipRow(
        options = options,
        selectedOptions = selectedOptions.toList(),
        onOptionsSelected = onOptionsSelected
    )
}

@Composable
fun <T : Enum<T>> FilterChipRow(
    options: List<T>,
    selectedOptions: List<T>,
    onOptionsSelected: (List<T>) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selectedOptions.contains(option),
                onClick = {
                    val new = if (selectedOptions.contains(option)) {
                        selectedOptions - option
                    } else {
                        selectedOptions + option
                    }
                    onOptionsSelected(new)
                },
                label = {
                    Text(
                        text = option.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp
                    )
                }
            )
        }
    }
}
