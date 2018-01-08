import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Program which returns tweet statistics of a user based on the 2016 elections.
 * Output consists of the total amount of tweets analyzed and relevant
 * statistics to check mentions and candidate exposure.
 * 
 * The program takes an input through the Java command line, in which an array
 * of strings is given consisting of user handles for Twitter without their @.
 * 
 * @author David Alderliesten
 *
 */
public class TweetExposure {
	private static ConfigurationBuilder configBuild;
	private static Twitter twitterInstance;
	private static BufferedWriter outStream;

	public static void main(String[] args) throws TwitterException, IOException, ParseException {
		// Configure the credentials for the environment.
		configureEnvironment();

		// Configure the credentials for the twitter access.
		configureTwitter();

		// Configure the information for the file writer to store the results.
		configureWriter();

		// Performing the analysis for each given user and populating their statuses and
		// analysis.
		for (String currentUser : args) {
			// Getting the statuses for the given user at the given page number and storing
			// them in an ArrayList.
			ArrayList<Status> statuses = populateUserStatuses(currentUser, 1);

			// Filtering the statuses to eliminate the early or late statuses beyond the
			// scope of this investigation.
			statuses = filterStatuses(statuses);

			// Analyzing the statuses for data output.
			analyzeStatuses(statuses);
		}
	}

	/**
	 * Set-up the configuration information, including the API key information and
	 * the builder required.
	 */
	private static void configureEnvironment() {
		// Using a ConfigurationBuilder to provide environment variables.
		configBuild = new ConfigurationBuilder();

		// Assigning public stream API keys as required.
		configBuild.setDebugEnabled(true).setOAuthConsumerKey("juoad4e14U28YDpfJRhSZoxWW")
				.setOAuthConsumerSecret("QOFfRAS7bnkSIwrQku3rmyjNNlOn4mkwYDrYHEJKjoKtovmZRv")
				.setOAuthAccessToken("934411977224523776-ZjWMAdLREIhRM76bYwIYfsSLC7ICKNn")
				.setOAuthAccessTokenSecret("IPOFo8EHhBPb8HOI6RkpBYhwqWv4sE3YcQMysZivPl0BX");
	}

	/**
	 * Configures the twitter instance for page loading.
	 */
	private static void configureTwitter() {
		// Set the instance using the credentials from the configuration builder.
		twitterInstance = new TwitterFactory(configBuild.build()).getInstance();
	}

	/**
	 * Writes content to the command line and to the file in an identical fashion.
	 * 
	 * @param toWrite
	 *            The content to write.
	 * @throws IOException
	 *             Exception during file writing.
	 */
	private static void writeContent(String[] toWrite) throws IOException {
		for (String currentString : toWrite) {
			// Printing the current string to the command line.
			System.out.println(currentString);

			// Writing the current line to the file.
			outStream.write(currentString);

			// Adding a new line to the file output buffer.
			outStream.newLine();
		}

		// Flushing the stream to write the report header.
		outStream.flush();
	}

	/**
	 * Configuring the writer to store the output.
	 * 
	 * @throws IOException
	 *             Exception if file issues persists.
	 */
	private static void configureWriter() throws IOException {
		// Checking if the file to be written to exists.
		File toWrite = new File("C:\\Users\\David\\eclipse-workspace\\TweetLocations\\target\\results.txt");

		// Checking if the file exists.
		if (toWrite.createNewFile()) {
			System.out.println("A file has been created for the storing of the data.");
		}

		// Making a BW upon a file writer, output to results.txt.
		outStream = new BufferedWriter(new FileWriter(toWrite, true));
	}

	/**
	 * Populates an array with all the statuses of a user.
	 * 
	 * @param passedUser
	 *            The desired user handle to check.
	 * @param passedPage
	 *            The desired page to start at.
	 * @return An array with all the statuses of the passedUser handle starting at
	 *         the passedPage.
	 * @throws TwitterException
	 *             An exception of Twitter4j in the case of an error.
	 * @throws IOException
	 *             An exception as a result of a failed file write.
	 */
	private static ArrayList<Status> populateUserStatuses(String passedUser, int passedPage)
			throws TwitterException, IOException {
		// Creating an ArrayList for the storing of statuses.
		ArrayList<Status> statuses = new ArrayList<Status>();

		// Ugly infinite loop which iterates until all desired pages have been iterated
		// through to evade Twitter's 5-day GET limit.
		while (true) {
			// Getting current size of statuses.
			int size = statuses.size();

			// Incrementing page viewed.
			Paging currentPage = new Paging(passedPage++, 100);

			// Add all statuses on this page.
			statuses.addAll(twitterInstance.getUserTimeline(passedUser, currentPage));

			// When all statuses have been found based on initial tweet set size, break form
			// the infinite loop.
			if (statuses.size() == size) {
				break;
			}
		}

		// Writing a report card with user name and handle.
		writeContent(new String[] {
				statuses.get(0).getUser().getName() + "(@" + statuses.get(0).getUser().getScreenName() + ")",
				"------------------------------------------------" });

		// Returning the found statuses.
		return statuses;
	}

	/**
	 * Filters the tweets to only contain tweets between the election date(s) and
	 * parameters, based off of the earliest candidate announcement of Clinton to
	 * the election night.
	 * 
	 * @param passedStatuses
	 *            The statuses for filtering.
	 * @return A list of statuses within the given time parameter.
	 * @throws ParseException
	 *             Exception in which the date parsers failed.
	 */
	private static ArrayList<Status> filterStatuses(ArrayList<Status> passedStatuses) throws ParseException {
		// Establishing an acceptable date format for tweet comparison.
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

		// Establishing the earliest allowed date.
		Date earlyFilter = dateFormat.parse("11/04/2015");

		// Establishing the latest allowed date.
		Date lateFilter = dateFormat.parse("09/11/2016");

		// Creating an empty ArrayList for the filtered statuses.
		ArrayList<Status> filteredList = new ArrayList<Status>();

		// Getting all the given statuses.
		for (Status currentStatus : passedStatuses) {
			// Setting the current status' creation moment to the simple date format.
			String currentDateString = dateFormat.format(currentStatus.getCreatedAt());
			Date currentDate = dateFormat.parse(currentDateString);

			// Adding the status iff it occurred between 12 April 2015 (Clinton candidacy
			// announcement) and 9 November 2016 (end of election night).
			if (currentDate.after(earlyFilter) && currentDate.before(lateFilter)) {
				// Adding a matching statuses to the filter list.
				filteredList.add(currentStatus);
			}
		}

		// Returning the filtered statuses.
		return filteredList;
	}

	/**
	 * Analyzes the found statuses for relevant data.
	 * 
	 * @param passedStatuses
	 *            The found statuses for analysis.
	 * @throws IOException
	 *             Error when writing to the file.
	 */
	private static void analyzeStatuses(ArrayList<Status> passedStatuses) throws IOException {
		// Instantiating all variables required to track tweet statuses.
		int totalTrump = 0, totalClinton = 0;
		int totalExactTrump = 0, totalExactClinton = 0;
		int allCandidates = 0;

		// Iterate over all statuses to identify patterns.
		for (Status currentStatus : passedStatuses) {
			// Getting the text of the current tweet and setting it to lower case for better
			// approximation.
			String currentText = currentStatus.getText().toLowerCase();

			// Create variables to track the Trump or Clinton checks for validation.
			boolean containsTrump = false, containsClinton = false;

			// Checking if a tweet contains a mention of the name Clinton.
			if (currentText.contains("clinton")) {
				totalClinton += 1;
				containsClinton = true;
			}

			// Checking if a tweet contains a mention of the exact name Hillary Clinton, to
			// avoid conflicts with Bill or Chelsea Clinton.
			if (currentText.contains("hillary clinton") || currentText.contains("candidate clinton")) {
				totalExactClinton += 1;
				containsClinton = true;
			}

			// Checking if a tweet contains a mention of the name Trump.
			if (currentText.contains("trump")) {
				totalTrump += 1;
				containsTrump = true;
			}

			// Checking if a tweet contains a mention of the exact name Donald or Candidate
			// Trump, to avoid conflicts with his children or wife.
			if (currentText.contains("donald trump") || currentText.contains("candidate trump")) {
				totalExactTrump += 1;
				containsTrump = true;
			}

			// Checking if a tweet mentions both candidates.
			if (containsTrump && containsClinton) {
				allCandidates += 1;
			}
		}

		// Displaying the results of the analysis and writing them out, and closing the
		// report.
		writeContent(new String[] { "Total Clinton Tweets: " + totalClinton,
				"Total Exact Clinton Tweets: " + totalExactClinton, "Total Trump Tweets: " + totalTrump,
				"Total Exact Trump Tweets: " + totalExactTrump, "Total Tweets with Both Candidates: " + allCandidates,
				"Total Tweets Analyzed: " + passedStatuses.size(),
				"------------------------------------------------" });
	}
}
