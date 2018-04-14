package net.yupol.transmissionremote.transport.rpc

import com.squareup.moshi.Moshi
import okhttp3.RequestBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class RpcRequestBodyConverterFactory(private val moshi: Moshi) : Converter.Factory() {

    companion object {
        fun create(moshi: Moshi) = RpcRequestBodyConverterFactory(moshi)
    }

    override fun requestBodyConverter(
            type: Type?, parameterAnnotations: Array<out Annotation>?,
            methodAnnotations: Array<out Annotation>?, retrofit: Retrofit?): Converter<*, RequestBody>? {

        val rpcMethod = methodAnnotations.find<RpcMethod>()?.name ?: return null
        val rpcFields = methodAnnotations.find<RpcFields>()?.fields
        val booleanArgs = methodAnnotations.findAll<RpcBooleanArg>()
        val argName = parameterAnnotations.find<RpcArg>()?.name

        val adapter = moshi.adapter<RpcBody>(RpcBody::class.java)

        val staticArgs = mutableMapOf<String, Any>().apply {
            if (rpcFields != null) {
                put("fields", rpcFields)
            }
            booleanArgs.forEach {
                put(it.name, it.value)
            }
        }

        return when {
            argName != null -> RpcArgRequestBodyConverter(rpcMethod, staticArgs, argName, adapter)
            else -> RpcRequestBodyConverter(rpcMethod, staticArgs, adapter)
        }
    }
}

private inline fun <reified T> Array<out Annotation>?.find(): T? {
    return this?.find { it.annotationClass == T::class } as? T
}

private inline fun <reified T> Array<out Annotation>?.findAll(): List<T> {
    @Suppress(names = ["UNCHECKED_CAST"])
    return this?.filter { it.annotationClass == T::class } as List<T>
}
