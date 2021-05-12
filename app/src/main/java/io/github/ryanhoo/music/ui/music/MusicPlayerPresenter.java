package io.github.ryanhoo.music.ui.music;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.github.ryanhoo.music.RxBus;
import io.github.ryanhoo.music.data.model.PlayList;
import io.github.ryanhoo.music.data.model.Song;
import io.github.ryanhoo.music.data.model.Task;
import io.github.ryanhoo.music.data.source.AppRepository;
import io.github.ryanhoo.music.data.source.PreferenceManager;
import io.github.ryanhoo.music.event.FavoriteChangeEvent;
import io.github.ryanhoo.music.player.PlayMode;
import io.github.ryanhoo.music.player.PlaybackService;
import io.github.ryanhoo.music.player.Player;
import io.github.ryanhoo.music.utils.FileUtils;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created with Android Studio.
 * User: ryan.hoo.j@gmail.com
 * Date: 9/12/16
 * Time: 8:30 AM
 * Desc: MusicPlayerPresenter
 */
public class MusicPlayerPresenter implements MusicPlayerContract.Presenter {

    private Context mContext;
    private MusicPlayerContract.View mView;
    private AppRepository mRepository;
    private CompositeSubscription mSubscriptions;

    private PlaybackService mPlaybackService;
    private boolean mIsServiceBound;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mPlaybackService = ((PlaybackService.LocalBinder) service).getService();
            mView.onPlaybackServiceBound(mPlaybackService);
            mView.onSongUpdated(mPlaybackService.getPlayingSong());
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mPlaybackService = null;
            mView.onPlaybackServiceUnbound();
        }
    };

    public MusicPlayerPresenter(Context context, AppRepository repository, MusicPlayerContract.View view) {
        mContext = context;
        mView = view;
        mRepository = repository;
        mSubscriptions = new CompositeSubscription();
        mView.setPresenter(this);
    }

    @Override
    public void subscribe() {
        bindPlaybackService();

        retrieveLastPlayMode();

        // TODO
        if (mPlaybackService != null && mPlaybackService.isPlaying()) {
            mView.onSongUpdated(mPlaybackService.getPlayingSong());
        } else {
            // - load last play list/folder/song
        }
    }

    @Override
    public void unsubscribe() {
        unbindPlaybackService();
        // Release context reference
        mContext = null;
        mView = null;
        mSubscriptions.clear();
    }

    @Override
    public void retrieveLastPlayMode() {
        PlayMode lastPlayMode = PreferenceManager.lastPlayMode(mContext);
        mView.updatePlayMode(lastPlayMode);
    }

    @Override
    public void setSongAsFavorite(Song song, boolean favorite) {
        Subscription subscription = mRepository.setSongAsFavorite(song, favorite)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Song>() {
                    @Override
                    public void onCompleted() {
                        // Empty
                    }

                    @Override
                    public void onError(Throwable e) {
                        mView.handleError(e);
                    }

                    @Override
                    public void onNext(Song song) {
                        mView.onSongSetAsFavorite(song);
                        RxBus.getInstance().post(new FavoriteChangeEvent(song));
                    }
                });
        mSubscriptions.add(subscription);

    }

    @Override
    public void doSetSongLevelTask() {
        Subscription subscription = mRepository.tasks()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Task>>() {
                    @Override
                    public void onStart() {
//                        mView.showLoading();
                    }

                    @Override
                    public void onCompleted() {
//                        mView.hideLoading();
                    }

                    @Override
                    public void onError(Throwable e) {
//                        mView.hideLoading();
                        mView.handleError(e);
                    }

                    @Override
                    public void onNext(List<Task> taskList) {
                        System.out.println(taskList);


                        taskList.forEach(t -> {
                            Song song = Player.getInstance().getPlayingSong();
                            if (song != null && t.getPath().equals(song.getPath())) {
                                return;
                            }

                            Mp3File mp3file = null;
                            try {
                                mp3file = new Mp3File(t.getPath());

                                if (mp3file.hasId3v2Tag()) {
                                    ID3v2 id3v2Tag = mp3file.getId3v2Tag();
                                    System.out.println("唱片歌曲数量: " + id3v2Tag.getTrack());
                                    System.out.println("艺术家: " + id3v2Tag.getArtist());
                                    System.out.println("歌曲名: " + id3v2Tag.getTitle());
                                    System.out.println("评级: " + id3v2Tag.getWmpRating());
                                    id3v2Tag.setWmpRating(t.getLevel());

//                                    new File(t.getPath()).delete();
                                    mp3file.save(t.getPath() + ".tmp");
                                    new File(t.getPath()).delete();
                                    new File(t.getPath() + ".tmp").renameTo(new File(t.getPath()));

                                }

                                mRepository.delete(t);
                            } catch (Exception e) {
                                e.printStackTrace();
                                mRepository.delete(t);
                            }

                        });


//                        mView.onPlayListsLoaded(playLists);
                    }
                });
        mSubscriptions.add(subscription);
    }

    @Override
    public void setSongLevel(Song song, int level) {
        Task task = new Task(song.getPath(), level);
        Subscription subscription = mRepository.create(task)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Task>() {
                    @Override
                    public void onCompleted() {
                        // Empty
                    }

                    @Override
                    public void onError(Throwable e) {
                        mView.handleError(e);
                    }

                    @Override
                    public void onNext(Task task) {
                        mView.onSongSetLevel(song, level);
                    }
                });
        mSubscriptions.add(subscription);


    }

    @Override
    public void bindPlaybackService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        mContext.bindService(new Intent(mContext, PlaybackService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsServiceBound = true;
    }

    @Override
    public void unbindPlaybackService() {
        if (mIsServiceBound) {
            // Detach our existing connection.
            mContext.unbindService(mConnection);
            mIsServiceBound = false;
        }
    }
}
