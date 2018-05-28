package net.yupol.transmissionremote.app.torrentlist

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.torrent_list_layout.view.*
import net.yupol.transmissionremote.app.R
import net.yupol.transmissionremote.app.di.Injector
import net.yupol.transmissionremote.app.utils.DividerItemDecoration
import net.yupol.transmissionremote.model.json.Torrent
import timber.log.Timber
import java.util.concurrent.TimeUnit

class TorrentListFragment2: Fragment() {

    private var torrentListSubscription: Disposable? = null

    override fun onStart() {
        super.onStart()

        val transport = Injector.transportComponent(requireContext())
                .transport()

        val repository = TorrentsRepository(transport)

        Timber.tag("TorrentsList").d("Subscribing")
        torrentListSubscription = repository.torrents()
                .subscribeOn(Schedulers.io())
                .retryWhen { handler ->
                    handler.flatMap { error ->
                        Observable.just("").delay(1, TimeUnit.SECONDS)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ torrents ->
                    Timber.tag("TorrentsList").d("Fragment onNext: $torrents")
                }, { error ->
                    Timber.tag("TorrentsList").d("onError: ${error.message}")
                })
    }

    override fun onStop() {
        Timber.tag("TorrentsList").d("Unsubscribing")
        torrentListSubscription?.dispose()
        super.onStop()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.torrent_list_layout, container, false)

        view.recyclerView.layoutManager = LinearLayoutManager(context)
        view.recyclerView.addItemDecoration(DividerItemDecoration(context))
        view.recyclerView.itemAnimator = null
        view.recyclerView.adapter = TorrentsAdapter()

        return view
    }

    fun search(query: String) {
        TODO()
    }

    fun closeSearch() {
        TODO()
    }

    interface OnTorrentSelectedListener {
        fun onTorrentSelected(torrent: Torrent)
    }

    interface ContextualActionBarListener {
        fun onCABOpen()
        fun onCABClose()
    }
}