package watchdog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Config {
  public String desc;
  public String look;
  public String exec;

  public Config(String desc, String look, String exec) {
    this.desc = desc;
    this.look = look;
    this.exec = exec;
  }
}


public class WatchDog {

  static ArrayList<Config> config = new ArrayList<Config>();
  static Logger logger = LoggerFactory.getLogger(WatchDog.class);

  public static void main(String[] args) {

    logger.info("watchdog start");

    Properties prop = new Properties();
    InputStream input = null;

    try {
      input = WatchDog.class.getResourceAsStream("/config.properties");

      // prevent broke korean character
      InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
      prop.load(reader);

    } catch (Exception e) {
      logger.error(e.toString());
    }

    config.add(new Config(prop.getProperty("app.desc").replace("\"", ""),
        prop.getProperty("app.look").replace("\"", ""),
        prop.getProperty("app.exec").replace("\"", "")));

    // main loop
    while (true) {
      try {
        // prevent high cpu usage
        Thread.sleep(1000);
        // run process if not runnning
        if (!CheckProcessRunning(config.get(0).look)) {
          // exec
          String execs[] = config.get(0).exec.split(";");
          for (int i = 0; i < execs.length; i++) {
            // execute process
            logger.info("Execute : {}", execs[i]);
            RunProcessSync(execs[i]);
          }
        }
      } catch (Exception e) {
        logger.error(e.toString());
        logger.error("watchdog terminate");
        System.exit(0);
      }
    }
  }

  // Check process runnning
  private static boolean CheckProcessRunning(String ProcessName) throws IOException {
    boolean isRunning = false;

    InputStream inputStream = RunProcess("tasklist.exe");

    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

    String line;
    while ((line = reader.readLine()) != null) {
      // logger_1.info(line);
      if (line.contains(ProcessName)) {
        isRunning = true;
        break;
      }
    }

    return isRunning;
  }

  // Run process async
  private static InputStream RunProcess(String processName) throws IOException {
    ProcessBuilder processBuilder = new ProcessBuilder(processName);
    Process process = processBuilder.start();
    return process.getInputStream();
  }

  // Run process sync
  private static void RunProcessSync(String processName) {
    try {
      ProcessBuilder processBuilder = new ProcessBuilder(processName);
      Process process = processBuilder.start();

      // 비동기적으로 프로세스 출력을 읽기
      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        try {
          BufferedReader reader =
              new BufferedReader(new InputStreamReader(process.getInputStream()));
          String line;
          while ((line = reader.readLine()) != null) {
            // 읽은 라인 출력
            logger.info(line);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      });

      // 작업이 완료될 때 까지 대기
      try {
        future.get();
      } catch (ExecutionException e) {
        e.printStackTrace();
      }

      // 프로세스 종료
      process.destroy();

    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }

}
