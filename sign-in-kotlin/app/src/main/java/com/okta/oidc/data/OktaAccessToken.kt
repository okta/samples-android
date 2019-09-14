package com.okta.oidc.data

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.lang.reflect.ParameterizedType

data class OktaAccessToken(
    val header: Header,
    val payload: Payload,
    val signature: String
) {

    @Suppress("UNCHECKED_CAST")
    companion object Parser {
        private const val NUMBER_OF_SECTIONS = 3
        fun parseAccessToken(tokenString: String): OktaAccessToken {
            val sections =
                tokenString.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            require(sections.size >= NUMBER_OF_SECTIONS) {
                "token missing header, payload or signature section"
            }
            val gson = GsonBuilder()
                .registerTypeAdapterFactory(object : TypeAdapterFactory {
                    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
                        if (type.rawType != List::class.java) {
                            return null
                        }
                        val elementType = (type.type as ParameterizedType).actualTypeArguments[0]
                        return ArrayTypeAdapter(
                            gson.getDelegateAdapter(this, type) as TypeAdapter<List<Any>>,
                            gson.getAdapter(TypeToken.get(elementType)) as TypeAdapter<Any>
                        ) as TypeAdapter<T>
                    }
                }).create()

            val headerSection = String(Base64.decode(sections[0], Base64.URL_SAFE))
            val payloadSection = String(Base64.decode(sections[1], Base64.URL_SAFE))
            val signature = String(Base64.decode(sections[2], Base64.URL_SAFE))
            return OktaAccessToken(
                gson.fromJson(headerSection, Header::class.java),
                gson.fromJson(payloadSection, Payload::class.java),
                signature
            )
        }
    }
}

data class Header(
    var alg: String = "",
    var kid: String = ""
)

data class Payload(
    var aud: List<String>? = null,
    var cid: String = "",
    var exp: Int = 0,
    var iat: Int = 0,
    var iss: String = "",
    var jti: String = "",
    var scp: List<String>? = null,
    var sub: String = "",
    var uid: String = "",
    var ver: Int = 0,
    var custom_claim: String = ""
)

class ArrayTypeAdapter(delegateAdapter: TypeAdapter<List<Any>>, elementAdapter: TypeAdapter<Any>) :
    TypeAdapter<List<Any>>() {

    private val delegate: TypeAdapter<List<Any>> = delegateAdapter
    private val element: TypeAdapter<Any> = elementAdapter

    override fun write(writer: JsonWriter, value: List<Any>) {
        if (value.size == 1) {
            element.write(writer, value[0])
        } else {
            delegate.write(writer, value)
        }
    }

    override fun read(reader: JsonReader): List<Any> {
        return if (reader.peek() != JsonToken.BEGIN_ARRAY) {
            listOf(element.read(reader))
        } else delegate.read(reader)
    }
}
