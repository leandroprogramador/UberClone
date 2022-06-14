package com.leandro.uberclone

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.bumptech.glide.Glide
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.leandro.uberclone.databinding.ActivityDriverHomeBinding
import com.leandro.uberclone.utils.Common
import com.leandro.uberclone.utils.UserUtils

class DriverHomeActivity : AppCompatActivity() {

    private var imageUri: Uri? = null
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityDriverHomeBinding
    private lateinit var navView: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController : NavController
    private lateinit var imgAvatar : ImageView
    private lateinit var waitingDialog : AlertDialog
    private lateinit var storageReference: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDriverHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarDriverHome.toolbar)

        drawerLayout = binding.drawerLayout
        navView= binding.navView
        navController = findNavController(R.id.nav_host_fragment_content_driver_home)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        init()
    }

    private fun init() {
        storageReference = FirebaseStorage.getInstance().getReference()
        waitingDialog = AlertDialog.Builder(this)
            .setMessage("Waiting...")
            .setCancelable(false)
            .create()

        navView.setNavigationItemSelectedListener {
            if (it.itemId == R.id.nav_sign_out) {
                val builder = AlertDialog.Builder(this@DriverHomeActivity)
                builder.setTitle("Sign out")
                    .setMessage("Do you really want to sign out?")
                    .setNegativeButton("CANCEL", { dialog, which -> dialog.dismiss() })
                    .setPositiveButton("SIGN OUT") { dialog, _ ->
                        FirebaseAuth.getInstance().signOut()
                        startActivity(
                            Intent(
                                this@DriverHomeActivity,
                                SplashScreenActivity::class.java
                            ).also { it ->
                                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                        finish()

                    }
                    .setCancelable(false)
                val dialog = builder.create()
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                        .setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
                }
                dialog.show()
            }
            return@setNavigationItemSelectedListener true
        }

        val headerView = navView.getHeaderView(0)
        val txtName = headerView.findViewById<TextView>(R.id.txt_name)
        val txtPhone = headerView.findViewById<TextView>(R.id.txt_phone)
        val txtStar = headerView.findViewById<TextView>(R.id.txt_start)
        imgAvatar = headerView.findViewById<ImageView>(R.id.img_avatar)
        txtName.text = Common.buildWelcomeMessage()
        txtPhone.text = Common.currentUser!!.phoneNumber
        txtStar.text = StringBuilder().append(Common.currentUser!!.rating)

        if(Common.currentUser != null && !Common.currentUser!!.avatar.isNullOrEmpty()){
            Glide.with(this)
                .load(Common.currentUser!!.avatar)
                .into(imgAvatar)
        }

        imgAvatar.setOnClickListener {
            val imgIntent = Intent().also {
                it.type = "image/*"
                it.action = Intent.ACTION_GET_CONTENT

            }
            avatarLauncher.launch(Intent.createChooser(imgIntent,"Select photo"))

        }
    }

    private val avatarLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == RESULT_OK){
            it.data?.let { uri ->
                imageUri = uri.data
                imgAvatar.setImageURI(imageUri)
                showDialogUpload()
            }
        }
    }

    fun showDialogUpload(){
        val builder = AlertDialog.Builder(this@DriverHomeActivity)
        builder.setTitle("Change avayat")
            .setMessage("Do you really want change your avatar?")
            .setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("CHANGE") { _, _ ->
                imageUri?.let { uri ->
                    waitingDialog.show()
                    val avatarFolder = storageReference.child("avatars/" + FirebaseAuth.getInstance().currentUser!!.uid)
                    avatarFolder.putFile(imageUri!!)
                        .addOnFailureListener { e ->
                            Snackbar.make(drawerLayout, e.message!!, Snackbar.LENGTH_LONG).show()
                            waitingDialog.dismiss()
                        }.addOnCompleteListener { task ->
                            if(task.isSuccessful){
                                avatarFolder.downloadUrl.addOnSuccessListener { uri ->
                                    val updateData = HashMap<String, Any>()
                                    updateData["avatar"] = uri.toString()
                                    UserUtils.updateUser(drawerLayout, updateData)

                                }
                            }
                            waitingDialog.dismiss()
                        }.addOnProgressListener { taskSnapshot ->
                            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                            waitingDialog.setMessage(StringBuilder("Uploading: ")
                                .append(progress)
                                .append("%")
                            )

                        }
                }

            }
            .setCancelable(false)
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
        }
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.driver_home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_driver_home)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}