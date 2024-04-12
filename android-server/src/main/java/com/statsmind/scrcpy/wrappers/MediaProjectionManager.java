package com.statsmind.scrcpy.wrappers;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.os.IInterface;
import android.view.InputEvent;
import lombok.SneakyThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MediaProjectionManager {
    private IInterface manager;

    public MediaProjectionManager(IInterface manager) {
        this.manager = manager;
    }

    public IInterface getManager() {
        return manager;
    }

    Method getMediaProjectionMethod;

    private Method getGetMediaProjectionMethod() throws NoSuchMethodException {
        if (getMediaProjectionMethod == null) {
            getMediaProjectionMethod = manager.getClass().getMethod("getMediaProjection", int.class, Intent.class);
        }
        return getMediaProjectionMethod;
    }

    @SneakyThrows
    public MediaProjection getMediaProjection() {
        return (MediaProjection) getGetMediaProjectionMethod().invoke(manager, Activity.RESULT_OK, createScreenCaptureIntent());
    }

    @SneakyThrows
    public Intent createScreenCaptureIntent() {
        Method method = manager.getClass().getMethod("createScreenCaptureIntent");
        return (Intent) method.invoke(manager);
    }
}
