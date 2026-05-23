package com.farah.jewelryar.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.farah.jewelryar.ui.theme.GreenDark
import com.farah.jewelryar.ui.theme.Gold

data class CategoryItem(val key: String?, val labelEn: String, val labelAr: String)

val CATEGORIES = listOf(
    CategoryItem(null, "All", "الكل"),
    CategoryItem("necklace", "Necklaces", "عقود"),
    CategoryItem("ring", "Rings", "خواتم"),
    CategoryItem("bracelet", "Bracelets", "أساور"),
    CategoryItem("luxury", "Luxury", "فاخر"),
)

@Composable
fun FilterChips(
    selectedCategory: String?,
    isArabic: Boolean,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        items(CATEGORIES) { cat ->
            val selected = cat.key == selectedCategory
            FilterChip(
                selected = selected,
                onClick = { onCategorySelected(cat.key) },
                label = { Text(if (isArabic) cat.labelAr else cat.labelEn) },
                modifier = Modifier.padding(end = 8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GreenDark,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White,
                    labelColor = GreenDark
                )
            )
        }
    }
}
