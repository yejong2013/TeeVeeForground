package com.it_tech613.zhe.teevee.activity;

import android.app.Dialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.it_tech613.zhe.teevee.R;
import com.it_tech613.zhe.teevee.apps.Constants;
import com.it_tech613.zhe.teevee.apps.MyApp;
import com.it_tech613.zhe.teevee.dialog.PackageDlg;
import com.it_tech613.zhe.teevee.ijklib.widget.media.AndroidMediaController;
import com.it_tech613.zhe.teevee.ijklib.widget.media.IjkVideoView;
import com.it_tech613.zhe.teevee.models.MovieModel;
import com.it_tech613.zhe.teevee.models.PositionModel;
import com.it_tech613.zhe.teevee.utils.Utils;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class VideoIjkPlayActivity extends AppCompatActivity implements  SeekBar.OnSeekBarChangeListener, View.OnClickListener, IMediaPlayer.OnErrorListener,IMediaPlayer.OnCompletionListener {
    private IjkVideoView surfaceView;
    private AndroidMediaController mMediaController;
    private TextView txt_num;
    private TableLayout mHudView;

    public int mHeight;
    public int mWidth;
    SeekBar seekBar;
    LinearLayout bottom_lay, def_lay,ly_play,ly_resolution,ly_audio,ly_subtitle,ly_fav;
    ImageView img_play;

    boolean first = true;

    TextView title_txt, start_txt, end_txt;
    ImageView imageView,image_icon;
    Handler mHandler = new Handler();
    Handler handler = new Handler();
    Runnable mTicker,rssTicker;
    String cont_url,title,rss="";
    int dration_time = 0,selected_item = 0,position,fav_pos = 0,msg_time = 0;
    long media_position;
    List<MovieModel>vod_favlist;
    List<PositionModel> positionModels;
    boolean is_create = true,is_long=false;
    boolean is_exit = false,is_rss = false,is_msg = false;
    List<String>  pkg_datas;
    Handler rssHandler = new Handler();
    TextView txt_rss;
    private FrameLayout mVideoSurfaceFrame = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vod_ijk_player);
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .penaltyLog()
                .detectAll()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .penaltyLog()
                .detectAll()
                .build());
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mMediaController = new AndroidMediaController(this, false);
        txt_num = findViewById(R.id.toast_text_view);
        mHudView = findViewById(R.id.hud_view);
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");

        pkg_datas = new ArrayList<>();
        for (int i = 0; i < getResources().getStringArray(R.array.package_list1).length; i++) {
            pkg_datas.add(getResources().getStringArray(R.array.package_list1)[i]);
        }
        if(MyApp.instance.getPreference().get(Constants.MOVIE_FAV)==null){
            vod_favlist = new ArrayList<>();
        }else {
            vod_favlist = (List<MovieModel>) MyApp.instance.getPreference().get(Constants.MOVIE_FAV);
        }
        mVideoSurfaceFrame = findViewById(R.id.video_surface_frame);
        surfaceView = findViewById(R.id.surface_view);
        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bottom_lay.getVisibility()== View.VISIBLE){
                    bottom_lay.setVisibility(View.GONE);
                }else {
                    bottom_lay.setVisibility(View.VISIBLE);
                    updateTimer();
                }
            }
        });
        def_lay = (LinearLayout) findViewById(R.id.def_lay);
        bottom_lay = (LinearLayout) findViewById(R.id.vod_bottom_lay);
        ly_fav = findViewById(R.id.ly_fav);
        ly_fav.setOnClickListener(this);
        title_txt = (TextView) findViewById(R.id.vod_channel_title);
        imageView = (ImageView) findViewById(R.id.vod_channel_img);
        start_txt = (TextView) findViewById(R.id.vod_start_time);
        end_txt = (TextView) findViewById(R.id.vod_end_time);
        seekBar = (SeekBar) findViewById(R.id.vod_seekbar);
        seekBar.setOnSeekBarChangeListener(this);

        ly_audio = findViewById(R.id.ly_audio);

        ly_play = findViewById(R.id.ly_play);
        ly_resolution = findViewById(R.id.ly_resolution);
        ly_subtitle = findViewById(R.id.ly_subtitle);

        ly_subtitle.setOnClickListener(this);
        ly_play.setOnClickListener(this);
        ly_resolution.setOnClickListener(this);
        ly_subtitle.setOnClickListener(this);
        ly_audio.setOnClickListener(this);

        img_play = findViewById(R.id.img_play);

        txt_rss = findViewById(R.id.txt_rss);
        txt_rss.setSingleLine(true);

        image_icon = findViewById(R.id.image_icon);
        Picasso.with(this).load(Constants.GetIcon(this))
                .memoryPolicy(MemoryPolicy.NO_CACHE)
                .networkPolicy(NetworkPolicy.NO_CACHE)
                .error(R.drawable.icon)
                .into(image_icon);

        title_txt.setText(getIntent().getStringExtra("title"));
        cont_url = getIntent().getStringExtra("url");
        title = getIntent().getStringExtra("title");
        try {
            Picasso.with(this).load(getIntent().getStringExtra("img"))
                    .placeholder(R.drawable.icon_default)
                    .error(R.drawable.icon_default)
                    .into(imageView);

        }catch (Exception e){
            Picasso.with(this).load(R.drawable.icon_default).into(imageView);
        }

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mHeight = displayMetrics.heightPixels;
        mWidth = displayMetrics.widthPixels;

        playVideo(cont_url);
        FullScreencall();
        getRespond();
    }

    private void getRespond(){
        StringRequest request = new StringRequest(Constants.GetUrl(this)+"="+Constants.GetKey(this), new Response.Listener<String>() {
            @Override
            public void onResponse(String string) {
                try {
                    JSONObject object = new JSONObject(string);
                    if (((String) object.get("status")).equalsIgnoreCase("success")) {
                        String msg_on_off = (String) object.get("message_on_off");
                        is_msg = !msg_on_off.equalsIgnoreCase("0");
                        try {
                            msg_time = Integer.parseInt((String)object.get("message_time"));
                        }catch (Exception e){
                            msg_time = 20;
                        }
                        String rss_feed = "                 "+object.get("msg")+"                 ";
                        if(rss.equalsIgnoreCase(rss_feed)){
                            txt_rss.setVisibility(View.GONE);
                            is_rss = false;
                        }else {
                            rss =rss_feed;
                            is_rss = true;
                            txt_rss.setVisibility(View.VISIBLE);
                        }
                        Paint paint = new Paint();
                        paint.setTextSize(25);
                        paint.setColor(Color.BLACK);
                        paint.setStyle(Paint.Style.FILL);
                        paint.setTypeface(Typeface.DEFAULT);
                        Rect result = new Rect();
                        paint.getTextBounds(rss, 0, rss.length(), result);
                        txt_rss.setBackgroundResource(R.color.black);
                        int divide = (MyApp.SCREEN_WIDTH)/Utils.dp2px(VideoIjkPlayActivity.this,result.width());
                        if(divide<1){
                            Log.e("rss1",rss);
                            if(is_msg){
                                image_icon.setVisibility(View.VISIBLE);
                                txt_rss.setSelected(true);
                                txt_rss.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                                txt_rss.setText(rss);
                            }else {
                                txt_rss.setVisibility(View.GONE);
                                image_icon.setVisibility(View.GONE);
                            }
                        }else {
                            for(int i =0;i<divide+1;i++){
                                rss +=rss;
                            }
                            Log.e("rss2",rss);
                            if(is_msg){
                                image_icon.setVisibility(View.VISIBLE);
                                txt_rss.setSelected(true);
                                txt_rss.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                                txt_rss.setText(rss);
                            }else {
                                txt_rss.setVisibility(View.GONE);
                                image_icon.setVisibility(View.GONE);
                            }

//                            txt_rss.setText(rss);
//                            txt_rss.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.marquee1));
                        }
                        rssTimer();
                    } else {
                        Toast.makeText(VideoIjkPlayActivity.this, "Server Error!", Toast.LENGTH_SHORT).show();
                    }
                }catch (JSONException e){
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(getApplicationContext(), "Some error occurred!!", Toast.LENGTH_SHORT).show();
            }
        });

        RequestQueue rQueue = Volley.newRequestQueue(VideoIjkPlayActivity.this);
        rQueue.add(request);
    }
    int rss_time;
    private void rssTimer() {
        rss_time = msg_time;
        rssTicker = new Runnable() {
            public void run() {
                if (rss_time < 1) {
                    txt_rss.setText("");
                    txt_rss.setBackgroundResource(R.color.trans_parent);
                    txt_rss.setVisibility(View.GONE);
                    image_icon.setVisibility(View.GONE);
                    return;
                }
                runRssTicker();
            }
        };
        rssTicker.run();
    }

    private void runRssTicker() {
        rss_time --;
        long next = SystemClock.uptimeMillis() + 1000;
        rssHandler.postAtTime(rssTicker, next);
    }

    private void playVideo(String path) {
        if(def_lay.getVisibility()==View.VISIBLE)def_lay.setVisibility(View.GONE);
        releaseMediaPlayer();
//        Log.e("url",path);
        toggleFullscreen(true);
        try {
            surfaceView.setMediaController(mMediaController);
            surfaceView.setHudView(mHudView);
            mMediaController.hide();
            surfaceView.setVideoPath(path);
            surfaceView.setOnCompletionListener(this);
            surfaceView.setOnErrorListener(this);
            surfaceView.start();
//            mediaSeekTo();
            updateProgressBar();
            updateTimer();
        } catch (Exception e) {
            Toast.makeText(this, "Error in creating player!", Toast
                    .LENGTH_LONG).show();
        }

    }
    public void updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            if (surfaceView != null) {
                long totalDuration = surfaceView.getDuration();
                long currentDuration = surfaceView.getCurrentPosition();
                end_txt.setText("" + Utils.milliSecondsToTimer(totalDuration));
                start_txt.setText("" + Utils.milliSecondsToTimer(currentDuration));
                int progress = (int) (Utils.getProgressPercentage(currentDuration, totalDuration));
                seekBar.setProgress(progress);
                mHandler.postDelayed(this, 500);
            }
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        if (!is_create) {
            if (surfaceView != null) {
                releaseMediaPlayer();
                surfaceView = null;
            }
            playVideo(cont_url);
        } else {
            is_create = false;
        }
    }
    private void toggleFullscreen(boolean fullscreen) {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        if (fullscreen) {
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        } else {
            attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        getWindow().setAttributes(attrs);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
    }
    @Override
    protected void onUserLeaveHint()
    {
        releaseMediaPlayer();
        finish();
        super.onUserLeaveHint();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaPlayer();
    }
    private void releaseMediaPlayer() {
        if (surfaceView == null)
            return;
        surfaceView.release(true);
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ly_audio:
                break;

            case R.id.ly_subtitle:
                break;
            case R.id.ly_resolution:
                surfaceView.toggleAspectRatio();
                break;

            case R.id.ly_play:
                if (surfaceView.isPlaying()) {
                    surfaceView.pause();
                    img_play.setImageResource(R.drawable.exo_play);
                } else {
                    surfaceView.start();
                    img_play.setImageResource(R.drawable.exo_pause);
                }
                break;
            case R.id.ly_fav:
                for(int i = 0;i<vod_favlist.size();i++){
                    if(vod_favlist.get(i).getStream_id().equalsIgnoreCase(MyApp.vod_model.getStream_id())){
                        pkg_datas.set(0,"Remove from Fav");
                        is_exit = true;
                        fav_pos = i;
                        break;
                    }else {
                        pkg_datas.set(0,"Add to Fav");
                    }
                }
                if(is_exit){
                    vod_favlist.remove(fav_pos);
                    Toast.makeText(this,"This movie has been removed from favorites.",Toast.LENGTH_SHORT).show();
                } else{
                    vod_favlist.add(MyApp.vod_model);
                    Toast.makeText(this,"This movie has been added to favorites.",Toast.LENGTH_SHORT).show();
                }
                MyApp.instance.getPreference().put(Constants.MOVIE_FAV,vod_favlist);
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mHandler.removeCallbacks(mUpdateTimeTask);
        long totalDuration = surfaceView.getDuration();
        int currentPosition = Utils.progressToTimer(seekBar.getProgress(), totalDuration);
        surfaceView.seekTo(currentPosition);
        updateProgressBar();
    }


    private void updateTimer() {
        handler.removeCallbacks(mTicker);
        startTimer();
    }

    int maxTime;
    private void startTimer() {
        maxTime = 10;
        mTicker = new Runnable() {
            public void run() {
                if (maxTime < 1) {
                    if (bottom_lay.getVisibility() == View.VISIBLE)
                        bottom_lay.setVisibility(View.GONE);
                    return;
                }
                runNextTicker();
            }
        };
        mTicker.run();
    }

    private void runNextTicker() {
        maxTime --;
        long next = SystemClock.uptimeMillis() + 1000;
        handler.postAtTime(mTicker, next);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        try {
            long curr_pos = surfaceView.getCurrentPosition();
            long max_pos = surfaceView.getDuration();
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                        if (surfaceView.isPlaying()) {
                            surfaceView.pause();
                            img_play.setImageResource(R.drawable.exo_play);
                        } else {
                            surfaceView.start();
                            img_play.setImageResource(R.drawable.exo_pause);
                        }
                        if (bottom_lay.getVisibility() == View.GONE) bottom_lay.setVisibility(View.VISIBLE);
                        updateTimer();
                        break;
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        dration_time += 30;
                        if (curr_pos < dration_time * 1000)
                            surfaceView.seekTo(1);
                        else {
                            int st = (int) (curr_pos - (long) dration_time * 1000);
                            surfaceView.seekTo(st);
                        }
                        dration_time = 0;
                        updateProgressBar();
                        updateTimer();
                        if (bottom_lay.getVisibility() == View.GONE) bottom_lay.setVisibility(View.VISIBLE);
                        break;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        dration_time += 30;
                        if (max_pos < dration_time * 1000)
                            surfaceView.seekTo((int) (max_pos - 10));
                        else surfaceView.seekTo((int) (curr_pos + (long) dration_time * 1000));
                        dration_time = 0;
                        updateProgressBar();
                        updateTimer();
                        if (bottom_lay.getVisibility() == View.GONE) bottom_lay.setVisibility(View.VISIBLE);
                        break;
                    case KeyEvent.KEYCODE_BACK:
                        if(bottom_lay.getVisibility()==View.VISIBLE){
                            bottom_lay.setVisibility(View.GONE);
                            return true;
                        }
                        releaseMediaPlayer();
                        finish();
                        break;
                    case KeyEvent.KEYCODE_MENU:
                        for(int i = 0;i<vod_favlist.size();i++){
                            if(vod_favlist.get(i).getStream_id().equalsIgnoreCase(MyApp.vod_model.getStream_id())){
                                pkg_datas.set(0,"Remove from Fav");
                                is_exit = true;
                                fav_pos = i;
                                break;
                            }else {
                                pkg_datas.set(0,"Add to Fav");
                            }
                        }
                        PackageDlg packageDlg = new PackageDlg(VideoIjkPlayActivity.this, pkg_datas, new PackageDlg.DialogPackageListener() {
                            @Override
                            public void OnItemClick(Dialog dialog, int position) {
                                dialog.dismiss();
                                is_long = false;
                                switch (position) {
                                    case 0:
                                        if(is_exit)
                                            vod_favlist.remove(fav_pos);
                                        else
                                            vod_favlist.add(MyApp.vod_model);
                                        MyApp.instance.getPreference().put(Constants.MOVIE_FAV,vod_favlist);
                                        break;
                                    case 1:
                                        break;
                                    case 2:
                                        break;
                                    case 3:
                                        break;
                                }
                            }
                        });
                        packageDlg.show();
                        break;
                }
            }
        }catch (Exception e){

        }
        return super.dispatchKeyEvent(event);
    }

    public void FullScreencall() {
        if(Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else  {
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    @Override
    public void onCompletion(IMediaPlayer iMediaPlayer) {

    }

    @Override
    public boolean onError(IMediaPlayer iMediaPlayer, int i, int i1) {
        return false;
    }
}
