package com.example.trabajoenclase2


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val redisManager = RedisManager()
    private val currentChannel = "general"

    // Generar usuario único
    val currentUser = "Usuario${System.currentTimeMillis() % 1000}"

    init {
        connectToRedis()
    }

    private fun connectToRedis() {
        viewModelScope.launch {
            _connectionStatus.value = ConnectionStatus.Connecting

            try {
                val host = "redis-19626.crce207.sa-east-1-2.ec2.redns.redis-cloud.com"
                val port = 19626
                val username = "default"
                val password = "Q1EUJbqe8mX0r7L9vDfpx8WbzpmawgbP"

                redisManager.connect(host, port, username, password)

                // Suscribirse al canal
                redisManager.subscribe(currentChannel, object : RedisManager.MessageListener {
                    override fun onMessage(channel: String, message: String) {
                        try {
                            val msg = Message.fromJson(message)
                            _messages.value = _messages.value + msg
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                })

                _connectionStatus.value = ConnectionStatus.Connected
            } catch (e: Exception) {
                _connectionStatus.value = ConnectionStatus.Disconnected
                e.printStackTrace()
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            val message = Message(
                username = currentUser,
                content = content,
                timestamp = System.currentTimeMillis()
            )

            redisManager.publish(currentChannel, message.toJson())
        }
    }

    override fun onCleared() {
        super.onCleared()
        redisManager.disconnect()
    }
}