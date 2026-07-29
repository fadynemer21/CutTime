package com.fadynemer.cutime.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fadynemer.cutime.R

@Composable
fun WelcomeScreen(navController: NavController) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .offset(y = (-50).dp)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Image(
            painter = painterResource(id = R.drawable.cutime_logo),
            contentDescription = "CutTime Logo",
            modifier = Modifier.size(400.dp)
        )

        Spacer(modifier = Modifier.height(0.dp))

        Text(
            text = "Your Time.\nYour Style.",
            style = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                textDirection = TextDirection.Ltr
            )
        )

        Spacer(modifier = Modifier.height(15.dp))

        Text(
            text = "Book appointments with your favourite barber in seconds.",
            style = TextStyle(
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                textDirection = TextDirection.Ltr
            )
        )

        Spacer(modifier = Modifier.height(70.dp))

        Button(
            onClick = {
                navController.navigate("login")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Login",
                style = TextStyle(textDirection = TextDirection.Ltr)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
            onClick = {
                navController.navigate("register")
            },
            modifier = Modifier.fillMaxWidth()
        ){
            Text(
                text = "Create Account",
                style = TextStyle(textDirection = TextDirection.Ltr)
            )
        }
    }
}