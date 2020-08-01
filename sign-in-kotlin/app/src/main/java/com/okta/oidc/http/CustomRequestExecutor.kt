package com.okta.oidc.http

import android.util.Log
import com.okta.authn.sdk.client.AuthenticationClient
import com.okta.authn.sdk.client.AuthenticationClientBuilder
import com.okta.authn.sdk.impl.client.DefaultAuthenticationClient
import com.okta.authn.sdk.impl.client.DefaultAuthenticationClientBuilder
import com.okta.commons.configcheck.ConfigurationValidator
import com.okta.commons.lang.Assert
import com.okta.commons.lang.Strings
import com.okta.sdk.client.AuthenticationScheme
import com.okta.sdk.client.Proxy
import com.okta.sdk.impl.api.ClientCredentialsResolver
import com.okta.sdk.impl.config.ClientConfiguration
import com.okta.sdk.impl.config.EnvironmentVariablesPropertiesSource
import com.okta.sdk.impl.config.OptionalPropertiesSource
import com.okta.sdk.impl.config.PropertiesSource
import com.okta.sdk.impl.config.ResourcePropertiesSource
import com.okta.sdk.impl.config.SystemPropertiesSource
import com.okta.sdk.impl.config.YAMLPropertiesSource
import com.okta.sdk.impl.http.Request
import com.okta.sdk.impl.http.RequestExecutor
import com.okta.sdk.impl.http.RequestExecutorFactory
import com.okta.sdk.impl.http.Response
import com.okta.sdk.impl.http.RetryRequestExecutor
import com.okta.sdk.impl.http.okhttp.OkHttpRequestExecutor
import com.okta.sdk.impl.io.ClasspathResource
import com.okta.sdk.impl.io.DefaultResourceFactory
import com.okta.sdk.impl.io.ResourceFactory
import com.okta.sdk.impl.util.BaseUrlResolver
import com.okta.sdk.impl.util.DefaultBaseUrlResolver
import java.io.File

class CustomRequestExecutor(private val requestExecutor: RequestExecutor) :
    RequestExecutor by requestExecutor {

    override fun executeRequest(request: Request?): Response {
        val tag = "CustomRequestExecutor"
        request?.run {
            Log.d(tag, "executeRequest ${this.resourceUrl}")
            headers.forEach {
                Log.d(tag, "header key-value: ${it.key} ${it.value}")
            }
        }
        return requestExecutor.executeRequest(request)
    }
}

class CustomRequestExecutorFactory : RequestExecutorFactory {
    override fun create(clientConfiguration: ClientConfiguration) = RetryRequestExecutor(
        clientConfiguration,
        CustomRequestExecutor(OkHttpRequestExecutor(clientConfiguration))
    )
}

class CustomAuthenticationClient(clientConfiguration: ClientConfiguration) :
    DefaultAuthenticationClient(clientConfiguration) {

    override fun createRequestExecutor(clientConfiguration: ClientConfiguration) =
        CustomRequestExecutorFactory().create(clientConfiguration)
}

class AuthenticationClientBuilder internal constructor(resourceFactory: ResourceFactory = DefaultResourceFactory()) :
    DefaultAuthenticationClientBuilder() {
    private val clientConfig = ClientConfiguration()
    private var allowNonHttpsForTesting = false

    override fun setProxy(proxy: Proxy) = apply {
        with(clientConfig) {
            proxyHost = proxy.host
            proxyPort = proxy.port
            proxyUsername = proxy.username
            proxyPassword = proxy.password
        }
    }

    override fun setConnectionTimeout(timeout: Int) = apply {
        Assert.isTrue(timeout >= 0, "Timeout cannot be a negative number.")
        clientConfig.connectionTimeout = timeout
    }

    override fun setBaseUrlResolver(baseUrlResolver: BaseUrlResolver) = apply {
        Assert.notNull(baseUrlResolver, "baseUrlResolver must not be null")
        clientConfig.baseUrlResolver = baseUrlResolver
    }

    override fun setRetryMaxElapsed(maxElapsed: Int) =
        apply { clientConfig.retryMaxElapsed = maxElapsed }

    override fun setRetryMaxAttempts(maxAttempts: Int) =
        apply { clientConfig.retryMaxAttempts = maxAttempts }

    override fun setOrgUrl(baseUrl: String) = apply {
        ConfigurationValidator.assertOrgUrl(baseUrl)
        clientConfig.baseUrl = baseUrl
    }

    override fun build(): AuthenticationClient {
        if (clientConfig.baseUrlResolver == null) {
            Assert.notNull(
                clientConfig.baseUrl,
                "Okta org url must not be null."
            )
            clientConfig.baseUrlResolver = DefaultBaseUrlResolver(clientConfig.baseUrl)
        }
        clientConfig.clientCredentialsResolver = ClientCredentialsResolver { null }
        return CustomAuthenticationClient(clientConfig)
    }

    companion object {
        private const val ENVVARS_TOKEN = "envvars"
        private const val SYSPROPS_TOKEN = "sysprops"
        private const val OKTA_CONFIG_CP = "com/okta/authn/sdk/config/"
        private const val OKTA_YAML = "okta.yaml"
        private const val OKTA_PROPERTIES = "okta.properties"
        private val USER_HOME =
            System.getProperty("user.home") + File.separatorChar
        private val DEFAULT_OKTA_PROPERTIES_FILE_LOCATIONS =
            arrayOf(
                ClasspathResource.SCHEME_PREFIX + OKTA_CONFIG_CP + OKTA_PROPERTIES,
                ClasspathResource.SCHEME_PREFIX + OKTA_CONFIG_CP + OKTA_YAML,
                ClasspathResource.SCHEME_PREFIX + OKTA_PROPERTIES,
                ClasspathResource.SCHEME_PREFIX + OKTA_YAML,
                USER_HOME + ".okta" + File.separatorChar + OKTA_YAML,
                ENVVARS_TOKEN,
                SYSPROPS_TOKEN
            )
    }

    init {
        val sources: MutableCollection<PropertiesSource> = ArrayList()
        DEFAULT_OKTA_PROPERTIES_FILE_LOCATIONS.forEach { location ->
            when {
                ENVVARS_TOKEN.equals(location, ignoreCase = true) -> {
                    sources.add(EnvironmentVariablesPropertiesSource.oktaFilteredPropertiesSource())
                }
                SYSPROPS_TOKEN.equals(location, ignoreCase = true) -> {
                    sources.add(SystemPropertiesSource.oktaFilteredPropertiesSource())
                }
                else -> {
                    val wrappedSource = if (Strings.endsWithIgnoreCase(location, ".yaml")) {
                        YAMLPropertiesSource(resourceFactory.createResource(location))
                    } else {
                        ResourcePropertiesSource(resourceFactory.createResource(location))
                    }
                    sources.add(OptionalPropertiesSource(wrappedSource))
                }
            }
        }
        val props: MutableMap<String, String> = LinkedHashMap()
        sources.forEach { props.putAll(it.properties) }

        props[AuthenticationClientBuilder.DEFAULT_CLIENT_TESTING_DISABLE_HTTPS_CHECK_PROPERTY_NAME]?.takeIf { it.isNotEmpty() }
            ?.run { allowNonHttpsForTesting = toBoolean() }
        props[AuthenticationClientBuilder.DEFAULT_CLIENT_ORG_URL_PROPERTY_NAME]?.takeIf { it.isNotEmpty() }
            ?.run {
                // remove backslashes that can end up in file when it's written programmatically, e.g. in a test
                clientConfig.baseUrl = replace("\\:", ":")
                ConfigurationValidator.assertOrgUrl(clientConfig.baseUrl)
            }
        props[AuthenticationClientBuilder.DEFAULT_CLIENT_CONNECTION_TIMEOUT_PROPERTY_NAME]?.takeIf { it.isNotEmpty() }
            ?.run { clientConfig.connectionTimeout = toInt() }
        props[AuthenticationClientBuilder.DEFAULT_CLIENT_AUTHENTICATION_SCHEME_PROPERTY_NAME]?.takeIf { it.isNotEmpty() }
            ?.run { clientConfig.authenticationScheme = AuthenticationScheme.valueOf(this) }
        props[AuthenticationClientBuilder.DEFAULT_CLIENT_PROXY_PORT_PROPERTY_NAME]?.takeIf { it.isNotEmpty() }
            ?.run { clientConfig.proxyPort = toInt() }
        props[AuthenticationClientBuilder.DEFAULT_CLIENT_PROXY_HOST_PROPERTY_NAME]?.takeIf { it.isNotEmpty() }
            ?.run { clientConfig.proxyHost = this }
        props[AuthenticationClientBuilder.DEFAULT_CLIENT_PROXY_USERNAME_PROPERTY_NAME]?.takeIf { it.isNotEmpty() }
            ?.run { clientConfig.proxyUsername = this }
        props[AuthenticationClientBuilder.DEFAULT_CLIENT_PROXY_PASSWORD_PROPERTY_NAME]?.takeIf { it.isNotEmpty() }
            ?.run { clientConfig.proxyPassword = this }
        props[AuthenticationClientBuilder.DEFAULT_CLIENT_REQUEST_TIMEOUT_PROPERTY_NAME]?.takeIf { it.isNotEmpty() }
            ?.run { clientConfig.retryMaxElapsed = toInt() }
        props[AuthenticationClientBuilder.DEFAULT_CLIENT_RETRY_MAX_ATTEMPTS_PROPERTY_NAME]?.takeIf { it.isNotEmpty() }
            ?.run { clientConfig.retryMaxAttempts = toInt() }
    }
}
