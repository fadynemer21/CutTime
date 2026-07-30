package com.fadynemer.cutime.repository

import com.fadynemer.cutime.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

interface ProfileDataSource {
    fun getCurrentUserProfile(
        onResult: (Result<UserProfile?>) -> Unit
    )

    fun updateFullName(
        fullName: String,
        onResult: (Result<UserProfile>) -> Unit
    )
}

class AuthRepository : ProfileDataSource {

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

            val displayNameUpdate =
                UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()

            firebaseUser.updateProfile(displayNameUpdate)
                .addOnSuccessListener {
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
                             * Authentication and Firestore form one
                             * logical registration. Remove the new
                             * Authentication account if the profile
                             * document cannot be saved.
                             */
                            firebaseUser
                                .delete()
                                .addOnCompleteListener {
                                    onResult(
                                        Result.failure(
                                            firestoreError
                                        )
                                    )
                                }
                        }
                }
                .addOnFailureListener { profileError ->
                    firebaseUser
                        .delete()
                        .addOnCompleteListener {
                            onResult(Result.failure(profileError))
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

    override fun getCurrentUserProfile(
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

    override fun updateFullName(
        fullName: String,
        onResult: (Result<UserProfile>) -> Unit
    ) {
        val cleanName = fullName.trim()
        val firebaseUser = auth.currentUser

        if (firebaseUser == null) {
            onResult(
                Result.failure(
                    IllegalStateException(
                        "Please log in again to update your profile."
                    )
                )
            )
            return
        }

        if (cleanName.length !in 2..60) {
            onResult(
                Result.failure(
                    IllegalArgumentException(
                        "Name must be between 2 and 60 characters."
                    )
                )
            )
            return
        }

        val userReference =
            firestore.collection("users").document(firebaseUser.uid)

        userReference.get()
            .addOnSuccessListener { document ->
                val currentProfile =
                    document.toObject(UserProfile::class.java)

                if (currentProfile == null) {
                    onResult(
                        Result.failure(
                            IllegalStateException(
                                "Your user profile could not be loaded."
                            )
                        )
                    )
                    return@addOnSuccessListener
                }

                val oldDisplayName = firebaseUser.displayName
                val authUpdate = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()

                firebaseUser.updateProfile(authUpdate)
                    .addOnSuccessListener {
                        userReference.update(
                            mapOf(
                                "fullName" to cleanName,
                                "updatedAt" to
                                    FieldValue.serverTimestamp()
                            )
                        ).addOnSuccessListener {
                            onResult(
                                Result.success(
                                    currentProfile.copy(
                                        fullName = cleanName
                                    )
                                )
                            )
                        }.addOnFailureListener { firestoreError ->
                            firebaseUser.updateProfile(
                                UserProfileChangeRequest.Builder()
                                    .setDisplayName(oldDisplayName)
                                    .build()
                            )
                            onResult(Result.failure(firestoreError))
                        }
                    }
                    .addOnFailureListener { authError ->
                        onResult(Result.failure(authError))
                    }
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    fun logout() {
        auth.signOut()
    }
}
