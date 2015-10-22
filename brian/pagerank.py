import numpy as np
import h5py
import pickle
from scipy.io import savemat
from scipy.sparse import csr_matrix, csc_matrix, lil_matrix, coo_matrix
from sklearn.preprocessing import normalize
from subprocess import check_output

src = h5py.File('newpagelinks_list.h5', 'r')
links = src['pagelinks_list']

# m is of form matrix[from_idx][to_idx] = 1
# build matrix from links
num_ids = 12000926
M = coo_matrix( (np.ones(len(links)), (links[:, 0], links[:, 1]) ), shape=(num_ids, num_ids), dtype=np.float64)
n = M.shape[0]

# convert to csr_matrix, then we will normalize by row using sklearn, this is efficient because of csr_matrix
M = M.tocsr()
normalize(M, norm='l1', copy=False)
# we will find right eigenvalue, so convert matrix to form matrix[to_idx][from_idx], transpose implicitly gives csc_matrix, so covert back to csr
M = M.transpose().tocsr()
del links
del src

print("Set up matrix")

with open('newid2title.pickle', 'rb') as f:
   id2title = pickle.load(f)

with open('title2newid.pickle', 'rb') as f:
   title2id = pickle.load(f)

with open('id2newid.pickle', 'rb') as f:
  id2newid = pickle.load(f)

# with open('id2categories1.pickle', 'rb') as f:
  # id2categories1 = pickle.load(f)

# with open('id2categories2.pickle', 'rb') as f:
  # id2categories2 = pickle.load(f)

with open('category2ids.pickle', 'rb') as f:
  category2ids = pickle.load(f)

print("Loaded dicts")

teleport = 0.15
def run(arrival_indices=None):
  # topic weighting
  if arrival_indices:
    random_arrival = np.zeros(n, dtype=np.float64)
    for idx in arrival_indices:
      random_arrival[idx] = 1
    random_arrival *= teleport / len(arrival_indices)
  # no topic weighting
  else:
    random_arrival = teleport * np.ones(n, dtype=np.float64) / n

  def iterate(v):
    newv = M.dot(v)
    newv = np.multiply(1 - teleport, newv, out=newv)
    newv = np.add(newv, random_arrival, out=newv)
    return newv

  v = np.ones(n) / n
  oldv = np.zeros(n)

  iteration = 0
  while abs(np.sum(v - oldv)) > 0.000000001:
    oldv = v
    v = iterate(v)
    iteration += 1
    if iteration % 10 == 0:
      print('Finished iteration:', iteration)
  print('Total iterations:', iteration)
  return v

def print_top(v):
  top = v.argsort()[::-1][:20]
  for top_idx in top:
    print('Idx: %i, with title: %s' % (top_idx, id2title[top_idx]) )

school_experiments = [
  'science',
  'engineering',
  'math',
  'artist',
  'music',
  'literature',
]
schools = [
  'University_of_California,_Berkeley',
  'Stanford_University',
  'Harvard_University',
  'California_Institute_of_Technology',
  'Massachusetts_Institute_of_Technology',
  'Princeton_University',
  'Yale_University',
  'Columbia_University',
  'Brown_University',
  'University_of_Oxford',
  'University_of_Cambridge',
  'Carnegie_Mellon_University',
  'University_of_Pennsylvania',
  'University_of_Chicago',
  'Duke_University',
  'Cornell_University',
  'New_York_University',
  'Juilliard_School',
  'Berklee_College_of_Music',
  'Rhode_Island_School_of_Design'
]

school_indices = [title2id[title] for school in schools]
school_experiment_ids = []
for experiment in school_experiments:
  categories = check_output(['grep', '-i', experiment, 'categories.txt']).decode('utf-8')
  categories = categories.split()
  ids = set()
  for category in categories:
    ids = ids.union(category2ids[category])
  school_experiment_ids.append(ids)

for experiment, ids in zip(school_experiments, school_experiment_ids):
  print('\n****Running:', experiment)
  v = run(ids)
  print_top(v)
  scores = v[school_indices]
  # indices in school_titles array of top ranking schools to low ranking schools
  order = scores.argsort()[::-1]
  for idx in order:
    print('%s has %f' % (schools[idx], scores[idx]) )

candidate_experiments = [
  'immigration',
  'gun_control',
  'welfare',
  'abortion',
  'campaign_finance',
  'healthcare'
]
candidates = [
  'Bernie_Sanders',
  'Hillary_Clinton',
  'Donald_Trump',
  'Jeb_Bush',
  'Ben_Carson',
  'Carly_Fiorina',
  'Joe_Biden',
  'Marco_Rubio',
  'Chris_Christie',
  'Lawrence_Lessig',
]
candidate_indices = [title2id[candidate] for candidate in candidates]
candidate_experiment_ids = []
for experiment in candidate_experiments:
  categories = check_output(['grep', '-i', experiment, 'categories.txt']).decode('utf-8')
  categories = categories.split()
  ids = set()
  for category in categories:
    ids = ids.union(category2ids[category])
  candidate_experiment_ids.append(ids)

for experiment, ids in zip(candidate_experiments, candidate_experiment_ids):
  print('\n****Running:', experiment)
  v = run(ids)
  print_top(v)
  scores = v[candidate_indices]
  # indices in candidate_titles array of top ranking candidates to low ranking candidates
  order = scores.argsort()[::-1]
  for idx in order:
    print('%s has %f' % (candidates[idx], scores[idx]) )

