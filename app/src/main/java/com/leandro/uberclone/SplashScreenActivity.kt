package com.leandro.uberclone

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceIdReceiver
import com.google.firebase.iid.internal.FirebaseInstanceIdInternal
import com.google.firebase.messaging.FirebaseMessaging
import com.leandro.uberclone.model.DriverInfoModel
import com.leandro.uberclone.utils.Common
import com.leandro.uberclone.utils.UserUtils
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import java.lang.StringBuilder
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {

    companion object {
        private val LOGIN_REQUEST_CODE = 7171
    }

    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener
    lateinit var database : FirebaseDatabase
    lateinit var driverInfoRef : DatabaseReference
    lateinit var progressBar : ProgressBar


    private fun delaySplashScreen() {
        Completable.timer(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .subscribe {
                firebaseAuth.addAuthStateListener(listener)

            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        progressBar = findViewById(R.id.progress_bar)
        init()

    }

    private fun init() {
        database = FirebaseDatabase.getInstance()
        driverInfoRef = database.getReference(Common.DRIVER_INFO_REFERENCE)


        providers = listOf(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        firebaseAuth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener { mFirebaseAuth ->
            val user = mFirebaseAuth.currentUser
            user?.let {
                FirebaseMessaging.getInstance().token
                    .addOnFailureListener { e->
                        Toast.makeText(this@SplashScreenActivity, e.message, Toast.LENGTH_SHORT).show()
                } .addOnSuccessListener { Log.d("TOKEN", it)
                    UserUtils.updateToken(this@SplashScreenActivity, it)
                }
                checkUserFromFirebase()
            } ?: run {
                showLoginLayout()
            }
        }
    }

    private fun checkUserFromFirebase() {
        driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()){
                        val model = snapshot.getValue(DriverInfoModel::class.java)
                        goToHomeActivity(model)
                    } else {
                        showRegisterLayout()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SplashScreenActivity, error.message, Toast.LENGTH_SHORT).show()
                }

            })
    }

    private fun goToHomeActivity(model: DriverInfoModel?) {
        Common.currentUser = model
        startActivity(Intent(this, DriverHomeActivity::class.java))
        finish()
    }

    private fun showRegisterLayout() {

        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_register, null)
        val edtFirstName = itemView.findViewById<EditText>(R.id.edt_first_name)
        val edtLastName = itemView.findViewById<EditText>(R.id.edt_last_name)
        val edtPhone = itemView.findViewById<EditText>(R.id.edt_phone)
        val btnContinue = itemView.findViewById<Button>(R.id.btn_continue)

        val phoneNumber = FirebaseAuth.getInstance().currentUser!!.phoneNumber
        if(phoneNumber != null && !TextUtils.isDigitsOnly(phoneNumber)) edtPhone.setText(phoneNumber)

        val alertDialog = AlertDialog.Builder(this, R.style.DialogTheme).setView(itemView).create()

        btnContinue.setOnClickListener {
            if(TextUtils.isDigitsOnly(edtFirstName.text.toString())){
                Toast.makeText(this@SplashScreenActivity, "Please enter a valid first name!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else if(TextUtils.isDigitsOnly(edtLastName.text.toString())){
                Toast.makeText(this@SplashScreenActivity, "Please enter a valid last name!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else if(TextUtils.isDigitsOnly(edtPhone.text.toString())){
                Toast.makeText(this@SplashScreenActivity, "Please enter a valid phone number!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else{
                val model = DriverInfoModel().also {
                    it.firstName = edtFirstName.text.toString()
                    it.lastName = edtLastName.text.toString()
                    it.phoneNumber = edtPhone.text.toString()
                }
                driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                    .setValue(model)
                    .addOnFailureListener {
                        Toast.makeText(this@SplashScreenActivity, it.message, Toast.LENGTH_SHORT).show()
                        alertDialog.dismiss()
                        progressBar.visibility = View.GONE
                    }
                    .addOnSuccessListener {
                        Toast.makeText(this@SplashScreenActivity, "Register successfully!", Toast.LENGTH_SHORT).show()
                        alertDialog.dismiss()
                        goToHomeActivity(model)
                        progressBar.visibility = View.GONE
                    }

            }
        }
        alertDialog.show()

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