package com.blesense.app

// Import necessary Android and Compose libraries for UI, authentication, and Google Sign-In
import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

// Enable experimental Material3 API
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel, // Authentication view model
    onNavigateToLogin: () -> Unit, // Callback for navigating to login screen
    onNavigateToHome: () -> Unit // Callback for navigating to home screen
) {
    // Observe theme and language state
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()

    // Initialize translated text with cached values or defaults
    var translatedText by remember {
        mutableStateOf(
            TranslatedRegisterScreenText(
                createAccount = TranslationCache.get("Create Account-$currentLanguage") ?: "Create Account",
                signUpToGetStarted = TranslationCache.get("Sign up to get started-$currentLanguage") ?: "Sign up to get started",
                usernamePlaceholder = TranslationCache.get("Username-$currentLanguage") ?: "Username",
                emailPlaceholder = TranslationCache.get("Email-$currentLanguage") ?: "Email",
                passwordPlaceholder = TranslationCache.get("Password-$currentLanguage") ?: "Password",
                confirmPasswordPlaceholder = TranslationCache.get("Confirm Password-$currentLanguage") ?: "Confirm Password",
                createAccountButton = TranslationCache.get("Create Account-$currentLanguage") ?: "Create Account",
                orContinueWith = TranslationCache.get("Or continue with-$currentLanguage") ?: "Or continue with",
                alreadyHaveAccount = TranslationCache.get("Already have an account?-$currentLanguage") ?: "Already have an account?",
                loginNow = TranslationCache.get("Login Now-$currentLanguage") ?: "Login Now",
                creatingAccount = TranslationCache.get("Creating Account-$currentLanguage") ?: "Creating Account"
            )
        )
    }

    // Preload translations when language changes
    LaunchedEffect(currentLanguage) {
        val translator = GoogleTranslationService()
        val textsToTranslate = listOf(
            "Create Account", "Sign up to get started", "Username", "Email", "Password",
            "Confirm Password", "Create Account", "Or continue with", "Already have an account?",
            "Login Now", "Creating Account"
        )
        val translatedList = translator.translateBatch(textsToTranslate, currentLanguage)
        // Update translated text state
        translatedText = TranslatedRegisterScreenText(
            createAccount = translatedList[0],
            signUpToGetStarted = translatedList[1],
            usernamePlaceholder = translatedList[2],
            emailPlaceholder = translatedList[3],
            passwordPlaceholder = translatedList[4],
            confirmPasswordPlaceholder = translatedList[5],
            createAccountButton = translatedList[6],
            orContinueWith = translatedList[7],
            alreadyHaveAccount = translatedList[8],
            loginNow = translatedList[9],
            creatingAccount = translatedList[10]
        )
    }

    // Define theme-based colors
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color.White // Screen background
    val textColor = if (isDarkMode) Color.White else Color.Black // Primary text
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF8E8E93) // Secondary text
    val textFieldBackgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFFFFFFF) // Text field background
    val buttonBackgroundColor = if (isDarkMode) Color(0xFFBB86FC) else Color(0xFF007AFF) // Button background
    val buttonTextColor = if (isDarkMode) Color.Black else Color.White // Button text
    val dividerColor = if (isDarkMode) Color(0xFFB0B0B0) else Color.LightGray // Divider
    val borderColor = if (isDarkMode) Color(0xFFB0B0B0) else Color.LightGray // Border

    // State variables for form inputs and validation
    var username by remember { mutableStateOf("") } // Username input
    var email by remember { mutableStateOf("") } // Email input
    var password by remember { mutableStateOf("") } // Password input
    var confirmPassword by remember { mutableStateOf("") } // Confirm password input
    var passwordVisible by remember { mutableStateOf(false) } // Password visibility toggle
    var confirmPasswordVisible by remember { mutableStateOf(false) } // Confirm password visibility toggle
    var isUsernameValid by remember { mutableStateOf(false) } // Username validation state
    var isEmailValid by remember { mutableStateOf(false) } // Email validation state
    var isPasswordValid by remember { mutableStateOf(false) } // Password validation state
    var isConfirmPasswordValid by remember { mutableStateOf(false) } // Confirm password validation state

    // Get current context
    val context = LocalContext.current
    // Launcher for Google Sign-In
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Handle successful Google Sign-In
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { viewModel.signInWithGoogle(it) }
            } catch (e: ApiException) {
                // Log and handle Google Sign-In failure
                Log.e("AuthViewModel", "Google Sign-In Failed", e)
                viewModel.handleGoogleSignInError("Google Sign-In failed: ${e.statusCode} - ${e.message}")
            }
        } else {
            // Log and handle Google Sign-In cancellation
            Log.w("AuthViewModel", "Google Sign-In Cancelled: ${result.resultCode}")
            viewModel.handleGoogleSignInError("Google Sign-In was cancelled")
        }
    }

    // Initialize Google Sign-In client
    val googleSignInClient = remember { GoogleSignInHelper.getGoogleSignInClient(context) }
    LaunchedEffect(Unit) {
        viewModel.setGoogleSignInClient(googleSignInClient)
    }

    // Observe authentication state
    val authState by viewModel.authState.collectAsState()

    // Validate username (minimum 4 characters, alphanumeric and underscore)
    fun validateUsername(value: String): Boolean {
        return value.length >= 4 && value.matches(Regex("^[a-zA-Z0-9_]+$"))
    }

    // Validate email format
    fun validateEmail(value: String): Boolean {
        return value.matches(Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$"))
    }

    // Validate password (minimum 8 characters, with uppercase, lowercase, digit, and special character)
    fun validatePassword(value: String): Boolean {
        return value.length >= 8 &&
                Regex("[A-Z]").containsMatchIn(value) && // At least one uppercase
                Regex("[a-z]").containsMatchIn(value) && // At least one lowercase
                Regex("\\d").containsMatchIn(value) &&   // At least one digit
                Regex("[!@#\$%^&()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]").containsMatchIn(value) // At least one special char
    }

    // Show toast if username exceeds 4 characters
    LaunchedEffect(username) {
        if (username.length > 4) {
            Toast.makeText(context, "Username must be less than 8 chars, digits, and _ allowed", Toast.LENGTH_SHORT).show()
        }
    }

    // Show toast if password exceeds 8 characters
    LaunchedEffect(password) {
        if (password.length > 8) {
            Toast.makeText(
                context,
                "Password must be more than 8 chars , digits  & special char",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Show loading dialog during authentication
    if (authState is AuthState.Loading) {
        AlertDialog(
            onDismissRequest = { }, // Non-dismissable dialog
            title = { Text(translatedText.creatingAccount, color = textColor) },
            text = {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator(color = buttonBackgroundColor)
                }
            },
            confirmButton = { }, // No confirm button
            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
        )
    }

    // Handle authentication state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                // Log success and navigate to home screen
                println("User registered: ${(authState as AuthState.Success).user.email}")
                onNavigateToHome()
            }
            is AuthState.Error -> {
                // Log error
                println("Error: ${(authState as AuthState.Error).message}")
            }
            else -> Unit
        }
    }

    // Main UI layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        // Title text
        Text(
            text = translatedText.createAccount,
            style = TextStyle(
                fontSize = 34.sp,
                fontFamily = helveticaFont,
                fontWeight = FontWeight.Bold,
                color = textColor
            ),
            textAlign = TextAlign.Center
        )
        // Subtitle text
        Text(
            text = translatedText.signUpToGetStarted,
            style = TextStyle(
                fontSize = 17.sp,
                color = secondaryTextColor,
                fontFamily = helveticaFont,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(60.dp))

        // Username input field
        TextField(
            value = username,
            onValueChange = {
                username = it
                isUsernameValid = validateUsername(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text(translatedText.usernamePlaceholder, color = secondaryTextColor) },
            isError = username.isNotEmpty() && !isUsernameValid,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            colors = TextFieldDefaults.textFieldColors(
                containerColor = textFieldBackgroundColor,
                unfocusedIndicatorColor = borderColor,
                focusedIndicatorColor = buttonBackgroundColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(
                fontSize = 17.sp,
                fontFamily = helveticaFont,
                color = textColor
            )
        )

        // Email input field
        TextField(
            value = email,
            onValueChange = {
                email = it
                isEmailValid = validateEmail(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text(translatedText.emailPlaceholder, color = secondaryTextColor) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            isError = email.isNotEmpty() && !isEmailValid,
            colors = TextFieldDefaults.textFieldColors(
                containerColor = textFieldBackgroundColor,
                unfocusedIndicatorColor = borderColor,
                focusedIndicatorColor = buttonBackgroundColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(
                fontSize = 17.sp,
                fontFamily = helveticaFont,
                color = textColor
            )
        )
        // Track interaction for password field
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()

        // Password input field
        TextField(
            value = password,
            onValueChange = {
                password = it
                isPasswordValid = validatePassword(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = {
                Text(
                    translatedText.passwordPlaceholder,
                    color = secondaryTextColor
                )
            },
            isError = password.isNotEmpty() && !isPasswordValid,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            trailingIcon = {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (passwordVisible) R.drawable.monkey else R.drawable.eyes
                        ),
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = Color.Unspecified
                    )
                }
            },
            interactionSource = interactionSource,
            colors = TextFieldDefaults.textFieldColors(
                containerColor = textFieldBackgroundColor,
                unfocusedIndicatorColor = borderColor,
                focusedIndicatorColor = if (isFocused && isPasswordValid) Color.Blue else buttonBackgroundColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                errorIndicatorColor = Color.Red,
                cursorColor = if (isPasswordValid) Color.Blue else buttonBackgroundColor
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(
                fontSize = 17.sp,
                fontFamily = helveticaFont,
                color = textColor
            )
        )

        // Confirm password input field
        TextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                isConfirmPasswordValid = password == it
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(translatedText.confirmPasswordPlaceholder, color = secondaryTextColor) },
            isError = confirmPassword.isNotEmpty() && !isConfirmPasswordValid,
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            trailingIcon = {
                IconButton(
                    onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (confirmPasswordVisible) R.drawable.monkey else R.drawable.eyes
                        ),
                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                        tint = Color.Unspecified
                    )
                }
            },
            colors = TextFieldDefaults.textFieldColors(
                containerColor = textFieldBackgroundColor,
                unfocusedIndicatorColor = borderColor,
                focusedIndicatorColor = buttonBackgroundColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(
                fontSize = 17.sp,
                fontFamily = helveticaFont,
                color = textColor
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Create account button
        Button(
            onClick = {
                if (isUsernameValid && isEmailValid && isPasswordValid && password == confirmPassword) {
                    viewModel.registerUser(email, password)
                }
            },
            enabled = isUsernameValid && isEmailValid && isPasswordValid && isConfirmPasswordValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonBackgroundColor,
                disabledContainerColor = buttonBackgroundColor.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = translatedText.createAccountButton,
                style = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = helveticaFont,
                    color = buttonTextColor
                )
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Social login divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = dividerColor
            )
            Text(
                text = translatedText.orContinueWith,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = TextStyle(
                    fontSize = 15.sp,
                    color = secondaryTextColor,
                    fontFamily = helveticaFont,
                    fontWeight = FontWeight.Bold
                )
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = dividerColor
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Social login button (Google)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SocialLoginButton(
                icon = R.drawable.google_g,
                onClick = { launcher.launch(googleSignInClient.signInIntent) },
                backgroundColor = textFieldBackgroundColor,
                borderColor = borderColor
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Login prompt
        Row(
            modifier = Modifier.padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = translatedText.alreadyHaveAccount,
                style = TextStyle(
                    fontSize = 15.sp,
                    color = textColor,
                    fontFamily = helveticaFont,
                    fontWeight = FontWeight.SemiBold
                )
            )
            TextButton(onClick = onNavigateToLogin) {
                Text(
                    text = translatedText.loginNow,
                    style = TextStyle(
                        fontSize = 15.sp,
                        color = buttonBackgroundColor,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = helveticaFont
                    )
                )
            }
        }
    }
}

// Data class for translatable text
data class TranslatedRegisterScreenText(
    val createAccount: String, // Title for create account
    val signUpToGetStarted: String, // Subtitle
    val usernamePlaceholder: String, // Username field placeholder
    val emailPlaceholder: String, // Email field placeholder
    val passwordPlaceholder: String, // Password field placeholder
    val confirmPasswordPlaceholder: String, // Confirm password field placeholder
    val createAccountButton: String, // Create account button text
    val orContinueWith: String, // Social login divider text
    val alreadyHaveAccount: String, // Login prompt text
    val loginNow: String, // Login button text
    val creatingAccount: String // Loading dialog title
)