package app

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException

class BundleTypeAdapterFactory : TypeAdapterFactory {
  override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
    return if (!Bundle::class.java.isAssignableFrom(type.rawType)) {
      null
    } else
        object : TypeAdapter<Bundle?>() {
          @Throws(IOException::class)
          override fun write(out: JsonWriter, bundle: Bundle?) {
            if (bundle == null) {
              out.nullValue()
              return
            }
            out.beginObject()
            for (key in bundle.keySet()) {
              out.name(key)
              val value = bundle[key]
              if (value == null) {
                out.nullValue()
              } else {
                gson.toJson(value, value.javaClass, out)
              }
            }
            out.endObject()
          }

          @Throws(IOException::class)
          override fun read(input: JsonReader): Bundle? {
            return when (input.peek()) {
              JsonToken.NULL -> {
                input.nextNull()
                null
              }
              JsonToken.BEGIN_OBJECT -> toBundle(readObject(input))
              else -> throw IOException("expecting object: " + input.path)
            }
          }

          @Throws(IOException::class)
          private fun toBundle(values: List<Pair<String, Any?>>): Bundle {
            val bundle = Bundle()
            for ((key, value) in values) {
              if (value is String) {
                bundle.putString(key, value as String?)
              } else if (value is Int) {
                bundle.putInt(key, value.toInt())
              } else if (value is Long) {
                bundle.putLong(key, value.toLong())
              } else if (value is Double) {
                bundle.putDouble(key, value.toDouble())
              } else if (value is Parcelable) {
                bundle.putParcelable(key, value as Parcelable?)
              } else if (value is List<*>) {
                val objectValues = value as List<Pair<String, Any?>>
                val subBundle = toBundle(objectValues)
                bundle.putParcelable(key, subBundle)
              } else {
                throw IOException("Unparcelable key, value: $key, $value")
              }
            }
            return bundle
          }

          @Throws(IOException::class)
          private fun readObject(input: JsonReader): List<Pair<String, Any?>> {
            val `object`: MutableList<Pair<String, Any?>> = ArrayList()
            input.beginObject()
            while (input.peek() != JsonToken.END_OBJECT) {
              when (input.peek()) {
                JsonToken.NAME -> {
                  val name = input.nextName()
                  val value = readValue(input)
                  `object`.add(Pair(name, value))
                }
                JsonToken.END_OBJECT -> {}
                else -> throw IOException("expecting object: " + input.path)
              }
            }
            input.endObject()
            return `object`
          }

          @Throws(IOException::class)
          private fun readValue(input: JsonReader): Any? {
            return when (input.peek()) {
              JsonToken.BEGIN_ARRAY -> readArray(input)
              JsonToken.BEGIN_OBJECT -> readObject(input)
              JsonToken.BOOLEAN -> input.nextBoolean()
              JsonToken.NULL -> {
                input.nextNull()
                null
              }
              JsonToken.NUMBER -> readNumber(input)
              JsonToken.STRING -> input.nextString()
              else -> throw IOException("expecting value: " + input.path)
            }
          }

          @Throws(IOException::class)
          private fun readNumber(input: JsonReader): Any {
            val doubleValue = input.nextDouble()
            if (doubleValue - Math.ceil(doubleValue) == 0.0) {
              val longValue = doubleValue.toLong()
              return if (longValue >= Int.MIN_VALUE && longValue <= Int.MAX_VALUE) {
                longValue.toInt()
              } else longValue
            }
            return doubleValue
          }

          @Throws(IOException::class)
          private fun readArray(input: JsonReader): List<*> {
            val list: MutableList<Any?> = ArrayList()
            input.beginArray()
            while (input.peek() != JsonToken.END_ARRAY) {
              val element = readValue(input)
              list.add(element)
            }
            input.endArray()
            return list
          }
        }
            as TypeAdapter<T>
  }
}
