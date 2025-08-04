package com.blesense.app
// Import necessary libraries for Firebase Authentication, Google Sign-In, and Kotlin Coroutines
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.*
import com.google.firebase.auth.GoogleAuthProvider.getCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

// Sealed class to represent different authentication states
sealed class AuthState {
    data object Idle : AuthState() // No authentication action in progress
    data object Loading : AuthState() // Authentication action is in progress
    data class Success(val user: FirebaseUser) : AuthState() // Authentication succeeded with user details
    data class Error(val message: String) : AuthState() // Authentication failed with an error message
    data object PasswordResetEmailSent : AuthState() // Password reset email was sent successfully
    data object AccountDeleted : AuthState() // User account was deleted successfully
}

// ViewModel for managing Firebase authentication operations
class AuthViewModel : ViewModel() {
    // Initialize Firebase Authentication instance
    private val auth = FirebaseAuth.getInstance()

    // StateFlow to manage the current authentication state
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // StateFlow to track the current Firebase user
    private val _currentUser = MutableStateFlow(auth.currentUser)

    // Google Sign-In client, initialized later
    private lateinit var googleSignInClient: GoogleSignInClient

    // Initialize the ViewModel
    init {
        // Update the current user state on initialization
        updateCurrentUser()
        // Add a listener to update user state on authentication changes
        auth.addAuthStateListener {
            updateCurrentUser()
        }
    }

    // Update the current user state and auth state
    private fun updateCurrentUser() {
        _currentUser.value = auth.currentUser
        auth.currentUser?.let { user ->
            // Set auth state to Success if a user is signed in
            _authState.value = AuthState.Success(user)
        } ?: run {
            // Set auth state to Idle if no user is signed in
            _authState.value = AuthState.Idle
        }
    }

    // Check the current Firebase user
    fun checkCurrentUser(): FirebaseUser? = auth.currentUser

    // Check if a user is authenticated
    fun isUserAuthenticated(): Boolean = auth.currentUser != null

    // Send a password reset email to the specified email address
    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            try {
                // Set auth state to Loading during the operation
                _authState.value = AuthState.Loading
                // Send the password reset email
                auth.sendPasswordResetEmail(email).await()
                // Update state to indicate email was sent
                _authState.value = AuthState.PasswordResetEmailSent
            } catch (e: Exception) {
                // Handle specific Firebase exceptions
                val errorMessage = when (e) {
                    is FirebaseAuthInvalidUserException -> "No account found with this email."
                    is FirebaseAuthInvalidCredentialsException -> "Invalid email format."
                    else -> e.message ?: "Failed to send password reset email."
                }
                // Update state with error message
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }



    // Handle Google Sign-In errors
    fun handleGoogleSignInError(errorMessage: String) {
        viewModelScope.launch {
            // Update auth state with the provided error message
            _authState.value = AuthState.Error(errorMessage)
        }
    }

    // Sign in as a guest (anonymous authentication)
    fun signInAsGuest() {
        viewModelScope.launch {
            try {
                // Set auth state to Loading
                _authState.value = AuthState.Loading
                // Perform anonymous sign-in
                val result = auth.signInAnonymously().await()
                result.user?.let {
                    // Process sign-in result and update states
                    onSignInResult(result)
                    _authState.value = AuthState.Success(it)
                    updateCurrentUser()
                } ?: throw Exception("Anonymous sign-in failed")
            } catch (e: Exception) {
                // Update state with error message
                _authState.value = AuthState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    // Register a new user with email and password
    fun registerUser(email: String, password: String) {
        viewModelScope.launch {
            try {
                // Set auth state to Loading
                _authState.value = AuthState.Loading
                // Create a new user with email and password
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                result.user?.let {
                    // Process sign-in result and update states
                    onSignInResult(result)
                    _authState.value = AuthState.Success(it)
                    updateCurrentUser()
                } ?: throw Exception("Registration failed. No user created.")
            } catch (e: Exception) {
                // Handle specific Firebase exceptions
                val errorMessage = when (e) {
                    is FirebaseAuthWeakPasswordException -> "Weak password: ${e.reason}"
                    is FirebaseAuthInvalidCredentialsException -> "Invalid email format."
                    is FirebaseAuthUserCollisionException -> "This email is already registered."
                    else -> e.message ?: "Registration failed due to an unknown error."
                }
                // Update state with error message
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }

    // Log in an existing user with email and password
    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            try {
                // Set auth state to Loading
                _authState.value = AuthState.Loading
                // Sign in with email and password
                val result = auth.signInWithEmailAndPassword(email, password).await()
                result.user?.let {
                    // Process sign-in result and update states
                    onSignInResult(result)
                    _authState.value = AuthState.Success(it)
                    updateCurrentUser()
                } ?: throw Exception("Login failed. No user found.")
            } catch (e: Exception) {
                // Handle specific Firebase exceptions
                val errorMessage = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
                    is FirebaseAuthInvalidUserException -> "No account found with this email."
                    else -> e.message ?: "Login failed due to an unknown error."
                }
                // Update state with error message
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }

    // Set the Google Sign-In client
    fun setGoogleSignInClient(client: GoogleSignInClient) {
        googleSignInClient = client
    }

    // Sign in with Google using an ID token
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                // Set auth state to Loading
                _authState.value = AuthState.Loading
                // Create a credential from the Google ID token
                val credential = getCredential(idToken, null)
                // Sign in with the credential
                val result = auth.signInWithCredential(credential).await()
                result.user?.let {
                    // Process sign-in result and update states
                    onSignInResult(result)
                    _authState.value = AuthState.Success(it)
                    updateCurrentUser()
                } ?: throw Exception("Google sign-in failed")
            } catch (e: Exception) {
                // Log and handle Google Sign-In errors
                Log.e("AuthViewModel", "Google Sign-In Error", e)
                _authState.value = AuthState.Error(
                    e.message ?: "Google sign-in failed due to an unknown error"
                )
            }
        }
    }

    // Sign out from Firebase and Google
    fun signOut(context: Context) {
        // Sign out from Firebase
        auth.signOut()
        // Update auth state to Idle
        _authState.value = AuthState.Idle
        updateCurrentUser()

        // Configure Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        // Sign out from Google
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        googleSignInClient.signOut()
    }

    // Delete the current account and sign in as a guest
    fun deleteAccountAndSignInAsGuest(password: String? = null, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                // Set auth state to Loading
                _authState.value = AuthState.Loading
                val currentUser = auth.currentUser ?: throw Exception("No user signed in.")
                val currentUserId = currentUser.uid

                // Re-authenticate if a password is provided
                password?.let {
                    val credential = EmailAuthProvider.getCredential(currentUser.email!!, it)
                    currentUser.reauthenticate(credential).await()
                }

                // Step 1: Sign in anonymously to start a new session
                val result = auth.signInAnonymously().await()
                val guestUser = result.user ?: throw Exception("Anonymous sign-in failed")

                // Step 2: Delete the previous user account
                currentUser.delete().await()

                // Step 3: Update the local user repository
                UserRepository.removeUser(currentUserId)
                UserRepository.addUser(
                    UserData(
                        id = guestUser.uid,
                        name = "Guest",
                        email = "guest@example.com",
                        isAnonymous = true,
                        profilePictureUrl = null
                    )
                )

                // Update auth state and notify success
                updateCurrentUser()
                _authState.value = AuthState.AccountDeleted
                callback(true, null)

            } catch (e: Exception) {
                // Handle specific Firebase exceptions
                val message = when (e) {
                    is FirebaseAuthRecentLoginRequiredException -> "Please re-authenticate before deleting your account."
                    is FirebaseAuthInvalidUserException -> "User already deleted."
                    else -> e.message ?: "Account deletion failed."
                }
                // Update state with error message and notify failure
                _authState.value = AuthState.Error(message)
                callback(false, message)
            }
        }
    }

    // Process sign-in results and update the user repository
    private fun onSignInResult(authResult: AuthResult) {
        val user = authResult.user
        if (user != null) {
            // Add user data to the local repository
            UserRepository.addUser(
                UserData(
                    id = user.uid,
                    name = user.displayName ?: user.email?.substringBefore('@') ?: "User",
                    email = user.email ?: "",
                    isAnonymous = user.isAnonymous,
                    profilePictureUrl = user.photoUrl?.toString(),
                    signInTime = System.currentTimeMillis()
                )
            )
        }
    }

    // Delete the current account and sign in as a guest without signing out
    fun deleteAccountStayInApp(callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: throw Exception("No user signed in.")
                val deletedUserId = currentUser.uid

                // Delete the current Firebase account
                currentUser.delete().await()

                // Remove the deleted user from the local repository
                UserRepository.removeUser(deletedUserId)

                // Sign in anonymously
                val result = auth.signInAnonymously().await()
                val newUser = result.user ?: throw Exception("Anonymous sign-in failed")

                // Create guest user data
                val guestUser = UserData(
                    id = newUser.uid,
                    name = "Guest",
                    email = "guest@example.com",
                    isAnonymous = true,
                    profilePictureUrl = null
                )

                // Add guest user to the repository
                UserRepository.addUser(guestUser)

                // Update auth state
                updateCurrentUser()

                // Notify success
                callback(true, null)
            } catch (e: Exception) {
                // Notify failure with error message
                callback(false, e.message)
            }
        }
    }

    // Retrieve all saved accounts from the repository
    fun getAvailableAccounts(): List<UserData> {
        return UserRepository.users
    }

    // Remove a specific account from the repository
    fun removeAccount(userId: String) {
        UserRepository.removeUser(userId)
    }

    // Clear all saved accounts from the repository
    fun clearAllAccounts() {
        UserRepository.clearUsers()
    }
}

// Singleton object to manage local user data
object UserRepository {
    // Thread-safe list to store user data
    private val _users = mutableStateListOf<UserData>()

    // Get the list of users
    val users: List<UserData>
        get() = _users.toList()

    // Add a user to the repository, ensuring no duplicates
    @Synchronized
    fun addUser(user: UserData) {
        if (!_users.any { it.id == user.id }) {
            _users.add(user)
        }
    }

    // Remove a user from the repository by ID
    @Synchronized
    fun removeUser(userId: String) {
        _users.removeAll { it.id == userId }
    }

    // Clear all users from the repository
    @Synchronized
    fun clearUsers() {
        _users.clear()
    }
}




// Data class to represent user information
data class UserData(
    val id: String,
    val name: String,
    val email: String,
    val isAnonymous: Boolean,
    val profilePictureUrl: String? = null,
    val signInTime: Long = System.currentTimeMillis()
)