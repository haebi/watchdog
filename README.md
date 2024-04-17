# watchdog
 watchdog written in java

## config log path in logback.xml before run
<property name="LOG_PATH" value="D:/log" />

## watchdog setting
config.properties
> app.look
> Process to be monitored by watchdog
> app.exec
> The process that the watchdog will run
>  (if there are multiple processes, you can specify a semicolon as the separator)
 