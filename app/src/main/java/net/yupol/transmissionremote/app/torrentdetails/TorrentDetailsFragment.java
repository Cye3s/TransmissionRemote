package net.yupol.transmissionremote.app.torrentdetails;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.yupol.transmissionremote.app.R;
import net.yupol.transmissionremote.app.model.json.Torrent;
import net.yupol.transmissionremote.app.transport.request.TorrentSetRequest;

public class TorrentDetailsFragment extends Fragment {

    private Torrent torrent;
    private TorrentDetailsPagerAdapter pagerAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.torrent_details_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        updateUi();
    }

    public void setTorrent(Torrent torrent) {
        this.torrent = torrent;
        updateUi();
    }

    private void updateUi() {
        if (getView() == null) return;

        ViewPager pager = (ViewPager) getView().findViewById(R.id.pager);
        FragmentManager fm = ((FragmentActivity) getActivity()).getSupportFragmentManager();
        pagerAdapter = new TorrentDetailsPagerAdapter(getActivity(), fm, torrent);
        pager.setAdapter(pagerAdapter);
    }

    public TorrentSetRequest.Builder getSetOptionsRequestBuilder() {
        if (pagerAdapter != null) {
            return pagerAdapter.getOptionsPageFragment().getSetOptionsRequestBuilder();
        }
        return null;
    }
}
