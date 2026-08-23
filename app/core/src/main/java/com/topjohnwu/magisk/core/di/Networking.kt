package com.topjohnwu.magisk.core.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.topjohnwu.magisk.ProviderInstaller
import com.topjohnwu.magisk.core.BuildConfig
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.model.DateTimeAdapter
import com.topjohnwu.magisk.core.utils.LocaleSetting
import okhttp3.Cache
import okhttp3.ConnectionSpec
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File
import java.net.InetAddress
import java.net.UnknownHostException

private class DnsResolver(private val client: OkHttpClient) : Dns {

    private fun buildDoh(url: String, hosts: List<String>): DnsOverHttps {
        return try {
            DnsOverHttps.Builder().client(client)
                .url(url.toHttpUrl())
                .bootstrapDnsHosts(hosts.map { InetAddress.getByName(it) })
                .resolvePrivateAddresses(true)  /* To make PublicSuffixDatabase never used */
                .build()
        } catch (e: Exception) {
            DnsOverHttps.Builder().client(client)
                .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
                .bootstrapDnsHosts(listOf(
                    InetAddress.getByName("1.1.1.1"),
                    InetAddress.getByName("8.8.8.8")
                ))
                .resolvePrivateAddresses(true)
                .build()
        }
    }

    private var lastProvider = Config.dnsProvider
    private var dohCache: DnsOverHttps? = null

    private fun currentDoh(): DnsOverHttps {
        val provider = Config.dnsProvider
        if (dohCache == null || provider != lastProvider) {
            lastProvider = provider
            dohCache = when (provider) {
                Config.Value.DNS_PROVIDER_GOOGLE -> buildDoh(
                    "https://dns.google/dns-query",
                    listOf("8.8.8.8", "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844")
                )
                Config.Value.DNS_PROVIDER_ADGUARD -> buildDoh(
                    "https://dns.adguard-dns.com/dns-query",
                    listOf("94.140.14.14", "94.140.15.15", "2a10:50c0::ad1:ff", "2a10:50c0::ad2:ff")
                )
                Config.Value.DNS_PROVIDER_CUSTOM -> buildDoh(
                    Config.dnsCustomUrl.ifEmpty { "https://cloudflare-dns.com/dns-query" },
                    listOf("1.1.1.1", "8.8.8.8")
                )
                else -> buildDoh(
                    "https://cloudflare-dns.com/dns-query",
                    listOf(
                        "162.159.36.1", "162.159.46.1", "1.1.1.1", "1.0.0.1",
                        "2606:4700:4700::1111", "2606:4700:4700::1001",
                        "2606:4700:4700::0064", "2606:4700:4700::6400"
                    )
                )
            }
        }
        return dohCache!!
    }

    override fun lookup(hostname: String): List<InetAddress> {
        if (Config.doh) {
            try {
                return currentDoh().lookup(hostname)
            } catch (e: UnknownHostException) {}
        }
        return Dns.SYSTEM.lookup(hostname)
    }
}


fun createOkHttpClient(context: Context): OkHttpClient {
    val appCache = Cache(File(context.cacheDir, "okhttp"), 10 * 1024 * 1024)
    val builder = OkHttpClient.Builder().cache(appCache)

    if (BuildConfig.DEBUG) {
        builder.addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
    } else {
        builder.connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
    }

    builder.dns(DnsResolver(builder.build()))

    builder.addInterceptor { chain ->
        val request = chain.request().newBuilder()
        request.header("User-Agent", "Magisk/${BuildConfig.APP_VERSION_CODE}")
        request.header("Accept-Language", LocaleSetting.instance.currentLocale.toLanguageTag())
        chain.proceed(request.build())
    }

    ProviderInstaller.install(context)

    return builder.build()
}

fun createMoshiConverterFactory(): MoshiConverterFactory {
    val moshi = Moshi.Builder().add(DateTimeAdapter()).build()
    return MoshiConverterFactory.create(moshi)
}

fun createRetrofit(okHttpClient: OkHttpClient): Retrofit.Builder {
    return Retrofit.Builder()
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(createMoshiConverterFactory())
        .client(okHttpClient)
}

inline fun <reified T> createApiService(retrofitBuilder: Retrofit.Builder, baseUrl: String): T {
    return retrofitBuilder
        .baseUrl(baseUrl)
        .build()
        .create(T::class.java)
}
