package com.leandro.uberclone

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {

    companion object {
        private val LOGIN_REQUEST_CODE = 7171
    }

    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener


    private fun delaySplashScreen() {
        Completable.timer(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .subscribe {
                firebaseAuth.addAuthStateListener(listener)

            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        init()

    }

    private fun init() {
        providers = listOf(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        firebaseAuth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener { mFirebaseAuth ->
            val user = mFirebaseAuth.currentUser
            user?.let {
                Toast.makeText(this@SplashScreenActivity, "Welcome: " + user.uid, Toast.LENGTH_SHORT).show()
            } ?: run {
                showLoginLayout()
            }
        }
    }

    private fun showLoginLayout(){
        val layoutMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.layout_sign_in)
            .setPhoneButtonId(R.id.btn_phone_sign_in)
            .setGoogleButtonId((R.id.btn_google_sign_in))
            .build()
        val intentLogin = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAuthMethodPickerLayout(layoutMethodPickerLayout)
            .setTheme(R.style.LoginTheme)
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false)
            .build()
        loginLauncher.launch(intentLogin)
    }

    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        val response = IdpResponse.fromResultIntent(it.data)
        if(it.resultCode == Activity.RESULT_OK){
            val user = FirebaseAuth.getInstance().currentUser
            Toast.makeText(this@SplashScreenActivity, "Welcome: " + user!!.uid, Toast.LENGTH_SHORT).show()
        } else{
            Toast.makeText(this@SplashScreenActivity, response!!.error!!.message, Toast.LENGTH_SHORT).show()

        }
    }

    override fun onStart() {
        super.onStart()
        delaySplashScreen()
    }

    override fun onStop() {
        if (firebaseAuth != null && listener != null) firebaseAuth.removeAuthStateListener(listener)
        super.onStop()
    }
}