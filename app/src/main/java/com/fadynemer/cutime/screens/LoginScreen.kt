package com.fadynemer.cutime.screens

import androidx.compose.ui.res.stringResource

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fadynemer.cutime.R
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeSuccess
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary
import com.fadynemer.cutime.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    loginViewModel: LoginViewModel = viewModel()
) {
    var email by rememberSaveable {
        mutableStateOf("")
    }

    var password by rememberSaveable {
        mutableStateOf("")
    }

    var passwordVisible by rememberSaveable {
        mutableStateOf(false)
    }

    var showValidationErrors by rememberSaveable {
        mutableStateOf(false)
    }

    var showResetEmailError by rememberSaveable {
        mutableStateOf(false)
    }

    val uiState = loginViewModel.uiState

    val emailIsValid =
        Patterns.EMAIL_ADDRESS
            .matcher(email.trim())
            .matches()

    val emailError =
        (showValidationErrors || showResetEmailError) &&
                !emailIsValid

    val passwordError =
        showValidationErrors && password.isBlank()

    val formIsValid =
        emailIsValid &&
                password.isNotBlank()

    LaunchedEffect(
        uiState.loginSuccessful,
        uiState.loggedInRole
    ) {
        if (uiState.loginSuccessful) {
            val destination =
                if (uiState.loggedInRole == "BARBER") {
                    "dashboard"
                } else {
                    "home"
                }

            navController.navigate(destination) {
                popUpTo("welcome") {
                    inclusive = true
                }

                launchSingleTop = true
            }
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = {
                        navController.popBackStack()
                    }
                ) {
                    Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = CutTimeNavy
                    )
                }
            }

            Spacer(modifier = Modifier.height(0.dp))

            Image(
                painter = painterResource(
                    id = R.drawable.cutime_logo
                ),
                contentDescription = stringResource(R.string.content_description_cutime_logo),
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(0.dp))

            Text(
                text = stringResource(R.string.login_welcome),
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = CutTimeNavy
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.login_subtitle),
                fontSize = 14.sp,
                color = CutTimeTextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    showResetEmailError = false
                    loginViewModel.clearMessages()
                },
                label = {
                    Text(stringResource(R.string.field_email))
                },
                placeholder = {
                    Text(stringResource(R.string.field_email_hint))
                },
                singleLine = true,
                isError = emailError,
                supportingText = {
                    if (emailError) {
                        Text(stringResource(R.string.field_email_invalid))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    loginViewModel.clearMessages()
                },
                label = {
                    Text(stringResource(R.string.field_password))
                },
                placeholder = {
                    Text(stringResource(R.string.field_password_login_hint))
                },
                singleLine = true,
                isError = passwordError,
                supportingText = {
                    if (passwordError) {
                        Text(stringResource(R.string.field_password_required))
                    }
                },
                shape = RoundedCornerShape(14.dp),
                visualTransformation =
                    if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            passwordVisible = !passwordVisible
                        }
                    ) {
                        Icon(
                            imageVector =
                                if (passwordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                            contentDescription =
                                if (passwordVisible) {
                                    "Hide password"
                                } else {
                                    "Show password"
                                }
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.login_forgot_password),
                color = CutTimeNavy,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = !uiState.isLoading
                    ) {
                        if (emailIsValid) {
                            showResetEmailError = false

                            loginViewModel.sendPasswordResetEmail(
                                email = email
                            )
                        } else {
                            showResetEmailError = true
                        }
                    },
                textAlign = TextAlign.End
            )

            Spacer(modifier = Modifier.height(20.dp))

            uiState.successMessage?.let { successMessage ->
                Text(
                    text = successMessage,
                    color = CutTimeSuccess,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            uiState.errorMessage?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    showValidationErrors = true

                    if (formIsValid) {
                        loginViewModel.loginUser(
                            email = email,
                            password = password
                        )
                    }
                },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CutTimeNavy
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.login_action),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.login_no_account),
                    color = CutTimeTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = stringResource(R.string.register_action),
                    color = CutTimeNavy,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.clickable {
                        navController.navigate("register")
                    }
                )
            }
        }
    }
}
