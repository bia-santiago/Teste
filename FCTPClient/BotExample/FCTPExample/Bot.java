package FCTPExample;

import java.awt.Color;
import java.io.File;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import FCTPClient.data.Bid;
import FCTPClient.data.Edge;
import FCTPClient.data.FCTPGraph;
import FCTPClient.dto.*;
import FCTPClient.socket.*;
import FCTPClient.utils.InstanceConverter;

public class Bot implements Runnable {

	private String name = "FCTP Client Example1";
	private String host = "atari.icad.puc-rio.br";

	HandleClient client = new HandleClient();
	List<ScoreBoard> scoreList = new ArrayList<ScoreBoard>();

	int round = 0;
	long time = 0;

	String gameStatus = "";
	String sscoreList = "";

	List<String> msg = new ArrayList<String>();
	List<String> instances = new ArrayList<String>();
	HashMap<String, HashMap<String, EdgeOwner>> winnerBids = new HashMap<String, HashMap<String, EdgeOwner>>();
	HashMap<String, List<Bid>> myBids = new HashMap<String, List<Bid>>();
	HashMap<String, Integer> instanceResults = new HashMap<String, Integer>();

	double msgSeconds = 0;
	int timer_interval = 1000;

	public Bot() {
		// Set command listener to process commands received from server
		client.addCommandListener(new CommandListener() {

			@Override
			public void receiveCommand(String[] cmd) {
				if (cmd != null)
					if (cmd.length > 0)
						try {
							// playerstatus p (param[3] countBids;state;score)
							if (cmd[0].equals("p")) {
								if (cmd.length == 4) {
								}
							} else if (cmd[0].equals("s")) {
								if (cmd.length > 1) {
									for (int i = 1; i < cmd.length; i++) {
										String[] a = cmd[i].split("#");
										if (a.length == 5)
											scoreList.add(
													new ScoreBoard(a[0], (a[1].equals("connected")), Long.parseLong(a[2]),
															convertToDouble(a[3]), convertFromString(a[4])));
									}
									sscoreList = "";
									for (ScoreBoard sb : scoreList) {
										sscoreList += sb.name + "\n";
										sscoreList += (sb.connected ? "connected" : "offline") + "\n";
										sscoreList += sb.bids + "\n";
										sscoreList += sb.score + "\n";
										sscoreList += "---\n";
									}
									scoreList.clear();
								}

								// bidlist b (params[3] instance#edge(x,y)#value
							} else if (cmd[0].equals("b")) {
								if (cmd.length > 1) {
								}
								// instancelist i (params[1] instancename)
							} else if (cmd[0].equals("i")) {

								boolean fromDecision = (gameStatus.equals("Game") && instances.size() == 0);

								for (int i = 1; i < cmd.length; i++) {
									String name = cmd[i];
									if (!instances.contains(name))
										instances.add(name);
								}

								if (fromDecision && instances.size() > 0)
									DoDecision();

								// gamestatus g (params[3] state#round#time)
							} else if (cmd[0].equals("g")) {

								if (cmd.length == 4) {
									if (!gameStatus.equals(cmd[1]))
										System.out.println("New Game Status: " + cmd[1]);

									round = Integer.parseInt(cmd[2]);
									time = Long.parseLong(cmd[3]);

									if (!gameStatus.equals(cmd[1]) && cmd[1].equals("Ready")) {
										ClearWinnerBids();
										ClearMyBids();
										instanceResults.clear();
									}
									if (!gameStatus.equals(cmd[1]) && cmd[1].equals("Game")){
										gameStatus = cmd[1];
										DoDecision();
									}

									gameStatus = cmd[1];
								}

								// result (params[2] instancename;bids
							} else if (cmd[0].equals("result")) {
								if (cmd.length == 3) {
									instanceResults.put(cmd[1], Integer.parseInt(cmd[2]));
								}
								// owner (params[6]
								// instancename;edge(x,y);owner;bids;winval;eFlow
							} else if (cmd[0].equals("owner")) {
								if (cmd.length == 7) {
									String instanceName = cmd[1];
									if (!winnerBids.containsKey(instanceName))
										winnerBids.put(instanceName, new HashMap<String, EdgeOwner>());

									String[] ed = cmd[2].split(",");
									int x = Integer.parseInt(trim(ed[0], "("));
									int y = Integer.parseInt(trim(ed[1], ")"));
									String owner = cmd[3];
									int nBids = Integer.parseInt(cmd[4]);
									double winVal = convertToDouble(cmd[5]);
									double eFlow = convertToDouble(cmd[6]);

									winnerBids.get(instanceName).put(cmd[2],
											new EdgeOwner(instanceName, x, y, owner, nBids, winVal, eFlow));
								}
							} else if (cmd[0].equals("notification")) {
								if (cmd.length > 1) {
									if (msg.size() == 0)
										msgSeconds = 0;
									msg.add(cmd[1]);
								}

							} else if (cmd[0].equals("hello")) {
								if (cmd.length > 1) {
									if (msg.size() == 0)
										msgSeconds = 0;

									msg.add(cmd[1] + " has entered the game!");
								}

							} else if (cmd[0].equals("goodbye")) {

								if (cmd.length > 1) {
									if (msg.size() == 0)
										msgSeconds = 0;

									msg.add(cmd[1] + " has left the game!");
								}

							} else if (cmd[0].equals("changename")) {
								if (cmd.length > 1) {
									if (msg.size() == 0)
										msgSeconds = 0;

									msg.add(cmd[1] + " is now known as " + cmd[2] + ".");
								}
							}

						} catch (Exception ex) {
							ex.printStackTrace();
						}

			}

		});

		// Set change status listener
		client.addChangeStatusListener(new CommandListener() {

			@Override
			public void receiveCommand(String[] cmd) {

				if (client.connected) {
					System.out.println("Connected");
					client.sendName(name);
					// client.sendColor(r, g, b);
					client.sendRequestGameStatus();
					client.sendRequestUserStatus();
				} else
					System.out.println("Disconnected");
			}
		});

		client.connect(host);
		Thread ctThread = new Thread(this);
		ctThread.start();

	}

	private String trim(String text, String trimBy) {
		int beginIndex = 0;
		int endIndex = text.length();

		while (text.substring(beginIndex, endIndex).startsWith(trimBy)) {
			beginIndex += trimBy.length();
		}

		while (text.substring(beginIndex, endIndex).endsWith(trimBy)) {
			endIndex -= trimBy.length();
		}

		return text.substring(beginIndex, endIndex);
	}

	private double convertToDouble(String value) {
		NumberFormat formatter = NumberFormat.getInstance(Locale.US);
		Number number = 0;
		try {
			number = formatter.parse(value);
		} catch (ParseException e) {
			// e.printStackTrace();
		}
		return number.doubleValue();
	}

	/**
	 * Convert a string color received from server to color
	 * 
	 * @param c
	 *            string color
	 * @return color object
	 */
	private Color convertFromString(String c) {
		String[] p = c.split("(,)|(])");

		int A = Integer.parseInt(p[0].substring(p[0].indexOf('=') + 1));
		int R = Integer.parseInt(p[1].substring(p[1].indexOf('=') + 1));
		int G = Integer.parseInt(p[2].substring(p[2].indexOf('=') + 1));
		int B = Integer.parseInt(p[3].substring(p[3].indexOf('=') + 1));

		return new Color(R, G, B, A);
	}

	/**
	 * send a message to other users
	 * 
	 * @param msg
	 *            message string
	 */
	private void sendMsg(String msg) {
		if (msg.trim().length() > 0)
			client.sendSay(msg);
	}

	/**
	 * Get current game time as string
	 * 
	 * @return current time as string
	 */
	private String GetTime() {
		int hours = (int) time / 3600;
		int minutes = ((int) time % 3600) / 60;
		int seconds = (int) time % 60;

		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	}

	/**
	 * Clear Winner Bid List
	 */
	public void ClearWinnerBids() {
		for (String s : winnerBids.keySet())
			winnerBids.get(s).clear();

		winnerBids.clear();
	}

	/**
	 * Clear Posted Bid List
	 */
	public void ClearMyBids() {
		for (String s : myBids.keySet())
			myBids.get(s).clear();

		myBids.clear();
	}

	/**
	 * Execute some decision
	 */
	private void DoDecision() {
		if (instances.size() == 0) {
			client.sendRequestInstanceList();
		} else {
			ClearMyBids();
			instanceResults.clear();

			Random rand = new Random();

			int i = 0;
			for (String instanceName : instances) {
				i++;
				String fullname = ".\\instances\\" + instanceName;
				File file = new File(fullname);
				if (file.exists()) {
					FCTPGraph problem;
					try { 
						problem = InstanceConverter.convert(fullname);

						Collection<Edge> edges = problem.getEdges();

						System.out.println(instanceName + "\t" + edges.size() + "\t"
								+ MessageFormat.format("{0,number,#.##%}", ((double) i / instances.size())));

						// escolhe precos aleatorios para as arestas
						for (Edge ei : edges) {
							if (rand.nextDouble() <= 0.1 || (round == 2 && winnerBids.containsKey(instanceName)
									&& winnerBids.get(instanceName).containsKey(ei.toString())
									&& winnerBids.get(instanceName).get(ei.toString()).owner.equals(this.name))) {

								double priceBid = 0;

								if (round == 2 && winnerBids.containsKey(instanceName) && winnerBids.get(instanceName).containsKey(ei.toString())
										&& winnerBids.get(instanceName).get(ei.toString()).owner.equals(this.name))

									priceBid = ((rand.nextDouble() + 1) / 0.3)
											* winnerBids.get(instanceName).get(ei.toString()).winVal;
								else
									priceBid = rand.nextDouble() * 10 * ei.getC();

								if (!myBids.containsKey(instanceName))
									myBids.put(instanceName, new ArrayList<Bid>());

								myBids.get(instanceName).add(new Bid(ei.getSource(), ei.getSink(), name, priceBid));

								client.sendPostBid(instanceName, ei.getSource(), ei.getSink(), priceBid);
							}

						}
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
			}
			ClearWinnerBids();
			client.sendBidsDone();
		}
	}

	/**
	 * Client thread (runnable)
	 */
	@Override
	public void run() {

		while (true) {
			try {
				Thread.sleep(timer_interval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			msgSeconds += timer_interval;

			client.sendRequestGameStatus();
			if (gameStatus.equals("Game"))
				DoDecision();
			else if (msgSeconds >= 5000) {

				System.out.println(gameStatus);
				System.out.println(GetTime());
				System.out.println("-----------------");
				System.out.println(sscoreList);

				client.sendRequestScoreboard();
			}

			if (msgSeconds >= 5000) {
				if (msg.size() > 0) {
					for (String s : msg)
						System.out.println(s);
					msg.clear();
				}
				msgSeconds = 0;
			}
		}
	}
}
