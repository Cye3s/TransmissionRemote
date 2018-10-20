package net.yupol.transmissionremote.app.home

import com.hannesdorfmann.mosby3.mvp.MvpNullObjectBasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.yupol.transmissionremote.app.model.mapper.TorrentMapper
import net.yupol.transmissionremote.app.mvp.MvpViewCallback
import net.yupol.transmissionremote.domain.usecase.TorrentListInteractor
import java.util.concurrent.TimeUnit

class MainActivityPresenter(
        private val interactor: TorrentListInteractor,
        private val torrentMapper: TorrentMapper): MvpNullObjectBasePresenter<MainActivityView>(), MvpViewCallback
{
    companion object {
        private const val TAG = "MainActivityPresenter"
    }

    private var torrentListDisposable: Disposable? = null

    override fun viewStarted() {
        view.showLoading()
        refresh()
    }

    override fun viewStopped() {
        torrentListDisposable?.dispose()
    }

    fun refresh() {
        view.showLoading()
        startTorrentListLoading()
    }

    fun pauseClicked(torrentId: Int) {
        val d = interactor.pauseTorrent(torrentId)
                .map(torrentMapper::toViewModel)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ torrent ->
                    view.showUpdatedTorrents(torrent)
                }, { error ->
                    view.showErrorAlert(error)
                })
    }

    fun resumeClicked(torrentId: Int) {
        val d = interactor.resumeTorrent(torrentId)
                .map(torrentMapper::toViewModel)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ torrent ->
                    view.showUpdatedTorrents(torrent)
                }, { error ->
                    view.showErrorAlert(error)
                })
    }

    fun pauseAllClicked() {
        view.showLoading()

        val d = interactor.pauseAllTorrents()
                .delay(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    refresh()
                }, { error ->
                    view.hideLoading()
                    view.showErrorAlert(error)
                })
    }

    fun resumeAllClicked() {
        view.showLoading()

        val d = interactor.resumeAllTorrents()
                .delay(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    refresh()
                }, { error ->
                    view.hideLoading()
                    view.showErrorAlert(error)
                })
    }

    private fun startTorrentListLoading() {
        torrentListDisposable?.dispose()

        torrentListDisposable = interactor.loadTorrentList()
                .map(torrentMapper::toViewMode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ torrents ->
                    view.hideLoading()
                    view.showTorrents(torrents)
                }, { error ->
                    view.hideLoading()
                    view.showError(error)
                })
    }
}
