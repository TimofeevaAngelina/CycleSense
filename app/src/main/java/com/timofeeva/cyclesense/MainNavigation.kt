package com.timofeeva.cyclesense

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.timofeeva.cyclesense.ui.theme.CycleSenseBackground
import com.timofeeva.cyclesense.ui.theme.CycleSenseTextSecondary

enum class MainTab {
    CALENDAR,
    ANALYTICS,
    PROFILE
}

@Composable
fun MainBottomBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CycleSenseBackground,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainBottomBarItem(
                selected = selectedTab == MainTab.CALENDAR,
                iconResId = R.drawable.ic_calendar,
                label = "Календарь",
                onClick = { onTabSelected(MainTab.CALENDAR) },
                modifier = Modifier.weight(1f)
            )

            MainBottomBarItem(
                selected = selectedTab == MainTab.ANALYTICS,
                iconResId = R.drawable.ic_analytics,
                label = "Аналитика",
                onClick = { onTabSelected(MainTab.ANALYTICS) },
                modifier = Modifier.weight(1f)
            )

            MainBottomBarItem(
                selected = selectedTab == MainTab.PROFILE,
                iconResId = R.drawable.ic_profile,
                label = "Профиль",
                onClick = { onTabSelected(MainTab.PROFILE) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MainBottomBarItem(
    selected: Boolean,
    iconResId: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val itemColor = if (selected) {
        Color.Black
    } else {
        CycleSenseTextSecondary
    }

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = label,
            tint = itemColor,
            modifier = Modifier.size(22.dp)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = itemColor,
            fontWeight = if (selected) {
                FontWeight.SemiBold
            } else {
                FontWeight.Medium
            }
        )
    }
}