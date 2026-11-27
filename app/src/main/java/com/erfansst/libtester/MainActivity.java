package com.erfansst.libtester;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;
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
@Override protected void onCreate(Bundle state){super.onCreate(state);setContentView(R.layout.activity_main);playerView=findViewById(R.id.player_view);adSlot=findViewById(R.id.companion_ad_slot);}
public void ldo(View view){view.setEnabled(false);awaitVastUrl(1,view);}
private void awaitVastUrl(int attempt,View button){
String url=Adivery.getVastUrl(PLACEMENT_ID);
if(!TextUtils.isEmpty(url)){Log.d(TAG,"VAST URL ready");Toast.makeText(this,"VAST ready",Toast.LENGTH_SHORT).show();startPlayback(url);button.setEnabled(true);return;}
if(attempt>MAX_ATTEMPTS){Log.e(TAG,"VAST URL unavailable after "+MAX_ATTEMPTS+" attempts");Toast.makeText(this,"VAST URL unavailable",Toast.LENGTH_LONG).show();startPlayback(null);button.setEnabled(true);return;}
Log.d(TAG,"Waiting for VAST URL: "+attempt+"/"+MAX_ATTEMPTS);
handler.postDelayed(()->awaitVastUrl(attempt+1,button),1000);
}
@OptIn(markerClass=UnstableApi.class)
private void startPlayback(String url){
releasePlayer();
ExoPlayer.Builder builder=new ExoPlayer.Builder(this);
MediaItem.Builder item=new MediaItem.Builder().setUri(CONTENT_URL);
if(url!=null){
adsLoader=new ImaAdsLoader.Builder(this).setCompanionAdSlots(Collections.singletonList(createCompanionSlot())).setAdEventListener(event->{AdEvent.AdEventType type=event.getType();Log.d(TAG,"Ad event: "+type);if(type==AdEvent.AdEventType.LOADED)adSlot.setVisibility(View.VISIBLE);if(type==AdEvent.AdEventType.SKIPPED||type==AdEvent.AdEventType.COMPLETED||type==AdEvent.AdEventType.ALL_ADS_COMPLETED)adSlot.setVisibility(View.GONE);}).build();
item.setAdsConfiguration(new MediaItem.AdsConfiguration.Builder(Uri.parse(url)).build());
builder.setMediaSourceFactory(new DefaultMediaSourceFactory(this).setLocalAdInsertionComponents(unused->adsLoader,playerView));
}
player=builder.build();playerView.setPlayer(player);if(adsLoader!=null)adsLoader.setPlayer(player);player.setMediaItem(item.build());player.prepare();player.play();
}
private CompanionAdSlot createCompanionSlot(){CompanionAdSlot slot=ImaSdkFactory.getInstance().createCompanionAdSlot();slot.setContainer(adSlot);slot.setSize(320,50);return slot;}
@OptIn(markerClass=UnstableApi.class)
private void releasePlayer(){if(player!=null){if(adsLoader!=null)adsLoader.setPlayer(null);playerView.setPlayer(null);player.release();player=null;}if(adsLoader!=null){adsLoader.release();adsLoader=null;}if(adSlot!=null)adSlot.setVisibility(View.GONE);}
@Override protected void onPause(){super.onPause();if(player!=null)player.pause();}
@Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);releasePlayer();super.onDestroy();}
}