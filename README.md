# Wikipedia Pagerank

### Team

- Brian Chu
- Nathan Mandi
- Daylen Yang

### System Requirements

- 16 GB of memory.
- 50 GB of free space.
- Java 8.
- PyPy.
- For topic sensitive PageRank: Python3 with `scipy`, `numpy`, and `h5py`.

### Usage

1. Create the `src/data/` directory

   ```
   mkdir -p src/data/
   ```

   and download your desired `enwiki-YYYYMMDD-page.sql.gz` and
   `enwiki-YYYYMMDD-pagelinks.sql.gz` from https://dumps.wikimedia.org/enwiki/
   into `src/data/`.

2. Unzip the Wikipedia database dumps in the `src/data/` directory.

   ```
   gunzip src/data/enwiki-YYYYMMDD-page.sql.gz src/data/enwiki-YYYYMMDD-pagelinks.sql.gz
   ```

3. Open src/convert.py and update the `page_dump_filename` and
   `pagelinks_dump_filename` global variables at the top of the file to point to
   the unzipped Wikipedia database .sql dumps. For example

   ```
   page_dump_filename = reljoin('data/enwiki-20151002-page.sql')
   pagelinks_dump_filename = reljoin('data/enwiki-20151002-pagelinks.sql')
   ```

4. Run src/convert.py with PyPy.

   ```
   pypy src/convert.py
   ```

   Once finished, you can safely delete the Wikipedia database .sql dumps.

5. Compile src/pagerank/Compactify.java

   ```
   javac src/pagerank/Utils.java src/pagerank/Compactify.java
   ```

   and run it with a heap size of 12 GB (`-Xmx12g`)

   ```
   java -cp src/ -Xmx12g pagerank/Compactify
   ```

6. Compile src/pagerank/PageRank.java

   ```
   javac src/pagerank/Utils.java src/pagerank/PageRank.java
   ```

   and run it with a heap size of 12 GB (`-Xmx12g`)

   ```
   java -cp src/ -Xmx12g pagerank/PageRank
   ```

7. Run src/sort.py with PyPy.

   ```
   pypy src/sort.py
   ```

8. View the sorted PageRank results in src/data/sorted_ranking.txt.

For topic sensitive PageRank, see src/topic/README.
