import numpy as np
import h5py
from scipy.io import savemat
from scipy.sparse import csr_matrix, csc_matrix, lil_matrix, coo_matrix
from signal import signal, SIGINT, SIG_DFL
from sklearn.preprocessing import normalize
import pickle

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

with open('newid2title.pickle', 'rb') as f:
  id2title = pickle.load(f)

teleport = 0.2
def run():
  random_arrival = teleport * np.ones(n, dtype=np.float64) / n

  def iterate(v):
    newv = M.dot(v)
    newv = np.multiply(1 - teleport, newv, out=newv)
    newv = np.add(newv, random_arrival, out=newv)
    return newv

  v = np.ones(n) / n
  oldv = np.zeros(n)

  sigint_called = False
  def sigint_handler(num, frame):
      global sigint_called
      sigint_called = True
  signal(SIGINT, sigint_handler)

  iteration = 0
  while abs(np.sum(v - oldv)) > 0.00000000001 and not sigint_called:
    oldv = v
    v = iterate(v)
    iteration += 1
    print('Finished iteration: %i' % iteration)

  signal(SIGINT, SIG_DFL)
  return v

v = run()
top = v.argsort()[::-1][:20]
for top_idx in top:
  print('Idx: %i, with title: %s' % (top_idx, id2title[top_idx]) )
