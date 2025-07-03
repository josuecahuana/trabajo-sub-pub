package com.example.trabajoenclase2

import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPubSub
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RedisManager {
    private var subscriberJedis: Jedis? = null // para suscripciones
    private var publisherJedis: Jedis? = null // para publicación
    private var executorService: ExecutorService? = null
    private var pubSub: JedisPubSub? = null

    interface MessageListener {
        fun onMessage(channel: String, message: String)
    }

    init {
        executorService = Executors.newCachedThreadPool()
    }

    suspend fun connect(host: String, port: Int, username: String, password: String) {
        withContext(Dispatchers.IO) {
            try {
                // Crear dos conexiones independientes
                subscriberJedis = Jedis(host, port).apply {
                    auth(username, password)
                    ping()
                }
                publisherJedis = Jedis(host, port).apply {
                    auth(username, password)
                    ping()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }

    fun subscribe(channel: String, listener: MessageListener) {
        executorService?.execute {
            try {
                pubSub = object : JedisPubSub() {
                    override fun onMessage(channel: String, message: String) {
                        listener.onMessage(channel, message)
                    }
                }
                subscriberJedis?.subscribe(pubSub, channel)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun publish(channel: String, message: String) {
        executorService?.execute {
            try {
                publisherJedis?.publish(channel, message)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun disconnect() {
        pubSub?.unsubscribe()
        subscriberJedis?.close()
        publisherJedis?.close()
        executorService?.shutdown()
    }
}
