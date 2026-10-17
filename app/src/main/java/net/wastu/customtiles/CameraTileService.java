package net.wastu.customtiles;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.provider.MediaStore;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

public class CameraTileService extends TileService {
    private static final String TAG = "CameraTileService";

    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_tile_camera));
            tile.setLabel(getString(R.string.tile_camera_label));
            tile.setState(Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        try {
            PendingIntent pi = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );
            startActivityAndCollapse(pi);
        } catch (Throwable t) {
            Log.w(TAG, "startActivityAndCollapse with PendingIntent failed, trying intent: " + t.getMessage());
            try {
                startActivityAndCollapse(intent);
            } catch (Throwable t2) {
                Log.e(TAG, "startActivityAndCollapse failed: " + t2.getMessage(), t2);
            }
        }
    }
}
