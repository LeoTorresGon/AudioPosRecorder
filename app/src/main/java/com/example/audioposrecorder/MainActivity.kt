package com.example.audioposrecorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore.Audio.Media
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.PackageManagerCompat.LOG_TAG
import java.io.IOException
import java.util.*

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    fun startRecording(view: View) {
        if(checkPermissions()){
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            if (intent.resolveActivity(packageManager) != null){
                startActivityForResult(intent, 202)
            } else{
                Toast.makeText(this,"Não suportado", Toast.LENGTH_SHORT).show()
            }

        }else{
            val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
            ActivityCompat.requestPermissions(this@MainActivity, permissions, 100)
        }
    }

    override fun onActivityResult(requestCode: Int,resultCode: Int, data: Intent?){
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val lblTextoReconhecido: TextView = findViewById(R.id.lblTexto)
            lblTextoReconhecido.text = result?.get(0) ?: ""
        }else{
            Toast.makeText(this, "Teste", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissions(): Boolean{
        val recordPerm = ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)
        return recordPerm == 0
    }

}