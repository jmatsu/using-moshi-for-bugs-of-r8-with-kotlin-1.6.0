package io.github.jmatsu.proguardkotlintest

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.internal.NullSafeJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.rawType
import java.lang.reflect.Type
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

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

        val example = Example(arg1 = "to", arg2 = "keep", arg3 = "properties")

        findViewById<TextView>(R.id.text).text = "${example.arg1} ${example.arg2} ${example.arg3}"

        // "com.squareup.moshi.kotlin.reflect.KotlinJsonAdapter was expected but not. ClassJsonAdapter was used instead.😨"
        Log.d("Logging", adapter.javaClass.name)
        Log.d("Logging", adapter.toJson(Example(arg3 = "arg3")))


    }
}

data class Example(
    @Json(name = "arg1")
    val arg1: String = "arg1",
    @Json(name = "different_name")
    val arg2: String = "arg2",
    val arg3: String? = null
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

        val rawTypeKotlin = rawType.kotlin

        val constructor = rawTypeKotlin.primaryConstructor ?: return null
        val parametersByName = constructor.parameters.associateBy { it.name }
        constructor.isAccessible = true

        Log.d("Logging", "${rawTypeKotlin.memberProperties.size} properties were found")

        for (property in rawTypeKotlin.memberProperties) {
            val parameter = parametersByName[property.name]

            property.isAccessible = true
            var jsonAnnotation = property.findAnnotation<Json>()
            val allAnnotations = property.annotations.toMutableList()

            if (parameter != null) {
                allAnnotations += parameter.annotations
                if (jsonAnnotation == null) {
                    jsonAnnotation = parameter.findAnnotation()
                }
            }

            Log.d("Logging", "${property.name} is associated with ${jsonAnnotation?.name}")
        }

        return null
    }

}