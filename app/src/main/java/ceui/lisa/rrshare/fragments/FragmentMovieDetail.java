package ceui.lisa.rrshare.fragments;

import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import ceui.lisa.rrshare.R;
import ceui.lisa.rrshare.BottomView;
import ceui.lisa.rrshare.TopView;
import ceui.lisa.rrshare.adapters.EpisodeAdapter;
import ceui.lisa.rrshare.databinding.FragmentMovieBinding;
import ceui.lisa.rrshare.network.Net;
import ceui.lisa.rrshare.response.Content;
import ceui.lisa.rrshare.response.Episode;
import ceui.lisa.rrshare.response.Movie;
import ceui.lisa.rrshare.response.Watch;
import ceui.lisa.rrshare.utils.Common;
import ceui.lisa.rrshare.utils.DensityUtil;
import ceui.lisa.rrshare.utils.LinearItemDecorationHorizon;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import rxhttp.RxHttp;

public class FragmentMovieDetail extends BaseMovieFragment<FragmentMovieBinding> {

    @Override
    protected void initLayout() {
        mLayoutID = R.layout.fragment_movie;
    }

    @Override
    protected void initView() {
        baseBind.recyList.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
        baseBind.recyList.addItemDecoration(
                new LinearItemDecorationHorizon(DensityUtil.dp2px(16.0f))
        );
    }

    @Override
    public void initModel() {
        super.initModel();
        model.getMovie().observe(getViewLifecycleOwner(), new Observer<Content>() {
            @Override
            public void onChanged(Content content) {
                if (content != null) {
                    Common.showLog("view model " + className + "收到了" + content.getTitle());
                    getEpisode(content.getId());
                    getDetail(content.getId());
                } else {
                    Common.showLog("view model " + className + "收到了 null");
                }
            }
        });
    }

    private void getEpisode(int seasonID) {
        RxHttp.get("https://api.rr.tv/rrtv-video/v4plus/season/get_episode_list")
                .addAllHeader(Net.header())
                .add("seasonId", seasonID)
                .asClass(Episode.class)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Episode>() {
                    @Override
                    public void accept(Episode episode) throws Throwable {
                        baseBind.recyList.setAdapter(new EpisodeAdapter(episode.getData().getEpisodeList(), mContext));
                        baseBind.allEpisode.setText("查看全部" + episode.getData().getEpisodeList().size() + "集");
                    }
                });
    }

    private void getPlayUrl(int seasonID, int episodeSid) {
        RxHttp.get("https://api.rr.tv/watch/v4/get_movie_play_info")
                .addAllHeader(Net.header())
                .add("quality", "HD")
                .add("seasonId", seasonID)
                .add("episodeSid", episodeSid)
                .asClass(Watch.class)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Watch>() {
                    @Override
                    public void accept(Watch watch) throws Throwable {
                    }
                });
    }

    private void getDetail(int seasonID) {
        RxHttp.get("https://api.rr.tv/rrtv-video/v4plus/season/detail")
            .addAllHeader(Net.header())
            .add("seasonId", seasonID)
            .add("token", "rrtv-b2228b19a37039db54172e9648c02a5dab579c88")
            .asClass(Movie.class)
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Consumer<Movie>() {
                @Override
                public void accept(Movie movie) throws Throwable {
                    TopView topView = new TopView(mContext);
                    topView.bindContent(movie.getData().getRecommendVideoList());
                    baseBind.createLinear.addView(topView);

                    BottomView bottomView = new BottomView(mContext);
                    bottomView.bindContent(movie.getData().getRecommendForYou());
                    baseBind.createLinear.addView(bottomView);
                }
            });
    }
}