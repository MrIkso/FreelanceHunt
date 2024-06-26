package tech.hombre.data.di

import android.content.Context
import android.os.Build
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tech.hombre.data.BuildConfig
import tech.hombre.data.common.extensions.getCurrentDefaultLanguage
import tech.hombre.data.networking.*
import java.util.concurrent.TimeUnit


val networkingModule = module {
    single { GsonConverterFactory.create(GsonBuilder().setLenient().create()) as Converter.Factory }
    single {
        OkHttpClient.Builder().apply {
            if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY).let(::addInterceptor)
            }
            addInterceptor {
                val prefs = androidContext().getSharedPreferences(
                    "preferences",
                    Context.MODE_PRIVATE
                )
                val apiToken = prefs.getString("KEY_API_TOKEN", "")
                it.proceed(
                    it.request().newBuilder().addHeader(
                        "Authorization",
                        if (!apiToken.isNullOrEmpty()) "Bearer $apiToken" else ""
                    )
                        .addHeader(
                            "Accept-Language",
                            prefs.getString("KEY_APP_LANGUAGE", getCurrentDefaultLanguage())!!
                        )
                        .addHeader(
                            "User-Agent",
                            "Freelancehunt for Android (v${BuildConfig.VERSION_NAME}, b${BuildConfig.VERSION_CODE}; Android ${Build.VERSION.RELEASE})"
                        )
                        .build()
                )
            }
            connectTimeout(30, TimeUnit.SECONDS)
            readTimeout(30, TimeUnit.SECONDS)
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
    single { get<Retrofit>().create(CitiesApi::class.java) }
    single { get<Retrofit>().create(ThreadsApi::class.java) }
    single { get<Retrofit>().create(BidsApi::class.java) }
    single { get<Retrofit>().create(EmployersApi::class.java) }
    single { get<Retrofit>().create(WorkspacesApi::class.java) }
    single { get<Retrofit>().create(SkillsApi::class.java) }
}