import java.io.*;
import java.util.*;
import org.jibble.pircbot.*;

public class WuBotMain {
    
    public static void main(String[] args) throws Exception {
        
        Properties p = new Properties();
        p.load(new FileInputStream(new File("./config.ini")));
        
        String server = p.getProperty("Server", "localhost");
        String channel = p.getProperty("Channel", "#test");
        String nick = p.getProperty("Nick", "WuBot");
        String joinMessage = p.getProperty("JoinMessage", "This channel is logged.");
        
        File outDir = new File(p.getProperty("OutputDir", "./output/"));
	File rawDir = new File(p.getProperty("RawDir", "./output/"));

        outDir.mkdirs();
        if (!outDir.isDirectory()) {
            System.out.println("Cannot make output directory (" + outDir + ")");
            System.exit(1);
        }

        rawDir.mkdirs();
        if (!rawDir.isDirectory()) {
            System.out.println("Cannot make raw output directory (" + rawDir + ")");
            System.exit(1);
        }

        WuBot bot = new WuBot(nick, outDir, rawDir, channel, joinMessage);

        bot.copy(new File("html/header.inc.php"), new File(outDir, "header.inc.php"));
        bot.copy(new File("html/footer.inc.php"), new File(outDir, "footer.inc.php"));
        bot.copy(new File("html/index.php"), new File(outDir, "index.php"));
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outDir, "config.inc.php")));
        writer.write("<?php");
        writer.newLine();
        writer.write("    $server = \"" + server + "\";");
        writer.newLine();
        writer.write("    $channel = \"" + channel + "\";");
        writer.newLine();
        writer.write("    $nick = \"" + nick + "\";");
        writer.newLine();
        writer.write("?>");
        writer.flush();
        writer.close();

        bot.connect(server);
        bot.joinChannel(channel);
	System.out.println("*** Joined channel to log");
    }
    
}
