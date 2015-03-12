package net.yupol.transmissionremote.app.transport.request;

import android.util.Base64;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class AddTorrentByFileRequest extends AddTorrentRequest {

    private static final String TAG = AddTorrentByFileRequest.class.getSimpleName();

    private String metaInfo;

    public AddTorrentByFileRequest(File file, String destination, boolean paused) throws IOException {
        super(destination, paused);
        byte[] content = FileUtils.readFileToByteArray(file);
        metaInfo = Base64.encodeToString(content, Base64.DEFAULT);
    }

    @Override
    protected JSONObject getArguments() {
        JSONObject args = super.getArguments();
        try {
            args.put("metainfo", metaInfo);
        } catch (JSONException e) {
            Log.e(TAG, "Error while creating json object");
        }
        return args;
    }
}
