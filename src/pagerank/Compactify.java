package pagerank;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Convert the links text file into a run length encoding binary format, so it
 * loads faster!
 * 
 * @author daylenyang
 *
 */
public class Compactify {

	public static final int PRINT_INTERVAL = 1000000;
	public static final String INPUT_FILE = Utils.reljoin("data/pagelinks_list.txt");
	public static final String FIRST_PASS_OUTPUT_FILE = Utils.reljoin("data/longs.binary");
	public static final String SECOND_PASS_OUTPUT_FILE = Utils.reljoin("data/rle.binary");

	/**
	 * Given a list of flattened pairs, returns data that can be passed into the
	 * rle() method for testing.
	 * 
	 * @param pairList
	 *            An array of flattened pairs.
	 * @return A list where each pair is represented as a 64-bit integer, with
	 *         the to page ID in the upper bits and the from page ID in the
	 *         lower bits.
	 */
	public static long[] genTestData(long[] pairList) {
		assert (pairList.length % 2 == 0);
		List<Long> data = new ArrayList<>();

		for (int i = 0; i < pairList.length; i += 2) {
			data.add((pairList[i + 1] << 32) | pairList[i]);
		}
		Collections.sort(data);

		return data.stream().mapToLong(l -> l).toArray();
	}

	/**
	 * Run Length Encodes the data.
	 * 
	 * @param data
	 *            A list where each pair is represented as a 64-bit integer,
	 *            with the to page ID in the upper bits and the from page ID in
	 *            the lower bits.
	 * @return The run length encoding of the data. The run length encoding is
	 *         as follows: the to page ID comes first, then the number of pages
	 *         that point to the to page ID, and then the IDs of the pages that
	 *         point to the to page ID. The first element is the number of
	 *         elements in the array plus 1.
	 */
	public static int[] rle(long[] data) {
		System.out.println("Performing RLE on " + data.length + " elements");

		int[] rle = new int[10];

		int endIndex = 1; // 0 is reserved for the size of the array

		int currentPageId = -1;
		int runLengthIndex = -1;
		int nextPrint = 0;

		for (int i = 0; i < data.length; i++) {
			int toId = (int) (data[i] >>> 32);
			int fromId = (int) (data[i] & 0xFFFFFFFF);

			// Check if we need to expand the array
			if (rle.length - endIndex < 5) {
				rle = Arrays.copyOf(rle, rle.length * 2);
			}

			if (toId != currentPageId) {
				if (runLengthIndex != -1)
					rle[runLengthIndex] = endIndex - runLengthIndex - 1;

				rle[endIndex++] = toId;
				currentPageId = toId;
				runLengthIndex = endIndex;
				rle[endIndex++] = -1; // Will be replaced on next page ID switch
				rle[endIndex++] = fromId;
			} else {
				rle[endIndex++] = fromId;
			}

			if (i >= nextPrint) {
				System.out.println("Encoded " + i + " elements");
				nextPrint += PRINT_INTERVAL;
			}
		}
		rle[runLengthIndex] = endIndex - runLengthIndex - 1;

		System.out.println("Finished main loop, truncating array");

		rle[0] = endIndex - 1;

		rle = Arrays.copyOf(rle, endIndex);

		System.out.println("Done");

		return rle;
	}

	/**
	 * Writes the run-length encoded data to a file.
	 * 
	 * @param data
	 *            Any array of integers.
	 * @throws IOException
	 */
	public static void writeSecondPass(int[] data) throws IOException {
		System.out.println("Writing RLE array");
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(
				SECOND_PASS_OUTPUT_FILE)));
		for (int i : data) {
			out.writeInt(i);
		}
		out.close();
		System.out.println("Done");
	}

	/**
	 * Transforms ASCII text into a simple binary format. Each line in the ASCII
	 * text represents a from and to page ID, space separated. The binary format
	 * is comprised of 64-bit integers, where the upper bits are the to page ID
	 * and the lower bits are the from page ID.
	 * 
	 * @return The number of pairs.
	 * @throws IOException
	 */
	public static int firstPass() throws IOException {
		System.out.println("Converting ASCII text file to binary format");
		int linesRead = 0;
		int nextPrint = 0;
		BufferedReader reader = new BufferedReader(new FileReader(INPUT_FILE));
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(
				FIRST_PASS_OUTPUT_FILE)));
		String line;
		while ((line = reader.readLine()) != null) {
			String[] pair = line.split(" ");
			Long from = Long.parseLong(pair[0]);
			Long to = Long.parseLong(pair[1]);
			long res = (to << 32) | from;
			out.writeLong(res);
			linesRead++;
			if (linesRead >= nextPrint) {
				System.out.println("Converted " + linesRead + " lines");
				nextPrint += PRINT_INTERVAL;
			}
		}

		System.out.println("Finished converting " + linesRead + " lines");

		reader.close();
		out.close();

		return linesRead;
	}

	/**
	 * Loads the binary file produced in the first stage. The binary format is
	 * comprised of 64-bit integers, where the upper bits are the to page ID and
	 * the lower bits are the from page ID.
	 * 
	 * @param size
	 *            The number of pairs.
	 * @return An array of 64-bit integers, where each integer represents a
	 *         pair.
	 * @throws IOException
	 */
	public static long[] loadBinaryFile(int size) throws IOException {
		System.out.println("Loading first pass file again");
		long[] links = new long[size];
		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(FIRST_PASS_OUTPUT_FILE)));
		for (int i = 0; i < links.length; i++) {
			links[i] = in.readLong();
		}
		in.close();
		System.out.println("Done");
		return links;
	}

	public static void main(String[] args) throws IOException {
		int size = firstPass();
		long[] data = loadBinaryFile(size);
		System.out.println("Sorting");
		Arrays.sort(data);
		int[] rle = rle(data);
		writeSecondPass(rle);
	}

}
