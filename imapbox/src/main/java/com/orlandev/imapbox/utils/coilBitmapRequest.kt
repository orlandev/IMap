package com.orlandev.imapbox.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.lang.Exception

suspend fun coilBitmapRequest(ctx: Context, urlImage: String): Bitmap? {
    return try {
        val loader = ImageLoader(ctx)
        val request = ImageRequest.Builder(context = ctx)
            .data(urlImage)
            .allowHardware(false) // Disable hardware bitmaps.
            .build()
        val result = (loader.execute(request) as SuccessResult).drawable
        (result as BitmapDrawable).bitmap

    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

}