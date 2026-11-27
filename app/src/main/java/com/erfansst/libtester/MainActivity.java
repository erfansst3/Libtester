package com.erfansst.libtester;
import android.app.Activity;
import android.os.Bundle;
import android.net.Uri;
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
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private ExoPlayer player;
    private ImaAdsLoader adsLoader;
    private PlayerView playerView;
    private FrameLayout adSlotContainer;
    private String vastUrl;
    private final String contentUri="https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Adivery.configure(getApplication(),"a2e19f45-f3d5-4abb-800b-3a02ae635736");
        vastUrl=Adivery.getVastUrl("14d0a684-9c7e-48d6-a72e-6cfde3a266fa");
        playerView=findViewById(R.id.player_view);
        adSlotContainer=findViewById(R.id.companion_ad_slot);
    }

    @OptIn(markerClass=UnstableApi.class)
    public void ldo(View view){
        if(vastUrl==null||vastUrl.isEmpty()){
            Toast.makeText(this,"VAST URL is unavailable",Toast.LENGTH_SHORT).show();
            return;
        }
        if(player!=null)releasePlayer();
        CompanionAdSlot slot=ImaSdkFactory.getInstance().createCompanionAdSlot();
        slot.setContainer(adSlotContainer);
        slot.setSize(320,50);
        List<CompanionAdSlot> slots=new ArrayList<>();
        slots.add(slot);
        adsLoader=new ImaAdsLoader.Builder(this)
            .setCompanionAdSlots(slots)
            .setAdEventListener(event->{
                AdEvent.AdEventType type=event.getType();
                if(type==AdEvent.AdEventType.LOADED)adSlotContainer.setVisibility(View.VISIBLE);
                if(type==AdEvent.AdEventType.SKIPPED||type==AdEvent.AdEventType.COMPLETED||type==AdEvent.AdEventType.ALL_ADS_COMPLETED)adSlotContainer.setVisibility(View.GONE);
            }).build();
        DefaultMediaSourceFactory factory=new DefaultMediaSourceFactory(this).setLocalAdInsertionComponents(unused->adsLoader,playerView);
        player=new ExoPlayer.Builder(this).setMediaSourceFactory(factory).build();
        playerView.setPlayer(player);
        adsLoader.setPlayer(player);
        MediaItem item=new MediaItem.Builder().setUri(Uri.parse(contentUri)).setAdsConfiguration(new MediaItem.AdsConfiguration.Builder(Uri.parse(vastUrl)).build()).build();
        player.setMediaItem(item);
        player.prepare();
        player.play();
    }

    private void releasePlayer(){
        if(adsLoader!=null){adsLoader.setPlayer(null);adsLoader.release();adsLoader=null;}
        if(player!=null){player.release();player=null;}
        playerView.setPlayer(null);
        adSlotContainer.setVisibility(View.GONE);
    }

    @Override
    protected void onPause(){super.onPause();if(player!=null)player.pause();}

    @Override
    protected void onDestroy(){releasePlayer();super.onDestroy();}
}
