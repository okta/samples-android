/*
 * Copyright 2022-Present Okta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okta.android.samples.browser_sign_in.test

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.okta.sdk.authc.credentials.ClientCredentials
import com.okta.sdk.cache.Caches
import com.okta.sdk.error.ErrorHandler
import com.okta.sdk.impl.api.DefaultClientCredentialsResolver
import com.okta.sdk.impl.client.DefaultClientBuilder
import com.okta.sdk.impl.deserializer.UserProfileDeserializer
import com.okta.sdk.impl.serializer.UserProfileSerializer
import com.okta.sdk.impl.util.DefaultBaseUrlResolver
import org.openapitools.client.ApiClient
import org.openapitools.client.model.UserProfile
import org.openapitools.jackson.nullable.JsonNullableModule
import org.springframework.http.MediaType
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.DefaultUriBuilderFactory
import java.util.Arrays
import java.util.concurrent.TimeUnit
import kotlin.String

internal class TestClientBuilder(
    private val clientId: String,
    private val orgUrl: String,
    private val clientCredentials: ClientCredentials<*>
) {
    private val clientConfiguration = DefaultClientBuilder().clientConfiguration.apply {
        baseUrl = orgUrl
        clientId = this@TestClientBuilder.clientId
    }

    fun build(): ApiClient {
        val cacheManager = Caches.newCacheManager()
            .withDefaultTimeToIdle(clientConfiguration.cacheManagerTti, TimeUnit.SECONDS)
            .withDefaultTimeToLive(clientConfiguration.cacheManagerTtl, TimeUnit.SECONDS)
            .build()

        clientConfiguration.baseUrlResolver = DefaultBaseUrlResolver(orgUrl)
        val apiClient = ApiClient(
            restTemplate(),
            cacheManager,
            clientConfiguration
        )
        apiClient.basePath = clientConfiguration.baseUrl
        clientConfiguration.clientCredentialsResolver = DefaultClientCredentialsResolver(clientCredentials)
        apiClient.setApiKeyPrefix("SSWS")
        apiClient.setApiKey(clientCredentials.credentials as String)
        return apiClient
    }

    private fun restTemplate(): RestTemplate {
        val objectMapper = ObjectMapper()
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
        objectMapper.registerModule(JavaTimeModule())
        objectMapper.registerModule(JsonNullableModule())
        val module = SimpleModule()
        module.addSerializer(UserProfile::class.java, UserProfileSerializer())
        module.addDeserializer(UserProfile::class.java, UserProfileDeserializer())
        objectMapper.registerModule(module)
        val mappingJackson2HttpMessageConverter = MappingJackson2HttpMessageConverter(objectMapper)
        mappingJackson2HttpMessageConverter.supportedMediaTypes = Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.parseMediaType("application/x-pem-file"),
            MediaType.parseMediaType("application/x-x509-ca-cert"),
            MediaType.parseMediaType("application/pkix-cert")
        )
        val messageConverters: MutableList<HttpMessageConverter<*>> = ArrayList()
        messageConverters.add(mappingJackson2HttpMessageConverter)
        val restTemplate = RestTemplate(messageConverters)
        restTemplate.errorHandler = ErrorHandler()
        restTemplate.requestFactory = OkHttp3ClientHttpRequestFactory()
        val uriTemplateHandler = DefaultUriBuilderFactory()
        uriTemplateHandler.encodingMode = DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY
        restTemplate.uriTemplateHandler = uriTemplateHandler
        return restTemplate
    }
}
