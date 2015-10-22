import numpy as np
import h5py
import pickle

num_ids = 12000926

kvs = []
with open('../title_id_dict.txt','r') as f:
  for line in f:
    c = line.split(' ')
    id = int(c[1].strip())
    title = c[0].strip()
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
for key, value in zip(range(len(values)), values):
  newid2title[key] = value
del keys
del values

with open('newid2title.pickle', 'wb') as f:
  pickle.dump(newid2title, f, protocol=pickle.HIGHEST_PROTOCOL)

with open('id2newid.txt', 'w') as f:
  for id, newid in id2newid.items():
    f.write('%i %i\n' % (id, newid))

num_links = 374556579
def update_links_ids(id2newid):
  with open('../pagelinks_list.txt', 'r') as f:
    lst = np.empty((num_links, 2), dtype=np.int32)
    for i, line in enumerate(f):
      c = line.split(' ')
      lst[i,0] = id2newid[np.int(c[0])]
      lst[i,1] = id2newid[np.int(c[1])]
    dst = h5py.File('newpagelinks_list.h5', 'w')
    dst.create_dataset('pagelinks_list', data=lst, compression='gzip')
    dst.close()
update_links_ids(id2newid)
