package pagerank;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

enum RleState {
	TO_PAGE_ID, RUN_LENGTH, FROM_PAGE_ID
}

public class PageRank {

	private static final double DAMPING_FACTOR = 0.85;
	private static final int MAX_PAGE_ID = 48043687; // wc -l title_id_dict.txt
	private static final String BINARY_FILE_NAME = "/Users/daylenyang/Desktop/wikipedia-pagerank/data/rle.binary";

	private boolean[] validPages = new boolean[MAX_PAGE_ID + 1];
	private int numValidPages = 0;
	private int[] outgoingDegree = new int[MAX_PAGE_ID + 1];

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
			
			System.out.println("Sum of PageRank of sink nodes: " + sinkNodePR);

			// Accumulate the summation
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

			// Compute change statistics
			double delta = 0;
			for (int i = 0; i < nextPageRanks.length; i++) {
				delta += Math.abs(nextPageRanks[i] - currPageRanks[i]);
			}

			// Recycle generation
			currPageRanks = nextPageRanks.clone();
			nextPageRanks = new double[currPageRanks.length];

			System.out.println("Finished iteration in " + (System.currentTimeMillis() - start) / 1000.0 + " sec");
			System.out.println("DELTA: " + delta);

			System.out.println("Top page IDs:");
			System.out.println(Arrays.toString(topIndices(currPageRanks, 20)));

			prIteration++;
			
			sanityCheck();
		}
	}
	
	public void sanityCheck() {
		for (int i = 0; i < validPages.length; i++) {
			if (validPages[i] == false) {
				try {
					assert(outgoingDegree[i] == 0);
					assert(currPageRanks[i] < 0.001);
					assert(nextPageRanks[i] < 0.001);
				} catch (AssertionError e) {
					System.out.println("Whoa! Sanity check failed at index " + i);
					System.out.println("Outgoing degree " + outgoingDegree[i]);
					System.out.println("PageRanks " + currPageRanks[i] + " " + nextPageRanks[i]);
					throw e;
				}
			} else {
//				assert(currPageRanks[i] <= 1);
			}
		}
	}

	public static int[] topIndices(double[] pageRanks, int quantity) {
		double[] sorted = pageRanks.clone();
		Arrays.sort(sorted);
		System.out.println("Highest PageRank values:");
		printSnippet(sorted, true);
		int[] topIndices = new int[quantity];

		for (int i = 0; i < quantity; i++) {
			int idx = 0;
			for (int j = 0; j < pageRanks.length; j++) {
				if (pageRanks[j] == sorted[sorted.length - i - 1]) {
					idx = j;
					break;
				}
			}
			topIndices[i] = idx;
		}

		return topIndices;
	}
	
	public static void printSnippet(double[] arr, boolean reversed) {
		if (reversed) {
			for (int i = arr.length - 1; i > arr.length - 1 - 20; i--) {
				System.out.print(arr[i] + " ");
			}
		} else {
			for (int i = 0; i < 20; i++) {
				System.out.print(arr[i] + " ");
			}
		}
		System.out.println();
	}

	public static void main(String[] args) throws IOException {
		long start = System.currentTimeMillis();
		System.out.println("Loading data...");
		int[] rle = loadBinaryFileFast();
		System.out.println("Done. Took " + (System.currentTimeMillis() - start) / 1000.0 + " sec");

		PageRank pr = new PageRank();
		pr.populateHelperArrays(rle);
		pr.runPageRank(rle);
	}

}
