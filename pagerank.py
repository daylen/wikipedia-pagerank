# coding=utf8
import array
from collections import defaultdict
from functools import partial

MAX_PAGE_ID = 48043687 # `tail` the titles file
ITERATIONS = 10
DAMPING_FACTOR = 0.85

"""
Compute Pagerank using the iterative method.
"""

links_file = "pagelinks_list.txt"
titles_file = "title_id_dict.txt"

"""
We build two data structures:
- A mapping from page ID to a list of incoming page IDs
- A mapping from page ID to the number of outgoing pages
"""

incoming = defaultdict(partial(array.array, 'L'))
outgoing = defaultdict(int)

print('Loading links file...')

with open(links_file) as f:
	for line in f:
		pair = line.split(' ')
		from_id = int(pair[0])
		to_id = int(pair[1])
		incoming[to_id].append(from_id)
		outgoing[from_id] += 1

print('Done')
print(len(incoming.keys()), 'pages with incoming links')
print(len(outgoing.keys()), 'pages with outgoing links')

def argsort(seq):
	return sorted(range(len(seq)), key=seq.__getitem__)

curr_probs = array.array('d', [1] * (MAX_PAGE_ID + 1))
next_probs = curr_probs

for iteration in range(ITERATIONS):
	print('Starting iteration', (iteration + 1))

	for pid in range(MAX_PAGE_ID + 1):
		if pid not in incoming:
			next_probs[pid] = 1 - DAMPING_FACTOR
			continue
		votes = 0
		for in_pid in incoming[pid]:
			votes += (curr_probs[in_pid] / float(outgoing[in_pid]))
		next_probs[pid] = (1 - DAMPING_FACTOR) + DAMPING_FACTOR * votes

	curr_probs = next_probs
	print('Top IDs:', list(reversed(argsort(curr_probs)))[:20])