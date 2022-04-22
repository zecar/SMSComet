package com.ar.smscomet

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import com.beust.klaxon.Klaxon
import java.util.*
import khttp.responses.Response

class SMS(val message: String, val number: String, val messageId: String)

class SendTask constructor(_settings: SettingsManager, _context: Context) : TimerTask() {
    var settings = _settings
    var mainActivity: MainActivity = _context as MainActivity



    override fun run() {

        lateinit var apiResponse : Response
        try {
            apiResponse = khttp.post(
                url = settings.sendURL,
                data = mapOf(
                    "deviceId" to settings.deviceId,
                    "deviceSecret" to settings.deviceSecret,
                    "action" to "SEND"
                )
            )
            mainActivity.runOnUiThread(Runnable {
                mainActivity.msgShow("Caut mesaje in Web")
            })
        } catch (e: Exception) {
            Log.d("-->", "Cannot connect to URL")
            return
        }
        //var sms: SMS? = SMS("", "", "")
        var smsArray: List<SMS>? = emptyList()
        var canSend: Boolean = false
        try {
            smsArray = Klaxon().parseArray<SMS>(apiResponse.text)
            canSend = true
        } catch (e: com.beust.klaxon.KlaxonException) {
            if (apiResponse.text == "") {
                mainActivity.runOnUiThread(Runnable {
                    //mainActivity.logMain(".", false)
                    mainActivity.logMain("Nu exista mesaje de trimis pe Cabinet ID#" + settings.deviceId)
                })
                Log.d("-->", "Nothing")
            } else {
                mainActivity.runOnUiThread(Runnable {
                    mainActivity.logMain("Eroare la analizarea raspunsului de la server: " + apiResponse.text)
                })
                Log.d("error", "Eroare la analizarea SMS-urilor" + apiResponse.text)
            }
        } finally {
            // optional finally block
        }
        if (canSend) {
            smsArray?.forEach {
                val smsManager = SmsManager.getDefault() as SmsManager
                val sentIn = Intent(mainActivity.SENT_SMS_FLAG)
                settings.updateSettings()
                sentIn.putExtra("messageId", it!!.messageId)
                sentIn.putExtra("sendURL", settings.sendURL)
                sentIn.putExtra("deviceId", settings.deviceId)
                sentIn.putExtra("deviceSecret", settings.deviceSecret)
                sentIn.putExtra("delivered", 0)

                val deliverIn = Intent(mainActivity.DELIVER_SMS_FLAG)
                deliverIn.putExtra("messageId", it!!.messageId)
                deliverIn.putExtra("sendURL", settings.sendURL)
                deliverIn.putExtra("deviceId", settings.deviceId)
                deliverIn.putExtra("deviceSecret", settings.deviceSecret)
                deliverIn.putExtra("delivered", 1)

                if (it!!.message.length > 160) {
                    var parts = smsManager.divideMessage(it!!.message)
                    var sentPIns = ArrayList<PendingIntent>();
                    var deliverPIns = ArrayList<PendingIntent>();

                    parts.forEach {
                        sentPIns.add(PendingIntent.getBroadcast(mainActivity, mainActivity.nextRequestCode(), sentIn,0))
                        deliverPIns.add(PendingIntent.getBroadcast(mainActivity, mainActivity.nextRequestCode(), deliverIn, 0))
                    }

                    smsManager.sendMultipartTextMessage(it!!.number, null, parts, sentPIns, deliverPIns)

                    mainActivity.runOnUiThread(Runnable {
                        mainActivity.logMain("Parti trimise catre: " + it!!.number + " - id: " + it!!.messageId + " - mesaj: " + it!!.message)
                    })
                } else {
                    val sentIn = Intent(mainActivity.SENT_SMS_FLAG)
                    val sentPIn = PendingIntent.getBroadcast(mainActivity, mainActivity.nextRequestCode(), sentIn,0)
                    val deliverPIn = PendingIntent.getBroadcast(mainActivity, mainActivity.nextRequestCode(), deliverIn, 0)
                    smsManager.sendTextMessage(it!!.number, null, it!!.message, sentPIn, deliverPIn)

                    mainActivity.runOnUiThread(Runnable {
                        mainActivity.logMain("Trimis catre: " + it!!.number + " - id: " + it!!.messageId + " - mesaj: " + it!!.message)
                    })
                }


                Log.d("-->", "Sent!")

                Thread.sleep(500)
            }
        }


    }

}