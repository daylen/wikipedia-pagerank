New Projects
Wikipedia Pagerank

Team

- Brian Chu
- Nathan Mandi
- Daylen Yang

System Requirements

- 16 GB of memory
- 50 GB of free space
- PyPy
- Java 8
- Python3.4 with scipy, numpy, and h5py.

Usage

1. Download "enwiki-YYYYMMDD-page.sql.gz" and "enwiki-YYYYMMDD-pagelinks.sql.gz" from https://dumps.wikimedia.org/enwiki/

2. Unzip the Wikipedia database dumps into the src folder.

3. Set the path to the SQL files at the top of convert.py. Then run convert.py using PyPy. When this is done, you can delete the SQL files.

4. Set the path to the pagelinks_list.txt file at the top of Compactify.java. Also set the output paths. Then compile and run Compactify. You should use a Java heap size of 12 GB (the flag is -Xmx12g).

5. Set the path to the rle.binary file at the top of PageRank.java. Also set the output path. Then compile and run PageRank. You should use a Java heap size of 12 GB (the flag is -Xmx12g).

6. Set the path to the relevant files at the top of sort.py. Then run sort.py using PyPy.

7. View sorted_ranking.txt using less or grep.

8. For topic sensitive PageRank, see src/topic/README
