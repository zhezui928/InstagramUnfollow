package com.it_tech613.zhe.instagramunfollow.activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.annotation.ColorRes;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.it_tech613.zhe.instagramunfollow.utils.ConfirmExitDlg;
import com.it_tech613.zhe.instagramunfollow.utils.DelayedProgressDialog;
import com.it_tech613.zhe.instagramunfollow.utils.LoadingDlg;
import com.it_tech613.zhe.instagramunfollow.utils.PreferenceManager;
import com.it_tech613.zhe.instagramunfollow.R;
import com.it_tech613.zhe.instagramunfollow.fragment.MyAccountFragment;
import com.it_tech613.zhe.instagramunfollow.fragment.RewardsFragment;
import com.it_tech613.zhe.instagramunfollow.fragment.ShareFragment;
import com.it_tech613.zhe.instagramunfollow.fragment.UnfollowFragment;
import com.it_tech613.zhe.instagramunfollow.fragment.WhiteListFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;
import dev.niekirk.com.instagram4android.Instagram4Android;
import dev.niekirk.com.instagram4android.requests.InstagramGetUserFollowersRequest;
import dev.niekirk.com.instagram4android.requests.InstagramGetUserFollowingRequest;
import dev.niekirk.com.instagram4android.requests.InstagramUserFeedRequest;
import dev.niekirk.com.instagram4android.requests.payload.InstagramFeedItem;
import dev.niekirk.com.instagram4android.requests.payload.InstagramFeedResult;
import dev.niekirk.com.instagram4android.requests.payload.InstagramGetUserFollowersResult;
import dev.niekirk.com.instagram4android.requests.payload.InstagramLoginResult;
import dev.niekirk.com.instagram4android.requests.payload.InstagramUserSummary;

public class NavigationActivity extends AppCompatActivity implements View.OnFocusChangeListener {
    private ConfirmExitDlg confirmExitDlg;
    private List<AHBottomNavigationItem> items=new ArrayList<AHBottomNavigationItem>();

    AHBottomNavigation bottomNavigation;
    boolean notificationVisible=false;
//    NoSwipePager viewPager;
//    BottomBarAdapter pagerAdapter;

    DelayedProgressDialog spinner = new DelayedProgressDialog();
    LoadingDlg loadingDlg;
    CircleImageView userProfileImage;
    private InterstitialAd mInterstitialAd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
//        setSupportActionBar(toolbar);
//        //noinspection ConstantConditions
//        getSupportActionBar().setTitle("Bottom Navigation");
        userProfileImage =findViewById(R.id.profile);
        if (savedInstanceState != null)
            login(savedInstanceState.getString("username"),
                    savedInstanceState.getString("password"));
        else if (PreferenceManager.isSaved())
            login(PreferenceManager.getUserName(),
                    PreferenceManager.getPassword());
        else
            startLoginActivity();
    }

    private void loadInterstitialAd() {
        mInterstitialAd = new InterstitialAd(this);
        //TODO Interstitial ID
        mInterstitialAd.setAdUnitId("ca-app-pub-7166764673125229/4811981390");
        mInterstitialAd.setAdListener(new AdListener() {

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                if(mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                }
            }

            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                Toast.makeText(NavigationActivity.this, "onAdFailedToLoad()", Toast.LENGTH_SHORT).show();
            }
        });

        AdRequest adRequest = new AdRequest.Builder().build();
        mInterstitialAd.loadAd(adRequest);
    }
    private void setUpGUI() {
//        setupViewPager();
        bottomNavigation=(AHBottomNavigation) findViewById(R.id.bottom_navigation);
        setupBottomNavBehaviors();
        setupBottomNavStyle();

//        createFakeNotification();
        bottomNavigation.removeAllItems();
        addBottomNavigationItems();
        bottomNavigation.addItems(items);

        // Setting the very 1st item as home screen.

        bottomNavigation.setOnTabSelectedListener(new AHBottomNavigation.OnTabSelectedListener() {
            @Override
            public boolean onTabSelected(int position, boolean wasSelected) {
                Fragment selectedFragment;
                bottomNavigation.removeAllItems();
                addBottomNavigationItems();
                ImageView refresh=findViewById(R.id.refresh);
                switch (position){
                    case 0:
                        selectedFragment=UnfollowFragment.newInstance();
                        items.remove(position);
                        items.add(position,new AHBottomNavigationItem(getString(R.string.bottomnav_title_0), R.drawable.menuicon_selected, R.color.colorPrimary));
                        break;
                    case 1:
                        selectedFragment=WhiteListFragment.newInstance();
                        items.remove(position);
                        items.add(position,new AHBottomNavigationItem(getString(R.string.bottomnav_title_1), R.drawable.add_contact_selected, R.color.colorPrimary));
                        break;
                    case 2:
                        selectedFragment=ShareFragment.newInstance();
                        refresh.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                spinner.show(getSupportFragmentManager(), "login");
                                loadData_following();
                            }
                        });
                        items.remove(position);
                        items.add(position,new AHBottomNavigationItem(getString(R.string.bottomnav_title_2), R.drawable.share_selected, R.color.colorPrimary));
                        break;
                    case 3:
                        selectedFragment=RewardsFragment.newInstance();
                        items.remove(position);
                        items.add(position,new AHBottomNavigationItem(getString(R.string.bottomnav_title_3), R.drawable.badge_selected, R.color.colorPrimary));
                        break;
                    case 4:
                        selectedFragment=MyAccountFragment.newInstance();
                        refresh.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                spinner.show(getSupportFragmentManager(), "login");
                                loadData_following();
                            }
                        });
                        items.remove(position);
                        items.add(position,new AHBottomNavigationItem(getString(R.string.bottomnav_title_4), R.drawable.profile_selected, R.color.colorPrimary));
                        break;
                    default:
                        selectedFragment=UnfollowFragment.newInstance();
                        items.remove(position);
                        items.add(position,new AHBottomNavigationItem(getString(R.string.bottomnav_title_0), R.drawable.menuicon_selected, R.color.colorPrimary));
                        break;
                }
                bottomNavigation.addItems(items);
                getSupportFragmentManager().beginTransaction().replace(R.id.frame,selectedFragment).addToBackStack("tag").commitAllowingStateLoss();
//                viewPager.setCurrentItem(position);
                Log.e("selected_tab",position+"");
                // remove notification badge
//                int lastItemPos = bottomNavigation.getItemsCount() - 1;
//                if (notificationVisible && position == lastItemPos)
//                    bottomNavigation.setNotification(new AHNotification(), lastItemPos);

                return true;
            }
        });
        bottomNavigation.setCurrentItem(4);
        spinner.cancel();
    }

    public void startLoginActivity() {
        startActivityForResult(new Intent(this, LoginActivity.class), 0);
    }

    @SuppressLint("StaticFieldLeak")
    private void login(final String username, final String password) {
        loadInterstitialAd();
        loadingDlg=new LoadingDlg(this);
        loadingDlg.show();
        loadingDlg.setCancelable(false);
//        spinner.show(getSupportFragmentManager(), "login");

        PreferenceManager.instagram = Instagram4Android.builder()
                .username(username)
                .password(password)
                .build();
        PreferenceManager.instagram.setup();
        new AsyncTask<Void, Void, Void>() {
            InstagramLoginResult loginResult;
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    loginResult = PreferenceManager.instagram.login();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.e("loginresult",loginResult.toString());
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (!PreferenceManager.instagram.isLoggedIn()) {
//                    spinner.cancel();
                    if (loadingDlg!=null && loadingDlg.isShowing()) loadingDlg.dismiss();
                    Toast.makeText(getApplicationContext(), R.string.loginError, Toast.LENGTH_LONG).show();
                    startLoginActivity();
                } else {
                    PreferenceManager.currentUser=loginResult.getLogged_in_user();
                    RequestOptions requestOptions = new RequestOptions();
                    requestOptions.placeholder(R.drawable.profile);
                    requestOptions.error(R.drawable.profile);
                    if (NavigationActivity.this.isDestroyed()) return;
                    Glide.with(NavigationActivity.this)
                            .load(PreferenceManager.currentUser.getProfile_pic_url())
                            .into(userProfileImage);
                    loadData_following();
//                    tvUsername.setText(username);
                    if (!PreferenceManager.isSaved()) {
                        PreferenceManager.setIsSaved(true);
                        PreferenceManager.setUserName(username);
                        PreferenceManager.setPassword(password);
                        PreferenceManager.checkLimit();
                    }
                }
            }
        }.execute();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frame);
        if(fragment == null) {
            confirmExitDlg =new ConfirmExitDlg(NavigationActivity.this, new ConfirmExitDlg.DialogNumberListener() {
                @Override
                public void OnYesClick(Dialog dialog) {
                    confirmExitDlg.dismiss();
                    finish();
                    System.exit(0);
                }

                @Override
                public void OnCancelClick(Dialog dialog) {
                    confirmExitDlg.dismiss();
                }
            });
            confirmExitDlg.show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.setLastLogin();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (PreferenceManager.instagram != null && PreferenceManager.instagram.isLoggedIn()) {
            outState.putString("username", PreferenceManager.instagram.getUsername());
            outState.putString("password", PreferenceManager.instagram.getPassword());
        }
        super.onSaveInstanceState(outState);
    }

    @SuppressLint("StaticFieldLeak")
    void loadData_following() {
        new AsyncTask<Void, Void, Void>() {
            ArrayList<InstagramUserSummary> following = new ArrayList<>();
            @Override
            protected Void doInBackground(Void... voids) {
                InstagramGetUserFollowersResult result;
                final long userId = PreferenceManager.instagram.getUserId();
                try {
                    result = PreferenceManager.instagram.sendRequest(new InstagramGetUserFollowingRequest(userId));
                    following.addAll(result.getUsers());
                    while (result.getNext_max_id() != null){
                        result = PreferenceManager.instagram.sendRequest(new InstagramGetUserFollowingRequest(userId, result.getNext_max_id()));
                        following.addAll(result.getUsers());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                PreferenceManager.following=following;
                loadData_follower();
            }
        }.execute();
    }

    @SuppressLint("StaticFieldLeak")
    void loadData_follower() {
        new AsyncTask<Void, Void, Void>() {
            ArrayList<InstagramUserSummary> followers = new ArrayList<>();
            @Override
            protected Void doInBackground(Void... voids) {
                InstagramGetUserFollowersResult result;
                final long userId = PreferenceManager.instagram.getUserId();
                try {
                    result = PreferenceManager.instagram.sendRequest(new InstagramGetUserFollowersRequest(userId));
                    followers.addAll(result.getUsers());
                    while (result.getNext_max_id() != null){
                        result = PreferenceManager.instagram.sendRequest(new InstagramGetUserFollowersRequest(userId, result.getNext_max_id()));
                        followers.addAll(result.getUsers());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                PreferenceManager.followers=followers;
                ArrayList<InstagramUserSummary> unfollowers = new ArrayList<>(PreferenceManager.following);
                for (InstagramUserSummary i : PreferenceManager.following) {
                    Set<String> whitelist=PreferenceManager.getWhitelist_ids();
                    ArrayList<String> whitelist_ids=new ArrayList<>(whitelist);
                    for (String id:whitelist_ids){
                        if (Long.valueOf(id)==i.getPk()) {
                            PreferenceManager.whitelist.add(i);
                            unfollowers.remove(i);
                            break;
                        }
                    }
                    for (InstagramUserSummary j : followers) {
                        if (i.equals(j)) {
                            unfollowers.remove(i);
                            break;
                        }
                    }
                }
                PreferenceManager.unfollowers=unfollowers;
                loadData_post();
            }
        }.execute();
    }

    @SuppressLint("StaticFieldLeak")
    void loadData_post() {
        new AsyncTask<Void, Void, Void>() {
            ArrayList<InstagramFeedItem> feedItems = new ArrayList<>();
            @Override
            protected Void doInBackground(Void... voids) {
                InstagramFeedResult posts;
                final long userId = PreferenceManager.instagram.getUserId();
                try {
                    posts = PreferenceManager.instagram.sendRequest(new InstagramUserFeedRequest());
                    feedItems.addAll(posts.getItems());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                PreferenceManager.feedItems=feedItems;
                Log.e("feed_number",String.valueOf(feedItems.size()));
//                spinner.cancel();
                if (loadingDlg!=null && loadingDlg.isShowing()) loadingDlg.dismiss();
                setUpGUI();
            }
        }.execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK)
            startLoginActivity();
        else
            login(data.getStringExtra("username"),
                    data.getStringExtra("password"));
    }

    private int fetchColor(@ColorRes int color) {
        return ContextCompat.getColor(this, color);
    }

//    private void setupViewPager() {
//        viewPager = (NoSwipePager) findViewById(R.id.viewpager);
//        viewPager.setPagingEnabled(false);
//        pagerAdapter = new BottomBarAdapter(getSupportFragmentManager());
//
//        pagerAdapter.addFragments(UnfollowFragment.newInstance());
//        pagerAdapter.addFragments(WhiteListFragment.newInstance());
//        pagerAdapter.addFragments(ShareFragment.newInstance());
//        pagerAdapter.addFragments(RewardsFragment.newInstance());
//        pagerAdapter.addFragments(MyAccountFragment.newInstance());
//
//        viewPager.setAdapter(pagerAdapter);
//    }

//    private void createFakeNotification() {
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                AHNotification notification = new AHNotification.Builder()
//                        .setText("1")
//                        .setBackgroundColor(Color.YELLOW)
//                        .setTextColor(Color.BLACK)
//                        .build();
//                // Adding notification to last item.
//                bottomNavigation.setNotification(notification, bottomNavigation.getItemsCount() - 1);
//                notificationVisible = true;
//            }
//        }, 1000);
//    }

    private void addBottomNavigationItems() {
        items=new ArrayList<>();
        items.add(new AHBottomNavigationItem(getString(R.string.bottomnav_title_0), R.drawable.menuicon, R.color.colorPrimary));
        items.add(new AHBottomNavigationItem(getString(R.string.bottomnav_title_1), R.drawable.add_contact, R.color.colorPrimary));
        items.add(new AHBottomNavigationItem(getString(R.string.bottomnav_title_2), R.drawable.share, R.color.colorPrimary));
        items.add(new AHBottomNavigationItem(getString(R.string.bottomnav_title_3), R.drawable.badge ,R.color.colorPrimary));
        items.add(new AHBottomNavigationItem(getString(R.string.bottomnav_title_4), R.drawable.profile,R.color.colorPrimary));
    }

    public static Drawable drawableFromUrl(String url_str) throws IOException {
        Bitmap x;
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
//        URL url = new URL(url_str);
//        x=BitmapFactory.decodeStream((InputStream)url.getContent());
        HttpURLConnection connection = (HttpURLConnection) new URL(url_str).openConnection();
        connection.connect();
        InputStream input = connection.getInputStream();

        x = BitmapFactory.decodeStream(input);
        return new BitmapDrawable(x);
    }

    public void setupBottomNavBehaviors() {
        bottomNavigation.setBehaviorTranslationEnabled(true);
        /*
        Before enabling this. Change MainActivity theme to MyTheme.TranslucentNavigation in
        AndroidManifest.
        Warning: Toolbar Clipping might occur. Solve this by wrapping it in a LinearLayout with a top
        View of 24dp (status bar size) height.
         */
//        bottomNavigation.setTranslucentNavigationEnabled(false);
    }

    /**
     * Adds styling properties to {@link AHBottomNavigation}
     */
    private void setupBottomNavStyle() {
        /*
        Set Bottom Navigation colors. Accent color for active item,
        Inactive color when its view is disabled.
        Will not be visible if setColored(true) and default current item is set.
         */
        bottomNavigation.setDefaultBackgroundColor(fetchColor(R.color.colorPrimary));
        bottomNavigation.setAccentColor(Color.WHITE);
        bottomNavigation.setSelectedBackgroundVisible(false);
        bottomNavigation.setInactiveColor(fetchColor(R.color.colorBackground));

//        // Colors for selected (active) and non-selected items.
//        bottomNavigation.setColoredModeColors(Color.WHITE,
//                fetchColor(R.color.grey));
//
//        //  Enables Reveal effect
//        bottomNavigation.setColored(true);

        //  Displays item Title always (for selected and non-selected items)
        bottomNavigation.setTitleState(AHBottomNavigation.TitleState.ALWAYS_HIDE);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (v.getId()==R.id.frame) findViewById(R.id.unfollow_btn_group).setVisibility(View.GONE);
    }
}
