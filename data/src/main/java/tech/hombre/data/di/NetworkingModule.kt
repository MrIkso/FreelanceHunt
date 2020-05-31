package tech.hombre.data.di

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tech.hombre.data.BuildConfig
import tech.hombre.data.networking.*
import java.util.concurrent.TimeUnit

var API_TOKEN = ""

val networkingModule = module {
    single { GsonConverterFactory.create() as Converter.Factory }
    single { HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY) as Interceptor }
    single {
        OkHttpClient.Builder().apply {
            if (BuildConfig.DEBUG) addInterceptor(get())
            addInterceptor {
                it.proceed(
                    it.request().newBuilder().addHeader(
                        "Authorization",
                        "Bearer $API_TOKEN"
                    ).build()
                )
            }
                .callTimeout(10, TimeUnit.SECONDS)
        }.build()
    }
    single {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(get())
            .addConverterFactory(get())
            .build()
    }
    single { get<Retrofit>().create(MyProfileApi::class.java) }
    single { get<Retrofit>().create(FeedApi::class.java) }
    single { get<Retrofit>().create(ProjectsApi::class.java) }
    single { get<Retrofit>().create(ContestsApi::class.java) }
    single { get<Retrofit>().create(FreelancersApi::class.java) }
    single { get<Retrofit>().create(CountriesApi::class.java) }
    single { get<Retrofit>().create(ThreadsApi::class.java) }
    single { get<Retrofit>().create(BidsApi::class.java) }
    single { get<Retrofit>().create(EmployersApi::class.java) }
}