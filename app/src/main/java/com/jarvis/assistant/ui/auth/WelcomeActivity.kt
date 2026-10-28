package com.jarvis.assistant.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.jarvis.assistant.JarvisApp
import com.jarvis.assistant.R
import com.jarvis.assistant.ui.home.MainActivity
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

class WelcomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check login state
        val currentUser = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        } catch (_: Throwable) {
            null
        }
        val prefs = JarvisApp.instance.preferences
        if (currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContent {
            var showIntro by remember { mutableStateOf(true) }
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current

            MaterialTheme {
                Surface(color = Color.White) {
                    if (showIntro) {
                        CinematicIntro(onFinished = { showIntro = false })
                    } else {
                        WelcomeScreen(
                            onRegister = { startActivity(Intent(this, RegisterActivity::class.java)) },
                            onLogin = { startActivity(Intent(this, LoginActivity::class.java)) },
                            onGoogleSignIn = {
                                val credentialManager = androidx.credentials.CredentialManager.create(context)
                                val clientId = context.getString(R.string.default_web_client_id)
                                val googleIdOption = com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
                                val request = androidx.credentials.GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
                                
                                coroutineScope.launch {
                                    try {
                                        val result = credentialManager.getCredential(this@WelcomeActivity, request)
                                        val credential = result.credential
                                        if (credential is androidx.credentials.CustomCredential && credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                            val googleIdToken = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data).idToken
                                            val authResult = JarvisApp.instance.authRepository.signInWithGoogle(googleIdToken)
                                            if (authResult.isSuccess) {
                                                startActivity(Intent(this@WelcomeActivity, MainActivity::class.java))
                                                finish()
                                            } else {
                                                android.widget.Toast.makeText(context, authResult.exceptionOrNull()?.message ?: "Sign-in failed", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                                        // Ignore
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, e.message ?: "Google Sign-In Error", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onActivateLicense = { startActivity(Intent(this, ActivateLicenseActivity::class.java)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(
    onRegister: () -> Unit,
    onLogin: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onActivateLicense: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 48.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_jarvis_logo),
                contentDescription = "Nusrat AI Logo",
                modifier = Modifier.size(96.dp),
                contentScale = ContentScale.Fit
			)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "NUSRAT AI",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF202020),
                letterSpacing = 2.sp
            )
            
            Text(
                text = "Your Intelligent AI Companion",
                fontSize = 16.sp,
                color = Color(0xFFFF7A00),
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Experience next-generation voice intelligence, secure website synchronization, and real-time AI automation.",
                fontSize = 14.sp,
                color = Color(0xFF777777),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onGoogleSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF202020)
                ),
                border = BorderStroke(1.dp, Color(0xFFF0E3D8))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A00))
            ) {
                Text("Get Started (Register)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Color(0xFFFF7A00)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF7A00))
            ) {
                Text("I Already Have an Account (Login)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onActivateLicense,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Color(0xFFE96500)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE96500))
            ) {
                Text("Activate License", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
