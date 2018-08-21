package org.mightyfrog.android.palette

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.graphics.Palette
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import io.fotoapparat.Fotoapparat
import kotlinx.android.synthetic.main.activity_main.*

/**
 * @author Shigehiro Soejima
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    private lateinit var camera: Fotoapparat

    companion object {
        const val PERM_REQ_CAMERA = 0xCAFE + 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        checkCameraPermission()

        fab.setOnClickListener {
            takePicture()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERM_REQ_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    init()
                } else {
                    showCameraPermissionSnackbar()
                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.apply {
            val paletteSize = prefs.getInt("palette_size", 16)
            findItem(R.id.action_palette_size_8).isChecked = paletteSize == 8
            findItem(R.id.action_palette_size_16).isChecked = paletteSize == 16
            findItem(R.id.action_palette_size_24).isChecked = paletteSize == 24
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_palette_size_8 -> {
                prefs.edit().putInt("palette_size", 8).apply()
                invalidateOptionsMenu()
            }
            R.id.action_palette_size_16 -> {
                prefs.edit().putInt("palette_size", 16).apply()
                invalidateOptionsMenu()
            }
            R.id.action_palette_size_24 -> {
                prefs.edit().putInt("palette_size", 24).apply()
                invalidateOptionsMenu()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun init() {
        fab.visibility = View.VISIBLE

        camera = Fotoapparat.with(this)
                .into(cameraView)
                .build()
        camera.start()
    }

    private fun checkCameraPermission() {
        val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        when (permissionCheck) {
            PackageManager.PERMISSION_GRANTED -> init()
            else -> requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERM_REQ_CAMERA)
    }

    private fun showCameraPermissionSnackbar() {
        Snackbar.make(findViewById<View>(android.R.id.content), R.string.perm_required_camera, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.allow) {
                    requestCameraPermission()
                }
                .show()
    }

    private fun takePicture() {
        progressBar.visibility = View.VISIBLE
        camera.takePicture().toBitmap().whenAvailable { callback ->
            callback?.apply {
                extractColors(bitmap)
            }
        }
    }

    private fun extractColors(bitmap: Bitmap) {
        val paletteSize = prefs.getInt("palette_size", 16)
        Palette.from(bitmap).maximumColorCount(paletteSize).generate { palette ->
            palette?.apply {
                openBottomSheet(this)
            }
            progressBar.visibility = View.GONE
        }
    }

    private fun openBottomSheet(palette: Palette) {
        val root = findViewById<FrameLayout>(android.R.id.content)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_swatches, root, false)

        val swatches = listOf(
                palette.dominantSwatch,
                palette.lightVibrantSwatch,
                palette.vibrantSwatch,
                palette.darkVibrantSwatch,
                palette.darkMutedSwatch,
                palette.mutedSwatch,
                palette.darkMutedSwatch)
        val labels = listOf<TextView>(
                view.findViewById(R.id.dominant_label),
                view.findViewById(R.id.lvibrant_label),
                view.findViewById(R.id.vibrant_label),
                view.findViewById(R.id.dvibrant_label),
                view.findViewById(R.id.lmuted_label),
                view.findViewById(R.id.muted_label),
                view.findViewById(R.id.dmuted_label))
        val bodies = listOf<TextView>(
                view.findViewById(R.id.dominant),
                view.findViewById(R.id.lvibrant),
                view.findViewById(R.id.vibrant),
                view.findViewById(R.id.dvibrant),
                view.findViewById(R.id.lmuted),
                view.findViewById(R.id.muted),
                view.findViewById(R.id.dmuted))

        swatches.forEachIndexed { index, swatch ->
            swatch?.apply {
                labels[index].setBackgroundColor(rgb)
                labels[index].setTextColor(titleTextColor)
                bodies[index].setBackgroundColor(rgb)
                bodies[index].setTextColor(bodyTextColor)
                bodies[index].text = toString()
            } ?: run {
                labels[index].visibility = View.GONE
                bodies[index].visibility = View.GONE
            }
        }

        BottomSheetDialog(this).apply {
            setContentView(view)
            show()
        }
    }
}
