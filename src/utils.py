# coding=utf8

from os.path import dirname, join as pjoin

def reljoin(path):
    return pjoin(dirname(__file__), path)
