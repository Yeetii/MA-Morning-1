package com.example.cheat

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.cheat.model.Message
import java.text.DateFormat
import java.util.*
import kotlin.random.Random
import kotlin.system.exitProcess


class ChatActivity : AppCompatActivity() {

    private val viewModel: MessageViewModel by viewModels()

    private lateinit var bt : BluetoothConnectivity
    private var btEnabled : Boolean = false

    var debug = true;   // just for debugging

    lateinit var text_entry: EditText;

    lateinit var button_send: ImageButton;

    lateinit var layout: LinearLayout;

    lateinit var history: ScrollView;

    var nextUid: Int = 0;

    lateinit var cheatingPartner: String;

    fun requestCamera(view: View) {
        if(debug) println("requestCamera");

        val i = Intent(getApplicationContext(), CameraActivity::class.java)
        startActivity(i)
    }

    fun openImages(view: View) {
        if(debug) println("openImages");
        val intent = Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
        // save image to local variable
        // encode it.
        // send to BT for transmission
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun sendMessage(view: View) {
//         Do something in response to button click
        if(!text_entry.text.isBlank()){
            var id = Random.nextInt()
            if (btEnabled){
                bt.writeMessage(text_entry.text.toString() + "\\0", id)
                if(text_entry.text.toString().toLowerCase() == "/disconnect") {
                    Toast.makeText(this, "Disconnected from " + cheatingPartner, Toast.LENGTH_LONG).show()
                    //Why postDelayed? because otherwise we will never see the toast message above ...
                    // Restarts the whole application - HOW CONVINIENT!!!
                    Handler().postDelayed({exitProcess(0)}, 2000)
                }
                else if(text_entry.text.toString().toLowerCase().startsWith("/edit")) {
                    EditMessageTask(viewModel).execute(text_entry.text.toString())
                    text_entry.text = null;
                }
                else if(text_entry.text.toString().toLowerCase().startsWith("/delete")) {
                    DeleteMessageTask(viewModel).execute(text_entry.text.toString())
                    text_entry.text = null;
                }
                else {
                    var message = Message(id, text_entry.text.toString(), Date(), true)
                    viewModel.insertMessage(message)
                    text_entry.text = null;
                    nextUid++
                }
            }
        }
    }

    fun receiveMessage(messageString : String, id: Int) {
        // TODO: Check the Date functionality - maybe get that from the sender device?
        if(messageString.startsWith("/edit")) {
            EditMessageTask(viewModel).execute(messageString)
        }
        else if(messageString.startsWith("/delete")) {
            DeleteMessageTask(viewModel).execute(messageString)
        }
        else {
            var message = Message(id, messageString, Date(), false)
            viewModel.insertMessage(message)
            nextUid++
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // called when img captured from camera intent
        if (resultCode == Activity.RESULT_OK && data != null) {
            // set image captured to image view
            val imageUri: Uri? = data!!.data
            val bitmap =  MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri);

            val imageView = ImageView(this);

            imageView.setImageURI(imageUri)

            imageView.setOnClickListener() {v -> onImageClick(imageUri!!)};
            imageView.maxHeight = 400;
            imageView.minimumHeight = 400;

            layout?.addView(imageView);
            history.post { history.fullScroll(View.FOCUS_DOWN)}
        }
    }

    fun onImageClick(photoUri: Uri) {
        val imageView = findViewById<ImageView>(R.id.fullScreenView);
        imageView.visibility = View.VISIBLE;
        val backgroundView = findViewById<ImageView>(R.id.fullScreenBackground);
        backgroundView.visibility = View.VISIBLE;
        imageView.setImageURI(photoUri)
        imageView.setOnClickListener() {v -> imageView.visibility = View.GONE; backgroundView.visibility = View.GONE}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        if (BluetoothAdapter.getDefaultAdapter() != null) {
            btEnabled = true
            bt = BluetoothConnectivity.Companion.instance(this, BluetoothAdapter.getDefaultAdapter())
            bt.updateContext(this)
            bt.setChatActivity(this)
            cheatingPartner = intent.getStringExtra("cp")
        }

        viewModel.deleteAllMessage()

        if(debug) println("onCreate");
        val df: DateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
        val tf: DateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

        viewModel.getAllMessages().observe(this, Observer<List<Message>> {
            layout.removeAllViews()
            val sorted = it.sortedBy { it.date }
            for (message in sorted) {
                val textView = TextView(this);
                val textViewDate = TextView(this);
                textViewDate.text = df.format(message.date) + " " + tf.format(message.date);
                textView.id = message.uid
                textView.text = message.text;
                textView.setTextSize(25f);
                textViewDate.setTextSize(17f);
                if (message.belongsToCurrentUser){
                    textView.setTextColor(Color.WHITE);
                    textView.setBackgroundResource(R.drawable.text_view_sent);

                    textView.layoutParams= LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        gravity = Gravity.RIGHT
                        bottomMargin = 2;
                        topMargin = 10;
                        rightMargin = 15;
                    }
                    textViewDate.layoutParams= LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        gravity = Gravity.RIGHT
                        bottomMargin = 10;
                        topMargin = 0;
                        rightMargin = 5;
                    }
                } else {
                    textView.setTextColor(Color.BLACK);
                    textView.setBackgroundResource(R.drawable.text_view_received);

                    textView.layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.LEFT
                        bottomMargin = 2;
                        topMargin = 10;
                    }
                    textViewDate.layoutParams= LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        gravity = Gravity.LEFT
                        bottomMargin = 10;
                        topMargin = 0;
                    }
                }
                layout?.addView(textView);
                layout?.addView(textViewDate);
            }
            history.post { history.fullScroll(View.FOCUS_DOWN) }
        })

        history = findViewById(R.id.scrollView);
        layout = findViewById(R.id.history_layout);
        text_entry = findViewById(R.id.text_entry);
        button_send = findViewById(R.id.button_send);
    }

    private class EditMessageTask(viewModel: MessageViewModel) : AsyncTask<String?, Int?, Int>() {
        private var viewModel = viewModel

        override fun doInBackground(vararg params: String?): Int? {
            var splitted = params.first()?.split(";")
            var message = viewModel.getMessageByText(splitted!!.get(1))
            message.text = splitted[2] + " (edited)"
            viewModel.updateMessage(message)
            return 0
        }
    }

    private class DeleteMessageTask(viewModel: MessageViewModel) : AsyncTask<String?, Int?, Int>() {
        private var viewModel = viewModel

        override fun doInBackground(vararg params: String?): Int? {
            var splitted = params.first()?.split(";")
            var message = viewModel.getMessageByText(splitted!!.get(1))
            message.text = "(deleted)"
            viewModel.updateMessage(message)
            return 0
        }
    }
}
