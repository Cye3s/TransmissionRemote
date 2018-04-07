package transport.rpc

import com.squareup.moshi.JsonAdapter
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Converter

class RpcRequestBodyConverter(private val method: String, private val adapter: JsonAdapter<RpcBody>) : Converter<Map<String, Any>, RequestBody> {

    companion object {
        private val MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8")
    }

    override fun convert(args: Map<String, Any>): RequestBody {
        return RequestBody.create(MEDIA_TYPE, adapter.toJson(RpcBody(method, args)))
    }
}

