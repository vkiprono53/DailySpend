package com.vkiprono.dailyspend

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.vkiprono.dailyspend.models.User
import kotlinx.android.synthetic.main.activity_signup.*
import java.util.*
import java.util.jar.Manifest

class SignupActivity : AppCompatActivity() {

    private var firebaseAuth: FirebaseAuth? = null
    private var firebaseDatabase: FirebaseDatabase? = null
    private var firebaseStorage: FirebaseStorage? = null

    var imgUri: Uri? = null
    var downloadUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()
    }


    fun signup(view: View) {


        val username = etSignupName.text.toString()
        val email = etSignupEmail.text.toString()
        val password = etSignupPassword.text.toString()

        if (username.isEmpty()) {
            etSignupName.error = "Name required"
        }

        if (email.isEmpty()) {
            etSignupEmail.error = "Email required"
            return
        }
        if (password.isEmpty()) {
            etSignupPassword.error = "Password required"
            return
        }


        firebaseAuth!!.createUserWithEmailAndPassword(email, password)

            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    uploadImage()

                    Log.d("CREATING", "SUCCESSFULLY CREATED USER")

                    Toast.makeText(
                        applicationContext,
                        "Account Successfully created..Log in now",
                        Toast.LENGTH_SHORT
                    ).show()


                    val intent = Intent(applicationContext, SigninActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(applicationContext, exception.localizedMessage, Toast.LENGTH_SHORT)
                    .show()

            }

    }

    private fun uploadImage() {
        if (imgUri == null){
            return
        }

        Log.d("Signup Activity","Beginning to save to the db")
        val imageName = UUID.randomUUID()
        val storageRef = FirebaseStorage.getInstance().getReference("images/$imageName")

        storageRef.putFile(imgUri!!).addOnCompleteListener {
            task ->
            if (task.isSuccessful){


                storageRef.downloadUrl.addOnSuccessListener {
                    downloadUri = it.toString()
                    Log.d("SIGNUP ACTIVITY","THE DOWNLOAD URI INSIDE UPLOAD IMAGE IS======>$downloadUri")
                    uploadDataToDatabase(downloadUri!!)

                }

            }
        }
            .addOnFailureListener {
                exception ->
                Toast.makeText(applicationContext, exception.localizedMessage, Toast.LENGTH_SHORT).show()
            }

    }


    fun uploadDataToDatabase(imageUri:String){

        val uid  = firebaseAuth!!.uid

        val dbRef = FirebaseDatabase.getInstance().getReference("users/$uid")


        val username = etSignupName.text.toString()
        val password = etSignupPassword.text.toString()
        val email = etSignupEmail.text.toString()

        Log.d("SIGNUP ACTIVITY","THE DOWNLOAD URI INSIDE UPLOADDATATODATABASE IS======>$imageUri")

        val user = User(username,password, imageUri, email)


        dbRef.setValue(user).addOnCompleteListener {
            task ->
            if (task.isSuccessful){
                Toast.makeText(applicationContext, "User data succesfully uploaded to the database", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            exception ->
            Toast.makeText(applicationContext, exception.localizedMessage, Toast.LENGTH_SHORT).show()
        }


    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun selectImage(view: View) {

        Log.d("Signup", "Beginning to select the image")

        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){

            requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }
        else {

            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent, "Select Picture"),2)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        if (requestCode == 1){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                startActivityForResult(intent,2)
            }

        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode==2){
            if (data!=null && resultCode == Activity.RESULT_OK){

                imgUri = data.data

                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imgUri)
                img_signup.setImageBitmap(bitmap)
            }
        }
    }


    fun signin(view: View) {

        val intent = Intent(applicationContext, SigninActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
