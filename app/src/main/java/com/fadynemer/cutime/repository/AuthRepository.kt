package com.fadynemer.cutime.repository

import com.fadynemer.cutime.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository {

    private val auth: FirebaseAuth =
        FirebaseAuth.getInstance()

    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()

    fun registerUser(
        fullName: String,
        email: String,
        password: String,
        role: String,
        onResult: (Result<UserProfile>) -> Unit
    ) {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim().lowercase()

        auth.createUserWithEmailAndPassword(
            cleanEmail,
            password
        ).addOnCompleteListener { authenticationTask ->

            if (!authenticationTask.isSuccessful) {
                val error = authenticationTask.exception
                    ?: Exception("Account creation failed.")

                onResult(Result.failure(error))
                return@addOnCompleteListener
            }

            val firebaseUser = auth.currentUser

            if (firebaseUser == null) {
                onResult(
                    Result.failure(
                        Exception(
                            "The account was created, but the user could not be found."
                        )
                    )
                )

                return@addOnCompleteListener
            }

            val userProfile = UserProfile(
                uid = firebaseUser.uid,
                fullName = cleanName,
                email = cleanEmail,
                role = role
            )

            val profileData = hashMapOf(
                "uid" to userProfile.uid,
                "fullName" to userProfile.fullName,
                "email" to userProfile.email,
                "role" to userProfile.role,
                "createdAt" to FieldValue.serverTimestamp()
            )

            firestore
                .collection("users")
                .document(firebaseUser.uid)
                .set(profileData)
                .addOnSuccessListener {
                    onResult(
                        Result.success(userProfile)
                    )
                }
                .addOnFailureListener { firestoreError ->

                    /*
                     * Remove the new Authentication account if its
                     * Firestore profile could not be saved.
                     */
                    firebaseUser
                        .delete()
                        .addOnCompleteListener {
                            onResult(
                                Result.failure(firestoreError)
                            )
                        }
                }
        }
    }

    fun loginUser(
        email: String,
        password: String,
        onResult: (Result<UserProfile>) -> Unit
    ) {
        val cleanEmail = email.trim().lowercase()

        auth.signInWithEmailAndPassword(
            cleanEmail,
            password
        ).addOnCompleteListener { authenticationTask ->

            if (!authenticationTask.isSuccessful) {
                val error = authenticationTask.exception
                    ?: Exception("Login failed.")

                onResult(Result.failure(error))
                return@addOnCompleteListener
            }

            val firebaseUser = auth.currentUser

            if (firebaseUser == null) {
                onResult(
                    Result.failure(
                        Exception(
                            "Login succeeded, but the user could not be found."
                        )
                    )
                )

                return@addOnCompleteListener
            }

            firestore
                .collection("users")
                .document(firebaseUser.uid)
                .get()
                .addOnSuccessListener { document ->

                    if (!document.exists()) {
                        auth.signOut()

                        onResult(
                            Result.failure(
                                Exception(
                                    "Your user profile could not be found."
                                )
                            )
                        )

                        return@addOnSuccessListener
                    }

                    val userProfile =
                        document.toObject(
                            UserProfile::class.java
                        )

                    if (userProfile == null) {
                        auth.signOut()

                        onResult(
                            Result.failure(
                                Exception(
                                    "Your user profile could not be loaded."
                                )
                            )
                        )

                        return@addOnSuccessListener
                    }

                    val roleIsValid =
                        userProfile.role == "CUSTOMER" ||
                                userProfile.role == "BARBER"

                    if (!roleIsValid) {
                        auth.signOut()

                        onResult(
                            Result.failure(
                                Exception(
                                    "This account has an invalid user role."
                                )
                            )
                        )

                        return@addOnSuccessListener
                    }

                    onResult(
                        Result.success(userProfile)
                    )
                }
                .addOnFailureListener { firestoreError ->
                    auth.signOut()

                    onResult(
                        Result.failure(firestoreError)
                    )
                }
        }
    }

    fun sendPasswordResetEmail(
        email: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val cleanEmail = email.trim().lowercase()

        auth.sendPasswordResetEmail(cleanEmail)
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    fun getCurrentUserProfile(
        onResult: (Result<UserProfile?>) -> Unit
    ) {
        val firebaseUser = auth.currentUser

        if (firebaseUser == null) {
            onResult(Result.success(null))
            return
        }

        firestore
            .collection("users")
            .document(firebaseUser.uid)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    auth.signOut()
                    onResult(Result.success(null))
                    return@addOnSuccessListener
                }

                val userProfile =
                    document.toObject(
                        UserProfile::class.java
                    )

                if (userProfile == null) {
                    auth.signOut()
                    onResult(Result.success(null))
                    return@addOnSuccessListener
                }

                val roleIsValid =
                    userProfile.role == "CUSTOMER" ||
                            userProfile.role == "BARBER"

                if (!roleIsValid) {
                    auth.signOut()
                    onResult(Result.success(null))
                    return@addOnSuccessListener
                }

                onResult(
                    Result.success(userProfile)
                )
            }
            .addOnFailureListener { error ->
                onResult(
                    Result.failure(error)
                )
            }
    }

    fun logout() {
        auth.signOut()
    }
}