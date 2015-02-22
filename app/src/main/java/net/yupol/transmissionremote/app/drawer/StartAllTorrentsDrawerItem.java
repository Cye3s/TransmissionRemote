package net.yupol.transmissionremote.app.drawer;

import android.content.Context;

import net.yupol.transmissionremote.app.R;
import net.yupol.transmissionremote.app.TransmissionRemote;
import net.yupol.transmissionremote.app.transport.TransportManager;
import net.yupol.transmissionremote.app.transport.request.StartTorrentRequest;

public class StartAllTorrentsDrawerItem extends DrawerItem {

    private TransmissionRemote app;
    private TransportManager transportManager;

    public StartAllTorrentsDrawerItem(Context context, TransportManager transportManager) {
        super(R.string.drawer_actions_start_all_torrents, context);
        app = (TransmissionRemote) context.getApplicationContext();
        this.transportManager = transportManager;
    }

    @Override
    public void itemSelected() {
        transportManager.doRequest(new StartTorrentRequest(app.getTorrents()), null);
    }

    @Override
    public int getLeftImage() {
        return R.drawable.ic_media_play;
    }
}
