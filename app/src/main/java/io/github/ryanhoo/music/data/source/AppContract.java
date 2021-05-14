package io.github.ryanhoo.music.data.source;

import io.github.ryanhoo.music.data.model.Folder;
import io.github.ryanhoo.music.data.model.PlayList;
import io.github.ryanhoo.music.data.model.Song;
import io.github.ryanhoo.music.data.model.Task;
import rx.Observable;

import java.util.List;

/**
 * Created with Android Studio.
 * User: ryan.hoo.j@gmail.com
 * Date: 9/10/16
 * Time: 4:52 PM
 * Desc: AppContract
 */
/* package */ interface AppContract {

    // Play List

    Observable<List<PlayList>> playLists();

    List<PlayList> cachedPlayLists();

    Observable<PlayList> create(PlayList playList);

    Observable<PlayList> update(PlayList playList);

    Observable<PlayList> delete(PlayList playList);

    // Folder

    Observable<List<Folder>> folders();

    Observable<Folder> create(Folder folder);

    Observable<List<Folder>> create(List<Folder> folders);

    Observable<Folder> update(Folder folder);

    Observable<Folder> delete(Folder folder);

    // Song

    Observable<List<Song>> insert(List<Song> songs);

    Observable<Song> update(Song song);

    Observable<Song> setSongAsFavorite(Song song, boolean favorite);

    void doTask(Task task);

    // Task

    /**
     * 查询任务列表
     *
     * @return 任务列表
     */
    Observable<List<Task>> tasks();

    /**
     * 创建任务
     *
     * @param task 任务
     * @return 任务
     */
    Observable<Task> create(Task task);

    /**
     * 删除任务
     *
     * @param task 任务
     * @return Task
     */
    Task delete(Task task);
}
