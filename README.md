## Publisher Alongside Camera Application

#####This application demonstrates how to make a TokBox custom video capturer to relinquish, and then re-obtain the camera, all without destroying the Publisher (and having audio publish to your Session)

####Usage
1. Replace API\_KEY, SESSION\_ID, and TOKEN inside OpenTokConfig.java with your own valid credentials
2. Run the application
3. Notice your video on the screen. The Publisher is iniitalized and publishing.
4. Background the appllication and go to the native camera app. Take a picture. Note that your audio is still being Published to the Session, but your video is not. You have relinquished the camera without destroying the Publisher object.
5. Return to the application and note that your video and audio are now both being published. You have re-obtained the camera and are publishing video again.

