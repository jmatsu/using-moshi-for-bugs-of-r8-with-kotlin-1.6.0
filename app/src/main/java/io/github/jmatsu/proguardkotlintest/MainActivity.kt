package io.github.jmatsu.proguardkotlintest

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.internal.NullSafeJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.rawType
import java.lang.reflect.Type

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val moshi = Moshi.Builder()
            .add(JsonFactoryForLogging())
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val adapter =
            (moshi.adapter(Example::class.java) as NullSafeJsonAdapter).delegate()

        // "com.squareup.moshi.kotlin.reflect.KotlinJsonAdapter was expected but not. ClassJsonAdapter was used instead.😨"
        Log.d("Logging", adapter.javaClass.name)

        Log.d("Logging", adapter.toJson(Example()))
    }
}

data class Example(
    @Json
    val arg1: String = "arg1",
    @Json(name = "different_name")
    val arg2: String = "arg2"
)

class JsonFactoryForLogging : JsonAdapter.Factory {
    override fun create(
        type: Type,
        annotations: MutableSet<out Annotation>,
        moshi: Moshi
    ): JsonAdapter<*>? {
        val rawType = type.rawType

        if (Example::class.java.name != rawType.name) {
            return null
        }

        // at least 1 annotation is expected and it has to be Kotlin Metadata
        Log.d("Logging", "${rawType.annotations.size} annotations were found.")

        if (!rawType.isAnnotationPresent(Metadata::class.java)) {
            Log.d("Logging", "kotlin metadata disappeared ;(")
            return null
        } else {
            Log.d("Logging", "kotlin metadata was found. :)") // expected but not
        }

        return null
    }

}