package com.paperpig.maimaidata.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat

/**
 * 权限工具类
 */
class PermissionHelper private constructor(private val activity: Activity) {

    companion object {
        fun with(activity: Activity): PermissionHelper {
            return PermissionHelper(activity)
        }
    }

    private var mPermissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private lateinit var permissionCallback: PermissionCallback


    interface PermissionCallback {
        fun onAllGranted()
        fun onDenied(deniedPermissions: List<String>)
    }

    fun registerLauncher(launcher: ActivityResultLauncher<Array<String>>): PermissionHelper {
        mPermissionLauncher = launcher
        return this
    }


    fun checkStoragePermission(callback: PermissionCallback) {
        permissionCallback = callback
        val permissionsStorage =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ 请求细粒度权限
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10-12 请求旧权限
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                // Android 10以下权限请求
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }

        if (!permissionsStorage.all { permission ->
                ActivityCompat.checkSelfPermission(
                    activity,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
        ) {
            mPermissionLauncher?.launch(permissionsStorage)
                ?: throw IllegalStateException("Permission launcher is not registered. Please call registerLauncher() before requesting permissions.")
        } else {
            permissionCallback.onAllGranted()
        }
    }

    fun onRequestPermissionsResult(map: Map<String, @JvmSuppressWildcards Boolean>) {
        val denied = map.filterValues { !it }.keys.toList()
        if (denied.isEmpty()) {
            permissionCallback.onAllGranted()
        } else {
            permissionCallback.onDenied(denied)
        }
    }
}