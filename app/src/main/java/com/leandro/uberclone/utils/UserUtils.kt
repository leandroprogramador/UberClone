package com.leandro.uberclone.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.leandro.uberclone.SplashScreenActivity
import com.leandro.uberclone.model.TokenModel
import com.leandro.uberclone.services.MyFirebaseMessagingService

object UserUtils {
    fun updateUser(view: View?, updateData: Map<String, Any>) {
        FirebaseDatabase.getInstance()
            .getReference(Common.DRIVER_INFO_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .updateChildren(updateData)
            .addOnFailureListener { e ->
                Snackbar.make(view!!, e.message!!, Snackbar.LENGTH_LONG).show()
            }.addOnSuccessListener {
                Snackbar.make(view!!, "Update information success", Snackbar.LENGTH_LONG).show()
            }
    }

    fun updateToken(context : Context, token: String) {
        val tokenModel = TokenModel().also { it.token = token }
        FirebaseDatabase.getInstance()
            .getReference(Common.TOKEN_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .setValue(tokenModel)
            .addOnFailureListener {
                    e -> Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
            .addOnSuccessListener {
                Toast.makeText(context, token, Toast.LENGTH_LONG).show()
            }

    }
}