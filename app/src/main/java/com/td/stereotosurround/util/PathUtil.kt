package com.td.stereotosurround.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns


class PathUtil {
    companion object {
        fun getDisplayName(context: Context, uri: Uri): String? {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            /*
             * Get the column indexes of the data in the Cursor,
             * move to the first row in the Cursor, get the data,
             * and display it.
             */
            /*
            * Get the column indexes of the data in the Cursor,
            * move to the first row in the Cursor, get the data,
            * and display it.
            */
            var name: String? = null
            name = cursor?.run {
                val nameIndex: Int = getColumnIndex(OpenableColumns.DISPLAY_NAME)
                moveToFirst()
                getString(nameIndex)
            }
            return name
        }

    }

}
