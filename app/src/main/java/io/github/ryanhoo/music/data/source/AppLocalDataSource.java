package io.github.ryanhoo.music.data.source;

import android.content.Context;
import android.util.Log;

import com.litesuits.orm.LiteOrm;
import com.litesuits.orm.db.assit.QueryBuilder;
import com.litesuits.orm.db.model.ConflictAlgorithm;

import io.github.ryanhoo.music.RxBus;
import io.github.ryanhoo.music.data.model.Folder;
import io.github.ryanhoo.music.data.model.PlayList;
import io.github.ryanhoo.music.data.model.Song;
import io.github.ryanhoo.music.data.model.Task;
import io.github.ryanhoo.music.event.PlayListCreatedEvent;
import io.github.ryanhoo.music.utils.DBUtils;
import rx.Observable;
import rx.Subscriber;

import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created with Android Studio.
 * User: ryan.hoo.j@gmail.com
 * Date: 9/10/16
 * Time: 4:54 PM
 * Desc: AppLocalDataSource
 */
/* package */ class AppLocalDataSource implements AppContract {

    private static final String TAG = "AppLocalDataSource";

    private Context mContext;
    private LiteOrm mLiteOrm;

    public AppLocalDataSource(Context context, LiteOrm orm) {
        mContext = context;
        mLiteOrm = orm;
    }

    // Play List

    @Override
    public Observable<List<PlayList>> playLists() {
        return Observable.create(new Observable.OnSubscribe<List<PlayList>>() {
            @Override
            public void call(Subscriber<? super List<PlayList>> subscriber) {
                List<PlayList> playLists = mLiteOrm.query(PlayList.class);
                if (playLists.isEmpty()) {
                    // First query, create the default play list
                    PlayList playList = DBUtils.generateFavoritePlayList(mContext);
                    PlayList playList5 =DBUtils.generateLevelPlayList(mContext, 5);
                    PlayList playList4 =DBUtils.generateLevelPlayList(mContext, 4);
                    PlayList playList3 =DBUtils.generateLevelPlayList(mContext, 3);
                    PlayList playList2 =DBUtils.generateLevelPlayList(mContext, 2);
                    PlayList playList1 =DBUtils.generateLevelPlayList(mContext, 1);


                    long result = mLiteOrm.save(playList);
                    Log.d(TAG, "Create default playlist(Favorite) with " + (result == 1 ? "success" : "failure"));

                    mLiteOrm.save(playList5);
                    mLiteOrm.save(playList4);
                    mLiteOrm.save(playList3);
                    mLiteOrm.save(playList2);
                    mLiteOrm.save(playList1);

                    playLists.add(playList);
                    playLists.add(playList5);
                    playLists.add(playList4);
                    playLists.add(playList3);
                    playLists.add(playList2);
                    playLists.add(playList1);
                }
                subscriber.onNext(playLists);
                subscriber.onCompleted();
            }
        });
    }

    @Override
    public List<PlayList> cachedPlayLists() {
        return null;
    }

    @Override
    public Observable<PlayList> create(final PlayList playList) {
        return Observable.create(new Observable.OnSubscribe<PlayList>() {
            @Override
            public void call(Subscriber<? super PlayList> subscriber) {
                Date now = new Date();
                playList.setCreatedAt(now);
                playList.setUpdatedAt(now);

                long result = mLiteOrm.save(playList);
                if (result > 0) {
                    subscriber.onNext(playList);
                } else {
                    subscriber.onError(new Exception("Create play list failed"));
                }
                subscriber.onCompleted();
            }
        });
    }

    @Override
    public Observable<PlayList> update(final PlayList playList) {
        return Observable.create(new Observable.OnSubscribe<PlayList>() {
            @Override
            public void call(Subscriber<? super PlayList> subscriber) {
                playList.setUpdatedAt(new Date());

                long result = mLiteOrm.update(playList);
                if (result > 0) {
                    subscriber.onNext(playList);
                } else {
                    subscriber.onError(new Exception("Update play list failed"));
                }
                subscriber.onCompleted();
            }
        });
    }

    @Override
    public Observable<PlayList> delete(final PlayList playList) {
        return Observable.create(new Observable.OnSubscribe<PlayList>() {
            @Override
            public void call(Subscriber<? super PlayList> subscriber) {
                long result = mLiteOrm.delete(playList);
                if (result > 0) {
                    subscriber.onNext(playList);
                } else {
                    subscriber.onError(new Exception("Delete play list failed"));
                }
                subscriber.onCompleted();
            }
        });
    }

    // Folder

    @Override
    public Observable<List<Folder>> folders() {
        return Observable.create(new Observable.OnSubscribe<List<Folder>>() {
            @Override
            public void call(Subscriber<? super List<Folder>> subscriber) {
                if (PreferenceManager.isFirstQueryFolders(mContext)) {
                    List<Folder> defaultFolders = DBUtils.generateDefaultFolders();
                    long result = mLiteOrm.save(defaultFolders);
                    Log.d(TAG, "Create default folders effected " + result + "rows");
                    PreferenceManager.reportFirstQueryFolders(mContext);
                }
                List<Folder> folders = mLiteOrm.query(
                        QueryBuilder.create(Folder.class).appendOrderAscBy(Folder.COLUMN_NAME)
                );
                subscriber.onNext(folders);
                subscriber.onCompleted();
            }
        });
    }

    @Override
    public Observable<Folder> create(final Folder folder) {
        return Observable.create(new Observable.OnSubscribe<Folder>() {
            @Override
            public void call(Subscriber<? super Folder> subscriber) {
                folder.setCreatedAt(new Date());

                long result = mLiteOrm.save(folder);
                if (result > 0) {
                    subscriber.onNext(folder);
                } else {
                    subscriber.onError(new Exception("Create folder failed"));
                }
                subscriber.onCompleted();
            }
        });
    }

    @Override
    public Observable<List<Folder>> create(final List<Folder> folders) {
        return Observable.create(new Observable.OnSubscribe<List<Folder>>() {
            @Override
            public void call(Subscriber<? super List<Folder>> subscriber) {
                Date now = new Date();
                for (Folder folder : folders) {
                    folder.setCreatedAt(now);
                }

                long result = mLiteOrm.save(folders);
                if (result > 0) {
                    List<Folder> allNewFolders = mLiteOrm.query(
                            QueryBuilder.create(Folder.class).appendOrderAscBy(Folder.COLUMN_NAME)
                    );
                    subscriber.onNext(allNewFolders);
                } else {
                    subscriber.onError(new Exception("Create folders failed"));
                }
                subscriber.onCompleted();
            }
        });
    }

    @Override
    public Observable<Folder> update(final Folder folder) {
        return Observable.create(new Observable.OnSubscribe<Folder>() {
            @Override
            public void call(Subscriber<? super Folder> subscriber) {
                mLiteOrm.delete(folder);
                long result = mLiteOrm.save(folder);
                if (result > 0) {
                    subscriber.onNext(folder);
                } else {
                    subscriber.onError(new Exception("Update folder failed"));
                }
                subscriber.onCompleted();
            }
        });
    }

    @Override
    public Observable<Folder> delete(final Folder folder) {
        return Observable.create(new Observable.OnSubscribe<Folder>() {
            @Override
            public void call(Subscriber<? super Folder> subscriber) {
                long result = mLiteOrm.delete(folder);
                if (result > 0) {
                    subscriber.onNext(folder);
                } else {
                    subscriber.onError(new Exception("Delete folder failed"));
                }
                subscriber.onCompleted();
            }
        });
    }

    @Override
    public Observable<List<Song>> insert(final List<Song> songs) {
        return Observable.create(new Observable.OnSubscribe<List<Song>>() {
            @Override
            public void call(Subscriber<? super List<Song>> subscriber) {
                for (Song song : songs) {
                    mLiteOrm.insert(song, ConflictAlgorithm.Abort);
                }
                List<Song> allSongs = mLiteOrm.query(Song.class);
                File file;
                for (Iterator<Song> iterator = allSongs.iterator(); iterator.hasNext(); ) {
                    Song song = iterator.next();
                    file = new File(song.getPath());
                    boolean exists = file.exists();
                    if (!exists) {
                        iterator.remove();
                        mLiteOrm.delete(song);
                    }
                }
                subscriber.onNext(allSongs);
                subscriber.onCompleted();
            }
        });
    }

    @Override
    public Observable<Song> update(final Song song) {
        return Observable.create(new Observable.OnSubscribe<Song>() {
            @Override
            public void call(Subscriber<? super Song> subscriber) {
                int result = mLiteOrm.update(song);
                if (result > 0) {
                    subscriber.onNext(song);
                } else {
                    subscriber.onError(new Exception("Update song failed"));
                }
                subscriber.onCompleted();
            }
        });
    }

    @Override
    public Observable<Song> setSongAsFavorite(final Song song, final boolean isFavorite) {
        return Observable.create(new Observable.OnSubscribe<Song>() {
            @Override
            public void call(Subscriber<? super Song> subscriber) {
                List<PlayList> playLists = mLiteOrm.query(
                        QueryBuilder.create(PlayList.class).whereEquals(PlayList.COLUMN_FAVORITE, String.valueOf(true))
                );
                if (playLists.isEmpty()) {
                    PlayList defaultFavorite = DBUtils.generateFavoritePlayList(mContext);
                    playLists.add(defaultFavorite);
                }
                PlayList favorite = playLists.get(0);
                song.setFavorite(isFavorite);
                favorite.setUpdatedAt(new Date());
                if (isFavorite) {
                    // Insert song to the beginning of the list
                    favorite.addSong(song, 0);
                } else {
                    favorite.removeSong(song);
                }
                mLiteOrm.insert(song, ConflictAlgorithm.Replace);
                long result = mLiteOrm.insert(favorite, ConflictAlgorithm.Replace);
                if (result > 0) {
                    subscriber.onNext(song);
                } else {
                    if (isFavorite) {
                        subscriber.onError(new Exception("Set song as favorite failed"));
                    } else {
                        subscriber.onError(new Exception("Set song as unfavorite failed"));
                    }
                }
                subscriber.onCompleted();
            }
        });
    }

    @Override
    public void doTask(Task task) {
//        return Observable.create(new Observable.OnSubscribe<Task>() {
//            @Override
//            public void call(Subscriber<? super Task> subscriber) {


        List<PlayList> playLists = mLiteOrm.query(
                QueryBuilder.create(PlayList.class).whereEquals(PlayList.NAME, "Level-" + task.getLevel())
        );
        PlayList playList = playLists.get(0);

        List<Song> songList = mLiteOrm.query(
                QueryBuilder.create(Song.class).whereEquals(Song.PATH, task.getPath())
        );

        Song song = songList.get(0);


        if (song.getLevel() > 0) {
            List<PlayList> oldPlayLists = mLiteOrm.query(
                    QueryBuilder.create(PlayList.class).whereEquals(PlayList.NAME, "Star-" + song.getLevel())
            );

            if (!oldPlayLists.isEmpty()) {
                oldPlayLists.get(0).removeSong(song);
                long result = mLiteOrm.insert(oldPlayLists.get(0), ConflictAlgorithm.Replace);
            }
        }


        song.setLevel(task.getLevel());
        playList.setUpdatedAt(new Date());


        // Insert song to the beginning of the list
        playList.addSong(song, 0);

        mLiteOrm.insert(song, ConflictAlgorithm.Replace);
        long result = mLiteOrm.insert(playList, ConflictAlgorithm.Replace);
//                if (result > 0) {
//                    subscriber.onNext(task);
//                } else {
//                    subscriber.onError(new Exception("Set song level failed"));
//                }
//                subscriber.onCompleted();
//            }
//        });
    }

    /**
     * ??????????????????
     *
     * @return ????????????
     */
    @Override
    public Observable<List<Task>> tasks() {
        return Observable.create(new Observable.OnSubscribe<List<Task>>() {
            @Override
            public void call(Subscriber<? super List<Task>> subscriber) {
                List<Task> tasks = mLiteOrm.query(QueryBuilder.create(Task.class));
                subscriber.onNext(tasks);
                subscriber.onCompleted();
            }
        });
    }

    /**
     * ????????????
     *
     * @param task ??????
     * @return ??????
     */
    @Override
    public Observable<Task> create(Task task) {
        return Observable.create(new Observable.OnSubscribe<Task>() {
            @Override
            public void call(Subscriber<? super Task> subscriber) {
                task.setCreatedAt(new Date());

                long result = mLiteOrm.save(task);
                if (result > 0) {
                    subscriber.onNext(task);
                } else {
                    subscriber.onError(new Exception("Create task failed"));
                }
                subscriber.onCompleted();
            }
        });
    }

    /**
     * ????????????
     *
     * @param task ??????
     * @return Task
     */
    @Override
    public Task delete(Task task) {
        long result = mLiteOrm.delete(task);
        if (result > 0) {
            return task;
        } else {
            throw new RuntimeException("Delete task failed");
        }
    }
}
