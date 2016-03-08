package net.yupol.transmissionremote.app.transport;

import com.octo.android.robospice.request.listener.RequestListener;

import net.yupol.transmissionremote.app.transport.request.Request;

public interface TransportManager {
    <T> void doRequest(final Request<T> request, RequestListener<T> listener);
    <T> void doRequest(final Request<T> request, RequestListener<T> listener, long delay);
    boolean isStarted();
}
