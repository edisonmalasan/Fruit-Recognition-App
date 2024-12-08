package com.example.fruitrecognitionapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlin.random.Random

class NotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "notification_channel"
        const val CHANNEL_NAME = "Random Notifications"
    }

    private val notificationManager =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun doWork(): Result {
        createNotificationChannel()

        // Choose a random topic
        val topic = listOf("Greeting", "Reminder", "Trivia").random()

        // Select a random notification based on the topic
        val notificationText = when (topic) {
            "Greeting" -> listOf(
                "Good morning! Have a great day ahead!",
                "Hello there! Keep smiling!",
                "Good evening! Hope you had a productive day!"
            ).random()

            "Reminder" -> listOf(
                "Don't forget to stay hydrated!",
                "Time to take a short break and stretch!",
                "Remember to finish your tasks for today!"
            ).random()

            "Trivia" -> listOf(
                "Did you know? Bananas are berries, but strawberries are not!",
                "An apple a day keeps the doctor away, but did you know it also floats on water?",
                "Tomatoes are fruits, but legally classified as vegetables in the US!"
            ).random()

            else -> "Hello! This is your friendly notification."
        }

        sendNotification(topic, notificationText)
        return Result.success()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(title: String, content: String) {
        val notificationId = Random.nextInt()
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
