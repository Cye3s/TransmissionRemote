package net.yupol.transmissionremote.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;

import net.yupol.transmissionremote.app.PauseResumeButton.State;
import net.yupol.transmissionremote.app.TransmissionRemote.OnFilterSelectedListener;
import net.yupol.transmissionremote.app.TransmissionRemote.OnTorrentsUpdatedListener;
import net.yupol.transmissionremote.app.filtering.Filter;
import net.yupol.transmissionremote.app.model.json.Torrent;
import net.yupol.transmissionremote.app.transport.BaseSpiceActivity;
import net.yupol.transmissionremote.app.transport.TransportManager;
import net.yupol.transmissionremote.app.transport.request.Request;
import net.yupol.transmissionremote.app.transport.request.StartTorrentRequest;
import net.yupol.transmissionremote.app.transport.request.StopTorrentRequest;
import net.yupol.transmissionremote.app.utils.SizeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TorrentListFragment extends ListFragment {

    private static final String TAG = TorrentListFragment.class.getSimpleName();

    private static final String MAX_STRING = "999.9 MB/s";

    private TransmissionRemote app;

    private Collection<Torrent> allTorrents = Collections.emptyList();
    private List<Torrent> torrentsToShow = Collections.emptyList();

    private Comparator<Torrent> comparator;

    private OnTorrentsUpdatedListener torrentsListener = new OnTorrentsUpdatedListener() {
        @Override
        public void torrentsUpdated(Collection<Torrent> torrents) {
            allTorrents = torrents;
            TorrentListFragment.this.updateTorrentList();
        }
    };

    private OnFilterSelectedListener filterListener = new OnFilterSelectedListener() {
        @Override
        public void filterSelected(Filter filter) {
            updateTorrentList();
        }
    };

    private OnTorrentSelectedListener torrentSelectedListener;

    public TorrentListFragment() {
        setListAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return torrentsToShow.size();
            }

            @Override
            public Torrent getItem(int position) {
                return torrentsToShow.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View itemView;

                if (convertView == null) {
                    LayoutInflater li = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    itemView = li.inflate(R.layout.torrent_list_item, parent, false);
                } else {
                    itemView = convertView;
                }

                int bgColorId = position % 2 == 0 ? R.color.torrent_list_odd_item_background
                        : R.color.torrent_list_even_item_background;
                itemView.setBackgroundColor(getResources().getColor(bgColorId));

                final Torrent torrent = getItem(position);

                TextView nameText = (TextView) itemView.findViewById(R.id.name);
                nameText.setText(torrent.getName());

                String totalSize = SizeUtils.displayableSize(torrent.getTotalSize());
                String downloadedText;
                if (torrent.getPercentDone() == 1.0) {
                    downloadedText = totalSize;
                } else {
                    String downloadedSize = SizeUtils.displayableSize((long) (torrent.getPercentDone() * torrent.getTotalSize()));
                    String percentDone = String.format("%.2f%%", 100 * torrent.getPercentDone());
                    downloadedText = getString(R.string.downloaded_text, downloadedSize, totalSize, percentDone);
                }

                TextView downloadedTextView = (TextView) itemView.findViewById(R.id.downloaded_text);
                downloadedTextView.setText(downloadedText);

                double uploadRatio = Math.max(torrent.getUploadRatio(), 0.0);
                String uploadedText = getString(R.string.uploaded_text,
                        SizeUtils.displayableSize(torrent.getUploadedSize()), uploadRatio);
                TextView uploadedTextView = (TextView) itemView.findViewById(R.id.uploaded_text);
                uploadedTextView.setText(uploadedText);

                ProgressBar progressBar = (ProgressBar) itemView.findViewById(R.id.progress_bar);
                progressBar.setProgress((int) (torrent.getPercentDone() * progressBar.getMax()));
                boolean isPaused = isPaused(torrent.getStatus());
                int progressbarDrawable = isPaused ? R.drawable.torrent_progressbar_disabled : R.drawable.torrent_progressbar;
                progressBar.setProgressDrawable(getResources().getDrawable(progressbarDrawable));

                TextView downloadRateText = (TextView) itemView.findViewById(R.id.download_rate);
                downloadRateText.setText(speedText(torrent.getDownloadRate()));

                TextView uploadRateText = (TextView) itemView.findViewById(R.id.upload_rate);
                uploadRateText.setText(speedText(torrent.getUploadRate()));

                Rect bounds = new Rect();
                downloadRateText.getPaint().getTextBounds(MAX_STRING, 0, MAX_STRING.length(), bounds);
                int maxWidth = bounds.width();
                downloadRateText.setWidth(maxWidth);
                uploadRateText.setWidth(maxWidth);

                PauseResumeButton pauseResumeBtn = (PauseResumeButton) itemView.findViewById(R.id.pause_resume_button);
                pauseResumeBtn.setState(isPaused ? State.RESUME : State.PAUSE);

                pauseResumeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PauseResumeButton btn = (PauseResumeButton) v;
                        State state = btn.getState();
                        btn.toggleState();

                        Activity activity = getActivity();
                        if (activity instanceof BaseSpiceActivity) {
                            TransportManager transportManager = ((BaseSpiceActivity) activity).getTransportManager();
                            Request<Void> request = state == State.PAUSE
                                    ? new StopTorrentRequest(Collections.singletonList(torrent))
                                    : new StartTorrentRequest(Collections.singletonList(torrent));
                            transportManager.doRequest(request, null);
                        } else {
                            Log.e(TAG, "Can't send Start/Stop request. " +
                                    "Fragment should be used inside BaseSpiceActivity to be able to obtain TransportManager");
                        }
                    }
                });

                TextView errorMsgView = (TextView) itemView.findViewById(R.id.error_message);
                Torrent.Error error = torrent.getError();
                if (error == Torrent.Error.NONE) {
                    errorMsgView.setVisibility(View.GONE);
                } else {
                    String errorMsg = torrent.getErrorMessage();
                    if (errorMsg != null && !errorMsg.trim().isEmpty()) {
                        errorMsgView.setVisibility(View.VISIBLE);
                        errorMsgView.setText(errorMsg);
                        int msgIconResId = error.isWarning() ? R.drawable.ic_action_warning : R.drawable.ic_action_error;
                        Drawable msgIcon = getResources().getDrawable(msgIconResId);
                        int size = getResources().getDimensionPixelSize(R.dimen.torrent_list_error_icon_size);
                        msgIcon.setBounds(0, 0, size, size);
                        errorMsgView.setCompoundDrawables(msgIcon, null, null, null);
                    } else {
                        errorMsgView.setVisibility(View.GONE);
                    }
                }

                return itemView;
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        app = (TransmissionRemote) activity.getApplication();
        app.addOnFilterSetListener(filterListener);
    }

    @Override
    public void onStart() {
        super.onStart();
        app.addTorrentsUpdatedListener(torrentsListener);
        allTorrents = app.getTorrents();
        updateTorrentList();
    }

    @Override
    public void onStop() {
        app.removeTorrentsUpdatedListener(torrentsListener);
        super.onStop();
    }

    @Override
    public void onDetach() {
        app.removeOnFilterSelectedListener(filterListener);
        super.onDetach();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (torrentSelectedListener != null) {
            Torrent torrent = (Torrent) getListAdapter().getItem(position);
            torrentSelectedListener.onTorrentSelected(torrent);
        }
    }

    public void setOnTorrentSelectedListener(OnTorrentSelectedListener listener) {
        torrentSelectedListener = listener;
    }

    private void updateTorrentList() {
        Filter filter = app.getActiveFilter();
        torrentsToShow = new ArrayList<>(FluentIterable.from(allTorrents).filter(filter).toList());
        setEmptyText(getResources().getString(filter.getEmptyMessageResId()));
        if (comparator != null)
            Collections.sort(torrentsToShow, comparator);
        ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
    }

    public void setSort(Comparator<Torrent> comparator) {
        this.comparator = comparator;
        if (allTorrents != null && !allTorrents.isEmpty())
            updateTorrentList();
    }

    private String speedText(long bytes) {
        return Strings.padStart(SizeUtils.displayableSize(bytes), 5, ' ') + "/s";
    }

    private boolean isPaused(Torrent.Status status) {
        return status == Torrent.Status.STOPPED;
    }

    public interface OnTorrentSelectedListener {
        void onTorrentSelected(Torrent torrent);
    }
}
