package com.example.audioposrecorder

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDateTime
import java.util.*

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, SensorEventListener {

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var locationManager : LocationManager
    private var txtLocation = ""
    private lateinit var acelerometro: Sensor
    private lateinit var gerenciadorSensor: SensorManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textToSpeech = TextToSpeech(this, this)
        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
        ActivityCompat.requestPermissions(this@MainActivity, permissions, 100)
        inicializarAcelerometro()
        showText()
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
            val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
            ActivityCompat.requestPermissions(this@MainActivity, permissions, 100)
        }
    }

    private fun playText(texto: String){
        textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun showText(){
        val database= FirebaseDatabase.getInstance()
        val linearL = findViewById<LinearLayout>(R.id.LinearL)
        val myRef = database.getReference("SpeechToText")
        myRef.get().addOnCompleteListener{task ->
            if(task.isSuccessful){
                val textos = task.result
                if (textos.exists()){
                    for(texto in textos.children){
                        val linearLayout = LinearLayout(this)
                        linearLayout.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        linearLayout.orientation = LinearLayout.VERTICAL

                        val txt = texto.child("texto").value as String
                        val txtTexto = TextView(this)
                        txtTexto.layoutParams = LinearLayout.LayoutParams (
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        txtTexto.text = txt
                        val btnOuvir = Button(this)
                        btnOuvir.layoutParams = LinearLayout.LayoutParams (
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        btnOuvir.text = "Reproduzir áudio"
                        btnOuvir.setOnClickListener {
                            playText(txt)
                        }
                        val btnDeletar = Button(this)
                        btnDeletar.layoutParams = LinearLayout.LayoutParams (
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        btnDeletar.text = "Deletar áudio"
                        btnDeletar.setOnClickListener {
                            linearL.removeView(btnDeletar.parent as ViewGroup)
                        }
                        linearL.addView(linearLayout)
                        linearLayout.addView(txtTexto)
                        linearLayout.addView(btnOuvir)
                        linearLayout.addView(btnDeletar)
                    }
                }
            }
        }
    }
    override fun onActivityResult(requestCode: Int,resultCode: Int, data: Intent?){
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val lblTextoReconhecido: TextView = findViewById(R.id.lblTexto)
            lblTextoReconhecido.text = result?.get(0) ?: ""
            val database = FirebaseDatabase.getInstance()
            if (result?.get(0) != "gravar posição"){
                val myRef = database.getReference("SpeechToText")
                val textoProBanco = mapOf(
                    "texto" to result?.get(0)
                )
                myRef.push().setValue(textoProBanco).addOnCompleteListener{
                    Toast.makeText(baseContext, "Texto gravado!", Toast.LENGTH_SHORT).show()
                }
            } else {
                val myRef = database.getReference("Location")
                val textoProBanco = mapOf(
                    "location" to txtLocation
                )
                myRef.push().setValue(textoProBanco).addOnCompleteListener {
                    Toast.makeText(baseContext, "Localização gravada!", Toast.LENGTH_SHORT).show()

                }
            }

        }else{
            Toast.makeText(this, "Teste", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissions(): Boolean{
        val recordPerm = ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)
        val finePerm = ActivityCompat.checkSelfPermission(applicationContext, ACCESS_FINE_LOCATION)
        val coarsePerm = ActivityCompat.checkSelfPermission(applicationContext, ACCESS_COARSE_LOCATION)
        return recordPerm == 0 && finePerm == 0 && coarsePerm == 0
    }

    override fun onInit(status: Int) {
        if(status == TextToSpeech.SUCCESS){
            val localeBR = Locale("pt", "BR")
            val result = textToSpeech.setLanguage(localeBR)
        }
    }
    public override fun onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(codigoIdentificacao: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(codigoIdentificacao, permissions, grantResults)
        if(codigoIdentificacao==10){
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.INTERNET
                    ) == PackageManager.PERMISSION_GRANTED){
                }
            }
            if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.checkSelfPermission(this,ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    locationManager = (getSystemService(LOCATION_SERVICE) as LocationManager?)!!
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, 0, 0f, locationListener
                    )
                }
            }
        }
    }
    private val locationListener: LocationListener = LocationListener { location ->
        val latitude = location.latitude
        val longitude = location.longitude
        txtLocation = ("$latitude:$longitude")
    }
    private fun inicializarAcelerometro(){
        gerenciadorSensor = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        acelerometro = gerenciadorSensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gerenciadorSensor.registerListener(this, this.acelerometro, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            if(event.values[1] != 0f){
                playText("Socorro, estou caindo")
                val database = FirebaseDatabase.getInstance()
                val myRef = database.getReference("location")
                val textoProBanco = mapOf(
                    "queda" to LocalDateTime.now()
                )
                myRef.push().setValue(textoProBanco)
            }
        }
    }
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }
}
