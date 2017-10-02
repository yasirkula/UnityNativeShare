package com.yasirkula.unity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.io.File;

/**
 * Created by yasirkula on 22.06.2017.
 */

public class NativeShare
{
    private static String authority = null;

    public static void MediaShareFile(Context context, String path, String authority, boolean isImage )
    {
        Intent intent = new Intent(Intent.ACTION_SEND);
        String mimeType;
        if( isImage )
            mimeType = "image/*";
        else
            mimeType = "video/mp4";

        Uri contentUri = UnitySSContentProvider.getUriForFile(context, authority, new File(path));

        intent.putExtra(Intent.EXTRA_STREAM, contentUri);
        intent.setType( mimeType );
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(Intent.createChooser(intent, ""));
    }
}
