package com.example.prova4

import retrofit2.http.Body
import retrofit2.http.POST
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

interface InfluxDBService {
    @POST("write?db=mydatabase")
    fun writeData(@Body data: String): Call<Void>
}

class MainActivity : AppCompatActivity() {
    private lateinit var influxDBService: InfluxDBService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar Retrofit para InfluxDB
        val retrofit = Retrofit.Builder()
            .baseUrl("http://84.247.188.251:8086/") // Cambia esto por la URL de InfluxDB
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

        influxDBService = retrofit.create(InfluxDBService::class.java)

        // Botón 1: Enviar datos de ejemplo a InfluxDB
        findViewById<Button>(R.id.button1).setOnClickListener {
            val data = "weather,location=us-midwest temperature=82"
            sendDataToInfluxDB(data)
        }

        // Botón 2: Enviar otro conjunto de datos a InfluxDB
        findViewById<Button>(R.id.button2).setOnClickListener {
            val data = "weather,location=us-west temperature=75"
            sendDataToInfluxDB(data)
        }

        // Botón 3: Llamar a FirstActivity
        findViewById<Button>(R.id.button3).setOnClickListener {
            startActivity(Intent(this, FirstActivity::class.java))
        }
    }

    private fun sendDataToInfluxDB(data: String) {
        influxDBService.writeData(data).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Data sent successfully to InfluxDB", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Failed to send data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
