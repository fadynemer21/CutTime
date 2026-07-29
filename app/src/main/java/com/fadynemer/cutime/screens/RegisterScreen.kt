package com.fadynemer.cutime.screens

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fadynemer.cutime.R
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary

private enum class AccountType {
    CUSTOMER,
    BARBER
}

@Composable
fun RegisterScreen(navController: NavController) {

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    var selectedAccountType by rememberSaveable {
        mutableStateOf<AccountType?>(null)
    }

    var showErrors by rememberSaveable { mutableStateOf(false) }

    val fullNameError =
        showErrors && fullName.isBlank()

    val emailError =
        showErrors && (
                email.isBlank() ||
                        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
                )

    val passwordLengthValid = password.length >= 8
    val passwordHasCapital = password.any { it.isUpperCase() }
    val passwordHasNumber = password.any { it.isDigit() }

    val passwordError =
        showErrors && (
                !passwordLengthValid ||
                        !passwordHasCapital ||
                        !passwordHasNumber
                )

    val confirmPasswordError =
        showErrors && (
                confirmPassword.isBlank() ||
                        confirmPassword != password
                )

    val accountTypeError =
        showErrors && selectedAccountType == null

    val formIsValid =
        fullName.isNotBlank() &&
                Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() &&
                passwordLengthValid &&
                passwordHasCapital &&
                passwordHasNumber &&
                confirmPassword == password &&
                selectedAccountType != null

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp),
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
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = CutTimeNavy
                    )
                }
            }

            Image(
                painter = painterResource(id = R.drawable.cutime_logo),
                contentDescription = "CuTime Logo",
                modifier = Modifier.size(100.dp)
            )

            Text(
                text = "Create Account",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = CutTimeNavy
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Join CutTime and book your next appointment",
                fontSize = 14.sp,
                color = CutTimeTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                },
                label = {
                    Text("Full Name")
                },
                placeholder = {
                    Text("Enter your full name")
                },
                singleLine = true,
                isError = fullNameError,
                supportingText = {
                    if (fullNameError) {
                        Text("Full name is required.")
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                },
                label = {
                    Text("Email")
                },
                placeholder = {
                    Text("Enter your email")
                },
                singleLine = true,
                isError = emailError,
                supportingText = {
                    if (emailError) {
                        Text("Enter a valid email address.")
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                },
                label = {
                    Text("Password")
                },
                placeholder = {
                    Text("Create a password")
                },
                singleLine = true,
                isError = passwordError,
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
                supportingText = {
                    if (passwordError) {
                        Text(
                            when {
                                !passwordLengthValid ->
                                    "Password must contain at least 8 characters."

                                !passwordHasCapital ->
                                    "Password must contain an uppercase letter."

                                !passwordHasNumber ->
                                    "Password must contain a number."

                                else -> ""
                            }
                        )
                    } else {
                        Text(
                            text = "At least 8 characters, 1 uppercase letter and 1 number."
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                },
                label = {
                    Text("Confirm Password")
                },
                placeholder = {
                    Text("Enter your password again")
                },
                singleLine = true,
                isError = confirmPasswordError,
                visualTransformation =
                    if (confirmPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            confirmPasswordVisible =
                                !confirmPasswordVisible
                        }
                    ) {
                        Icon(
                            imageVector =
                                if (confirmPasswordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                            contentDescription =
                                if (confirmPasswordVisible) {
                                    "Hide password"
                                } else {
                                    "Show password"
                                }
                        )
                    }
                },
                supportingText = {
                    if (confirmPasswordError) {
                        Text(
                            if (confirmPassword.isBlank()) {
                                "Please confirm your password."
                            } else {
                                "Passwords do not match."
                            }
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Account Type",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = CutTimeNavy,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedAccountType == AccountType.CUSTOMER) {
                    Button(
                        onClick = {
                            selectedAccountType = AccountType.CUSTOMER
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CutTimeNavy
                        )
                    ) {
                        Text("Customer")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            selectedAccountType = AccountType.CUSTOMER
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Customer")
                    }
                }

                if (selectedAccountType == AccountType.BARBER) {
                    Button(
                        onClick = {
                            selectedAccountType = AccountType.BARBER
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CutTimeNavy
                        )
                    ) {
                        Text("Barber")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            selectedAccountType = AccountType.BARBER
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Barber")
                    }
                }
            }

            if (accountTypeError) {
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Please select an account type.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    showErrors = true

                    if (formIsValid) {
                        // Firebase registration will be added next.
                        // selectedAccountType tells us whether
                        // this is a CUSTOMER or BARBER account.
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CutTimeNavy
                )
            ) {
                Text(
                    text = "Create Account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account?",
                    color = CutTimeTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Login",
                    color = CutTimeNavy,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.clickable {
                        navController.navigate("login") {
                            popUpTo("register") {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}