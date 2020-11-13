package ceui.lisa.rrshare.fragments;

import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;

import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.scwang.smart.refresh.header.MaterialHeader;
import com.scwang.smart.refresh.header.TwoLevelHeader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;

import java.util.ArrayList;
import java.util.List;

import ceui.lisa.rrshare.BaseFragment;
import ceui.lisa.rrshare.ItemView;
import ceui.lisa.rrshare.R;
import ceui.lisa.rrshare.TabItem;
import ceui.lisa.rrshare.databinding.FragmentMovieBinding;
import ceui.lisa.rrshare.databinding.FragmentRBinding;
import ceui.lisa.rrshare.network.Net;
import ceui.lisa.rrshare.network.NullCtrl;
import ceui.lisa.rrshare.response.Page;
import ceui.lisa.rrshare.response.Section;
import ceui.lisa.rrshare.utils.Common;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import rxhttp.RxHttp;

public class FragmentMovie extends BaseFragment<FragmentRBinding> {

    protected String type = "CHANNEL_MOVIE";
    protected Page mPage;
    protected int nowPage = 1;
    protected boolean isLoaded;

    @Override
    protected void initLayout() {
        mLayoutID = R.layout.fragment_r;
    }

    @Override
    protected void initView() {
        baseBind.smartRefreshLayout.setEnableRefresh(true);
        baseBind.smartRefreshLayout.setEnableLoadMore(true);
        baseBind.smartRefreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                showData(false);
            }
        });
        baseBind.smartRefreshLayout.setRefreshHeader(new MaterialHeader(mContext));
        baseBind.smartRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                fetch();
            }
        });
        baseBind.smartRefreshLayout.setRefreshFooter(new ClassicsFooter(mContext));
    }

    public void lazyData() {
        Common.showLog("setUserVisibleHint lazyData " + className);
        baseBind.smartRefreshLayout.autoRefresh();
        isLoaded = true;
    }

    @Override
    protected void initData() {
        if (forceLoad()) {
            lazyData();
        }
    }

    public boolean forceLoad() {
        return false;
    }

    public void fetch() {
        RxHttp.get("https://api.rr.tv/v3plus/index/channel")
                .addAllHeader(Net.header())
                .add("position", type)
                .asClass(Page.class)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new NullCtrl<Page>() {
                    @Override
                    public void success(Page page) {
                        mPage = page;
                        baseBind.viewPager.setAdapter(new FragmentPagerAdapter(getChildFragmentManager(), 0) {
                            @NonNull
                            @Override
                            public Fragment getItem(int position) {
                                return FragmentBanner.newInstance(
                                        mPage.getData().getBannerTop().get(position)
                                );
                            }

                            @Override
                            public int getCount() {
                                return mPage.getData().getBannerTop().size();
                            }

                            @Override
                            public long getItemId(int position) {
                                return mPage.getData().getBannerTop().get(position).getId();
                            }
                        });
                        baseBind.bannerCard.setVisibility(View.VISIBLE);
                        showData(true);
                    }
                });
    }

    public void showData(boolean refresh) {
        if (mPage == null) {
            return;
        }
        if (refresh) {
            nowPage = 1;
            if (baseBind.createLinear.getChildCount() != 0) {
                baseBind.createLinear.removeAllViews();
            }
        }
        List<Section> nextList = mPage.getPage(nowPage);
        for (Section section : nextList) {
            if (!"AD".equals(section.getSectionType())) {
                if ("TAB".equals(section.getDisplay())) {
                    TabItem itemView = new TabItem(mContext);
                    itemView.bindSection(section);
                    baseBind.createLinear.addView(itemView);
                } else {
                    ItemView itemView = new ItemView(mContext);
                    itemView.bindSection(section);
                    baseBind.createLinear.addView(itemView);
                }
            }
        }
        nowPage++;
        if (refresh) {
            baseBind.smartRefreshLayout.finishRefresh(true);
        } else {
            baseBind.smartRefreshLayout.finishLoadMore(true);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && !isLoaded && isAdded() && viewCreated) {
            lazyData();
        }
    }
}
