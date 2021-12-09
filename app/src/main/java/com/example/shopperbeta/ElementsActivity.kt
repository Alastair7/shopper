package com.example.shopperbeta

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shopperbeta.adapters.ElementAdapter
import com.example.shopperbeta.adapters.UserListAdapter
import com.example.shopperbeta.models.ListElement
import com.example.shopperbeta.models.UserList
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.activity_elements.*
import kotlinx.android.synthetic.main.activity_home.*

class ElementsActivity : AppCompatActivity() {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var elementAdapter : ElementAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_elements)

        val bundle: Bundle? = intent.extras
        var listid = bundle?.getString("listid").toString()
        var listName = bundle?.getString("listName").toString()

        lblElementListName.text = listName

        val collectionReference: CollectionReference = db.collection("listElements").document(listid).collection("elements")
        buttonElementShare.setOnClickListener{
            // Configure Builder and show alertDialog
            var builder = AlertDialog.Builder(this)

            builder.setTitle("Share List")

            var editEmail = EditText(this)
            editEmail.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            editEmail.hint = "User email"
            builder.setView(editEmail)

            // Builder buttons config
            builder.setPositiveButton("Share", DialogInterface.OnClickListener{dialog, which ->
                var editListEmail = editEmail.text.toString()
                shareList(editListEmail)

            })
            builder.setNegativeButton("Cancel", DialogInterface.OnClickListener{dialog, which -> dialog.cancel() })

            // Show builder
            var alertDialog = builder.create()
            alertDialog.show()
        }



        configureRecyclerView()

        buttonElementAdd.setOnClickListener {
            // Configure Builder and show alertDialog
            var builder = AlertDialog.Builder(this)
            builder.setTitle("New Element")
            var editName = EditText(this)
            editName.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            editName.hint = "Element name"
            builder.setView(editName)

            // Builder buttons config
            builder.setPositiveButton("Add", DialogInterface.OnClickListener{ dialog, which ->
                var elementName = editName.text.toString()
                val addElement = hashMapOf(
                    "elementName" to elementName,
                    "elementAuthor" to FirebaseAuth.getInstance().currentUser?.displayName.toString(),
                    "elementid" to collectionReference.document().id
                )
                collectionReference.document(addElement["elementid"].toString()).set(addElement)

            })
            builder.setNegativeButton("Cancel", DialogInterface.OnClickListener{ dialog, which -> dialog.cancel() })

            // Show builder
            var alertDialog = builder.create()
            alertDialog.show()
        }
    }

    private fun configureRecyclerView(){
        val bundle: Bundle? = intent.extras
        var listid = bundle?.getString("listid").toString()

        val collectionReference: CollectionReference = db.collection("listElements").document(listid).collection("elements")
        val query: Query = collectionReference
        val firestoreRecyclerOptions : FirestoreRecyclerOptions<ListElement> = FirestoreRecyclerOptions.Builder<ListElement>().setQuery(query,
            ListElement::class.java).build()

        elementAdapter = ElementAdapter(firestoreRecyclerOptions)

       recyclerElementView.layoutManager = LinearLayoutManager(this)
        recyclerElementView.isNestedScrollingEnabled = false
        recyclerElementView.adapter = elementAdapter

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT){
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                elementAdapter!!.deleteItem(viewHolder.bindingAdapterPosition)
            }

        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerElementView)
    }
    private fun shareList(userEmail: String){
        val bundle: Bundle? = intent.extras
        var listid = bundle?.getString("listid").toString()
        var listName = bundle?.getString("listName").toString()

        // Check if user exists
        if(userEmail.isNotEmpty()) {
            db.collection("users").document(userEmail).get().addOnCompleteListener {
                if (it.isSuccessful) {
                    val shareList = hashMapOf(
                        "listid" to listid,
                        "listName" to listName
                    )
                    // Share List
                    db.collection("userLists").document(userEmail).collection("lists")
                        .document(listid).set(shareList)
                    Toast.makeText(this, "Success Shared", Toast.LENGTH_SHORT).show()

                } else {
                    // user does not exist
                    Toast.makeText(this, "User is not registered in Shopper", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }else {
            Toast.makeText(this, "User Email is empty", Toast.LENGTH_SHORT)
                .show()

        }

    }


    override fun onStart() {
        super.onStart()
        elementAdapter?.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        elementAdapter?.stopListening()
    }
}