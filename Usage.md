# Requirements #
This application only works with **Android 3.2 or higher**.  Although you might use it on phones, the app is really targeted to tables.

You need an **USB card-reader of the brand ACS** (Advanced Card Systems Ltd).  It must be a CCID compatible device, which excludes the older ACR38U.  The ACR38U PocketMate on the other hand is CCID compliant and in my opinion the best match for mobile devices.

Naturally you need to be able to connect the card reader in host mode.  You can't connect your card reader directly to most devices, you need an adapter.  This is the same adapter as you need to connect an external hard-disk or USB stick directly to your Android device.

# Starting #

You can start the application in 2 different ways:
  * With the icon
  * By connecting a compatible reader

When you use the icon, you might not have access to the card reader.  Just click the USB icon in the right upper corner to try to connect.

When you connect a compatible reader, Android will ask if you want to use the eID View to access it. If you confirm the app will start and your card-reader will be connected.

# detached/attach/connected #

The card-reader can be detached, attached or connected.  See the upper right corner to see the (estimated) current state and switch states.

The displayed state isn't necessary the actual state.  The state is only updated on each action.

## detached ##
![http://eid-for-android.googlecode.com/svn/trunk/res/drawable-xhdpi/ic_usb_detached.png](http://eid-for-android.googlecode.com/svn/trunk/res/drawable-xhdpi/ic_usb_detached.png)

This means the card-reader isn't present or isn't accessible to the application.

When clicking on the icon, the application will try to attach and if that succeeds it will try to connect (and become connected).

When removing the card reader, the app will return to this state.

## attached ##
![http://eid-for-android.googlecode.com/svn/trunk/res/drawable-xhdpi/ic_usb_attached.png](http://eid-for-android.googlecode.com/svn/trunk/res/drawable-xhdpi/ic_usb_attached.png)

This means the card-reader is accessible to the application but isn't accessed.

When clicking on the icon, the application will try to connect (and become connected)

## connected ##
![http://eid-for-android.googlecode.com/svn/trunk/res/drawable-xhdpi/ic_usb_connected.png](http://eid-for-android.googlecode.com/svn/trunk/res/drawable-xhdpi/ic_usb_connected.png)

This means the application is actually connected to the card-reader.  At this point the app will get notified when a new eID-card is inserted and it will update the content on the screen.

When clicking on the icon, the application will try to deconnect (and become attached).