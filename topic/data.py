import numpy as np
import h5py
import pickle
import csv
from collections import defaultdict

num_ids = 12000926

kvs = []
with open('../title_id_dict.txt','r') as f:
  for line in f:
    c = line.split(' ')
    id = int(c[1].strip())
    # strip the leading and trailing quotes
    title = c[0].strip()[1:-1]
    kvs.append((id, title))
kvs.sort(key=lambda pair: pair[0])
keys, values = list(zip(*kvs))
del kvs

count = 0
id2newid = {}
for key in keys:
  id2newid[key] = count
  count += 1

newid2title = {}
title2newid = {}
for key, value in zip(range(len(values)), values):
  newid2title[key] = value
  title2newid[value] = key
del keys
del values

with open('newid2title.pickle', 'wb') as f:
  pickle.dump(newid2title, f, protocol=pickle.HIGHEST_PROTOCOL)

with open('title2newid.pickle', 'wb') as f:
  pickle.dump(title2newid, f, protocol=pickle.HIGHEST_PROTOCOL)

with open('newid2title.txt', 'w') as f:
  for key, value in newid2title.items():
    f.write('%i %s\n' % (key, value))

with open('id2newid.pickle', 'wb') as f:
  pickle.dump(id2newid, f, protocol=pickle.HIGHEST_PROTOCOL)

with open('id2newid.txt', 'w') as f:
  for id, newid in id2newid.items():
    f.write('%i %i\n' % (id, newid))

#### Category links data

category2ids = defaultdict(set)
id2categories = defaultdict(set)

with open('categorylinks.csv', newline='', errors='ignore') as f:
  links = csv.reader(f)
  for link in links:
    try:
      id = int(link[0])
      id = id2newid[id]
    except (ValueError, KeyError):
      continue
    category = link[1]
    category2ids[category].add(id)
    id2categories[id].add(category)
  del links

with open('category2ids.pickle', 'wb') as f:
  pickle.dump(category2ids, f, protocol=pickle.HIGHEST_PROTOCOL)
del category2ids

id2categories1 = {key:value for i,(key, value) in enumerate(id2categories.items()) if i % 2 == 0}
with open('id2categories1.pickle', 'wb') as f:
  pickle.dump(id2categories1, f, protocol=pickle.HIGHEST_PROTOCOL)
del id2categories1

id2categories2 = {key:value for i,(key, value) in enumerate(id2categories.items()) if i % 2 == 1}
with open('id2categories2.pickle', 'wb') as f:
  pickle.dump(id2categories2, f, protocol=pickle.HIGHEST_PROTOCOL)

#### Links data

# num_links = 374556579
# def update_links_ids(id2newid):
#   with open('../pagelinks_list.txt', 'r') as f:
#     lst = np.empty((num_links, 2), dtype=np.int32)
#     for i, line in enumerate(f):
#       c = line.split(' ')
#       lst[i,0] = id2newid[np.int(c[0])]
#       lst[i,1] = id2newid[np.int(c[1])]
#     dst = h5py.File('newpagelinks_list.h5', 'w')
#     dst.create_dataset('pagelinks_list', data=lst, compression='gzip')
#     dst.close()
# update_links_ids(id2newid)
