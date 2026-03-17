package com.mahavtaar.vibecoder.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mahavtaar.vibecoder.agent.AgentCommand
import com.mahavtaar.vibecoder.agent.AgentCommandBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AgentNotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var commandBus: AgentCommandBus

    @Inject
    lateinit var notificationManager: NotificationManager

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        GlobalScope.launch {
            when (intent.action) {
                NotificationManager.ACTION_AGENT_STOP -> {
                    commandBus.sendCommand(AgentCommand.Stop)
                    notificationManager.cancelNotification(1001) // Ongoing running
                    notificationManager.cancelNotification(1004) // Ongoing confirmation
                }
                NotificationManager.ACTION_AGENT_APPROVE -> {
                    commandBus.sendCommand(AgentCommand.Confirm(true))
                    notificationManager.cancelNotification(1004)
                }
                NotificationManager.ACTION_AGENT_REJECT -> {
                    commandBus.sendCommand(AgentCommand.Confirm(false))
                    notificationManager.cancelNotification(1004)
                }
            }
        }
    }
}
