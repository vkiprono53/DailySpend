package com.vkiprono.dailyspend

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.vkiprono.dailyspend.models.ItemSpend
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.display_items_spend.view.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class HomeActivity : AppCompatActivity() {

    var firebaseAuth: FirebaseAuth? = null
    var firebaseDatabase: FirebaseDatabase? = null
    var firebaseStorage: FirebaseStorage? = null

    var firebaseRef:DatabaseReference?=null

    private var item: String? = null
    private var description: String? = null
    private var amount: Int = 0

    private var postKey: String? = null

    private val keys = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()


        val user:FirebaseUser = FirebaseAuth.getInstance().currentUser!!

        val userId:String = user.uid

        firebaseRef = FirebaseDatabase.getInstance().getReference("items").child(userId)

        itemsFromDatabase()

    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.logout) {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(applicationContext, SigninActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun addItems(view: View) {
        Log.d("MAIN ACTIVITY", "BEGINNING TO ADD ITEMS----")

        val alertDialogView = LayoutInflater.from(this).inflate(R.layout.add_daily_items, null)

        val builder = AlertDialog.Builder(this)

        builder.setView(alertDialogView)

        val alertDialog = builder.create()


        alertDialog.show()


        val addButton = alertDialogView.findViewById<Button>(R.id.btnSave)

        val cancelSaveButton = alertDialogView.findViewById<Button>(R.id.btnCancelSave)

        addButton.setOnClickListener {


            val item = alertDialogView.findViewById<EditText>(R.id.etInputItem)

            val description = alertDialogView.findViewById<EditText>(R.id.etInputDescription)

            val amount = alertDialogView.findViewById<EditText>(R.id.etInputAmount)


            val inputItem: String = item!!.text.toString().trim()
            val inputDescription: String = description!!.text.toString().trim()
//            val inputAmount: String = amount!!.text.toString().trim()
//
//            val intAmount = inputAmount.toInt()


            val intAmount = amount.text.toString().toIntOrNull()
          //  val intAmount = inputAmount != null


            Log.d("Item", "Type is======>$inputItem")
            Log.d("Description", "Note is======>$inputDescription")
            Log.d("Amount", "Amount is======>$intAmount")



            if (inputItem.isEmpty() || inputItem.isBlank()) {
                item.error = "Item required"
                return@setOnClickListener

            }

            if (intAmount == null) {
                amount.error = "Amount required"
                return@setOnClickListener

            }


            if (inputDescription.isEmpty() || inputDescription.isBlank()) {
                description.error = "Description required"
                return@setOnClickListener

            }

            //Saving to the database
            Log.d("SAVING TO DB", "BEGINNING TO SAVE TO THE DATABASE")

            val uuid = UUID.randomUUID().toString()
            val userId = firebaseAuth!!.uid


            val date = LocalDate.now()
            Log.d("SAVING TO DB", "DATE IS============>$date")

            val formatter = DateTimeFormatter.ofPattern("dd, MM yyyy")
            val text = date.format(formatter)
            val parsedDate = LocalDate.parse(text, formatter)



            Log.d("SAVING TO DB", "parsedDate DATE IS============>$parsedDate")

         //   val itemRef = FirebaseDatabase.getInstance().getReference("items")

            postKey = firebaseRef!!.push().key
            Log.d("HOMEACTIVITY", "POSTKEY TO BE SAVED IS:::$postKey")

            val itemSpend = ItemSpend(inputItem, inputDescription, parsedDate.toString(), intAmount)


            firebaseRef!!.child(postKey!!).setValue(itemSpend).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("HOMEACTIVITY", "Item successfully saved")
                    Toast.makeText(
                        applicationContext,
                        "Item Successfully saved!!",
                        Toast.LENGTH_SHORT
                    ).show()

                    alertDialog.dismiss()
                }
            }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        applicationContext,
                        exception.localizedMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                    Toast.makeText(
                        applicationContext,
                        "Cannot save your items, Please try again!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnFailureListener

                }
            Log.d("HOMEACTIVITY", "ENDING TO SAVE TO THE DATABASE")


        }

        cancelSaveButton.setOnClickListener {
            Log.d("HOME ACTIVITY", "CANCEL SAVE BUTTON!!")
            alertDialog.cancel()
        }


    }


    fun itemsFromDatabase() {


        val adapter = GroupAdapter<GroupieViewHolder>()


        firebaseRef!!.addValueEventListener(object : ValueEventListener {


            override fun onDataChange(p0: DataSnapshot) {
                adapter.clear()
                keys.clear()
                homeTotalAmount.text = "0000"
                var totalAmount = 0


                p0.children.forEach {

                    val itemSpend = it.getValue(ItemSpend::class.java)

                    adapter.add(SpendItems(itemSpend!!))
                    totalAmount += itemSpend.amount

                    homeTotalAmount.text = totalAmount.toString()

                    keys.add(it.key!!)

                }

                homeRecyclerView.adapter = adapter


            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(applicationContext, p0.message, Toast.LENGTH_SHORT).show()
            }

        })
    }

    inner class SpendItems(val itemsSpend: ItemSpend) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int {
            return R.layout.display_items_spend
        }

        @SuppressLint("NewApi")
        @TargetApi(Build.VERSION_CODES.O)
        @RequiresApi(Build.VERSION_CODES.O)
        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            postKey = keys[position]


            viewHolder.itemView.tvdbAmount.text = itemsSpend.amount.toString()
            viewHolder.itemView.tvdbItem.text = itemsSpend.item
            viewHolder.itemView.tvdbDate.text = itemsSpend.date
            viewHolder.itemView.tvdbDesc.text = itemsSpend.description

            //Updating Here:

            Log.d("HOME ACTIVITY", "THE KEY FOR THE SELECTED ITEM IS----->> : $postKey")


            val tvUpdate = viewHolder.itemView.findViewById<TextView>(R.id.tvEditItem)


            tvUpdate.setOnClickListener {
                Log.d("HOMEACTIVITY", "BEGINNING OF UPDATE!!!")


                item = itemsSpend.item.toString()
                description = itemsSpend.description
                amount = itemsSpend.amount.toString().toInt()
                postKey = keys[position]

                Log.d("HOME ACTIVITY", "THE KEY FOR THE ITEM ON CLICK UPDATE=====>$postKey")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    updateItems(postKey!!)
                }
            }


            //Deleting here:::

            val tvDelete = viewHolder.itemView.findViewById<TextView>(R.id.tvDeleteItem)

            tvDelete.setOnClickListener {
                postKey = keys[position]

                Log.d("HOME ACTIVITY", "THE KEY FOR THE ITEM ON CLICK DELETE=====>$postKey")

                deleteItems(postKey!!)

            }


        }

        fun deleteItems(postKey:String) {
            Log.d("MAIN ACTIVITY", "BEGINNING TO DELETE ITEMS")

            Log.d("HOME ACTIVITY", "THE KEY FOR THE SELECTED ITEM IS : $postKey")

            val alertDialogView = AlertDialog.Builder(this@HomeActivity)
                .setTitle("Delete")
                .setMessage("Do you want to Delete?")
                .setPositiveButton("Delete", DialogInterface.OnClickListener { dialog, which ->

                    Log.d("HOME ACTIVITY", "THE KEY FOR THE SELECTED ITEM IS : $postKey")


                    firebaseRef!!.child(postKey!!).setValue(null).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("HOME ACTIVITY", "THE KEY SUCCESSFULLY DELETED $postKey")

                            Toast.makeText(
                                applicationContext,
                                "Successfully deleted ${itemsSpend.item}",
                                Toast.LENGTH_SHORT
                            ).show()

                            dialog.dismiss()
                        }
                    }

                })

                .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
                    dialog.cancel()

                })
                .create()
            alertDialogView.show()


        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun updateItems(postKey:String) {

            Log.d("MAIN ACTIVITY", "BEGINNING TO EDIT ITEMS---->")

        //    Log.d("HOME ACTIVITY", "THE KEY FOR THE SELECTED ITEM IS : $postKey")


            val alertDialogView = LayoutInflater.from(this@HomeActivity).inflate(R.layout.update_items_spend, null)

            val builder = AlertDialog.Builder(this@HomeActivity)

            builder.setView(alertDialogView)

            val alertDialog = builder.create()


            alertDialog.show()


            val updateItem = alertDialogView.findViewById<EditText>(R.id.etInputUpdateItem)

            val updateDescription = alertDialogView.findViewById<EditText>(R.id.etInputUpdateDescription)

            val updateAmount = alertDialogView.findViewById<EditText>(R.id.etInputUpdateAmount)


            updateItem.setText(item)
            updateItem.setSelection(item!!.length)

            updateDescription.setText(description)
            updateDescription.setSelection(description!!.length)

            updateAmount.setText(amount.toString())
            updateAmount.setSelection(amount.toString().length)

            val updateButton = alertDialog.findViewById<Button>(R.id.btnUpdateItem)
            val cancelUpdateButton = alertDialog.findViewById<Button>(R.id.btnCancelUpdate)


            updateButton!!.setOnClickListener {


                val inputUpdateItem: String = updateItem!!.text.toString().trim()
                val inputUpdateDescription: String = updateDescription!!.text.toString().trim()
//                val inputUpdateAmount: String = updateAmount!!.text.toString().trim()
//
//                val intAmount = inputUpdateAmount.toInt()

                val intAmount = updateAmount!!.text.toString().toIntOrNull()



                Log.d("Item", "Type is======>$inputUpdateItem")
                Log.d("Description", "Note is======>$inputUpdateDescription")
                Log.d("Amount", "Amount is======>$intAmount")


                if (inputUpdateItem.isEmpty() || inputUpdateItem.isBlank()) {
                    updateItem.error = "Item required"
                    return@setOnClickListener

                }

                if (intAmount == null) {
                    updateAmount.error = "Amount required"
                    return@setOnClickListener

                }
/*

            if (intAmount.isBlank() || intAmount.isEmpty()) {
                amount.error = "Amount required"
                return@setOnClickListener

            }
*/

                if (inputUpdateDescription.isEmpty() || inputUpdateDescription.isBlank()) {
                    updateDescription.error = "Description required"
                    return@setOnClickListener

                }

                //Saving to the database
                Log.d("SAVING TO DB", "BEGINNING TO SAVE TO THE DATABASE")


                val date = LocalDate.now()
                Log.d("SAVING TO DB", "DATE IS============>$date")

                val formatter = DateTimeFormatter.ofPattern("dd, MM yyyy")
                val text = date.format(formatter)
                val parsedDate = LocalDate.parse(text, formatter)



                Log.d("SAVING TO DB", "parsedDate DATE IS============>$parsedDate")

           //     val itemRef = FirebaseDatabase.getInstance().getReference("items")

             //   postKey = firebaseRef!!.key // FirebaseDatabase.getInstance().getReference("/items").key
            //    Log.d("HOME ACTIVITY", "KEY HAPA FOR UPDATE====>$postKey")

                val itemUpdateSpend = ItemSpend(inputUpdateItem, inputUpdateDescription, parsedDate.toString(), intAmount)


                firebaseRef!!.child(postKey).setValue(itemUpdateSpend).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("HOMEACTIVITY", "Item successfully saved")
                        Toast.makeText(
                            applicationContext,
                            "Item Successfully Updated!!",
                            Toast.LENGTH_SHORT
                        ).show()

                        alertDialog.dismiss()
                    }
                }
                    .addOnFailureListener { exception ->
                        Toast.makeText(
                            applicationContext,
                            exception.localizedMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                        Toast.makeText(
                            applicationContext,
                            "Cannot Update your items, Please try again!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@addOnFailureListener

                    }
                Log.d("HOMEACTIVITY", "ENDING TO SAVE TO THE DATABASE")


            }

            cancelUpdateButton!!.setOnClickListener {
                Log.d("JHOME ACTIVITY", "CANCEL UPDATES")
                alertDialog.cancel()
            }


        }


    }


}
