package com.m2team.myjourney.home;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.share.internal.ShareFeedContent;
import com.facebook.share.model.ShareContent;
import com.facebook.share.model.ShareMedia;
import com.facebook.share.model.ShareMediaContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.widget.ShareDialog;
import com.google.gson.Gson;
import com.m2team.myjourney.BaseActivity;
import com.m2team.myjourney.R;
import com.m2team.myjourney.home.recyclerview.DividerItemDecoration;
import com.m2team.myjourney.home.recyclerview.HomeAdapter;
import com.m2team.myjourney.home.recyclerview.ItemTouchHelperCallback;
import com.m2team.myjourney.home.recyclerview.ItemTouchHelperExtension;
import com.m2team.myjourney.login.LoginActivity;
import com.m2team.myjourney.model.ImageItem;
import com.m2team.myjourney.model.ImageItem_Table;
import com.m2team.myjourney.model.Journey;
import com.m2team.myjourney.model.Journey_Table;
import com.m2team.myjourney.model.MyDatabase;
import com.m2team.myjourney.model.User;
import com.m2team.myjourney.new_entry.NewEntryActivity;
import com.m2team.myjourney.settings.SettingActivity;
import com.m2team.myjourney.utils.Common;
import com.m2team.myjourney.utils.Constant;
import com.m2team.myjourney.utils.Log;
import com.m2team.myjourney.utils.SharePrefUtils;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.raizlabs.android.dbflow.config.DatabaseDefinition;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.structure.database.transaction.ProcessModelTransaction;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;
import jp.wasabeef.recyclerview.animators.OvershootInLeftAnimator;
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;
import jp.wasabeef.recyclerview.animators.SlideInRightAnimator;


public class HomeActivity extends BaseActivity implements HomeAdapter.OnItemClickListener {

    public static final int REQ_NEW_JOURNEY = Constant.REQ_NEW_JOURNEY;
    private ImageLoader imageLoader;

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    @BindView(R.id.avatar)
    CircleImageView avatarImageView;

    @BindView(R.id.tv_month)
    TextView tvMonth;

    @BindView(R.id.tv_year)
    TextView tvYear;

    @BindView(R.id.imageView_settings)
    ImageView imageView_settings;

    @BindView(R.id.imageView_change_view)
    ImageView imageView_change_view;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.appbar)
    AppBarLayout appBarLayout;

    private DateTime dateTime, currentDateTime;
    private HomeAdapter adapter;
    private BroadcastReceiver receiver;
    public ItemTouchHelperExtension mItemTouchHelper;
    public ItemTouchHelperExtension.Callback mCallback;
    private ShareDialog shareDialog;
    private int mPos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ButterKnife.bind(this);

        avatarImageView.setVisibility(View.GONE);
        toolbar.setTitle("");
        toolbar.setContentInsetsAbsolute(0, 0);
        toolbar.getContentInsetEnd();
        toolbar.setPadding(0, 0, 0, 0);
        setSupportActionBar(toolbar);

        shareDialog = new ShareDialog(this);

        initVariable();

        initUser();

        initNightMode();

        handleIntent(getIntent());

    }

    //-------------INIT DATA-----------------

    private void initNightMode() {
        boolean isNightMode = SharePrefUtils.getBooleanValue(this, Constant.IS_NIGHT_MODE, false);
        setThemeMode(isNightMode);
    }

    private void initVariable() {
        IntentFilter intentFilter = new IntentFilter(Constant.INTENT_FILTER_ACTION_REFRESH_DATA);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("receive intent " + intent);
                if (intent != null && Constant.INTENT_FILTER_ACTION_REFRESH_DATA.equals(intent.getAction())) {
                    Log.d("receive action " + intent.getAction());
                    adapter.reloadList();
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);

        isOpenDialog = false;
        imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration.createDefault(this));

        dateTime = DateTime.now();
        currentDateTime = DateTime.now();

        adapter = new HomeAdapter(this, dateTime);
        adapter.setType(0);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.scrollToPosition(dateTime.getDayOfMonth());

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new SlideInLeftAnimator());
        initSwipe();

        DateTimeFormatter formatter = DateTimeFormat.forPattern("MMM");
        tvMonth.setText(dateTime.toString(formatter));
        tvYear.setText(String.valueOf(dateTime.getYear()));
    }

    private void initSwipe() {
        recyclerView.addItemDecoration(new DividerItemDecoration(this));
        mCallback = new ItemTouchHelperCallback();
        mItemTouchHelper = new ItemTouchHelperExtension(mCallback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void initUser() {
        String data = SharePrefUtils.getStringValue(this, Constant.USER_KEY);
        Log.d("user data = " + data);

        if (!TextUtils.isEmpty(data)) {
            Gson gson = new Gson();
            User user = gson.fromJson(data, User.class);
            setUserToDrawer(user);
        } else {
            setThemeModeAvatar();
        }
    }

    private void setUserToDrawer(User user) {
        String id = user.getUserId();
        String name = user.getName();
        String avatarURI = user.getAvatarURI();

        Log.d("user= " + name + " " + id + " " + avatarURI);
        if (!TextUtils.isEmpty(avatarURI)) {
            imageLoader.displayImage(avatarURI, avatarImageView);
        } else {
            setThemeModeAvatar();
        }
    }

    //-------------SEARCH-----------------

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        toolbar.setBackgroundColor(ContextCompat.getColor(HomeActivity.this, R.color.night_top_bar));
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        adapter.setType(0);
                        adapter.reloadList();
                        boolean isNightMode = SharePrefUtils.getBooleanValue(getApplicationContext(), Constant.IS_NIGHT_MODE, false);
                        if (!isNightMode)
                            toolbar.setBackgroundColor(ContextCompat.getColor(HomeActivity.this, R.color.day_top_bar));
                        return true;
                    }
                }
        );
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    private void handleIntent(Intent intent) {
        Log.d("handleIntent " + intent.getAction());

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {

            adapter.setType(1);
            String keyword = intent.getStringExtra(SearchManager.QUERY);
            if (TextUtils.isEmpty(keyword)) return;
            keyword = keyword.toLowerCase();
            if (keyword.length() < 3) {
                Toast.makeText(this, R.string.search_invalid, Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d("keyword = " + keyword);
            //use the query to search your data somehow
            List<Journey> journeyList = new Select().from(Journey.class)
                    .where(Journey_Table.content.like("%" + keyword + "%"))
                    .or(Journey_Table.title.like("%" + keyword + "%"))
                    //        .groupBy(Journey_Table.id)
                    .limit(Constant.MAX_QUERY_RESULT)
                    .orderBy(Journey_Table.dateTime, false)
                    .queryList();

            Log.d("Query result " + journeyList.size());
            adapter.setJourneyList(journeyList);
        }
    }

    //------------THEME MODE-------------------

    private void setThemeMode(boolean isNightMode) {
        if (isNightMode) {
            //avatarImageView.setImageResource(R.drawable.ic_account_circle_white_24dp);
            imageView_settings.setImageResource(R.drawable.ic_settings_white_24dp);
            imageView_change_view.setImageResource(R.mipmap.ic_day);
            toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.night_top_bar));
            recyclerView.setBackgroundColor(ContextCompat.getColor(this, R.color.night_top_bar));
            tvMonth.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_bg_day));
            tvMonth.setTextColor(ContextCompat.getColor(this, R.color.day_text_content));
            tvYear.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_bg_day));
            tvYear.setTextColor(ContextCompat.getColor(this, R.color.day_text_content));
        } else {
            //avatarImageView.setImageResource(R.drawable.ic_account_circle_black_24dp);
            imageView_settings.setImageResource(R.drawable.ic_settings_black_24dp);
            imageView_change_view.setImageResource(R.mipmap.ic_night);
            toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.day_top_bar));
            recyclerView.setBackgroundColor(ContextCompat.getColor(this, R.color.day_top_bar));
            tvMonth.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_bg_night));
            tvMonth.setTextColor(ContextCompat.getColor(this, R.color.night_text_content));
            tvYear.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_bg_night));
            tvYear.setTextColor(ContextCompat.getColor(this, R.color.night_text_content));
        }
    }

    private void setThemeModeAvatar() {
        boolean isNightMode = SharePrefUtils.getBooleanValue(this, Constant.IS_NIGHT_MODE, false);

        if (isNightMode)
            avatarImageView.setImageResource(R.drawable.ic_account_circle_white_24dp);
        else
            avatarImageView.setImageResource(R.drawable.ic_account_circle_black_24dp);
    }

    @OnClick(R.id.imageView_change_view)
    public void changeMode() {
        boolean isNightMode = SharePrefUtils.getBooleanValue(this, Constant.IS_NIGHT_MODE, false);
        isNightMode = !isNightMode;
        SharePrefUtils.putBooleanValue(this, Constant.IS_NIGHT_MODE, isNightMode);

        setThemeMode(isNightMode);

        adapter.changeNightMode();

    }


    //-----------OVERRIDE METHOD------------------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("HomeActitivy onActivityResult requestCode = " + requestCode + " result " + resultCode);
        if (requestCode == REQ_NEW_JOURNEY) {
            if (resultCode == RESULT_OK) {
                Log.d("Reload list " + mPos);
                adapter.queryJouney();
                recyclerView.setItemAnimator(new SlideInRightAnimator());
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyItemChanged(mPos);

                    }
                }, 2500);

            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("onResume Home Activity");
        currentDateTime = DateTime.now();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            Log.d("stop receiver");
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        }
    }

    //-----------POPUP DATE----------------------

    private PopupWindow popupWindowMonth;

    @OnClick(R.id.tv_month)
    public void changeMonth(View view) {

        ScrollView scrollView = (ScrollView) LayoutInflater.from(this).inflate(R.layout.popup_month, null);
        LinearLayout monthView = (LinearLayout) scrollView.findViewById(R.id.layout_month);

        monthView.removeAllViews();

        DateTimeFormatter formatter = DateTimeFormat.forPattern("MMM");
        boolean isNightMode = SharePrefUtils.getBooleanValue(this, Constant.IS_NIGHT_MODE, false);

        int numOfMonth = 12;
        int year = dateTime.getYear();
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        int currentYear = calendar.get(Calendar.YEAR);
        if (year == currentYear) {
            numOfMonth = DateTime.now().getMonthOfYear();
        }
        for (int i = 1; i <= numOfMonth; i++) {
            View v = LayoutInflater.from(this).inflate(R.layout.item_popup_month, null, false);
            TextView tv = (TextView) v.findViewById(R.id.tv_month);
            tv.setText(dateTime.withMonthOfYear(i).toString(formatter));
            if (!isNightMode) {
                tv.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_bg_night));
                tv.setTextColor(ContextCompat.getColor(this, R.color.night_text_content));
            } else {
                tv.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_bg_day));
                tv.setTextColor(ContextCompat.getColor(this, R.color.day_text_content));
            }

            tv.setTag(i);

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    TextView textView = (TextView) view.findViewById(R.id.tv_month);
                    tvMonth.setText(textView.getText());

                    dateTime = dateTime.withMonthOfYear((Integer) textView.getTag());
                    adapter.setDateTime(dateTime);
                    popupWindowMonth.dismiss();
                }
            });
            monthView.addView(v);
        }
        Rect rect = locateView(view);
        popupWindowMonth = new PopupWindow(scrollView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        popupWindowMonth.setOutsideTouchable(true);
        popupWindowMonth.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindowMonth.showAsDropDown(view);
        popupWindowMonth.showAtLocation(view, Gravity.NO_GRAVITY, rect.left, rect.bottom);
    }

    private PopupWindow popupWindowYear = null;

    @OnClick(R.id.tv_year)
    public void changeYear(View view) {

        ScrollView layoutYear = (ScrollView) LayoutInflater.from(this).inflate(R.layout.popup_year, null);

        LinearLayout yearView = (LinearLayout) layoutYear.findViewById(R.id.layout_year);

        yearView.removeAllViews();
        boolean isNightMode = SharePrefUtils.getBooleanValue(this, Constant.IS_NIGHT_MODE, false);
        for (int i = Common.START_YEAR; i <= currentDateTime.getYear(); i++) {
            View v = LayoutInflater.from(this).inflate(R.layout.item_popup_year, null, false);
            TextView tv = (TextView) v.findViewById(R.id.tv_year_1);
            tv.setText(String.valueOf(i));
            if (!isNightMode) {
                tv.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_bg_night));
                tv.setTextColor(ContextCompat.getColor(this, R.color.night_text_content));
            } else {
                tv.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_bg_day));
                tv.setTextColor(ContextCompat.getColor(this, R.color.day_text_content));
            }
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    TextView textView = (TextView) view.findViewById(R.id.tv_year_1);
                    tvYear.setText(textView.getText());

                    dateTime = dateTime.withYear(Integer.parseInt(textView.getText().toString()));
                    adapter.setDateTime(dateTime);

                    popupWindowYear.dismiss();
                }
            });
            yearView.addView(v);
        }
        Rect rect = locateView(view);
        popupWindowYear = new PopupWindow(layoutYear, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        popupWindowYear.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindowYear.setOutsideTouchable(true);
        popupWindowYear.showAsDropDown(view);
        popupWindowYear.showAtLocation(view, Gravity.NO_GRAVITY, rect.left, rect.bottom);
    }

    //---------------ONCLICK----------------------

    @OnClick(R.id.imageView_settings)
    public void openSettings() {
        startActivity(new Intent(this, SettingActivity.class));
    }

    public Rect locateView(View v) {
        int[] loc_int = new int[2];
        if (v == null) return null;
        try {
            v.getLocationOnScreen(loc_int);
        } catch (NullPointerException npe) {
            //Happens when the view doesn't exist on screen anymore.
            return null;
        }
        Rect location = new Rect();
        location.left = loc_int[0];
        location.top = loc_int[1];
        location.right = location.left + v.getWidth();
        location.bottom = location.top + v.getHeight();
        return location;
    }

    @Override
    public void onItemClick(int id, DateTime dateTime, int pos) {
        mPos = pos;
        startActivityForResult(NewEntryActivity.createIntent(this, id, dateTime), REQ_NEW_JOURNEY);
    }

    @Override
    public void onShareFB(int id) {
        shareFacebook(id);
    }

    @Override
    public void onDeleteItem(final int pos) {
        recyclerView.setItemAnimator(new OvershootInLeftAnimator());
        adapter.queryJouney();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                adapter.notifyItemChanged(pos);
                recyclerView.setItemAnimator(new SlideInLeftAnimator());
            }
        }, 1500);

    }

    private View getViewInRecycleView(int pos) {
        RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(pos);
        if (viewHolder instanceof HomeAdapter.ViewHolder) {

            HomeAdapter.ViewHolder holder = (HomeAdapter.ViewHolder) viewHolder;
            return holder.parent_layout;
        }
        return null;

    }

    @OnClick(R.id.avatar)
    public void login() {
        startActivity(new Intent(this, LoginActivity.class));
    }

    public static Intent createIntent(Context context) {
        Intent intent = new Intent(context, HomeActivity.class);
        return intent;
    }

    //------------------SHARE------------------------
    public void shareFacebook(int journey) {
        List<ImageItem> images = new Select().from(ImageItem.class)
                .where(ImageItem_Table.journey_id.eq(journey))
                .queryList();


        List<ShareMedia> photos = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            File f = new File(images.get(i).getPath());
            if (f.exists()) {
                SharePhoto sharePhoto = new SharePhoto.Builder()
                        .setImageUrl(Uri.fromFile(f)).build();
                photos.add(sharePhoto);

            }
        }

        ShareContent shareContent = new ShareMediaContent.Builder()
                .addMedia(photos)
                .build();

        if (shareDialog.canShow(shareContent)) {
            shareDialog.show(shareContent);
        }
    }

}
