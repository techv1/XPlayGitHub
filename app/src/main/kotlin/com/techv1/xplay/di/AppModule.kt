package com.techv1.xplay.di

import android.content.Context
import androidx.room.Room
import com.techv1.xplay.data.local.XPlayDatabase
import com.techv1.xplay.data.local.dao.VideoDao
import com.techv1.xplay.data.remote.XPlayApiService
import com.techv1.xplay.data.repository.VideoRepositoryImpl
import com.techv1.xplay.domain.repository.VideoRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val BASE_URL = "https://streamtape-backend.vishnusharma72925.workers.dev/v1/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): XPlayApiService =
        retrofit.create(XPlayApiService::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): XPlayDatabase =
        Room.databaseBuilder(
            context,
            XPlayDatabase::class.java,
            XPlayDatabase.DATABASE_NAME
        ).build()

    @Provides
    @Singleton
    fun provideVideoDao(db: XPlayDatabase): VideoDao = db.videoDao

    @Provides
    @Singleton
    fun provideVideoRepository(api: XPlayApiService, dao: VideoDao): VideoRepository =
        VideoRepositoryImpl(api, dao)
}
