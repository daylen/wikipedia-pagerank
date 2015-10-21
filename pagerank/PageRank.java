package pagerank;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

enum RleState {
	TO_PAGE_ID, RUN_LENGTH, FROM_PAGE_ID
}

public class PageRank {

	private static final double DAMPING_FACTOR = 0.85;
	private static final int MAX_PAGE_ID = 48043687; // wc -l title_id_dict.txt
	private static final String BINARY_FILE_NAME = "/Users/daylenyang/Desktop/wikipedia-pagerank/data/rle.binary";
	private static final String OUTPUT_FILE_NAME = "/Users/daylenyang/Desktop/wikipedia-pagerank/data/ranking.txt";
	private static final int ACCEPTABLE_DELTA = 500;

	private boolean[] validPages = new boolean[MAX_PAGE_ID + 1];
	private int numValidPages = 0;
	private int[] outgoingDegree = new int[MAX_PAGE_ID + 1];

	private double[] prevPageRanks = new double[MAX_PAGE_ID + 1];
	private double[] currPageRanks = new double[MAX_PAGE_ID + 1];
	private double[] nextPageRanks = new double[MAX_PAGE_ID + 1];

	/**
	 * Load the RLE encoded binary data.
	 * 
	 * @return The run length encoded data.
	 * @throws IOException
	 */
	public static int[] loadBinaryFileFast() throws IOException {

		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(BINARY_FILE_NAME),
				8192 * 4));
		int[] rle = new int[in.readInt()];
		for (int i = 0; i < (rle.length / 2) * 2; i += 2) {
			long l = in.readLong();
			rle[i] = (int) (l >>> 32);
			rle[i + 1] = (int) (l & 0xFFFFFFFF);
		}
		if (rle.length % 2 != 0) {
			rle[rle.length - 1] = in.readInt();
		}
		in.close();
		return rle;
	}

	/**
	 * Fill in the validPages, numValidPages, and outgoingDegree arrays.
	 * 
	 * @param rle
	 *            The run length encoded data.
	 */
	public void populateHelperArrays(int[] rle) {
		System.out.println("Populating helper arrays...");
		RleState state = RleState.TO_PAGE_ID;
		int runLength = -1;
		for (int x : rle) {
			switch (state) {
			case TO_PAGE_ID:
				validPages[x] = true;
				state = RleState.RUN_LENGTH;
				break;
			case RUN_LENGTH:
				runLength = x;
				state = RleState.FROM_PAGE_ID;
				break;
			case FROM_PAGE_ID:
				validPages[x] = true;
				outgoingDegree[x]++;
				runLength--;
				if (runLength == 0) {
					state = RleState.TO_PAGE_ID;
				}
				break;
			default:
				break;
			}
		}
		// Calculate numValidPages
		for (boolean isValid : validPages) {
			if (isValid)
				numValidPages++;
		}
		System.out.println("Done");
		System.out.println(numValidPages + " valid pages");
	}

	public void runPageRank(int[] rle) {

		// Init PageRanks
		double initPageRank = 1.0;
		for (int i = 0; i < currPageRanks.length; i++) {
			if (validPages[i]) {
				currPageRanks[i] = initPageRank;
			}
		}

		int prIteration = 1;
		while (true) {
			System.out.println("\nStarting PageRank iteration " + prIteration);
			long start = System.currentTimeMillis();

			// Clone to previous to calculate delta later
			prevPageRanks = currPageRanks.clone();

			// Divide all PageRanks by outgoing degree

			/*
			 * Wikipedia: When calculating PageRank, pages with no outbound
			 * links are assumed to link out to all other pages in the
			 * collection. Their PageRank scores are therefore divided evenly
			 * among all other pages.
			 */
			double sinkNodePR = 0;

			for (int i = 0; i < currPageRanks.length; i++) {
				if (validPages[i]) {
					if (outgoingDegree[i] > 0)
						currPageRanks[i] /= ((double) outgoingDegree[i]);
					else
						sinkNodePR += currPageRanks[i];
				}
			}
			sinkNodePR /= numValidPages;

			// Accumulate the summation in next
			RleState state = RleState.TO_PAGE_ID;
			int currPageId = -1;
			int runLength = -1;
			for (int i = 0; i < rle.length; i++) {
				switch (state) {
				case TO_PAGE_ID:
					currPageId = rle[i];
					state = RleState.RUN_LENGTH;
					break;
				case RUN_LENGTH:
					runLength = rle[i];
					state = RleState.FROM_PAGE_ID;
					break;
				case FROM_PAGE_ID:
					nextPageRanks[currPageId] += currPageRanks[rle[i]];
					runLength--;
					if (runLength == 0) {
						state = RleState.TO_PAGE_ID;
					}
					break;
				default:
					break;
				}
			}

			// Dampen
			for (int i = 0; i < nextPageRanks.length; i++) {
				if (validPages[i]) {
					nextPageRanks[i] = (nextPageRanks[i] + sinkNodePR) * DAMPING_FACTOR + (1.0 - DAMPING_FACTOR);
				}
			}

			// Recycle generation
			currPageRanks = nextPageRanks;
			nextPageRanks = new double[currPageRanks.length];

			// Compute change statistics
			double delta = 0;
			for (int i = 0; i < currPageRanks.length; i++) {
				delta += Math.abs(currPageRanks[i] - prevPageRanks[i]);
			}

			prIteration++;

			System.out.println("Finished iteration in " + (System.currentTimeMillis() - start) / 1000.0 + " sec");
			System.out.println("DELTA: " + delta);

			if (delta < ACCEPTABLE_DELTA) {
				System.out.println("DELTA threshold crossed");
				break;
			}
		}
	}

	public void writePageRanks() throws IOException {
		System.out.println("Saving");
		PrintWriter writer = new PrintWriter(OUTPUT_FILE_NAME, "UTF-8");
		for (int i = 0; i < currPageRanks.length; i++) {
			if (validPages[i])
				writer.write(i + " " + currPageRanks[i] + "\n");
		}
		writer.close();
		System.out.println("Saved to disk");
	}

	public static void main(String[] args) throws IOException {
		long start = System.currentTimeMillis();
		System.out.println("Loading data...");
		int[] rle = loadBinaryFileFast();
		System.out.println("Done. Took " + (System.currentTimeMillis() - start) / 1000.0 + " sec");

		PageRank pr = new PageRank();
		pr.populateHelperArrays(rle);
		pr.runPageRank(rle);
		pr.writePageRanks();
	}

}
