import org.jibble.pircbot.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.text.SimpleDateFormat;

public class WuBot extends PircBot {
	private static char switchChar = '@';

	private static final Pattern urlPattern = Pattern.compile("(?i:\\b((http|https|ftp|irc)://[^\\s]+))");
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("[HH:mm]");
	
	public static final String GREEN = "irc-green";
	public static final String BLACK = "irc-black";
	public static final String BROWN = "irc-brown";
	public static final String NAVY = "irc-navy";
	public static final String BRICK = "irc-brick";
	public static final String RED = "irc-red";

	private File outDir;
	private File rawDir;
	private String joinMessage;
	private String loggedChannel;

	private boolean writeLogs = true;

	private final long bootTime = System.currentTimeMillis();
	private Hashtable seenKids = new Hashtable();
	private Hashtable karmaKids = new Hashtable();

	WuBot(String name, File outDir, File rawDir, String channel, String joinMessage) {
		this.setName(name);
		this.setLogin(name);
		this.setAutoNickChange(true);
		this.outDir = outDir;
		this.rawDir = rawDir;
		this.joinMessage = joinMessage;
		this.loggedChannel = channel;
	}

	public static void main(String [] args) {
	}
	
	public void append(String color, String line) {
	    if(!writeLogs) return;
	    
	    line = Colors.removeFormattingAndColors(line);
	    String rawLine = line;
	    
	    line = line.replaceAll("&", "&amp;");
	    line = line.replaceAll("<", "&lt;");
	    line = line.replaceAll(">", "&gt;");
	    
	    Matcher matcher = urlPattern.matcher(line);
	    line = matcher.replaceAll("<a href=\"$1\">$1</a>");
	    
	            
	    try {
	        Date now = new Date();
	        String date = DATE_FORMAT.format(now);
	        String time = TIME_FORMAT.format(now);
	        File file = new File(outDir, date + ".log");
		File rawFile = new File(rawDir, date + ".txt");
	        BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
		BufferedWriter rawWriter = new BufferedWriter(new FileWriter(rawFile,true));
	        String entry = "<span class=\"irc-date\">" + time + "</span> <span class=\"" + color + "\">" + line + "</span><br />";
		String rawEntry = time + ' ' + rawLine;
	        writer.write(entry);
	        writer.newLine();
	        writer.flush();
	        writer.close();
		rawWriter.write(rawEntry);
		rawWriter.newLine();
		rawWriter.flush();
		rawWriter.close();
	    }
	    catch (IOException e) {
	        System.out.println("Could not write to log: " + e);
	    }
	}
	
	public void onAction(String sender, String login, String hostname, String target, String action) {
	    append(BRICK, "* " + sender + " " + action);
	}
	
	public void onJoin(String channel, String sender, String login, String hostname) {
	    if(loggedChannel.equalsIgnoreCase(channel))
	        append(GREEN, "* " + sender + " (" + login + "@" + hostname + ") has joined " + channel);
	    else return;

	    if (sender.equals(getNick())) {
	        sendNotice(channel, joinMessage);
	    }
	    else {
	        sendNotice(sender, joinMessage);
	    }
	}
	
	public void onMode(String channel, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
		if(loggedChannel.equalsIgnoreCase(channel))
	    append(GREEN, "* " + sourceNick + " sets mode " + mode);
	}
	
	public void onNickChange(String oldNick, String login, String hostname, String newNick) {
	    append(GREEN, "* " + oldNick + " is now known as " + newNick);
	}
	
	public void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice) {
	    append(BROWN, "-" + sourceNick + "- " + notice);
	}
	
	public void onPart(String channel, String sender, String login, String hostname) {
		if(loggedChannel.equalsIgnoreCase(channel))
	    append(GREEN, "* " + sender + " (" + login + "@" + hostname + ") has left " + channel);
	}
	
	public void onPing(String sourceNick, String sourceLogin, String sourceHostname, String target, String pingValue) {
	    append(RED, "[" + sourceNick + " PING]");
	}
	
	public void onPrivateMessage(String sender, String login, String hostname, String message) {
	     append(BLACK, "<- *" + sender + "* " + message);
	}
	
	public void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
	    append(NAVY, "* " + sourceNick + " (" + sourceLogin + "@" + sourceHostname + ") Quit (" + reason + ")");
	}
	
	public void onTime(String sourceNick, String sourceLogin, String sourceHostname, String target) {
	    append(RED, "[" + sourceNick + " TIME]");
	}
	
	public void onTopic(String channel, String topic, String setBy, long date, boolean changed) {
	    if(!loggedChannel.equalsIgnoreCase(channel)) return;

            if (changed) {
                 append(GREEN, "* " + setBy + " changes topic to '" + topic + "'");
            }
            else {
                append(GREEN, "* Topic is '" + topic + "'");
                append(GREEN, "* Set by " + setBy + " on " + new Date(date));
            }
	}
	
	public void onVersion(String sourceNick, String sourceLogin, String sourceHostname, String target) {
	    append(RED, "[" + sourceNick + " VERSION]");
	}
	
	public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
	    if(loggedChannel.equalsIgnoreCase(channel))
	        append(GREEN, "* " + recipientNick + " was kicked from " + channel + " by " + kickerNick);
	    if (recipientNick.equalsIgnoreCase(getNick())) {
	        joinChannel(channel);
	    }
	}
	
	public void onDisconnect() {
	    append(NAVY, "* Disconnected.");
	    while (!isConnected()) {
	        try {
	            reconnect();
	        }
	        catch (Exception e) {
	            try {
	                Thread.sleep(10000);
	            }
	            catch (Exception anye) {
	                // Do nothing.
	            }
	        }
	    }
	}
	
	public static void copy(File source, File target) throws IOException {
	    BufferedInputStream input = new BufferedInputStream(new FileInputStream(source));
	    BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(target));
	    int bytesRead = 0;
	    byte[] buffer = new byte[1024];
	    while ((bytesRead = input.read(buffer, 0, buffer.length)) != -1) {
	        output.write(buffer, 0, bytesRead);
	    }
	    output.flush();
	    output.close();
	    input.close();
	}
	
	
	protected void onMessage(String channel, String sender, String login, String hostname, String message) {
		String [] tokens = message.split("\\s+");

		if(loggedChannel.equalsIgnoreCase(channel) && !message.startsWith(Character.toString(switchChar))) 
			append(BLACK, "<" + sender + "> " + message);

		Date now = new Date();
		String stamp = DATE_FORMAT.format(now) + " " + TIME_FORMAT.format(now);
		seenKids.put(sender.toLowerCase(),stamp + "|" + channel + "|<" + sender + "> " + message);

		if(karmaKids.containsKey(sender.toLowerCase())) {
			((Karma) karmaKids.get(sender.toLowerCase())).giveCredit();
		} else {
			karmaKids.put(sender.toLowerCase(), new Karma());
		}

		// Some karma code
		if(tokens.length >= 1) {
			String tmp = tokens[0].substring(tokens[0].length() - 2);
			if(tmp.compareTo("++") == 0) {
				String nick = tokens[0].substring(0, tokens[0].length() - 2);
				if(!sender.equalsIgnoreCase(nick) && karmaKids.containsKey(nick.toLowerCase())) {
					((Karma) karmaKids.get(nick.toLowerCase())).increment();
				}
			}
		}
		
		if(!message.startsWith(Character.toString(switchChar))) {
			return;
		}

		String command = tokens[0].substring(1);
		
		if(command.length() == 0) return;
		
		String msgOut = new String();
		if(command.equalsIgnoreCase("roll")) {
			msgOut = rollDice(tokens);
		} else if(command.equalsIgnoreCase("help")) {
			msgOut = joinMessage;
		} else if(command.equalsIgnoreCase("seen")) {
			msgOut = seenNick(tokens);
		} else if(command.equalsIgnoreCase("uptime")) {
			msgOut = uptime();
		} else if(command.equalsIgnoreCase("karma")) {
			if(tokens.length >= 2 && sender.equalsIgnoreCase(tokens[1]))
				msgOut = getKarma(sender, true);
			else if(tokens.length >= 2)
				msgOut = getKarma(tokens[1], false);
			else
				msgOut = getKarma(sender, true);
		} else if(command.equalsIgnoreCase("fortune")) {
			msgOut = fortune();
		} else if(command.equalsIgnoreCase("log")) {
			msgOut = setLogs(tokens);
		}
		
		if(msgOut.length() > 0) {
			this.sendMessage(channel, sender + ": " + msgOut);
		}
	}
	
	protected void onInvite(String targetNick, String sourceNick, String sourceLogin, String sourceHostname, String channel) {
		this.joinChannel(channel);
	}

	private String getKarma(String nick, boolean you) {
		if(!karmaKids.containsKey(nick.toLowerCase()))
			if(!you) return nick + " does not believe in karma.";
			else return "You do not believe in karma.";

		Karma k = (Karma) karmaKids.get(nick.toLowerCase());
		if(!you) return nick + "'s karma is " + k.getKarma();
		else return "Your karma is " + k.getKarma();
	}

	private String setLogs(String [] tokens) {
		String usage = new String("Usage: @log [on|off]");
		if(tokens.length > 2) return usage;

		if(tokens.length == 1) return writeLogs ? "Logs are on." : "Logs are off.";

		String arg = tokens[1];

		if(arg.compareToIgnoreCase("on") == 0) {
			writeLogs = true;
			return "Logs are turned on.";
		}

		if(tokens[1].compareToIgnoreCase("off") == 0) {
			writeLogs = false;
			return "Logs are turned off.";
		}
		
		return usage;
	}
	
	private String rollDice(String [] tokens) {
		int dice, sides, modifier;
		String usage = new String("Usage: @roll <dice>d<sides>");
		
		if(tokens.length != 2)
			return usage;
		
		String subArgs[] = tokens[1].split("[dD]");
		
		try {
			if(subArgs.length == 2 && !subArgs[0].equalsIgnoreCase("")) {
				String preparse = subArgs[1];
				String splitString[] = preparse.split("[+-]");
				if(splitString.length == 2 && !splitString[1].equalsIgnoreCase("")) {
					System.out.println("Matched a plus with non-empty");
					dice = Math.abs( Integer.parseInt(subArgs[0]) );
					sides = Math.abs( Integer.parseInt(splitString[0]) );
					modifier = Math.abs( Integer.parseInt(splitString[1]) );
					if(preparse.indexOf('-') != -1)
						modifier = -modifier;
					else
						System.out.println("Preparse has a plus: " + preparse);
				} else {
					System.out.println("Normal roll");
					dice = Math.abs( Integer.parseInt(subArgs[0]) );
					sides = Math.abs( Integer.parseInt(subArgs[1]) );
					modifier = 0;
				}
			} else {
				return usage;
			}
		} catch(Exception e) {
			System.out.println("Great failure.");
			return usage;
		}
		
		if(dice <= 0 || dice > 10)
			return "Dice must be in range of [1,10]";
		if(sides <= 1 || sides > 100)
			return "Sides must be in range of [2, 100]";
		
		Random g = new Random();
		int sum = modifier;
		String out = new String();
		for(int i = 0; i < dice; i++) {
			int r = g.nextInt(sides) + 1;
			sum += r;
			out += r;
			if(i + 1 < dice) out+=',';
			out += ' ';
			g = new Random();
		}
		if(dice > 1 || modifier != 0) {
			out += " (Sum: " + sum + ")";
		}
		return out;
	}

	public String seenNick(String [] tokens) {
		String usage = "USAGE: seen <nick>";
		if(tokens.length != 2) return usage;
		String nick = tokens[1];

		System.out.println("In the seen func");

		if(!seenKids.containsKey(nick.toLowerCase()))	return "I have not seen " + nick;

		System.out.println("Apparently we've seen this dood " + nick );

		String [] data = ((String) seenKids.get(nick.toLowerCase())).split("[|]");
		System.out.println("parsed data concerning " + nick );

		for(int i = 0; i < data.length; i++) 
			System.out.print(data[i] + " ");
		System.out.println();
		if(data.length != 3) return "Error, Will Robinson!";
		return "Last seen at " + data[0] + " in " + data[1] + " saying: " + data[2];
	}

	public String uptime() {
		long diff = System.currentTimeMillis() - bootTime;
		int minutes = (int) (diff / 60000);
		int hours = minutes / 60;
		minutes = minutes % 60;
		int days = hours / 24;
		hours = hours % 24;
		return "Up " + days + " days, " + hours + " hours, " + minutes + " minutes";
	}

	public String fortune() {
		try {
			String command = "fortune fortunes";
			Process child = Runtime.getRuntime().exec(command);
			InputStream in = child.getInputStream();
			Scanner input = new Scanner(in);
			StringBuffer s = new StringBuffer();

			if(input.hasNext()) { s.append(input.next()); }

			while(input.hasNext()) {
				s.append(" " + input.next());
			}
			return s.toString();
		}
		catch(Exception e) {
			return "No fortune for you.";
		}
	}
}
