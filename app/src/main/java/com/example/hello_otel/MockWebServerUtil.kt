package com.example.hello_otel

import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

object MockWebServerUtil {


    fun initServer(mockWebServer: MockWebServer) {
        Completable.fromAction {
            mockWebServer.start()

            val mockDispatcher: Dispatcher = object : Dispatcher() {
                @Throws(InterruptedException::class)
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val path = request.path
                    return when (path) {
                        "/rt/v1/auth" -> mockResponse(request.getHeader("x-bypass") == "1")
                        "/rt/v1/profile" -> MockResponse().setResponseCode(200).setBody("""
                            {
                              "info": {
                                "userId": "1234",
                                "name": "Lucas Albuquerque",
                                "age": "21",
                                "gender": "male"
                              }
                            }
                        """.trimIndent())

                        else -> MockResponse().setResponseCode(404)
                    }
                }

                private fun mockResponse(alwaysSuccess: Boolean): MockResponse {
                    return if (alwaysSuccess) MockResponse().setResponseCode(200)
                            .setBody("""
                                {
                                    "token":"1234"
                                }
                            """.trimIndent())
                    else MockResponse().setResponseCode(401)
                            .setBody("""
                                {
                                    "error":{
                                        "code":1
                                        "message":"Incorrect password"
                                    }
                                }
                            """.trimIndent())
                }
            }
            mockWebServer.dispatcher = mockDispatcher
        }.subscribeOn(Schedulers.computation()).subscribe()

    }

}