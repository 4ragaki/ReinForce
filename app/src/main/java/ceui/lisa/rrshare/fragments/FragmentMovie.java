package ceui.lisa.rrshare.fragments;

import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;

import com.scwang.smart.refresh.header.FalsifyFooter;
import com.scwang.smart.refresh.header.MaterialHeader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;

import ceui.lisa.rrshare.BaseFragment;
import ceui.lisa.rrshare.ItemView;
import ceui.lisa.rrshare.R;
import ceui.lisa.rrshare.TabItem;
import ceui.lisa.rrshare.databinding.FragmentMineBinding;
import ceui.lisa.rrshare.databinding.FragmentMovieBinding;
import ceui.lisa.rrshare.network.Net;
import ceui.lisa.rrshare.network.NullCtrl;
import ceui.lisa.rrshare.response.Page;
import ceui.lisa.rrshare.response.Section;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import rxhttp.RxHttp;

public class FragmentMovie extends BaseFragment<FragmentMovieBinding> {

    @Override
    protected void initLayout() {
        mLayoutID = R.layout.fragment_movie;
    }

    @Override
    protected void initView() {
        baseBind.smartRefreshLayout.setEnableRefresh(true);
        baseBind.smartRefreshLayout.setEnableLoadMore(true);
        baseBind.smartRefreshLayout.setRefreshHeader(new MaterialHeader(mContext));
        baseBind.smartRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                fetch();
            }
        });
        baseBind.smartRefreshLayout.setRefreshFooter(new FalsifyFooter(mContext));
        baseBind.smartRefreshLayout.autoRefresh();
    }

    private void fetch() {
        RxHttp.get("https://api.rr.tv/v3plus/index/channel")
                .addAllHeader(Net.header())
                .add("position", "CHANNEL_MOVIE")
                .asClass(Page.class)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new NullCtrl<Page>() {
                    @Override
                    public void success(Page rankResponse) {
                        baseBind.viewPager.setAdapter(new FragmentPagerAdapter(getChildFragmentManager(), 0) {
                            @NonNull
                            @Override
                            public Fragment getItem(int position) {
                                return FragmentBanner.newInstance(
                                        rankResponse.getData().getBannerTop().get(position)
                                );
                            }

                            @Override
                            public int getCount() {
                                return rankResponse.getData().getBannerTop().size();
                            }

                            @Override
                            public long getItemId(int position) {
                                return rankResponse.getData().getBannerTop().get(position).getId();
                            }
                        });
                        baseBind.bannerCard.setVisibility(View.VISIBLE);
                        baseBind.createLinear.removeAllViews();
                        for (Section section : rankResponse.getData().getSections()) {
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
                        baseBind.smartRefreshLayout.finishRefresh(true);
                    }
                });
    }
}
