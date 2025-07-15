import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

object GoogleSignInHelper {
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e("GoogleSignIn", "Google Play Services unavailable: $resultCode")
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                (context as? Activity)?.let {
                    googleApiAvailability.getErrorDialog(it, resultCode, 9000)?.show()
                }
            }
            throw IllegalStateException("Google Play Services unavailable: $resultCode")
        }
        val webClientId = "634409104545-t9obf7nmakk2jhahr31jlaspva4858fb.apps.googleusercontent.com"
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }
}