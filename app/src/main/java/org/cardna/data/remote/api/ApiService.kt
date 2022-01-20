package org.cardna.data.remote.api

import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

object ApiService {
    private val BASE_URL = "https://asia-northeast3-cardna-29f5b.cloudfunctions.net/api/"

    var gson = GsonBuilder().setLenient().create()

    private val Retrofit: Retrofit = retrofit2.Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(provideOkHttpClient(AppInterceptor()))
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val authService: AuthService = Retrofit.create(AuthService::class.java)

    val cardService: CardService = Retrofit.create(CardService::class.java)
    val friendService: FriendService = Retrofit.create(FriendService::class.java)
    val insightService: InsightService = Retrofit.create(InsightService::class.java)
    val likeService: LikeService = Retrofit.create(LikeService::class.java)
    val myPageService: MyPageService = Retrofit.create(MyPageService::class.java)
}

private fun provideOkHttpClient(
    interceptor: AppInterceptor
): OkHttpClient = OkHttpClient.Builder()
    .run {
        addInterceptor(interceptor)
        build()
    }

class AppInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain)
        : okhttp3.Response = with(chain) {
        val newRequest = request().newBuilder()
            .addHeader("token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6NCwiZW1haWwiOiJqdW5namFlQGdtYWlsLmNvbSIsIm5hbWUiOiLsoJXsnqwiLCJmaXJlYmFzZUlkIjoidnZJMXFERTVsS2F3NTEyYkl5bUNIekN2SEx1MiIsImlhdCI6MTY0MjQ4NDQyNCwiZXhwIjoxNjQ1MDc2NDI0LCJpc3MiOiJjYXJkbmEifQ.yNk3jLxi6t37oq1p8ShwEghCyGzlv9s-lT3Y7VN-SZg")
            .build()
        proceed(newRequest)
    }
}
