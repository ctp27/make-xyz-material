package com.example.xyzreader.ui;


import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String CURSOR_COUNT_EXTRA="cursor-count-extra";
    private static final String TAG = ArticleDetailActivity.class.getSimpleName();
    private static final String BUNDLE_IS_FIRST_TIME_KEY = "is-first-time" ;

    private long mStartId;

    private long mSelectedItemId;
    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;
    private int mTopInset;

    private int cursorCount = 0;


    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
    boolean isFirstTime;

    private int scrollY;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_article_detail);

        getSupportLoaderManager().initLoader(26, null, this);

        if (getIntent() != null && getIntent().getData() != null) {
            mStartId = ItemsContract.Items.getItemId(getIntent().getData());
            mSelectedItemId = mStartId;
            cursorCount = getIntent().getIntExtra(CURSOR_COUNT_EXTRA,0);
        }

        if (savedInstanceState == null) {
            isFirstTime = true;
        }else {
            isFirstTime = savedInstanceState.getBoolean(BUNDLE_IS_FIRST_TIME_KEY);
        }

        mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager(), null);
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        Log.d(TAG, "Called");

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        if(i==26) {
            return ArticleLoader.newAllArticlesInstance(this);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, final Cursor cursor) {

        if(cursorLoader.getId()==26) {

            mPagerAdapter.swapCursor(cursor);
            if(isFirstTime) {
                mPager.setCurrentItem((int) mStartId, false);
                isFirstTime = false;
            }
            mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
//                    mStartId = mPager.getCurrentItem();
                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });
        }
//        mPager.setOffscreenPageLimit(cursor.getCount());

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();
//        if(mPager!=null) {
//            mPager.setCurrentItem((int) mStartId, false);
//        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mPagerAdapter.swapCursor(null);
    }


    private class MyPagerAdapter extends FragmentStatePagerAdapter {


        private Cursor adapterCursor;

        public MyPagerAdapter(FragmentManager fm,Cursor adapterCursor) {
            super(fm);
            this.adapterCursor = adapterCursor;
        }


        @Override
        public Fragment getItem(int position) {

            adapterCursor.moveToPosition(position);

            ArticleDetailFragment fragmentNew = new ArticleDetailFragment();
            Bundle bundle = new Bundle();
            bundle.putInt(ArticleDetailFragment.BUNDLE_ARTICLE_ID,Integer.parseInt(adapterCursor.getString(ArticleLoader.Query._ID)));
            bundle.putString("author",adapterCursor.getString(ArticleLoader.Query.AUTHOR));
            bundle.putString("date",adapterCursor.getString(ArticleLoader.Query.PUBLISHED_DATE));
            bundle.putString("title",adapterCursor.getString(ArticleLoader.Query.TITLE));
            bundle.putString("body",adapterCursor.getString(ArticleLoader.Query.BODY).substring(0,3000));
            bundle.putString("image",adapterCursor.getString(ArticleLoader.Query.PHOTO_URL));
            fragmentNew.setArguments(bundle);
            return fragmentNew;
        }

        @Override
        public int getCount() {
            if(adapterCursor==null)
                return 0;
            return adapterCursor.getCount();
        }


        public void swapCursor(Cursor newCursor){

            if(adapterCursor!=null){
                adapterCursor=null;
            }
            adapterCursor = newCursor;
            notifyDataSetChanged();
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(BUNDLE_IS_FIRST_TIME_KEY,isFirstTime);
    }
}
