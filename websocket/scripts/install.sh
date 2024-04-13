base_dir=../..

adb connect 192.168.10.13:31551
adb -s 192.168.10.13:31551 push ${base_dir}/server/build/intermediates/dex/release/mergeDexRelease/classes.dex /data/local/tmp/scrcpy.jar
adb -s 192.168.10.13:31551 push ${base_dir}/websocket/build/intermediates/dex/release/mergeDexRelease/classes.dex /data/local/tmp/websocket.jar
adb -s 192.168.10.13:31551 shell CLASSPATH=/data/local/tmp/scrcpy.jar:/data/local/tmp/websocket.jar app_process / com.statsmind.scrcpy.websocket.DumpVideo 2.4 audio=false control=false sendFrameMeta=false sendCodecMeta=false
adb -s 192.168.10.13:31551 pull /data/local/tmp/record.h264 ~/record.h264
