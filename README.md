In Android 14+, `MediaProjection` must run in **ForegroundService**, or may encountering Security Exception. 

Permission-requesting and getting `MediaProjectionManager` **MUST BE BEFORE** starting `ForegroundService`, or may encountering Security Exception.

# 1. Guidelines

## PREREQUISITE

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

## The whole process as follow
1. request permission of media projection
2. create `NotificationChannel`
3. get `MediaProjectionManager`
4. declare `ServiceInfo.FOREGROUND_SERVICE_TYPE_`,and start `ForegroundService` (must be foreground)
5. set `setOnImageAvailableListener`, and attach `ImageReader` to `VirtualDisplay`
6. process the Image from `ImageReader` as your will
7. release unused data

After the first time running, loop 1, 3-7 steps

## How to use

Process the bitmap data further from service at the **TODO** comment from MainActivity.kt.

**In order to get the right screenshot after alert dialog disappeared, 1000ms delay is set before capturin in the demo.**

---

# 2. Problems

## \*Defect: repetitve permission request

Everytime caturing the screen, you must request a permission from the user, which means you can't cature other app's display correctly.
So I set 1000ms delay to let user to switching to the right app. In your development, you can use **countdown showing on [Floating Window](https://github.com/only52607/compose-floating-window/tree/0bd5025c480dbc3827b729596590f8a4a9689fea)** to notify the user.

## \*WARNING: Android 34+

[User consent | Android Devs](https://developer.android.com/media/grow/media-projection#user_consent)
> Your app must request user consent before each media projection session. A session is a single call to createVirtualDisplay(). A MediaProjection token must be used only once to make the call.
> 
> On Android 14 or higher, the createVirtualDisplay() method throws a SecurityException if your app does either of the following:
> 
> Passes an Intent instance returned from createScreenCaptureIntent() to getMediaProjection() more than once
> Calls createVirtualDisplay() more than once on the same MediaProjection instance

---

# 3. References

## Relevant library

[compose-floating-window | Github](https://github.com/only52607/compose-floating-window/blob/0bd5025c480dbc3827b729596590f8a4a9689fea)

## Pages
[Media Projection | Android Dev](https://developer.android.com/reference/android/media/projection/MediaProjection)

[MediaProjectionManager#createScreenCaptureIntent](https://developer.android.com/reference/android/media/projection/MediaProjectionManager#createScreenCaptureIntent\(android.media.projection.MediaProjectionConfig\))

[Service-type](https://developer.android.com/develop/background-work/services/fgs/service-types?)
[Stack overflow](https://stackoverflow.com/questions/77307867/screen-capture-mediaprojection-on-android-14)

---
**PS:**

The author is not a professional Android dever. In the process of DIY an app for self-use, I found that there is lack of references and demos for kotlin version of convenient screenshot function. It takes me one day to ask GPT to get a mixed version of SDK 34+ and SDK 34- : (

Be free to use this demo, hope it brings you less tortured : )
