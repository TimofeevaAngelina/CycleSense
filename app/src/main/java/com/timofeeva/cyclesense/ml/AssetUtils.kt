package com.timofeeva.cyclesense.ml

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AssetUtils {

    fun readAssetText(context: Context, assetName: String): String {
        return context.assets.open(assetName).bufferedReader().use { it.readText() }
    }

    fun readAssetBytes(context: Context, assetName: String): ByteArray {
        return context.assets.open(assetName).use { it.readBytes() }
    }

    fun readAssetByteBuffer(context: Context, assetName: String): ByteBuffer {
        val bytes = readAssetBytes(context, assetName)
        return ByteBuffer.allocateDirect(bytes.size)
            .order(ByteOrder.nativeOrder())
            .apply {
                put(bytes)
                rewind()
            }
    }
}