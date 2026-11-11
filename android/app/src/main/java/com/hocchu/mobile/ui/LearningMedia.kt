package com.hocchu.mobile.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaDataSource
import android.media.MediaPlayer
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hocchu.mobile.BuildConfig
import com.hocchu.mobile.data.LocalMediaServer
import com.hocchu.mobile.data.publicMediaAddress
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection

val LocalSoundEnabled=staticCompositionLocalOf {true}

/** Public content only. Never attach a JWT to media requests or follow redirects. */
internal fun mediaBytes(raw:String,server:String):ByteArray {
    val limit=8*1024*1024
    if(raw.startsWith("data:")) {
        require(raw.length<12*1024*1024&&raw.substringBefore(',').endsWith(";base64"))
        return Base64.decode(raw.substringAfter(','),Base64.DEFAULT).also {require(it.size<=limit)}
    }
    val uri=publicMediaAddress(raw,server,BuildConfig.DEBUG)
    val connection=uri.toURL().openConnection() as HttpURLConnection
    try {
        connection.instanceFollowRedirects=false;connection.connectTimeout=6000;connection.readTimeout=8000
        require(connection.responseCode==200&&connection.contentLengthLong<=limit)
        return connection.inputStream.use {input->val output=ByteArrayOutputStream();val buffer=ByteArray(8192)
            while(true){val n=input.read(buffer);if(n<0)break;require(output.size()+n<=limit);output.write(buffer,0,n)};output.toByteArray()}
    } finally {connection.disconnect()}
}
internal fun mediaBitmap(data:ByteArray):Bitmap {
    val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true};BitmapFactory.decodeByteArray(data,0,data.size,bounds)
    require(bounds.outWidth>0&&bounds.outHeight>0&&bounds.outWidth.toLong()*bounds.outHeight<=32_000_000)
    var sample=1;while(maxOf(bounds.outWidth,bounds.outHeight)/sample>1024)sample*=2
    return requireNotNull(BitmapFactory.decodeByteArray(data,0,data.size,BitmapFactory.Options().apply {inSampleSize=sample}))
}
@Composable
internal fun LearningPicture(source:String,fallback:String,description:String,modifier:Modifier=Modifier) {
    val server=LocalMediaServer.current;val preview=LocalInspectionMode.current
    var failed by remember(source,server){mutableStateOf(false)}
    val bitmap by produceState<Bitmap?>(null,source,server) {
        if(source.isNotBlank()&&!preview) try {value=withContext(Dispatchers.IO){mediaBitmap(mediaBytes(source,server))}}
        catch(e:CancellationException){throw e}catch(_:Exception){failed=true}
    }
    if(bitmap!=null) Image(bitmap!!.asImageBitmap(),description,modifier.fillMaxWidth().heightIn(min=120.dp,max=360.dp),contentScale=ContentScale.Fit)
    else Column(modifier) {
        if(fallback.isNotBlank()) Text(fallback,fontSize=64.sp)
        if(source.isNotBlank()&&!preview) {
            if(failed) Text("Chưa tải được ảnh. Hãy kiểm tra kết nối.",style=MaterialTheme.typography.bodySmall,color=Muted)
            else LinearProgressIndicator(Modifier.width(90.dp))
        }
    }
}
private class ByteAudio(private val bytes:ByteArray):MediaDataSource() {
    override fun getSize()=bytes.size.toLong()
    override fun close() {}
    override fun readAt(position:Long,buffer:ByteArray,offset:Int,size:Int):Int {
        require(position>=0 && offset>=0 && size>=0 && offset.toLong()+size<=buffer.size)
        if(size==0)return 0
        if(position>=bytes.size)return -1
        val length=minOf(size,bytes.size-position.toInt());bytes.copyInto(buffer,offset,position.toInt(),position.toInt()+length);return length
    }
}
@Composable
internal fun rememberMediaSpeech(fallback:(String)->Unit):(String,String)->Unit {
    val scope=rememberCoroutineScope();val context=LocalContext.current;val server=LocalMediaServer.current
    val currentFallback by rememberUpdatedState(fallback);val enabled by rememberUpdatedState(LocalSoundEnabled.current)
    var player by remember {mutableStateOf<MediaPlayer?>(null)}
    var job by remember {mutableStateOf<Job?>(null)}
    DisposableEffect(server) {onDispose {job?.cancel();player?.release();player=null}}
    val owner=androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer=androidx.lifecycle.LifecycleEventObserver {_,event->
            if(event==androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {job?.cancel();player?.release();player=null}
        }
        owner.lifecycle.addObserver(observer)
        onDispose {owner.lifecycle.removeObserver(observer)}
    }
    LaunchedEffect(enabled) {if(!enabled){job?.cancel();player?.release();player=null}}
    return {text,url->
        job?.cancel();player?.release();player=null
        if(enabled) {
            if(url.isBlank()) currentFallback(text)
            else job=scope.launch {
                try {
                    val bytes=withContext(Dispatchers.IO){mediaBytes(url,server)}
                    val media=MediaPlayer();player=media;media.setDataSource(ByteAudio(bytes))
                    media.setOnPreparedListener {if(player===it)it.start()}
                    media.setOnCompletionListener {it.release();if(player===it)player=null}
                    media.setOnErrorListener {p,_,_->p.release();if(player===p)player=null;currentFallback(text);true}
                    media.prepareAsync()
                }catch(e:CancellationException){throw e}catch(_:Exception){player?.release();player=null
                    Toast.makeText(context,"Chưa phát được âm thanh, đang dùng giọng đọc của máy.",Toast.LENGTH_SHORT).show();currentFallback(text)}
            }
        }
    }
}
