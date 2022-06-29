package zechs.drive.stream.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import zechs.drive.stream.BuildConfig
import zechs.drive.stream.data.remote.DriveApi
import zechs.drive.stream.data.remote.TokenApi
import zechs.drive.stream.data.repository.DriveRepository
import zechs.drive.stream.data.repository.TokenAuthenticator
import zechs.drive.stream.utils.SessionManager
import zechs.drive.stream.utils.util.Constants.Companion.GOOGLE_ACCOUNTS_URL
import zechs.drive.stream.utils.util.Constants.Companion.GOOGLE_API
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BODY)
    }

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        driveRepository: Lazy<DriveRepository>
    ): TokenAuthenticator {
        return TokenAuthenticator(driveRepository)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        logging: Lazy<HttpLoggingInterceptor>,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .also {
                if (BuildConfig.DEBUG) {
                    // Logging only in debug builds
                    it.addInterceptor(logging.get())
                }
                it.authenticator(tokenAuthenticator)
            }.build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideDriveApi(client: OkHttpClient, moshi: Moshi): DriveApi {
        return Retrofit.Builder()
            .baseUrl(GOOGLE_API)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(DriveApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTokenApi(client: OkHttpClient, moshi: Moshi): TokenApi {
        return Retrofit.Builder()
            .baseUrl(GOOGLE_ACCOUNTS_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TokenApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDriveRepository(
        driveApi: DriveApi,
        tokenApi: Lazy<TokenApi>,
        sessionManager: SessionManager
    ): DriveRepository {
        return DriveRepository(driveApi, tokenApi, sessionManager)
    }


}