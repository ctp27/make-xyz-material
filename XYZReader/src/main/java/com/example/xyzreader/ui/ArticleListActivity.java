package com.example.xyzreader.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import butterknife.BindBool;
import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */

public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ArticleListActivity.class.toString();
    private static final String BUNDLE_SAVED_POSITION = "bundle-saved-position";
    private int savedClickedPosition=-1;
    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private Adapter adapter;

    @BindBool(R.bool.tabletPort)
    boolean isTablet;

    @BindBool(R.bool.landscape)
    boolean isLandscape;

    @BindView(R.id.empty_cursor_view)
    TextView emptyCursorView;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        ButterKnife.bind(this);

//        ((CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar_layout)).setTitle(getString(R.string.app_name));

        mToolbar = (Toolbar) findViewById(R.id.toolbar);


        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        adapter = new Adapter(null);
        adapter.setHasStableIds(true);

        if(isLandscape){
            GridLayoutManager manager = new GridLayoutManager(this,2);
            mRecyclerView.setLayoutManager(manager);
        }else {
            LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
            mRecyclerView.setLayoutManager(layoutManager);
        }


        mRecyclerView.setAdapter(adapter);



        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });


        if(isTablet && savedInstanceState ==null){

            DefaultTabFragment fragmentNew = new DefaultTabFragment();

            getSupportFragmentManager().beginTransaction().
                    add(R.id.article_detail_pane,fragmentNew)
                    .commit();
        }else {
            if(isTablet){
                Log.d(TAG,"set saved clicked position");
                savedClickedPosition = savedInstanceState.getInt(BUNDLE_SAVED_POSITION);
            }
        }

        getSupportLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }
    }



    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        hideEmptyCursorMessage();

        if(cursor==null || cursor.getCount()==0){
            showEmptyCursorMessage();
        }

        adapter.setClickedPosition(savedClickedPosition);
        adapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(isTablet){
            outState.putInt(BUNDLE_SAVED_POSITION,adapter.getClickedPosition());
        }
    }

    private void hideEmptyCursorMessage(){
        emptyCursorView.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showEmptyCursorMessage(){
        emptyCursorView.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.INVISIBLE);
    }

    public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
        private Cursor mCursor;

        private View lastView;
        private int clickedPosition;

        public Adapter(Cursor cursor) {
            clickedPosition = savedClickedPosition;
            mCursor = cursor;
        }


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);

            return vh;
        }


        private Date parsePublishedDate() {
            try {
                String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            if(isTablet && position ==clickedPosition){
            /*  If last view is null, this is the first time the view is being set */
                if(lastView==null) {
                    View v = holder.itemView;
                    v.setBackgroundColor(holder.highlightColor);
                    lastView = v;
                }
            }


            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.subtitleView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            } else {
                holder.subtitleView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate)
                        + "<br/>" + " by "
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            }

            Picasso.get().load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
                    .placeholder(R.drawable.empty_detail)
                    .error(R.drawable.empty_detail)
                    .into(holder.thumbnailView);
        }

        @Override
        public int getItemCount() {
            if(mCursor==null)
                return 0;
            return mCursor.getCount();
        }

        public void swapCursor(Cursor newCursor){
            if(mCursor!=null){
                mCursor=null;
            }
            mCursor = newCursor;
            notifyDataSetChanged();
        }

        public int getClickedPosition() {
            return clickedPosition;
        }

        public void setClickedPosition(int clickedPosition) {
            this.clickedPosition = clickedPosition;
        }

        public class ViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener{

            @BindView(R.id.thumbnail)
            ImageView thumbnailView;

            @BindView(R.id.article_title)
            TextView titleView;

            @BindView(R.id.article_subtitle)
            TextView subtitleView;

            @BindColor(R.color.defaultBg)
            int defaultColor;

            @BindColor(R.color.colorAccentHighlight)
            int highlightColor;

            public ViewHolder(View view) {
                super(view);
                ButterKnife.bind(this,view);
                view.setOnClickListener(this);

            }

            @Override
            public void onClick(View view) {
                int thisPosition = getAdapterPosition();
               mCursor.moveToPosition(thisPosition);

                if(isTablet){
                    clickedPosition = thisPosition;
                    if(lastView!=null) {
                        lastView.setBackgroundColor(defaultColor);
                    }
                    view.setBackgroundColor(highlightColor);
                    lastView = view;

                    ArticleDetailFragment fragmentNew = new ArticleDetailFragment();
                    Bundle bundle = new Bundle();
                    bundle.putInt(ArticleDetailFragment.BUNDLE_ARTICLE_ID,Integer.parseInt(mCursor.getString(ArticleLoader.Query._ID)));
                    bundle.putString("author",mCursor.getString(ArticleLoader.Query.AUTHOR));
                    bundle.putString("date",mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE));
                    bundle.putString("title",mCursor.getString(ArticleLoader.Query.TITLE));
                    bundle.putString("body",mCursor.getString(ArticleLoader.Query.BODY).substring(0,3000));
                    bundle.putString("image",mCursor.getString(ArticleLoader.Query.PHOTO_URL));
                    fragmentNew.setArguments(bundle);

                    getSupportFragmentManager().beginTransaction().replace(R.id.article_detail_pane,fragmentNew)
                            .commit();
                }
                else {
                    Uri uri = ItemsContract.Items.buildItemUri(thisPosition);
                    int count = mCursor.getCount();
                    Intent intent = new Intent(ArticleListActivity.this, ArticleDetailActivity.class);
                    intent.setData(uri);
                    intent.putExtra(ArticleDetailActivity.CURSOR_COUNT_EXTRA, count);
                    startActivity(intent);
                }
            }
        }
    }


}
