"""
Sorts and combines the final rankings
"""

from utils import reljoin

rankings_file = reljoin('data/ranking.txt')
titles_file = reljoin('data/title_id_dict.txt')
output_file = reljoin('data/sorted_ranking.txt')

rankings = []
titles = {}

print('Loading rankings')
with open(rankings_file, 'r') as f:
	for line in f:
		pair = line.split(' ')
		rankings.append((float(pair[1]), int(pair[0])))

print('Loading titles')
with open(titles_file, 'r') as f:
	for line in f:
		pair = line.split(' ')
		title = pair[0]
		title = title[1:-1]
		title = title.replace('_', ' ')
		titles[int(pair[1])] = title

print('Sorting')
rankings.sort()

print('Reversing')
rankings.reverse()

print('Writing output file')
with open(output_file, 'w+') as f:
	for pagerank, pageid in rankings:
		f.write(str(pagerank) + ' ' + titles[pageid] + '\n')

print('Done')
