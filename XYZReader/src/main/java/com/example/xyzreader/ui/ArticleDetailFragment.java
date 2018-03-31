package com.example.xyzreader.ui;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ItemsContract;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import butterknife.BindBool;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by clinton on 3/25/18.
 */

public class ArticleDetailFragment extends Fragment
    implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXPAND_TEXT_LOADER_ID = 22;

    private static final String SCROLL_X_BUNDLE_KEY="scroll-x";
    private static final String SCROLL_Y_BUNDLE_KEY="scroll-y";
    private static final java.lang.String BUNDLE_BODY_KEY = "body-key";

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    public static final String BUNDLE_ARTICLE_ID="article-id-extra";


    @BindView(R.id.article_byLine)
    TextView articleByLineView;

    @BindView(R.id.article_body)
    TextView articleBodyView;

    @Nullable
    @BindView(R.id.app_bar)
    Toolbar toolbar;

    @BindView(R.id.photo)
    ImageView imageView;

    @Nullable
    @BindView(R.id.collapsing_toolbar_layout)
    CollapsingToolbarLayout collapsingToolbarLayout;

    @BindView(R.id.expand_text)
    Button expandBtn;

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    @BindView(R.id.nested_scroller)
    NestedScrollView scrollView;

    @Nullable
    @BindView(R.id.back_nav)
    ImageView landBackNav;

    @Nullable
    @BindView(R.id.article_title)
    TextView landArticleTitle;

    @BindBool(R.bool.landscape)
    boolean isLandscape;

    @BindView(R.id.share_fab)
    FloatingActionButton floatingActionButton;

    private int thisId;
    private String body;
    private String titleToShare;

    private int scrollX=0;
    private int scrollY=0;

    public ArticleDetailFragment() {
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view =
                inflater.inflate(R.layout.fragment_article_detail, container, false);

        ButterKnife.bind(this,view);



        if(toolbar!=null) {
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    NavUtils.navigateUpFromSameTask(getActivity());
                }
            });
        }

        if(landBackNav!=null){
            landBackNav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    NavUtils.navigateUpFromSameTask(getActivity());
                }
            });
        }

        expandBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showProgressBar();
                getLoaderManager().restartLoader(EXPAND_TEXT_LOADER_ID,null,ArticleDetailFragment.this);
            }
        });

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                shareText();
            }
        });

        bindViews(getArguments(),savedInstanceState);
        return view;
    }


    private void bindViews(Bundle data, Bundle savedInstanceState){


        if(savedInstanceState!=null){
            body=savedInstanceState.getString(BUNDLE_BODY_KEY);
            scrollX = savedInstanceState.getInt(SCROLL_X_BUNDLE_KEY);
            scrollY = savedInstanceState.getInt(SCROLL_Y_BUNDLE_KEY);
        }


        if(data==null)
            return;

            String publishedDate = data.getString("date");
            String author = data.getString("author");
            String title = data.getString("title");
            titleToShare = title;

            if(landArticleTitle!=null){
                landArticleTitle.setText(title);
            }

            String image = data.getString("image");
            thisId = data.getInt(BUNDLE_ARTICLE_ID);

            if(TextUtils.isEmpty(body)) {
                body = data.getString("body");
            }

            setByLineString(publishedDate, author);
//            articleTitleView.setText(title);
            articleBodyView.setText(body);

        Picasso.get().load(image)
                .error(R.drawable.empty_detail)
                .placeholder(R.drawable.empty_detail)
                .into(imageView);

        if(collapsingToolbarLayout!=null) {
            collapsingToolbarLayout.setTitle(title);
        }
//        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(R.color.textColor));

            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.scrollTo(scrollX, scrollY);
                }
            });


    }


    private void setByLineString(String date,String author){

        Date thisDate = parsePublishedDate(date);
        if (!thisDate.before(START_OF_EPOCH.getTime())) {
           articleByLineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            thisDate.getTime(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + author
                            + "</font>"));

        } else {
            // If date is before 1902, just show the string
            articleByLineView.setText(Html.fromHtml(
                    outputFormat.format(thisDate) + " by <font color='#ffffff'>"
                            + author
                            + "</font>"));

        }

    }



    private Date parsePublishedDate(String date) {
        try {
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            return new Date();
        }
    }


    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {

        if (id == EXPAND_TEXT_LOADER_ID) {
            Uri uri =  ItemsContract.Items.buildItemUri(thisId);
            String projection[] = {ItemsContract.Items.BODY};
            return new CursorLoader(getContext(),uri,projection,null,null,null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if(data!=null) {
            data.moveToFirst();
            body = data.getString(0);
            articleBodyView.setText(body);
            hideProgressBar();
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }

    private void showProgressBar(){
        expandBtn.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar(){
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if(scrollView!=null) {
            outState.putInt(SCROLL_X_BUNDLE_KEY, scrollView.getScrollX());
            outState.putInt(SCROLL_Y_BUNDLE_KEY, scrollView.getScrollY());
        }
        outState.putString(BUNDLE_BODY_KEY,body);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(scrollView!=null) {
            scrollY = scrollView.getScrollY();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(scrollView!=null){
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                   scrollView.scrollTo(scrollX,scrollY);
                }
            });
        }
    }

    private void shareText(){

        ShareCompat.IntentBuilder
                .from(getActivity())
                .setType("text/plain")
                .setChooserTitle(titleToShare)
                .setText(titleToShare)
                .startChooser();
    }

}
