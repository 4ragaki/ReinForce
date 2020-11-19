package ceui.lisa.rrshare.fragments;

import android.view.View;

import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.HashMap;

import ceui.lisa.rrshare.MovieActivity;
import ceui.lisa.rrshare.R;
import ceui.lisa.rrshare.adapters.OnItemClickListener;
import ceui.lisa.rrshare.view.BottomView;
import ceui.lisa.rrshare.view.TopView;
import ceui.lisa.rrshare.adapters.EpisodeAdapter;
import ceui.lisa.rrshare.databinding.FragmentMovieDetailBinding;
import ceui.lisa.rrshare.network.Net;
import ceui.lisa.rrshare.network.NullCtrl;
import ceui.lisa.rrshare.response.Content;
import ceui.lisa.rrshare.response.Episode;
import ceui.lisa.rrshare.response.Movie;
import ceui.lisa.rrshare.response.Watch;
import ceui.lisa.rrshare.utils.Common;
import ceui.lisa.rrshare.utils.DensityUtil;
import ceui.lisa.rrshare.utils.LinearItemDecorationHorizon;
import fun.aragaki.rr.RR;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import rxhttp.RxHttp;

public class FragmentMovieDetail extends BaseMovieFragment<FragmentMovieDetailBinding> {

    @Override
    protected void initLayout() {
        mLayoutID = R.layout.fragment_movie_detail;
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
                    if ("相关视频".equals(content.getFrom())) {
                        getPlayUrl(content.getId());
                        getVideoDetail(content.getId());
                    } else {
                        getEpisode(content.getId());
                        getDetail(content.getId());
                    }
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
                .subscribe(new NullCtrl<Episode>() {
                    @Override
                    public void success(Episode episode) {
                        if (!Common.isEmpty(episode.getData().getEpisodeList())) {
                            baseBind.episodeLl.setVisibility(View.VISIBLE);
                            episode.getData().getEpisodeList().get(0).setPlaying(true);
                            EpisodeAdapter adapter = new EpisodeAdapter(episode.getData().getEpisodeList(), mContext);
                            adapter.setOnItemClickListener(new OnItemClickListener() {
                                @Override
                                public void onItemClick(View v, int position, int viewType) {
                                    for (int i = 0; i < episode.getData().getEpisodeList().size(); i++) {
                                        if (i == position) {
                                            episode.getData().getEpisodeList().get(i).setPlaying(true);
                                        } else {
                                            episode.getData().getEpisodeList().get(i).setPlaying(false);
                                        }
                                    }
                                    adapter.notifyDataSetChanged();
                                    getPlayUrl(seasonID, episode.getData().getEpisodeList().get(position).getId());
                                }
                            });
                            baseBind.recyList.setAdapter(adapter);
                            baseBind.allEpisode.setText("查看全部" + episode.getData().getEpisodeList().size() + "集");

                            //直接播放第一P
                            getPlayUrl(model.getMovie().getValue().getId(), episode.getData().getEpisodeList().get(0).getId());
                        }
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
                .subscribe(new NullCtrl<Watch>() {
                    @Override
                    public void success(Watch watch) {
                        if ("DIRECT".equals(watch.getData().getM3u8().getParserType())) {
                            if (mActivity instanceof MovieActivity) {
                                ((MovieActivity) mActivity).play(watch.getData().getM3u8().getUrl());
                            }
                        } else {
                            String url = RR.INSTANCE.decrypt(watch.getData().getM3u8().getUrl(),
                                    Net.TOKEN , new HashMap<>());
                            if (mActivity instanceof MovieActivity) {
                                ((MovieActivity) mActivity).play(url);
                            }
                        }
                    }
                });
    }


    private void getPlayUrl(int videoId) {
        RxHttp.get("https://api.rr.tv/watch/get_video_info")
                .addAllHeader(Net.header())
                .add("quality", "super")
                .add("videoId", videoId)
                .asClass(Watch.class)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new NullCtrl<Watch>() {
                    @Override
                    public void success(Watch watch) {
                        if ("DIRECT".equals(watch.getData().getM3u8().getParserType())) {
                            if (mActivity instanceof MovieActivity) {
                                ((MovieActivity) mActivity).play(watch.getData().getM3u8().getUrl());
                            }
                        }
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
            .subscribe(new NullCtrl<Movie>() {
                @Override
                public void success(Movie movie) {
                    if (!Common.isEmpty(movie.getData().getRecommendVideoList())) {
                        TopView topView = new TopView(mContext);
                        topView.bindContent(movie.getData().getRecommendVideoList());
                        baseBind.createLinear.addView(topView);
                    }

                    if (!Common.isEmpty(movie.getData().getRecommendForYou())) {
                        BottomView bottomView = new BottomView(mContext);
                        bottomView.bindContent(movie.getData().getRecommendForYou());
                        baseBind.createLinear.addView(bottomView);
                    }
                }
            });
    }

    private void getVideoDetail(int videoId) {
        RxHttp.get("https://api.rr.tv/v3plus/video/detail")
                .addAllHeader(Net.header())
                .add("videoId", videoId)
                .add("token", "rrtv-b2228b19a37039db54172e9648c02a5dab579c88")
                .asClass(Movie.class)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new NullCtrl<Movie>() {
                    @Override
                    public void success(Movie movie) {
                        if (!Common.isEmpty(movie.getData().getRecommendVideoList())) {
                            TopView topView = new TopView(mContext);
                            topView.bindContent(movie.getData().getRecommendVideoList());
                            baseBind.createLinear.addView(topView);
                        }

                        if (!Common.isEmpty(movie.getData().getRecommendForYou())) {
                            BottomView bottomView = new BottomView(mContext);
                            bottomView.bindContent(movie.getData().getRecommendForYou());
                            baseBind.createLinear.addView(bottomView);
                        }
                    }
                });
    }
}
