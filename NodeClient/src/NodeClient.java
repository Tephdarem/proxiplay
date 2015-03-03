import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONArray;
import org.json.JSONObject;

public class NodeClient {

	private int actualNodeID = 0, actualRoomID = 0, actualVol = 0,
			serverPort = 80, refreshRate = 100;
	private String serverIp, serverVolumeData = "", streamUrl = "";
	private Process qq;
	private long beforeCollecData = 0;

	public NodeClient() {

		System.out.println("Starting client...");
		initialize();

		// Set volume on PI to 0%
		try {
			qq = Runtime.getRuntime().exec("amixer cset numid=1 0%");
		} catch (IOException e2) {
			System.out.println("Error: failed to set Amixer volume to 0%");
		}
		try {
			qq.waitFor();
		} catch (InterruptedException e2) {
			System.out
					.println("Error: interrupted while setting Amixer volume to 0%.");
		}
		System.out.println("Amixer volume set to 0%");

		try {
			qq = Runtime.getRuntime().exec("amixer set PCM mute");
		} catch (IOException e1) {
			System.out.println("Error: failed to set Amixer to mute");
		}
		try {
			qq.waitFor();
		} catch (InterruptedException e1) {
			System.out.println("Error: interrupted while muting Amixer");
		}
		System.out.println("Amixer mute.");

		if (streamUrl != null && streamUrl != "") {
			System.out.println("Starting stream...");
			// Start streaming from VLC
			try {
				qq = Runtime.getRuntime().exec("cvlc " + streamUrl);
			} catch (IOException e) {
				System.out.println("Error: failed to start streaming with VLC");
			}
			System.out.println("Stream started.");
		} else {
			System.out.println("Stream URL malformed");
		}

	}

	private void initialize() {
		System.out.println("Initializing.");
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader("src/setup.json"));
		} catch (FileNotFoundException e) {
			System.out.println("Error: Setup file not found - setup.json");
		}
		System.out.println("Setup file loaded.");
		String str, data = "";
		try {
			if (reader != null) {
				while ((str = reader.readLine()) != null) {
					data += str;
				}
			}
		} catch (IOException e) {
			System.out.println("Error: Failed to read data from setup file.");
		}
		try {
			reader.close();
		} catch (IOException e) {
			System.out.println("Error: Failed to close setup file.");
		}
		JSONObject jo = new JSONObject(data);

		if (jo != null && jo.getInt("nodeId") >= 0 && jo.getInt("roomId") >= 0
				&& jo.getInt("vol") >= 0 && jo.getInt("vol") <= 100
				&& jo.getString("serverIp") != null
				&& !jo.getString("serverIp").equals("")
				&& jo.getString("serverVolumeData") != null
				&& !jo.getString("serverVolumeData").equals("")
				&& jo.getInt("serverPort") >= 80
				&& jo.getInt("serverPort") <= 9999
				&& jo.getInt("refreshRate") > 0
				&& jo.getString("streamUrl") != null
				&& !jo.getString("streamUrl").equals("")) {

			System.out.println("JSON perfect");
			actualNodeID = jo.getInt("nodeId");
			actualRoomID = jo.getInt("roomId");
			actualVol = jo.getInt("vol");
			serverPort = jo.getInt("serverPort");
			serverIp = jo.getString("serverIp");
			serverVolumeData = jo.getString("serverVolumeData");
			refreshRate = jo.getInt("refreshRate");
			streamUrl = jo.getString("streamUrl");

		} else {
			System.out
					.println("Error: Something went wrong duing the initialization of the setup-file.\nI can't read it!");
		}

	}

	protected void receiveData() {
		// Connect to server - receive JSON data
		URL oracle = null;
		try {
			oracle = new URL("http://" + serverIp + ":" + serverPort + "/"
					+ serverVolumeData);
		} catch (MalformedURLException e) {
			System.out.println("Error: Malformed URL to server");
		}
		System.out.println("Starting connection with server.");

		// Creation of variables before entering while-loop.
		URLConnection yc = null;
		BufferedReader in = null;
		String inputLine, data = "";
		JSONArray jarray;
		JSONObject jo;
		int newVol = 0;

		if (oracle != null) {
			// Start pulling data
			while (true) {
				beforeCollecData = System.currentTimeMillis();
				data = "";
				try {
					yc = oracle.openConnection();
				} catch (IOException e) {
					System.out.println("Error: failed to connect to server");
				}
				if (yc != null) {
					try {
						in = new BufferedReader(new InputStreamReader(
								yc.getInputStream()));
					} catch (IOException e) {
						System.out
								.println("Error: Failed to receive stream from server");
					}

					if (in != null) {
						try {
							while ((inputLine = in.readLine()) != null)
								data += inputLine;
						} catch (IOException e) {
							System.out
									.println("Error: failed to read stream from server");
						}
						try {
							in.close();
						} catch (IOException e) {
							System.out
									.println("Error: failed to close reading stream");
						}
					}

					// Create JSON object from server data

					jarray = new JSONArray(data);
					newVol = 0;
					// System.out.println("Read volume data from server, jarray.length="
					// + jarray.length());
					for (int i = 0; i < jarray.length(); i++) {
						// Create object
						jo = (JSONObject) jarray.get(i);

						// Check if object is valid
						if (jo != null && jo.getInt("nodeId") == actualNodeID
								&& jo.getInt("roomId") == actualRoomID
								&& jo.getInt("vol") >= 0) {
							newVol = jo.getInt("vol");
							System.out.println(jo.toString());
							break;
						}
					}

					// Set volume
					// setVolume(newVol);
					if (newVol == 100) {
						setVolumeFull();
					} else if (newVol == 0) {
						setVolumeZero();
					}
				}

				// Sleep the loop.
				try {
					Thread.sleep(refreshRate);
					long afterProgramSleep = System.currentTimeMillis();
					if (beforeCollecData > 0 && afterProgramSleep > 0) {
						System.out
								.println("Total system run time: "
										+ (afterProgramSleep - beforeCollecData)
										+ "ms");
					} else {
						System.out.println("Time measurement failed.");
					}
				} catch (InterruptedException e) {
					System.out.println("Error: Failed to sleep.");
				}
			}
		}

	}

	private void setVolumeFull() {

		try {
			qq = Runtime.getRuntime().exec("amixer set PCM unmute");
		} catch (IOException e) {
			System.out.println("Error: Failed to mute Amixer.");
		}
		try {
			qq.waitFor();
		} catch (InterruptedException e) {
			System.out.println("Error: interrupted while muting Amixer.");
		}

		try {
			qq = Runtime.getRuntime().exec("amixer cset numid=1 90%");
		} catch (IOException e) {
			System.out.println("Error: Failed to set amixer volume to 90%");
		}

		try {
			qq.waitFor();
		} catch (InterruptedException e) {
			System.out
					.println("Error: interrupted while setting amixer volume to 90%");
		}

	}

	private void setVolumeZero() {
		try {
			qq = Runtime.getRuntime().exec("amixer cset numid=1 0%");
		} catch (IOException e) {
			System.out.println("Error: failed to set Amixer volume to 0%");
		}
		try {
			qq.waitFor();
		} catch (InterruptedException e) {
			System.out
					.println("Error: interrupted while setting Amixer volume to 0%");
		}
		System.out.println("Amixer volume set to 0%");

		try {
			qq = Runtime.getRuntime().exec("amixer set PCM mute");
		} catch (IOException e) {
			System.out.println("Error: Failed to mute Amixer");
		}
		try {
			qq.waitFor();
		} catch (InterruptedException e) {
			System.out.println("Error: interrupted while muting Amixer");
		}

	}

	@SuppressWarnings("unused")
	private void setVolume(int newVol) throws IOException, InterruptedException {
		int scaledNewVol = newVol - 50;
		// Set new volume from received data
		if (scaledNewVol >= 0 && scaledNewVol <= 100) {
			if (scaledNewVol > actualVol) {

				System.out.println("Fade in");
				// fade in

				qq = Runtime.getRuntime().exec("amixer set PCM unmute");
				qq.waitFor();
				System.out.println("Amixer unmute.");

				while (actualVol < scaledNewVol) {
					Process qq = Runtime.getRuntime().exec(
							"amixer cset numid=1 " + actualVol + "%");
					qq.waitFor();
					actualVol++;
				}
			} else if (scaledNewVol < actualVol) {

				System.out.println("Fade out");
				// fade out

				while (actualVol > scaledNewVol) {
					Process qq = Runtime.getRuntime().exec(
							"amixer cset numid=1 " + actualVol + "%");
					qq.waitFor();
					actualVol--;
				}

				qq = Runtime.getRuntime().exec("amixer set PCM mute");
				qq.waitFor();
				System.out.println("Amixer mute.");

			} else {
				// else do nothing, because newVol = actualVol
				System.out.println("No change in volume");
			}
			// make sure actual vol is as it should be
			actualVol = scaledNewVol;
		}
	}

}
