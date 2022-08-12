package com.mandev.drawingapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.mandev.drawingapp.databinding.ActivityMainBinding
import com.mandev.drawingapp.databinding.DialogBrushSizeBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var dialogBrushSizeBinding: DialogBrushSizeBinding
    private var mImageButtonCurrentPaint: ImageButton? =
            null // A variable for current color is picked from color pallet.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.drawingView.setSizeForBrush(20.toFloat()) // Setting the default brush size to drawing view.


        mImageButtonCurrentPaint = binding.llPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(
                        this,
                        R.drawable.pallet_pressed
                )
        )

        binding.ibBrush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        binding.ibGallery.setOnClickListener {
            if (isReadStorageAllowed()) {
                val pickPhoto = Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
                startActivityForResult(pickPhoto, GALLERY)
            } else {
                requestStoragePermission()
            }
        }

        binding.ibUndo.setOnClickListener {
            binding.drawingView.onClickUndo()
        }

        binding.ibSave.setOnClickListener {
            if (isReadStorageAllowed()) {
                BitmapAsyncTask(getBitmapFromView(binding.flDrawingViewContainer)).execute()
            } else {
                requestStoragePermission()
            }
        }
    }


    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                        this@MainActivity,
                        "Permission granted now you can read the storage files.",
                        Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                        this@MainActivity,
                        "Oops you just denied the permission.",
                        Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY) {
                try {
                    if (data!!.data != null) {
                        binding.ivBackground.visibility = View.VISIBLE
                        binding.ivBackground.setImageURI(data.data)
                    } else {
                        Toast.makeText(
                                this@MainActivity,
                                "Error in parsing the image or its corrupted.",
                                Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
//        dialogBrushSizeBinding = DialogBrushSizeBinding.inflate(layoutInflater)
//        setContentView(dialogBrushSizeBinding.root)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size :")
        val ibSmallBrush: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        val ibMediumBrush: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        val ibLargeBrush: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        val smallBtn = ibSmallBrush
        smallBtn.setOnClickListener {
            binding.drawingView.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn = ibMediumBrush
        mediumBtn.setOnClickListener {
            binding.drawingView.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        val largeBtn = ibLargeBrush
        largeBtn.setOnClickListener {
            binding.drawingView.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    fun paintClicked(view: View) {
        if (view !== mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            binding.drawingView.setColor(colorTag)
            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))
            mImageButtonCurrentPaint!!.setImageDrawable(
                    ContextCompat.getDrawable(
                            this,
                            R.drawable.pallet_normal
                    )
            )

            mImageButtonCurrentPaint = view
        }
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ).toString()
                )
        ) {
            //If the user has denied the permission previously your code will come to this block
            //Here you can explain why you need this permission
            //Explain here why you need this permission
        }

        //And finally ask for the permission
        ActivityCompat.requestPermissions(
                this, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ),
                STORAGE_PERMISSION_CODE
        )
    }

    private fun isReadStorageAllowed(): Boolean {
        //Getting the permission status
        // Here the checkSelfPermission is

        val result = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
        )

        //If permission is granted returning true and If permission is not granted returning false
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitmapFromView(view: View): Bitmap {

        //Define a bitmap with the same size as the view.
        // CreateBitmap : Returns a mutable bitmap with the specified width and height
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        //Bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        //Get the view's background
        val bgDrawable = view.background
        if (bgDrawable != null) {
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas)
        } else {
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE)
        }
        // draw the view on the canvas
        view.draw(canvas)
        //return the bitmap
        return returnedBitmap
    }

    @SuppressLint("StaticFieldLeak")
    private inner class BitmapAsyncTask(val mBitmap: Bitmap?) :
            AsyncTask<Any, Void, String>() {

        @Suppress("DEPRECATION")
        private var mDialog: ProgressDialog? = null

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        override fun doInBackground(vararg params: Any): String {

            var result = ""

            if (mBitmap != null) {

                try {
                    val bytes = ByteArrayOutputStream() // Creates a new byte array output stream.
                    // The buffer capacity is initially 32 bytes, though its size increases if necessary.

                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(
                            externalCacheDir!!.absoluteFile.toString()
                                    + File.separator + "KidDrawingApp_" + System.currentTimeMillis() / 1000 + ".jpg"
                    )
                    // Here the Environment : Provides access to environment variables.
                    // getExternalStorageDirectory : returns the primary shared/external storage directory.
                    // absoluteFile : Returns the absolute form of this abstract pathname.
                    // File.separator : The system-dependent default name-separator character. This string contains a single character.

                    val fo =
                            FileOutputStream(f) // Creates a file output stream to write to the file represented by the specified object.
                    fo.write(bytes.toByteArray()) // Writes bytes from the specified byte array to this file output stream.
                    fo.close() // Closes this file output stream and releases any system resources associated with this stream. This file output stream may no longer be used for writing bytes.
                    result = f.absolutePath // The file absolute path is return as a result.
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
            return result
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)

            cancelProgressDialog()

            if (!result.isEmpty()) {
                Toast.makeText(this@MainActivity, "File saved successfully :$result", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Something went wrong while saving the file.", Toast.LENGTH_SHORT).show()
            }

            MediaScannerConnection.scanFile(
                    this@MainActivity, arrayOf(result), null
            ) { path, uri ->
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(
                        Intent.EXTRA_STREAM,
                        uri
                ) // A content: URI holding a stream of data associated with the Intent, used to supply the data being sent.
                shareIntent.type =
                        "image/jpeg" // The MIME type of the data being handled by this intent.
                startActivity(
                        Intent.createChooser(
                                shareIntent,
                                "Share"
                        )
                )
            }
        }

        private fun showProgressDialog() {
            @Suppress("DEPRECATION")
            mDialog = ProgressDialog.show(
                    this@MainActivity,
                    "",
                    "Saving your image..."
            )
        }

        private fun cancelProgressDialog() {
            if (mDialog != null) {
                mDialog!!.dismiss()
                mDialog = null
            }
        }
    }

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }
}