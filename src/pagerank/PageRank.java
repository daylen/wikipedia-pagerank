package pagerank;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

enum RleState {
	TO_PAGE_ID, RUN_LENGTH, FROM_PAGE_ID
}

class PageRankWorker implements Runnable {

	int startIdx;
	int endIdx;
	double[] currPageRanks;
	double[] nextPageRanks;
	int[] rle;

	PageRankWorker(int startIdx, int endIdx, double[] currPageRanks, double[] nextPageRanks, int[] rle) {
		this.startIdx = startIdx;
		this.endIdx = endIdx;
		this.currPageRanks = currPageRanks;
		this.nextPageRanks = nextPageRanks;
		this.rle = rle;
	}

	@Override
	public void run() {
		RleState state = RleState.TO_PAGE_ID;
		int currPageId = -1;
		int runLength = -1;
		for (int i = startIdx; i < endIdx; i++) {
			if (state == RleState.TO_PAGE_ID) {
				currPageId = rle[i];
				state = RleState.RUN_LENGTH;
			} else if (state == RleState.RUN_LENGTH) {
				runLength = rle[i];
				state = RleState.FROM_PAGE_ID;
			} else {
				nextPageRanks[currPageId] += currPageRanks[rle[i]];
				if (--runLength == 0)
					state = RleState.TO_PAGE_ID;
			}
		}

	}

}

public class PageRank {

	private static final double DAMPING_FACTOR = 0.85;
	private static final String BINARY_FILE_NAME = "/Users/daylenyang/Desktop/wikipedia-pagerank/data/rle.binary";
	private static final String OUTPUT_FILE_NAME = "/Users/daylenyang/Desktop/wikipedia-pagerank/data/ranking.txt";
	private static final double ACCEPTABLE_DELTA = 1;
	private static final int THREADS = 8;

	private boolean[] validPages;
	private int numValidPages = 0;
	private int[] outgoingDegree;

	private double[] prevPageRanks;
	private double[] currPageRanks;
	private double[] nextPageRanks;

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

	public int findMaxPageId(int[] rle) {
		int maxPageId = 0;
		System.out.println("Finding max page ID...");
		RleState state = RleState.TO_PAGE_ID;
		int runLength = -1;
		for (int x : rle) {
			switch (state) {
			case TO_PAGE_ID:
				maxPageId = Math.max(maxPageId, x);
				state = RleState.RUN_LENGTH;
				break;
			case RUN_LENGTH:
				runLength = x;
				state = RleState.FROM_PAGE_ID;
				break;
			case FROM_PAGE_ID:
				maxPageId = Math.max(maxPageId, x);
				if (--runLength == 0) {
					state = RleState.TO_PAGE_ID;
				}
				break;
			default:
				break;
			}
		}
		System.out.println("Max page ID: " + maxPageId);
		return maxPageId;
	}

	/**
	 * Fill in the validPages, numValidPages, and outgoingDegree arrays.
	 * 
	 * @param rle
	 *            The run length encoded data.
	 */
	public void populateHelperArrays(int[] rle, int maxPageId) {
		System.out.println("Populating helper arrays...");
		validPages = new boolean[maxPageId + 1];
		outgoingDegree = new int[maxPageId + 1];

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
				if (--runLength == 0) {
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

	public void runPageRank(int[] rle, int maxPageId, int[] splitIndices) throws InterruptedException {

		currPageRanks = new double[maxPageId + 1];

		for (int i = 0; i < currPageRanks.length; i++) {
			if (validPages[i]) {
				currPageRanks[i] = 1.0;
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

			nextPageRanks = new double[currPageRanks.length];

			ExecutorService threadPool = Executors.newFixedThreadPool(splitIndices.length);
			for (int i = 0; i < splitIndices.length - 1; i++) {
				threadPool.submit(new PageRankWorker(splitIndices[i], splitIndices[i + 1], currPageRanks,
						nextPageRanks, rle));
			}
			threadPool.submit(new PageRankWorker(splitIndices[splitIndices.length - 1], rle.length, currPageRanks,
					nextPageRanks, rle));
			threadPool.shutdown();
			threadPool.awaitTermination(60, TimeUnit.DAYS);

			// Dampen
			for (int i = 0; i < nextPageRanks.length; i++) {
				if (validPages[i]) {
					nextPageRanks[i] = (nextPageRanks[i] + sinkNodePR) * DAMPING_FACTOR + (1.0 - DAMPING_FACTOR);
				}
			}

			// Recycle generation
			currPageRanks = nextPageRanks;

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

	public int[] findSplitIndices(int[] rle, int numSplit) {
		System.out.println("Finding split points for length " + rle.length + " array");
		int approxSize = rle.length / numSplit;
		int[] targetIndices = new int[numSplit];
		for (int i = 0; i < targetIndices.length; i++) {
			targetIndices[i] = approxSize * i;
		}
		System.out.println("Target: " + Arrays.toString(targetIndices));

		int[] actualIndices = new int[numSplit];

		RleState state = RleState.TO_PAGE_ID;
		int runLength = -1;
		int region = 0;
		for (int i = 0; i < rle.length; i++) {
			if (state == RleState.TO_PAGE_ID) {
				state = RleState.RUN_LENGTH;
				if (region == targetIndices.length) {
					break;
				}
				if (i < targetIndices[region]) {
					actualIndices[region] = i;
				} else {
					region++;
				}
			} else if (state == RleState.RUN_LENGTH) {
				runLength = rle[i];
				state = RleState.FROM_PAGE_ID;
			} else {
				if (--runLength == 0)
					state = RleState.TO_PAGE_ID;
			}
		}

		System.out.println("Actual: " + Arrays.toString(actualIndices));

		return actualIndices;

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

	public static void main(String[] args) throws IOException, InterruptedException {
		long start = System.currentTimeMillis();
		System.out.println("Loading data...");
		int[] rle = loadBinaryFileFast();
		System.out.println("Done. Took " + (System.currentTimeMillis() - start) / 1000.0 + " sec");

		PageRank pr = new PageRank();
		int[] splitIndices = pr.findSplitIndices(rle, THREADS);
		int maxPageId = pr.findMaxPageId(rle);
		pr.populateHelperArrays(rle, maxPageId);
		pr.runPageRank(rle, maxPageId, splitIndices);
		pr.writePageRanks();
	}

}
