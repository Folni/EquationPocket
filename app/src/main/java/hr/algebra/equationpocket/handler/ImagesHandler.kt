package hr.algebra.equationpocket.handler

import android.content.Context
import android.util.Log
import hr.algebra.equationpocket.factory.createHttpUrlCon
import java.io.File
import java.net.HttpURLConnection
import java.nio.file.Files

fun download(context: Context, url: String) : String?{
    val filename = url.substring(url.lastIndexOf(File.separator) + 1)
    val file: File = createFile(context, filename)
    try {
        val con: HttpURLConnection = createHttpUrlCon(url)
        Files.copy(con.getInputStream(), file.toPath())
        return file.absolutePath
    } catch (e: Exception) {
        Log.e("ERROR", e.toString(), e)
    }
    return null
}

fun createFile(context: Context, filename: String): File {
    val dir = context.getExternalFilesDir(null)
    val file = File(dir, filename)
    if(file.exists()) file.delete()
    return file
}