import igraph as ig
import networkx as nx
import random as rd
import sys


import numpy as np
import matplotlib.pyplot as plt

graph_ig=ig.load('lngraph20211125connected.txt',format='pickle')
graph= graph_ig.to_networkx()



with open('graph.txt_LN_PARAMS', 'w') as f:
    sys.stdout = f # Change the standard output to the file we created.
    print('# Graph Property Class')
    print('paymentrouting.datasets.LNParams')
    print('# Key')
    print('LN_PARAMS')
    for e,v,d in graph.edges(data=True):
        print(str(e) + ' ' + str(v) + ' ' + str(d['base_fee']) + ' ' + str(d['fee_rate']) + ' 144.0 0.0 -1.0')
        if not graph.has_edge(v, e):
            print(str(v) + ' ' + str(e) + ' ' + '10000000 10000000 144.0 0.0 -1.0')
