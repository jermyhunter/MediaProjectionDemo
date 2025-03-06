In Android 14+, `MediaProjection` must run in **ForegroundService**, or may encountering Security Exception. 

Permission-requesting and getting `MediaProjectionManager` **MUST BE BEFORE** starting `ForegroundService`, or may encountering Security Exception.

**PREREQUISITE**

Setting up `FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_MEDIA_PROJECTION` and `Service` label in `AndroidManifest.xml`

```xml
...
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />  
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />

<application>
	...
	<service  
	    android:name=".service.ScreenShotService"  
	    android:exported="false"  
	    android:foregroundServiceType="mediaProjection" />
</application>
```

The whole process as follow:
1. request permission of media projection
2. create `NotificationChannel`
3. get `MediaProjectionManager`
4. declare `ServiceInfo.FOREGROUND_SERVICE_TYPE_`,and start `ForegroundService` (must be foreground)
5. set `setOnImageAvailableListener`, and attach `ImageReader` to `VirtualDisplay`
6. process the Image from `ImageReader` as your will
7. release unused data

After the first time running, loop 1, 3-7 steps



References:
[Media Projection | Android Dev](https://developer.android.com/reference/android/media/projection/MediaProjection)
[MediaProjectionManager#createScreenCaptureIntent](https://developer.android.com/reference/android/media/projection/MediaProjectionManager#createScreenCaptureIntent\(android.media.projection.MediaProjectionConfig\))
[Service-type](https://developer.android.com/develop/background-work/services/fgs/service-types?)
[Stack overflow](https://stackoverflow.com/questions/77307867/screen-capture-mediaprojection-on-android-14)
