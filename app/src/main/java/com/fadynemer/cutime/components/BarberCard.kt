package com.fadynemer.cutime.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fadynemer.cutime.model.BarberShop
import com.fadynemer.cutime.ui.theme.CutTimeLightGrey
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeRed
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary

@Composable
fun BarberCard(
    barberShop: BarberShop,
    onViewProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(82.dp),
                shape = RoundedCornerShape(16.dp),
                color = CutTimeLightGrey
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = null,
                        tint = CutTimeNavy,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = barberShop.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CutTimeNavy
                )

                Spacer(modifier = Modifier.height(5.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = CutTimeRed,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = barberShop.rating.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "(${barberShop.reviewCount})",
                        fontSize = 13.sp,
                        color = CutTimeTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Haircuts from ₪${barberShop.startingPrice}",
                    fontSize = 14.sp,
                    color = CutTimeTextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = CutTimeNavy,
                        modifier = Modifier.size(17.dp)
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = "Next: ${barberShop.nextAvailable}",
                        fontSize = 13.sp,
                        color = CutTimeTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onViewProfile,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CutTimeNavy
                    ),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("View Profile")
                }
            }
        }
    }
}