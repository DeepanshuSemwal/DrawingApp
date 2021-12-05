package com.example.drawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Color.WHITE
import android.media.Image
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.BoringLayout.make
import android.view.View
import android.widget.*
import androidx.activity.contextaware.withContextAvailable
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception


class MainActivity : AppCompatActivity() {
    private var drawingview:DrawingView?=null
    private var mImageButtoncurrentpaint:ImageButton?=null
    var customProgressDialog:Dialog?=null

    val openGalleryLauncher:ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    {
        result->
        if(result.resultCode== RESULT_OK && result.data!=null)
        {
            val imagebackgorund:ImageView=findViewById(R.id.iv_background)
            imagebackgorund.setImageURI(result.data?.data)
        }
    }



    val requestpermission:ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        {
                permission->
            permission.entries.forEach {
                val permissionName=it.key
                val isGranted=it.value
                if(isGranted)
                {
                    Toast.makeText(this,"permission granted now you can read the storage file.",Toast.LENGTH_LONG).show()
                    val pickntent=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickntent)

                }
                else
                {
                    if(permissionName==Manifest.permission.READ_EXTERNAL_STORAGE)
                    {
                        Toast.makeText(this,"Oops you just denied the permission.",Toast.LENGTH_LONG).show()
                    }
                }
            }


        }


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingview=findViewById(R.id.drawing_view)
        drawingview?.setbrushsize(20.toFloat())
         val linearLayoutcolor_paints=findViewById<LinearLayout>(R.id.paints_color)
        mImageButtoncurrentpaint=linearLayoutcolor_paints[1] as ImageButton
        mImageButtoncurrentpaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_pressed))





        val brushbtn:ImageButton=findViewById(R.id.brush_button)
        brushbtn.setOnClickListener(){
            brushsizechoserdialog()
        }
        val btn_gallery:ImageButton=findViewById(R.id.gallery_button)
        btn_gallery.setOnClickListener{
            RequestStoragePermission()

        }
        val undo:ImageButton=findViewById(R.id.undo_btn)
        undo.setOnClickListener(){
            drawingview?.onclickUndo()
        }
        val savebtn:ImageButton=findViewById(R.id.save_btn)
        savebtn.setOnClickListener(){
            if(isReadStorageAllowed())
            {
                showProgressDialog()
               lifecycleScope.launch{
                   val fldrawingview:FrameLayout=findViewById(R.id.fl_drawingview)
                   Savebitfile(getbitmapView(fldrawingview))
               }
            }
        }

    }
    private fun brushsizechoserdialog()
    {
        val brushdailog=Dialog(this)
        brushdailog.setContentView(R.layout.dialogue_brush)
        brushdailog.setTitle("Brush Size: ")
        val smallbtn:ImageButton=brushdailog.findViewById(R.id.small_brush)
        smallbtn.setOnClickListener(){
            drawingview?.setbrushsize(10.toFloat())
            brushdailog.dismiss()
        }

        val mediumbtn:ImageButton=brushdailog.findViewById(R.id.medium_brush)
        mediumbtn.setOnClickListener(){
            drawingview?.setbrushsize(20.toFloat())
            brushdailog.dismiss()
        }
        val largebtn:ImageButton=brushdailog.findViewById(R.id.large_brush)
        largebtn.setOnClickListener(){
            drawingview?.setbrushsize(30.toFloat())
            brushdailog.dismiss()
        }
        brushdailog.show()

    }
    fun paintclicked(view:View)
    {
        if(view!==mImageButtoncurrentpaint)
        {
            val imagebutton=view as ImageButton
            val colortag=imagebutton.tag.toString()
            drawingview?.setcolor(colortag)
            imagebutton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_pressed)

            )
            mImageButtoncurrentpaint?.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_normal)
            )
            mImageButtoncurrentpaint=view


        }
    }
    private fun RequestStoragePermission()
    {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE))
        {
            showRationaleDialog("Drawing App","Drawing App"+"Needs to access your external stroage")
        }
        else
        {
            requestpermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    private fun showRationaleDialog(
        title: String,
        message: String,
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }
    private fun isReadStorageAllowed():Boolean
    {
        val result=ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
        return result==PackageManager.PERMISSION_GRANTED
    }
    private fun getbitmapView(view:View):Bitmap
    {
        val returnbitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        val canvas= Canvas(returnbitmap)
        val bgdrawable=view.background
        if(bgdrawable!=null)
        {
            bgdrawable.draw(canvas)

        }
        else
        {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
       return  returnbitmap
    }
    private suspend fun Savebitfile(mBitmap: Bitmap?):String{
        var result=""
        withContext(Dispatchers.IO)
        {
            if(mBitmap!=null)
            {
                try {
                    val bytes=ByteArrayOutputStream()
                    val compress = mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f= File(externalCacheDir?.absoluteFile.toString()+File.separator+"Drawing App"+System.currentTimeMillis()/1000+ ".png")
                    val fo=FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result=f.absolutePath

                    runOnUiThread{
                        cancelProgressDialog()
                    if(!result.isEmpty())
                    {
                       Toast.makeText(this@MainActivity,"File save successfully :$result",Toast.LENGTH_SHORT).show()
                        shareImage(result)
                    }

                        else
                    {

                        Toast.makeText(this@MainActivity,"Something went wrong while saving" ,Toast.LENGTH_SHORT).show()
                    }
                    }

                }
                catch (e:Exception)
                {
                    result=""
                    e.printStackTrace()
                }
            }
        }
        return result
    }
    private fun showProgressDialog() {
        customProgressDialog = Dialog(this@MainActivity)
        customProgressDialog?.setContentView(R.layout.dialog_custom)
        customProgressDialog?.show()
    }
    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }
    private fun shareImage(result:String)
    {
        MediaScannerConnection.scanFile(this, arrayOf(result),null)
        {
            path,uri->
            val shareintent=Intent()
            shareintent.action=Intent.ACTION_SEND
            shareintent.putExtra(Intent.EXTRA_STREAM,uri)
            shareintent.type="image/png"
            startActivity(Intent.createChooser(shareintent,"share"))
        }
    }


}