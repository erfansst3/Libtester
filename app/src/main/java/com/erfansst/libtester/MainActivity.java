package com.erfansst.libtester;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.ima.ImaAdsLoader;
import androidx.media3.ui.PlayerView;
import com.adivery.sdk.Adivery;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.CompanionAdSlot;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import java.util.Collections;
import java.util.List;
public class MainActivity extends Activity{
private static final String TAG="AdiveryVast";
private static final String PLACEMENT_ID="14d0a684-9c7e-48d6-a72e-6cfde3a266fa";
private static final String CONTENT_URL="https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4";
private static final int MAX_ATTEMPTS=10;
private final Handler handler=new Handler(Looper.getMainLooper());
private ExoPlayer player;
private ImaAdsLoader adsLoader;
private PlayerView playerView;
private FrameLayout adSlot;
private Button playButton;
@Override protected void onCreate(Bundle state){
super.onCreate(state);
setContentView(R.layout.activity_main);
playerView=findViewById(R.id.player_view);
adSlot=findViewById(R.id.companion_ad_slot);
playButton=findViewById(R.id.play_button);
playButton.setOnClickListener(v->{playButton.setEnabled(false);awaitVastUrl(1);});
}
private void awaitVastUrl(int attempt){
String url=Adivery.getVastUrl(PLACEMENT_ID);
if(!TextUtils.isEmpty(url)){
Log.d(TAG,"VAST URL ready: "+url);
startPlayback(url);
playButton.setEnabled(true);
return;
}
if(attempt>MAX_ATTEMPTS){
Log.e(TAG,"VAST URL unavailable");
startPlayback(null);
playButton.setEnabled(true);
return;
}
Log.d(TAG,"Waiting for VAST URL: "+attempt+"/"+MAX_ATTEMPTS);
handler.postDelayed(()->awaitVastUrl(attempt+1),1000);
}
@OptIn(markerClass=UnstableApi.class)
private void startPlayback(String url){
releasePlayer();
ExoPlayer.Builder builder=new ExoPlayer.Builder(this);
MediaItem.Builder item=new MediaItem.Builder().setUri(CONTENT_URL);
if(url!=null){
adsLoader=buildAdsLoader();
item.setAdsConfiguration(new MediaItem.AdsConfiguration.Builder(Uri.parse(url)).build());
builder.setMediaSourceFactory(new DefaultMediaSourceFactory(this).setLocalAdInsertionComponents(tag->adsLoader,playerView));
}
player=builder.build();
playerView.setPlayer(player);
if(adsLoader!=null)adsLoader.setPlayer(player);
player.setMediaItem(item.build());
player.prepare();
player.play();
}
@OptIn(markerClass=UnstableApi.class)
private ImaAdsLoader buildAdsLoader(){
return new ImaAdsLoader.Builder(this)
.setCompanionAdSlots(companionAdSlots())
.setAdEventListener(event->{
AdEvent.AdEventType type=event.getType();
Log.d(TAG,"Ad event: "+type);
switch(type){
case LOADED:adSlot.setVisibility(View.VISIBLE);break;
case SKIPPED:
case COMPLETED:
case ALL_ADS_COMPLETED:adSlot.setVisibility(View.GONE);break;
default:break;
}
}).build();
}
private List<CompanionAdSlot> companionAdSlots(){
CompanionAdSlot slot=ImaSdkFactory.getInstance().createCompanionAdSlot();
slot.setContainer(adSlot);
slot.setSize(320,50);
return Collections.singletonList(slot);
}
@Override protected void onPause(){super.onPause();if(player!=null)player.pause();}
@OptIn(markerClass=UnstableApi.class)
private void releasePlayer(){
if(player!=null){
if(adsLoader!=null)adsLoader.setPlayer(null);
playerView.setPlayer(null);
player.release();
player=null;
}
if(adsLoader!=null){adsLoader.release();adsLoader=null;}
if(adSlot!=null)adSlot.setVisibility(View.GONE);
}
@Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);releasePlayer();super.onDestroy();}
}